# Technical documentation : [`architecture`] `opensilex-core` module

**Document history (please add a line when you edit the document)**

| Date       | Editor(s)        | OpenSILEX version | Comment           |
|------------|------------------|-------------------|-------------------|
| 2026-09-29 | Arnaud Charleroy | BUILD-SNAPSHOT    | Document creation |

> ⚠️ _WARNING_ : written from the `develop` branch (commit `6725c2912`) by reading the code; nothing was built or run.
> Counts are `grep` or `wc` results over `opensilex-core/src/main/java` (419 Java files) and can drift.
>
> Currently covered topics : module identity, concept map, layering, storage, import and export, tests.
>
> Missing topics : a per-concept description. Existing feature documents cover data, documents, location, germplasm,
> ontology and scientific objects (see [Documentation](#documentation)); experiments, variables, devices, organisations,
> events, provenance, annotations, areas, metrics, logs and URI search have none.

## Table of contents

<!-- TOC -->
* [Technical documentation : [`architecture`] `opensilex-core` module](#technical-documentation--architecture-opensilex-core-module)
  * [Table of contents](#table-of-contents)
  * [Definitions](#definitions)
  * [Solution](#solution)
    * [Module identity](#module-identity)
    * [Lifecycle](#lifecycle)
    * [Concept map](#concept-map)
    * [Layering as used](#layering-as-used)
    * [MongoDB and SPARQL storage](#mongodb-and-sparql-storage)
    * [Import and export](#import-and-export)
    * [Ontologies and resources](#ontologies-and-resources)
  * [Tests](#tests)
  * [Limitations and improvements](#limitations-and-improvements)
  * [Documentation](#documentation)
<!-- TOC -->

## Definitions

- **Concept** : a top-level package of `org.opensilex.core` for one scientific notion (experiment, variable, device, ...).
- **API / bll / dal** : the REST layer, the business logic layer (`*Logic` classes) and the data access layer (`*DAO`
  and `*Model` classes). The business logic package is named `bll`, not `logic`.
- **V1 / V2 DAO** : the two generations of MongoDB DAOs (see [MongoDB and SPARQL storage](#mongodb-and-sparql-storage)).

## Solution

### Module identity

- Module class: [CoreModule](../../../../../../opensilex-core/src/main/java/org/opensilex/core/CoreModule.java),
  config id `core`, configuration interface `CoreConfig`.
- Maven dependencies: `opensilex-main`, `opensilex-security`, `opensilex-sparql`, `opensilex-nosql`, `opensilex-fs`.
  `CoreModule` declares no dependency in Java; its place in the load order (after `security`, before `front`) comes from
  the [built-in list](../architecture/modules-overview.md#runtime-load-order).
- Implemented interfaces, and what `CoreModule` does with each:

| Interface                              | What core does                                                                                             |
|----------------------------------------|------------------------------------------------------------------------------------------------------------|
| `APIExtension`                         | Adds the packages `core.logs.filter` (only if `core.enableLogs`) and `core.metrics.schedule` (only if `core.metrics.enableMetrics`) to the scan |
| `SPARQLExtension`                      | Registers `ontologies/peco_factors.owl` (prefix `peco`) and `ontologies/oeso-core.owl` (prefix `vocabulary`, also exposed to the Staple API) |
| `JCSApiCacheExtension`                 | Inherits the default cache configuration file; its only region looks unused                                |
| `ModuleWithNosqlEntityLinkedToAccount` | Checks the Mongo collections for documents published by an account (see [security](../opensilex-security/module-architecture.md)) |
| `SwaggerExtension`                     | Adds `ResourceDagDTO` to the Swagger definition                                                            |

- Core does **not** implement `LoginExtension`, defines no extension interface, and has no CLI command and no `cli` package.
- It registers a second `ServiceLoader` provider, `FactorCategoryDeleteVerificationAskQueryBuilder`, for the sparql
  extension `ClassSpecificDeleteVerificationAskQueryProvider`.
- `CoreConfig` keys: `enableLogs` (default false), `metrics` (`enableMetrics`, and `experiments` and `system`
  schedules with `timeBeforeFirstMetric`, `delayBetweenMetrics`, `metricsTimeUnit`), `sharedResourceInstances` (a list of
  `uri`, `apiUrl`, `label`, `accountName`, `accountPassword`) and `agroportal` (`basePath`, `baseAPIPath`, `externalAPIKey`).

### Lifecycle

- `setup()` registers the SPARQL prefixes `Oeso`, `Oeev` and `Time` and the datatype `Oeso.longString`.
- `install(reset)` (run only by `opensilex system install`) inserts a default Mongo provenance, a default variables group
  ("Environmental variables"), the method `oeso:standard_method` and six interest entities (Plot, Plant, Genotype, Site,
  Green house, Chamber growth). The three SPARQL inserts rethrow on failure, so running it twice without `--reset` may
  fail.
- `startup()` registers the Mongo indexes of the `data`, `deviceAttribute`, `file`, `provenance` and `location`
  collections on `MongoDBServiceV2`, then calls `createIndexes()` unless the profile is `test` or reserved.

### Concept map

Root package `org.opensilex.core`; 32 top-level packages. Java files by layer: 222 in `api` packages, 118 in `dal`, 24 in
`bll`, 55 elsewhere.

| Concept (files)                | Layers                          | Key classes                                                                         | Storage                      |
|--------------------------------|---------------------------------|-------------------------------------------------------------------------------------|------------------------------|
| `experiment` (28)              | api, dal, utils, `factor/{api,dal}` | `ExperimentAPI`, `ExperimentDAO`, `FactorAPI`, `FactorLevelAPI`, `FactorDAO`     | SPARQL                       |
| `scientificObject` (17)        | api, bll, dal                   | `ScientificObjectLogic`, `ScientificObjectCsvImporterLogic`, `ScientificObjectDAO`  | SPARQL, Mongo geometry       |
| `variable` (48)                | api (+5 sub-APIs), dal          | `VariableAPI`, `VariableDAO`, `BaseVariableDAO`, `BaseVariableModel`                | SPARQL                       |
| `variablesGroup` (7)           | api, dal                        | `VariablesGroupAPI`, `VariablesGroupDAO`                                            | SPARQL                       |
| `germplasm` (13)               | api, bll, dal                   | `GermplasmAPI`, `GermplasmLogic`, `GermplasmDAO` (facade over a SPARQL DAO and a metadata DAO) | SPARQL + Mongo    |
| `germplasmGroup` (8)           | api, dal                        | `GermplasmGroupApi`, `GermplasmGroupDAO`                                            | SPARQL                       |
| `device` (12)                  | api, dal                        | `DeviceAPI`, `DeviceDAO`, `DeviceMetadataDao`, `CachedDeviceDAO`                    | SPARQL + Mongo               |
| `data` (65)                    | api (+`spectra`), bll (+`dataImport`), dal (+`aggregations`, `batchHistory`), factory, utils | `DataAPI`, `DataFilesAPI`, `DataLogic`, `DataImportLogic`, `DataDAO`, `DataDaoV2`, `DataFileDaoV2` | Mongo (+ SPARQL lookups, file system) |
| `provenance` (14)              | api, dal                        | `ProvenanceAPI`, `ProvenanceDAO`, `ProvenanceDaoV2`                                 | Mongo                        |
| `organisation` (38)            | api, bll, dal, each with `facility` and `site`, exception | `OrganizationAPI`, `FacilityLogic`, `SiteLogic`, `FacilityDAO`, `SiteDAO` | SPARQL (+ Mongo location) |
| `project` (7)                  | api, dal                        | `ProjectAPI`, `ProjectDAO`                                                          | SPARQL                       |
| `event` (26)                   | api (`csv`, `move`, `validation`), bll, dal (`move`) | `EventLogic`, `MoveLogic`, `EventDAO`, `MoveEventDAO`, the CSV importers | SPARQL              |
| `location` (11)                | api, bll, dal                   | `LocationAPI`, `LocationObservationLogic`, `LocationObservationDAO`                 | Mongo + SPARQL collection    |
| `position` (4), `geospatial` (4), `area` (7) | api only; api and dal; api and dal | `PositionAPI`; `GeospatialDAO`; `AreaAPI`, `AreaDAO`                  | Mongo; Mongo; SPARQL + Mongo |
| `document` (6)                 | api, dal                        | `DocumentAPI`, `DocumentDAO`                                                        | SPARQL + file system         |
| `annotation` (8), `species` (4), `address` (2) | api, dal        | `AnnotationDAO`, `SpeciesDAO`, `AddressModel`                                       | SPARQL                       |
| `ontology` (21)                | api (11), dal (1), 9 root classes | `OntologyAPI`, `SPARQLRelationFetcher`, `Oeso`, `Oeev`, `SOSA`, `Time`            | SPARQL                       |
| `uriSearch` (4)                | api, bll, dal                   | `UriSearchApi`, `UriSearchLogic`, `UriSearchSparqlDao`                              | SPARQL, and Mongo via `data` |
| `metrics` (14), `logs` (3)     | api, dal, schedule; dal, filter | `MetricAPI`, `MetricDAO`, `ScheduleMetrics`; `LogsDAO`, `UserAccessLogFilter`       | Mongo, configuration-gated   |
| `external` (8), `agroportal` (3), `csv` (2), `system` (7) | services; api; api; api | `AgroportalService`, `OpenStreetMapGeocodingService`, `SharedResourceInstanceService`, `SystemAPI` | HTTP clients / none |
| `exception` (14), `sharedResource` (2), `utils` (5), `config` (1) | flat | `ApiUtils`, `SharedResourceInstanceDTO`                                | none                         |

The actual concept count is therefore about thirty, not the two ("Experiment" and "Infrastructure") listed in
[../architecture/README.md](../architecture/README.md).

### Layering as used

[code-organization.md](../architecture/code-organization.md) prescribes `concept/api` and `concept/dal`. Core mostly follows
it: 22 of its 32 top-level packages have both. It adds a `bll` package for seven concepts (`data`, `event`, `germplasm`,
`location`, `organisation`, `scientificObject`, `uriSearch`); only those APIs go through a `*Logic` class, the others call
their DAOs directly.

**Wiring.** An `API` class is a JAX-RS resource with injected services (`SPARQLService`, `MongoDBService`,
`FileStorageService`, `@CurrentUser AccountModel`). DAOs and logic objects are **not injected**: each endpoint creates them with
`new`, passing the services. `ExperimentAPI` alone contains 30 `new ...DAO(...)` expressions.

**Typical call chains**

- Without `bll` : `ExperimentAPI.createExperiment` builds `new ExperimentDAO(sparql, nosql, fs)`, which calls
  `sparql.create(...)`. The conversion between DTO and model is done by the DTO itself (`ExperimentCreationDTO.newModel()`,
  `ExperimentGetDTO.fromModel()`).
- With `bll` : `ScientificObjectAPI` calls `ScientificObjectLogic`, whose constructor creates a `ScientificObjectDAO`;
  the DAO calls `sparql.create(...)`.
- Mongo : `DataAPI` calls `DataLogic`, which creates a `DataDaoV2` (extends `MongoReadWriteDao`) and a `CachedDeviceDAO`
  around a `DeviceDAO`; the DAO uses `MongoDBServiceV2`.

**Where the prescribed rule is broken** (verified by import search)

| Direction                         | Count                                                                                                                   |
|-----------------------------------|-------------------------------------------------------------------------------------------------------------------------|
| `dal` imports `bll`               | 7 files: `MetricDAO`, `DocumentDAO`, `ScientificObjectCsvExporter`, `DeviceDAO`, `VariableDAO`, `ExperimentDAO`, `FacilityDAO` |
| `dal` imports `api`               | 9 files, including `DataDAO`, `DataDaoV2`, `ScientificObjectDAO`, `DeviceDAO`, `GermplasmDAO`, `GermplasmSparqlDAO`      |
| `bll` imports `api`               | 13 files; `DataImportLogic` imports `DataAPI` and `ExperimentAPI`                                                        |
| Module class reads an API constant | `CoreModule` uses `DeviceAPI.METADATA_COLLECTION_NAME`                                                                  |

**Coupling.** Concepts import each other heavily: 19 pairs of concepts depend on each other (for example `data` with
`device`, `experiment`, `organisation`, `scientificObject`, `variable`). At class level there are construction cycles between
`FacilityLogic` and `SiteLogic`, `FacilityLogic` and `LocationObservationLogic`, and `FacilityLogic` and `DataLogic`. The most
referenced classes (word matches in `opensilex-core/src/main/java`) are `ExperimentDAO` (88 references in 21 files),
`OntologyDAO` from `sparql` (66), `AccountDAO` from `security` (59), `VariableDAO` (52), `LocationObservationLogic` (50) and
`DataLogic` (44). The `ontology` package is imported by 22 concepts, mainly for the `Oeso` vocabulary.

### MongoDB and SPARQL storage

- **Mongo only** : `data`, data files, `provenance`, `geospatial`, `logs`, `metrics`, `batchHistory`.
- **Hybrid** : `device` (attributes in the `deviceAttribute` collection), `germplasm` (the `germplasmAttribute` collection),
  `location` (a SPARQL collection object plus Mongo observations). Scientific objects, areas, facilities and sites are in
  SPARQL with their geometry and location in Mongo.
- **Two generations of DAOs.** V1 DAOs use `MongoDBService` (`DataDAO`, `ProvenanceDAO`, `LogsDAO`, `GeospatialDAO`,
  `MetricDAO`). V2 DAOs extend `MongoReadWriteDao` or `MetaDataDaoV2` over `MongoDBServiceV2`, obtained with
  `nosql.getServiceV2()` (`DataDaoV2`, `DataFileDaoV2`, `ProvenanceDaoV2`, `BatchHistoryDao`, `LocationObservationDAO`,
  `DeviceMetadataDao`, `GermplasmMetadataDAO`). The V1 `DataDAO` is still used by nine files.
- **Cross-store transactions** use `SparqlMongoTransaction` (see [nosql](../opensilex-nosql/module-architecture.md#transactions-spanning-both-stores)
  for its exact guarantees); it is called by `DataLogic`, `DataImportLogic`, `ScientificObjectLogic`, `FacilityLogic`,
  `SiteLogic`, `GermplasmDAO`, `AreaAPI`, `DeviceAPI` and `ScientificObjectAPI`.
- **Indexes.** The V2 collections register their indexes in `CoreModule.startup`. Other indexes are created in DAO
  **constructors** (`GeospatialDAO`, `ProvenanceDAO`, `GermplasmDAO`), and since DAOs are created for each request that
  code runs at each instantiation; `MetricDAO` has its own `createIndexes` method.

### Import and export

- **Framework** lives in `opensilex-sparql`: `AbstractCsvImporter`, `DefaultCsvImporter`, `CachedCsvImporter`, and
  `AbstractCsvExporter`.
- **Core subclasses** : `ScientificObjectCsvImporterLogic` and `ScientificObjectCsvExporter`; the event importers
  (`EventCsvImporter`, `MoveEventCsvImporter`); `DeviceAPI` uses the default importer directly.
- **Data CSV is separate** : `DataImportLogic` (`bll/dataImport`) parses with univocity in parallel batches, caches
  validation results and records each import through `BatchHistoryLogic` and `BatchHistoryDao`.
- **Endpoints** : `import` and `import_validation` exist for scientific objects, data, events and devices; experiment
  data uses `{uri}/data/import` and `{uri}/data/import_validation`. Exports exist for data, variables, devices,
  germplasm and geospatial concepts. Germplasm import is a JSON upsert, not a CSV import.

### Ontologies and resources

`src/main/resources` holds four files: `ontologies/oeso-core.owl`, `ontologies/peco_factors.owl`, `jsc-cache.ccf` and
`provenancesSchemas/software.json`. There are no configuration profiles, no i18n files and no mail templates in this
module; the translations and the Vue components for the core concepts are in `opensilex-front`. `opensilex-core/front` is a
stub (`index.ts` and a generated `lib`).

Vocabulary constant classes are in `core/ontology`: `Oeso` (prefix `vocabulary`), `Oeev`, `SOSA`, `Time`, `OntologyReference`
and the `SKOSReferences` family. The `@SPARQLResource` annotation is used by 31 model classes here, 21 of them on `Oeso`.
Note that `org.opensilex.core.ontology.Time` and `org.opensilex.sparql.model.time.Time` are two different classes with the
same simple name.

## Tests

- 54 Java files, JUnit 4. Base classes: `AbstractIntegrationTest` (main), `AbstractSecurityIntegrationTest` (security) and
  `AbstractMongoIntegrationTest` (defined **here**; starts an embedded MongoDB on port 28018). 35 tests use the Mongo base,
  12 only the security base, and 4 are unit-level (`DataMathFunctionsTest`, `GeometryToGeoJsonTest`, `DataImportLogicTest`,
  `GermplasmLogicTest`).
- Names: `*APITest` / `*ApiTest` for tests through the REST API, `*DAOTest` / `*DaoTest` for DAO tests, which are still
  integration tests.
- **Covered** : annotation, area, data (API, files, import logic), device, document, event, experiment and factor,
  geospatial, germplasm, germplasm group, organisation, project, provenance, ontology, scientific object (API, CSV import and
  export, DAO), species, variable and variables group.
- **No test found** : address, agroportal, external services, csv, location (`LocationAPI`, `LocationObservationLogic`),
  logs, metrics, position, shared resources, system, URI search, and `DataLogic`, `ExperimentDAO`, `DeviceDAO`,
  `FacilityLogic`, `EventDAO` and `ProjectDAO` directly.
- The module also hosts tests of other modules: `GroupAPITest` (security), `GridFSConnectionTest` (fs) and
  `MongoMetadataTest` (nosql).
- Two test files are inert: `MetricsDAOTest` is entirely commented out, and `GeospatialDAOTest` has one `@Ignore`.
- Test packages do not always mirror main: `experiment/factors/api` against `experiment/factor`, and `ontology/bll` against
  `ontology/dal`.

## Limitations and improvements

- **God classes** (lines): `DataDAO` 1,848, `ScientificObjectCsvImporterLogic` 1,514, `DataAPI` 1,495, `DataImportLogic` 1,359,
  `ExperimentAPI` 1,245, `DeviceAPI` 1,211, `DataFilesAPI` 1,178.
- `CoreModule.startup` registers `DataFileDaoV2.getIndexes()` for the **provenance** collection, although
  `ProvenanceDaoV2.getIndexes()` exists; this looks like a copy-paste slip.
- The layering rules are broken in both directions and the concepts form dependency cycles (see
  [Layering as used](#layering-as-used)).
- Dead or legacy code: `JsonSchemaValidation` (the whole file is commented out) and its `software.json`,
  `MoveNosqlModel` and `MoveNoSqlSearchFilter` (used only by `opensilex-migration`).
- Several GET-by-URIs endpoints are `@Deprecated(forRemoval = true, since = "1.5.2")` (`ProjectAPI`, `VariablesGroupAPI`,
  `GermplasmGroupApi`, `FacilityAPI`, and the method, unit, entity, interest entity and characteristic APIs).
- `OntologyAPI` is mounted at `/ontology` and `SiteAPI` at `core/sites` (no leading slash), unlike the other `/core/...`
  resources.
- `LocationAPI` and `PositionAPI` both expose `history` and `count`.

## Documentation

Existing feature documents in this folder:

- Data: [data_module_v2.md](./data/data_module_v2.md), [data_mongodb_indexing.md](./data/data_mongodb_indexing.md),
  [data_validation.md](./data/data_validation.md), [data_validation_import_csv.md](./data/data_validation_import_csv.md),
  [annotation-during-data-imports.md](./data/annotation-during-data-imports.md).
- [document-and-datafile-storage.md](./document/document-and-datafile-storage.md),
  [new_location_model.md](./geospatial/new_location_model.md),
  [germplasms-access.md](./germplasms/germplasms-access.md), [germplasms_import.md](./germplasms/germplasms_import.md),
  [ontology-api.md](./ontology/ontology-api.md),
  [scientific_object_import_technical_doc.md](./scientific-object/scientific_object_import_technical_doc.md).
- Some of them use names that no longer exist (`DataService.importCSVDataV2`, `ScientificObjectCsvImporter`,
  `GermplasmResource`): the classes are now `DataImportLogic.importCSVData`, `ScientificObjectCsvImporterLogic` and
  `GermplasmAPI`.
- [../architecture/modules-overview.md](../architecture/modules-overview.md), [../architecture/java-naming-conventions.md](../architecture/java-naming-conventions.md).
