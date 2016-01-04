+++
date = "2015-11-018T09:56:14Z"
title = "Changelog"
[menu.main]
  weight = 90
  pre = "<i class='fa fa-cog'></i>"
+++

## Changelog

Changes between released versions


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

