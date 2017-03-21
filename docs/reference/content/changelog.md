+++
date = "2015-11-18T09:56:14Z"
title = "Changelog"
[menu.main]
  weight = 90
  pre = "<i class='fa fa-cog'></i>"
+++

## Changelog

Changes between released versions

### 2.0.0

  * Added SingleObservable trait and implicits for easy conversion and identification of Observables that return a single result. [SCALA-234](https://jira.mongodb.org/browse/SCALA-234)
  * MongoCollection methods now default to the collection type rather than Document. [SCALA-250](https://jira.mongodb.org/browse/SCALA-250) 

### 1.2.1

  * Removed erroneous scala-reflect dependency. [SCALA-288](https://jira.mongodb.org/browse/SCALA-288) 

### 1.2.0

  * Added support for maxStaleness for secondary reads. [SCALA-251](https://jira.mongodb.org/browse/SCALA-251) [SCALA-280](https://jira.mongodb.org/browse/SCALA-280)
  * Added support for MONGODB-X509 auth without username. [SCALA-279](https://jira.mongodb.org/browse/SCALA-279)
  * Added support for library authors to extend the handshake metadata. [SCALA-252](https://jira.mongodb.org/browse/SCALA-252)
  * Added support for the new Aggregation stages in 3.4 [SCALA-258](https://jira.mongodb.org/browse/SCALA-258)
  * Added support for views [SCALA-255](https://jira.mongodb.org/browse/SCALA-255)
  * Added Collation support [SCALA-249](https://jira.mongodb.org/browse/SCALA-249)
  * Added support for BsonDecimal128 [SCALA-241](https://jira.mongodb.org/browse/SCALA-241)
  * Added support for ReadConcern.LINEARIZABLE [SCALA-247](https://jira.mongodb.org/browse/SCALA-247)
  * Fixed bug where some connection string options were not applied [SCALA-253](https://jira.mongodb.org/browse/SCALA-253)
  * Added GridFS Support [SCALA-154](https://jira.mongodb.org/browse/SCALA-154)

### 1.1.1
  * Updated Mongodb Driver Async dependency to [3.2.2](https://jira.mongodb.org/browse/SCALA-237)
  * Ensure Observables can be subscribed to multiple times [SCALA-239](https://jira.mongodb.org/browse/SCALA-239)

### 1.1

  * Updated to support MongoDB 3.2.
    * Added support for [Document Validation](https://docs.mongodb.org/manual/release-notes/3.2/#document-validation).
    * Added support for [ReadConcern](https://docs.mongodb.org/manual/release-notes/3.2/#readconcern).
    * Added support for [partialIndexes](https://docs.mongodb.org/manual/release-notes/3.2/#partial-indexes).
    * Added new helpers for [Aggregation](https://docs.mongodb.org/manual/release-notes/3.2/#aggregation-framework-enhancements).
    * Added new helpers for [bitwise filters](https://docs.mongodb.org/manual/release-notes/3.2/#bit-test-query-operators).
    * Added support for version 3 [text indexes](https://docs.mongodb.org/manual/release-notes/3.2/#text-search-enhancements).
  * Updated Mongodb Driver Async dependency to [3.2.0](https://jira.mongodb.org/browse/SCALA-222)

[Full issue list](https://jira.mongodb.org/issues/?jql=fixVersion%20%3D%201.1%20AND%20project%20%3D%20SCALA).

### 1.0.1

  * Fixed missing scala codec registry issue when using custom MongoSettings
  * Removed unnecessary scala dependency 

[Full issue list](https://jira.mongodb.org/issues/?jql=fixVersion%20%3D%201.0.1%20AND%20project%20%3D%20SCALA).

### 1.0 

  * Initial release

