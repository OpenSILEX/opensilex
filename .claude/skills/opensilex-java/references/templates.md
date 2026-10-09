# Templates: create a new concept

`assets/templates/` holds a complete, **compile-checked** CRUD concept (`Widget`, JDK 17, real `opensilex-core`
classpath, `-Xlint:all`: 0 errors, 0 warnings) and `scripts/scaffold.py` instantiates it. Prefer the scaffold over
writing from memory: it applies the naming, layering, annotation order, header and formatting rules of
`naming-and-style.md` for you.

```bash
# preview, then write (module defaults to opensilex-core; git user.email goes into the header)
python3 .claude/skills/opensilex-java/scripts/scaffold.py --concept SensorKit --prefix skit --dry-run
python3 .claude/skills/opensilex-java/scripts/scaffold.py --concept SensorKit --prefix skit \
        --parts model,filter,dao,dto,api,test            # add logic and/or migration when needed
```

| Part | Generated file | Template | Add when |
|---|---|---|---|
| `model` | `dal/SensorKitModel.java` | `dal/WidgetModel.java` | always |
| `filter` | `dal/SensorKitSearchFilter.java` | `dal/WidgetSearchFilter.java` | search with 3+ criteria (fluent setters, extends `org.opensilex.sparql.service.SearchFilter`) |
| `dao` | `dal/SensorKitDAO.java` | `dal/WidgetDAO.java` | always |
| `logic` | `bll/SensorKitLogic.java` | `bll/WidgetLogic.java` | a rule spans several DAOs/stores, needs a transaction or must be reused by API + CLI + migration |
| `dto` | `api/SensorKitDTO.java`, `...CreationDTO`, `...UpdateDTO` | `api/Widget*DTO.java` | always (add `...GetDTO`/`...DetailsDTO` only when the read view differs from the shared DTO) |
| `api` | `api/SensorKitAPI.java` | `api/WidgetAPI.java` | always |
| `test` | `src/test/java/<pkg>/api/SensorKitAPITest.java` | `test/WidgetAPITest.java` | always (CRUD helper + conflict + search) |
| `migration` | `opensilex-migration/.../SensorKitMigration.java` | `migration/WidgetMigration.java` | stored data changes (see `modules-migrations.md`) |

Useful options: `--module`, `--package`, `--plural` (irregular plurals), `--graph`, `--route`, `--ontology` (e.g.
`org.opensilex.security.authentication.SecurityOntology` outside `opensilex-core`), `--migration-name` (descriptive,
no date), `--out-root DIR` (write elsewhere, e.g. to review before copying), `--no-header`, `--force`.

## After scaffolding (the generator cannot do these)
1. **Ontology**: the class and every mapped property must exist in the module's OWL file
   (`src/main/resources/ontologies/`) and, for `Oeso`, as a constant in `org.opensilex.core.ontology.Oeso`. The test
   profile enables SHACL validation, so a model that does not match its ontology fails on create even though it compiles.
2. **Fields**: replace the sample `description` / `startDate` in the model, in `toModel`/`fromModel` of the DTO and in the
   test. Every model field must be copied in both directions, otherwise `update` silently erases it.
3. **Credentials**: the group label key (`credential-groups.sensor-kits`) must exist in the front language files; grant
   the new credentials to profiles, or non-admin users get 403 while the admin test passes.
4. **Header**: keep the generated block; edit the contact line only if the author is someone else.
5. **Verify**: compile with JDK 17, run the generated test, run `scripts/lint.py`, then do a review pass
   (`review-and-fix.md`):
   ```bash
   .claude/skills/opensilex-java/scripts/jdk.sh mvn -o -q -pl opensilex-core compile -DskipFrontBuild
   .claude/skills/opensilex-java/scripts/jdk.sh mvn -o -pl opensilex-core test -Dtest=SensorKitAPITest -DskipFrontBuild
   ```

## Variants (adapt the generated code, do not start from a blank file)
- **Named vs unnamed resource**: `SPARQLNamedResourceModel<T>` brings `name` (rdfs:label); use `SPARQLResourceModel` for
  nameless resources and `ResourceDTO<T>` instead of `NamedResourceDTO<T>`.
- **URI generation**: implement `ClassURIGenerator<T>` (`getInstancePathSegments`) on the model when the URI must not be
  derived from the name (see `ProjectModel`).
- **Hierarchies**: `SPARQLTreeModel<T>` (parent/children), see `ScientificObjectModel`, `DeviceModel`.
- **Relations**: typed fields (`List<OtherModel>`) with `@SPARQLProperty(... cascadeDelete / inverse / autoUpdate /
  ignoreUpdateIfNull)`; see `sparql-model-dao.md` for the update semantics before using them.
- **Eager fetching in lists**: build a `SparqlSchema` and call `searchWithPaginationUsingSchema` (`GroupDAO.search`).
- **MongoDB-backed concepts**: DAO takes `MongoDBService` (and `FileStorageService`), search filter extends
  `MongoSearchFilter`; examples `LocationObservationCollectionDAO`, `AnnotationDAO`; tests extend
  `AbstractMongoIntegrationTest` (`core/annotation/dal/AnnotationDAOTest`).
- **DAO tests**: integration-style (`AbstractMongoIntegrationTest` + `OpenSilexTestEnvironment`), no mocking of
  `SPARQLService`; see `testing.md`.
- **New module / config interface / `OpenSilexModule` wiring**: `modules-migrations.md`.
- **Concept already exists with older style** (e.g. `project`, `group`): imitate the template for new code, keep the
  neighbouring file's style for edits.
