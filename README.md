# OpenSILEX
# Phenotyping Hybrid Information System (PHIS)

## Introduction

This repository contains source code for Phenotyping Hybrid Information System (PHIS) as an OpenSILEX instance

## Installation

### Prerequesite

In order to intall and run PHIS from source you must have the following software installed:

- **Git** at least version 1.8.2, see: <https://git-scm.com/book/en/v2/Getting-Started-Installing-Git>
- **Java** at least version 8, see: <https://www.oracle.com/technetwork/java/javase/overview/index.html>
- **Maven** at least version 3.5, see: <https://maven.apache.org/install.html>
- **Tomcat** at least version 8, see: <https://tomcat.apache.org/>

You will also need this databases available on your system :

- [MongoDB 4.0](https://docs.mongodb.com/manual/administration/install-on-linux/) *for noSQL databases*
- [PostgreSQL 9.5](https://www.postgresql.org/docs/9.5/release-9-5.html) *for SQL databases*
- [PostGIS 2.2](https://postgis.net/docs/postgis_installation.html#install_short_version) (minimal PostGIS version)
- [RDF4J 2.4.5](http://rdf4j.org/download/) *for triplestores*

### Get sources

Run this command to get all sources:

```
git clone --recursive https://github.com/OpenSILEX/opensilex-phis.git
```

### Configure

Edit src/main/config/dev/opensilex.yaml file with your favorite text editor and set the following parameters:


logger:
>    directory: *Path to logs file*
>
>    level: DEBUG
>
>    traceRequest: true

opensilex-core-rdf4j:
>    repository: *RDF4J repository name*
>
>    host: *Optional: default value to localhost*
>
>    port: *Optional: default value to 8080*
>
>    path: *Optional: rdf4j-server*

opensilex-core-mongo:
>    database: *MongoDb database name*
>
>    host: *Optional: default value to localhost*
>
>    port: *Optional: default value to 27017*  

phis-ws-pg:
>    database: *PostgreSQL database name*
>
>    username: *PostgreSQL user name*
>
>    password: *PostgreSQL user password*
>
>    host: *Optional: default value to localhost*
>
>    port: *Optional: default value to 5432*

phis-ws-service:
>    infrastructure: *Custom ontology suffix for generated uri*
>
>    uploadFileServerUsername: *Linux user name*
>
>    uploadFileServerPassword: *Linux user password*
>
>    uploadFileServerDirectory: *Path to file storage directory*
>
>    layerFileServerDirectory: *Path to layers storage directory*
>
>    layerFileServerAddress: *Uri to get layers*
>
>    uploadImageServerDirectory: *Path to image storage directory*
>
>    imageFileServerDirectory: *Uri to image storage directory*

### Build war file

Go into the root opensilex-phis folder and run this command to build the project:

```
mvn clean install
```

The resulting war file will be created: ./target/opensilex-ws-1.0.0-SNAPSHOT.war

You can also specify a version for the build with the following command line:

```
mvn clean install -Drevision=X.X.X
```

The resulting war file will be created: ./target/opensilex-ws-X.X.X.war


### Deploy

Simply copy the created war file to Tomcat "webapps" folder and access it threw the tomcat manager
