# Technical documentation : [`sparql`] `opensilex-sparql` module architecture

**Document history (please add a line when you edit the document)**

| Date       | Editor(s)        | OpenSILEX version | Comment           |
|------------|------------------|-------------------|-------------------|
| 2026-09-29 | Arnaud Charleroy | BUILD-SNAPSHOT    | Document creation |

> ⚠️ _WARNING_ : this page is the **module-level** view of `opensilex-sparql`: what the Maven and OpenSILEX module
> contains, how its packages depend on each other and how the rest of the platform uses it. It was written by reading the
> code, which is identical to the code of `develop` for this module; nothing was built or run.
>
> It deliberately does **not** repeat how the ORM works. For that, read [orm-architecture.md](./orm-architecture.md) first and
> the [document index](./README.md).
>
> Two companion documents cover the rest of the platform:
> [modules-overview.md](../architecture/modules-overview.md) (how all modules are loaded and how they depend on each other)
> and [java-naming-conventions.md](../architecture/java-naming-conventions.md) (Java naming rules for the whole code base).

## Table of contents

<!-- TOC -->
* [Technical documentation : [`sparql`] `opensilex-sparql` module architecture](#technical-documentation--sparql-opensilex-sparql-module-architecture)
  * [Table of contents](#table-of-contents)
  * [Definitions](#definitions)
  * [Solution](#solution)
    * [Module identity](#module-identity)
    * [Configuration](#configuration)
    * [Lifecycle](#lifecycle)
    * [Package map](#package-map)
    * [Dependencies inside the module](#dependencies-inside-the-module)
    * [How other modules use the module](#how-other-modules-use-the-module)
    * [Resources and vocabularies](#resources-and-vocabularies)
    * [Naming as applied in this module](#naming-as-applied-in-this-module)
    * [opensilex-graphql](#opensilex-graphql)
  * [Tests](#tests)
  * [Limitations and improvements](#limitations-and-improvements)
  * [Checks against the ORM documents](#checks-against-the-orm-documents)
  * [Documentation](#documentation)
<!-- TOC -->

## Definitions

- **Module class** : `SPARQLModule`, the class extending `OpenSilexModule`.
- **Extension point** : `SPARQLExtension`, an interface that other module classes implement to register ontology files.
- **Staple ontology** : an ontology file registered with `addToStaple = true`; it is exported by `opensilex-graphql`.

## Solution

### Module identity

- Module class: [SPARQLModule](../../../../../../opensilex-sparql/src/main/java/org/opensilex/sparql/SPARQLModule.java)
  extends `OpenSilexModule`. It implements **no** extension interface; it **defines**
  [SPARQLExtension](../../../../../../opensilex-sparql/src/main/java/org/opensilex/sparql/extensions/SPARQLExtension.java)
  (which extends `OpenSilexExtension`).
- `SPARQLExtension` has four default methods: `getOntologiesFiles()`, `installOntologies(sparql, reset)`,
  `checkOntologies(sparql)` and `inMemoryInitialization()`. An ontology file is described by an `OntologyFileDefinition`
  (URI, file path, RDF language, prefix, optionally the prefix URI and `addToStaple`).
- **No dependency is declared in Java.** The Maven dependencies of the module are `opensilex-main` (compile, and its `tests`
  classifier for the tests) and `jgrapht-core` 1.5.1, used only by the ontology stores and `JgraphtUtils`. RDF4J, Jena's
  query builder, byte-buddy, reflections and univocity come from the dependencies that `opensilex-parent` gives to every
  module. The `pom.xml` registers `SPARQLDeserializer` as a `ServiceLoader` service.
- Load order: fourth in the built-in list (`main`, `fs`, `nosql`, `sparql`, `security`, `core`, `front`). `nosql` depends on
  `sparql` in Maven but is loaded before it.

### Configuration

Config id `ontologies`, interface `SPARQLConfig`, eleven keys:

| Key                      | Default                            | Role                                                          |
|--------------------------|------------------------------------|---------------------------------------------------------------|
| `baseURI`                | `http://installation.domain.org/`  | Base of the resource URIs                                     |
| `baseURIAlias`           | `local`                            | Prefix alias for the base URI                                 |
| `generationBaseURI`      | empty                              | Base for generated URIs (the base URI when empty)             |
| `generationBaseURIAlias` | `id`                               | Alias used when computing the generation prefix               |
| `sparql`                 | none                               | The `SPARQLServiceFactory`, see [orm/10](./orm/10-connection-and-lifecycle.md) |
| `usePrefixes`            | `true`                             | Write short prefixed URIs                                     |
| `customPrefixes`         | none                               | Extra prefixes, a map                                         |
| `enableSHACL`            | `false`                            | Generate and enforce SHACL shapes                             |
| `enableOntologyStore`    | `true`                             | Keep the vocabulary in memory                                 |
| `csvBatchSize`           | `4096`                             | Rows per batch in the CSV pipeline                            |
| `csvMaxErrorNb`          | `100`                              | Errors after which a CSV import stops                         |

Two different default base URIs exist: the configuration default above, and a `DEFAULT_BASE_URI` constant
(`http://default.opensilex.org/`) in `SPARQLModule`, used only when the configuration is null.

### Lifecycle

`SPARQLModule` overrides `setup`, `install`, `check` and `startup`. It does **not** override `shutdown` or `clean`.
`setup` computes the URIs and prefixes; `startup` calls `SPARQLExtension.inMemoryInitialization()` on the extensions only when
the factory is an `RDF4JLMDBServiceFactory`, then builds the ontology store; `SPARQLServiceFactory.shutdown` only clears the
prefixes. The details are in [orm/10-connection-and-lifecycle.md](./orm/10-connection-and-lifecycle.md).

### Package map

Root package `org.opensilex.sparql`; 150 Java files, about 21,300 lines. `SPARQLService.java` alone has 2,822 lines.

| Group               | Package                | Files | Role and representative classes                                                            |
|---------------------|------------------------|-------|--------------------------------------------------------------------------------------------|
| Module              | root                   | 2     | `SPARQLModule`, `SPARQLConfig`                                                              |
| Module              | `extensions`           | 2     | `SPARQLExtension`, `OntologyFileDefinition`                                                 |
| Module              | `cli`                  | 1     | `SPARQLCommands`: `reset-ontologies`, `rename-graph`, `shacl-enable`, `shacl-disable`        |
| Mapping             | `annotations`          | 7     | `SPARQLResource`, `SPARQLProperty`, `SPARQLResourceURI`, `SPARQLManualLoading`               |
| Mapping             | `mapping`              | 18    | `SPARQLClassAnalyzer`, `SPARQLClassObjectMapper` and its index, `SPARQLClassQueryBuilder`, the proxies, `SparqlNoProxyFetcher`, `SPARQLListFetcher` |
| Models              | `model`                | 11    | `SPARQLResourceModel`, `SPARQLNamedResourceModel`, `SPARQLTreeModel`, `SPARQLDagModel`, `SPARQLLabel`, `VocabularyModel` |
| Models              | `model/time`           | 2     | `InstantModel`, `Time` (vocabulary)                                                         |
| Service, connection | `service`              | 10    | `SPARQLService`, `SPARQLServiceFactory`, `SPARQLConnection`, `SPARQLQueryHelper`, `SearchFilter`, `SPARQLResult`, `SPARQLLiteral` |
| Service, connection | `rdf4j`                | 6     | `RDF4JConnection`, `RDF4JServiceFactory`, `RDF4JLMDBServiceFactory`, `RDF4JConfig`           |
| Query building      | `service/query`        | 4     | `SparqlUrisQuery`, `SparqlMultiGraphQuery`, `SparqlMultiClassQuery`                          |
| Query building      | `service/schemaQuery`  | 4     | `SparqlSchema`, `SparqlSchemaNode`                                                          |
| Ontology            | `ontology/dal`         | 9     | `OntologyDAO`, `ClassModel`, `ObjectPropertyModel`, `OwlRestrictionModel` (the `ontology` package itself has no file) |
| Ontology            | `ontology/store`       | 5     | `OntologyStore`, `DefaultOntologyStore`, `NoOntologyStore`, `OntologyStoreLoader`            |
| Ontology            | `owl`                  | 2     | `OwlRestrictionValidator`, `ValidationContext`                                              |
| CSV                 | `csv`                  | 6     | `CsvImporter`, `AbstractCsvImporter`, `DefaultCsvImporter`, `CSVCell`, `CSVValidationModel`   |
| CSV                 | `csv/error`, `export`, `header`, `validation` | 3, 4, 1, 4 | `CSVDatatypeError`; `AbstractCsvExporter`; `CsvHeader`; `CachedCsvImporter`, `CustomCsvValidation` |
| Deserializers       | `deserializer`         | 17    | `SPARQLDeserializer`, the `SPARQLDeserializers` registry, `URIDeserializer`, 14 type deserializers |
| REST DTOs           | `response`             | 12    | `ResourceDTO`, `NamedResourceDTO`, `ResourceTreeDTO`, `CreatedUriResponse`                   |
| Exceptions          | `exceptions`           | 16    | `SPARQLException` and 15 subclasses                                                         |
| Utilities           | `utils`                | 4     | `Ontology` (property paths and factories), `SHACL`, `URIEquator`, `JgraphtUtils`             |

### Dependencies inside the module

From an import analysis of the packages (wildcard imports included):

- **Import nothing from the module** : `annotations`, `deserializer`, `exceptions`. They are the most imported: `deserializer` by
  15 packages, `exceptions` by 13.
- **Near leaves** : `model` (imports annotations, deserializer and `utils.Ontology`), `response` (deserializer and model), `owl`.
- **The core web** : `mapping` imports `service` (15 files), `model` (14), `deserializer`, `exceptions`, `utils`, `annotations`
  and `ontology.dal`; `service` imports `mapping`, `ontology.dal`, `rdf4j`, `schemaQuery`, `extensions` and the root;
  `ontology.dal` imports the root, `mapping`, `ontology.store`, `response`, `service` and `model`.
- **Top of the stack** : `csv` and its sub-packages, and `cli`.

The packages form **two strongly connected components**: one of twelve packages (root, `service`, `service.schemaQuery`,
`mapping`, `utils`, `model`, `model.time`, `ontology.dal`, `ontology.store`, `response`, `rdf4j`, `extensions`) and one made
of `csv` with its three sub-packages. Examples of cycles: root and `service` (`SPARQLService` refers to `SPARQLModule`), root and
`rdf4j`, `service` and `mapping`, `mapping` and `utils` (`SHACL` refers to `mapping`, the query builder refers to `SHACL`),
`ontology.dal` and `ontology.store`. In practice the module cannot be split by package.

**Dependencies on `opensilex-main`.**

- `response` extends main's REST types (`PaginatedListResponse`, `JsonResponse`, `ObjectUriResponse`).
- `SPARQLService` throws main's HTTP-flavoured `ConflictException`, `NotFoundException` and `NotFoundURIException`;
  `SPARQLQueryHelper` throws `DisplayableBadRequestException`.
- `URIDeserializer.setPrefixes` mirrors the prefixes into main's `UriFormater`, a second static copy.
- `AbstractOntologyStore` reads `ServerConfig.availableLanguages`.

### How other modules use the module

About 360 files outside the module import from `org.opensilex.sparql` (a script count, dataverse included): `core` 242, `security` 32,
`migration` 22, `faidare` 21, `front` 15, `nosql` 11, `brapi` 8, `phis` 3, `graphql` 2, `dev-tools` 1. By package, the
most imported are `service` (about 180 files), `model` (about 160), `deserializer` (about 150), `exceptions`, `response`,
`ontology.dal`, `annotations` and `utils`. Nobody outside imports `cli`, `owl` or `csv.error`.

| Class                                         | Files that import it       |
|-----------------------------------------------|----------------------------|
| `SPARQLService`                               | about 170 (core 108, migration 22, security 15) |
| `SPARQLDeserializers`                         | about 144                  |
| `SPARQLResourceModel`                         | about 100                  |
| `SPARQLException`                             | about 60                   |
| `SPARQLQueryHelper`                           | about 46                   |
| `SPARQLNamedResourceModel`, `@SPARQLResource` | about 41 each              |
| `@SPARQLProperty`, `utils.Ontology`           | about 35 each              |
| `OntologyDAO`, `ClassModel`, `SPARQLModule`   | about 30, 29 and 27        |

Most used `SPARQLDeserializers` methods: `compareURIs` (about 240 call sites), `nodeURI`, `getExpandedURI`, `getShortURI`,
`formatURI`, `getForClass`. `ListWithPagination` is **not** in this module: it is in `opensilex-main`.

**`SPARQLExtension` implementers.**

| Module class      | What it registers or does                                                                                      |
|-------------------|----------------------------------------------------------------------------------------------------------------|
| `SecurityModule`  | Overrides `inMemoryInitialization` to create the default super administrator; registers no ontology file        |
| `CoreModule`      | `peco_factors.owl` (prefix `peco`) and `oeso-core.owl` (prefix `vocabulary`, `addToStaple`); the `Oeso`, `Oeev`, `Time` prefixes in its own `setup()` |
| `PhisWsModule`    | `oeso-phis.owl` (`addToStaple`); loads `species.ttl` at install                                                 |
| `DataverseModule` | `oeso-dataverse.owl`; the module is commented out of the root `pom.xml`                                          |

**Other extension points used outside the module.** `SearchFilter` (10 subclasses in `core`), `AbstractCsvImporter`
(`EventCsvImporter`, `ScientificObjectCsvImporterLogic`), `AbstractCsvExporter` (`ScientificObjectCsvExporter`),
`ClassSpecificDeleteVerificationAskQueryProvider` (`FactorCategoryDeleteVerificationAskQueryBuilder`). `SPARQLDeserializer` is
registered as a `ServiceLoader` service but has no implementation outside the module.

**Models.** `@SPARQLResource` appears in 42 main-source files: `core` 29, `sparql` 6, `security` 5, `front` 2. Outside this
module, about 30 concrete classes extend one of the four base models directly (`SPARQLResourceModel`,
`SPARQLNamedResourceModel`, `SPARQLTreeModel`, `SPARQLDagModel`).

### Resources and vocabularies

- The only main resource is `rdf4j-lmdb-repository-creation-template.ttl`, read by `RDF4JServiceFactory`. **No ontology file
  ships in this module**: they ship in the modules that register them (`oeso-core.owl` in `core`, `oeso-phis.owl` in `phis`).
- Vocabularies written in Java: `Time` (prefix `time`, `http://www.w3.org/2006/time#`; the prefix is registered by `core`),
  `SHACL` (prefix `sh`), the constant `SPARQLModule.ONTOLOGY_BASE_DOMAIN` (`http://www.opensilex.org/`), and the default
  prefixes `rdfs`, `foaf`, `dc`, `owl` and `xsd` set in `SPARQLService`.
- The test profile is configured in `opensilex-main` (`config/test/opensilex.yml`): an RDF4J LMDB store with `enableSHACL`.

### Naming as applied in this module

- Types are `SPARQL*` (53) far more often than `Sparql*` (11). The `Sparql*` names are `SparqlMapper`, `SparqlNoProxyFetcher`,
  `SparqlMinimalFetcher`, `SparqlProxyNamedResource`, `SparqlUrisQuery`, `SparqlMultiGraphQuery`, `SparqlMultiClassQuery` and
  `SparqlSchema*`. Backend types are `RDF4J*`. CSV is `CSV*` in 5 types and `Csv*` in 8.
- Role suffixes: `Fetcher`, `Builder`, `Mapper`, `Analyzer`, `Index`, `Validator`, `Store`, `Loader`, `Factory`, `Config`, `Helper`
  and `DAO` (`OntologyDAO`, in `ontology/dal`). `Abstract*`, `Default*` and `No*` mark the template, default and null variants.
  There is no `Handler` type.
- Constants: `LOGGER` in every class that declares a logger; field names as `*_FIELD` (`SPARQLResourceModel`).
- Exceptions: all 15 `SPARQL*Exception` subclasses extend `SPARQLException`, which extends `Exception`: they are all **checked**,
  including `SPARQLIllegalStateException`.
- Deviations: `SPARQLDeserializerNotFoundException` extends plain `Exception` and sits outside `exceptions`;
  `SPARQLUnkownKeyException` is misspelled; the package `schemaQuery` is camel case; the CSV errors in `csv/error` are
  `CSV*Error` and extend `CSVCell`, not an exception type; the test `SPARQLServiceTest` is in the root test package although it
  tests `service`; a test vocabulary class is named `TEST_ONTOLOGY`.

### opensilex-graphql

The four-file `opensilex-graphql` module depends on this one and exposes the Staple ontology, built from the files that
extensions register with `addToStaple`, plus the map from each `rdf:type` to its default graph, read from the mapper index. It
is described in [opensilex-graphql/module-architecture.md](../opensilex-graphql/module-architecture.md).

## Tests

JUnit 4, 21 files. The base is `AbstractUnitTest` (from `opensilex-main`) or the `OpenSilexTestEnvironment` singleton, which six
test files in `security` and `core` also import. Because Surefire is configured with `reuseForks=true`, static state is shared
between test classes.

| Class                                   | Tests | Covers                                                            |
|-----------------------------------------|-------|-------------------------------------------------------------------|
| `SPARQLServiceTest` (abstract)          | 30    | CRUD, URI generation, rename, labels, prefixes, other graph       |
| `RDF4JConnectionTest`                   | inherits | Runs `SPARQLServiceTest` on `RDF4JLMDBServiceFactory`           |
| `SPARQLMetadataTest`                    | 3     | Publisher and dates                                               |
| `SPARQLListFetcherTest`                 | 9     | `SPARQLListFetcher`                                               |
| `SPARQLQueryHelperTest`                 | 11    | `VALUES` clauses for URIs, strings, booleans, numbers, dates, characters, bytes and emails, and date ranges |
| `SparqlUrisQueryTest`                   | 5     | URI-existence queries                                             |
| `SHACLTest` (abstract), `RDF4JSHACLTest`| 1     | SHACL generation and failure                                      |
| `JgraphtUtilsTest`                      | 1     | Graph utilities                                                   |
| `SPARQLClassAnalyzerTest`               | 0     | Inert: every body is commented out                                |

Test models are `A`, `B`, `C`, `D`, `InverseModel`, `ModelInAnotherGraph`, `UriGeneratedTestModel` and the constants class
`TEST_ONTOLOGY`; `NoGetterClass` and `NoSetterClass` are fixtures that nothing uses. Seven files under
`src/test/resources/ontologies`; `sparql_list_fetcher.ttl` is not referenced.

**Not covered by a test in this module** : `SparqlSchema`, the ontology store and `OntologyDAO`, the CSV import and export,
`owl`, `cli`, `SPARQLExtension`, `SPARQLTreeModel` and `SPARQLDagModel`, `response`, and the remote HTTP backend. `core` tests reach
some of these indirectly (the CSV importer, the exporter, the ontology store and DAO).

## Limitations and improvements

- **Global static state**: `SPARQLModule.ontologyStore`, `SPARQLService.prefixes`, the `SPARQLDeserializers` registry and a second
  prefix copy in main's `UriFormater`. With `reuseForks=true` it leaks between test classes.
- The module does no shutdown work: no `shutdown()` override, and the ontology store is not cleared.
- The two-component cycle structure (see above) and the size of `SPARQLService` (2,822 lines) and `SPARQLClassQueryBuilder`
  (about 1,240 lines) are the main obstacles to splitting the module.
- A module meant to be independent of REST depends on main's HTTP exceptions and response classes.
- `SPARQLClassAnalyzerTest` is inert because its bodies use `includeResourceClass`, which no longer exists.
- A commented logger in `opensilex-main/src/main/resources/logback.xml` names `org.opensilex.sparql.mapper.SPARQLClassObjectMapper`;
  the real package is `mapping`.

## Checks against the ORM documents

Statements in the existing ORM documents that this review could not reproduce from the code of this branch. Nothing was
edited.

| Document                                   | Statement                                                                            | What the code shows                                                                                       |
|--------------------------------------------|--------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------|
| [orm-architecture.md](./orm-architecture.md) | Forty-five `@SPARQLResource` classes: 31 in `core`, 6 in `sparql`, 5 in `security`, 2 in `front` and 1 in `opensilex-data-analysis` | 42 files: `core` 29, `sparql` 6, `security` 5, `front` 2; there is no `opensilex-data-analysis` module in the tree |
| [orm-architecture.md](./orm-architecture.md) | The startup step for `SPARQLModule.setup()` lists `baseURI`, `baseURIAlias`, `generationBaseURI`, `customPrefixes` | `setup()` also reads `generationBaseURIAlias` to compute the generation prefix                            |
| [orm/10-connection-and-lifecycle.md](./orm/10-connection-and-lifecycle.md) | `NoGetterClass` and `NoSetterClass` are malformed models that `SPARQLClassAnalyzerTest` expects to be rejected | The test has no live method: its bodies are commented out                                                |
| [orm/12-models-and-responses.md](./orm/12-models-and-responses.md) | About 100 files declare `extends SPARQLResourceModel`                        | 100 files contain that text across all trees, tests and generic bounds included; about 30 concrete main-source classes outside this module extend one of the four base models directly |

## Documentation

- [README.md](./README.md) : the index of this documentation set; [orm-architecture.md](./orm-architecture.md) : how the ORM works.
- [orm/10-connection-and-lifecycle.md](./orm/10-connection-and-lifecycle.md) : configuration, connections, startup and shutdown.
- [orm/09-ontology-store-and-owl.md](./orm/09-ontology-store-and-owl.md) and
  [orm/11-csv-pipeline.md](./orm/11-csv-pipeline.md) : the ontology and CSV subsystems whose packages are listed above.
- [orm-bugs-and-memory-leaks.md](./orm-bugs-and-memory-leaks.md) : verified bugs and retention risks.
