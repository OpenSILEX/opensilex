# Technical documentation : [`data import`] Design patterns and coding conventions of `opensilex-ai-import`

**Document history (please add a line when you edit the document)**

| Date       | Editor(s)        | OpenSILEX version | Comment           |
|------------|------------------|-------------------|-------------------|
| 2026-09-26 | Arnaud Charleroy | BUILD-SNAPSHOT    | Document creation |
| 2026-09-26 | Arnaud Charleroy | BUILD-SNAPSHOT    | Memento for stored sessions, binding of created resources |
| 2026-09-27 | Arnaud Charleroy | BUILD-SNAPSHOT    | Data import through the template, `finish` hook           |
| 2026-09-27 | Arnaud Charleroy | BUILD-SNAPSHOT    | Coverage rule at 90 %                                     |
| 2026-09-29 | Arnaud Charleroy | BUILD-SNAPSHOT    | Link to the guide for adding a profile                    |

> This document explains **how the module is built**: the coding conventions it takes from the rest
> of OpenSILEX, and the design patterns it uses, each with where it lives, why it was chosen, and how
> to extend it. **What the module does** is described in [`ARCHITECTURE.md`](ARCHITECTURE.md).

## Table of contents

<!-- TOC -->
* [Technical documentation : [`data import`] Design patterns and coding conventions of `opensilex-ai-import`](#technical-documentation--data-import-design-patterns-and-coding-conventions-of-opensilex-ai-import)
  * [Table of contents](#table-of-contents)
  * [The rule every pattern serves](#the-rule-every-pattern-serves)
  * [Coding conventions taken from the platform](#coding-conventions-taken-from-the-platform)
    * [Java](#java)
    * [REST and DTOs](#rest-and-dtos)
    * [Messages](#messages)
    * [Front end](#front-end)
    * [Tests](#tests)
  * [Design patterns](#design-patterns)
    * [Overview](#overview)
    * [Chain of responsibility — the resolution order](#chain-of-responsibility--the-resolution-order)
    * [Gateway — `InstanceLookups`](#gateway--instancelookups)
    * [Template method — one skeleton, several variations](#template-method--one-skeleton-several-variations)
    * [Strategy — profiles, categories, selectors](#strategy--profiles-categories-selectors)
    * [Command and registry — the model's tools](#command-and-registry--the-models-tools)
    * [Adapter — the platform's validators](#adapter--the-platforms-validators)
    * [Result objects — outcomes instead of exceptions](#result-objects--outcomes-instead-of-exceptions)
    * [Repository — `CorrectionStore`](#repository--correctionstore)
    * [Builder — `ProposalBuilder`, `GeneratedCsv`](#builder--proposalbuilder-generatedcsv)
    * [Memento — `SavedSession`](#memento--savedsession)
  * [Patterns deliberately not used](#patterns-deliberately-not-used)
  * [Refactoring of 2026-09-26](#refactoring-of-2026-09-26)
  * [Checklists](#checklists)
    * [Adding a creation target](#adding-a-creation-target)
    * [Adding a report category](#adding-a-report-category)
    * [Adding a file family](#adding-a-file-family)
    * [Adding a bulk import through a platform importer](#adding-a-bulk-import-through-a-platform-importer)
    * [Adding state to a session](#adding-state-to-a-session)
<!-- TOC -->

## The rule every pattern serves

**The language model proposes; Java resolves, validates and writes; a person confirms.**

No URI ever originates from the model. Every pattern below exists to keep that boundary easy to
see and hard to cross:

- whatever reads the instance goes through the platform's own DAOs and logic classes, so the
  platform's access checks apply (see the [gateway](#gateway--instancelookups));
- whatever writes goes through the platform's own importers and logic classes, in one transaction
  (see the [template method](#template-method--one-skeleton-several-variations));
- whatever fails is returned as data the user can act on — a row, a column, a reason — never as a
  stack trace (see the [result objects](#result-objects--outcomes-instead-of-exceptions)).

## Coding conventions taken from the platform

The module is written to read like the rest of OpenSILEX. When in doubt, copy the nearest platform
class, not a pattern from elsewhere.

### Java

- **File header**: the AGPL licence block, then the class Javadoc ending with `@author`.
- **Javadoc says why, not what.** A method's name says what it does; its comment says the reason,
  the trade-off, or the trap. `ResolutionService.resolveScientificObjects` ends with the paragraph
  on what `checkUniqueNameByGraph` gives up, for instance — the kind of knowledge that is lost
  otherwise.
- **Regions**: `//#region name` … `//#endregion` group the methods of a class by step, as in the
  platform's own logic classes. A region holds one concern; helpers go last, in `//#region helpers`.
- **Named limits**: every limit is a constant with a Javadoc saying why it has this value and what
  happens when it is reached (`InstanceLookups.NEAR_MATCH_CANDIDATE_LIMIT`,
  `BulkValidationDTO.MAX_ERRORS`). Reaching a limit is reported to the user, never passed over.
- **Fluent setters** returning `this` on models and DTOs (`new ResolvedItem(name).setStatus(…)`),
  as the platform's DTOs do.
- **Visibility**: package-private by default for collaborators of one package
  (`InstanceLookups`, `VariableResolver`); `public` only for what another package uses.
- **Search filters**: the platform's `SearchFilter` subclasses, with `setPage(0)` and an explicit
  `setPageSize(limit)` — never an unbounded search.
- **Transactions**: `SparqlMongoTransaction` around anything that writes both stores; one
  transaction per confirmation.
- **Dependencies**: `SPARQLService`, `MongoDBService`, `FileStorageService` and the current
  `AccountModel` are passed to constructors, as in the platform's logic classes. A service is built
  per request; per-analysis state (`ConfirmedMatches`, learned corrections) lives in a field
  documented as such.

### REST and DTOs

- JAX-RS resources with the platform's Swagger annotations (`@ApiOperation`, `@ApiResponses`,
  `@ApiProtected`) and responses wrapped in `SingleObjectResponse` / `ErrorResponse`.
- **JSON is snake_case, Java is camelCase**: DTO fields stay camelCase and carry
  `@JsonProperty("snake_case")`.
- A DTO is built from its model by a static `fromModel` (or `of`) factory; the API never assembles a
  DTO field by field from business objects.
- The API layer checks, routes and wraps. It does not decide which operation serves a target —
  that is the service's job (`AiImportCreationService.apply`).

### Messages

Every message the user may read is a `ReportMessage`: a translation key, its parameters, and the
English sentence. The key feeds the interface (`AiImport.*` in `ai-import-fr.json` and
`ai-import-en.json`); the English feeds the prompt, where a key would mean nothing.
`TranslationKeyTest` fails when a key is emitted without a translation. Where the set of keys follows
an enum (`CreationTarget`, `RowError.Kind`), the test derives the keys from the enum, so adding a
constant without its translation fails the build.

### Front end

- **Reuse the platform's components.** Every core `.vue` file is registered globally as
  `opensilex-<Name>`; the module renders `opensilex-TypeForm`, `opensilex-VariableForm`, the
  selectors, rather than a second implementation of them.
- The module's own components are registered as `opensilex-ai-import-<Name>` in
  `front/src/index.ts`.
- Only what the platform exposes globally can be used without bundling it: `vue` is external, and so
  are the naive-ui components the core registers. A library the core does not expose (Tabulator) is
  not used, because it would be bundled a second time.

### Tests

- JUnit 4, test methods named as sentences (`aMisspeltFacilityIsSuggestedNotResolved`).
- Unit tests need neither a database nor a language model. `StubLlmEndpoint` stands for the model,
  an in-memory RDF4J store for SPARQL-only code.
- Integration tests extend the core's `AbstractMongoIntegrationTest` (RDF4J in memory, an embedded
  MongoDB) and declare what they clean: `getModelsToClean()` for the default graphs, `afterEach()`
  for the others (an experiment's graph, the corrections graph).
- **Coverage is a rule, not a wish**: 90 % of the module's lines, checked by `jacoco:check` under
  the `with-test-report` profile. A change that lowers it below comes with its tests.
- The REST API is tested through REST, with the language model replaced by `StubLlmEndpoint`
  replaying scripted tool calls: what the interface and the model actually exchange is what is
  tested.
- Seed resources the way the platform stores them. A germplasm label needs its language
  (`new SPARQLLabel(name, "en")`), or no `rdfs:label` is written and every search misses it — a
  failure that looks like a DAO bug and is not one.

## Design patterns

### Overview

| Pattern                  | Where                                                                   | Why                                                        |
|--------------------------|-------------------------------------------------------------------------|------------------------------------------------------------|
| Chain of responsibility  | `ResolutionService.resolve`                                             | one order of confidence for every name                     |
| Gateway                  | `InstanceLookups`                                                       | one place that reads the instance, under the user's rights |
| Template method          | `ResolutionService.resolveEach`, `PlatformBulkImport`                   | one skeleton, the variation in one method                  |
| Strategy                 | `ImportProfile`, `ReportCategory`, `CandidateSource`, front `SELECTORS` | behaviour chosen by data, not by a chain of `if`           |
| Command + registry       | `AiTool`, `ToolRegistry`, `ImportProfileRegistry`                       | the model calls tools by name                              |
| Adapter                  | `PlatformValidationAdapter`                                             | platform errors shown on the user's workbook               |
| Result object            | `CreationOutcome`, `BulkOutcome`                                        | a refusal is data, not an exception                        |
| Repository               | `CorrectionStore`                                                       | the learned corrections behind one API                     |
| Builder                  | `ProposalBuilder`, `GeneratedCsv`                                       | step-by-step assembly with checks                          |
| Memento                  | `SavedSession`, `AiImportSessionStore`                                  | leave a conversation and resume it days later              |

### Chain of responsibility — the resolution order

**Where**: [`ResolutionService.resolve`](../java/org/opensilex/aiimport/resolve/ResolutionService.java).

Every name the file gives goes down the same steps, in order of confidence, and stops at the first
one that settles it:

```
confirmed ─► exact ─► learned ─► near
  (user)    (DAO)   (taught)   (suggested only)
```

| Step      | Settles the item as                   | Source                                  |
|-----------|---------------------------------------|-----------------------------------------|
| confirmed | `FOUND`                               | `ConfirmedMatches`, this conversation   |
| exact     | `FOUND`, `AMBIGUOUS` or `MISSING`     | `InstanceLookups.…Named`                |
| learned   | `FOUND`, with the correction attached | `CorrectionStore`, re-read by `visible` |
| near      | stays `MISSING`, with suggestions     | `NearMatchFinder` over `candidates`     |

It is written as **ordered passes over the report**, not as linked handler objects. Two reasons:
the later steps work on *all* missing items of a category at once (one candidate list per category,
not per name), and experiments must finish every step before the plots can be looked up inside
them. A handler-per-name chain would hide both constraints; the passes make them read top to bottom
in `resolve`.

The one rule to keep: **a later step only ever sees what an earlier one left `MISSING`**
(`missingItems`). A correctly spelt name always wins over a learned correction; a learned correction
always wins over a suggestion.

### Gateway — `InstanceLookups`

**Where**: [`InstanceLookups`](../java/org/opensilex/aiimport/resolve/InstanceLookups.java).

All reads of the instance made by the resolution, behind three questions per kind of resource:

- `projectsNamed`, `germplasmNamed`, `facilitiesNamed`, `personsMatching` — exact matches;
- `visible(category, uri)` — the resource again, under the current user's rights, for what the
  instance was taught;
- `candidates(category)`, `germplasmCloseTo(name)` — what a misspelt name could have meant.

The gateway knows **how** to fetch; `ResolutionService` knows **in which order** and **what the
report says**. The gateway never writes, never builds a report line, and never queries around an
access check: facilities go through `FacilityLogic`, germplasm through the DAO that filters on the
user's groups. That is what stops a correction taught by one person from revealing to another a
resource they may not see.

`VariableResolver` is the same idea for variables, which are tried in more ways than the rest
(ontology identifier, name, shared resource instance, then their four components).

### Template method — one skeleton, several variations

**`ResolutionService.resolveEach`** — the confirmed and exact steps for every category recognised
by a name alone (projects, germplasm, persons, facilities). The skeleton — add the report line, try
the confirmation, run the lookup, turn a failure into a warning — is written once; each category
supplies only its lookup, as a method reference:

```java
resolveEach(report, ReportCategory.PROJECTS, plan.getProjectNames(), ResolvedItem::new,
        lookups::projectsNamed, "project", missingHint);
```

This is the functional form of the pattern: the varying step is an `ExactLookup` lambda rather than
an abstract method, because there is no state to share between the variations.

**`PlatformBulkImport`** — the classic form, for bulk imports through a platform importer:

```java
public final BulkOutcome validate(session, values)   // generate, module errors, platform check
public final BulkOutcome importAll(session, values)  // validate, then import only when clean
protected abstract GeneratedCsv generate(...);
protected abstract V validateWithPlatform(...);
protected abstract List<RowError> errorsOf(V validation, GeneratedCsv csv);
protected abstract BulkOutcome importWithPlatform(...);
```

The final methods guarantee the order every subclass must respect: the module's own refusals are
reported before the platform is asked, and nothing is imported without a clean validation. A hook,
`finish(written)`, runs once the run is over — even after an exception — for what a subclass must
release: `DataBulkImport` deletes there the provenance it created for the validation when nothing was
written. Two subclasses: `ScientificObjectBulkImport` and `DataBulkImport`.

### Strategy — profiles, categories, selectors

- **`ImportProfile`** — one implementation per file family (STAR, VitisExplorer, MIAPPE, generic).
  Each says how well it recognises a workbook (`match`) and how to read it (`extract`,
  `extractObjectRows`). `ImportProfileRegistry.select` picks the best score.
- **`ReportCategory`** — an enum carrying, per category, its key, whether near matching applies
  (not for scientific objects, whose names are codes) and how to reach its items in a report. The
  passes of the resolution iterate over it instead of naming each category.
- **`CandidateSource`** — the near-match step asks for a list of candidates, and the category decides
  where it comes from.
- **Front `SELECTORS`** in `CreationProposalCard.vue` — a map from a field's resource type to the
  platform component that edits it (`opensilex-TypeForm`, the variable selectors…), with its model
  property, its update event and its fixed props. A new resource type is one entry, not a new
  branch in the template.

### Command and registry — the model's tools

Each tool the model may call is a command object implementing `AiTool` (`getName`,
`getDescription`, `getParametersSchema`, `execute`). `ToolRegistry` holds them by name, hands their
definitions to the model and dispatches its calls. Shared helpers live in `ToolSchemas` — schema
building, argument reading, and `error(message)`, the one shape a tool answers with when it cannot.

A tool never writes: `propose_creation` records a draft, and only the user's confirmation, through
the REST API, writes.

### Adapter — the platform's validators

**Where**: [`PlatformValidationAdapter`](../java/org/opensilex/aiimport/create/rows/PlatformValidationAdapter.java).

The platform's importers report errors on the CSV they were given — a file the user never saw,
with its own row numbering (`CSVValidationModel`: physical line, two header lines;
`DataCSVValidationModel`: body index, three header lines). The adapter turns both into the module's
`RowError`, placed on **the user's workbook** — sheet, row and column as their spreadsheet shows
them — through the `RowOrigins` recorded while the CSV was generated. Columns are mapped by header
name, because the importers' column indices are not consistent.

### Result objects — outcomes instead of exceptions

A refusal is an expected answer, not an error, so it is returned as data:

- `CreationOutcome` — what confirming a draft came to, for every target: `created(uri)`,
  `inserted(count)` or `refused(rowsChecked, errors)`. The API turns any of them into its response
  the same way.
- `BulkOutcome` — valid, imported, or refused with the rows in the way; never both, with one
  stated exception: `interrupted`, for data written in several batches of which one failed after
  the validation passed. It carries the batch histories already written.

All three have private constructors and static factories named after the outcome, so an object in
an impossible state (written *and* refused) cannot be built. Exceptions remain for what the user
cannot fix from the card — an unreachable store — and `CreationFieldException` for a named field
whose value is unusable.

### Repository — `CorrectionStore`

The misspellings the instance was taught live as `skos:hiddenLabel` in the module's graph, with
PROV-O provenance. `CorrectionStore` is the only class that reads or writes them (`remember`,
`load`, `forget`); the rest of the module sees `LearnedCorrection` objects. Reads are re-checked
under the current user's rights by the [gateway](#gateway--instancelookups).

### Builder — `ProposalBuilder`, `GeneratedCsv`

- `ProposalBuilder` assembles a creation draft from the model's arguments, the file's suggestions
  and the requirements, refusing invented fields and malformed values on the way.
- `GeneratedCsv` assembles the CSV handed to a platform importer, line by line, recording the origin
  of each line (`addDataLine(sheet, row, cells)`) and the module's own refusals as it goes.

### Memento — `SavedSession`

A conversation is stored after every step that changes it and can be resumed after the memory cache
has let it go, for the configured number of days (`savedSessionDays`, 30 by default).
`SavedSession` is the memento: a snapshot of **what cannot be recomputed** — the two sides of the
conversation, the drafts, the confirmed names, the token counters — taken with `SavedSession.of`
and turned back into a session with `toSession`. `AiImportSessionStore` is its caretaker: it keeps
the snapshot in MongoDB and the workbook in the platform's file storage, and nothing else.

What the snapshot leaves out is as deliberate as what it holds. The workbook structure, the plan,
the report and the column mapping are recomputed from the file on resumption
(`AiImportChatService.restore`), against the instance as it is then, without calling the language
model: a report stored for a month would describe an instance that no longer exists. The confirmed
names are put back *before* that resolution runs, so the user's confirmations still hold.

Expiry is the caretaker's job too, not a MongoDB TTL index: a TTL index would delete the document
and leave the workbook in the file storage for good.

## Patterns deliberately not used

- **No dependency-injection framework, no service locator.** The platform passes its services to
  constructors; so does the module.
- **No abstract DAO of the module's own.** The platform's DAOs are used directly, through the
  gateway; a second abstraction over them would be a second place for access rules to drift.
- **No observer or event bus.** A confirmation revalidates the report synchronously
  (`AiImportChatService.revalidate`); there is one listener, the user, and it is waiting for the
  answer.
- **No per-category class hierarchy for the resolution.** The categories differ in one lookup each;
  a lambda per category (`resolveEach`) says that with less code than six classes. Experiments and
  variables, which genuinely differ, have their own method or class.

## Refactoring of 2026-09-26

| Before                                                                                          | After                                                                                           |
|-------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------|
| `ResolutionService`: 1,175 lines mixing the resolution order, every DAO query and the variables | `ResolutionService` (the chain, 532 lines), `InstanceLookups` (the gateway), `VariableResolver` |
| four copies of the confirmed, exact, warning loop                                               | one `resolveEach` template                                                                      |
| a `visible()` switch and five `…Candidates()` methods in the service                            | `InstanceLookups.visible` and `candidates(category)`                                            |
| the `switch` on the target inside `AiImportAPI.create`                                          | `AiImportCreationService.apply`, returning a `CreationOutcome`                                  |
| the list of targets written out by hand in two tools and their error messages                   | `CreationTarget.parse` and `CreationTarget.choices()`                                           |
| `SearchVariablesTool.error`, used by every tool                                                 | `ToolSchemas.error`                                                                             |
| translation keys of targets and row errors listed by hand in the test                           | derived from `CreationTarget` and `RowError.Kind`                                               |
| an orphaned Javadoc in the facilities region, a duplicated import, an unused warning key        | removed                                                                                         |

Behaviour is unchanged, and this is checked rather than assumed: `ResolutionServiceTest` — an
integration test on a seeded instance, written *before* the refactoring — exercises every step of
the chain (exact, synonym, confirmed, learned, near, the experiment-before-plots order) through the
public entry point, and passes before and after.

## Checklists

### Adding a creation target

1. A constant in `CreationTarget` — the tools list it to the model by themselves.
2. Its requirements in `AiImportCreationService.requirementsFor`.
3. Its operation in `AiImportCreationService.apply`, returning a `CreationOutcome`.
4. `AiImport.proposal.target_<TARGET>` in both language files (`TranslationKeyTest` fails otherwise),
   and its applied message in `CreationProposalCard.vue`.

### Adding a report category

1. A constant in `ReportCategory`, with its key, near-matching flag and items accessor.
2. Its lookups in `InstanceLookups`: a `…Named` method, a `visible` case, a `candidates` case.
3. One `resolveEach` call in `ResolutionService.resolve`, at the right place in the order.

### Adding a file family

A step-by-step guide, with the STAR profile as the worked example, is in
[`ADDING_A_PROFILE.md`](ADDING_A_PROFILE.md).

1. An `ImportProfile` implementation, registered in `ImportProfileRegistry`.
2. Its header roles, if new, in `HeaderRoleDictionary`.
3. A test on a real workbook in `src/test/resources`.

### Adding a bulk import through a platform importer

1. A subclass of `PlatformBulkImport`, typed by the importer's validation model.
2. `generate` records every line's origin in `GeneratedCsv`, and the module's own refusals.
3. `errorsOf` goes through `PlatformValidationAdapter` — add a method there for a new validation
   model, with its header-line count.
4. `importWithPlatform` runs in one `SparqlMongoTransaction`.
5. A case in `AiImportCreationService.apply`.

### Adding state to a session

1. Decide whether it can be recomputed from the file and the instance. If it can, do not store it:
   recompute it in `AiImportChatService.analyse`, which resumption runs.
2. If it cannot — it is the user's work or the model's memory — add it to `SavedSession`, in both
   `of` and `toSession`. The snapshot is serialised by its fields, so a field is stored as soon as it
   is declared.
3. Extend `AiImportSessionStoreTest.aConversationComesBackWithAllItsWork` with it.
