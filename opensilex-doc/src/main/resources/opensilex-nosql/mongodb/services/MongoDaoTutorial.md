---
title: MongoDB DAO and service usage documentation
tags:
    - MongoDB
    - Transaction management
description: This document explain how to use MongoDB based DAOs and describe transaction management for write operations
author: Renaud COLIN
date: 30/01/2024
---

<!-- TOC -->
* [Description](#description)
  * [Using Mongo based DAO](#using-mongo-based-dao)
  * [Read/Write operations and Session management](#readwrite-operations-and-session-management)
* [Transaction management : MongoDBServiceV2](#transaction-management--mongodbservicev2)
  * [Single document write](#single-document-write)
    * [Single document operation](#single-document-operation)
    * [Multiple write operation](#multiple-write-operation)
      * [Automatic transaction management](#automatic-transaction-management)
      * [Explicit transaction management](#explicit-transaction-management)
  * [Multiple document operation](#multiple-document-operation)
  * [Distributed transaction](#distributed-transaction)
* [Dao and service lifecycle](#dao-and-service-lifecycle)
  * [Service creation](#service-creation)
  * [DAO Creation](#dao-creation)
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
    * [Insert models (without explicit session management)](#insert-models-without-explicit-session-management)
    * [Insert models (with explicit session management)](#insert-models-with-explicit-session-management)
    * [Insert models and get results (with explicit session management)](#insert-models-and-get-results-with-explicit-session-management)
  * [Update](#update)
    * [Update a model](#update-a-model-)
    * [Update a model (with explicit session management)](#update-a-model-with-explicit-session-management)
  * [Delete](#delete)
    * [Delete a model by URI](#delete-a-model-by-uri)
    * [Delete a model (with explicit session management)](#delete-a-model-with-explicit-session-management)
  * [Delete many](#delete-many)
    * [Delete many models](#delete-many-models)
    * [Delete many models (with explicit session management)](#delete-many-models-with-explicit-session-management)
<!-- TOC -->

# Description

## Using Mongo based DAO

- MongoDB based Dao can perform Read (Get, Search) and Write (Create, Update, Delete)
  operations for a specific model inside a given collection.
- The standard implementation `MongoReadWriteDao` relies on `MongoDBServiceV2` for
  read/write and transaction management and uses Java Generics in order to specify the `MongoModel` and `MongoSearchFilter` to use
- The recommended way is to specialize this class or to simply use it for each domain/concept
  related to a given API

## Read/Write operations and Session management

- MongoDB allows the use of transactions in order to guarantee
the atomicity of write operations (The operation is either a success or a fail)
- With the MongoDB JAVA API, the use of transactions is performed
with the use of [ClientSession](https://mongodb.github.io/mongo-java-driver/3.6/javadoc/index.html?com/mongodb/session/ClientSession.html).
- Several ways for transaction handling are described in the next session

# Transaction management : MongoDBServiceV2

## Single document write

### Single document operation

- When creating, updating of deleting a single document, then
transaction managed is not mandatory since the operation is atomic.
- Operations which don't require transaction management (from` MongoWriteDao<T extends MongoModel, F extends MongoSearchFilter>` interface) : 
  - `create(T instance)`
  - `update(T instance)`
  - `delete(URI uri)`
### Multiple write operation

- If you want to group several single write operations inside an atomic operation (ex: doing write on several collections),
use a `ClientSession` and handle a transaction. In this case, there are two-ways :

#### Automatic transaction management

- Use `MongoDBServiceV2.runTransaction` and `MongoDBServiceV2.computeTransaction` methods 
  - This is the **recommended** way, since you don't have to handle start, committing, rollback of transaction and session start and close
  - You just have to define which operations perform by using the provided `ClientSession`
  
> **Example**

```java
mongodbServiceV2.runTransaction((session) -> {
    // use session here
    // the session is created and closed. Transaction is commited (if there are some write to commit) or rollback in case of error  
});

// or if you have to return some results (here an integer but any type can be returned)
int result = mongodbServiceV2.computeTransaction((session) -> {
  // use session here
  // perform operation and return some results
  return 1;
});
```

#### Explicit transaction management
- Explicit creation of the `ClientSession` with `MongoDBServiceV2.newSession()`
  - This is **not recommended** since you have to manually handle transaction and session lifecycle. Only use it for specific usage

See `MongoDBServiceV2.runTransaction` and  `MongoDBServiceV2.computeTransaction` for a deeper example of session and transaction management

## Multiple document operation

- For operations which can result in multiple document writes, the transaction handling 
is mandatory to ensure atomicity. In this case, there are two-ways :
  - If you don't provide a `ClientSession`, then the write operations (`create`, `deleteOnCriteria`) which deal with multiple documents, automatically creates one (if not provided), and uses it
to ensure transaction management
  - You create the `ClientSession` with `MongoDBServiceV2.runTransaction`/`MongoDBServiceV2.computeTransaction` and 
you provide the write operation(s) to the created session

## Distributed transaction

Use `SparqlMongoTransaction` when you need to perform operations on RDF and on MongoDB

```java
/* Assume objects are well initialized */
SPARQLService sparql;
MongoDBServiceV2 mongodb;

new SparqlMongoTransaction(sparql,mongodb).execute(session -> {
    // execute distributed transaction on triple store and mongodb dabatases
    // transaction management over the two database is handled (i.e. both operation are commited or rollback)
});
```

# Dao and service lifecycle

## Service creation

```java

class MyAPI {
  @Inject
  private MongoDBService mongodb;

  @GET
  public Response service{
      
    MongoDBServiceV2 mongodbV2 = mongodb.getServiceV2();
    // work with new service 
  }
}
```

## DAO Creation

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
- The search criteria `MongoSearchFilter#getRdfTypes()` is generic but may be not relevant to some MongoDB DAOs

#### Custom property filtering

> Considering the following MongoModel and the corresponding MongoSearchFilter which adds the search filter `name`

```java
public class DataModel extends MongoModel {
    private String name;
}

public class DataSearchFilter extends MongoSearchFilter {

    /* Match document with an equality filter on the "name" field */
    private String name;
}
```

and the following Dao which uses this `DataSearchFilter` as search filter.
This Dao must override the method `public List<Bson> getBsonFilters(F searchQuery)` like the following :

```java
import com.mongodb.client.model.Filters;
import org.apache.commons.lang3.StringUtils;

// The DataDao define how to handle the DataSearchFilter search criteria
// It's the responsibility of this class to use the correct filter on MongoCollection
public class DataDao extends MongoReadWriteDao<DataModel, DataSearchFilter> {

  @Override
  public List<Bson> getBsonFilters(DataSearchFilter searchQuery) {
    // Conserve semantic of basic field from search filter
    List<Bson> filters = super.getBsonFilters();

    // Define a new regex filter with the name
    if (!StringUtils.isEmpty(searchQuery.getName())) {
      filters.add(Filters.regex("name", searchQuery.getName()));
    }
    
    return filters;
  }
}
```

```java
/* Create filter */
MongoSearchFilter filter = new MongoSearchFilter().setName("opensilex");

/* Run search*/
ListWithPagination<DataModel> results = dataDao.search(filter);
```

This filter is used for several methods : 

- `count()`
- `search()`
- `deleteMany()`
- `distinct()`
- `lookupAggregation()`

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

> The search method provides a way to limit the fields to fetch from the Database.
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

> The search method allows the conversion of results from the database and to collect them inside the `ListWithPagination` usually returned.
> You can use this method when you need to iterate model over database results and convert each model to a DTO.
> In this case you don't have to collect the model list to re-create a new DTO result. This saves space and CPU time.

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
> is to perform a Stream-based search and to provide a projection which only includes the uri field.

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

> The `count(*)` methods have the same signature as `search(*)` methods, but instead return 
> the number of Documents matching the given search criteria. 

# Write

## Create

### Insert models (without explicit session management)

- When using `MongoReadWriteDao`, this dao relies on `MongoDBServiceV2#createAll()` method
- This method guarantees that the insertion of List of models is done with transaction management
- So the operation is atomic, all models are inserted or no models are inserted

```java
/* Assume models and dao are well initialized */
MongoReadWriteDao<DataModel,DataSearchFilter> dao;
List<DataModel> models;

/* Insert model and get insert results */
InsertManyResult insertResults = dao.create(models);
```

### Insert models (with explicit session management)

- If you want to use a particular transaction context you can pass the `ClientSession` to the `dao.create()` method
- This is useful if you want to group several operations in the same transaction
- This relies on `MongoDBServiceV2#runTransaction()` methods which handle transaction management for any write operation

```java
/* Assume objects are well initialized */
MongoReadWriteDao<DataModel, DataSearchFilter> dao;
List<DataModel> models, models2;
MongoDBServiceV2 mongoDBServiceV2;

/*  Insert the two list inside the same transaction. The session is auto-created and closed */
mongoDBServiceV2.runTransaction(session -> {
  InsertManyResult insertResults = dao.create(models, session);
  InsertManyResult insertResults2 = dao.create(models2, session);
});
```

### Insert models and get results (with explicit session management)

- If you want to use a particular transaction context you can pass the `ClientSession` to the `dao.create()` method
- This is useful if you want to group several operations in the same transaction
- You can also get a result after the operations are done (here the List of results from MongoDB)
- This relies on `MongoDBServiceV2#computeTransaction()` method which handles transaction management for any write operation

```java
// Assume objects are well initialized 
MongoReadWriteDao<DataModel, DataSearchFilter> dao;
List<DataModel> models, models2;
MongoDBServiceV2 mongoDBServiceV2;

/*  Insert the two list inside the same transaction. The session is auto-created and closed */
List<InsertManyResult> results = mongoDBServiceV2.computeTransaction(session -> {
  InsertManyResult insertResults = dao.create(models, session);
  InsertManyResult insertResults2 = dao.create(models2, session);
  return List.of(insertResults, insertResults2);
});
```

## Update

### Update a model 

```java
//  Assume objects are well initialized
DataModel model;

// Update model
dao.update(model);
```

### Update a model (with explicit session management)

> This case in only useful if you want to perform several operations in one transaction, otherwise there is no need for transaction
> management for only one document update

```java
//  Assume objects are well initialized
DataModel model;

mongoDBServiceV2.computeTransaction(session -> {
    // ... Other operation with session before
        
    // update model with session    
    dao.update(model,session)
}

```


## Delete

### Delete a model by URI

```java
//  Assume objects are well initialized
URI modelToDeleteURI;

// Delete model (throws NoSQLInvalidURIException if the model don't exist)
// No need to check it before it's done during the delete(URI) method 
dao.delete(modelToDeleteURI);
```


### Delete a model (with explicit session management)

> This case in only useful if you want to perform several operations in one transaction, otherwise there is no need for transaction
> management for only one document delete

```java
//  Assume objects are well initialized
URI modelToDeleteURI;

mongoDBServiceV2.computeTransaction(session -> {
        // ... Other operation with session before

        // update model with session    
        dao.delete(modelToDeleteURI,session)
});
```

## Delete many

### Delete many models

> - The example below reuses the DataSearchFilter example class used [previously](#custom-property-filtering)
> - By default, `MongoReadWriteDao` uses the `mongoDBServiceV2.deleteMany` which handles transaction management if no
> session is provided

```java
//  Delete all data with the name "test"
DataSearchFilter deleteFilter = new DataSearchFilter().setName("test");
dao.deleteMany(deleteFilter);
```

### Delete many models (with explicit session management)

```java
//  Delete all data with the name "test"
DataSearchFilter deleteFilter = new DataSearchFilter().setName("test");

mongoDBServiceV2.computeTransaction(session -> {
      // ... Other operation with session before
        
      dao.deleteMany(deleteFilter, session);  
});

```