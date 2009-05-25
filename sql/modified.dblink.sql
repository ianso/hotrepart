/* $PostgreSQL: pgsql/contrib/dblink/dblink.sql.in,v 1.14 2007/11/13 04:24:27 momjian Exp $ */

create schema dblink;

-- dblink_connect now restricts non-superusers to password
-- authenticated connections
CREATE OR REPLACE FUNCTION dblink.connect (text)
RETURNS text
AS '$libdir/dblink','dblink_connect'
LANGUAGE C STRICT;

CREATE OR REPLACE FUNCTION dblink.connect (text, text)
RETURNS text
AS '$libdir/dblink','dblink_connect'
LANGUAGE C STRICT;

-- dblink_connect_u allows non-superusers to use
-- non-password authenticated connections, but initially
-- privileges are revoked from public
CREATE OR REPLACE FUNCTION dblink.connect_u (text)
RETURNS text
AS '$libdir/dblink','dblink_connect'
LANGUAGE C STRICT SECURITY DEFINER;

CREATE OR REPLACE FUNCTION dblink.connect_u (text, text)
RETURNS text
AS '$libdir/dblink','dblink_connect'
LANGUAGE C STRICT SECURITY DEFINER;

REVOKE ALL ON FUNCTION dblink.connect_u (text) FROM public;
REVOKE ALL ON FUNCTION dblink.connect_u (text, text) FROM public;

CREATE OR REPLACE FUNCTION dblink.disconnect ()
RETURNS text
AS '$libdir/dblink','dblink_disconnect'
LANGUAGE C STRICT;

CREATE OR REPLACE FUNCTION dblink.disconnect (text)
RETURNS text
AS '$libdir/dblink','dblink_disconnect'
LANGUAGE C STRICT;

CREATE OR REPLACE FUNCTION dblink.open (text, text)
RETURNS text
AS '$libdir/dblink','dblink_open'
LANGUAGE C STRICT;

CREATE OR REPLACE FUNCTION dblink.open (text, text, boolean)
RETURNS text
AS '$libdir/dblink','dblink_open'
LANGUAGE C STRICT;

CREATE OR REPLACE FUNCTION dblink.open (text, text, text)
RETURNS text
AS '$libdir/dblink','dblink_open'
LANGUAGE C STRICT;

CREATE OR REPLACE FUNCTION dblink.open (text, text, text, boolean)
RETURNS text
AS '$libdir/dblink','dblink_open'
LANGUAGE C STRICT;

CREATE OR REPLACE FUNCTION dblink.fetch (text, int)
RETURNS setof record
AS '$libdir/dblink','dblink_fetch'
LANGUAGE C STRICT;

CREATE OR REPLACE FUNCTION dblink.fetch (text, int, boolean)
RETURNS setof record
AS '$libdir/dblink','dblink_fetch'
LANGUAGE C STRICT;

CREATE OR REPLACE FUNCTION dblink.fetch (text, text, int)
RETURNS setof record
AS '$libdir/dblink','dblink_fetch'
LANGUAGE C STRICT;

CREATE OR REPLACE FUNCTION dblink.fetch (text, text, int, boolean)
RETURNS setof record
AS '$libdir/dblink','dblink_fetch'
LANGUAGE C STRICT;

CREATE OR REPLACE FUNCTION dblink.close (text)
RETURNS text
AS '$libdir/dblink','dblink_close'
LANGUAGE C STRICT;

CREATE OR REPLACE FUNCTION dblink.close (text, boolean)
RETURNS text
AS '$libdir/dblink','dblink_close'
LANGUAGE C STRICT;

CREATE OR REPLACE FUNCTION dblink.close (text, text)
RETURNS text
AS '$libdir/dblink','dblink_close'
LANGUAGE C STRICT;

CREATE OR REPLACE FUNCTION dblink.close (text, text, boolean)
RETURNS text
AS '$libdir/dblink','dblink_close'
LANGUAGE C STRICT;

CREATE OR REPLACE FUNCTION dblink.query (text, text)
RETURNS setof record
AS '$libdir/dblink','dblink_record'
LANGUAGE C STRICT;

CREATE OR REPLACE FUNCTION dblink.query (text, text, boolean)
RETURNS setof record
AS '$libdir/dblink','dblink_record'
LANGUAGE C STRICT;

CREATE OR REPLACE FUNCTION dblink.query (text)
RETURNS setof record
AS '$libdir/dblink','dblink_record'
LANGUAGE C STRICT;

CREATE OR REPLACE FUNCTION dblink.query (text, boolean)
RETURNS setof record
AS '$libdir/dblink','dblink_record'
LANGUAGE C STRICT;

CREATE OR REPLACE FUNCTION dblink.exec (text, text)
RETURNS text
AS '$libdir/dblink','dblink_exec'
LANGUAGE C STRICT;

CREATE OR REPLACE FUNCTION dblink.exec (text, text, boolean)
RETURNS text
AS '$libdir/dblink','dblink_exec'
LANGUAGE C STRICT;

CREATE OR REPLACE FUNCTION dblink.exec (text)
RETURNS text
AS '$libdir/dblink','dblink_exec'
LANGUAGE C STRICT;

CREATE OR REPLACE FUNCTION dblink.exec (text,boolean)
RETURNS text
AS '$libdir/dblink','dblink_exec'
LANGUAGE C STRICT;

CREATE TYPE dblink.pkey_results AS (position int, colname text);

CREATE OR REPLACE FUNCTION dblink.get_pkey (text)
RETURNS setof dblink.pkey_results
AS '$libdir/dblink','dblink_get_pkey'
LANGUAGE C STRICT;

CREATE OR REPLACE FUNCTION dblink.build_sql_insert (text, int2vector, int, _text, _text)
RETURNS text
AS '$libdir/dblink','dblink_build_sql_insert'
LANGUAGE C STRICT;

CREATE OR REPLACE FUNCTION dblink.build_sql_delete (text, int2vector, int, _text)
RETURNS text
AS '$libdir/dblink','dblink_build_sql_delete'
LANGUAGE C STRICT;

CREATE OR REPLACE FUNCTION dblink.build_sql_update (text, int2vector, int, _text, _text)
RETURNS text
AS '$libdir/dblink','dblink_build_sql_update'
LANGUAGE C STRICT;

CREATE OR REPLACE FUNCTION dblink.current_query ()
RETURNS text
AS '$libdir/dblink','dblink_current_query'
LANGUAGE C;

CREATE OR REPLACE FUNCTION dblink.send_query(text, text)
RETURNS int4
AS '$libdir/dblink', 'dblink_send_query'
LANGUAGE C STRICT;

CREATE OR REPLACE FUNCTION dblink.is_busy(text)
RETURNS int4
AS '$libdir/dblink', 'dblink_is_busy'
LANGUAGE C STRICT;

CREATE OR REPLACE FUNCTION dblink.get_result(text)
RETURNS SETOF record
AS '$libdir/dblink', 'dblink_get_result'
LANGUAGE C STRICT;

CREATE OR REPLACE FUNCTION dblink.get_result(text, bool)
RETURNS SETOF record
AS '$libdir/dblink', 'dblink_get_result'
LANGUAGE C STRICT;

CREATE OR REPLACE FUNCTION dblink.get_connections()
RETURNS text[]
AS '$libdir/dblink', 'dblink_get_connections'
LANGUAGE C;

CREATE OR REPLACE FUNCTION dblink.cancel_query(text)
RETURNS text
AS '$libdir/dblink', 'dblink_cancel_query'
LANGUAGE C STRICT;

CREATE OR REPLACE FUNCTION dblink.error_message(text)
RETURNS text
AS '$libdir/dblink', 'dblink_error_message'
LANGUAGE C STRICT;
