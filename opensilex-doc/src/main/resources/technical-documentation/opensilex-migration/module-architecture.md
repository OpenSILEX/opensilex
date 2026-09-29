# Technical documentation : [`architecture`] `opensilex-migration` module

**Document history (please add a line when you edit the document)**

| Date       | Editor(s)        | OpenSILEX version | Comment           |
|------------|------------------|-------------------|-------------------|
| 2026-09-29 | Arnaud Charleroy | BUILD-SNAPSHOT    | Document creation |

> ⚠️ _WARNING_ : written from the `develop` branch (commit `6725c2912`) by reading the code; nothing was built or run.
> How an administrator runs a migration is described in
> [migration_command.md](../../how-to/migration_command.md); this page describes how the module is organised.

## Table of contents

<!-- TOC -->
* [Technical documentation : [`architecture`] `opensilex-migration` module](#technical-documentation--architecture-opensilex-migration-module)
  * [Table of contents](#table-of-contents)
  * [Definitions](#definitions)
  * [Solution](#solution)
    * [Module identity](#module-identity)
    * [How a migration is declared and run](#how-a-migration-is-declared-and-run)
    * [The migrations](#the-migrations)
  * [Tests](#tests)
  * [Limitations and improvements](#limitations-and-improvements)
  * [Documentation](#documentation)
<!-- TOC -->

## Definitions

- **Migration** : a class implementing `OpenSilexModuleUpdate`, run once by an administrator to convert data written by an
  older version.
- **Transactional migration** : a subclass of `DatabaseMigrationModuleUpdate`, which wraps the work in a SPARQL and/or
  MongoDB transaction.

## Solution

### Module identity

- Module class: [MigrationModule](../../../../../../opensilex-migration/src/main/java/org/opensilex/migration/MigrationModule.java)
  extends `OpenSilexModule` and implements `APIExtension` with an **empty body**: no configuration and no lifecycle code.
- The parent is `opensilex-module`; the pom declares no other `opensilex-*` dependency. 24 Java files, 3 test files, and a single
  resource: `migration/sparql_graph_rename_template.rq`.

### How a migration is declared and run

There is **no registry, no version key and no ordering** in the code. A migration is any class implementing
`OpenSilexModuleUpdate` (`getDate`, `getDescription`, `execute`, `setOpensilex`, defined in `opensilex-main`). It is run by
its fully qualified class name:

- with the hidden command `opensilex system run-update <class name>` (`SystemCommands`), or
- with the `RunUpdate` main class of `opensilex-dev-tools`.

Two base classes help: `AbstractOpenSilexModuleUpdate` (in `main`) and `DatabaseMigrationModuleUpdate`, whose final
`execute()` calls the hooks `applyOnSparql`, `applyOnMongodb`, `sparqlOperation` and `mongoOperation` for a transactional
SPARQL and/or Mongo migration. Many migrations implement the interface directly.

The chronology exists only as a manual table in
[migration_command.md](../../how-to/migration_command.md); it lists 9 of the 22 concrete classes.

### The migrations

Package `org.opensilex.migration` (16 classes), `experiment/` (1) and `one_point_five_ALL/` (7).

| Class                                                     | Purpose                                                                          |
|-----------------------------------------------------------|----------------------------------------------------------------------------------|
| `GraphAndCollectionMigration`                             | Rename SPARQL graphs and Mongo collections                                       |
| `MongoCustomCoordinatesDataTypeUpdate`                    | Convert `integer` to `String` in the custom coordinates of the moves collection    |
| `ScientificObjectNameIntegerConvertMigration`             | Fix the `rdfs:label` datatype of digit-only names                                |
| `AgentsMigrateToAccountAndPersons`                        | Split users into accounts and persons                                            |
| `ObjectMigrationFromAccountToPerson`                      | Change `foaf:OnlineAccount` objects to `foaf:Person`                             |
| `AddAccountCredentialsToProfilWithUserCredential`         | Add the account credentials to profiles that had the user credential             |
| `MongoDbIndexesMigration`                                 | Update the Mongo indexes                                                         |
| `UpdateOntologyContexts`                                  | Update the ontology contexts                                                     |
| `UpdateSitesWithLocationObservationCollectionModel`       | Move sites to the location observation collection model                          |
| `DataRectifyDateWithoutTimeValues`                        | Rectify date values without a time part                                          |
| `DeviceAttributeModelRefactorMigration`                   | Rename the device attribute field                                                |
| `MetadataMigration`                                       | `dc:creator` to `dc:publisher`, `dc:created` to `dc:issued`                       |
| `ExportDocumentFilesFromLocalFSToGRIDFS`, `ImportDocumentFilesFromLocalFSToGRIDFS` | Move document files between the local file system and GridFS |
| `experiment/UpdateExperimentSpecies`                      | Derive the species of an experiment from its germplasm                           |
| `one_point_five_ALL/MigrateToOnePointFive`                | Runs six migrations of the 1.5 upgrade in one `SparqlMongoTransaction`           |
| the six `one_point_five_ALL` parts                        | Facilities, scientific objects and moves to the location model; experiment relation; germplasm attribute rights; parameter type URI |

## Tests

Three tests: `AddAccountCredentialsToProfilWithUserCredentialTest` (on the security integration base),
`ObjectMigrationFromAccountToPersonTest` and `ScientificObjectNameIntegerConvertMigrationTest` (on the Mongo integration base).

## Limitations and improvements

- `MigrateToOnePointFive.execute` logs errors and swallows them, so the command reports success even when the transaction was
  rolled back.
- Nothing prevents running a migration twice, nor records which ones were run.
- The package `one_point_five_ALL` breaks the lower-case package rule.
- `MigrationModule implements APIExtension` adds nothing: the default scan already covers every `@Path` class.
- The chronology table in `migration_command.md` is incomplete.

## Documentation

- [migration_command.md](../../how-to/migration_command.md) : running a migration.
- [../architecture/main.md](../architecture/main.md) : the older description of the update mechanism.
- [../architecture/modules-overview.md](../architecture/modules-overview.md).
