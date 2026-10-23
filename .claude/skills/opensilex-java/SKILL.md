---
name: opensilex-java
description: Writes, reviews, fixes and lints Java code in the OpenSILEX repository so that it matches the team's conventions - naming (technical documentation page java-naming-conventions.md), layering (api/bll/dal), SPARQL models and DAOs, JAX-RS endpoints and DTOs, modules/config/migrations, JUnit 4 tests, formatting - and the Java release defined in opensilex-parent/pom.xml. Ships a scaffold for new concepts, a conventions linter, Checkstyle/SonarLint integration and JDK-safe Maven commands. Use whenever the user asks to create, add or modify Java here (new model, DAO, endpoint, DTO, migration, config, test), to review or critique a class, diff or PR, to fix review remarks or lint/Sonar findings, to run SonarLint or Checkstyle, or when a Maven/JDK build or compile fails. Also trigger on French requests such as "crée une classe", "ajoute un endpoint", "nouveau modèle", "fais une revue", "corrige", "lint", "sonar", "test d'intégration", "ça ne compile pas", even when OpenSILEX is not named.
---

# OpenSILEX Java

Multi-module Maven project. Business data lives in an RDF triplestore (RDF4J) reached through an in-house ORM-like layer
(`opensilex-sparql`); time series and files in MongoDB (`opensilex-nosql`, `opensilex-fs`); REST API = Jersey + Swagger
(javax namespace), from which a TypeScript client for the Vue front is generated at build time.

Names come from the technical documentation page
[`java-naming-conventions.md`](../../../opensilex-doc/src/main/resources/technical-documentation/architecture/java-naming-conventions.md)
(called "the naming page" below): packages, type suffixes, method prefixes, constants, JSON and REST names, RDF names,
tests, file header, branches and merge request titles. That page is the reference; this skill adds the formatting,
layering, API, persistence, test and build rules. When the page changes, align this skill with it.

**Principle: follow what the code does, not what a guide says.** CONTRIBUTING.md cites the Google Java Style, but the
code uses 4 spaces, `if (`, IntelliJ import order and 120-column habits. The naming page and
`references/naming-and-style.md` give the evidence and tell legacy from current.

Prerequisites: `git`, Python 3.8+ (the scripts use only the standard library), Maven, and a JDK matching the project
release (`scripts/jdk.py check` reports it). SonarLint's engine also needs a JDK 21+ (found automatically). The
scripts run on Linux, macOS and Windows; on Windows, type `py` (or `python`) where this skill writes `python3`.

## Start here
1. `scripts/jdk.py check` prints the project's Java release (`java.compiler.version`, **17**) and the JDK that matches
   it. The `java` first on PATH is often 11 and cannot compile `release 17`: run every Maven command through
   `scripts/jdk.py mvn ...`. Language and API choices must stay within that release.
2. Find the nearest sibling of what you touch (same concept: model, DAO, API, DTOs, test) and read it first.
3. Pick the mode:

| The user wants to... | Mode | Read |
|---|---|---|
| write a new class, concept, endpoint, DTO, migration, config, test | **CREATE** | `templates.md`, the naming page, `naming-and-style.md`, then the layer reference |
| review a class, a diff, a branch | **REVIEW** | `review-and-fix.md`, `lint.md`, the naming page for every new name |
| apply review remarks / lint / Sonar findings | **FIX** | `review-and-fix.md` section 5 |
| lint or "run sonar" | **LINT** | `lint.md` |
| build, test, JDK or compile problem | **BUILD** | section below |

All paths below are relative to `.claude/skills/opensilex-java/`; run the scripts from the repository root.

## CREATE
1. New concept: `python3 .claude/skills/opensilex-java/scripts/scaffold.py --concept Xxx --prefix xx --dry-run`, then
   without `--dry-run`. It writes a model, search filter, DAO, three DTOs, API and API test that compile with JDK 17 and
   follow the conventions (header with the developer's git email included). Then do the manual steps listed in
   `references/templates.md` (ontology term, fields in both DTO directions, credentials, test data).
2. Edit of an existing class: smallest change that fits the neighbouring file; do not reformat what you do not change.
3. Layer references: SPARQL models/DAOs `references/sparql-model-dao.md`; endpoints/DTOs `references/rest-api.md`;
   modules, config interfaces, migrations `references/modules-migrations.md`; tests `references/testing.md`.
4. Verify before saying "done": `python3 .claude/skills/opensilex-java/scripts/lint.py`, compile the module, run the
   targeted test class. Report what ran and what did not.

## REVIEW
Scope = the files the user names, else the diff against develop. Run `lint.py` first (mechanical rules, changed lines
only), then walk the severity checklist of `references/review-and-fix.md` (data loss on update, missing `@ApiProtected`
/ `@ApiCredential`, `dal` importing `api`, N+1 on lazy proxies, transactions, swallowed exceptions, migration safety,
tests). Report findings as `SEVERITY path:line id problem -> fix`, most severe first, plus what you did not check. Legacy
problems on untouched lines are mentioned once as pre-existing.

## FIX
Minimal diffs in severity order. Mechanical findings: `python3 .claude/skills/opensilex-java/scripts/lint.py --fix`
(reported lines only). No drive-by reformatting, renames or version bumps (CONTRIBUTING.md). Re-run lint, compile, run
the targeted tests, and state the result.

## LINT
`python3 .claude/skills/opensilex-java/scripts/lint.py [paths] [--sonar] [--fix]`: project conventions + the project's
Checkstyle config (offline Maven) + SonarLint when installed. Rule catalogue, noise triage and SonarLint setup:
`references/lint.md`.

## BUILD and TEST
```bash
python3 .claude/skills/opensilex-java/scripts/jdk.py check                                            # release, JDK, Maven
python3 .claude/skills/opensilex-java/scripts/jdk.py mvn -o -q -pl opensilex-core compile -DskipFrontBuild   # one module, offline
python3 .claude/skills/opensilex-java/scripts/jdk.py mvn -o -pl opensilex-core test -Dtest=ProjectAPITest#testCreate -DskipFrontBuild
python3 .claude/skills/opensilex-java/scripts/jdk.py mvn -pl opensilex-core -am install -DskipTests -DskipFrontBuild   # after upstream changes
```
- `-DskipFrontBuild` skips the Vue/yarn build (almost always wanted for backend work); `-DskipTests=true` skips tests.
- Without `-am`, Maven compiles against the snapshot jars in `~/.m2`: after changing `opensilex-sparql`, `-main` or
  `-security`, re-install them or use `-am`, otherwise you chase phantom compile errors.
- Integration tests start an embedded RDF4J and an embedded MongoDB (no Docker): run the targeted class, not the module.
- CONTRIBUTING.md asks for `mvn clean install` before a review request; say plainly what you did not run.

## Conventions at a glance (names: the naming page; formatting and evidence: `references/naming-and-style.md`)
- **Layout**: `org.opensilex.<module>.<concept>.{api,bll,dal}`; `dal` never imports `api` or DTOs; packages lowercase.
- **Names**: `XxxModel`, `XxxDAO`, `XxxSearchFilter`, `XxxLogic` (bll), `XxxAPI`, `XxxGetDTO`/`XxxCreationDTO`/
  `XxxUpdateDTO`, `XxxConfig`, `XxxModule`, `XxxException`, `XxxUtils`; `DTO`, `API`, `DAO` in upper case; no `Impl`
  suffix, no `I` prefix; tests `XxxTest` (a name not matching surefire's defaults is silently never run). Constants
  `UPPER_SNAKE`, field-name constants `XXX_FIELD`, logger `LOGGER` (SLF4J). Anything else: the naming page.
- **REST**: `/<module>/<plural>` (underscores for multi-word), snake_case query params, `@ApiOperation("Imperative sentence")`
  without final period, annotation order verb > `@Path` > `@ApiOperation` > `@ApiProtected` > `@ApiCredential` >
  `@Consumes` > `@Produces` > `@ApiResponses`, batch lookup by `POST by_uris`, `javax.*` (never `jakarta.ws.rs`).
- **Java**: release 17. `var`, `Stream.toList()`, `List.of` are used; records only for small internal carriers; models are
  non-final POJOs with accessors; `throws Exception` is the norm on API/DAO/logic methods; `java.time`, not `Date`;
  JUnit 4 only. Details and what *not* to modernise: `references/java17-practices.md`.
- **Format**: 4 spaces, no tabs, K&R braces, `if (`, aim <= 120 columns (hard limit 150), explicit imports in IntelliJ
  order, final newline, English comments, no trailing blanks; license header block (form: the naming page) on **new**
  files only.
- **Git**: branch `feature/<name>` into `develop`, `hotfix/<name>` into `master`; commits and MR title
  `type(scope): message` in English (CI job `merge-request:check`, see the naming page); changelog entry in English or
  `ignore-changelog`; tests and docs expected (`.gitlab/merge_request_templates`).

## Orient before editing
Modules (all `opensilex-*` at the repo root): `main` (module system `OpenSilexModule`, server, exceptions, test base
`AbstractIntegrationTest`), `sparql` (annotations, `SPARQLService`, mapping/proxies, `SparqlSchema`), `security`
(accounts, auth, credentials, `AbstractSecurityIntegrationTest`), `core` (the business concepts and the `Oeso`
vocabulary), `nosql`, `fs`, `migration`, `front` (Vue), `doc`, `release`, `dev-tools`, `parent` (versions), and integration
modules (`brapi`, `faidare`, `graphql`, `phis`). `opensilex-dataverse` exists but is not in the build.

Package layout of a concept (`opensilex-doc/.../opensilex-module/module-api-and-interface-extension.md`): `concept/api` (API class + DTOs) ->
optional `concept/bll` (rules spanning several DAOs/stores) -> `concept/dal` (model, DAO, search filter). Small complete
examples: `opensilex-security/.../security/group/`, `opensilex-core/.../core/annotation/`, `.../core/location/` (newest
layering with `bll` and a search filter). Older concepts (`project`, `group`) carry 2019-2021 idioms: imitate the
templates for new code.

## Traps
1. **Search noise**: worktree folders (such as `.claude/worktrees/`) hold full copies of the sources; `target/`,
   `node_modules/`, `graft/` hold generated files. Search tracked files only, scoped to a module:
   `git grep -n XxxDAO -- opensilex-core/src`.
2. **Never edit generated or ignored output**: `**/front/src/lib`, `**/front/types`, `target/`, `site/`. The TypeScript client
   is regenerated from Swagger: renaming an endpoint method, path, parameter or DTO field breaks `.vue/.ts` callers; tell
   the user what changes on the front side.
3. **Deprecations are real** (101 `@Deprecated`): do not copy deprecated neighbours (GET `by_uris`, positional `search`
   overloads, `getJson*ResponseAsAdmin` test helpers); use the replacement named in the Javadoc.
4. **`dal` objects are plain objects**: `new XxxDAO(sparql)`; services come from HK2 (`@Inject SPARQLService`), the caller
   from `@CurrentUser AccountModel`.
5. **Update semantics erase data**: `sparql.update(model)` removes every stored value whose model field is `null`; every
   model field must travel through the DTO both ways (`references/sparql-model-dao.md`).
6. **Migrations**: `getDate()` is documented as the creation date but the runner only logs it (17 of 18 migrations return
   `now()`); the real risks are non-idempotent logic and swallowed failures (`references/modules-migrations.md`).
7. **Secrets**: `~/.m2/settings.xml` and CI variables hold credentials (OSS Index, SonarQube token): never print or copy them.

## Files of this skill
`scripts/jdk.py` (JDK/Maven), `scripts/lint.py` (lint orchestrator), `scripts/scaffold.py` (new concept),
`scripts/sonarlint/` (SonarLint runner, see `references/lint.md`), `assets/templates/` (compile-checked Widget concept),
`references/`: `naming-and-style.md` (formatting, imports, injection, exceptions; names point to the naming page), `java17-practices.md`, `templates.md`, `review-and-fix.md`, `lint.md`,
`sparql-model-dao.md`, `rest-api.md`, `modules-migrations.md`, `testing.md`.
