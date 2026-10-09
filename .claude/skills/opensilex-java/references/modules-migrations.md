# Modules, configuration, ontologies, migrations

Docs: `opensilex-doc/src/main/resources/technical-documentation/architecture/{code-organization,module}.md`, `.../how-to/migration_command.md`, `.../how-to/database_upgrade.md`.

## Contents
- Module class
- Module structure on disk
- Configuration interface
- Registration and wiring
- Where dependencies go
- Data migrations

## Module class

A module extends `org.opensilex.OpenSilexModule` and opts into capabilities by implementing extension interfaces. `CoreModule` is the reference:

```java
public class CoreModule extends OpenSilexModule implements APIExtension, SPARQLExtension,
        JCSApiCacheExtension, ModuleWithNosqlEntityLinkedToAccount, SwaggerExtension {

    @Override public Class<?> getConfigClass() { return CoreConfig.class; }
    @Override public String getConfigId() { return "core"; }

    @Override
    public List<String> getPackagesToScan() {
        List<String> list = APIExtension.super.getPackagesToScan();   // keep the default scan
        if (getConfig(CoreConfig.class).enableLogs()) { list.add("org.opensilex.core.logs.filter"); }
        return list;
    }
    ...
}
```

- `APIExtension` - exposes REST resources found by package scan. Put API classes under the module's base package so the default scan finds them; add extra packages (filters, schedulers) in `getPackagesToScan()` like `CoreModule` does.
- `SPARQLExtension` - the module contributes ontologies via `getOntologiesFiles()`; files live in `src/main/resources/ontologies` of the module.
- `SwaggerExtension` - additional Swagger model classes (`getAdditionalSwaggerDefinitions()`).
- Lifecycle hooks inherited from `OpenSilexModule`: `install(boolean reset)`, `check()`, `setup()`, `clean()`, `startup()`, `shutdown()`. What `CoreModule` does in each: `setup()` registers the SPARQL prefixes (`SPARQLService.addPrefix(Oeso.PREFIX, Oeso.NS)`) and datatype deserializers; `install(reset)` inserts default data (`insertDefaultProvenance`, `insertDefaultVariablesGroup`, ...) and runs at install time, not on every start; `startup()` registers MongoDB indexes and creates them only outside the test and reserved profiles. Keep each kind of work in its matching hook.
- Read config with `getConfig(MyConfig.class)`; read module resources with `getFileInputStream(...)`, `listResourceDirectory(...)`.

## Module structure on disk

```
my-module/
  pom.xml                          artifactId/groupId, dependencies
  src/main/java/org/example/mymodule/
      MyModule.java  MyConfig.java
      concept/{api,bll?,dal}/
      cli/                         OpenSilexCommand implementations (optional)
  src/main/resources/
      ontologies/                  OWL files for the module's concepts
      config/(dev|prod|test)/opensilex.yml   per-profile config overrides (optional)
  src/test/java/...                API tests (integration) and DAO tests (unit)
  front/                           Vue sources, theme, lang files (optional)
```

Keep modules coherent and minimise dependencies between them; a new concept can instead be added as a package in an existing module.

## Configuration interface

Configuration is a Java interface whose methods are the settings; each method carries `@ConfigDescription` with a description and, where sensible, a default (`defaultBoolean`, ...). Nested interfaces model nested YAML sections (`CoreConfig.metrics()`).

```java
public interface CoreConfig {
    @ConfigDescription(value = "Activate access logs by user", defaultBoolean = false)
    boolean enableLogs();
}
```

Give every setting a default that lets the app start without extra configuration; a service that cannot work without configuration must check it and report a clear error at startup (architecture doc, "Service").

User-facing YAML lives under the module's `getConfigId()`. Development config for the dev server: `opensilex-dev-tools/src/main/resources/config/opensilex.yml`; test profile: `opensilex-main/src/main/resources/config/test/opensilex.yml`.

## Registration and wiring

Modules and CLI commands are discovered with `ServiceLoader` (`OpenSilexModuleManager` loads `OpenSilexModule`). The `META-INF/services` files are generated at build by `serviceloader-maven-plugin`, configured in `opensilex-parent/pom.xml` for `org.opensilex.OpenSilexModule` and `org.opensilex.cli.OpenSilexCommand` - do not hand-write them. To include a new module in the product, add it to the `<modules>` list and to the dependency list of the root `pom.xml`, which is where `opensilex-phis`, `opensilex-brapi`, `opensilex-faidare` and `opensilex-graphql` are wired today; copy one of those. `opensilex-dataverse` is currently commented out in the root pom, so it is not a good model for wiring.

## Where dependencies go

Common libraries needed by several modules: `opensilex-parent/pom.xml` (dependency management, plugin versions). Libraries specific to one module: that module's `pom.xml`. Do not add a version to a module pom if the parent already manages it.

## Data migrations

Use a migration when a code change alters stored data (new mandatory property, renamed predicate, moved graph, Mongo collection change). Existing instances will otherwise break on upgrade.

Two ways to write one, in `opensilex-migration/src/main/java/org/opensilex/migration/`:

1. Implement `org.opensilex.update.OpenSilexModuleUpdate`: `getDate()`, `getDescription()`, `setOpensilex(OpenSilex)`, `execute() throws OpensilexModuleUpdateException`. Get services from the instance: `opensilex.getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class).provide()` and `MongoDBService.DEFAULT_SERVICE`.
2. Extend `DatabaseMigrationModuleUpdate` (it extends `AbstractOpenSilexModuleUpdate`) and override `sparqlOperation(...)` / `mongoOperation(...)`, with `applyOnSparql(...)` / `applyOnMongodb(...)` deciding whether each store needs the operation. It handles service retrieval and the execute skeleton for you.

Guidelines:
- `getDate()` is documented as the update's creation date ("for auto execution" in the interface Javadoc), but its only consumer today is a debug log line in `SystemCommands` (`run-update`, `SystemCommands.java:97`): nothing orders or skips migrations by it. `AbstractOpenSilexModuleUpdate` returns `OffsetDateTime.now()` and 17 of the 18 existing migrations inherit or repeat that; only `MongoCustomCoordinatesDataTypeUpdate` returns a fixed date. Prefer a fixed creation date (`OffsetDateTime.parse("2026-10-09T00:00:00+02:00")`) for new migrations so the log is meaningful, but do not spend review effort on it: the real risks are the ones below.
- Make the migration safe to run twice and safe on empty data - a past fix in this repo was needed because a germplasm-attributes migration broke when there were zero entries to migrate. Check preconditions, return early when there is nothing to do.
- Wrap partial work in a transaction (`sparql.startTransaction()` / `commitTransaction()` / `rollbackTransaction(...)`, `mongodb.startTransaction()`), and convert failures to `OpensilexModuleUpdateException`.
- Log what changed (counts) at INFO so an administrator can verify a run.
- Run it with `java -jar opensilex.jar [--CONFIG_FILE=<file>] system run-update <fully.qualified.MigrationClass>`, or from the IDE via `main` in `RunUpdate` (`opensilex-dev-tools`) with the class name as argument.
- Document it: add an entry to `opensilex-doc/src/main/resources/how-to/migration_command.md` (list and description sections) and mention it in `CHANGELOG.md` when releasing.
- Naming: a descriptive verb phrase, **no date in the class name** (`AgentsMigrateToAccountAndPersons`, `MetadataMigration`, `MigrateToOnePointFive`); a release's migrations may share a sub-package (`one_point_five_ALL`; new packages should be all lowercase). `scaffold.py --parts migration` generates the skeleton (`assets/templates/migration/WidgetMigration.java`, compile-checked).
- Prefer `extends DatabaseMigrationModuleUpdate` (the base class owns service retrieval and the `execute()` skeleton; you implement `getDescription()`, `applyOnSparql`/`sparqlOperation` and/or `applyOnMongodb`/`mongoOperation`). `AbstractOpenSilexModuleUpdate` already provides `protected final Logger logger`. The older style `implements OpenSilexModuleUpdate` with hand-written services is only worth keeping when editing an existing class.
- Do not copy `UpdateOntologyContexts`: it rolls back, logs, and then swallows the exception, so the run reports success on failure. Rethrow as `OpensilexModuleUpdateException`.
