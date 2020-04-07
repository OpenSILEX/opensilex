OpenSilex base architecture
================================

# Introduction

This document is a description of OpenSilex base architecture.

It will not contain any information on how to install, configure or implement features in the application.

OpenSilex is an extensible web application providing a REST API and a corresponding Vue.js interface.

OpenSilex provide also core concept for scientific experiments modeling based on ontologies.

OpenSilex is build as a set of modules providing differents features.

OpenSilex main module provide following features:
- Embed Java [Tomcat Web Server](http://tomcat.apache.org/)
- Extensible Command Line Interface (CLI) based on [Picocli library](https://picocli.info/) to start, stop the server and many other actions throught terminal
- Extensible configuration system based on YAML files using [Jackson library](https://github.com/FasterXML/jackson)
- Extensible REST API based on [Jersey library](https://eclipse-ee4j.github.io/jersey/).
- Custom service loading with injection mechanism
- Extensible modular system build with [Maven](https://maven.apache.org/)

Other following features are provided by built-in modules and won't be covered in this document:
- SPARQL support with a custom ORM-like using [Apache Jena library](https://jena.apache.org/) and annotations
- MongoDB support with [DataNucleus ORM-like](http://www.datanucleus.org/)
- Extensible [Vue.js](https://vuejs.org/) web application
- Security API  (TODO: add link to internal doc)
- Core concepts & API for modeling scientific experiments (TODO: add link to internal doc)

# Modular system

## Introduction to Maven

OpenSilex is build on a [Maven multi-module project](https://www.baeldung.com/maven-multi-module).

Maven offer different type of modules (there are others but out of the scope of this docuumentation):
- `JAR`: Standard java JAR file which can extends another POM maven project
- `POM`: Virtual module use to aggregate other module or to define common dependencies, build mechanism and be used as parent for other modules

### Built-in modules

Here is a description of the built-in modules by directories.

```
opensilex-dev --> POM - Main module aggregating all others
|
+-- opensilex --> JAR - Main application module
|
+-- opensilex-core  --> JAR - Main application concepts & API to manipulate experimental data
|
+-- opensilex-front --> JAR - Add extensible Vue.js web application
|
+-- opensilex-fs --> JAR - Add service to access file system
|
+-- opensilex-module --> POM - Parent maven module to use for external module creation
|
+-- opensilex-nosql --> JAR - Add service to access NoSQL database (MongoDB)
|
+-- opensilex-parent --> POM - Define application dependencies and default build configuration
|
+-- opensilex-security --> JAR - Define REST API default mechanism and implement security API
|
+-- opensilex-sparql --> JAR - Add service to access SPARQL triple stores like RDF4J with an ORM-like implementation
|
+-- phis2ws --> JAR - Legacy web services for compatibility
```

### Development modules

Here is a description of the development modules by directories, they will not be included in produced OpenSilex release archive.

```
opensilex-dev --> POM - Main module aggregating all others
|
+-- opensilex-dev-tools  --> JAR - Development tools module
|
+-- opensilex-doc  --> POM - Helper module to produce Javadoc & Developpers documentation
|
+-- opensilex-release --> POM - Helper module to produce release zip file with all built-in modules included, ready to deploy & run
|
+-- opensilex-swagger-codegen --> MAVEN-PLUGIN - Helper module to generate TypeScript API for Vue.js usage with custom templates (fork of https://github.com/swagger-api/swagger-codegen)
```

### Module dependency hierarchy

Maven module can declare dependencies between them. 

You can only access classes and features from a module that you depend on.

This define a dependency tree represented by the following hierarchy.

You can read it both ways like by example:
- "opensilex-parent" is a dependency of "opensilex"
- "opensilex" is dependent of "opensilex"

```
                    opensilex-parent
                            ↑
                        opensilex
                            ↑
          +-----------------+-------------------+
          |                 |                   |
  opensilex-sparql     opensilex-fs     opensilex-nosql
          ↑                 ↑                   ↑
          +-----------------+-------------------+
                            |
                   opensilex-security
                            ↑
                     opensilex-core
                            ↑
                     opensilex-front
                            ↑
                     opensilex-module
                            ↑
                         phis2ws
```

And for development modules dependency tree is represented by the following hierarchy:

```
                      opensilex-dev 
         (dependent itself to all built-in modules)
                            ↑
          +-----------------+-------------------+
          |                 |                   |
  opensilex-doc     opensilex-release     opensilex-dev-tools
```

Module `opensilex-swagger-codegen` is not represented in this hiearchy because all modules are dependent to it to build TypeScript API client.

## OpenSilex modules

### OpenSilex modules VS Maven modules

An OpenSilex module is a Maven `JAR` module containing a class extending `org.opensilex.OpenSilexModule`.

Not all Maven modules in the project are OpenSilex modules.

A maven module si build statically and the resulting JAR containing OpenSilex module is the only thing available at runtime.

See below hierarchy of built-in OpenSilex module classes by Maven modules:

```
                        opensilex
                       ServerModule
                            ↑
          +-----------------+-------------------+
          |                 |                   |
   opensilex-sparql     opensilex-fs     opensilex-nosql
     SPARQLModule    FileStorageModule     NoSQLModule
          ↑                 ↑                   ↑
          +-----------------+-------------------+
                            |
                   opensilex-security
                       RestModule
                            ↑
                     opensilex-core
                       CoreModule
                            ↑
                     opensilex-front
                       FrontModule
                            ↑
                         phis2ws
                       PhisWsModule
```

### Default features

An OpenSilex module can by default:
- Add new configuration parameters
- Add new configurable services to inject
- Use services provided by other modules
- Add new CLI commands
- Implement custom `install` method
- Implement custom `check` method
- Implement custom `startup` methods
- Implement custom `shutdown` methods
- Create new extensions mechanism for other modules

See how-to's for more information on how to implement and use this features.

(TODO: add link to internal doc)

### Basic directory structure

The simplest working module is a folder in `opensilex-dev` with the following directory structure:

```
opensilex-dev --> Root of OpenSilex sources
|
+-- my-module --> Root module folder
|   |
|   +-- src/main/java --> Java sources
|   |   |
|   |   +-- org/opensilex/myModule --> Package name
|   |       |
|   |       +-- MyModule.java --> MyModule implementation extending class OpenSilexModule
|   |
|   +-- pom.xml --> Maven module configuration file extending `opensilex-module`
|
+-- pom.xml --> Existing Maven module configuration to update with `my-module` dependency
```

### Startup mechanism

At application startup the following process occured:
- read all cached dependencies files stored in `.opensilex.dependencies` file
- list all jar files in ./modules sub-folder
- if cached dependencies not found or new module file is found in sub-folder:
    - resolve missing dependencies by automatically download dependencies artifacts from modules pom.xml with maven dependency resolver.
    - store found dependencies files references in `.opensilex.dependencies` file
- register all dependencies in application class loader
- build global configuration
- assign respective configuration to each module
- register all modules services based on configuration
- add a shutdown hook to call global `shutdown` method when application stop
- call `startup` method on each services
- call `startup` method on each modules
- at this point the application and all it's modules are initialized

See: `org.opensilex.OpenSilex::init()`
See: `org.opensilex.module.ModuleManager::startup()`

### Shutdown mechanism

At application shutdown the following process occured:
- call `shutdown` method on each modules  
- call `shutdown` method on each services

See: `org.opensilex.module.ModuleManager::shutdown()`

# Configuration mechanism

## Introduction

OpenSilex application is configured with a YAML file.

This file can be provide at application startup with the flag `--CONFIG_FILE=...`

You can alternatively use the environment variable `OPENSILEX_CONFIG_FILE`

A concept of profile also exists which is configured with the flag `--PROFILE_ID=...`

You can alternatively use the environment variable `OPENSILEX_PROFILE_ID`

This profile can only have one of "prod", "dev" or "test" for value.

Each module can provide is own configuration section with it's own identifier.

A module configuration is described by the methods of a Java interface.

Each method with its return type can define default values and description for itself with `@ConfigDescription` annotations.

To enable module configuration you just have to override `getConfigClass` and `getConfigId` methods in your module.

Then you can access to module configuration anywhere using:

```
// CCCCC is the name of the configuration interface
// MMMMM is the name of the module class using this configuration
// XXXXX is the name of the value type class

CCCCC config = OpenSilex.getModuleConfig(MMMMM.class, CCCCC.class);
XXXXX value = config.getValue();
```

The resulting config class instance is created using a [Proxy pattern](https://www.baeldung.com/java-dynamic-proxies).

This pattern allow to create a proxy instance which load dynamically typed objects on demand when an interface method is called.

## Loading process

At application startup, the following building process occured:

- Initialize a YAML parser for global configuration 
- Initialize global configuration with `CONFIG_FILE` parameter content (if exists, to be sure all required keys are initialized and check extend property)
- If global configuration has property "extend", use it as profile value
- For each modules:
    - Override global configuration with module ./src/main/resources/config/prod/opensilex.yml content (if exists)
    - Override global configuration with module ./src/main/resources/config/dev/opensilex.yml content (if exists and profile is "dev")
    - Override global configuration with module ./src/main/resources/config/test/opensilex.yml content (if exists and profile is "test")
- Override global configuration with "opensilex.yml" file content located at root directory (if exists)
- Override global configuration with "opensilex-dev.yml" file content located at root directory (if exists and profile is "dev")
- Override global configuration with "opensilex-test.yml" file content located at root directory (if exists and profile is "test")
- Override global configuration with `CONFIG_FILE` parameter content (if exists, to override properly all values defined by the external config)

See: `org.opensilex.module.config.ConfigManager::build()` method for details (TODO: add javadoc link)

This algorithm has for result a merged configuration represent by a tree of `JsonNode` with all defaults value initialized for each modules using the defined profile.

A `JsonNode` as a key and a value which is itself a `JsonNode` or a primitive value.

This tree represent the global common configuration of the application and is used ad contructor parameter for configuration proxy instance interfaces.

Each method of the configuration interface correspond to a sub-key in the tree.

When a method of the configuration proxy instance is called, the following process occured depending of the method return type:

- Case Primitive:
    - primitives are Boolean, Byte, Character, Short, Integer, Long, Float, Double, String
    - return the `JsonNode` value of the current key converted in the specified primitive type or the default value or empty element (0, "", ... depending of the primitive)

- Case Interface:
    - Interface can not be generic (only List<?> and Map<String, ?> are supported as generic, see below)
    - return a new configuration proxy for the `JsonNode` value of the current key 

- Case List<?>:
    - for each `JsonNode` value elements of the current key, load them recursively with the given generic type
    - return the list of loaded elements or default value if `JsonNode` key doesn't exists or an empty List<?> if no defaults

- Case Map<String, ?>:
    - for each pair (sub-key, `JsonNode`) of the current key, load `JsonNode` value recursively with the given generic type
    - return the map of loaded elements with respective keys or default value if `JsonNode` key doesn't exists or an empty Map<String, ?> if no defaults

- Case Class<?>:
    - return the class corresponding to the given class name stored in `JsonNode` value of the current key or the default class or java.lang.Class object

- Case Class<? extends Service>:
    - load default service configuration if `@ServiceDefaultDefinition` annotation exists for service class
    - load override service configuration stored in `JsonNode` value of the current key
    - determine service `implementation` class by using override implementation or default implementation or method return type by default
    - check that `implementation` is a subclass of the expected method return type
    - if `implementation` has a constructor accepting a single `Service` parameter:
        - determine `serviceID` by using override serviceID or default serviceID or an empty string
        - determine `serviceClass` by using override serviceClass or default serviceClass or fail
        - load service parameter recursively using `serviceID` as sub-key of current `JsonNode` and `serviceClass` as base service classe
        - return result of calling `implementation` contructor with loaded service
    - else if `implementation` has a constructor accepting a single `ServiceConfig` parameter:
        - determine `configID` by using override configID or default configID or an empty string
        - determine `configClass` by using override configClass or default configClass or fail
        - build configuration interface `configClass` recursively with `JsonNode` value of `configID` (or current node if no `configID`)
        - return result of calling `implementation` contructor with builded configuration interface
    - else if `implementation` has an empty constructor:
        - return result of calling `implementation` contructor with no arguments

- Other cases:
    - throw InvalidConfigException

- Notes:
    - Generic types for List<?> and Map<String, ?> can be any type describe in the previous cases recursively.
    - Map key generic parameter must be a String.
    - Methods calls for primitives and interfaces return types will never fails and never return null.
    - During the application startup configuration building process, services are NOT available to be used.
    - Configuration changes implies to restart application.

See: `org.opensilex.module.config.ConfigProxyHandler::invoke()` method for details (TODO: add javadoc link)

You can view the complete loaded configuration with all defaults and overrides by using command:

```
opensilex system full-config --CONFIG_FILE=...
```

Alternatively you can start class `org.opensilex.dev.DisplayConfig` in `opensilex-dev-tools` module to do the same from an IDE.

## Built-in configuration

See below hierarchy of built-in OpenSilex module configuration interface prefixed by their IDs:

```
                                      opensilex
                                     ServerModule
                                 server: ServerConfig
                                          ↑
          +-------------------------------+---------------------------+
          |                               |                           |
   opensilex-sparql                  opensilex-fs              opensilex-nosql
     SPARQLModule                  FileStorageModule             NoSQLModule
ontologies: SPARQLConfig   file-system: FileStorageConfig   big-data: NoSQLConfig
          ↑                               ↑                           ↑
          +-------------------------------+---------------------------+
                                          |
                                 opensilex-security
                                     RestModule
                                  rest: RestConfig
                                          ↑
                                   opensilex-core
                                     CoreModule
                               (No configuration yet)
                                          ↑
                                   opensilex-front
                                     FrontModule
                                 front: FrontConfig
                                          ↑
                                       phis2ws
                                     PhisWsModule
                                 phisws: PhisWsConfig
```

See below the output of the resulted fully processed configuration with comments:

```yml
# ------------------------------------------------------------------------------
# Configuration for module: ServerModule (ServerConfig)
server:
  # Modules loading order list (List<String>)
  modulesOrder:
  # Default application language (String)
  defaultLanguage: en
  # Server public URI (String)
  publicURI: http://localhost:8666/
  # Available application language list (List<String>)
  availableLanguages:
    - en
    - fr

# ------------------------------------------------------------------------------
# Configuration for module: FileStorageModule (FileStorageConfig)
file-system:
  # File storage access (FileStorageService)
  fs:
    # Service implementation class for: fs (FileStorageService)
    implementation: org.opensilex.fs.service.FileStorageService
    # Service class used as constructor parameter for: fs
    serviceClass: org.opensilex.fs.local.LocalFileSystemConnection
  # Base path for file storage (String)
  storageBasePath: 

# ------------------------------------------------------------------------------
# Configuration for module: NoSQLModule (NoSQLConfig)
big-data:
  # No SQL data source (NoSQLService)
  nosql:
    # Service implementation class for: nosql (NoSQLService)
    implementation: org.opensilex.nosql.service.NoSQLService
    # Service class used as constructor parameter for: nosql
    serviceClass: org.opensilex.nosql.mongodb.MongoDBConnection
    # Service identifier used as constructor parameter for: nosql
    serviceID: mongodb
    mongodb:
      # Service implementation class for: mongodb (NoSQLConnection)
      implementation: org.opensilex.nosql.mongodb.MongoDBConnection
      # Configuration class used as constructor parameter for: mongodb (MongoDBConfig)
      configClass: org.opensilex.nosql.mongodb.MongoDBConfig
      # MongoDB password (String)
      password: 
      # MongoDB database (String)
      database: 
      # MongoDB username (String)
      username: 
      # MongoDB authentication database (String)
      authDB: 
      # MongoDB main host (String)
      host: localhost
      # MongoDB main host port (int)
      port: 27017
      # MongoDB other connection options (Map<String>)
      options:

# ------------------------------------------------------------------------------
# Configuration for module: SPARQLModule (SPARQLConfig)
ontologies:
  # Platform base URI (String)
  baseURI: http://installation.domain.org/
  # SPARQL data source (SPARQLServiceFactory)
  sparql:
    # Service implementation class for: sparql (SPARQLServiceFactory)
    implementation: org.opensilex.sparql.rdf4j.RDF4JServiceFactory
    # Configuration class used as constructor parameter for: sparql (RDF4JConfig)
    configClass: org.opensilex.sparql.rdf4j.RDF4JConfig
    # Configuration identifier used as constructor parameter for: sparql
    configID: rdf4j
    rdf4j:
      # RDF4J repository name (String)
      repository: opensilex
      # RDF4J Server URI (String)
      serverURI: http://localhost:8080/rdf4j-server/
      # RDF4J connectrion timeout (Integer)
      timeout: 0
  # Enable SHACL usage (boolean)
  enableSHACL: false
  # Platform base URI alias (String)
  baseURIAlias: local
  # Enable URI prefixes (boolean)
  usePrefixes: true

# ------------------------------------------------------------------------------
# Configuration for module: RestModule (RestConfig)
rest:
  # Authentication service (AuthenticationService)
  authentication:
    # Service implementation class for: authentication (AuthenticationService)
    implementation: org.opensilex.rest.authentication.AuthenticationService
  # Option to allow multiple connection with the same account (NOT RECOMMENDED IN PRODUCTION) (boolean)
  allowMultiConnection: false

# ------------------------------------------------------------------------------
# Configuration for module: FrontModule (FrontConfig)
front:
  # List of menu identifiers to exclude (List<String>)
  menuExclusions:
  # Front home component once logged (String)
  homeComponent: opensilex-DefaultHomeComponent
  # Front not found component (String)
  notFoundComponent: opensilex-DefaultNotFoundComponent
  # Front header component definition (String)
  headerComponent: opensilex-DefaultHeaderComponent
  # Front login component definition (String)
  loginComponent: opensilex-DefaultLoginComponent
  # Front menu component definition (String)
  menuComponent: opensilex-DefaultMenuComponent
  # Front footer component definition (String)
  footerComponent: opensilex-DefaultFooterComponent
  # Front theme identifier (String)
  theme: opensilex-front#phis

# ------------------------------------------------------------------------------
# Configuration for module: PhisWsModule (PhisWsConfig)
phisws:
  # MongoDB image storage collection (String)
  imagesCollection: images
  # Session time (String)
  sessionTime: 12000
  # Time to wait for document upload (String)
  waitingFileTime: 50000
  # Maximum value for page size (String)
  pageSizeMax: 2097152
  # MongoDB documents storage collection (String)
  documentsCollection: documents
  # MongoDB provenance storage collection (String)
  provenanceCollection: provenance
  # RDF Vocabulary context (String)
  vocabulary: http://www.opensilex.org/vocabulary/oeso
  # Infrastructure name used for URI generation (String)
  infrastructure: 
  # GnpIS public key filename (String)
  gnpisPublicKeyFileName: GnpIS-JWT-public-key
  # PHIS public key filename (String)
  phisPublicKeyFileName: Phis-JWT-public-key.der
  # MongoDB data storage collection (String)
  dataCollection: rawData
```

To override this configuration you just have to start application with a configuration file
 containing ONLY the configuration key you want to override.

# Services

## Basic definition

Services are classes or interfaces extending `org.opensilex.service.Service` interface.

It means they simply implement two methods:
- startup: executed at service startup
- shutdown: executed at service shutdown

The purpose of a service is to abstract communication between OpenSilex application and external resources or libraries which can be by example:
- Access to the file-system
- Access to various databases
- Access to remote web-services
- Authorization mechanism
- etc...

It allow developper's to create multiple service implementation even in external modules and automatically switch them by changing configuration.

A service instance always belong to an OpenSilex module configuration.

In order to work and be automatically loaded, a service class must have at least one of this constructor types:
- A constructor with no parameter
- A constructor with one parameter of a class extending `org.opensilex.service.ServiceConfig`
- A constructor with one parameter of a class extending `org.opensilex.service.Service`

If you need to have multiple instances of the same service configuration, you can implement `org.opensilex.service.ServiceFactory`.

Modules `startup` method is called after ALL services of ALL modules `startup` have been called.

Services `shutdown` method is called after ALL modules `shutdown` have been called.

## Built-in services

### Service `fs`

Simple abstraction layer to access to any file storage system.

- Module: FileStorageModule
- Default Service class: org.opensilex.fs.FileStorageService
- Default Service class constructor parameter: org.opensilex.fs.LocalFileSystemConnection

### Service `nosql` (In development)

Abstraction layer around datanucleus ORM-like for various NoSQL databases types.

- Module: NoSQLModule
- Default Service class: org.opensilex.nosql.service.NoSQLService
- Default Service class constructor parameter: org.opensilex.nosql.mongodb.MongoDBConnection
- MongoDB configuration ID: mongodb
- MongoDB configuration class: org.opensilex.nosql.mongodb.MongoDBConfig

### Service `sparql`

Abstraction layer around a custom ORM-like based on Jena and annotations for any sparql compatible database.

- Module: SPARQLModule
- Default Service class: org.opensilex.sparql.service.SPARQLServiceFactory
- Default Service class constructor parameter: org.opensilex.sparql.rdf4j.RDF4JServiceFactory
- MongoDB configuration ID: rdf4j
- MongoDB configuration class: org.opensilex.sparql.rdf4j.RDF4JConfig

### Service `authentication`

Abstraction layer for user authentication.

Default implementation store user with password in triplestore with `sparql` service

- Module: RestModule
- Default Service class: org.opensilex.rest.authentication.AuthenticationService

# CLI

## Description

Command Line Interface is the main entry point of OpenSilex application.

CLI is implemented with Picocli library which provide automatic help, parameter validation, default values and much more.

To add a new CLI command group, you have to add a new class in your module which
- extends `org.opensilex.cli.help.HelpPrinterCommand`
- implements `org.opensilex.cli.OpenSilexCommand`
- use Picocli annotations on class and methods

When a command is run throught a terminal, the following process occured:
- initialize OpenSilex application (config, modules and services), using terminal parameters CONFIG_FILE, DEBUG, BASE_DIRECTORY and PROFILE_ID
- load all classes which implements `org.opensilex.cli.OpenSilexCommand` with java standart `ServiceLoader`
- register each command instance as a new sub-command.
- call Picocli main method with all given terminal parameters except CONFIG_FILE, DEBUG, BASE_DIRECTORY and PROFILE_ID
- Picocli will automatically resolve command to execute and check parameters

See: [More information on ServiceLoader](https://www.baeldung.com/java-spi)
See: `org.opensilex.cli.MainCommand::run` (TODO: internal javadoc link)

See below an example of default help message produced by he given annotated Java code:

```java
@Command(
        name = "user",
        header = "Subcommand to group OpenSILEX users operations"
)
public class UserCommands extends HelpPrinterCommand implements OpenSilexCommand {

    @Command(
            name = "add",
            header = "Add an OpenSilex user"
    )
    public void add(
            @CommandLine.Option(names = {"--firstName"}, description = "Define user first name", defaultValue = "Admin") String firstName,
            @CommandLine.Option(names = {"--lastName"}, description = "Define user last name", defaultValue = "OpenSilex") String lastName,
            @CommandLine.Option(names = {"--email"}, description = "Define user email", defaultValue = "admin@opensilex.org") String email,
            @CommandLine.Option(names = {"--password"}, description = "Define user password", defaultValue = "admin") String password,
            @CommandLine.Option(names = {"--admin"}, description = "Define if user is admin", defaultValue = "false") boolean isAdmin,
            @CommandLine.Option(names = {"--lang"}, description = "Define if user default language", defaultValue = OpenSilex.DEFAULT_LANGUAGE) String lang,
            @CommandLine.Mixin HelpOption help
    ) throws Exception {
        ...
    }
}

```

```
         .+.
        +++++`.
      ;+;  ++.++`.
     ++     '+,  ++`.
   '+.        ++  `+++       ___                    ___  ___  _     ___ __  __
  ++           +++`  ++     / _ \  _ __  ___  _ _  / __||_ _|| |   | __|\ \/ /
`++   ,;++++++++++++++++   | (_) || '_ \/ -_)| ' \ \__ \ | | | |__ | _|  >  <
++++++:`        ++   .++    \___/ | .__/\___||_||_||___/|___||____||___|/_/\_\
 .++    `'+++   ++  ++`           |_|
   ++.      '+++'+.++
     ++:+++.`   .++
      ``````````

Global options:
  --DEBUG                 Enable debug logging
  --CONFIG_FILE=<path>    Define OpenSilex configuration file
  --BASE_DIRECTORY=<path> Define OpenSilex running directory

Add an OpenSilex user

Usage: opensilex user add [-h] [--admin] [--email=<email>]
                          [--firstName=<firstName>] [--lang=<lang>]
                          [--lastName=<lastName>] [--password=<password>]

Description:

Options:
      --admin                   Define if user is admin
      --email=<email>           Define user email
      --firstName=<firstName>   Define user first name
  -h, --help                    Display this help and exit
      --lang=<lang>             Define if user default language
      --lastName=<lastName>     Define user last name
      --password=<password>     Define user password

Copyright(c) INRA - UMR MISTEA - 2019
```

## Built-in commands

### `system` command group

Commands:
- check        Check local installation (execute OpenSilexModule::check for each module)
- full-config  Return full configuration
- install      Setup initial database content (execute OpenSilexModule::install for each module)

### `server` command group

Commands:
- start  Start OpenSILEX server
- stop   Stop OpenSILEX server

### `sparql` command group

Commands:
- rename-graph      Rename a SPARQL graph
- reset-ontologies  Reset configred ontologies graph
- shacl-disable     Disable SHACL validation
- shacl-enable      Enable SHACL validation

### `user` command group

Commands:
- add   Add an OpenSilex user


# Tomcat Server integration

## Start with command line

Starting Tomcat Server for OpenSilex is achieved by running command `server start`.

This command is defined in `org.opensilex.cli.ServerCommands::start` method.

See below the help message for this command:

```
Usage: opensilex server start [-dh] [--adminPort=<adminPort>] [--host=<host>]
                              [-p=<port>] <tomcatDirectory>

Description:
Start OpenSILEX server with given hostname and port

Parameters:
      <tomcatDirectory>   Tomcat directory

Options:
      --adminPort=<adminPort>   Server port on which server is listening for admin commands
  -d, --daemon                  Run server as a daemon
  -h, --help                    Display this help and exit
      --host=<host>             Define server host
  -p, --port=<port>             Define server port
```

If command is used with `--daemon` flag, OpenSilex will launch a new headless process with the same parameters without this flag enabled and shutdown the current process.

This is achieved using Java standart [`ProcessBuilder`](https://www.baeldung.com/java-lang-processbuilder-api)

Admin port is used to communicate with running server through command line, especially when launched as a dameon.

Actually only `server stop` method is implemented using it.

This is achieved using Java standart [`ServerSocket`](https://www.baeldung.com/a-guide-to-java-sockets)

## Initialization

Once `server start` command is called without `--daemon` flag, the following process occured:
- load current `ServerConfig`
- initialize all system properties in `tomcatSystemProperties` configuration
- initialize tomcat base value (base path, host name, port)
- setup Swagger UI web interface folder `src/main/webapp` to map server root URL
- initialize all war files in `webapps` sub-folder of OpenSilex application if any using their filename without extension as URL prefix
- for each `OpenSilexModule` implementing `ServerExtension` call method `initServer` with current server a parameter for confuration extension
- enable GZIP compression
- enable UTF-8 encoding
- start admin server socket thread
- start tomcat server

Notes:
- Swagger UI web interface is launched with [`RewriteValve`](https://ci.apache.org/projects/tomcat/tomcat9/docs/rewrite.html), see `src/main/webapp/WEB-INF/rewrite.config`
- Swagger UI web interface is launched with `StuckThreadDetectionValve` to prevent any thread locking application for more than 30s
- `ServerExtension` is used to load Vue.js application as an extension in `FrontModule`

See: `org.opensilex.server.Server::start` method for more details.

# REST API

## Introduction to Jersey

Jersey is a Java REST Framework to build API based on annotations.

An API is defined by a class annotated with `@Path`.

Each methods of this class annotated with `@Path` is a sub-path of this API.

Each of these methods are also annotaded with `@GET`, `@PUT`, `@POST` or `@DELETE` to define which HTTP verb is supported for this path.

Each of these methods are also annotaded `@Consumes` and `@Produces` to define which MIME media types are expected as input request and output response for this entry point. 

Each parameters of these methods is annotated with either `@QueryParam`, `@PathParam`, `@FormParam`, ... to determine how to extract parameters from input request.

There is a lot more of options in Jersey, please check their [documentation](https://eclipse-ee4j.github.io/jersey.github.io/documentation/latest/jaxrs-resources.html#d0e2043)

## Introduction to Swagger API

Swagger API automatically produce an API description based on annotations.

This API description is represented by a JSON file automatically generated and accessible when server is running at URL: ̀`/rest/swagger.json`

This file is automatically processed by Swagger UI to generate a dynamic web interface accessible when server is running at URL: ̀`/api-docs`

Swagger API mechanism is also used to generate TypeScript service and types library to be included in Vue.js application.

Each Jersey API class is also annotated with `@Api` to define in which API group this class belong.

Each API methods of these classes are also annotated with:
- `@ApiOperation`: Description (short and/or long) of what this API method do
- `@ApiResponses`: List of `@ApiResponse` which each describe a return code, a description and eventually a return class object (which can be in a List)

Return codes MUST match [HTTP standart status codes](https://en.wikipedia.org/wiki/List_of_HTTP_status_codes)

Each API method parameters are annotated with `@ApiParam` to provide description and examples.

There is a lot more of options and details in Swagger API, please check their [documentation](https://github.com/swagger-api/swagger-core/wiki/Annotations-1.5.X)

## Initialization

Once Tomcat server is started, Jersey scan automatically loaded classes and initialize `org.opensilex.rest.RestApplication`:
- Setup properties and enable Jersey features for Jackson, GZIP and File data transfert
- for each `OpenSilexModule` implementing `APIExtension` register all extra packages for Jersey & Swagger API to scan
- initialize Swagger API by scanning classpath
- register all services of OpenSilex instance to be injectable in found API classes
- for each `OpenSilexModule` implementing `APIExtension` call `initAPI` method to allow post API initialization extension for `RestApplication`

See: `org.opensilex.rest.RestApplication` for more informations


