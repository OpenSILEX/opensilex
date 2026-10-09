# Naming and style (measured on the code, not copied from a guide)

## Contents
1. Naming: packages and layers, classes, members
2. Formatting: rules table, imports, license header
3. Documentation and comments
4. Dependency injection and object creation
5. Exceptions and logging
6. What a linter can check mechanically

Evidence base (scanned 2026-10-09): 1,155 Java files (1,025 main + 130 test). "Recent" = files touched since
2025-01-01 (306), "new" = files added since 2025-01-01 (67). Labels used below:

- **MUST**: enforced by tooling or CONTRIBUTING.md (Checkstyle config in `opensilex-parent/pom.xml`, compiler, surefire).
- **DEFAULT**: what recent code does. Follow it in new code.
- **LEGACY**: exists in old files. Never copy it, never "fix" it in code you are not otherwise changing
  (CONTRIBUTING.md: do not mix reformatting and functional changes).

CONTRIBUTING.md points to the Google Java Style guide. **The code does not follow it** (4-space indentation, `if (`,
120-column habit, imports in IntelliJ order). Follow the code.

## 1. Naming

### Packages and layers
`org.opensilex.<module>.<concept>[.<layer>]`, layers `api` (REST + DTOs), `bll` (business logic, optional), `dal`
(models, DAOs, search filters). All lowercase (Checkstyle `PackageName`, Sonar S120).
**LEGACY**: 72 files sit in packages with capitals/underscores (`scientificObject`, `dataImport`, `schemaQuery`,
`one_point_five_ALL`, `multipleError`). They exist even in recent code; do not create new ones.
`dal` must not import `api` classes or DTOs (11 historical violations, e.g. `GermplasmDAO` imports a search filter that
lives in `api`; new code must not add more).

### Classes (counts = files in main code)

| Kind | Name | Count | Notes |
|---|---|---|---|
| SPARQL / Mongo model | `XxxModel` | 91 | `dal`, `@SPARQLResource` for RDF models |
| DAO | `XxxDAO` | 37 | `dal`; Mongo-backed ones also `...Dao`/`DaoV2` (legacy spelling) |
| Search criteria | `XxxSearchFilter` | 19 | `dal`; SPARQL: `extends org.opensilex.sparql.service.SearchFilter`; Mongo: `extends MongoSearchFilter` |
| Business logic | `XxxLogic` | 14 (all recent) | `bll`; this is the current name for service/BLL classes |
| REST resource | `XxxAPI` | 50 | `api` |
| DTOs | `XxxDTO` 124, `XxxGetDTO` 44, `XxxCreationDTO` 36, `XxxUpdateDTO` 26, `XxxDetailsDTO` 13, `XxxListDTO` 4, `XxxSearchDTO` 3 | | see `rest-api.md`; "detail" view = `...DetailsDTO` (13) rather than `...GetDetailDTO` (2) |
| Config | `XxxConfig` (interface) | 37 | `@ConfigDescription` on settings |
| Module | `XxxModule` | 16 | `extends OpenSilexModule` |
| Exceptions | `XxxException` | 57 | see section 5 |
| Services | `XxxService` + `XxxServiceFactory` | 17 / 7 | HK2 factory pattern |
| Utilities | `XxxUtils` | 9 | `Util` (1) and `Helper` (2) are minority: use `Utils` |
| Importers/exporters | `XxxImporter`, `XxxExporter` | 7 / 7 | |
| Migrations | descriptive verb phrase, **no date in the name** | 18 | `MigrateToOnePointFive`, `UpdateOntologyContexts` |
| Abstract classes | `AbstractXxx` | 10 of 48 | `Abstract` prefix is DEFAULT for new abstract classes |
| Tests | `XxxTest`, `XxxAPITest`/`XxxApiTest`, `XxxDAOTest` | 87 | MUST end with `Test`: surefire defaults pick up only `Test*`, `*Test`, `*Tests`, `*TestCase` (no custom includes in the pom), anything else is silently never run |

No `I` prefix on interfaces (0 of 0). No Hungarian prefixes (`m_`, `_x`): 0.

### Members
- Methods and fields: `lowerCamelCase`; 0 non-camelCase methods in main code. Booleans read as `isXxx`/`hasXxx`.
- Constants: `UPPER_SNAKE_CASE`. RDF vocabulary classes (`Oeso`, `SecurityOntology`) use Jena style
  (`public static final Property hasXxx`) - that is the only accepted lowerCamel constant family.
- Field-name constants on SPARQL models: `public static final String START_DATE_FIELD = "startDate";` next to the field
  (24 of 42 models; the one model added since 2025 has none). **DEFAULT**: define them and use them in DAO filters,
  ordering and `SparqlSchema` nodes instead of string literals.
- Graph constant: `public static final String GRAPH = "project";` referenced from `@SPARQLResource(graph = ...)`.
- DAO method families (414 methods): `get*` 143, `search*` 55, `create` 51, `update` 41, `delete` 38, `count*` 12,
  `is*`/`exists*` 11, `check*` 9, `validate*` 6. Reuse these prefixes.
- API method names: `createXxx`, `updateXxx`, `deleteXxx`, `getXxx`, `searchXxxs`, `getXxxsByURI`/`searchXxxsByURIs`,
  `countXxx`, `exportXxx`, `importXxx`.
- DTO conversion: instance `fromModel(model)`, `toModel(model)`, `newModel()`, `newModelInstance()`, static
  `getDTOFromModel(model)` (counts: 134 / 62 / 32 / 27 / 62).
- Credential constants in the API class: `CREDENTIAL_<CONCEPT>_GROUP_ID` / `..._GROUP_LABEL_KEY`,
  `CREDENTIAL_<CONCEPT>_MODIFICATION_ID` / `..._LABEL_KEY`, `CREDENTIAL_<CONCEPT>_DELETE_ID`; id values are kebab-case
  (`"project-modification"`), label keys are i18n keys (`"credential.default.modification"`).
- Wire format: Java fields stay camelCase, JSON is snake_case through `@JsonProperty("rdf_type")`; **query parameters are
  snake_case** (572 vs 30 camelCase outside BrAPI/FAIDARE). BrAPI/FAIDARE follow their external specs (camelCase 31 vs
  snake_case 18): keep the spec's names there.
- REST paths: `/<module>/<concept-plural>` lowercase, underscores for multi-word (`/core/uri_search`), nesting for
  sub-resources (`/core/experiments/factors/levels`), `{uri}` for the identifier, `by_uris` for batch lookups.
  One outlier lacks the leading slash (`core/sites`): do not copy.
- Loggers: SLF4J only (117 files; Log4j2 0). `private static final Logger LOGGER = LoggerFactory.getLogger(Xxx.class);`
  (about 95 of 105 declarations use the name `LOGGER`). Migrations inherit `protected final Logger logger` from
  `AbstractOpenSilexModuleUpdate`. Use `{}` placeholders, not string concatenation.
- Tests: `testXxx` in 76 % of methods, but about half of the new test methods are descriptive sentences
  (`groupWithoutProfileHasEmptyListOfUserProfiles`). Either is accepted; do not mix styles inside one class.

## 2. Formatting

| Rule | Evidence | Strength |
|---|---|---|
| 4 spaces, no tabs | 944 of 1,025 main files have 4-space members; 0 tab-indented lines in files added since 2025 (5,087 legacy tab lines); Checkstyle `FileTabCharacter` | MUST |
| K&R braces (`{` on the same line), `} else {` on one line | 6 Allman lines in main; 828 cuddled `else` vs 34 | DEFAULT |
| `if (`, `for (`, `while (`, `switch (`, `catch (` with a space | 4,680 vs 848 | DEFAULT (Checkstyle `WhitespaceAfter`) |
| No trailing whitespace | Checkstyle `RegexpSingleline` | MUST |
| File ends with a newline, LF endings, UTF-8, no BOM | 4 % of files miss the final newline | MUST |
| Line length: aim <= 120, never exceed 150 | 94 % of lines <= 100, 97 % <= 120, 99.2 % <= 150; Checkstyle `LineLength` max 150 | MUST (150) / DEFAULT (120) |
| One statement per line, braces on every `if/for/while` | Checkstyle `NeedBraces`, `MultipleVariableDeclarations` | MUST |
| Long signatures: one parameter per line, 8-space continuation, `)` back at member indentation | `GroupAPI`, `ProjectAPI` | DEFAULT |
| Annotation attributes: one per line, 8-space continuation, `)` on its own line | `@SPARQLResource(...)`, `@ApiCredential(...)` | DEFAULT |
| No `final` on method parameters | 360 uses, only 8 in recent files, 0 in new | LEGACY |
| `this.` prefix only where it disambiguates | 3,838 uses, mostly constructors/setters | DEFAULT |

### Imports
- **Explicit imports** in new code (Checkstyle `AvoidStarImport`, Sonar S2208). 369 star imports exist in main (IntelliJ
  collapses 5+ imports of a package; 264 of them are in recently touched files), so a `*` import in an old file is not
  a reason to rewrite it. Static star imports (`import static org.junit.Assert.*;`) are tolerated in tests.
- Order (IntelliJ default layout, the dominant signature: 366 files `other > java`, 113 files `other > javax > java`):
  all non-`java` imports alphabetically, blank line, `javax.*`, `java.*`, blank line, `import static ...`. 84 % of the
  recently touched files separate the groups with blank lines. Legacy NetBeans files have one flat block: leave them.
- Remove unused imports (Checkstyle `UnusedImports`, `RedundantImport`).
- Namespace: **`javax.ws.rs`, `javax.validation`, `javax.inject`** (113 / 146 files; 0 `jakarta` equivalents). The only
  `jakarta.*` imports are `jakarta.json.*` (3 files). Do not import `jakarta.ws.rs`/`jakarta.validation`.

### License header
CONTRIBUTING.md: "Include the OpenSILEX header at the top of each file". Reality: 25 % of main files have the old `//***`
header, 28 % a NetBeans "To change this license header" stub, 27 % nothing; of the 61 main files added since 2025,
22 carry the block below and 37 carry nothing. **New files get the block below** (it is what the team's IntelliJ
copyright profile generates; `Last Modification` is a creation timestamp, the contact line starts with the developer's
own address from `git config user.email`). Never add, rewrite or "modernise" headers of existing files.

```java
/*
 * *****************************************************************************
 *                         ClassName.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2026.
 * Last Modification: 09/10/2026 10:30
 * Contact: first.last@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr,
 * *****************************************************************************
 */

package org.opensilex.core.xxx.dal;
```

## 3. Documentation and comments
- English everywhere (3,177 English comment lines vs 3 French). Commit messages and changelog entries in English too.
- Javadoc is welcome (CONTRIBUTING.md) but not systematic: write it on public API methods and non-obvious logic, with
  `@param`, `@return`, `@throws`; `@see` to the DAO in API classes is a local habit. `@author` appears in 73 % of
  main files (30 % of new ones) and `@since`/`@version` never: optional, never invent a name.
- Checkstyle's Javadoc rules (`JavadocVariable`, `MissingJavadocMethod`, `JavadocType`, `JavadocStyle`, `JavadocMethod`)
  are configured but violated almost everywhere: ignore them when triaging.
- Swagger text is the user-facing documentation: `@ApiOperation("Imperative sentence")` without trailing period
  (353 of 362 have none), see `rest-api.md`.
- No commented-out code, no `TODO` without context (76 `TODO/FIXME` exist; say what and why).

## 4. Dependency injection and object creation
- API classes: `@Inject private SPARQLService sparql;` and `@CurrentUser AccountModel currentUser;` (package-private).
- DAOs are plain objects: `new XxxDAO(sparql)` inside the endpoint or the logic class (19 of 37 DAOs take only
  `SPARQLService`; Mongo-backed ones add `MongoDBService` and `FileStorageService`). `final class` DAOs exist (5): optional.
- Services are created by HK2 factories (`XxxServiceFactory`); do not `new` a service.

## 5. Exceptions and logging
- `throws Exception` is the project norm on API, DAO and logic methods (2,145 occurrences, 76 % of DAO methods).
  Keep that signature for consistency; narrow it only when a method is trivial and private.
- For expected business failures throw the typed exceptions of `org.opensilex.server.exceptions`
  (`NotFoundException`, `BadRequestException`, `ConflictException`, ...) or return an `ErrorResponse`; never throw a bare
  `RuntimeException("...")` for a client error (it becomes a 500).
- Custom exceptions: extend `Exception` (21), `SPARQLException` (15) or `WebApplicationException` (12) according to layer.
- Never leave an empty `catch` (41 legacy ones), never `printStackTrace()` (10) or `System.out/err` (13): log with
  SLF4J (`LOGGER.error("message", e)`).
- Catching `Exception` is common (272) at API boundaries; do not catch it to hide a failure in a DAO or migration.

## 6. What a linter can check mechanically
`scripts/lint.py` implements the rows marked MUST plus the layering/namespace/test-name rules above; see `lint.md`.
