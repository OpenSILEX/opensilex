OpenSilex  architecture
=======================

This document present the general technical architecture of OpenSilex.

## Key words

In this document we will use the key words "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL NOT", "SHOULD", "SHOULD NOT", 
"RECOMMENDED", "MAY", and "OPTIONAL" in this document are to be interpreted as described in RFC 2119, see definitions below.

1. MUST This word, or the terms "REQUIRED" or "SHALL", mean that the
   definition is an absolute requirement of the specification.

2. MUST NOT This phrase, or the phrase "SHALL NOT", mean that the
   definition is an absolute prohibition of the specification.

3. SHOULD This word, or the adjective "RECOMMENDED", mean that there
   may exist valid reasons in particular circumstances to ignore a
   particular item, but the full implications must be understood and
   carefully weighed before choosing a different course.

4. SHOULD NOT This phrase, or the phrase "NOT RECOMMENDED" mean that
   there may exist valid reasons in particular circumstances when the
   particular behavior is acceptable or even useful, but the full
   implications should be understood and the case carefully weighed
   before implementing any behavior described with this label.

5. MAY This word, or the adjective "OPTIONAL", mean that an item is
   truly optional. 

## Best practices references

- [SOLID](https://en.wikipedia.org/wiki/SOLID)
- [You aren't gonna need it](https://en.wikipedia.org/wiki/You_aren%27t_gonna_need_it)

## layers of code

OpenSilex application is architecured with multiple ordered `layer` of code.

Each `layer` has is own responsabilty according to `SOLID` principles.

A lower level `layer` MUST NOT have any reference to a higher level.

A highest `layer` application level MAY access lower levels at any depth level.

 The following schema show Opensilex `layers` from the highest to the lowest level.

`↓` sign means the highest `layer` "has access to" the lowest `layer`.

```
+------------------------- OpenSilex -------------------------+
+                                                             +
+  +----- FRONT WEB APP -----+   +---------- CLI ----------+  +
+  +-------------------------+   +-------------------------+  + 
+               ↓                             ↓               +
+  +---------------------- REST API -----------------------+  +
+  +-------------------------------------------------------+  +
+                              ↓                              +
+  +------------------ DATA ACCESS LAYER ------------------+  +
+  +-------------------------------------------------------+  +
+                              ↓                              +
+  +---------------------- SERVICES -----------------------+  +
+  +-------------------------------------------------------+  +
+                              ↓                              +
+  +---------------------- LIBRARIES ----------------------+  +
+  +-------------------------------------------------------+  +
+                                                             +
+-------------------------------------------------------------+
             ↓                 ↓                   ↓
     +---------------+  +---------------+  +---------------+ 
     +     RDF4J     +  +    MongoDB    +  +  File System  +
     +---------------+  +---------------+  +---------------+
```

OpenSilex rely on external data sources provided by:

- `RDF4J` or any other triplestore supporting SPARQL request language for semantic data storage
- `MongoDB` for big data storage
- `File system` for data file storage

This tools are outside of the scope of this documentation but shown above for global understanding.

## Definitions

OpenSilex code is based on an extensible modular architecture.

OpenSilex is build as a `Maven multi-module` project.

This means it's a list of `Maven module` related together with dependency relations.

Each `OpenSilex module` (called `module` in this document) is a `Maven module` with a specific architecture described in this document.

Some `Maven module` in OpenSilex are not `OpenSilex module` and exists just for technical or practical reasons see [here](./maven.md) for more information on Maven integration with OpenSilex.

Each `module` MUST respect the general application layers organization principles.

Each `module` MUST contains exactly one class implementing `OpenSilexModule` which will be called `module class`.

Each `module` MAY be extensible by providing any number of module `extension points`.

Each `extension point` is custom code logic using an `extension interface` extending `OpenSilexExtension`.

Each `extension interface` MAY be implemented by any `module` acting as an `extension`.

Note: This extension mechanism is freely inspired from [Eclipse extension plugin](https://wiki.eclipse.org/FAQ_What_are_extensions_and_extension_points%3F ) (with `Eclispe plugins` equivalent to `OpenSilex modules`), see also our [documentation](./module.md) for more informations on modules.

Each `module` MAY also implement many application `layers`.

A `layer` implementation is called a `feature` in this document.

From lowest to hightest `layer`, a `feature` could be:

LIBRARIES:

- The integration of an external libray.

SERVICES:

- A `service` class to define any configurable behavior for the application.
- A `service` class implementation for anoter `service`.
- A `service connection` interface to be used by other `services` to switch implementations by configuration

DATA ACCESS LAYER:

- A `model` class to describe application high level concept.
- A `data Access Object (DAO)` class to manipulate application `model` classes.

REST API:

- A `data Transfert Object (DTO)` class to represent REST JSON message serialization.
- An `API` class to add REST entry points using `DTO` as input and output objects.

CLI:

- A `command` class implementation to provide new commands.

FRONT WEB APP

- A new `web application` at a given path of the `application server`.
- A `web component` for an existing `web application`.

A `module` MAY also provide `extensions` for other modules to interact with it.

A `module` SHOULD NOT provide more than one `extension`.

A `module` MAY implement as many extensions as it needs.

Details on this differents concepts are provided ? // TODO: doc links

## Main module

There is one module slightly different from other modules: `opensilex-main`.

The purpose of this module is to provide main OpenSilex application entry point.

It contains OpenSilex bootstrap classes to provide:
- Module extension management
- Global YAML configuration extensible by module
- Automatic module dependency resolution
- Automatic module services configuration and loading
- Extensible CLI with autogenerated documentation and help
- Application updates (WORK IN PROGRESS)

The `Maven build` for this `Maven module` produce a special JAR embeding all dependencies used by `built-in modules`.

This main `Maven module` also contains an `OpenSilex module` implemented by `org.opensilex.server.ServerModule`

This schema represent all `features` by `layers` implemented by `ServerModule`.

It also present the provided `extensions` and  implemented `extension points`of the module.

For more information on this module see [documentation](./main.md)

```
+-------------- opensilex-main - ServerModule ----------------+
+                                                             +
+  +---- FRONT WEB APP ------+   +--------- CLI -----------+  +
+  + Swagger-UI              +   + Add system commands     +  +
+  +   Setup on "/"          +   + Add server commands     +  +
+  +-------------------------+   +-------------------------+  +
+               ↓                             ↓               +
+  +---------------------- REST API -----------------------+  +
+  + Initialize all REST API mechanisms                    +  +
+  + Provide generic DTOs and JSON Response                +  +
+  + Provide default REST error handling                   +  +
+  + Provide generic validation annotations for API params +  +
+  +-------------------------------------------------------+  +
+                              ↓                              +
+  +---------------------- LIBRARIES ----------------------+  +
+  + Integrate Tomcat Server                               +  +
+  + Integrate Jersey REST Framework                       +  +
+  +-------------------------------------------------------+  +
+                                                             +
+++++++++++++++++++ Provided extensions +++++++++++++++++++++++
+ ServerExtension:                                            +
+   allow other modules to interact with Tomcat Server        +
+ APIExtension:                                               +
+   allow other modules to interact with Jersey & Swagger     +
+++++++++++++++ Implemented extension points ++++++++++++++++++
+ APIExtension:                                               +
+   register Swagger API packages                             +
+   register OpenSilex validation annotations                 +
+   register OpenSilex generic serialization objects          +
+-------------------------------------------------------------+
```

## Built-in modules

A set of modules are built in OpenSilex and loaded by default:

This list describe by their loading order:

- `opensilex-fs`: `Services` to interact with file system
- `opensilex-sparql`: `Services` to interact with SPARQL triplestores
- `opensilex-nosql`: `Services` to interact with NoSQL databases
- `opensilex-security`: Add security `features` to REST application
- `opensilex-core`: Add common `models` and `API` for scientific research data management
- `opensilex-phis`: Add specific concept for phenotyping and scientific vegetal research
- `opensilex-front`: Add an extensible Vue.js interface


### opensilex-fs

This module simply provide a `service` to interact with file systems.

`LocalFileSystemConnection` is used as the default implementation of `FileStorageService`.

```
+------------ opensilex-fs - FileStorageModule ---------------+
+                                                             +
+  +---------------------- SERVICES -----------------------+  +
+  + FileStorageService:                                   +  +
+  +   declare FileStorageConnection service connection    +  +
+  +   provide LocalFileSystemConnection implementation    +  +
+  +-------------------------------------------------------+  +
+                              ↓                              +
+  +---------------------- LIBRARIES ----------------------+  +
+  + Integrate Apache FileUtils                            +  +
+  +-------------------------------------------------------+  +
+                                                             +
+-------------------------------------------------------------+
```

### opensilex-sparql

This module provide `services` to interact with a triplestore compatible with SPARQL.

It use annotations on `models` classes and methods to automatically map Java Object with RDF triples.

For more information on this module see [documentation](./sparql.md)

```
+------------- opensilex-sparql - SPARQLModule ---------------+
+                                                             +
+                                +---------- CLI ----------+  +
+                                + Add sparql commands     +  +
+                                +-------------------------+  + 
+                                             ↓               +
+  +---------------------- REST API -----------------------+  +
+  + Provide generic DTOs corresponding to SPARQL models   +  +
+  +                                                       +  +
+  +-------------------------------------------------------+  +
+                              ↓                              +
+  +------------------ DATA ACCESS LAYER ------------------+  +
+  + Provide generic SPARQL models                         +  +
+  + Provide @SPARQLResource model description annotation  +  +
+  + Provide @SPARQLProperty model description annotation  +  +
+  +-------------------------------------------------------+  +
+                              ↓                              +
+  +---------------------- SERVICES -----------------------+  +
+  + SPARQLService:                                        +  +
+  +   declare SPARQLConnection service connection         +  +
+  + SPARQLConnection:                                     +  +
+  +   provide RDF4J implementation                        +  +
+  + SPARQLServiceFactory                                  +  +
+  +   provide RDF4J remote server implementation          +  +
+  +   provide RDF4J in-memory server implementation       +  +
+  +-------------------------------------------------------+  +
+                              ↓                              +
+  +---------------------- LIBRARIES ----------------------+  +
+  + Integrate Apache Jena for SPARQL query generation     +  +
+  + Integrate Eclipse RDF4J API in implementations        +  +
+  +-------------------------------------------------------+  +
+                                                             +
+++++++++++++++++++ Provided extensions +++++++++++++++++++++++
+ SPARQLExtension:                                            +
+   allow other modules to register managed ontology files    +
+-------------------------------------------------------------+
```

### opensilex-nosql

This module provide `services` to interact with NoSQL databases.

It actually only implements MongoDB drivers

For more information on this module see [documentation](./nosql.md)

```
+-------------- opensilex-nosql - NoSQLModule ----------------+
+                                                             +
+  +---------------------- SERVICES -----------------------+  +
+  + MongoDBService:                                       +  +
+  +   provide access to MongoDB database                  +  +
+  +-------------------------------------------------------+  +
+                              ↓                              +
+  +---------------------- LIBRARIES ----------------------+  +
+  + Integrate MongoDB driver                              +  +
+  +-------------------------------------------------------+  +
+                                                             +
+-------------------------------------------------------------+
```

### opensilex-security

This module provide several `features` to add security management to OpenSilex application.

It provide user authentication based on user and password hashed with BCrypt.

It provide REST API security check with a JWT token in headers provided after authentication.

This security check is based on verifying if a user is associated to the credentials correponding to the requested `API` method.

It use annotations on `API` classes and methods to define credentials by credential groups.

Credentials are associated to a user with group and profiles.

Profiles are a list of credentials.

Group are a list of associated User+Profile.

User credentials correspond to the merged set of all user's profiles credentials accross all user's group.

For more information on this module see [documentation](../opensilex-security/security.md)

```
+----------- opensilex-security - SecurityModule -------------+
+                                                             +
+                                +---------- CLI ----------+  +
+                                + Add user commands       +  +
+                                +-------------------------+  + 
+                                             ↓               +
+  +---------------------- REST API -----------------------+  +
+  + Add Authentication API and related DTOs               +  +
+  + Add User API and related DTOs                         +  +
+  + Add Group API and related DTOs                        +  +
+  + Add Profile API and related DTOs                      +  +
+  + Provide @CurrentUser injection annotation             +  +
+  + Provide @ApiProtected security annotation             +  +
+  + Provide @ApiCredential security annotation            +  +
+  + Provide @ApiCredentialGroup security annotation       +  +
+  +-------------------------------------------------------+  +
+                              ↓                              +
+  +------------------ DATA ACCESS LAYER ------------------+  +
+  + Add Authentication DAO                                +  +
+  + Add User DAO and related models                       +  +
+  + Add Group DAO and related models                      +  +
+  + Add Profile DAO and related models                    +  +
+  +-------------------------------------------------------+  +
+                              ↓                              +
+  +---------------------- SERVICES -----------------------+  +
+  + AuthenticationService:                                +  +
+  +   default implementation based on BCryp hashed        +  +
+  +   password validation and JWT token generation        +  +
+  +-------------------------------------------------------+  +
+                              ↓                              +
+  +---------------------- LIBRARIES ----------------------+  +
+  + Integrate Auth0 JWT token library                     +  +
+  + Integrate BCrypt java library                         +  +
+  +-------------------------------------------------------+  +
+                                                             +
+++++++++++++++++++ Provided extensions +++++++++++++++++++++++
+ LoginExtension:                                             +
+   allow other modules to interact on user login/logout      +
+++++++++++++++ Implemented extension points ++++++++++++++++++
+ APIExtension:                                               +
+   register module API packages                              +
+   register Jersey filter to protected API entry points      +
+ LoginExtension:                                             +
+   store user group credentials id into user token           +
+-------------------------------------------------------------+
```
### opensilex-core

This module contains REST API and DATA ACCESS LAYER `features` for common concepts for scientific research data management.

- Experiment
- Infrastructure

For more information on this module see [documentation](./core.md)

```
+--------------- opensilex-core - CoreModule -----------------+
+                                                             +
+  +---------------------- REST API -----------------------+  +
+  + Provide DTOs corresponding to core models             +  +
+  + Provide APIs corresponding to core models             +  +
+  +-------------------------------------------------------+  +
+                              ↓                              +
+  +------------------ DATA ACCESS LAYER ------------------+  +
+  + Provide APIs corresponding to core models             +  +
+  + Provide models for scientific research management     +  +
+  +-------------------------------------------------------+  +
+                                                             +
+++++++++++++++ Implemented extension points ++++++++++++++++++
+ APIExtension:                                               +
+   register module API packages                              +
+ LoginExtension:                                             +
+   store custom informations into user token                 +
+ SPARQLExtension:                                            +
+   Add mapping to core ontology files                        +
+-------------------------------------------------------------+
```

### opensilex-phis

This module contains all legacy PHIS services and will not be covered by this documentation.

### opensilex-front

This module integrate a Vue.js extensible application for OpenSlex modules.


For more information on this module see [documentation](./vuejs.md)

```
+-------------- opensilex-front - FrontModule ----------------+
+                                                             +
+  +------------------- FRONT WEB APP ---------------------+  +
+  + Vue.js base application , Setup on "/app"             +  +
+  + Integrate Vue.js common mechanisms and libraries      +  +
+  + Add Generic Vue.js components                         +  +
+  + Add Vue.js components related to built-in concepts    +  +
+  +-------------------------------------------------------+  +
+                             ↓                               +
+  +---------------------- REST API -----------------------+  +
+  + Add Front API to provide front app configuration      +  +
+  + Add mechanism for Vue.js module extension management  +  +
+  + Add mechanism for Vue.js theme extension management   +  +
+  +-------------------------------------------------------+  +
+                                                             +
+++++++++++++++ Implemented extension points ++++++++++++++++++
+ ServerExtension:                                            +
+   register Vue.js application in Tomcat                     +
+ APIExtension:                                               +
+   register module API packages                              +
+-------------------------------------------------------------+
```
