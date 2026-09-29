# Technical documentation : [`architecture`] `opensilex-fs` module

**Document history (please add a line when you edit the document)**

| Date       | Editor(s)        | OpenSILEX version | Comment           |
|------------|------------------|-------------------|-------------------|
| 2026-09-29 | Arnaud Charleroy | BUILD-SNAPSHOT    | Document creation |

> ⚠️ _WARNING_ : written from the `develop` branch (commit `6725c2912`) by reading the code; nothing was built or run.
> The S3 configuration is documented in [../file-systems/amazon_s3.md](../file-systems/amazon_s3.md); this page covers the
> module as a whole.

## Table of contents

<!-- TOC -->
* [Technical documentation : [`architecture`] `opensilex-fs` module](#technical-documentation--architecture-opensilex-fs-module)
  * [Table of contents](#table-of-contents)
  * [Definitions](#definitions)
  * [Solution](#solution)
    * [Module identity](#module-identity)
    * [Package map](#package-map)
    * [How a path reaches a connection](#how-a-path-reaches-a-connection)
    * [Implementations](#implementations)
    * [Who uses it](#who-uses-it)
  * [Tests](#tests)
  * [Limitations and improvements](#limitations-and-improvements)
  * [Documentation](#documentation)
<!-- TOC -->

## Definitions

- **Connection** : an implementation of `FileStorageConnection`, one storage back end (local disk, S3, GridFS, iRODS).
- **Default file system** : the connection selected by the `defaultFS` configuration key.

## Solution

### Module identity

- Module class: [FileStorageModule](../../../../../../opensilex-fs/src/main/java/org/opensilex/fs/FileStorageModule.java),
  config id `file-system`, configuration interface `FileStorageConfig` with one method returning a `FileStorageService`.
- It implements no extension interface and defines none.
- Maven dependencies: `opensilex-main` and `opensilex-nosql`. The dependency on `nosql` exists **only for GridFS**:
  `GridFSConnection` and its config import `MongoDBService` and `MongoDBConfig`.

### Package map

Root package `org.opensilex.fs`. Sixteen Java files, no main resources.

| Package                 | Files | Role                                                                                   |
|-------------------------|-------|----------------------------------------------------------------------------------------|
| `fs`                    | 2     | `FileStorageModule`, `FileStorageConfig`                                                |
| `fs/service`            | 3     | `FileStorageService`, `FileStorageServiceConfig`, `FileStorageConnection`               |
| `fs/local`              | 3     | `LocalFileSystemConnection`, `TempFileSystemConnection` and a config class              |
| `fs/s3`                 | 2     | `S3FileStorageConnection` (AWS SDK v2) and its config                                   |
| `fs/s3/transferManager` | 2     | `S3TransferManagerStorageConnection` and its config                                     |
| `fs/gridfs`             | 2     | `GridFSConnection` (MongoDB GridFS) and its config                                      |
| `fs/irods`              | 2     | `IrodsFileSystemConnection` and its config                                              |

### How a path reaches a connection

`FileStorageService` is the service other modules inject. It is declared with
`@ServiceDefaultDefinition(config = FileStorageServiceConfig.class)`, its default name is `fs`, and it dispatches a
request to a connection **by the prefix of the path**. `FileStorageServiceConfig` has three keys: `defaultFS`,
`connections` (a map from connection name to connection) and `customPath`.

`FileStorageConnection` extends `Service` and declares the operations every back end implements: read, write, existence
test, delete, directory creation and absolute path resolution.

Default behaviour: when `defaultFS` is blank, the service uses a `TempFileSystemConnection` (a temporary directory). When
`defaultFS` names a connection that is not in `connections`, it throws an error whose text mentions MongoDB, a leftover of
the GridFS origin.

### Implementations

| Class                                | Backed by                                            | Note                                                        |
|--------------------------------------|------------------------------------------------------|-------------------------------------------------------------|
| `LocalFileSystemConnection`          | a base directory on the local disk                   |                                                             |
| `TempFileSystemConnection`           | a temporary directory                                | The default; also used by the `test` profile                |
| `S3FileStorageConnection`            | Amazon S3 or a compatible service                    | Configuration in [amazon_s3.md](../file-systems/amazon_s3.md) |
| `S3TransferManagerStorageConnection` | S3 with the AWS transfer manager                     |                                                             |
| `GridFSConnection`                   | MongoDB GridFS                                       | The only reason for the `nosql` dependency                  |
| `IrodsFileSystemConnection`          | iRODS                                                | Shells out to the `iget` command; no iRODS library is used  |

All extend `BaseService` and implement `FileStorageConnection`.

### Who uses it

`opensilex-core` injects `FileStorageService` into its API classes and passes it to the DAOs that handle files (documents,
data files, experiments). `opensilex-migration` contains the two migrations that move document files between the local
file system and GridFS.

## Tests

- `service/FileStorageServiceTest` : a plain unit test of the service.
- `fs/FileStorageServiceTest` : an abstract class extending `AbstractUnitTest` that has no subclass anywhere.
- The S3 tests are annotated `@Ignore`.
- The GridFS test, `GridFSConnectionTest`, lives in the `opensilex-core` tests because it needs the embedded MongoDB base
  class defined there.

## Limitations and improvements

- A module named `fs` that depends on `nosql` only for one connection ties file storage to MongoDB at build time.
- The iRODS connection depends on the `iget` command being installed on the host.
- The module is loaded **before** `nosql` at runtime although it depends on it in Maven (see the
  [modules overview](../architecture/modules-overview.md#runtime-load-order)).

## Documentation

- [../file-systems/amazon_s3.md](../file-systems/amazon_s3.md) : S3 connection and its configuration.
- [../architecture/modules-overview.md](../architecture/modules-overview.md) : where this module sits.
- [../architecture/java-naming-conventions.md](../architecture/java-naming-conventions.md) : naming rules.
