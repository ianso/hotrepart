
/*
 * 
 * PL/Proxy setup
 * 
 */

create language plpythonu;
create language plpgsql;

-- handler function
CREATE FUNCTION plproxy_call_handler ()
RETURNS language_handler AS '$libdir/plproxy' LANGUAGE C;

-- language
CREATE LANGUAGE plproxy HANDLER plproxy_call_handler;

CREATE SCHEMA plproxy;

--PL/Proxy internal function
CREATE OR REPLACE FUNCTION plproxy.get_cluster_config (
  in cluster_name text,
  out key text,
  out val text)
RETURNS SETOF record AS $$
BEGIN
    -- lets use same config for all clusters
    key := 'connection_lifetime';
    val := 30*60; -- 30m
    RETURN NEXT;
    RETURN;
END;
$$ LANGUAGE plpgsql;

/*
plproxy & partition configuration
*/
create table plproxy.config (
	proxy_connstr     text,
	partition_pass    text,
	cluster_version   int4
);

/*

Partition configuraton table. Note that read start & end may be null.

*/
CREATE TABLE plproxy.partitions
(
  id serial not null,
  "name" text NOT NULL,
  read_start uuid,
  read_end uuid,
  write_start uuid NOT NULL,
  write_end uuid NOT NULL,
  connstr text NOT NULL,
  status character(1) not null,
  CONSTRAINT partition_pkey PRIMARY KEY (id)
)
WITH (OIDS=FALSE);

/*

Retrieves cluster connection strings for a given cluster.

We pad the result to return a number of results to the power of two.

*/
CREATE OR REPLACE FUNCTION plproxy.get_cluster_partitions(cluster_name text, out connstr text, out read_start uuid, out read_end uuid)
RETURNS SETOF record AS $$
declare
	total_range uuid;
	num_ranges integer;
	closest_power   int4;
	last_connstr   text;
BEGIN
	num_ranges := 0;
	total_range := '00000000-0000-0000-0000-000000000000'::uuid;
	
	for connstr, read_start, read_end in 
        select partitions.connstr, partitions.read_start, partitions.read_end
        from plproxy.partitions
        where partitions.name = cluster_name and partitions.read_start is not null	loop		
		total_range := plproxy.uuid_add(total_range, plproxy.uuid_difference(read_end, read_start)::uuid);
		num_ranges := num_ranges + 1;
		last_connstr := connstr;
		return next;
	end loop;

	if num_ranges = 0 then
		raise exception 'no ranges found in the DB for cluster %!', cluster_name;
	end if;

	if plproxy.uuid_add(total_range, num_ranges-1)::uuid
	  != 'ffffffff-ffff-ffff-ffff-ffffffffffff'::uuid then
		raise exception 'Total UUID range % is not equal to enture range 32*f for cluster %', 
			total_range, cluster_name;
	end if;

	closest_power := 1;

	if closest_power = num_ranges then
		return;
	end if;

	loop
		closest_power := closest_power * 2;
		exit when closest_power >= num_ranges;
	end loop;
	
	/*
	 
	this brings the total number of connections up to the required
	power-of-two necessary to make PL/Proxy think its doing 
	hash-based partitioning... 
	
	*/
	
	if closest_power > num_ranges then
		for connstr, read_start, read_end in
			select partitions.connstr, partitions.read_start, partitions.read_end
			from plproxy.partitions, generate_series(1, closest_power - num_ranges)
			where connstr = last_connstr
			and name = cluster_name
		loop
			return next;
		end loop;
	end if;
	
END;
$$ LANGUAGE plpgsql;


/*

Called on every call to PL/Proxy to ensure that the cluster configuration
hasn't changed. Not used in practise in this configuration.

*/
CREATE OR REPLACE FUNCTION plproxy.get_cluster_version(cluster_name text)
RETURNS int4 AS $$
select cluster_version from plproxy.config;
$$ LANGUAGE sql;



/*

All the UUID maths is done in Python because it's
easier than decomposing a 128-bit number into two
64-bit numbers in PL/PGSQL, subtracting a number, etc,
or hacking a hex-to-bigdecimal converter in SQL.

*/
create or replace function plproxy.uuid_add(in_uuid uuid, num bigint) returns text as $$
	import uuid;
	return uuid.UUID(int=(uuid.UUID(in_uuid).int + num)).hex;
$$ language plpythonu;

create or replace function plproxy.uuid_add(in_uuid1 uuid, in_uuid2 uuid) returns text as $$
	import uuid;
	return uuid.UUID(int=(uuid.UUID(in_uuid1).int + uuid.UUID(in_uuid2).int)).hex;
$$ language plpythonu;

create or replace function plproxy.uuid_midpoint(range_start uuid, range_end uuid) returns text as $$
	import uuid;
	uuid_from = uuid.UUID(range_start);
	diff = uuid.UUID(range_end).int - uuid_from.int;
	return uuid.UUID(int=(uuid_from.int + (diff / 2))).hex;
$$ language plpythonu;

/* assumes first one is bigger ! (lazy me) */
create or replace function plproxy.uuid_difference(in_uuid1 uuid, in_uuid2 uuid) returns text as $$
	import uuid;
	return uuid.UUID(int=(uuid.UUID(in_uuid1).int - uuid.UUID(in_uuid2).int)).hex;
$$ language plpythonu;

/*

This procedure is called via a loopback db link in split_partition, adds
a new partition to the partition table, and updates the write range
of the original partition.

 * ranges are always "take it from the top" - 
 * the range_start is where the new range starts
 * and the old one is split (+1).
 */
create or replace function plproxy.add_partition(id_parentpartition integer, range_start uuid, new_connstr text) returns integer as $$
declare 
	parent_partition	record;
	new_partition		integer;
begin
	--retrieve parent partition
	select * into parent_partition from plproxy.partitions where id = id_parentpartition;

	/*
	 * 1) Add new partition with the new start-point and the parent end-point
	 */
	insert into plproxy.partitions(name, write_start, write_end, connstr, status) 
		values (parent_partition.name, range_start, parent_partition.write_end, new_connstr, 'X');

	new_partition := currval('plproxy.partitions_id_seq');
	
	raise notice 'new proxy % inserted from: %, to:%, connstr:%', new_partition, range_start, parent_partition.write_end, new_connstr;

	/*
	 * 2) modify the source partition with the revised end-point.
	 */
	update plproxy.partitions set write_end = plproxy.uuid_add(range_start, -1)::uuid where id = id_parentpartition;

	raise notice 'partition % updated with to = %', id_parentpartition, plproxy.uuid_add(range_start, -1);

	return new_partition;

end;
$$ language plpgsql;

/*

Called below via loopback dblink in split_partition, this procedure
unlocks a new partition for writes, makes it available for
reads, and updates the read range of the parent partition.

*/
create or replace function plproxy.activate_partition(id_partition integer, id_parentpartition integer) returns void as $$
begin

	/*
	 * 1) Unlock the new partition for writes, and activate it for reads.
	 */
	update plproxy.partitions set status='A', read_start = write_start, read_end = write_end where partitions.id = id_partition;
	
	/*
	 * 2) update the parent partition reads to equal the writes.
	 */
	update plproxy.partitions set read_end = write_end where id = id_parentpartition;

    update plproxy.config set cluster_version = cluster_version + 1;

	raise notice 'partition % activated', id_partition;

end;
$$ language plpgsql;

/*

This is the function that is invoked from externally to
repartition an existing partition.

*/
create or replace function plproxy.split_partition(id_partition integer) returns void as $$
declare
	config	 record;
	partition 	record;
	id_queue 			integer;
	id_newpartition 		integer;
	newconnstr 			text;
	new_start 			uuid;
	dbname 				text;
	remove_result 		integer;
	--exception handling
	connections 			text[];
	num_con 				integer;
begin

	select * into config from plproxy.config;

	select * into partition from plproxy.partitions where id = id_partition;
	
	perform dblink.connect('source', partition.connstr);

	new_start := plproxy.uuid_midpoint(partition.write_start, partition.write_end);

	raise notice 'Dividing range % to % at point %', partition.write_start, partition.write_end, new_start;

	/*
	 * 1) add the queue range to the partition we are going to split.
	 *
	 * From this point forward, all calls for this range will be queued 
	 * for later update on the new partition.
	 */

	select o_id_queue into id_queue from 
		dblink.query('source', 'select partition.add_range_queue('''||new_start||''', '''||partition.write_end||''')')
		as (o_id_queue integer);

	raise notice 'Range queue added, id_queue=%', id_queue;

	/*
	 * 2) Create the new database with a partial copy of the old database
	 */

	select o_dbname into dbname from
		dblink.query('source', 'select partition.get_unused_database(''items'')')
		as (o_dbname text);

	raise notice 'Creating database %', dbname;

	perform dblink.exec('source', 'create database '||dbname);

	perform * from dblink.query(
		'source', 
		'select partition.backup_restore_to('''||dbname||''', '''||config.partition_pass||''')')
		as tmp(result text);

 	newconnstr := regexp_replace(partition.connstr, 'dbname=[a-zA-Z0-9_]+', 'dbname='||dbname);

	raise notice 'New DB created, connstr=%', newconnstr;

	perform dblink.connect('newconnstr', newconnstr);

	--clean up unneeded data & queue from new database 
	-- a) the half of the database we dont need:
	perform dblink.exec('newconnstr', 'delete from private.items where iditem < '''||new_start||'''');
	-- b) the writes captured before we backed up the database
	perform dblink.exec('newconnstr', 'delete from partition.queue where id_queue = '||id_queue);
	-- c) the queue itself
	perform dblink.exec('newconnstr', 'delete from partition.queue_ranges where id = '||id_queue);

	raise notice 'Connected to %, doing initial read_from_queue', newconnstr;

	-- 3) read in calls accumulated during the setup of the new DB.

	perform * from 
		dblink.query('newconnstr', 'select partition.read_from_queue('''||partition.connstr||''', '||id_queue||')')
		as tmp(result text);

	/* 
	   this is the loopback connection to this database, so that we can
	   commit stuff mid-procedure.
	 */
	perform dblink.connect('proxy', config.proxy_connstr);


	/*
	 *  4) lock the PL/Proxy to any more additions to the queue;
	 * (range is added and automatically locked, no updates or queries 
	 * may be sent to that range)
	 * 
	 * using loopback dblink (*sigh*)
	 */
	select result into id_newpartition from 
		dblink.query('proxy', 'select plproxy.add_partition('||id_partition||', '''||new_start||''', '''||newconnstr||''')')
		as (result integer);

	-- 5) read anything extra that may be in the queue

	loop
		raise notice 'Doing read_from_queue';
		perform * from 
			dblink.query('newconnstr', 'select partition.read_from_queue('''||partition.connstr||''', '||id_queue||')') as tmp(result text);
		select result into remove_result from 
			dblink.query('source', 'select partition.remove_queue('||id_queue||')') as (result integer);
		raise notice 'remove result: %', remove_result;
		if remove_result = 1 then
			exit;
		end if;
	end loop;

	-- 7) activate the range

	perform * from 
		dblink.query('proxy', 'select plproxy.activate_partition('||id_newpartition||', '''||id_partition||''')')
		as tmp(result text);

	raise notice 'Queue query finished';

	-- 8) remove unused data in old range!
	perform dblink.exec('source', 'delete from private.items where iditem >= '''||new_start||'''');

	perform dblink.disconnect('source');
	perform dblink.disconnect('newconnstr');
	perform dblink.disconnect('proxy');
exception when OTHERS then
	connections := dblink.get_connections();
	for num_con in coalesce(array_lower(connections,1),0) .. coalesce(array_upper(connections,1),-1) loop
		perform dblink.disconnect(connections[num_con]);
	end loop;
	raise exception 'Error, msg:%', SQLERRM;
end;
$$ language plpgsql;



/*
 * 
 * PL/Proxy private proxy functions

These are all called by the the procedure in the same name
in the 'public' partition, and all forward the call to the
connection string specified in the connstr argument.

 * 
 */


CREATE SCHEMA private;

create or replace function private.insert_item(i_iditem uuid, i_description text, i_owner bigint, range_start uuid, range_end uuid, connstr text) returns integer as $$
connect connstr;
$$ language plproxy;

create or replace function private.update_item(i_iditem uuid, i_description text, i_v int, range_start uuid, range_end uuid, connstr text) returns integer as $$
connect connstr;
$$ language plproxy;

create or replace function private.get_items(owners bigint[], out iditem uuid, out description text, out owner bigint, out v int) returns setof record as $$
cluster 'items';
run on all;
select * from private.get_items(owners, read_start, read_end);
$$ language plproxy;

create or replace function private.get_item(i_iditem uuid, range_start uuid, range_end uuid, connstr text, out iditem uuid, out description text, out owner bigint, out v int) returns setof record as $$
connect connstr;
$$ language plproxy;


/*
 * 
 * PL/proxy public read functions.

The read function get_item only retrieves the proper partition for the
query and then passes this (plus range info) to the PL/Proxy function
above. If no partition is found with a range containing this item ID,
an exception is raised.

 */

CREATE OR REPLACE FUNCTION public.get_item(IN i_iditem uuid, OUT iditem uuid, OUT description text, OUT "owner" bigint, OUT v integer)
  returns setof record AS $$
declare
	partition  record;
	num_found  integer;
begin
	select * into partition from plproxy.partitions where name='items' and read_start <= i_iditem and read_end >= i_iditem;

	get diagnostics num_found = row_count;
	if num_found = 0 then
			raise exception 'could not find partition with range containing uuid %', i_iditem;
	end if;

	return query select * from private.get_item(i_iditem, partition.read_start, partition.read_end, partition.connstr);
end;
$$ language plpgsql;

/*

get_items makes a query across all partitions based on criteria which is not
in the primary key.

*/
CREATE OR REPLACE FUNCTION public.get_items(IN owners bigint[], OUT iditem uuid, OUT description text, OUT "owner" bigint, OUT v integer)
  RETURNS SETOF record AS
$$
    select * from private.get_items($1);
$$ language sql;

/*

PL/Proxy public write functions.

The write functions may currently be executed on one partition only, and 
only on partitions that are active (status 'A'). When partitions are found
with status non-A, the write enters a spinlock. When no partitions are found,
an exception is raised.

*/

CREATE OR REPLACE FUNCTION public.insert_item(i_iditem uuid, i_description text, i_owner bigint)
  RETURNS integer AS $$
declare
	partition record;
	num_found integer;
begin
	loop
		select * into partition from plproxy.partitions where name='items' and write_start <= i_iditem and write_end >= i_iditem;

		get diagnostics num_found = row_count;
		if num_found = 0 then
			raise exception 'could not find partition with range containing uuid %', i_iditem;
		end if;

		exit when partition.status = 'A';
		perform pg_sleep(0.2); --sleep for 200 milliseconds while the range is updated.
	end loop;

	return private.insert_item(i_iditem, i_description, i_owner, partition.write_start, partition.write_end, partition.connstr);
end;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION public.update_item(i_iditem uuid, i_description text, i_v integer)
  RETURNS integer AS $$
declare
	partition record;
	num_found integer;
begin
	loop
		select * into partition from plproxy.partitions where name='items' and write_start <= i_iditem and write_end >= i_iditem;

		get diagnostics num_found = row_count;
		if num_found = 0 then
			raise exception 'could not find partition with range containing uuid %', i_iditem;
		end if;

		exit when partition.status = 'A';
		perform pg_sleep(0.2); --sleep for 200 milliseconds while the range is updated.
	end loop;

	return private.update_item(i_iditem, i_description, i_v, partition.write_start, partition.write_end, partition.connstr);
end;
$$ LANGUAGE plpgsql;
