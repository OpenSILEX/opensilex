# Review checklist: five axes

Each check has an id (cite it in the report), what to look for, how to see it (a `review_scope.py` lead id, a
command or what to read) and the norm it is measured against. Default severity is in the last column; adjust it to the
actual impact (`report.md`). Judge **changed lines**; a legacy problem on untouched lines is mentioned once as
"existant" unless the change makes it worse.

Norms, in this order of authority:
1. the siblings listed by `review_scope.py`, or methods of the same role found with `git grep`: what the team does;
2. `opensilex-doc/src/main/resources/technical-documentation/opensilex-front/component-guidelines-template.md` (Vue),
   the `opensilex-java` skill references (Java), `CONTRIBUTING.md`;
3. the team workflow documents (separate GitLab repository `OpenSILEX/opensilex-dev-tools`, `docs/workflow/`, not the
   `opensilex-dev-tools` Maven module of this repository). Their rules that matter for a review are summarised below;
   you do not need the repository.

A change that applies the *current* guideline where siblings are legacy (a new Vue component with the guideline
regions, typed, `readonly` props) is not a homogeneity finding: it is the goal.

---

## 0. Correctness (CORR), in the changed code
Not one of the five axes, but a reviewer who sees it must report it first. Hunt it in the changed code only.

| id | Look for | How to see it |
|---|---|---|
| CORR-1 | The change does not do what its name, MR or test claims, for a realistic scenario (existing production data, inverse relations, empty lists, a non-admin user) | trace one scenario through the new code; read the models it touches (`inverse = true` relations are stored on the other resource) |
| CORR-2 | A built query does not filter what it says (variable passed as a string instead of a `Var`, missing `?`, wrong graph) | print or build the query (Jena builder) and read the generated SPARQL; compare with the neighbouring method that works |
| CORR-3 | Validation after writes: data deleted or modified, then a check fails, leaving a half-done state | order of calls inside the transaction; the check must run before any write |
| CORR-4 | Java blockers of the `opensilex-java` REVIEW list (update erasing data, missing `@ApiProtected`, injection, swallowed failures) | `opensilex-java/references/review-and-fix.md` |

## 1. Maintainability (MAINT)

| id | Look for | How to see it | Norm / fix | Sev. |
|---|---|---|---|---|
| MAINT-1 | **Duplication**: a block copied from another place of the diff or of the codebase; a helper re-implemented | lead `MAINT-DUPLICATED-BLOCK`; for each new private helper, search the behaviour (code graph `search_graph`, `find_usages`, `grep -rn` in the module) | extract a method, or reuse the existing helper (list below) | Important |
| MAINT-2 | **Responsibility in the wrong layer**: business rule or multi-DAO orchestration in an `*API` method; `dal` importing `api`/DTOs; Vue component shaping data that a service should | read API methods > ~30 lines; `opensilex-java` lint `ARCH-DAL-API` | `api -> bll -> dal` (`opensilex-java/references/templates.md`, `WidgetLogic`) | Important |
| MAINT-3 | **Long or multi-purpose method** touched by the change | leads `MAINT-LONG-METHOD` (> 80 lines), `CLAR-DEEP-NESTING` | split along the steps the method already has; never demand a refactor of untouched legacy | Suggestion (Important when the change adds > 30 lines to it) |
| MAINT-4 | **Error handling**: empty `catch`, `printStackTrace`, `System.out`, generic `RuntimeException` ending as HTTP 500, log-and-rethrow twice, a user-facing refusal with raw English text and URIs; front promise without `.catch(this.$opensilex.errorHandler)` | leads `MAINT-EMPTY-CATCH`, `MAINT-STDOUT`; read thrown messages and service calls in `.vue/.ts` | typed exceptions of `org.opensilex.server.exceptions` (`NotFoundURIException`, `ForbiddenURIAccessException`...); a refusal shown to users: `DisplayableResponseException` with an i18n key, status documented in `@ApiResponses` (`ExperimentAPI.checkExperimentDontContainDatasBeforeDeletion`); front `.catch(this.$opensilex.errorHandler)` | Important |
| MAINT-5 | **Dead or temporary code**: commented-out code, unused private members/imports, `TODO` without card, `setTimeout` used to wait for rendering | leads `MAINT-COMMENTED-CODE`, `CLAR-TODO`, `MAINT-SETTIMEOUT`, `MAINT-SUPPRESS` | delete it (git keeps history); a TODO names its Trello card | Suggestion |
| MAINT-6 | **New code on a deprecated path**: calling or copying a `@Deprecated` method or endpoint (GET `by_uris`, positional `search` overloads, old test helpers) | read the Javadoc of the called methods; `grep -n "@Deprecated"` in the callee file | the replacement named in the Javadoc | Important |
| MAINT-7 | **Hidden contract change**: renamed/removed endpoint, path, query param or DTO field (the TypeScript client is generated from Swagger); model field without its DTO round trip | "endpoint removed" in the Tests section; diff of `*DTO.java`; `grep -rn "<javaMethodName>(" opensilex-front/front/src` | list the `.vue/.ts` callers to update; DTO `fromModel`/`toModel` both ways | Bloquant when a caller breaks |
| MAINT-8 | **Hand-edited generated file** (`front/src/lib`, `front/types`, `target/`) | lead `MAINT-GENERATED-EDIT` | regenerate from the Java side | Bloquant |
| MAINT-9 | **Hard-coded values**: ontology URIs as strings, magic numbers, user-visible text outside i18n, environment URLs | lead `HOMO-URI-LITERAL`; read templates for raw text | `Oeso` / `RDFS` / `FOAF` constants; named constants; `$t(...)` keys | Important |
| MAINT-10 | **Signature cascade**: a dependency added to the constructor of a shared class (DAO, Logic) and passed through dozens of callers that do not use it; parameter order and names drifting between them | lead `MAINT-SIGNATURE-CASCADE` (mechanical files); find the one method that needs the dependency | give the dependency only to the path that uses it (a dedicated constructor or method parameter), as `ExperimentAPI.checkExperimentDontContainDatasBeforeDeletion` does | Important |

Helpers that exist and are often re-implemented (verified in the code; check the current signature before citing):
- Java: `SPARQLService#getListByURIs` / `loadListByURIs` (batch fetch), `SparqlSchema` + `searchWithPaginationUsingSchema`
  (related objects in one query), `ListWithPagination`, `PaginatedListResponse` / `SingleObjectResponse` /
  `ObjectUriResponse` / `ErrorResponse`, `SPARQLDeserializers.compareURIs` / `formatURI`, `URIDeserializer`, Caffeine
  caches (`OrganizationDAO`, `DataImportLogic`).
- Front: `this.$opensilex.getService(...)`, `errorHandler`, `showSuccessToast` / `showErrorToast`; shared components
  `opensilex-InputForm`, `opensilex-ModalForm`, `opensilex-FormField`, `opensilex-PageContent` / `PageHeader` /
  `PageActions`, `opensilex-TableAsyncView` / `TableView`, `opensilex-StringFilter`, `opensilex-SearchFilterField`,
  `opensilex-UriLink`, `opensilex-CreateButton` / `EditButton` / `DeleteButton` / `DetailButton`, `opensilex-DateForm`.

## 2. Homogeneity (HOMO)

| id | Look for | How to see it | Norm / fix | Sev. |
|---|---|---|---|---|
| HOMO-1 | **Names and placement** unlike the siblings: suffixes (`XxxModel/DAO/SearchFilter/Logic/API/DTO/CreationDTO/UpdateDTO/GetDTO`), package `concept/{api,bll,dal}`, test class ending in `Test`, Vue file name = class name | "Compare with" section; `opensilex-java/scripts/lint.py` (`NAME-*`, `ARCH-*`) | `opensilex-java/references/naming-and-style.md` | Important (a test class not ending in `Test` never runs: Bloquant) |
| HOMO-2 | **Same problem solved differently** from the siblings: pagination params (`page`, `page_size`, `order_by`), response wrappers, exception types, search filter object, DTO conversion (`fromModel`/`toModel`), credential annotations, batch lookup by `POST by_uris` | open the sibling of the same layer, compare method by method | align on the sibling unless the change follows a newer documented pattern | Important |
| HOMO-3 | **Java mechanics**: formatting, imports, annotation order, `javax.*` (never `jakarta.ws.rs`), logger, header | `python3 .claude/skills/opensilex-java/scripts/lint.py --base <base>` (changed lines only) | do not re-check by eye; report the lint output grouped | Détail (lint `error`: Important) |
| HOMO-4 | **Vue guidelines** on new or substantially changed components: no `any`, `private`/`public` on every member, `readonly` on `@Prop`/`@Ref`/plugins, `//#region` groups, events emitted through `emitXxx()` methods, handlers `onXxx`, watchers `onXxxChange` | leads `HOMO-TS-ANY`, `HOMO-PROP-NOT-READONLY`, `HOMO-RAW-EMIT`; read the class | `component-guidelines-template.md`; on a legacy component, only the members the change touches | Important on new components, Suggestion on legacy ones |
| HOMO-5 | **i18n**: visible text through `$t`, every key in `en` and `fr`, key style of the neighbouring keys (component `<i18n>` block `Component.kebab-key`, or `lang/message-*.json`) | leads `HOMO-I18N-PARITY`, `HOMO-I18N-INVALID`; read templates for raw text | add the missing translation; move raw text to a key | Important |
| HOMO-6 | **Shared components** bypassed by raw Bootstrap/HTML for forms, tables, buttons, modals | read the template; compare with a sibling view | the `opensilex-*` components above | Suggestion |
| HOMO-7 | **Dates and URIs**: `java.util.Date` / `SimpleDateFormat` in new code; URIs compared as strings | lead `HOMO-LEGACY-DATE`; read comparisons | `java.time`; `SPARQLDeserializers.compareURIs` | Suggestion |
| HOMO-8 | **Git conventions**: commits `type(scope): message` in English, branch `feature/<dev>/[fix/]<name>` | `git log --format=%s <base>..<head>` | `CONTRIBUTING.md`, `opensilex-mr` skill | Détail |

## 3. Clarity (CLAR)

| id | Look for | How to see it | Norm / fix | Sev. |
|---|---|---|---|---|
| CLAR-1 | **Names** that do not say what the thing is or does (`data`, `tmp`, `list2`, `handle()`), booleans not phrased as a question, abbreviations outside the domain vocabulary (scientific object, germplasm, facility, experiment, provenance...) | read every new identifier | rename; use the ontology and UI vocabulary | Important on public API, Suggestion inside a method |
| CLAR-2 | **Control flow** hard to follow: four nested blocks, flag parameters, long boolean expressions, `else` after `return` | lead `CLAR-DEEP-NESTING` | guard clauses, extracted predicate methods | Suggestion |
| CLAR-3 | **Comments**: a comment explaining *what* unclear code does, a missing *why* on a non-obvious choice, stale comments, no Javadoc/JSDoc on a public method with edge cases | read comments around changed code | team rule: clear names over comments; Javadoc for behaviour and edge cases | Suggestion |
| CLAR-4 | **Logs and messages**: `console.log` left behind, SLF4J without `{}`, error messages a user cannot act on, French in code identifiers or logs | lead `CLAR-CONSOLE-LOG`; read `LOGGER.` calls and thrown messages | `LOGGER.debug("... {}", x)`; `console.debug` for intentional front debug; English in code, i18n for users | Suggestion (`console.log`: Détail) |
| CLAR-5 | **Readability of the change itself**: reformatting mixed with functional changes, unrelated drive-by edits, one MR doing several things, files > 400 changed lines | leads `CLAR-REFORMAT-MIXED`, `CLAR-BIG-FILE-CHANGE`; review those files with `git diff -w` | CONTRIBUTING: never mix reformatting and functional change; split the MR | Important |

## 4. Test coverage (TEST)

| id | Look for | How to see it | Norm / fix | Sev. |
|---|---|---|---|---|
| TEST-1 | **Every new or changed endpoint is exercised** by an API test (`XxxAPI.class.getMethod("method", ...)`), including the endpoint whose behaviour changes through the logic it calls | Tests section, leads `TEST-ENDPOINT-UNTESTED`, `TEST-METHOD-UNTESTED` (a declared but unused `DELETE_PATH` is not a test) | `opensilex-java/references/testing.md`, template `WidgetAPITest` | Bloquant for a new endpoint, Important for a changed one |
| TEST-2 | **Failure paths** of the changed services: 400 (validation), 403 (credential / private resource), 404 (unknown URI), 409 (duplicate) | read the test methods | dev workflow: test normal behaviour, wrong parameters and particular states | Important |
| TEST-3 | **A bug fix has a regression test** reproducing the reported scenario, failing without the fix | compare the test with the bug; ask the author if unclear | MR checklist "Tests écrits et OK" | Important |
| TEST-4 | **Logic without API surface** (DAO query, `bll` rule, `SPARQLService`/`MongoDBService`, CSV import/export, utils) has a test: new public methods, and public methods whose private helpers changed | leads `TEST-METHOD-UNTESTED`, `TEST-CLASS-UNREFERENCED`; "public method" lines of the test map | such tests exist (`SiteDAOTest`, `DataImportLogicTest`, `MongoReadWriteDaoTest`) | Important |
| TEST-5 | **Migration** (`OpenSilexModuleUpdate`) tested on data and on empty data, safe to re-run | `layer=migration` in the Files table; `opensilex-migration/src/test` | `opensilex-java/references/modules-migrations.md` | Important |
| TEST-6 | **Test quality**: asserts content, not only status; `getModelsToClean()` lists every written model; no `Thread.sleep`; no `@Ignore` without reason; independent tests; builders reused | lead `TEST-SLEEP`; "@Ignore added" in the Tests section; read assertions | `AbstractSecurityIntegrationTest` / `AbstractMongoIntegrationTest` helpers (`technical-documentation/architecture/tests.md`) | Important |
| TEST-7 | **Front**: there is no automated front test (no Jest, Vitest or Playwright in `opensilex-front/front/package.json`) | - | ask for manual test steps (MR "Comment tester"); a `data-testid` on new interactive elements prepares Playwright | Question / Suggestion |
| TEST-8 | **Measured coverage of the changed lines**, when the static map leaves doubt or the user asks | `coverage.md` (`coverage_changed.py`) | name the uncovered changed branches | Important when an uncovered branch carries business logic |

## 5. Performance (PERF)

The team's optimisation rules (dev-tools `good-practices/optimization_good_practices.md`) made concrete for this code:

| id | Look for | How to see it | Norm / fix | Sev. |
|---|---|---|---|---|
| PERF-1 | **One query per item (N+1)**: DAO / `SPARQLService` / Mongo call inside a loop or a stream `map`, a front call per row, a lazy proxy dereferenced in a loop (`model.getOwner().getName()`) | leads `PERF-QUERY-IN-LOOP`, `PERF-CALL-IN-LOOP`; read loops over search results | batch: `getListByURIs` / `loadListByURIs`, `SparqlSchema` fetch, Mongo `$in`; front: one `POST by_uris` then a `Map` | Bloquant on list, search, import or export paths; Important elsewhere |
| PERF-2 | **Store work done in memory**: fetch everything then filter, paginate, sort or count in Java; an aggregation pipeline for what `distinct()` does | read DAO and logic methods returning lists | filters, `LIMIT/OFFSET`, `ORDER BY`, count query in the store; Mongo `distinct`, projections | Important |
| PERF-3 | **Algorithmic cost**: nested loops over two collections, `List.contains` / `indexOf` in a loop, repeated lookups by URI | read loops | a `Map<URI, X>` or `Set` built once (the team document's own example) | Important when the collections grow with user data |
| PERF-4 | **Memory**: whole result sets or files loaded at once (exports, imports, data series), large list copies | read import, export and data code | streaming, pagination, chunks | Important |
| PERF-5 | **Repeated identical calls**: the same reference data fetched per request or per component instance | read services and `created()` hooks | Caffeine cache (Java); cache in a service or the store (front) | Suggestion |
| PERF-6 | **Mongo index**: a new query on a field without index | read Mongo DAO filters; look for `createIndexes` / `getIndexes` in the DAO or module | add the index with the query | Important |
| PERF-9 | **Counting to test existence**: `count` of everything (data, files, SPARQL SELECT without `LIMIT`) when "at least one" is enough | read checks before deletion or validation | `countData(filter, new CountOptions().limit(1))` (`ExperimentAPI.checkExperimentDontContainDatasBeforeDeletion`), SPARQL `ASK` or `LIMIT 1` | Suggestion (Important on large collections) |
| PERF-7 | **Front rendering**: `v-for` without `:key`, `deep: true` watchers on large objects, heavy computed recalculated per keystroke, everything loaded upfront instead of on tab or click, a page-wide loader for a count | leads `PERF-VFOR-NO-KEY`, `PERF-DEEP-WATCH`; read hooks and watchers | keys, targeted watchers, lazy loading, local spinners | Suggestion (Important on list pages) |
| PERF-8 | **Not tried at volume**: a list, search, import, export or migration changed and nothing says it ran on the reference datasets | MR description; ask the author | dev workflow step 2: test on the optimisation server's reference databases | Question |
