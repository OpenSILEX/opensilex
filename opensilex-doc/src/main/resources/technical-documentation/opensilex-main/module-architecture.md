# Technical documentation : [`architecture`] `opensilex-main` module

**Document history (please add a line when you edit the document)**

| Date       | Editor(s)        | OpenSILEX version | Comment           |
|------------|------------------|-------------------|-------------------|
| 2026-09-29 | Arnaud Charleroy | BUILD-SNAPSHOT    | Document creation |

> ⚠️ _WARNING_ : written from the `develop` branch (commit `6725c2912`) by reading the code; nothing was built or run.
> The bootstrap and configuration mechanisms are also described, with several outdated names, in
> [../architecture/main.md](../architecture/main.md); the differences are listed in the
> [modules overview](../architecture/modules-overview.md#corrections-to-earlier-documents).

## Table of contents

<!-- TOC -->
* [Technical documentation : [`architecture`] `opensilex-main` module](#technical-documentation--architecture-opensilex-main-module)
  * [Table of contents](#table-of-contents)
  * [Definitions](#definitions)
  * [Solution](#solution)
    * [Module identity](#module-identity)
    * [Package map](#package-map)
    * [Bootstrap sequence](#bootstrap-sequence)
    * [Command line](#command-line)
    * [Configuration and services](#configuration-and-services)
    * [Embedded server and REST application](#embedded-server-and-rest-application)
    * [REST infrastructure](#rest-infrastructure)
    * [Build products](#build-products)
  * [Tests](#tests)
  * [Limitations and improvements](#limitations-and-improvements)
  * [Documentation](#documentation)
<!-- TOC -->

## Definitions

- **Module class** : the class extending `OpenSilexModule` in a module. Here `org.opensilex.server.ServerModule`.
- **Service** : a configurable object built by the configuration mechanism from a config interface method that returns a
  `Service` (for example `SPARQLService`, `MongoDBService`, `FileStorageService`).
- **Profile** : `prod`, `dev`, `test`, or the internal `internal_operations` used by non-server commands.

## Solution

`opensilex-main` is the only module the others cannot do without. It contains no domain concept: it provides the
bootstrap of the application, the module system, the YAML configuration proxies, the service framework, the command line,
the embedded Tomcat and the shared REST plumbing (response envelopes, exceptions, validation annotations, pagination).
Its Maven dependencies on other `opensilex-*` modules: none.

### Module identity

- Module class: [ServerModule](../../../../../../opensilex-main/src/main/java/org/opensilex/server/ServerModule.java),
  config id `server`.
- Implements `APIExtension` and `JCSApiCacheExtension`.
- Defines the extension interfaces `OpenSilexExtension` (the base), `APIExtension` and `ServerExtension` (both extend it),
  and the plain interfaces `SwaggerExtension` and `JCSApiCacheExtension`.
- `ServerConfig` keys: `publicURI`, `availableLanguages`, `tomcatSystemProperties`, `enableAntiThreadLock`, `cache`,
  `pathPrefix` and `ajpConnector` (`enable`, `port`, `secret`).
- The jar is shaded (classifier `full`, `Main-Class` is `org.opensilex.cli.MainCommand`).

### Package map

Root package `org.opensilex`. File counts are Java files in `src/main/java`.

| Package                                   | Files | Role                                                                                      |
|-------------------------------------------|-------|-------------------------------------------------------------------------------------------|
| `org.opensilex` (root)                    | 8     | `OpenSilex`, `OpenSilexModule`, `OpenSilexModuleManager`, `OpenSilexSetup`, `OpenSilexConfig`, the extension interfaces |
| `cli`                                     | 8     | `MainCommand`, `SystemCommands`, `ServerCommands`, command base classes and help printing |
| `config`                                  | 4     | `ConfigManager`, `ConfigProxyHandler`, `ConfigDescription`, and one more support class     |
| `dependencies`                            | 2     | `DependencyManager`: Maven resolution of external module jars                             |
| `server`                                  | 4     | `ServerModule`, `ServerConfig`, `Server` (embedded Tomcat) and one more class             |
| `server/admin`                            | 2     | The admin socket thread                                                                   |
| `server/exceptions`                       | 11 + 3 + 4 | `WebApplicationException` subclasses, plus `displayable/` and `multipleError/`         |
| `server/extensions`                       | 2     | `APIExtension`, `ServerExtension`                                                         |
| `server/response`                         | 11 + 4 | `JsonResponse`, `SingleObjectResponse`, `PaginatedListResponse`, `ObjectUriResponse`, `ErrorResponse`, `StatusDTO`, and `multipleError/` |
| `server/rest`                             | 2     | `RestApplication` and one more class                                                      |
| `server/rest/cache`                       | 6     | `@ApiCache`, `ApiCacheFilter`, `ApiCacheService` and its implementations                  |
| `server/rest/serialization`               | 4 + 3 | Jackson context resolver, parameter converters, GeoJSON conversion, `uri/UriFormater` and URI deserializers |
| `server/rest/validation`                  | 19 + 2 + 1 | Bean validation annotations (`@Required`, `@ValidURI`, `@Date`, `@ValidLanguage`, ...) |
| `server/scanner`                          | 1     | Class scanning support                                                                    |
| `service`                                 | 7 + 1 | `Service`, `BaseService`, `ServiceFactory`, `ServiceManager`, `ServiceDefaultDefinition`  |
| `update`                                  | 3     | `OpenSilexModuleUpdate`, `AbstractOpenSilexModuleUpdate` and an exception                 |
| `uri/generation`                          | 3     | `URIGenerator`, `ClassURIGenerator`, `DefaultURIGenerator`                                |
| `utils`                                   | 12 + 7 | Utility classes, `ListWithPagination`, `SwaggerAPIGenerator`; sub-packages `pagination`, `security`, `unix`, `functionnal` |

### Bootstrap sequence

1. [MainCommand](../../../../../../opensilex-main/src/main/java/org/opensilex/cli/MainCommand.java) `main` calls
   `OpenSilex.createSetup(args)`. The base directory comes from `--BASE_DIRECTORY` (or the environment) and defaults to
   the working directory; the profile from `--PROFILE_ID` and defaults to `prod`. `--CONFIG_FILE` and `--DEBUG` are
   also read.
2. Unless the arguments are `server start`, the profile is forced to `internal_operations`.
3. `createInstance` then builds, in order: the logging configuration (`logback.xml` and `logback-` followed by the
   profile id, from the base directory), the system configuration, the `DependencyManager` and the
   `OpenSilexModuleManager`, then the `ServiceManager`.
4. `initialize()` builds the configuration (`ConfigManager.build`), loads each module's configuration and registers the
   services found in the configuration interfaces.
5. `startup()` runs module `setup`, service `setup`, service `startup`, module `startup` (see the
   [modules overview](../architecture/modules-overview.md#lifecycle)).

Module discovery, load order and configuration merge order are in the
[modules overview](../architecture/modules-overview.md).

### Command line

Commands are `OpenSilexCommand` implementations found with `ServiceLoader` in each module. Groups:

| Group    | Commands                                                                              | Defined in                                   |
|----------|---------------------------------------------------------------------------------------|----------------------------------------------|
| `system` | `check`, `install [--reset]`, `full-config`, and a hidden `run-update` taking a class name | `cli/SystemCommands`                      |
| `server` | `start`, `stop`                                                                       | `cli/ServerCommands`                         |
| `sparql` | `reset-ontologies`, `rename-graph`, `shacl-enable`, `shacl-disable`                    | `opensilex-sparql`                           |
| `user`   | `add`, `add-guest`                                                                    | `opensilex-security`                         |
| `dev`    | `install`, `start`                                                                    | `opensilex-dev-tools`                        |

`run-update` executes a class implementing `OpenSilexModuleUpdate`; that is how the
[migrations](../opensilex-migration/module-architecture.md) are run.

### Configuration and services

- `ConfigManager` merges YAML sources into one Jackson tree (order in the
  [modules overview](../architecture/modules-overview.md#configuration-merge-order)).
- `ConfigProxyHandler` turns a configuration **interface** into a `java.lang.reflect.Proxy`. Default values come from
  `@ConfigDescription`. Supported return types are primitives and their wrappers, `String`, `List`, `Map` with string keys,
  `Class`, `JsonNode`, nested config interfaces, and `Service` or `ServiceFactory`.
- A method returning a `Service` is instantiated by `ConfigProxyHandler.getService`: the implementation class comes
  from the `implementation` key, otherwise from `@ServiceDefaultDefinition(implementation = ...)`; it is built with a
  constructor taking the service's config interface (from `@ServiceDefaultDefinition(config = ...)`, read under the
  `config` sub-key), otherwise with a no-argument constructor.
- The `Service` contract is `setOpenSilex`, `setup`, `startup`, `shutdown`, `clean` and `getConfig`. `BaseService` is the
  usual base class.

### Embedded server and REST application

- `Server` is an embedded Tomcat. Its rewrite rules are written inline in the class. It maps the `/webapp` folder of the
  jar (Swagger UI at `/api-docs/`), can add an AJP connector, installs a `StuckThreadDetectionValve` when
  `enableAntiThreadLock` is true (120 seconds threshold, 30 seconds interrupt threshold; the `dev` overlay turns it
  off), calls `ServerExtension.initServer` on the modules and starts the admin socket thread.
- [RestApplication](../../../../../../opensilex-main/src/main/java/org/opensilex/server/rest/RestApplication.java) is
  `@ApplicationPath("/rest")` and is found by Tomcat's servlet scanning. See the
  [modules overview](../architecture/modules-overview.md#rest-wiring) for what it registers.

### REST infrastructure

- **Envelope** : `JsonResponse` is the abstract base with `status`, `metadata` (pagination, status list, data files) and
  `result`. `SingleObjectResponse`, `PaginatedListResponse`, `ObjectUriResponse` and `ErrorResponse` specialise it.
  `PaginatedListResponse` is built from a `ListWithPagination` or a `StreamWithPagination`.
- **Exceptions** : eleven `WebApplicationException` subclasses (bad request, not found, conflict, forbidden,
  unauthorized, invalid value, ...). Each builds its own `ErrorResponse` entity. There is **no global exception
  mapper** in this module: the only `ExceptionMapper` here is `JsonMappingExceptionResponse`; the others are in
  `opensilex-security` (`ExceptionJsonMapper`) and `opensilex-core`.
- **Providers and filters** : `ApiCacheFilter` (driven by `@ApiCache`), `ObjectMapperContextResolver`, parameter
  converters for `URI`, `LocalDate` and `OffsetDateTime`. The cache service defaults to a no-op; a JCS implementation is
  available.
- **Validation** : annotations in `server/rest/validation` (`@Required`, `@ValidURI`, `@URL`, `@Date`,
  `@ValidOffsetDateTime`, `@ValidLanguage`, `@ValidTranslationMap`, `@FilteredName`, `@NullOrNotEmpty`).
- **Pagination** : `ListWithPagination` and the `utils/pagination` classes. Note that `ListWithPagination` lives here, not in
  `opensilex-sparql`.
- **URI generation** : `URIGenerator`, `ClassURIGenerator`, `DefaultURIGenerator` (normalisation only). URI prefixes are
  handled by `UriFormater`, whose prefix mapping is set by `opensilex-sparql`.

### Build products

- The shaded `full` jar is what `opensilex-release` ships as `opensilex.jar`.
- `SwaggerAPIGenerator` (in `utils`) is run at `compile` by `exec-maven-plugin` in the modules that declare it and writes
  the Swagger specification used to generate the TypeScript client; the templates are the 24 mustache files under
  `src/main/resources/swagger/templates/typescript-inversify`.
- Other resources: `config/dev/opensilex.yml`, `config/test/opensilex.yml`, `logback.xml`, `jsc-cache.ccf` (a JCS
  configuration), and `webapp/` (Swagger UI).

## Tests

Eight files. `AbstractUnitTest` (`unit/test`) boots an `OpenSilex` instance with the `test` profile.
`AbstractIntegrationTest` (`integration/test`) is a `JerseyTest` on Grizzly with `PublicCall`, `PublicCallBuilder`,
`Result`, `ServiceDescription` and `UriResourceDTO`. Other tests: `ConfigTest`, `URIGeneratorTest`, `SecretReadUtilsTest`.
The test classes are published as a `test-jar` that every other module depends on. See
[../architecture/tests.md](../architecture/tests.md).

## Limitations and improvements

Observed by reading the code; none was reproduced by running it.

- The `--NO-CACHE` option is stored in `OpenSilexSetup` and copied, but never read.
- `opensilex-main/src/main/webapp/META-INF/MANIFEST.MF` names `org.opensilex.dev.RunUpdate` as `Main-Class`, a class of
  `opensilex-dev-tools`; the effective main class is set by the shade plugin.
- `ConfigManager.build` tests `ignoredModules.containsValue(...)` with the module class name while the map is keyed by
  class name, so an ignored module's configuration files are still merged.
- `OpenSilexModuleManager.IGNORED_MODULES` and `BUILD_IN_MODULES_ORDER` are static and only appended to, so they accumulate
  when several instances are created in one JVM (as tests do).
- `ServiceManager` registers services by name only; two configuration methods with the same name in different modules
  would collide.
- `UriFormater` holds static state that `opensilex-sparql` sets, so it is shared by every instance in the JVM.
- The `server/rest/validation` and `service` packages are large and flat; the `utils` package has grown to 12 files plus
  five sub-packages, with a misspelled `functionnal` package.

## Documentation

- [../architecture/modules-overview.md](../architecture/modules-overview.md) : module discovery, load order, lifecycle.
- [../architecture/main.md](../architecture/main.md) : the older description of the bootstrap and configuration.
- [../architecture/tests.md](../architecture/tests.md) : the test base classes.
- [../architecture/java-naming-conventions.md](../architecture/java-naming-conventions.md) : naming rules.
