# Naming and style (measured on the code, not copied from a guide)

## Contents
1. Naming: the naming page, and what it leaves to this file
2. Formatting: rules table, imports, license header
3. Documentation and comments
4. Dependency injection and object creation
5. Exceptions and logging
6. What a linter can check mechanically

Evidence base: the Java files of the built modules, main and test. "Recent" = files touched since 2025-01-01,
"new" = files added since 2025-01-01. Labels used below:

- **MUST**: enforced by tooling or CONTRIBUTING.md (Checkstyle config in `opensilex-parent/pom.xml`, compiler, surefire).
- **DEFAULT**: what recent code does. Follow it in new code.
- **LEGACY**: exists in old files. Never copy it, never "fix" it in code you are not otherwise changing
  (CONTRIBUTING.md: do not mix reformatting and functional changes).

CONTRIBUTING.md points to the Google Java Style guide. **The code does not follow it** (4-space indentation, `if (`,
120-column habit, imports in IntelliJ order). Follow the code.

## 1. Naming

The reference for names is the technical documentation page
`opensilex-doc/src/main/resources/technical-documentation/architecture/java-naming-conventions.md` ("the naming page").
Read the section that matches what is being named before choosing a name. Its tags say how firm each rule is: `[P]`
prescribed by a document or enforced by the build, `[C]` convention followed by about 85 % of the code, `[T]` majority
tendency with the counts given; where the code has no majority, the page says "follow the neighbours".

| Naming... | Section of the naming page |
|---|---|
| a module package, a layer package (`api`, `bll`, `dal`) | Packages |
| a class: suffix per role, DTO form, acronym casing, layer match | Types |
| a DAO, logic or REST method, a DTO conversion, a boolean accessor, a setter | Methods |
| a constant, a credential constant, a logger, an enum constant, a type parameter | Fields and constants |
| a JSON property, a REST path, a query parameter, an `@ApiOperation` text | JSON and REST naming |
| a `@SPARQLResource`, a graph, a vocabulary term, a MongoDB field | RDF, ontology and MongoDB naming |
| a test class, a test method, a test resource | Tests |
| the header of a new file, Javadoc tags | File headers and documentation comments |
| a branch, a commit, a merge request title | Branches, commits and merge requests |
| every file of a new concept | New concept checklist |

The page also lists the known deviations (`Api`, `Dao` and `DaoV2` classes, camelCase packages, `organisation` against
`Organization*`, `CREDENTIAL_GROUP_<X>_ID`, ...). Never copy them in new code, and never rename them in code you are not
otherwise changing (a rename of a public type, method, path, parameter or DTO field also changes the generated
TypeScript client).

Two habits the page does not spell out:
- Field-name constants on SPARQL models (`public static final String START_DATE_FIELD = "startDate";`, next to the
  field) are used in DAO filters, ordering and `SparqlSchema` nodes instead of string literals. **DEFAULT** for new
  models; the linter reports a missing one (`MODEL-FIELD-CONSTANT`, info).
- Test methods are `test<Something>` or a descriptive sentence (`groupWithoutProfileHasEmptyListOfUserProfiles`): either
  is accepted, but do not mix the two styles inside one class.

The scaffold names the read DTO `<Concept>DTO`, the base that `<Concept>CreationDTO` and `<Concept>UpdateDTO` extend:
the page lists that form, while its rule for a new concept is `<Concept>GetDTO`. Keep the scaffold's name when the
three DTOs share that base; use `<Concept>GetDTO` for a read DTO that stands alone.

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
New files only, in the form given in the "File headers and documentation comments" section of the naming page.
`scripts/scaffold.py` writes it, with the developer's address from `git config user.email` and the creation time as
`Last Modification`. Never add, rewrite or "modernise" the header of an existing file (CONTRIBUTING.md: do not mix
reformatting and functional changes).

## 3. Documentation and comments
- English everywhere, comments, commit messages and changelog entries included (naming page).
- Javadoc is welcome (CONTRIBUTING.md) but not systematic: write it on public API methods and non-obvious logic, with
  `@param`, `@return`, `@throws`; `@see` to the DAO in API classes is a local habit. `@author` is optional, never
  invent a name; `@since`/`@version` are not used (naming page).
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
