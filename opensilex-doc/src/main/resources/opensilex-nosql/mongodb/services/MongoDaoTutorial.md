---
title: MongoDB DAO usage documentation
tags:
    - MongoDB
author: Renaud COLIN
date: 30/01/2024
---

<!-- TOC -->
* [Description](#description)
  * [Read and write operations](#read-and-write-operations)
  * [Session management use cases](#session-management-use-cases)
  * [Specialization and reimplementation](#specialization-and-reimplementation)
* [Dao lifecycle](#dao-lifecycle)
  * [Creation](#creation)
* [Read](#read)
  * [Check existence](#check-existence)
  * [Check existence (with Session)](#check-existence-with-session)
  * [Get](#get)
  * [Get (With session)](#get-with-session)
  * [Search](#search)
    * [Search Filter](#search-filter)
    * [Filtering](#filtering)
      * [Search models with a URI inside a given List](#search-models-with-a-uri-inside-a-given-list)
      * [Search models with a given rdf:type](#search-models-with-a-given-rdftype)
      * [Custom property filtering](#custom-property-filtering)
    * [Session management](#session-management)
    * [Results filtering/projection](#results-filteringprojection)
      * [Retrieve only the uri and rdfType Fields](#retrieve-only-the-uri-and-rdftype-fields)
    * [Results conversion](#results-conversion)
    * [Stream search methods](#stream-search-methods)
      * [Get a Stream from filtered search](#get-a-stream-from-filtered-search)
      * [Combine Stream search and results projection](#combine-stream-search-and-results-projection)
  * [Count](#count)
* [Write](#write)
  * [Create](#create)
  * [Update](#update)
  * [Upsert](#upsert)
  * [Delete](#delete)
  * [Delete many](#delete-many)
* [Transaction management](#transaction-management)
  * [Distributed transaction](#distributed-transaction)
* [Parallelism](#parallelism)
  * [Read](#read-1)
  * [Write](#write-1)
<!-- TOC -->

# Description

## Read and write operations

## Session management use cases

## Specialization and reimplementation

# Dao lifecycle

## Creation

> Initialization of a Dao for handling `DataModel` inside the `data` collection

```java
MongoReadWriteDao<DataModel,DataSearchFilter> dao = new MongoReadWriteDao(mongodb, DataModel.class, "data", "data");

/* with Java >= 11 */
var dataDao = new MongoReadWriteDao(mongodb, DataModel.class, "data", "data");
```

# Read

## Check existence

> Check if a `DataModel` with the URI `opensilex:data_1` exists

```java
boolean exists = dataDao.exists(URI.create("opensilex:data_1"));
```

## Check existence (with Session)

> Check if a `DataModel` with the URI `opensilex:data_1` exists inside a given session context

```java
boolean exists = dataDao.exists(session, URI.create("opensilex:data_1"));
```

## Get

> Get a `DataModel` with the URI `opensilex:data_1`

```java
DataModel data = dataDao.get(URI.create("opensilex:data_1"));
```

## Get (With session)

> Get a `DataModel` with the URI `opensilex:data_1` inside a given session context

```java
DataModel data = dataDao.get(session, URI.create("opensilex:data_1"));
```

## Search

### Search Filter

### Filtering

```java
/* Create filter */
MongoSearchFilter filter = new MongoSearchFilter().setUri(SINGLETON_URI);

/* Run search */
ListWithPagination<DataModel> results = dataDao.search(filter);
```

#### Search models with a URI inside a given List

```java
/* Create filter */
Collection<URI> uris = List.of(URI.create("opensilex:data_1"), URI.create("opensilex:data_2"));
MongoSearchFilter filter = new MongoSearchFilter().setIncludedUris(uris);

/* Run search */
ListWithPagination<DataModel> results = dataDao.search(filter);
```
#### Search models with a given rdf:type

> The example below consider the two types: `vocabulary:TemperatureMeasure`, `vocabulary:HumidityMeasure` 
> as two subClass of `vocabulary:Data` type

```java
/* Create filter */
Collection<URI> types = List.of(URI.create("vocabulary:TemperatureMeasure"), URI.create("vocabulary:HumidityMeasure"));
MongoSearchFilter filter = new MongoSearchFilter().setRdfTypes(types);

/* Run search */
ListWithPagination<DataModel> results = dataDao.search(filter);
```

**Note** :
- The search criteria `MongoSearchFilter#getRdfTypes()` is generic but may be not relevant to some MongoDB DAO

#### Custom property filtering

> Considering the following MongoModel and the corresponding MongoSearchFilter which add the search filter `name`

```java
public class DataModel extends MongoModel {
    private String name;
}

public class DataSearchFilter extends MongoSearchFilter {

    /* Match document with an equality filter on the "name" field */
    private String name;
}


```

and the following Dao which use this `DataSearchFilter` as search filter

```java
// The DataDao define how to handle the DataSearchFilter search criteria
// It's the responsibility of this class to use the correct filter on MongoCollection
public class DataDao extends MongoReadWriteDao<DataModel, DataSearchFilter> {
}
```

```java
/* Create filter */
MongoSearchFilter filter = new MongoSearchFilter().setName("opensilex");

/* Run search*/
ListWithPagination<DataModel> results = dataDao.search(filter);
```

### Session management

> Each `MongoReadWriteDao#search` method can use a custom `ClientSession` during search. 
> See [Usage of session management](#session-management-use-cases)

```java
/* Session already created */
ClientSession session;

/* Create filter */
Collection<URI> uris = List.of(URI.create("opensilex:data_1"), URI.create("opensilex:data_2"));
MongoSearchFilter filter = new MongoSearchFilter().setIncludedUris(uris);

/* Run search */
ListWithPagination<DataModel> results = dataDao.search(session, filter, null);
```

### Results filtering/projection

> Search method provide a way to limit to field to fetch from the Database.
> This is useful is you known that only a subset of Document fields is required.
> It offers better performance due to a lower Network bandwidth usage and less CPU/RAM usage on client side.

#### Retrieve only the uri and rdfType Fields

```java
import com.mongodb.client.model.Projections;

/* Create projection on uri and rdfType field */
Bson projection = Projections.fields(
        Projections.include(MongoTestModel.URI_FIELD, MongoTestModel.TYPE_FIELD)
);

/* Create filter */
Collection<URI> uris = List.of(URI.create("opensilex:data_1"), URI.create("opensilex:data_2"));
MongoSearchFilter filter = new MongoSearchFilter().setIncludedUris(uris);

/* Run search, only DataModel.getUri() and DataModel.getRdfType() are not null */
ListWithPagination<DataModel> results = dataDao.search(null, filter, projection);
```

### Results conversion

> Search method allow to convert results from database and to collect it inside the `ListWithPagination` usually returned.
> You can use this method when you need to iterate model over database results and convert each model to a DTO.
> In this case you don't have to collect the model list and to re-create a new DTO result. This save space and CPU time.

```java
/* Create filter */
Collection<URI> uris = List.of(URI.create("opensilex:data_1"), URI.create("opensilex:data_2"));
MongoSearchFilter filter = new MongoSearchFilter().setIncludedUris(uris);

/* Directly create a List of DataGetDTO : DataModel -> DataGetDTO is performed on-the-fly with the DataGetDTO::fromModel Function */
ListWithPagination<DataGetDTO> results = dao.search(null, filter, null, DataGetDTO::fromModel);
```

### Stream search methods

#### Get a Stream from filtered search

```java
Collection<URI> uris = List.of(URI.create("opensilex:data_1"), URI.create("opensilex:data_2"));
MongoSearchFilter filter = new MongoSearchFilter().setIncludedUris(uris);

/* StreamWithPagination includes the Stream of result and keep information about pagination (including count and total) */
StreamWithPagination<DataModel> results = dao.searchAsStream(filter);

/* Get wrapped stream, extract model URI and collect it */
List<URI> resultUris = results.getSource().map(DataModel::getUri).collect(Collectors.toList());

/* Or directly lookup on it (Re-run search in the example since previous Stream has been consumed) */
List<URI> resultUris2 = new ArrayList<>(results.getPageSize());
dao.searchAsStream(filter).forEach(model -> resultUris2.add(model.getUri()));
```

#### Combine Stream search and results projection

> If we only want to extract a List of URIs (or any subset of field) of document which match some criteria, the best way
> is to perform a Stream-based search and to provide a projection which only include the uri field.

```java
/* Create projection on uri field */
Bson projection = Projections.fields(Projections.include(MongoTestModel.URI_FIELD));

Collection<URI> uris = List.of(URI.create("opensilex:data_1"), URI.create("opensilex:data_2"));
MongoSearchFilter filter = new MongoSearchFilter().setIncludedUris(uris);

/* StreamWithPagination includes the Stream of result and keep information about pagination (including count and total) */
StreamWithPagination<DataModel> results = dao.searchAsStream(null, filter, projection);

/* Get wrapped stream, extract model URI and collect it. Only the uri field has been fetched from database */
List<URI> resultUris = results.getSource().map(DataModel::getUri).collect(Collectors.toList());
```

## Count

> The `count(*)` methods has the same signature as `search(*)` methods, but instead return 
> the number of Document matching the given search criteria. 

# Write

## Create

## Update

## Upsert

## Delete

## Delete many

# Transaction management

## Distributed transaction

# Parallelism

## Read

## Write