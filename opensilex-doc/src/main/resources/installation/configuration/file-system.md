---
- file-system.md
- OpenSILEX
- Copyright © INRAE 2022
- Creation date: 14 February, 2022
- Contact: arnaud.charleroy@inrae.fr
---

# Service `fs` - file-system

- [Service `fs` - file-system](#service-fs---file-system)
  - [File System storage Definition](#file-system-storage-definition)
  - [Default storage system](#default-storage-system)
    - [Available custom implementations](#available-custom-implementations)
  - [Custom paths](#custom-paths)
  - [Full example configuration can be found here.](#full-example-configuration-can-be-found-here)

## File System storage Definition

Simple abstraction layer to access to any file storage system.

- Module: FileStorageModule
- Default Service class: org.opensilex.fs.FileStorageService
- Default Service class constructor parameter: org.opensilex.fs.TempFileSystemConnection

The filesystem abstraction system can be configured using several connection sources and the default must be set. `If not, temporary file system will be used.`

```yml
# Configuration for module: FileStorageModule (FileStorageConfig)
file-system:
  # File storage service (FileStorageService)
  fs:
    # Default file system storage (String)
    defaultFS: local
    config:
      # Map of file storage connection definition by identifier (Map<String,FileStorageConnection>)
      connections:
        local:
          # Service implementation class for: local (FileStorageConnection)
          implementation: org.opensilex.fs.local.LocalFileSystemConnection
          config:
            # Base path for file storage (String)
            basePath: ../../opensilex-data
```

## Default storage system

These sources will make it possible to change the default storage system.

Here, there is a gridfs used by default instead of local connection.

```yml
# ------------------------------------------------------------------------------
# Configuration for module: FileStorageModule (FileStorageConfig)
file-system:
  # File storage service (FileStorageService)
  fs:
    config:
      # Default file system storage (String)
      defaultFS: gridfs
      # Map of file storage connection definition by identifier (Map<String,FileStorageConnection>)
      connections:
        gridfs:
          # Service implementation class for: gridfs (FileStorageConnection)
          implementation: org.opensilex.fs.gridfs.GridFSConnection
          config:
            # MongoDB main host (String)
            host: localhost
            # MongoDB main host port (int)
            port: 8668
            database: opensilex
        local:
          # Service implementation class for: local (FileStorageConnection)
          implementation: org.opensilex.fs.local.LocalFileSystemConnection
          config:
            # Base path for file storage (String)
            basePath: ../../opensilex-data
```

### Available custom implementations

It is possible to defined differents implementations. Each implementation has a specific configuration.

We provide the following file system implementations:


| Implementation                                                          | Description                                                                                                             | Links                                                        |
|-------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------|
| `org.opensilex.fs.irods.IrodsFileSystemConnection`                      | [IRODS](https://irods.org/) Data Management Software                                                                    |                                                              |
| `org.opensilex.fs.gridfs.GridFSConnection`                              | [GridFS](https://docs.mongodb.com/manual/core/gridfs/) is a MongoDB JSON specification for storing and retrieving files |                                                              |
| `org.opensilex.fs.local.LocalFileSystemConnection`                      | Storage on local filesystem by using Java file access                                                                   |                                                              |
| `org.opensilex.fs.s3.S3FileStorageConnection`                           | [S3](http://aws.amazon.com/s3/) is a storage service from Amazon for storing and retrieving object and file             | `opensilex-doc/src/main/resources/file-systems/amazon_s3.md` |
| `org.opensilex.fs.s3.tranferManager.S3TransferManagerStorageConnection` | Another S3 implementation which use the S3 transfer manager API                                                         | `opensilex-doc/src/main/resources/file-systems/amazon_s3.md` |



Example of configuration with all implementation of file storage connection : 


```yml
# Map of file storage connection definition by identifier (Map<String,FileStorageConnection>)
connections:
  irods:
    # Service implementation class for: irods (FileStorageConnection)
    implementation: org.opensilex.fs.irods.IrodsFileSystemConnection
    config:
      # Base path for file storage (String)
      basePath: /FranceGrillesZone/home/fg-phenome/PHIS
  gridfs:
    # Service implementation class for: gridfs (FileStorageConnection)
    implementation: org.opensilex.fs.gridfs.GridFSConnection
    config:
      # MongoDB main host (String)
      host: localhost
      # MongoDB main host port (int)
      port: 8668
      database: opensilex
  local:
    # Service implementation class for: local (FileStorageConnection)
    implementation: org.opensilex.fs.local.LocalFileSystemConnection
    config:
      # Base path for file storage (String)
      basePath: ../../opensilex-data
  s3:
    # Service implementation class for: s3 (S3FileStorageConnection)
    implementation: org.opensilex.fs.s3.S3FileStorageConnection
    config:
      endpoint: s3-website.eu-west-3.amazonaws.com 
      region: eu-west-3
      bucket: opensilex-datafile-bucket
```

## Custom paths

It is possible to defined custom path according to services (Example : datafiles or documents).

Here there is an example with a custom connection which is used in order to store only datafile.

```yml
# ------------------------------------------------------------------------------
# Configuration for module: FileStorageModule (FileStorageConfig)
file-system:
  # File storage service (FileStorageService)
  fs:
    config:
      # Default file system storage (String)
      defaultFS: gridfs
      # Map of file storage connection definition by identifier (Map<String,FileStorageConnection>)
      connections:
        irods:
          # Service implementation class for: irods (FileStorageConnection)
          implementation: org.opensilex.fs.irods.IrodsFileSystemConnection
          config:
            # Base path for file storage (String)
            basePath: /FranceGrillesZone/home/fg-phenome/PHIS
        gridfs:
          # Service implementation class for: gridfs (FileStorageConnection)
          implementation: org.opensilex.fs.gridfs.GridFSConnection
          config:
            # MongoDB main host (String)
            host: localhost
            # MongoDB main host port (int)
            port: 8668
            database: opensilex
        local:
          # Service implementation class for: local (FileStorageConnection)
          implementation: org.opensilex.fs.local.LocalFileSystemConnection
          config:
            # Base path for file storage (String)
            basePath: ../../opensilex-data
    # Map of custom path connection management (Map<String,String>)
    customPath:
      datafile/: irods
```

## Full example configuration can be found here.

```yml
# ------------------------------------------------------------------------------
# Configuration for module: FileStorageModule (FileStorageConfig)
file-system:
  # File storage service (FileStorageService)
  fs:
    config:
      # Map of custom path connection management (Map<String,String>)
      customPath:
        datafile/: irods
      # Map of file storage connection definition by identifier (Map<String,FileStorageConnection>)
      connections:
        irods:
          # Service implementation class for: irods (FileStorageConnection)
          implementation: org.opensilex.fs.irods.IrodsFileSystemConnection
          config:
            # Base path for file storage (String)
            basePath: /FranceGrillesZone/home/fg-phenome/PHIS
        gridfs:
          # Service implementation class for: gridfs (FileStorageConnection)
          implementation: org.opensilex.fs.gridfs.GridFSConnection
          config:
            # MongoDB main host (String)
            host: localhost
            # MongoDB main host port (int)
            port: 8668
            # MongoDB other connection options (Map<String,String>)
            options:
            # MongoDB password (String)
            password:
            # MongoDB database (String)
            database: opensilex
            # MongoDB username (String)
            username:
            # MongoDB authentication database (String)
            authDB:
        local:
          # Service implementation class for: local (FileStorageConnection)
          implementation: org.opensilex.fs.local.LocalFileSystemConnection
          config:
            # Base path for file storage (String)
            basePath: ../../opensilex-data
      # Default file system storage (String)
      defaultFS: gridfs
```
