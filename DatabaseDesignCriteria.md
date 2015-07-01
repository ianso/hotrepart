

# Database design criteria #

Normal database partitioning can only be done on database schemas that have certain properties. For example, social networks generally have a large and highly interconnected graph at the centre of their data model, which cannot be easily partitioned. It follows that schemas such as these that can't be partitioned manually, can't be partitioned by this system either.

In addition to that, there are other criteria that have to be followed in order for the process to work correctly, and for the system to return correct results regardless of the number of partitions. Some of these principles are standard good practice, and some of them are a bit more demanding.

## Principle 1: the partition key has a wide and pseudo-random<sup>[0]</sup> distribution throughout the key-space, and joins do not cross partitions ##

The basics of database horizontal range-based partitioning are already well understood. Choose a column, and depending on the value of the column, send the row to a different database server. The client has a lookup of which column values, or range of values, go to which server.

The sample schema used in this project works along exactly the same principle. In this case, there is one table "items", which has a client-generated UUID as its primary key<sup>[1]</sup>. As a starting point, this table is already easily partitionable across multiple databases if we treat the UUID-space as a range:

| Range start | Range end | database |
|:------------|:----------|:---------|
| 00000000-0000-0000-0000-000000000000 | 7fffffff-ffff-ffff-ffff-fffffffffffe | dbname=items1 host=10.0.0.1 user=items |
| 7fffffff-ffff-ffff-ffff-ffffffffffff | ffffffff-ffff-ffff-ffff-ffffffffffff | dbname=items2 host=10.0.0.2 user=items |

Taking the UUID of this table as our partition key has several advantages: firstly, data is written to partitions on a pseudo-random basis, so load is spread evenly throughout the cluster. Secondly, no central counter is required, so many writes can be made to partitions independently.

Although demonstrated with a single table, it follows that a complete schema could be partitioned along the same lines, as long as the database table/view relations form a strictly hierarchical graph with this table at the root. This still allows joins, but only along the lines of the relation tree. This is a standard limitation of most partitioned databases.

## Principle 2: the database is accessed only via an API ##

It has in the past been considered best practise to allow access to a database via an API defined in stored procedures. This was for the same reason that defined interfaces are good elsewhere: you can hide the implementation details, change internals and shuffle data around without having any impact on the users of the API.

Database APIs have fallen out of fashion recently, partly because of ORMs such as Hibernate and ActiveRecord, and partly because business logic has moved away from the database and into intermediate tiers.

In any case, repartitioning logic is exactly the kind of stuff from which the user of a database should be protected, and so defining an API in this case frees the user of the schema from having to worry about where the data is stored and on how many computers.

## Principle 3: the target partition(s) for a read query are derivable from the read query itself ##

In the sample database schema, this is simply done by classifying all reads into two general categories: reads that are either for one partition, or for all partitions.

  * For reads that retrieve a single record by ID, the partition on which to call the read can be determined simply by looking up the partition that has a range containing that ID.
  * All other reads retrieve a series of records (or a single record) by other criteria, in this case the owner of each item. In this case, the read query should be run on all on all partitions, and the results collated before being returned to the client.

In this second case there are some gotchas that are standard to all partitioned databases:

Firstly, ordering, paging and result limiting generally has to be done at the application layer.

Secondly, collation queries (those using "group by" and "having" in SQL) generally return multiple results per query, which must then be re-collated. The [PL/Proxy FAQ](http://plproxy.projects.postgresql.org/doc/faq.html#toc16) contains some good general advice on this issue.

## Principle 4: any database writes are atomic, and executed on one partition only ##

What this means is that a transaction should never span multiple calls to the database API. One call to the API should be one transaction. Workarounds can be created using tables to store intermediate state, but if a transaction across API calls is needed, then a new API method that encompasses the needed calls should be considered.

The reason for this is that the cross-database transactions are not supported in PostgreSQL at present, and that cross-call transactions are not supported by PL/Proxy, as of version 2.0.8.

## Principle 5: database writes are idempotent ##

This means that a write should be only have an effect once, no matter how often it's executed. For SQL inserts, this is easy since attempting to insert the same record twice will cause a primary key violation.

For single-row writes this can be accomplished by adding a "version" column to the table, which is initialised to zero. Every update should then be executed as:

```
update items set column='value', version = version + 1 where items.id = XXXX and version = N
```

In this way, each update will only be applied once, no matter how many times it is executed. The version number should not be passed into an untrusted environment, for example as a hidden input on a web form. In this case, "update token" of a small sequence of pseudo-random bytes should be used instead, to prevent an attacker predicting version numbers.


---


Given that a schema meets all these criteria, then it should be possible to do online repartitioning. How this is done is explained in RepartitioningProcess.

###### Footnotes ######

**0** Pseudo-random in this document effectively means random. Truly random data would have to be extracted from background radiation or something, whereas a good hashing algorithm such as SHA-256 is fine for our purposes.

**1** In the implementation, the primary key is a pseudo-random 128-bit number, which for our purposes has practically, but not theoretically, the same properties as a UUID. A group of UUIDs generated on the same host may all have identical most significant bits depending on the UUID type. A real UUID may have to be reversed before the partition is chosen to swap the LSBs and the MSBs and maintain the same properties.