# Review and fix

Two modes that chain: **review** produces a ranked list of findings, **fix** applies them with minimal diffs.

## 1. Scope
- The user names files/classes: review those. Otherwise review the diff:
  `git diff $(git merge-base HEAD origin/develop)` (committed + working tree) plus untracked files.
- Read the class **and its closest siblings** first (model + DAO + API + DTO + test of the same concept). A rule broken by
  every sibling is a legacy habit to mention, not a defect of the change.
- Judge the lines that changed. Legacy problems on untouched lines are reported once, as "pre-existing", and never fixed
  in the same change (CONTRIBUTING.md: do not mix reformatting and functional changes).

## 2. Procedure
1. `python3 .claude/skills/opensilex-java/scripts/lint.py` (changed lines only; add `--sonar` when SonarLint is
   installed, see `lint.md`). It covers the mechanical rules; do not re-check them by eye.
2. Walk the checklist below top-down, severity first. Open the callers (`find_usages`/`callers`) of any changed public
   signature, DTO field or endpoint: they ripple across modules and into the generated TypeScript client.
3. Check the MR checklist the team uses (`.gitlab/merge_request_templates`): tests written and passing, technical
   documentation when behaviour changes, changelog entry (English) or `ignore-changelog`, Vue components follow the
   front guidelines when `.vue` files change.
4. Report (section 4). Do not fix anything the user did not ask you to fix.

## 3. Checklist (flag in this order)

### Blocker: wrong behaviour, data loss, security
| # | Check | How to see it |
|---|---|---|
| B1 | **Update erases data**: an update path builds the model from a DTO that does not carry every persisted field; a field added to the model is missing from `toModel`/`fromModel`; `ignoreUpdateIfNull`/`cascadeDelete`/`inverse` used without understanding (`sparql-model-dao.md`) | compare model fields with DTO conversions |
| B2 | **Endpoint without authentication or credential**: missing `@ApiProtected`; PUT/DELETE without `@ApiCredential`; new credential not granted to any profile | lint `API-PROTECTED`, `API-CREDENTIAL` |
| B3 | **Layer break**: `dal` imports `api`/DTO; business rule written in an API method instead of `bll`; DAO built with a DTO | lint `ARCH-DAL-API`, `NAME-LAYER` |
| B4 | **Query built by string concatenation** with user input (SPARQL injection); regex/filter not escaped | read the DAO |
| B5 | **N+1**: list/search code dereferences lazy proxies (`getXxx().getName()` in a loop) instead of fetching with a `SparqlSchema` or using URIs only | read the loop body |
| B6 | **Multi-store write without transaction/rollback** (SPARQL + Mongo), half-transactions scattered across API methods | read `bll`/API methods |
| B7 | **Failure swallowed or mis-typed**: empty `catch`, `printStackTrace`, generic `RuntimeException` for a client error (becomes 500), `return null` hiding an error | lint `EXC-EMPTY-CATCH`, `LOG-STACKTRACE` |
| B8 | **Migration not safe to re-run / on empty data / swallowing its failure** (log-and-continue after rollback), no log of counts | read the migration |
| B9 | **Resource leak**: Mongo cursor/session, stream, file not closed (use try-with-resources) | read |
| B10 | **Mutation of an immutable result**: `Stream.toList()`/`List.of` result modified later (UnsupportedOperationException; Sonar S6204) | read callers of the list |
| B11 | **Release violation**: language/API newer than `java.compiler.version` | lint `JAVA-RELEASE` |

### Major: contract and maintainability
| # | Check |
|---|---|
| M1 | Endpoint documentation: `@ApiOperation` imperative sentence without final period, `@ApiResponses` for every status, `@ApiParam` on parameters, annotation order (`rest-api.md`) |
| M2 | Parameters: snake_case query params (lint `API-QUERYPARAM-CASE`), `order_by`/`page`/`page_size` defaults, `@Valid`/`@NotNull`/`@ValidURI`/`@Min` validation, batch lookups by `POST by_uris` (never a new GET) |
| M3 | Naming/layout: suffixes (`XxxModel/DAO/SearchFilter/Logic/API/...DTO`), package lowercase, `*_FIELD` constants used instead of string literals, constants UPPER_SNAKE |
| M4 | Wrong exception type or HTTP status (404 vs 409 vs 400), error messages not actionable |
| M5 | Tests: new behaviour covered, failure paths (403/404/409/400) covered, `getModelsToClean()` lists every written model, JUnit 4 only, name ends with `Test`, no `Thread.sleep`, no `@Ignore` without reason |
| M6 | Breaking change for the front: renamed endpoint method/parameter/DTO field (the TypeScript client is generated from Swagger): list the `.vue/.ts` callers or tell the user |
| M7 | Documentation: public API/behaviour change documented in `opensilex-doc`; changelog entry; migration listed in `how-to/migration_command.md` |

### Minor: style and Java practices (only on changed lines)
Formatting and hygiene (lint `FMT-*`, `IMP-*`, `HDR-MISSING`), Javadoc on public API, logging with `{}` placeholders,
legacy idioms in new code (`java.util.Date`, `Optional` as field/parameter, `final` parameters, `Arrays.asList`),
modernisation opportunities that preserve behaviour (`var`, `.toList()` for read-only results, `instanceof` pattern,
text block for multi-line SPARQL/JSON in tests): see `java17-practices.md` before suggesting a rewrite.

## 4. Report format
Group by severity, most severe first, one line per finding, then what you did **not** check:
```
BLOCKER  opensilex-core/.../WidgetDAO.java:57  B5 N+1: search() reads model.getOwner().getName() per row
         -> fetch the owner with a SparqlSchema node (see GroupDAO.search) or expose owner URIs only
MAJOR    opensilex-core/.../WidgetAPI.java:92  M2 GET by_uris added: breaks on long URI lists -> POST by_uris
MINOR    WidgetDTO.java:14  FMT-KEYWORD-SPACE (fixable with lint.py --fix)
Not checked: runtime behaviour (no test run), front callers of renamed endpoint, SonarLint (not installed).
```
If there is nothing to report in a severity, say so; never pad. Credit what follows the conventions only when it
changes the user's decision.

## 5. Fix
1. Fix in severity order; one concern per commit-sized step. Keep the diff minimal: **no drive-by reformatting, no
   renames of untouched code, no dependency or version changes**.
2. Mechanical findings: `python3 .claude/skills/opensilex-java/scripts/lint.py --fix` (tabs, trailing blanks, final
   newline, `if (` spacing; changed lines only). Everything else by hand.
3. Recipes:
   - B1 -> add the field to the DTO (`WidgetDTO`: field, getter/setter with `@ApiModelProperty`, `toModel`, `fromModel`),
     to the creation/update DTOs when mandatory, and to the test.
   - B2 -> `@ApiProtected` (+ `@ApiCredential(credentialId, credentialLabelKey)` on writes); reuse
     `credential.default.modification` / `credential.default.delete` labels; add the group with `@ApiCredentialGroup`.
   - B3 -> move the shared type to `dal`, convert in the DTO (`fromModel`/`toModel`); move rules to a `bll` class
     (`templates.md`, `WidgetLogic`).
   - B5 -> `SparqlSchema` + `SparqlSchemaNode`/`SparqlSchemaRootNode` and `searchWithPaginationUsingSchema` (`GroupDAO`).
   - B6 -> `bll` method owning `startTransaction/commitTransaction/rollbackTransaction(e)` and rethrowing.
   - B7 -> typed exception from `org.opensilex.server.exceptions`, or `ErrorResponse` with the right status; log once.
   - B10 -> `new ArrayList<>(stream.toList())` or `Collectors.toList()`.
4. Verify after fixing, and say exactly what ran:
   ```bash
   python3 .claude/skills/opensilex-java/scripts/lint.py                       # changed lines clean?
   .claude/skills/opensilex-java/scripts/jdk.sh mvn -o -q -pl <module> compile -DskipFrontBuild
   .claude/skills/opensilex-java/scripts/jdk.sh mvn -o -pl <module> test -Dtest=<TestClass> -DskipFrontBuild
   ```
   Changing `opensilex-sparql`, `-main` or `-security`: re-install them (or use `-am`) before compiling downstream
   modules, otherwise stale snapshot jars in `~/.m2` produce phantom errors.
5. Final message: what was fixed (finding ids), what was not (and why), what was compiled/tested and what was not.
