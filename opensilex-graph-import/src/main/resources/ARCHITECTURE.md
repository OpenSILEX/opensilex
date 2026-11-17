# Technical documentation : [`data import`] Experimental import assistant (`opensilex-graph-import`)

**Document history (please add a line when you edit the document)**

| Date       | Editor(s)        | OpenSILEX version | Comment           |
|------------|------------------|-------------------|-------------------|
| 2026-11-15 | Arnaud Charleroy | BUILD-SNAPSHOT    | Document creation |
| 2026-11-16 | Arnaud Charleroy | BUILD-SNAPSHOT    | Graph view of the "understand" step, column left out |
| 2026-11-17 | Arnaud Charleroy | BUILD-SNAPSHOT    | Given values and declared resources, step 4 "Create", data |

> ⚠️ _WARNING_ : This module is experimental. It writes to the instance only at step 4, one step at a
> time on the user's click, and only where the platform allows it (`creationEnabled`, off by default). ⚠️
>
> Currently covered topics : the request flow, the package layout, the extension points, the rules that
> merge rows, resolve links and compare with the instance, given values and declared resources, the
> creation step and its writers, data, the REST surface, the front end, the tests.
>
> Missing topics : completing a resource that exists (not built), the cost of a preview on a large
> workbook (not measured).

## Table of contents

<!-- TOC -->
* [Definitions](#definitions)
* [What it does, and what it refuses to do](#what-it-does-and-what-it-refuses-to-do)
* [Request flow](#request-flow)
* [Package layout](#package-layout)
* [Extension points](#extension-points)
* [Rules](#rules)
* [REST surface](#rest-surface)
* [Front end](#front-end)
* [Graph view of the "understand" step](#graph-view-of-the-understand-step)
* [Given values and declared resources](#given-values-and-declared-resources)
* [Step 4: creation](#step-4-creation)
* [Data](#data)
* [The module's ontology file](#the-modules-ontology-file)
* [Configuration](#configuration)
* [Tests](#tests)
* [Limitations](#limitations)
* [Findings about the platform](#findings-about-the-platform)
<!-- TOC -->

## Definitions

- **Import** : the files a user brought (one workbook or several CSV files), read together as one
  workbook, with what the user decided about them. Stored, so that it can be resumed.
- **Graph** : the import once read: a row is a **node** (a resource), a column a predicate of the
  ontology (oeso or an extension), a cell a literal or a link. Rows of any sheet that share an identifier
  are one node.
- **Evidence** : a reason to think a column holds a property (its header, a label, the type of its values,
  values that name existing instances…). Each suggestion comes with its evidence.
- **Scope** : the instances of a root class a user may find by name, with the platform's rules on who
  sees what. An instance under no scope is never found.
- **Preview** : what the graph would do to the instance: resources created, completed, unchanged or in
  conflict, value by value.

## What it does, and what it refuses to do

- It works without a language model: every suggestion comes from evidence the user can read.
- Nothing is written without a click: steps 1 to 3 write nothing; step 4 writes one step at a time,
  after a dry run if the user wants one, through the platform's own classes so that its rules apply.
- It never picks silently: a name that designates two resources, two values for a single-valued
  property, a link that is both a row and an unrelated resource are shown to the user.
- It never reveals what the user may not see: lookups, close names, URI checks and stored values all go
  through the scopes, and an invisible resource reads as absent.
- The same file with the same decisions gives the same result, Turtle export included.

## Request flow

```text
files ──► session (workbook, layouts, decisions, experiment)        session/, binding/, workbook/
            │
            ▼
        Interpreter: sheet class, identifier column, column        interpret/  (EvidenceSource × 6)
        properties, each with its evidence and status
            │  user decisions (PUT sheet_decisions, column_readings…)
            ▼
        GraphBuilder: rows merged by key, cells read for their     graph/      (LinkResolver,
        datatype, links resolved (URI → file row → instance)                    GuardedInstanceIndex)
            │
            ▼
        GraphValidation: every GraphValidator, problems by cell    validate/   (cells, owl)
            │
            ▼
        PreviewBuilder: each node matched to what exists, each     preview/    (InstanceComparator,
        value compared with what it holds; grid, map, Turtle                    PreviewGrid, SheetMap,
                                                                                PreviewTurtle)
```

`api/Interpretations` runs this chain for one request. The preview is kept on the session
(`PreviewMemo`) while the workbook object, the decisions and the experiment stay the same, so that turning
the pages of the grid does not read the instance again; `afresh=true` reads it again.

## Package layout

| Package | Role |
|---|---|
| `api`, `api/dto` | one API class per subject: sessions and files, layouts, decisions, interpretation, validation, preview |
| `session`, `session/store` | the import in memory (`GraphImportSessionCache`) and stored (`SnapshotSessionStore`, Mongo `graph_import_sessions` + files) |
| `workbook` | the header row and first row of values a user chooses for a sheet (`RawRows`, `SheetLayout`) |
| `binding` | what the user decided: class, identifier, column properties, how dates are written (`ImportDecisions`) |
| `ontology` | `ClassView` (accepted properties, cardinalities), `HeaderResolver`, `literal/LiteralReader` |
| `interpret` | evidence sources, their combination, identifier column, sheet class, learned aliases |
| `graph` | `GraphBuilder`, `LinkResolver`, `InstanceIndex` and its guarded implementation, the scopes |
| `validate` | `GraphValidator`, `CellValueValidator`, `OwlRestrictionGraphValidator`, `ValidationReport` |
| `preview` | comparison with the instance, the grid of a sheet, the map of the sheets, the Turtle export |
| `write` | the creation step: `CreationPlan`, one `ResourceWriter` per kind of resource, `DataWriter` |
| `measure` | the sheets of data read row by row (`MeasureReader`, `DataDraft`), never as nodes |

Workbook reading, name similarity and report messages are reused from `opensilex-ai-import`
(`SheetSources`, `WorkbookAssembler`, `NameSimilarity`, `NearMatchFinder`, `ReportMessage`), not copied.

## Extension points

A module implements `GraphImportExtension`; the module reads every implementation through
`getModulesImplementingInterface`. A class or property brought by an ontology extension needs no code.

| To… | Implement | Checklist |
|---|---|---|
| suggest a property from a new kind of evidence | `EvidenceSource` | a signed weight (positive for, negative against), a translatable reason, bounded work (`ColumnValues.EVIDENCE_SAMPLE` values) |
| let users find instances of a new root class | `InstanceScope` | `rootClass`, the graph they live in if any, `visible` that drops (never throws for) what the user may not see |
| check the merged graph | `GraphValidator` | a stable `id` (validators run in id order), problems on the cells concerned, ERROR only for what blocks |
| create resources of a new root class | `ResourceWriter` | `rootClasses`, the `credentialId` checked, `after` (steps first), `whyNot` for every value it cannot write (never drop one silently), `check` that writes nothing, `write` all or none, `existing` if the preview cannot find them by name |

## Rules

- **Merging.** A row is keyed by its identifier: a URI by its expanded form, a name in lower case
  (`InstanceIndex.key`), so `P1` and `p1` are one node. A row without identifier is a node of its own.
- **Links.** A cell of an object property resolves, in order, as a URI, as the identifier of a file row
  whose class fits the range (class views, ancestors included), then as the name of visible instances of
  the range. A file row and an instance of the same name are both kept (`ROW_AND_INSTANCE`); the preview
  judges. Nothing found gives up to three close names, from the file and from the instance.
- **Values.** Cells are read for the property's datatype (`LiteralReader`: decimal comma, Excel dates, a
  date order and zone per column, booleans; ambiguity refused). Numbers lose their trailing zeros, so
  `1,50` and `1.5` are one value.
- **Restrictions** are checked on the merged node: unrelated classes are an error; more distinct values
  than the maximum is an error on every cell; fewer than the minimum is settled by the preview.
  Language-tagged values count apart, as OWL counts them.
- **Matching with the instance.** A node is the resource its written URI names, if it exists, is visible
  and its class fits; otherwise the one visible resource of its class its name designates. Several is a
  conflict. Unidentified rows and nodes whose classes conflict are matched to nothing.
- **Comparing.** Stored literals are read back through `LiteralReader` before comparison. A value the
  resource lacks is added while the cardinality allows it, and is a conflict beyond. The identifier cell
  of a node matched by name is the stored name it matched, case and language aside. A link to a row is
  the resource that row is matched to.
- **Left out.** A column the user leaves out gives no value and is not suggested again; its candidates
  stay, so that it can be taken back. Choosing a property for it takes it back.
- **Turtle.** Existing resources keep their URI; others get an indicative `urn:graph-import:` URI built
  from their key, so the export is the same every time.

## REST surface

All under `/graph-import`, protected, answering 503 while the module is disabled.

| Route | Purpose |
|---|---|
| `POST/GET sessions`, `GET/DELETE sessions/{id}`, `POST/DELETE sessions/{id}/files…` | imports and their files |
| `GET sessions/{id}/sheets/{sheet}/rows`, `PUT sheet_layouts`, `DELETE …/layout` | header row and first row of values |
| `GET sessions/{id}/interpretation` | what each sheet and column holds, with the evidence |
| `PUT sheet_decisions`, `DELETE …/decisions`, `PUT import_experiments` | the user's decisions; each answers the new interpretation |
| `GET sessions/{id}/validation`, `PUT column_readings` | the graph's figures and problems, where the links of each column land (`column_links`), the sheets sharing resources (`shared_resources`); how dates are written |
| `GET sessions/{id}/preview[?afresh]` | counts and map of the sheets |
| `GET sessions/{id}/preview/sheets/{sheet}/rows` | a page (≤ 100 rows) of a sheet, each cell with its state |
| `GET sessions/{id}/preview/nodes/{index}` | one resource, its values merged with their origin |
| `GET sessions/{id}/preview/turtle` | the graph in Turtle |
| `PUT declared_resources`, `DELETE sessions/{id}/declared_resources/{id}` | a resource declared in the assistant |
| `PUT import_provenances` | the provenance of the import's data (404 if unknown) |
| `GET sessions/{id}/creation[?afresh]` | the plan of step 4, step by step |
| `POST creation_checks`, `POST creations` | body `{session_id, step, expected}`: a dry run, or the writing of one step; 409 when `expected` is no longer the step's `count`, 403 when creation is off or the credential is missing |

Routes that take a JSON body carry the session id in the body, not in the path: the platform's
TypeScript generator puts an optional body first, which breaks with path parameters.

## Front end

`front/` is a UMD bundle with only `vue` external, built apart from the host. Consequences:

- `n-*` components are used as the host's globals, never imported from `naive-ui` (that would bundle it);
  only the globals of the host's `main.ts` exist (no `n-select`, `n-tabs`, `n-pagination`).
- Only the page `opensilex-graph-import-GraphImportView` is registered; its panels are imported.
- `<script setup>` cannot export types: shared types live in `.ts` files (`decisions.ts`, `problems.ts`,
  `preview.ts`, `understandGraph.ts`, `dropTargets.ts`, `graphViewState.ts`).
- The map of the sheets is drawn by `opensilex-ai-import-MermaidDiagram`, loaded with the ai-import
  bundle; when it cannot be loaded or fails, the map stays a list. Labels are escaped: sheet names are user
  text.
- Report keys from the server are translated under `GraphImport.report.`, with the English text as
  fallback (`reasons.ts`).

## Graph view of the "understand" step

Step 2 shows the same interpretation as tables or as a graph (buttons **Table | Graph**, the choice kept by
the browser). The graph draws the structure, not the rows: a node per sheet, per column still to decide or
holding values, and per class of existing resources the links reach; an edge per place the cells of a link
column land, and a dotted edge between sheets sharing identifiers.

**Every gesture is an existing request.** A link's target is not a decision: dropping a column on a sheet
chooses a property whose range accepts that sheet's rows, through `PUT sheet_decisions` built by
`decisions.ts` (`withColumn`, `leftOut`, `acceptSheetSuggestions`). The graph is then redrawn from the
answer. While a request runs, the graph is locked: `UnderstandGraph` sets its own flag when it sends,
since the parent's busy flag reaches it only after a redraw.

**What only the server can tell**, because it needs the class hierarchy or the cells:

- which sheets each link property may point to: `PropertyOptionDTO.fitting_sheets` (`interpret/SheetFits`,
  the rule of `LinkResolver`: the sheet's class, or a class given by a type column, is the range or below);
- where the cells of each link column land: `ValidationDTO.column_links` (`graph/GraphLinks`): rows of a
  sheet (counted on the first sheet holding them), existing resources (by the property's range), or
  nothing settled. The preview map is built on the same counts.

| File (`front/src`) | Role |
|---|---|
| `understandGraph.ts` | nodes and edges from the interpretation and the validation; a check one decision behind is not used for a column whose property changed |
| `understandLayout.ts` | deterministic layout: sheets on a circle, columns fanned outward, existing resources on an outer ring, parallel edges set apart, loops; a position saved by the user wins |
| `dropTargets.ts` | where a column may be dropped, with the properties that would link it there, the suggested first |
| `graphViewState.ts` | the chosen view and node positions in the browser's storage, failing silently |
| `components/UnderstandGraph.vue` | selection (held by subject: a column stays selected when it turns from a node into an edge), drag, zoom, lock |
| `components/GraphCanvas.vue` | SVG drawing and pointer events only; user text in SVG text nodes, never as markup |
| `components/GraphToolbar.vue`, `GraphInspector.vue`, `SheetChoices.vue` | counters and legend; details of the selection, reusing the table's components |

Known limits: a column of values lights no target (making it a link goes through "Other property" in the
inspector); a drop on existing resources offers only the properties whose range is that exact class;
beyond about fifteen sheets, "Show all" and the zoom are needed.

## Given values and declared resources

What the file lacks, the user gives in the assistant, and it is read **as if the file held it**, so that
interpretation, links, validation, preview and creation need no special case (`workbook/WorkbookAdditions`):

- a **value given for every row** of a sheet (`SheetDecisions.given`) is a column appended to the sheet,
  its header the URI of the property;
- a **declared resource** (`ImportDecisions.declared`, a factor the file never names) is a row of a sheet
  added to the workbook, one per class, named `[LocalName]`, its headers URIs.

The order follows the platform's links: **the experiment first, then its factor, then the factor's
levels**. A factor names its experiment (`studiedEffectIn`): the drawer fills it with the experiment chosen
for the import, else with the one experiment declared in the assistant. The levels sheet then gives the
factor for all its rows (`hasFactor`), by its name.

A header written as a predicate is recognised as decided. The read workbook is cached on the session and is
the same object while files and decisions are equal, which `PreviewMemo` relies on. Added sheets cannot be
laid out or read raw (404), and are read-only in both views of step 2.

## Step 4: creation

The plan is worked out **from the preview**, which already compares the files with the instance
(`CreationPlan.of`). A node is in the step of the first writer whose root class it belongs to:

- `UNCHANGED` is done; `COMPLETED` and `CONFLICT` are blocked (completing is not supported); a node to
  correct is blocked; `whyNot` blocks a node holding a value its writer cannot write;
- a node pointing to a row still to create waits: for another step to run first, or, in its own step, for
  that row to be ready; a node pointed to by a blocked node of its step waits too (a factor is created with
  its levels or not at all).

After a write the preview is read afresh: what was created is found by name and reads as done. **Running a
step again creates nothing twice, and a step stopped halfway is resumed**, without a log to replay. An event
has no name: `EventWriter.existing` finds those of the same type, targets and moments.

| Step | Writer | Platform entry point | Credential |
|---|---|---|---|
| `experiments` | `ExperimentWriter` | `ExperimentDAO.create`, publisher set | `experiment-modification` |
| `factors` | `FactorWriter` | as `FactorAPI.createFactor`: `FactorDAO.create` with its levels | `factor-modification` |
| `scientific-objects` | `ScientificObjectWriter` | `ScientificObjectLogic.createScientificObject`, parents first, in one transaction; at most 500 (about 170 ms each) | `scientific-objects-modification` |
| `events` | `EventWriter` | `EventLogic.setEventRelations` then `create`; one date is an instant event held as its end | `event-modification` |
| `data` | `DataWriter` (not a `ResourceWriter`) | `DataLogic.createMany`, see below | `data-modification` |

An experiment just created becomes the import's when it has none. Each write is recorded on the session
(`CreationRecord`). Links to existing resources are passed in the platform's prefixed form: its scientific
object logic compares factor levels as written.

## Data

`oeso:Data` (module ontology) mirrors `DataModel`: `hasVariable`, `hasTarget`, `hasDate`, `rdf:value`,
`hasProvenance`, and optional `hasConfidence`, `hasRawData`, `participatesIn`, `prov:wasAssociatedWith`,
`prov:used`. A sheet of this class is **not built into the graph**: a sheet may give tens of thousands of
data. `measure/MeasureReader` reads it row by row and hands each `DataDraft` to a consumer that counts or
collects.

- **Wide** sheets: a column whose variable is chosen (`SheetDecisions.measures`) or suggested from its header
  (one variable of that name, `MeasureChoice`) gives one datum per row. **Long** sheets: a `hasVariable`
  column and a `rdf:value` column.
- The target is a URI, a scientific object row of the file (the datum waits for it until the preview finds
  it in the instance), or a visible object found by name. The date is a date-time, else a day. The value is
  read for the variable's datatype. `NA` and `n/a` are counted apart; a datum given twice, in any sheet, is
  refused.
- The provenance is chosen for the whole import (`PUT import_provenances`); the experiment of the import
  goes in `provenance.experiments`.
- `DataWriter`: a dry run is `DataValidation.validate()` per batch (it writes nothing); a run is
  `DataLogic.createMany` per batch of `DataAPI.SIZE_MAX` (10 000), each all or none. A refused batch stops
  the step, the batches before it stay. A datum the instance holds (same provenance, variable, target and
  date, searched by provenance, variables and date range) is counted done, never sent again: this makes the
  step resumable, and replaces the unique index the test profile lacks.
- URIs of a `DataModel` go through `URIDeserializer.formatURI`: `DataValidation` finds the variable and
  the provenance by the form the data API's JSON reader gives, and fails on a null otherwise.
- Validating 50 000 cells takes about one second; the data are read again for each plan, not kept.

## The module's ontology file

`src/main/resources/ontologies/oeso-graph-import.ttl` adds, in the `oeso:` namespace and its own graph,
what the core ontology lacks for the assistant: `FactorLevel hasFactor` (exactly 1), `Factor hasCategory`
(at most 1), `oeso:Data`. `ModuleOntology.ensureLoaded` loads it when an import is first interpreted,
whenever the graph lacks the current `owl:versionInfo`. It is not a `SPARQLExtension`, so an instance never
needs a setup for it.

**The platform's ontology store reads the restrictions of every graph when the platform starts**, and
refuses to start when one restricts a property no graph declares. The file therefore declares, without
domain, the properties it restricts that the core does not (`rdf:value`, `prov:wasAssociatedWith`), and
`ModuleOntologyTest` builds the real store over it. A broken file already loaded must be replaced by hand
in the triplestore: the module reloads its graph only once the platform runs.

## Configuration

`graph-import` in the platform's configuration: `enabled` (true by default), `creationEnabled` (false by
default: step 4 then only shows what it would do, and writing answers 403), `maxFileSizeMb` (20),
`sessionTtlMinutes` (60, in memory), `savedSessionDays` (30, stored).

## Tests

JUnit 4. Rules are tested without a database on workbooks built with POI (`GraphFixtures`) and a stub
`InstanceIndex`; scopes, stored values and the API run on the embedded triplestore and MongoDB, as a
user who is not an administrator where rights matter. Coverage follows `opensilex-ai-import`'s rule:

```bash
mvn -pl opensilex-graph-import -Pwith-test-report verify   # fails below 90 % of lines
```

## Limitations

- A cell is located by its row among the rows of values: the reader skips empty rows and keeps no
  spreadsheet row number.
- Scientific objects are looked up only in the chosen experiment: without one they read as created. So
  does a resource the user may not see, or one under no scope.
- Close names: at most 100 unfound names, the first three letters equal, at most 2000 labels read before
  the rights filter.
- A date zone is a fixed offset: a series across a clock change is an hour off.
- One header row per sheet; a sheet with fewer than two filled header cells is read as text by the
  platform's reader.
- `someValuesFrom`, `allValuesFrom`, `hasValue` and `oneOf` restrictions are not loaded by the platform,
  so they are not applied.
- The preview kept on a session holds the whole compared graph; its memory cost is not measured.
- Step 4 creates only; it does not complete a resource that exists, nor add a level to an existing factor.
  Germplasm, devices, variables, projects, organizations and facilities are shown, not created.
- No geometry or move for scientific objects; no confidence, raw data or agents for data; no provenance
  created from the assistant.
- At most 500 scientific objects per run of their step.

## Findings about the platform

- Asking the ontology store for a class with an ancestor writes the inherited restrictions into the shared
  cached class; restrictions are therefore read by SPARQL (`ClassViewReader`).
- `OwlRestrictionModel.isList()` treats an exact cardinality of 1 as a list, and `OntologyStoreLoader`
  keeps an exact cardinality as a maximum only.
- The oeso prefix is registered as `vocabulary:`; `oeso:` is accepted as an alias in headers.
- Jena's query builder joins two `addWhereValueVar` calls into one block of tuples, whose lengths must
  match: two independent lists go in `IN` filters.
- `ScientificObjectLogic.checkFactorLevelsBelongsToExperiment` compares URIs with `List.contains`: a full
  URI never matches the prefixed one it holds.
- `EventLogic.create(list, validationOnly = true)` still writes; a dry run calls `setEventRelations` only.
- `DataValidation` throws a `NullPointerException`, not a clear error, for a variable or provenance written
  in another form than the platform's.
- Jena's `AskBuilder` turns the string literal `"1"` into a number: build the literal with `NodeFactory`.
