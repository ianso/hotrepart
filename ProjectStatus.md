

# 16th August 2009 #

The sample database scheme has one table against which four operations are called.

  * **create\_item** inserts a new row into the table.
  * **get\_item** retrieves a row from the table.
  * **update\_item** updates a row in the table.
  * **get\_items** retrieves many rows from the table, spread across all partitions.µ

This last method used to execute the query against all partitions in serial, due to a distortion in the way hotrepart used PL/Proxy. In order to fix this issue, a patch to PL/Proxy was required, which is now written. This patch is in the SVN repo, and was [posted to the mailing list here](http://lists.pgfoundry.org/pipermail/plproxy-users/2009-July/000186.html).

This method is not yet used by the load generating tool. Updating the tool would ordinarily be the next order of business. However, as laid out [in this blog post](http://ianso.blogspot.com/2009/08/you-were-saying-something-about-best.html) there's a scaling problem to be solved first. Some simulations need to be run to see if the proposed solution would work.

## bugspotting ##

There may be an issue in the sample database schema, in the code that reads from the 'write queue' used during database partitioning. The queue application may not correctly terminate during the first invokation, and instead continue to read until the repartitioning process is complete. TBC.