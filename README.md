<!-- TOC -->
- [OpenSILEX Developer's installation](#opensilex-developers-installation)
- [Required software](#required-software)
- [Check your installed software](#check-your-installed-software)
- [Download sources](#download-sources)
- [Build project](#build-project)
- [Create opensilex command alias](#create-opensilex-command-alias)
  - [Linux](#linux)
  - [Windows](#windows)
- [Setup configuration](#setup-configuration)
- [Setup Databases with docker](#setup-databases-with-docker)
- [Initialize system data](#initialize-system-data)
  - [With Netbeans](#with-netbeans)
  - [With command line](#with-command-line)
- [Start OpenSILEX development server with Netbeans](#start-opensilex-development-server-with-netbeans)
  - [For web services only (with compiled Vue.js code)](#for-web-services-only-with-compiled-vuejs-code)
  - [For webservices and Vue.js hot reload server](#for-webservices-and-vuejs-hot-reload-server)
- [Start OpenSILEX development server with command line](#start-opensilex-development-server-with-command-line)
  - [For web services only (with compiled Vue.js code)](#for-web-services-only-with-compiled-vuejs-code-1)
  - [For webservices and Vue.js hot reload server](#for-webservices-and-vuejs-hot-reload-server-1)
- [Access to OpenSilex \& tools](#access-to-opensilex--tools)
  - [OpenSilex Application](#opensilex-application)
  - [RDF4J workbench](#rdf4j-workbench)
  - [MongoDB](#mongodb)
- [Generate documentation](#generate-documentation)
- [Other maven build profiles and options](#other-maven-build-profiles-and-options)
  - [Generate release](#generate-release)
  - [Skip unit and integration tests](#skip-unit-and-integration-tests)
  - [Skip Vue js build parts](#skip-vue-js-build-parts)
  - [Generate documentation](#generate-documentation-1)
  - [Generate documentation with security report audit](#generate-documentation-with-security-report-audit)
  - [Check javascript security issues](#check-javascript-security-issues)
  - [Special profile for Eclipse](#special-profile-for-eclipse)
  - [Default configuration example](#default-configuration-example)
<!-- TOC -->

OpenSILEX Developer's installation
=============================================

This repository contains source code for Phenotyping Hybrid Information System (PHIS) as an OpenSILEX instance

NOTE: OpenSILEX Production's installation available in : [production.md](./opensilex-doc/src/main/resources/installation/production.md)

# Required software

First you need to have this software installed :

- [Java JDK 11+](https://jdk.java.net/) (Our project is tested with JDK versions 11, 13 and 14)
- [Maven 3.6+](https://maven.apache.org/install.html)
- [Git 2.17.1+](https://git-scm.com/book/en/v2/Getting-Started-Installing-Git)
- [docker 19.03.1+](https://docs.docker.com/install/)
- [docker-compose 1.24.1+](https://docs.docker.com/compose/install/)

Note: the `<BASE_DIR>` variable referenced in this documentation is the root folder of your installation where your specific user must have read and write permissions.

# Check your installed software

Following commands should work from everywhere in your system without errors:

`java -version`

`mvn --version`

`git --version`

`docker --version`

`docker-compose --version`

# Download sources

```
cd <BASE_DIR>
git clone https://github.com/OpenSILEX/opensilex.git
cd opensilex
```

# Build project

```
cd <BASE_DIR>/opensilex
mvn install
```

# Create opensilex command alias

## Linux

Create the file `.bash_aliases` in your home directory if it doesn't exist.

Add this line to the file replacing the variable <BASE_DIR>:

```
alias opensilex=<BASE_DIR>/opensilex/opensilex-release/target/opensilex/opensilex.sh
```

Reload bash aliases and test it:

```
cd ~
source .bash_aliases
opensilex help
```

## Windows

Add the following directory to your PATH environment variable replacing the variable <BASE_DIR>:

```
path %PATH%;<BASE_DIR>\opensilex\opensilex-release\target\opensilex\
opensilex help
```

# Setup configuration

Edit `<BASE_DIR>/opensilex/opensilex-dev-tools/src/main/resources/config/opensilex.yml`

Be careful if you change the host and/or the port of databases as you will have to update the docker-compose configuration file accordingly.

Be sure to configure properly the read and write rights for your specific user on configured folders.

The only mandatory options to set up are:

- file-system.storageBasePath: Base directory for file storage

# Setup Databases with docker

On Linux OS, you should add your current user `<USER>` to the docker group in order to avoid using sudo:

```
sudo usermod -aG docker <USER>
```

You need to log out and log in again to make it work.

```
cd <BASE_DIR>
cd opensilex/opensilex-dev-tools/src/main/resources/docker
docker-compose up -d
```

Docker containers will be automatically started on your machine at startup.

You can change the "restart" parameter in "docker-compose.yml" file if you don't want this behavior,
but you will have to run the previous command after each restart manually.

# Initialize system data

## With Netbeans

Right-click on OpenSILEX project and select "Open Required Projects" --> "Open All Projects"

Then right-click on the class `org.opensilex.dev.Install` within the opensilex-dev-tools projet and select "run" or "debug".

If you want to reset all your databases, you can do the same with the class `org.opensilex.dev.InstallReset`.

## With command line

```
opensilex dev install
```

If you want to reset all your database use `-r` flag with the previous command.

# Start OpenSILEX development server with Netbeans

## For web services only (with compiled Vue.js code)

Right-click on the class `org.opensilex.dev.StartServer` within the opensilex-dev-tools projet and select "run" or "debug".

## For webservices and Vue.js hot reload server

Right-click on the class `org.opensilex.dev.StartServerWithFront`  within the opensilex-dev-tools projet and select "run" or "debug".

# Start OpenSILEX development server with command line

When using the command line you need to recompile all modules in order to have taken into account the changes in the Java source code (but not for Vue-JS code if using hot reload).

## For web services only (with compiled Vue.js code)

```
opensilex dev start --no-front-dev
```

## For webservices and Vue.js hot reload server

```
opensilex dev start
```

# Access to OpenSilex & tools

## OpenSilex Application

- OpenSilex Vue App is accessible at: [http://localhost:8666/](http://localhost:8666/)

- OpenSILEX API is accessible at: [http://localhost:8666/api-docs](http://localhost:8666/api-docs)

If you start the server with Vue.js, the hot reload server will tell you on which port it is accessible (probably 8080 depending of which is available).

Set the Default Super Admin user which will give you access to all web services is created with:

- login: admin@opensilex.org
- password: admin

## RDF4J workbench

RDF4J workbench is accessible by default at: [http://localhost:8667/rdf4j-workbench](http://localhost:8667/rdf4j-workbench)

Upon first connection, you must configure RDF4J server URL with this value: `http://opensilex-rdf4j:8080/rdf4j-server`

## MongoDB

MongoDB is accessible on port 8668

You could download MongoDB Compass to manage your database

# Generate documentation

```
cd <BASE_DIR>
mvn site -Pwith-test-report
```

All documentation will be available in <BASE_DIR>/opensilex-doc/src/main/doc directory.

# Other maven build profiles and options

## Generate release

```
mvn install -Drevision=X.Y.Z
```

A Zip file for revision will be available at <BASE_DIR>/opensilex-release/target/opensilex-release-X.Y.Z.zip

An uncompressed version is available in <BASE_DIR>/opensilex-release/target/opensilex-release-X.Y.Z/

## Skip unit and integration tests

It is not recommended, but it can be useful if you need to rebuild very often at some point in order to save time.

```
mvn install -DskipTests=true
```

## Skip Vue js build parts

This command can be useful if you work only on web services to speed up build.

```
mvn install -DskipFrontBuild
```

## Generate documentation

Make sure the environment variable `JAVA_HOME` is defined.

```
cd <BASE_DIR>/opensilex
mvn site -Pwith-test-report -DskipFrontBuild
```

## Generate documentation with security report audit

This build may be very very very long because it as to download a lot of stuff from internet (OWASP report).

It would be better to configure it periodically on an automated platform.

```
mvn site -Pwith-test-report -Pwith-security-check -DskipFrontBuild
```

## Check javascript security issues

```
mvn verify -DskipFrontAudit=false
```

## Special profile for Eclipse

If you are using Eclipse you may need to enable this profile to avoid build errors

```
mvn install -Pfor-eclipse
```

## Default configuration example

```yml

# ------------------------------------------------------------------------------
# Base system configuration OpenSilex (OpenSilexConfig)
system:
  # Default application language (String)
  defaultLanguage: en 
# ------------------------------------------------------------------------------
# Configuration for module: FileStorageModule (FileStorageConfig)
file-system:
  # File storage service (FileStorageService)
  fs:
    # Service implementation class for: fs (FileStorageService)
    implementation: org.opensilex.fs.service.FileStorageService
    config:
      # Default file system storage (String)
      defaultFS: gridfs
      # Map of custom path connection management (Map<String,String>)
      # customPath:
      #   datafile/: irods
      # Map of file storage connection definition by identifier (Map<String,FileStorageConnection>)
      connections:
        # irods:
        #   # Service implementation class for: irods (FileStorageConnection)
        #   implementation: org.opensilex.fs.irods.IrodsFileSystemConnection
        #   config:
        #     # Base path for file storage (String)
        #     basePath: /FranceGrillesZone/home/fg-phenome/PHIS
        gridfs:
          # Service implementation class for: gridfs (FileStorageConnection)
          implementation: org.opensilex.fs.gridfs.GridFSConnection
          config:
            # MongoDB main host (String)
            host: localhost
            # MongoDB main host port (int)
            port: 8668 
            timezone: UTC
            # MongoDB database (String)
            database: opensilex 
        local:
          # Service implementation class for: local (FileStorageConnection)
          implementation: org.opensilex.fs.local.LocalFileSystemConnection
          config:
            # Base path for file storage (String)
            basePath: ../../opensilex-data


# ------------------------------------------------------------------------------
# Configuration for module: NoSQLModule (NoSQLConfig)
big-data:
  # MongoDB data source (MongoDBService)
  mongodb:
    # Service implementation class for: mongodb (MongoDBService)
    implementation: org.opensilex.nosql.mongodb.MongoDBService
    config:
      # MongoDB main host (String)
      host: localhost
      # MongoDB main host port (int)
      port: 8668
      # timezone (String)
      timezone: UTC
      # MongoDB database (String)
      database: opensilex
 

# ------------------------------------------------------------------------------
# Configuration for module: SPARQLModule (SPARQLConfig)
ontologies:
  # Platform base URI (String)
  baseURI: http://opensilex.dev/
  # SPARQL data source (SPARQLServiceFactory)
  sparql:
    # Service implementation class for: sparql (SPARQLServiceFactory)
    implementation: org.opensilex.sparql.rdf4j.RDF4JServiceFactory
    config: 
      # RDF4J repository name (String)
      repository: opensilex
      # RDF4J Server URI (String)
      serverURI: http://localhost:8667/rdf4j-server/
  # Platform base URI alias (String)
  baseURIAlias: dev
 
# ------------------------------------------------------------------------------
# Configuration for module: CoreModule (CoreConfig)
core:
  # Activate access logs by user (boolean)
  enableLogs: false

  ```