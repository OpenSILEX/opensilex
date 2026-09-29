# Technical documentation : [`architecture`] `opensilex-nosql` module

**Document history (please add a line when you edit the document)**

| Date       | Editor(s)        | OpenSILEX version | Comment           |
|------------|------------------|-------------------|-------------------|
| 2026-09-29 | Arnaud Charleroy | BUILD-SNAPSHOT    | Document creation |

> ⚠️ _WARNING_ : written from the `develop` branch (commit `6725c2912`) by reading the code; nothing was built or run.
> This page describes the module as a whole. The DAO API itself is covered by
> [MongoDao.md](./mongodb/services/MongoDao.md) and [MongoDaoTutorial.md](./mongodb/services/MongoDaoTutorial.md), and the
> authentication mechanisms by [MongoDbAuthentication.md](./mongodb/security/MongoDbAuthentication.md).

## Table of contents

<!-- TOC -->
* [Technical documentation : [`architecture`] `opensilex-nosql` module](#technical-documentation--architecture-opensilex-nosql-module)
  * [Table of contents](#table-of-contents)
  * [Definitions](#definitions)
  * [Solution](#solution)
    * [Module identity](#module-identity)
    * [Why it depends on opensilex-sparql](#why-it-depends-on-opensilex-sparql)
    * [Package map](#package-map)
    * [Two generations of the MongoDB service](#two-generations-of-the-mongodb-service)
    * [DAO base classes](#dao-base-classes)
    * [Transactions spanning both stores](#transactions-spanning-both-stores)
    * [Authentication and configuration](#authentication-and-configuration)
  * [Tests](#tests)
  * [Limitations and improvements](#limitations-and-improvements)
  * [Documentation](#documentation)
<!-- TOC -->

## Definitions

- **V1 / V2 service** : the two coexisting MongoDB services, `MongoDBService` and `MongoDBServiceV2`.
- **DAO** : a data access object built on `MongoReadWriteDao`, taking a `MongoDBServiceV2`.

## Solution

### Module identity

- Module class: [NoSQLModule](../../../../../../opensilex-nosql/src/main/java/org/opensilex/nosql/NoSQLModule.java),
  config id `big-data`, configuration interface `NoSQLConfig` with one method returning a `MongoDBService`.
- It implements no extension interface and defines none.
- `install(reset)` drops the database or checks the replica set; `check()` does an insert, count and drop round trip;
  `setup()` registers `MongoDBServiceV2` under the name `mongodb2`, reusing the configuration path `big-data.mongodb.config`.
- Maven dependencies: `opensilex-main` and `opensilex-sparql`.

### Why it depends on opensilex-sparql

The dependency is real, not incidental: `URICodec` uses `SPARQLDeserializers`; `MongoSearchFilter` extends the sparql
`SearchFilter`; `MongoDBService.getGenerationPrefixURI` calls `SPARQLModule`; and `MetaDataDao` works on
`SPARQLResourceModel`. A MongoDB document is identified by the same URIs as an RDF resource.

### Package map

Root package `org.opensilex.nosql`. Java files in `src/main/java`.

| Package                     | Files | Role                                                                                       |
|-----------------------------|-------|--------------------------------------------------------------------------------------------|
| `nosql`                     | 2     | `NoSQLModule`, `NoSQLConfig`                                                                |
| `nosql/distributed`         | 1     | `SparqlMongoTransaction`                                                                    |
| `nosql/exceptions`          | 6     | Exceptions of the module                                                                    |
| `nosql/mongodb`             | 3     | `MongoDBService` (V1), `MongoDBConfig` and one more class                                   |
| `nosql/mongodb/auth`        | 2 + 7 | `MongoAuthenticationService` and the `password/` implementations                            |
| `nosql/mongodb/codec`       | 3     | `URICodec`, `ObjectCodec`, `ZonedDateTimeCodec`                                             |
| `nosql/mongodb/dao`         | 5     | `MongoReadDao`, `MongoWriteDao` (interfaces), `MongoReadWriteDao`, `MongoSearchQuery`, `MongoSearchFilter` |
| `nosql/mongodb/logging`     | 1     | Logging support                                                                             |
| `nosql/mongodb/metadata`    | 4     | `MetaDataDao`, `MetaDataDaoV2`, `MetaDataModel`                                             |
| `nosql/mongodb/service/v2`  | 1     | `MongoDBServiceV2`                                                                          |

The base model is `MongoModel` (uri, rdf type, publisher, dates); it implements `ClassURIGenerator` so that documents get
URIs like RDF resources.

### Two generations of the MongoDB service

Both are registered and neither is deprecated.

| Service                              | Service name | Size      | What it offers                                                             |
|--------------------------------------|--------------|-----------|----------------------------------------------------------------------------|
| `MongoDBService` (V1)                | `mongodb`    | ~700 lines | CRUD, search and sessions used directly by V1 DAOs                          |
| `MongoDBServiceV2`                   | `mongodb2`   | one class | `runTransaction` / `computeTransaction`, and a registry of indexes (`registerIndexes`, `createIndexes`) |

`opensilex-core` still has DAOs on both generations; see the [core module page](../opensilex-core/module-architecture.md#mongodb-and-sparql-storage).

### DAO base classes

`MongoReadWriteDao` (about 1,000 lines) is the generic DAO. It is parameterised by a model extending `MongoModel` and a
filter extending `MongoSearchFilter`, and takes a `MongoDBServiceV2`. `MetaDataDaoV2` is the variant for metadata
collections attached to a resource.

### Transactions spanning both stores

`SparqlMongoTransaction.execute` starts a SPARQL transaction, runs the operation inside a MongoDB transaction
(`MongoDBServiceV2.computeThrowingTransaction`, which hands the operation a `ClientSession`), then commits the SPARQL
transaction. Any exception rolls back the SPARQL transaction and is rethrown, optionally translated by a custom exception
mapping (`customException`).

The two stores are therefore **not committed atomically**: the MongoDB transaction ends when the operation returns, and
nothing undoes it if the SPARQL commit itself then fails.

It is used by `opensilex-core` (data import, scientific objects, facilities, sites, germplasm) and by the
[migrations](../opensilex-migration/module-architecture.md).

### Authentication and configuration

`MongoDBConfig` keys: `host`, `port`, `database`, `authentication`, `options`, `timezone`, `connectTimeoutMs`,
`serverSelectionTimeoutMs`, `readTimeoutMs`, `maxCountLimit`, `maxPageCountLimit`. The `authentication` value is a
`MongoAuthenticationService`, with three implementations in `mongodb/auth/password`: `PasswordMongoAuthentication`,
`CredentialsFileMongoAuthentication` and `EncryptedCredentialsFileMongoAuthentication`.

Under the `test` profile the configuration comes from `opensilex-main` (`config/test/opensilex.yml`): a MongoDB at
`127.0.0.1:28018`.

## Tests

- `MongoDBServiceTest` : abstract, extends `AbstractUnitTest`.
- `EmbedMongoClient` : starts an embedded MongoDB (flapdoodle, MongoDB 7.0.12) as a replica set `rs0` on port 28018.
- `MongoReadWriteDaoTest` : about 800 lines exercising the generic DAO.

The base class that starts the embedded MongoDB for integration tests, `AbstractMongoIntegrationTest`, is in the
`opensilex-core` tests, not here.

## Limitations and improvements

- `MongoDBServiceV2` is shut down twice: by `NoSQLModule.shutdown` and by the generic service shutdown loop.
- The Javadoc of `MongoDBServiceV2` and `MongoReadWriteDao` points to a documentation path
  (`opensilex-doc/src/main/resources/opensilex-nosql/...`) that does not exist; the documents are under
  `technical-documentation/opensilex-nosql/`.
- The module directory contains a `nbactions.xml` (NetBeans) that nothing uses.
- V1 and V2 overlap in functionality, and `opensilex-core` still contains DAOs on both generations.

## Documentation

- [MongoDao.md](./mongodb/services/MongoDao.md), [MongoDaoTutorial.md](./mongodb/services/MongoDaoTutorial.md) : the DAO API.
- [MongoDbAuthentication.md](./mongodb/security/MongoDbAuthentication.md) : authentication.
- [../databases/mongodb.md](../databases/mongodb.md) : MongoDB installation and configuration.
- [../architecture/modules-overview.md](../architecture/modules-overview.md) : where this module sits.
