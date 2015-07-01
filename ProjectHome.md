

# Project description #

Hotrepart is a demonstrator project to illustrate a process for online repartitioning of a relational database.

The ultimate goal is to demonstrate auto-scaling of an RDBMS under production load on a utility computing platform, probably [Amazon EC2](http://aws.amazon.com/ec2/).

The current codebase does not approach production requirements, but nicely illustrates the principle of the process.

The project consists of a sample database schema and a load tester that puts the database under load, during which at any time the database can be repartitioned by calling a stored procedure via an SQL client. The database clients shouldn't notice anything except (hopefully) an improvement in performance once the process is complete.

Future goals of this project include:
  * Adapting the code to deploy new partitions on EC2 instances. Currently, the new partition runs on the same host as the existing database.
  * Building a plugin for monitoring software such as [Nagios](http://www.nagios.org/) to automatically trigger repartitioning under extended periods of high load.
  * Adapting the stress tester to run on multiple hosts and collate telemetry on a single console.

The demonstration is implemented in [PostgreSQL](http://postgresql.org/), but the mechanism can be applied to any RDBMS with the right plugins or additions.

The load tester is a small command-line tool built in Java 1.6 using Spring and built with Maven 2.

# How it works #

The demonstrated process cannot be applied to any SQL schema. Constraints common to most database partitioning schemas apply, along with some additional constraints; the [principles behind the design of a hot-repartionable database are documented here](DatabaseDesignCriteria.md).

Assuming a database schema follows the above principles, the [process of repartioning a database while under load is documented here](RepartitioningProcess.md).

The implementation of the above process in the real world is quite hackish, chiefly because we're doing things with tools that they weren't really built for. [A detailed description of the implementation can be found here](ImplementationDetails.md).

# Getting started #

[The latest version of hotrepart is v.0.9.0, which can be downloaded here](http://hotrepart.googlecode.com/files/hotrepart.0.9.0.zip). The zip file contains the SQL files to setup the database schema, and the stress tester.

Source for everything is in [the subversion repository](http://code.google.com/p/hotrepart/source/checkout), and is covered by the Apache License 2.0. Currently one of the functions is broken, so the DB can't be verified by the load tester, but the load test still works. Version 1 will be released once this is fixed.

[Installing the sample database schema is documented here](DatabaseInstallation.md). The database implementation uses PostgreSQL with the PL/Python and dblink modules, and [PL/Proxy](http://developer.skype.com/SkypeGarage/DbProjects/PlProxy).

[Running the load tester is documented here](LoadTesting.md). Java 1.6 or above is required. <a href='Hidden comment: 
For those interested in detailed documentation, [/site/javadoc/html Javadocs can be found here !!].'></a>


The sample schema has a simple API that consists of four stored procedures, so writing a stress tester in your favourite language or framework of choice shouldn't be complicated.

# Contributing #

First and foremost, any bug reports are gratefully accepted, although this is absolutely a spare-time project :-) Any general feedback or critique of the process would be interesting to hear too.

If you're interested in helping out, feel free to get in touch with me via the address ian dot sollars at gmail dot com. A [task list](TaskList.md) and todo list exists that I'm working my own way through, but there's room in the pool for everyone.