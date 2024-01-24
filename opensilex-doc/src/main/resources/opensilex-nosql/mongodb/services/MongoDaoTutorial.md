# Code samples

## Object lifecycle

### Creation

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
import org.opensilex.core.data.dal.DataModel;
import org.opensilex.utils.ListWithPagination;

/* Create filter */
MongoSearchFilter filter = new MongoSearchFilter().setUri(SINGLETON_URI);

/* Run search */
ListWithPagination<DataModel> results = dataDao.search(filter);
```

#### Search models with an URI inside a given List

```java
import org.opensilex.core.data.dal.DataModel;
import org.opensilex.nosql.mongodb.dao.MongoSearchFilter;
import org.opensilex.utils.ListWithPagination;

import java.net.URI;
import java.util.List;

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
import org.opensilex.core.data.dal.DataModel;
import org.opensilex.nosql.mongodb.dao.MongoSearchFilter;
import org.opensilex.utils.ListWithPagination;

import java.net.URI;
import java.util.List;

/* Create filter */
Collection<URI> types = List.of(URI.create("vocabulary:TemperatureMeasure"), URI.create("vocabulary:HumidityMeasure"));
MongoSearchFilter filter = new MongoSearchFilter().setRdfTypes(types);

/* Run search */
ListWithPagination<DataModel> results = dataDao.search(filter);
```

**Note** : 
- The search criteria `MongoSearchFilter#getRdfTypes()` is generic but may be not relevant to some MongoDB DAO

### Session management

### Results filtering/projection

### Results conversion

## Count

The `count(*)` methods has the same signature as `search(*)` methods, but instead return 
the number of Document matching the given search criteria. 

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