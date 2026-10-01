# Technical documentation : [`data import`] LLM-assisted import assistant (`opensilex-ai-import`)

**Document history (please add a line when you edit the document)**

| Date       | Editor(s)       | OpenSILEX version | Comment           |
|------------|-----------------|-------------------|-------------------|
| 2026-09-08 | Arnaud Charleroy | BUILD-SNAPSHOT    | Document creation |
| 2026-09-26 | Arnaud Charleroy | BUILD-SNAPSHOT    | Bulk scientific objects, resolution split, design document |
| 2026-09-27 | Arnaud Charleroy | BUILD-SNAPSHOT    | Data through the platform's data import, in batches        |
| 2026-09-29 | Arnaud Charleroy | BUILD-SNAPSHOT    | Link to the guide for adding a profile                     |
| 2026-09-29 | Arnaud Charleroy | BUILD-SNAPSHOT    | Assistant status, conversation folded when not connected   |

> ⚠️ _WARNING_ : This document is incomplete ! You can help by expanding it. ⚠️
>
> How the module is built — its coding conventions and design patterns, with a checklist per kind of
> extension — is in the companion document [`DESIGN.md`](DESIGN.md).
>
> Currently covered topics :
>
> - module wiring, configuration, and the request flow
> - workbook reading, import profiles, resolution, column mapping and type checking
> - the language model connector and its tool-calling loop
> - creation of projects, experiments and observation data
> - the front-end bundle and the constraints the `vue3` branch imposes on it
> - the measured token cost of a conversation, and how to bring it down
>
> Missing topics :
>
> - streaming replies (no server-sent events exist in the repository yet)
> - persisting a conversation beyond the in-memory cache
> - exporting the validation report

## Table of contents

<!-- TOC -->
* [Technical documentation : [`data import`] LLM-assisted import assistant (`opensilex-ai-import`)](#technical-documentation--data-import-llm-assisted-import-assistant-opensilex-ai-import)
  * [Table of contents](#table-of-contents)
  * [Definitions](#definitions)
  * [Functional requirements](#functional-requirements)
  * [Non-functional requirements](#non-functional-requirements)
  * [Solution](#solution)
    * [The central guard-rail](#the-central-guard-rail)
    * [Request flow](#request-flow)
  * [Technical specifications](#technical-specifications)
    * [Module wiring](#module-wiring)
    * [Configuration](#configuration)
    * [Package layout](#package-layout)
    * [UML](#uml)
      * [Reading a file: the profile SPI](#reading-a-file-the-profile-spi)
      * [Confronting the instance, and creating](#confronting-the-instance-and-creating)
      * [Proposing, then writing](#proposing-then-writing)
    * [`workbook` — reading the file](#workbook--reading-the-file)
    * [`profile` — knowing the file family](#profile--knowing-the-file-family)
      * [The STAR profile](#the-star-profile)
      * [Events](#events)
      * [The MIAPPE profile](#the-miappe-profile)
    * [`resolve` — confronting the instance](#resolve--confronting-the-instance)
    * [`mapping` — business entities and expected types](#mapping--business-entities-and-expected-types)
    * [`service` — the language model and the tool loop](#service--the-language-model-and-the-tool-loop)
    * [`create` — proposing, then writing](#create--proposing-then-writing)
      * [Insertion is all or nothing](#insertion-is-all-or-nothing)
    * [`api` — the REST surface](#api--the-rest-surface)
    * [Front end](#front-end)
    * [Tests](#tests)
    * [Environment](#environment)
  * [Token cost](#token-cost)
  * [Remaining cost work](#remaining-cost-work)
  * [Limitations and improvements](#limitations-and-improvements)
  * [Documentation](#documentation)
<!-- TOC -->

## Definitions

- **Data entry file** : a spreadsheet filled in the field by an observer, following a template that
  is documented inside the file itself rather than in a schema.
- **Import profile** : the knowledge about one family of data entry files — what its columns mean,
  which sheets are fixed, and which conventions its author prescribed.
- **Column role** : what a column stands for in OpenSILEX terms (an observed object, a date, a
  variable, an observer, …). See `mapping/ColumnRole.java`.
- **Resolution** : looking every name found in the file up in this instance, and reporting whether
  it exists, is ambiguous, lives on a shared resource instance, or is missing.
- **Column mapping** : a column, its role, the resource it matched, the kind of value its cells
  actually hold, and the disagreements between the last two.
- **Session** (or *conversation*) : one uploaded file and the exchange about it, held in memory.
- **Blocker** : a reason a creation cannot be attempted at all, as opposed to a form field the user
  can simply fill.

## Functional requirements

A scientist has a spreadsheet and no clear path into OpenSILEX. The module gives them one:

1. upload the file and be told, in plain language, what it appears to contain;
2. be told what this instance already has and what it does not — variables, projects, experiments,
   germplasm, scientific objects;
3. see how each column was mapped onto a business entity, and where its values disagree with the
   data type the matching variable expects;
4. be asked about what is genuinely missing or contradictory, rather than being handed a stack
   trace;
5. create the project, the experiment, or insert the observations, each refused while a required
   field is empty.

## Non-functional requirements

- **Reproducibility.** The mapping and the resolution are computed in Java. The same file analysed
  twice yields the same report. A mapping that changed between two runs could not be reviewed.
- **Data minimisation.** Only column headers, a bounded sample of rows (5 per sheet by default) and
  the text of instruction sheets are sent to the language model. The file itself never leaves.
- **Sovereignty.** A single OpenAI-compatible connector, so an instance can point `baseUrl` at a
  gateway inside its own network (vLLM, Ollama, LiteLLM) and keep everything in-house.
- **Least privilege.** Every lookup the assistant performs runs as the account that opened the
  conversation, through the existing DAOs. A conversation is invisible to any other account.
- **Traceability.** Every lookup is listed on the reply that used it. No value is ever silently
  converted.

## Solution

### The central guard-rail

> The language model never produces an identifier, and never decides a mapping.

Everything factual the assistant is allowed to state comes from one of two places: the report that
`ResolutionService` computed, or the result of a tool call that ran a real query. The assistant
explains, questions, and proposes; Java resolves, validates and writes. This is what makes the
feature auditable, and it is the constraint every design decision below follows from.

Three consequences worth stating explicitly:

- creation is driven by a form whose required fields the server declares, not by the assistant;
- a type mismatch is reported cell by cell, with a suggestion, and the correction stays the user's;
- a profile that cannot map a file with confidence refuses to, rather than guessing.

### Request flow

```
POST /ai-import/sessions  (multipart)
      │
      ├─ WorkbookReader ──────────────► WorkbookStructure   (POI, bounded)
      ├─ ImportProfileRegistry.select ► ImportProfile        (best match wins)
      ├─ profile.extract ─────────────► ExtractedImportPlan  (names + anomalies)
      ├─ profile.extractDataPoints ───► List<DataPoint>      (observations, in the file's words)
      ├─ ResolutionService.resolve ───► ResolutionReport     (DAO queries, + shared resources)
      ├─ MappingService.map ──────────► List<ColumnMapping>  (roles + type checks)
      ├─ PromptBuilder.build ─────────► system prompt
      └─ LlmService.complete ─────────► opening analysis
                                        │
                              tool_calls│ (bounded loop)
                                        ▼
                              read-only tools over the existing DAOs
```

Later calls reuse the session: `POST /ai-import/messages` runs the same tool loop,
`POST /ai-import/sessions/{id}/revalidate` recomputes the report and the prompt without losing the
conversation, and `POST /ai-import/create` writes and then revalidates.

## Technical specifications

### Module wiring

`AiImportModule extends OpenSilexModule implements APIExtension`.

- `getConfigId()` → `ai-import`, which is the top-level key read from `opensilex.yml`.
- `getConfigClass()` → `AiImportConfig`.
- No `META-INF/services` file is written by hand: `eu.somatik.serviceloader-maven-plugin`, configured
  in `opensilex-parent/pom.xml`, generates `META-INF/services/org.opensilex.OpenSilexModule`.
- REST classes are discovered automatically — `APIExtension.apiPackages()` scans for `@Path`.
- `LlmService` and `AiImportSessionCache` are `@SelfBound @Service`, so `RestApplication` binds them
  into HK2 and they can be `@Inject`-ed into the API.

**One dependency is added**: `org.apache.poi:poi-ooxml`. Nothing in OpenSILEX read spreadsheets
before this module; CSV goes through univocity. HTTP (Jersey), JSON (Jackson) and the cache
(Caffeine) were already available.

### Configuration

`AiImportConfig` and `config/LlmConfig` are **interfaces** annotated with `@ConfigDescription`, which
the framework proxies from YAML. They follow `core.agroportal` exactly.

```yaml
ai-import:
    enabled: true
    llm:
        baseUrl: "http://localhost:11434/v1"   # any OpenAI-compatible /chat/completions
        model: "qwen2.5:14b"
        apiKey: ""                             # omitted as a header when empty
        maxTokens: 2048
        temperature: 0.2
        timeoutMs: 120000
        maxToolIterations: 6
    maxFileSizeMb: 20
    sampleRowsPerSheet: 5
    sessionTtlMinutes: 60
    searchSharedResourceInstances: true
```

`LlmService.isEnable()` guards on `baseUrl` and `model` being non-empty, so an unconfigured instance
reports the module as unavailable instead of failing at the first request.

### Package layout

88 classes, ~13700 lines under `org.opensilex.aiimport`:

| Package        | Responsibility                                                      |
|----------------|---------------------------------------------------------------------|
| `workbook`     | reads the file into a bounded in-memory model                       |
| `profile`      | knowledge about one family of files; extracts names and observations |
| `resolve`      | confronts those names with this instance and with shared resources  |
| `mapping`      | column → business entity, and expected type versus observed type    |
| `service`      | the language model connector, the tool loop, the session, the prompt |
| `create`       | what a creation requires, and the creation itself                    |
| `export`       | the other direction: an experiment of the instance written as STAR  |
| `api`          | the REST surface and its DTOs                                       |
| `exception`    | `WorkbookReadException`                                             |

### UML

Three views rather than one diagram, because the module has three separable concerns: reading a
family of files, confronting what it read with the instance, and turning the difference into
something created.

#### Reading a file: the profile SPI

A profile is the only place that knows a file *family*. Everything downstream works on what a
profile produced, never on the spreadsheet, which is why adding a fourth template touches one
package.

```mermaid
classDiagram
    direction LR

    class ImportProfile {
        <<interface>>
        +getId() String
        +getLabel() String
        +match(WorkbookStructure) int
        +getPromptContext(WorkbookStructure) String
        +extract(WorkbookStructure) ExtractedImportPlan
        +extractEvents(WorkbookStructure) List~EventCandidate~
        +extractDataPoints(WorkbookStructure) List~DataPoint~
        +roleOf(WorkbookStructure, String, String) ColumnRole
    }

    class ImportProfileRegistry {
        +select(WorkbookStructure) ImportProfile
        +getById(String) Optional~ImportProfile~
    }

    class VitisExplorerProfile
    class StarProfile
    class MiappeProfile
    class GenericTabularProfile

    class ExtractedImportPlan {
        -String profileId
        -List~String~ experimentNames
        -List~String~ projectNames
        -List~String~ germplasmNames
        -List~String~ scientificObjectNames
        -List~String~ facilityNames
        -List~String~ observerNames
        -List~VariableCandidate~ variables
        -List~String~ anomalies
        -Map~String, String~ notes
    }

    class VariableCandidate {
        -String columnKey
        -String label
        -String externalId
        +hasComponents() boolean
    }

    class VariableComponent {
        -String name
        -String accession
    }

    class DataPoint {
        -String sheet
        -int rowNumber
        -String objectName
        -TargetKind targetKind
        -LocalDate date
        -String variableKey
        -String rawValue
    }

    class EventCandidate {
        -String sheet
        -LocalDate date
        -String typeLabel
        -String description
        -List~String~ targetNames
        -TargetKind targetKind
        +toEventDescription() String
    }

    ImportProfile <|.. VitisExplorerProfile
    ImportProfile <|.. StarProfile
    ImportProfile <|.. MiappeProfile
    ImportProfile <|.. GenericTabularProfile
    ImportProfileRegistry o-- ImportProfile : scores, highest wins

    ImportProfile ..> ExtractedImportPlan : produces
    ImportProfile ..> DataPoint : produces
    ImportProfile ..> EventCandidate : produces
    ExtractedImportPlan *-- VariableCandidate
    VariableCandidate *-- VariableComponent : trait, method, unit

    StarProfile ..> StarSheets
    StarProfile ..> StarDictionary
    StarProfile ..> PlotIdReconciliation
    MiappeProfile ..> MiappeSheets
    MiappeSheets ..> MiappeSection
```

`VariableComponent` is what makes a variable *creatable* rather than merely *missing*: only MIAPPE
fills it today, and only a variable that knows its trait, method and scale can be proposed for
creation without inventing them.

#### Confronting the instance, and creating

The resolution never writes and the creation never guesses. `ResolvedItem.status` is the whole
vocabulary of the report, and every URI a creation uses comes from one of those items.

```mermaid
classDiagram
    direction TB

    class AiImportSession {
        -String id
        -URI accountUri
        -WorkbookStructure workbook
        -ExtractedImportPlan plan
        -ResolutionReport report
        -List~ColumnMapping~ mappings
        -List~DataPoint~ dataPoints
        -List~EventCandidate~ events
        -CreationProposal pendingProposal
        -TokenUsage tokenUsage
    }

    class ResolutionService {
        +resolve(ExtractedImportPlan, ConfirmedMatches) ResolutionReport
    }

    class InstanceLookups {
        ~projectsNamed(String) List~ResourceReference~
        ~germplasmNamed(String) List~ResourceReference~
        ~facilitiesNamed(String) List~ResourceReference~
        ~personsMatching(PersonCandidate) List~ResourceReference~
        ~visible(ReportCategory, URI) Optional~ResourceReference~
        ~candidates(ReportCategory) List~ResourceReference~
    }

    class VariableResolver {
        ~resolve(VariableCandidate, ResolvedItem, ResolutionReport)
    }

    class ResolutionReport {
        -List~ResolvedItem~ experiments
        -List~ResolvedItem~ projects
        -List~ResolvedItem~ variables
        -List~ResolvedItem~ germplasm
        -List~ResolvedItem~ scientificObjects
        -List~ResolvedItem~ facilities
        -List~String~ anomalies
        -List~String~ warnings
    }

    class ResolvedItem {
        -String sourceValue
        -String externalId
        -ResolutionStatus status
        -List~ResourceReference~ matches
        -String hint
    }

    class ResourceReference {
        -URI uri
        -String name
        -String sharedResourceInstance
        -String datatype
    }

    class ResolutionStatus {
        <<enumeration>>
        FOUND
        AMBIGUOUS
        FOUND_IN_SHARED_RESOURCE
        MISSING
        NOT_CHECKED
    }

    class AiImportCreationService {
        +requirementsFor(CreationTarget, AiImportSession) CreationRequirements
        +createProject(AiImportSession, Map) URI
        +createExperiment(AiImportSession, Map) URI
        +createEvents(AiImportSession, Map) int
        +apply(CreationTarget, AiImportSession, Map) CreationOutcome
    }

    class DataBulkImport {
        +importAll(AiImportSession, Map) BulkOutcome
        #generate(AiImportSession, Map) GeneratedCsv
        #finish(boolean written)
    }

    class CreationRequirements {
        -CreationTarget target
        -List~RequiredField~ fields
        -List~String~ blockers
        -List~String~ warnings
        +isAvailable() boolean
    }

    class RequiredField {
        -String name
        -String labelKey
        -String kind
        -boolean required
        -String suggestedValue
        -String suggestedFrom
        +isSatisfiedBy(String) boolean
    }

    class CreationTarget {
        <<enumeration>>
        PROJECT
        EXPERIMENT
        EVENT
        DATA
    }

    class CreationProposal {
        -String id
        -CreationTarget target
        -Map~String, String~ fields
        -Map~String, FieldSource~ fieldSources
        -List~String~ missingRequired
        -List~String~ blockers
        -Status status
    }

    class BulkOutcome {
        -int rowsChecked
        -int imported
        -List~RowError~ errors
        -List~URI~ batches
        +isRefused() boolean
    }

    class UnresolvedRow {
        -String sheet
        -int rowNumber
        -String column
        -String reasonKey
        -String value
    }

    AiImportSession *-- ResolutionReport
    AiImportSession *-- CreationProposal : pending
    ResolutionService ..> ResolutionReport : produces
    ResolutionService --> InstanceLookups : reads the instance through
    ResolutionService --> VariableResolver : delegates variables to
    ResolutionReport *-- ResolvedItem
    ResolvedItem --> ResolutionStatus
    ResolvedItem *-- ResourceReference

    AiImportCreationService ..> CreationRequirements : answers "what would this take?"
    AiImportCreationService --> DataBulkImport : inserts the data through
    DataBulkImport ..> BulkOutcome : answers "what was written?"
    CreationRequirements *-- RequiredField
    CreationRequirements --> CreationTarget
    CreationProposal --> CreationTarget

    AiImportCreationService ..> ResolutionReport : reads every URI from
```

#### Proposing, then writing

The guard-rail as a sequence. Two things are worth following: the model never holds a URI, and the
second validation — not the first — is the one that decides.

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant UI as CreationProposalCard
    participant API as AiImportAPI
    participant Chat as AiImportChatService
    participant LLM as LlmService
    participant Creation as AiImportCreationService
    participant DAO as Core DAOs / Logic

    User->>API: POST /ai-import/messages
    API->>Chat: ask(session, question)
    Chat->>LLM: completion(prompt + tools)
    LLM-->>Chat: tool_call propose_creation(target, values)
    Chat->>Creation: requirementsFor(target, session)
    Creation-->>Chat: CreationRequirements
    Note over Chat: ProposalBuilder validates without writing:<br/>an invented field name is refused,<br/>a missing required value comes back named
    Chat-->>API: transcript + CreationProposal
    API-->>UI: the card, values editable, each labelled by source

    User->>UI: corrects a value, ticks the confirmation, validates
    UI->>API: POST /ai-import/create
    API->>Creation: requirementsFor(target, session)
    Note over API,Creation: Revalidated, not replayed —<br/>the instance can have changed since the draft
    alt a blocker or an unsatisfied required field
        API-->>UI: 400, naming what stands in the way
    else data insertion
        API->>Creation: apply(DATA, session, values)
        Creation->>Creation: DataBulkImport.generate — every row placed, before any write
        alt one row cannot be placed
            Creation-->>API: refused(rows)
            Note over Creation: Refused before the provenance exists,<br/>so nothing is left behind
            API-->>UI: the offending sheet, row and reason
        else every row resolves
            Creation->>DAO: DataImportLogic.validateWholeCsv, batch after batch
            alt the platform refuses a row
                Creation->>DAO: delete the provenance of the run
                Creation-->>API: refused(rows, on the workbook)
                API-->>UI: the rows, drawn on the user's sheets
            else every batch valid
                Creation->>DAO: DataImportLogic.importCSVData, one transaction per batch
                DAO-->>Creation: written, with a batch history per batch
                Creation-->>API: inserted(n, batches)
                API->>Chat: revalidate(session)
                API-->>UI: the count, and the recomputed report
            end
        end
    end
```

### `workbook` — reading the file

`WorkbookReader` (243 lines) opens the file with POI and produces a `WorkbookStructure`. Cells are
kept **as text**, deliberately: the point is to show what was actually typed before any
interpretation. Two decisions carry weight:

- **Header detection.** The header row is the first row whose non-empty cells all look like labels:
  at least two of them, none numeric, none longer than 80 characters. That last bound matters — the
  `ReadMe` of the reference template has a row made of a long sentence plus a date-formatted cell,
  which without it reads as a header row and the author's instructions are lost. A sheet with no
  header row is kept as prose in `SheetStructure.text`.
- **Date system.** `WorkbookStructure.date1904` records whether the workbook uses the 1904 system.
  The same serial reads four years apart in the two systems, so a file saved in one and read in the
  other files data under the wrong season. POI resolves date-formatted cells itself; the flag covers
  the cells it cannot — a bare number in a date column — and lets a profile tell the user their
  workbook is not in the system their template prescribes.

`ExcelValueParser` normalises what field templates actually contain: `NA` and its variants as
missing, a comma as decimal separator, non-breaking and zero-width spaces stripped, and serial
numbers converted with the workbook's own epoch.

`HeaderMatcher` locates a column by the words its header contains, accent- and separator-insensitive.
Templates version their header names — `Nouvelle_Abréviation_FR_maj_03_2023` — and matching on words
survives a revision where matching on an exact name does not.

### `profile` — knowing the file family

```java
public interface ImportProfile {
    String getId();
    String getLabel();
    int match(WorkbookStructure structure);              // 0 = not mine; highest wins
    String getPromptContext(WorkbookStructure structure);
    ExtractedImportPlan extract(WorkbookStructure structure);
    default List<ObjectRow> extractObjectRows(WorkbookStructure structure); // empty = none
    default ObjectSheetDefaults objectSheetDefaults(WorkbookStructure s, String sheet);
    default List<FactorLevelCandidate> extractFactorLevels(WorkbookStructure structure);
    default List<EventCandidate> extractEvents(WorkbookStructure structure); // empty = none
    default List<DataPoint> extractDataPoints(WorkbookStructure structure);  // empty = refuse
    default ColumnRole roleOf(WorkbookStructure s, String sheet, String header);
}
```

`ImportProfileRegistry` ships `VitisExplorerProfile`, `StarProfile`, `MiappeProfile` and
`GenericTabularProfile`, and picks up any
profile contributed by another module through `ServiceLoader`. The generic profile scores 1, so it
only wins when nothing recognises the file.

To write a profile for a new file family, follow [`ADDING_A_PROFILE.md`](ADDING_A_PROFILE.md), a
step-by-step guide with the STAR profile as the worked example.

`VitisExplorerProfile` (614 lines) knows the grapevine observation template: three fixed sheets
(`ReadMe`, `Chronologie` catalogue, `Cartouche_Fixe`) then one sheet per phenological stage. It maps
`Dispositif` → experiment, `PU` → scientific object, `Genotype` → germplasm, `Statut` → factor level,
`Observateur` → provenance, and keys variable columns to the catalogue by abbreviation, carrying the
CropOntology identifier along.

Two subtleties encoded there:

- a stage sheet dated by its own column, such as `Deb_Date`, uses that column **both** as the row's
  date and as the observation — the date of budburst *is* the measurement;
- `Obs_libre` appears in the catalogue but is a free comment, not a measurement.

`extractDataPoints` returning an empty list is a **refusal, not a gap**. Guessing which column
identifies the observed object would attach measurements to the wrong plots, which is worse than
offering no insertion at all. Data creation is therefore unavailable under the generic profile.

#### The STAR profile

`profile/star/` handles the STAR agronomic trial model, a normalised template with one entity per
sheet. It is a better fit for OpenSILEX than an observation template, because it carries its own
metadata — the experiment, its objective and dates, the treatments, the plots, the field — and its
own schema.

Three pieces:

- **`StarSheets`** finds sheets **by prefix**, not by name: `ed_` for the experimental design,
  `data_` for observations, `dictionary` for the schema. The prefix is stable where names are not —
  the reference template calls a sheet `ed_placette` where an earlier revision called it
  `placette` — and it absorbs a future `data_` sheet without a code change.
- **`StarDictionary`** reads the sheets that describe every column. It reads **by content, not by
  position**: the header declares eight columns but a row carries only the ones it has, so a row is
  four to seven cells wide and the same index means a different field from one line to the next.
  Classifying each cell by what it looks like — a boolean, a known R class, something with a colon —
  is what makes it safe. This is a property of the format, not a defect of any one file (the
  reference template behaves identically), so it is handled quietly rather than reported.
- **`PlotIdReconciliation`** resolves the identifier mismatch described below.

The mapping onto OpenSILEX is settled by the published STAR to ELOA alignment rather than guessed:
`expe` is the experiment — with the organisation and the unit that run it — the field sheet a
**facility** (address from the commune, location from the coordinates, a WKT point), each `ed_`
sheet **scientific objects**, `cultivar_name` the germplasm, `modalite` a factor and its levels,
`meteo` the weather measured on the field.

**Where the field is.** The current convention prefixes the facility sheets with `field`; the
earlier revisions filed the field among the design sheets as `ed_parcelle`, or bare as `parcelle`.
`StarSheets.field()` takes the first `field…` sheet, then the older names. The commune is
`town_name`, `commune_name` in the older revisions; both are read. The plot sheet is `ed_placette`,
or else the first design sheet identifying its rows by `plot_id` — which is how an exported
workbook names it, after the type of its objects (`ed_plot`). The ELOA classes are not loaded in this instance, so each resource takes an
existing OESO type and keeps its ELOA IRI as an external reference — the same treatment already
given to the CropOntology identifiers of variables.

**Two real defects the profile surfaces**, both present in the reference template and therefore in
every workbook derived from it:

1. **The plot sheet and the data sheets disagree on how a plot is named.** The plot sheet composes
   block then treatment (`A1`, `A10`); the data sheets compose treatment then block (`1A`, `10A`).
   Nothing matches, so no observation can be attached. Recomposing the plot sheet's treatment and
   block resolves all 44 — 40 by recomposition, 4 controls that already matched. The profile
   reconciles deterministically and **states what it applied**, as an anomaly and as a plan note.
   The observations are extracted, recomposed; the decision to accept the recomposition is asked at
   the confirmation before writing, as a checkbox carrying that sentence. Returning nothing from
   `extractDataPoints` was the earlier design and it hid the disagreement instead of raising it —
   the page showed no observation and no reason — where every other decision of this kind already
   lives in the conversation.
2. **`rain_mm` is filed under metadata rather than variables** in the reference template, beside
   `plot_id` and `commune_name`, though it is a measured quantity and a column of `data_meteo`. An
   import driven by that template would quietly drop rainfall. The check is general — any column of
   a `data_` sheet the dictionary does not declare a variable is raised.

**Sheet names across the two revisions.** `dataSheets()` also accepts the earlier revision's
unprefixed `meteo`, named explicitly rather than inferred — deciding by content which sheet holds
observations would sweep in `suivi_data` and `listes`. Without it a workbook of that revision lost
its **847 weather observations** in silence: the sheet was read, then never looked at. That sheet
also names no object column at all, so when the workbook declares exactly **one** field the
observations are attached to it — weather is the field's — and only then; more than one field and
there is nothing that can stand in.

**What a `data_` sheet is.** The contract is *an object identifier, a date, then one or more
variables* — in that sense, not in that order, and the column names vary. The identifier names
either a **scientific object** (a plot) or a **facility** (the field): weather is measured at the
field, not on a micro plot, which is why `DataPoint.targetKind` exists and why resolving every
target as a scientific object silently dropped the weather. `data_template` is an empty sheet whose
only purpose is to show that shape, and it is skipped as a source of observations.

#### Events

Two STAR sheets record what happened during the trial, and both were being read and then ignored:

- **`evenement`** — a date, a free-text type, a description. No column says what the event
  concerned, and the sheet sits at trial level, so it concerns the **field**. Naming the field
  rather than every plot keeps one event where the file records one.
- **`ppp`** — plant protection product applications, named per treatment. A treatment covers several
  plots and a spraying is one pass of the sprayer over all of them, so one row becomes **one event
  concerning every plot of that treatment**.

The shipped event vocabulary (`oeev`) describes device maintenance and moves; it has no class for a
spraying or a hailstorm. An event therefore takes the generic `oeev:Event` type and keeps the file's
own wording — product, AMM number, dose, volume, sprayer — in its description, where nothing is
lost. Forcing an agronomic event into `Incident` or `ScientificObjectManagement` would state
something the file never said.

`EventCandidate` carries the targets **as names**, resolved against the report at creation time like
every other target, so an event is never filed against a URI the assistant invented.

#### The MIAPPE profile

`profile/miappe/` handles MIAPPE v1.1, the minimum information checklist for a plant phenotyping
experiment. It differs from the other two in kind: it is a **metadata checklist, not a data entry
file**. The observations live in a separate file that the `Data file` section links to, so
`extractDataPoints` always returns nothing — not a refusal this time, simply the truth about the
format. What the workbook carries is everything *around* the measurements, which is the part an
import usually has to invent.

Three conventions of the checklist do the work:

- **A trailing asterisk marks a mandatory field.** The standard states what a valid submission must
  carry, so the report can name what is missing without a rule being written for each field —
  `MiappeSection.unfilledMandatoryFields()` reads the asterisks.
- **The three rows under each header are documentation** — definition, example, format — followed by
  a `Values (add rows if necessary)` banner. Reading them as values would import the standard's own
  examples: a plot called `plot:894`, a person called `Ines Chaves`. `MiappeSection` steps over
  them by the label in the first column.
- **`Investigation` is written transposed**, one field per row with its value in the second column,
  because it describes a single thing.

The mapping: `Investigation` → project, `Study` → experiment (its `Experimental site name` → a
facility), `Observation Unit` → scientific objects, `Biological Material` → germplasm, `Event` →
events, `Observed Variable` → variables.

**`Observed Variable` is why MIAPPE matters here.** It defines each variable as a *trait*, a
*method* and a *scale*, each with an optional ontology accession — the components an OpenSILEX
variable is made of, which neither STAR nor VitisExplorer supplies. `VariableCandidate` carries them
as `VariableComponent` values, which is what lets a missing variable be **proposed for creation**
rather than only reported as absent. One gap remains and is deliberate: OpenSILEX splits the trait
into an entity and a characteristic — MIAPPE's "plant height" becomes the entity "plant" and the
characteristic "height" — and that split is a judgement about the user's science, so it goes through
a form they can correct rather than being applied by a reader.

**What happens to those components.** When a variable resolves nowhere, `resolveComponents` looks
each one up in this instance by name, through the same `BaseVariableDAO` the variables screen uses,
and records the result on the report as a `ResolvedComponent` — role, name, accession, and the URI
when it is here. Three things follow. The report shows the four parts as tags, green for the ones
that already exist, so the user can see at a glance whether creating this variable is two clicks or
a form to fill. The Create button opens `opensilex-VariableForm` with the found components already
selected. And what was not found goes into the description, where the user can read the file's own
words while choosing or creating it — otherwise that information is lost between the spreadsheet and
the form.

Reusing beats creating, and this is where it is enforced: a component entered a second time under a
second spelling stays in the instance's referential for good.

The training spreadsheet ships **empty**, every section documented and no values. That is the state
a submission starts in, so it is the state the profile handles best: it reports "9 of the 11
sections are still blank templates" rather than reporting nothing found and leaving the user to
wonder what went wrong.

#### When the model is unreachable

The assistant is a conversation, not a dependency. Reading the file, choosing a profile, extracting
the plan, confronting the instance and mapping the columns are all plain Java: a user whose model
endpoint is down still uploads a workbook and still gets the report, the mapping and the creation
forms. `LlmService` funnels every failure — unreachable, misconfigured, disabled, timed out — into
one exception, which the chat service turns into a notice in the transcript rather than an error
page, carrying a translation key because the module wrote it and not the model.

The page does not wait for that notice. When a session opens it asks `GET /ai-import/assistant`,
which probes the endpoint's model list (`LlmService.isReachable`: no token spent, 3 s at most, only
a success counts), and **folds the conversation away** when the model does not answer, so the
report takes the whole width. A button beside the file name opens or closes the conversation at
any time, and once the user has used it, their choice stands. A reply that comes back as the
unreachable notice updates the "not connected" badge without folding the panel the user is reading.

`HeaderRoleDictionary` is what makes that promise worth something for an **unrecognised** file. The
default `roleOf` used to answer only DATE, COMMENT or VARIABLE, leaving the business mapping to the
conversation; the dictionary answers OBJECT, TRIAL, PROJECT, GERMPLASM, LOCATION, PERSON, OBSERVER,
SEASON and FACTOR_LEVEL from the header alone, in French and English, as a table lookup.

Two rules keep it from guessing. A term matches the whole normalised header before it matches an
edge of it, so `plot_id` is an object where `surface_bloc_m2` is a measurement. And a word that
means two things in two templates is **absent** from the table — STAR calls a field a *parcelle* and
treats it as a facility, other templates use it for the observed plot — because a column left
`UNKNOWN` asks a question, where a column mapped wrongly answers one nobody asked.

### `resolve` — confronting the instance

`ResolutionService` turns names into a `ResolutionReport`. It holds the **order** in which a name is
tried — confirmed by the user, exact, taught to the instance, merely close — and writes the report.
How each kind of resource is fetched lives in `InstanceLookups`, always through the platform's own
DAOs and logic classes, so their access checks apply; variables, which are tried in more ways than
the rest, in `VariableResolver`. [`DESIGN.md`](DESIGN.md) explains the split. Per category:

| Category           | How                                                                            |
|--------------------|--------------------------------------------------------------------------------|
| Experiment         | `ExperimentDAO.getExperimentByNameOrURI`                                       |
| Project            | `ProjectDAO.search`, then strict equality on name or short name                |
| Variable           | pass 1: `skos:exactMatch` ending with the ontology id from the file; pass 2: `VariableDAO.search` then strict equality on name or alternative name; pass 3: the shared resource instances |
| Germplasm          | `GermplasmSearchFilter` on name, then strict equality on name or synonym       |
| Person             | `PersonDAO.search` on the ORCID, the email or the name, then strict equality on that key |
| Facility           | `FacilityLogic.search`, then strict equality                                   |
| Scientific object  | `ScientificObjectDAO.checkUniqueNameByGraph`, every name in one query          |

Each entry carries a `ResolutionStatus`: `FOUND`, `AMBIGUOUS`, `FOUND_IN_SHARED_RESOURCE`, `MISSING`
or `NOT_CHECKED`. The strict-equality filter matters: every DAO name filter is a regular expression,
so a near miss would otherwise point the user at a variable that merely looks similar.

The ontology-identifier pass is the most reliable key the file offers. The file carries a compact
`CO_356:1000217` while the instance stores a full URI whose prefix is a local choice, so the query
matches on the **end** of the URI (`STRENDS`) rather than hard-coding anyone's prefix.

`SharedResourceVariableLookup` queries the instances declared in `core.sharedResourceInstances`,
lazily and per instance. An unreachable instance is recorded once as a warning on the report and then
left alone: one unavailable service must not sink the whole analysis. **Nothing is copied** — the
report points at where the variable lives, and importing it stays a deliberate act in the variables
screen.

Facilities are resolved through **`FacilityLogic.search(filter)`**, not through a direct
`sparql.search`. The direct query bypassed the access control that logic applies, so a user could be
told about a facility they are not entitled to see. Going through the logic layer is also what the
rest of the codebase does.

#### Misspelt names: suggest, then let a person confirm

Resolution compares names exactly, case aside. A typo — "Chardonay" for "Chardonnay" — therefore
came out `MISSING`, and the next thing the page offered was to **create** it: a duplicate in the
referential, the one outcome nothing later undoes. A missing name now comes with up to three
**suggestions**, existing resources whose name is close, and stays missing until the user picks one.

*Why not in the triplestore.* Standard SPARQL has no edit distance; the DAOs use `REGEX`, which
tolerates nothing. RDF4J's `LuceneSail` does fuzzy queries (`term~2`), but it is a configuration of
the repository **on the server**, which OpenSILEX reaches over HTTP: enabling it means rebuilding and
reindexing every instance's repository, with a syntax other stores do not share. A custom SPARQL
function would have to be deployed in the server's classpath and would scan every label with no
index. So the comparison runs here, in Java; `LuceneSail` remains the scaling option for germplasm.

`NameSimilarity` decides what counts as close, and leans towards silence — a missed suggestion costs
one search, a wrong one files data against somebody else's resource:

- **restricted Damerau-Levenshtein**, so a swap of two neighbours ("Genotpye") is one edit, not two;
  bounded, stopping as soon as the answer is known to be "too far";
- **short names are compared exactly** (≤ 4 normalised characters): `A1` and `A10` are one edit
  apart and are two plots;
- **digits must agree**: "Clone 115" and "Clone 116" are one edit apart and are two clones;
- case, accents and separators are normalised away first, through `HeaderMatcher.normalize`.

`NearMatchFinder` ranks candidates and keeps three. The candidates come from the same DAOs and logic
the exact lookup uses — `ExperimentSearchFilter.setUser`, `FacilityLogic.search`, the user-scoped
project and variable searches — so a suggestion can never reveal a resource the user is not allowed
to see. Small referentials are listed once per analysis, up to 2000, with a warning if that limit is
reached; germplasm, which can run to tens of thousands, is searched per name through its three-letter
fragments, since a typo spoils one or two fragments and never all of them. **Scientific objects are
excluded** (`ReportCategory.SCIENTIFIC_OBJECTS`): plot codes are too close to one another for a
distance to mean anything. Variable components get the same treatment, shown as "≈ name" beside the
component and picked, if at all, in the variable form's own selector.

A confirmation goes through `POST /ai-import/matches` and is stored in `ConfirmedMatches` on the
session. It survives revalidation, is consulted before any search, and makes the name resolve
exactly as a correct spelling would — so `SessionFacts` and the insertion use it without a line of
their own. The server accepts only **one of the suggestions the report made for that very name**
(`ResolutionReport.suggestion`), never an arbitrary URI sent in a request. `POST
/ai-import/matches/forget` takes a confirmation back. Nothing is renamed, in the file or in the
instance, and the assistant is told it may mention suggestions but never confirm one.

Regex literals sent to the triplestore are escaped character by character (`escapeRegex`) rather
than with `Pattern.quote`, whose `\Q…\E` form is a Java extension outside the XPath regular
expressions SPARQL specifies.

#### Teaching the instance a misspelling

A confirmation lasts one conversation. The next file that writes "Chardonay" would ask again — so a
confirmed match can also be **remembered**: `POST /ai-import/corrections`.

*Where it goes.* A misspelling is not a synonym, and writing it as one would put "Chardonay" in the
germplasm's list of names for everyone to read. SKOS has a property for exactly this case,
`skos:hiddenLabel`, defined for misspelt variants: searchable, never displayed. `CorrectionStore`
writes it on the resource, **in a graph of the module's own** (`<base>/set/ai-import/corrections`),
together with a small PROV-O record — `prov:wasAttributedTo`, `prov:generatedAtTime`,
`dcterms:type` for the category. Nothing is written into a resource's own graph; any SPARQL client
can use what was learned; forgetting is deleting a few triples. A correction whose resource has
since been deleted — the resource has lost its `rdf:type` — is simply not read back. Literals are
serialised by Jena, never concatenated: a spreadsheet cell holding `" } ; DROP ALL` is stored as
text, which a test checks against a real in-memory RDF4J store.

*How it acts.* Read once per analysis, applied only to what is still **missing** after the exact
lookup — a name spelt exactly as a resource here always wins — and before near matches: a
remembered correction resolves outright, a merely close name is only suggested. The report says so
("recognised from a correction by A. Martin on 2026-09-26") and offers to forget it.

*Who may teach it.* A remembered correction acts on everyone's imports, so teaching or forgetting
one takes the right to modify that kind of resource (`ReportCategory.getModificationCredential`,
the platform's own `germplasm-modification`, `variable-modification`…), or administrator rights.
Only a suggestion the report made for that name, or the match the user confirmed for it, can be
taught.

*Who may benefit.* A correction names a URI, and URIs are not visible to everyone. Before one is
applied, the resource is read again **under the current user's rights**, through the platform's own
access-checked getters (`ExperimentDAO.get(uri, user)`, `FacilityLogic.get(uri, user)`…). A
correction taught by one person never reveals to another a resource they may not see.

*Showing the difference.* `front/src/textDiff.ts` aligns the file's spelling with a suggestion and
underlines what differs — Chardon**n**ay — ignoring case, accents and separators as the matching
does. The user checks one letter instead of rereading two words.

*A fix found on the way.* The germplasm lookup searched synonyms in SPARQL but then kept only exact
**names** in Java, so a variety written under one of its synonyms was reported missing although the
query had found it. Synonyms are now accepted.

#### Errors on the user's own rows

Bulk creation goes through the platform's own validators — `ScientificObjectCsvImporterLogic`,
`DataImportLogic` — fed with a CSV the module generates. Their errors point at lines of that CSV,
which the user never sees, so every error is brought back to the **workbook**: sheet, row number as
the spreadsheet shows it, column header as the file writes it.

- `RowOrigins` records, while a CSV is written, which workbook row each data line came from and
  which workbook header each generated column holds. Columns are matched on the generated
  **header**, never its index: the platform's CSV engine counts columns from 0 for some errors and
  from 1 for others, while the header it reports is always the one written.
- `PlatformValidationAdapter` reads both validation models — physical CSV lines from 1 after two
  header lines for the scientific-object engine, body indexes from 0 after three header lines for
  the data import — and folds their twenty-odd error buckets into a dozen `RowError.Kind`s a user
  can act on (missing, refused, wrong type, unknown, already there, duplicate…). The platform's own
  message is kept as the detail. Row-0 errors, which the engine emits for whole chunks, are said
  about the file rather than pinned on a row.
- The module's own refusals (`UnresolvedRow`) become `RowError`s too, so both look the same.
- `BulkValidationDTO` carries the errors (at most 1000, with the total) **and the faulty rows with
  their values**, in the column order of their sheet, so the interface draws them without reading
  the file again. Only faulty rows travel.
- `ImportRowsGrid.vue` draws them with the platform's `n-data-table` — globally registered, so the
  module bundles nothing — one sheet at a time, the faulty cell marked with its message on hover,
  counts per family above. Tabulator was the first idea; it is imported directly by the core front,
  not exposed to modules, and would have been bundled whole into this one.

#### Scientific objects in bulk

Target `SCIENTIFIC_OBJECTS` creates every observed unit of the workbook in one pass, through the
platform's own `ScientificObjectCsvImporterLogic` — the code the scientific-object screen runs, with
all its rules — rather than a second implementation.

- Each profile reads its plot sheet into `ObjectRow`s (`extractObjectRows`): name, germplasm,
  treatment, hosting facility, position. STAR: `plot_id`, cultivar, `xp_trt_code`, the field,
  `plot_x` / `plot_y`; VitisExplorer: the cartouche's plot, genotype and status; MIAPPE: the
  observation unit, its biological material and its factor value.
- `ScientificObjectBulkImport` (a `PlatformBulkImport`, see [`DESIGN.md`](DESIGN.md)) writes the
  importer's CSV with URIs only: germplasm and facility from the report, the treatment as a level of
  the experiment's factors (`FactorDAO`), a position as a move dated from the experiment's start —
  the workbook says where, never since when. A value that resolves to nothing is refused on its own
  row before the platform is asked: the importer would accept an empty factor level and silently
  lose the information.
- **The object type is the user's choice**, made per sheet with the platform's `opensilex-TypeForm`
  restricted to scientific object types. What a "plot" is on this instance is not the file's call.
- Validation first (`importCSV(file, true)`), then the import in one `SparqlMongoTransaction`.
  A refusal comes back as `RowError`s on the plot sheet, drawn by `ImportRowsGrid`.

**One type per sheet, and any column to any property of that type.** A workbook can list objects
on several sheets — STAR's `ed_*` sheets, one kind of experimental unit each — and each sheet takes
its own type. The pieces, in `create/objects/`:

| Piece | Role |
|---|---|
| `ImportProfile.objectSheetDefaults` | what the profile knows of a sheet: the column naming the objects, what the columns it recognises become, the type the file states (`object_type`, as an exported workbook writes it) |
| `ObjectSheetPlan` | what the user changed — type, sheet left out, column to target — kept in the session and in its stored copy (Memento) |
| `ObjectSheets` / `ObjectSheet` | the effective view: the user's choice, else the profile's, else nothing; and `problemsOf`, what the type contradicts in the mapping |
| `TypeProperties` | the properties a type accepts, read where the platform's importer reads them: the ontology store's restrictions, inherited up to `oeso:ScientificObject` |
| `ObjectValueResolver` | a name written in a cell turned into the URI of the resource it names, by exact label among the instances of the property's range; a name matching two is refused, not guessed |

A target is a property URI, `x` / `y` for a position, or nothing. Every sheet goes into **one** CSV
— the importer reads the type row by row — so all sheets are validated and written together or
not at all. The header is the union of what the sheets write; a property two columns feed appears
twice, which the importer reads as a list. The name column always stays the name.

A **parent** (`isPartOf`) must already exist in the experiment: the importer checks every object of
a file against the instance before writing any. A parent created by another sheet of the same run
is therefore stopped with that explanation — create that sheet first, leave it out the second time.

Two findings from the platform, recorded here because they shaped the code:

- `OntologyStore.classExist(type, ancestor)` answers yes for **any** class the store knows,
  whatever its ancestry — `oeso:Facility`, `oeso:Germplasm` pass as scientific object types.
  `TypeProperties.isObjectType` walks the class's parents instead.
- The platform reports some headers of its errors back **prefixed** (`vocabulary:hasCreationDate`)
  where the module wrote them in full; `RowOrigins` expands both before comparing, otherwise an
  error lands on the property's URI instead of the user's column.

The panel (`ObjectSheetsPanel.vue`, under the conversation, full width): a tab per sheet, the
type, the rows as the file holds them with a drop-down above each column — nothing, a position,
the factor level, or a property of the type — and what the type contradicts. *Check* runs the whole
validation without writing (`POST /object-sheets/validate`); *Create the objects* asks the assistant,
whose proposal card confirms, as every other creation.

#### Creating from the platform's own forms

A missing variable, person, project, facility or organisation in the report has a **Create** button
that opens the platform's creation form — `VariableForm`, `PersonForm`, `ProjectForm`,
`FacilityModalForm`, `OrganizationForm` — prefilled from the file, rather than a form of this module.
What each brings:

| Form                | Prefilled from the file                                           | Sub-resources it can create on the spot                                     |
|---------------------|-------------------------------------------------------------------|-----------------------------------------------------------------------------|
| `VariableForm`      | name, ontology identifier, the components the report found        | entity, entity of interest, characteristic, method, unit (Agroportal forms) |
| `PersonForm`        | given and family name (a guess, shown to be corrected)            | —                                                                           |
| `ProjectForm`       | name, acronym, dates, objective, description (the PROJECT draft's suggestions) | coordinators and contacts, through `PersonSelector`             |
| `FacilityModalForm` | name; commune (address locality); the centroid as a dated location (a point, shown in WKT); row and plant spacing and INSEE code in the description; the organisations the report found | none: organisations, sites and variable groups must already exist          |
| `OrganizationForm`  | name; a unit's institution as its parent, when the instance has it | —                                                                           |

An experiment has no creation form in this front yet, so it is still drafted on the proposal card.
The button only shows when the user holds the platform's modification credential for that kind of
resource, as on the platform's own screens.

**The row is bound to what the form created, by URI.** `VariableForm` renames a variable after its
components as soon as one is chosen, and any name can be edited in any form, so finding the resource
again by the file's name would leave the row "missing" right after it was created. The front sends
the created URI to `POST /ai-import/matches/created`, which reads it back under the user's rights
(`ResolutionService.visibleResource`) and records it as a confirmed match for that row.

Two changes to core forms made this possible, both backwards compatible: `ProjectForm` gained the
optional `initForm` hook `FacilityModalForm` already had, and `FacilityModalForm` now hands the
created URI to its `onCreate` listeners instead of `undefined`.

#### The rest of a STAR workbook: organisations, the field, the treatments

- **Organisations.** `expe` names the institution and the unit running the trial. They form a
  report category of their own, `ORGANIZATIONS`, resolved exactly and by near match like the
  others; a unit's row carries the institution the file places it in (`ResolvedItem.parentValue`),
  so the organisation form opens with that parent. Create the institution first.
- **The field.** Its row carries what the file says of it (`ResolvedItem.details`): commune, INSEE
  code, latitude and longitude, row and plant spacing — the facility form starts from them. No core
  facility property holds a spacing, so it is written in the description rather than dropped.
- **The experiment** is drafted with the organisations and the facilities the report found
  (`organisations`, `facilities`, platform selectors on the card). This is not cosmetic: the
  platform lets a plot be hosted only by a facility the experiment uses, or that its organisations
  host.
- **The treatments** (`modalite`) are the levels of the experiment's factors: target `FACTORS`,
  created as the platform's factor screen creates them (`FactorDAO`, the experiment read under the
  user's rights, the right `factor-modification`). The level's name is the treatment's code — what
  the plots write and what the object import matches — its description the treatment's name and
  what the file says of it. A treatment the experiment already has is skipped; a factor it already
  names is refused on its field. The experiment must exist: the requirements say so until it does.

The order that follows — institution, unit, facility, experiment, factors, objects, data — is the
order each step checks the previous one, and the prompt tells the assistant as much.

#### Storing and resuming a conversation

Every conversation is stored as it goes, with its file. The first screen of the assistant has two
tabs, **New file** and **My sessions** — the second always shown, with its count, and an explicit
message when there is nothing to resume yet; leaving a conversation lands on it. The REST endpoints
find a stored session transparently when the memory cache has let it go. What is stored, what is recomputed, and how expiry works is described in
[`DESIGN.md`](DESIGN.md#memento--savedsession).

| Endpoint                          | Does                                                        |
|-----------------------------------|-------------------------------------------------------------|
| `GET /ai-import/sessions`         | the user's stored conversations, most recent first, with their expiry date |
| `GET /ai-import/sessions/{id}`    | a conversation, resumed from storage if needed               |
| `DELETE /ai-import/sessions/{id}` | closes a conversation and deletes it, with its file          |

### `mapping` — business entities and expected types

**A property is not a measurement, and neither is a place.** `OBJECT_PROPERTY` describes the
observed object — the first row of a unit plot, its last vine, the spacing it was planted at — where
`LOCATION` says where something sits and `VARIABLE` says what was measured on it. The difference
decides where the value goes: a property is written **once**, on the scientific object, as a
relation of the ontology; a measurement is written **per observation**, with a date and a
provenance. Filing one as the other produces either a variable nobody ever observes, or an object
attribute buried in a time series.

**A coordinate is a dated event, not an attribute.** `POSITION` covers `plot_x` and `plot_y`,
because OpenSILEX records where an object is as a **move**: a pot moves and a plot does not, and the
model does not decide that in advance. Its own scientific object CSV import carries exactly these
columns — `x`, `y`, `z`, `textualPosition`, with a start and an end date — and turns them into a
`MoveModel`. Writing them as properties would put a coordinate on the object with no date and no
history.

**A block is not a treatment.** `PARENT_OBJECT` covers `block_code`: a block groups plots, where a
treatment is a level of a factor. Two plots in the same block carry different treatments — that is
what blocking is for — so calling the block a factor level made the design unreadable.
`xp_trt_code` stays `FACTOR_LEVEL`, which is what it is: a level of a factor declared on the
experiment.

So, per column: VitisExplorer's `Premier_Rang`, `Dernier_Rang`, `Premiere_Souche`,
`Derniere_Souche` are properties; STAR's `row_spacing`, `plant_spacing` and `plot_n` are too;
`plot_x`/`plot_y` are positions; `block_code` is a parent object; `field_latitude`,
`field_longitude`, `commune_name` and `commune_insee_id` locate the facility.

#### Two things the file cannot settle

Both are raised as anomalies rather than defaulted, because both are expensive to undo — creating
several hundred objects under a type nobody chose is not repaired by editing one of them.

- **What a plot *is*.** STAR counts plants per plot (`plot_n`) and never states the scientific
  object type. That type decides what can be observed on the object and how it is created.
- **What a block *is*.** OpenSILEX supports both readings: a scientific object of its own with the
  plots as its parts, or a property carried by each plot. The first is right when something is
  observed on the block itself, and only the user knows whether anything is.

`MappingService` (420 lines) produces one `ColumnMapping` per column:

- `role` from `profile.roleOf(...)`, and `ColumnRole.getEntity()` names the OpenSILEX concept it
  feeds;
- the matched variable and its `expectedDatatype`, carried up from `ResourceReference`;
- `observedKind` read from the cells: `INTEGER`, `DECIMAL`, `DATE`, `BOOLEAN`, `TEXT`, `MIXED`,
  `EMPTY`;
- a column-level `suggestion` when the two disagree, and up to 10 `TypeIssue` entries naming the
  offending cells with the row number **as it appears in the spreadsheet**.

Only `VARIABLE` columns are type-checked; checking an observer column would produce noise the user
cannot act on. A column with no matching variable gets the datatype to create it with, deduced from
its values — which is the information the user actually needs at that moment.

The six datatypes are the ones `VariableAPI.getDatatypes()` offers: `xsd:boolean`, `xsd:date`,
`xsd:dateTime`, `xsd:decimal`, `xsd:integer`, `xsd:string`.

### `service` — the language model and the tool loop

`LlmService` (173 lines) posts to `{baseUrl}/chat/completions`. It follows
`opensilex-core`'s `AgroportalService` closely: `@SelfBound @Service`, config injected, a JAX-RS
client with an explicit timeout, the shared Jackson mapper, an `isEnable()` guard and
`DisplayableServiceUnavailableException` with an i18n key. `Authorization: Bearer` is omitted
entirely when `apiKey` is empty, so a local endpoint is not handed an empty token. Streaming is not
used; progress is reported from the tool round trips, which is what actually takes time.

`AiImportChatService` runs the loop: while the reply carries `tool_calls`, execute them in Java,
append `role: tool` messages, call again — at most `maxToolIterations` times, after which the user is
told rather than the request running to the timeout. A tool failure is reported *to the model*, not
raised, so it can say so or try something else.

Six tools, all **read-only**, all running as the current account:

| Tool                                    | Backed by                                             |
|-----------------------------------------|-------------------------------------------------------|
| `search_variables`                      | `VariableDAO.search`                                  |
| `search_variables_in_shared_resource`   | `SharedResourceInstanceService.search`                |
| `search_experiments`                    | `ExperimentDAO.search`                                |
| `search_projects`                       | `ProjectDAO.search`                                   |
| `search_germplasm`                      | `GermplasmDAO.search`                                 |
| `get_sheet_preview`                     | the session's workbook, not the databases             |

`PromptBuilder` (306 lines) assembles the system prompt: the rules, the profile's domain knowledge,
the bounded file view, the resolution report, and the mapping restricted to columns worth discussing.
The rules forbid inventing a URI, forbid claiming something exists without having seen it, and forbid
silently correcting a value — a measurement converted behind the user's back is a measurement nobody
can trace.

`AiImportSession` holds the workbook, plan, report, mappings, data points, the wire history and the
visible transcript. `AiImportSessionCache` keeps sessions in a **static** Caffeine cache: self-bound
services are request-scoped, so a per-instance cache would be discarded between two calls of the same
conversation. Lookups require the owning account, so a conversation opened by someone else is
indistinguishable from one that never existed.

### `create` — proposing, then writing

`AiImportCreationService` (441 lines) answers two questions with the same computation — *what would
this take?* and *is this allowed?* — which is why the rules live here rather than in the API.

`CreationRequirements` separates two things deliberately:

- a **`RequiredField`** is something a human can type, optionally pre-filled with a suggestion read
  from the file and labelled with where it came from. A field of kind `boolean` is a *decision*
  rather than a value — rendered as a checkbox, and satisfied only when ticked, since `false` is
  exactly what an unticked box submits. `RequiredField.isSatisfiedBy` owns that rule so the API,
  the proposal builder and the card cannot disagree about it;
- a **blocker** is something they cannot, because it depends on a resource that does not exist yet.
  Only a blocker makes the action unavailable outright.

| Target       | Required fields                                 | Blockers                                                                                       |
|--------------|-------------------------------------------------|------------------------------------------------------------------------------------------------|
| `PROJECT`    | `name`, `start_date`                            | none                                                                                           |
| `EXPERIMENT` | `name`, `objective`, `start_date`               | none                                                                                           |
| `VARIABLE`   | `variable_summary` (informational)              | not analysed; no variable of the file found on a shared resource instance                      |
| `DATA` (STAR)| `reconciliation_confirmed` — a checkbox carrying the sentence the profile wrote | as `DATA`                                                    |
| `EVENT`      | `event_summary` (informational)                 | no event read; not analysed; none of the targets exists yet                                    |
| `DATA`       | `experiment`, `provenance_name`                 | no readable observation; not analysed; experiment missing; a variable missing, ambiguous or only on a shared resource instance; a scientific object missing or unchecked; a facility missing, when the file measures at one |

Those three are the fields the model genuinely demands: `required = true` on the SPARQL mapping
raises at insert time, independently of the DTO annotations. `objective` is the one no field
observation file can answer — which is why a STAR workbook, which states it, changes what is
possible.

**The flow is conversational, not a form.** A panel of three forms used to sit beside the
conversation; it was replaced because a form cannot explain *why* it proposes a given start date,
and a sentence can.

1. the assistant describes what the resource would contain, in prose;
2. it calls `propose_creation`, which validates without writing and stores one
   `CreationProposal` on the session;
3. a card renders under that message with the values, each editable, and a confirm button;
4. `POST /ai-import/create` names the draft, revalidates it, and writes.

`ProposalBuilder` is what keeps step 2 honest: a field name the assistant invented is refused, a
malformed date is refused, and a required field left empty comes back **named** so the assistant can
ask for it in the same turn instead of proposing something incomplete. A value the assistant omitted
falls back to the file's own suggestion, and `fieldSources` records for each value whether it came
from the file, the assistant, or the user — a date read from a spreadsheet and a date written by a
language model do not deserve the same trust, and the card says which is which.

Confirmation revalidates rather than replays: the instance can change between the draft and the
click, and the second check is the one that decides. A proposal's `status` is what prevents
confirming twice.

#### Reading the session once

`SessionFacts` holds everything a conversation already knows: the URI of a resolved plot, the
earliest observed date, the variables available on a shared instance. Two very different callers
kept asking the same questions — the code that answers *what would this creation take?* and the code
that performs it — and a requirement that says a creation is possible while the write disagrees is
the worst bug this module can have.

Nothing in it touches a database; every answer comes from the report, computed once. That is what
lets the requirements be tested with no instance at all, which the tests had been claiming
informally.

#### Importing a variable rather than inventing one

A variable needs an entity, a characteristic, a method and a unit — all four `required = true` on
the SPARQL mapping — so creating one from nothing means creating up to five resources and adding
four permanent entries to this instance's referential. When the report says
`FOUND_IN_SHARED_RESOURCE`, there is a cheaper and safer path: copy the variable and its components
from the instance that already defines them, in one transaction.

That copy already existed, but only inside `VariableAPI.copyFromSharedResourceInstance` — reachable
over HTTP and nowhere else. It moved to **`org.opensilex.core.variable.bll.VariableCopyLogic`**,
the `bll` layer this codebase already uses for `DataLogic`, `EventLogic` and `FacilityLogic`; the
API method is now a delegation, and this module calls the same code. Reimplementing a five-resource
transaction would have been a second way to leave it half done.

`importVariables` groups the report's matches by the instance they came from — one copy call per
instance — and reports how many components arrived with the variables, which is usually the
surprise: two variables can bring four entities and three units.

#### Creating a variable: the platform's own form, not a second one

When a variable exists nowhere — not here, not on a shared instance — it has to be created with its
four components, and the temptation is to add a fifth creation target with five selectors of its
own. The module does **not** do that. The report's Create button beside a missing variable opens
`opensilex-VariableForm`, the very form the variables screen uses, prefilled with the name and the
ontology identifier the file supplies (carried as an `exact_match`, which is what it is).

That form already brings what this step needs and what would otherwise have to be rebuilt: an
entity, characteristic, method and unit selector that search what the instance has, each with the
inline modal that creates a new one, plus the datatype selector, the URI generator and the
Agroportal lookup. Rebuilding it would mean a second form to keep in step with the platform's rules
about a resource whose accidental creation is permanent.

So there is no Java code here for creating a variable — the useful work is elsewhere: saying which
variables are missing, what the file does supply about each, and revalidating afterwards so the
report picks up what was created. `MiappeProfile` is what makes that saying worthwhile, since it
alone reads a trait, a method and a scale out of the file.

#### Insertion is all or nothing

The observations go through the platform's own data import, `DataImportLogic` — what the data
import screen runs — rather than a write of this module's own. `DataBulkImport` (a
`PlatformBulkImport`, see [`DESIGN.md`](DESIGN.md)) writes the CSV that import reads, with URIs
where the workbook has names, and brings the platform's findings back to the workbook:

- **Every row is placed before any is written.** A row whose variable, plot or facility the report
  did not resolve is refused by the module itself, before the platform is asked, with its sheet, row
  and reason. So is a row giving two different values for the same variable, target and date: the
  platform's format has one cell for them, and dropping one silently is not an option.
- **The format**: three header lines (variable URIs, the workbook's own headers, empty
  descriptions), then one line per workbook row, target and date, with a cell per variable — empty
  where that row did not measure it, which the platform skips. Plots and facilities share the
  generic `target` column: a `scientific_object` cell may not be empty, so a file mixing both could
  not use it, and the plots were resolved inside the experiment already.
- **A provenance must exist before the platform validates**, so one is created for the run and
  deleted again when nothing was written: a refusal still leaves nothing behind. Being fresh, it also
  makes a duplicate *already in the instance* impossible — MongoDB's unique index includes the
  provenance — which is why only duplicates *within* the file are reported.
- **At most 10,000 lines per platform import** (`DataAPI.SIZE_MAX`). A larger file goes in batches,
  **all validated before the first is written**; each batch is then written in its own transaction,
  with its own batch history and archived CSV. Should a batch still fail after the validation passed,
  the outcome says so plainly — how much was written, in which batches — the draft is closed so it
  cannot write them twice, and the batches can be deleted from the data import history. The module
  caps a file at 50,000 observations, so at five batches.

What the platform brings that the module's own write did not: every value checked against its
variable's type, the batch history, and the CSV archived as a document — data imported from here is
found, traced and deleted like any other import.

The three silent losses the first version of the insertion had are still closed, by the same means:
facility targets resolve against the facilities (`DataPoint.targetKind`), every plot is resolved in
one `checkUniqueNameByGraph` query rather than a sample, and an unresolved row refuses the insertion
instead of being skipped while the count reported success.

Event creation follows the same rule for the same reason, through `EventLogic.create`.

### `export` — an experiment written as STAR

The other direction from the import: `GET /ai-import/star?experiment=` returns the experiment as a
STAR workbook, the file the STAR profile reads. No language model is involved, so the export stays
available when the assistant is not configured; nothing is written to the instance.

Three pieces, one responsibility each:

- **`ExperimentSnapshotReader`** — the only part that knows the platform (a gateway, like
  `InstanceLookups`). Every read goes through the platform's own DAO or logic, under the user's
  rights: `ExperimentDAO.get` (which refuses an experiment the user cannot see), `FactorDAO`,
  `FacilityLogic` and its last location, the scientific-object search the platform's CSV export runs
  (factor levels and custom properties included), `DataDAO.search` page by page, `VariableDAO`.
  Beyond 500 000 values it refuses with a 400 that points at the platform's data export — a workbook
  is no longer the right shape, and failing on memory half-way would say nothing.
- **`ExperimentSnapshot`** — what the two others share: names where the format expects names (a
  STAR file points from one sheet to another by name), the URIs beside them in columns of their own.
- **`StarWorkbookBuilder`** — the only part that knows the workbook. Streamed (`SXSSFWorkbook`), in
  the profile's own column constants so the import and the export cannot drift apart.

What is written:

| Sheet | Content |
|---|---|
| `readme` | origin, date, and the values nobody could attach, counted |
| `expe` | one row per project, organisation or contact — STAR reads distinct values per column, so a list in one cell would read as one name. An organisation with a parent is the unit (`suborganization_name`), its parent the institution |
| `field` | the facilities; latitude and longitude are the **centroid** of the last location (JTS), as STAR asks; the facility's custom properties follow the standard columns, named by the property's last segment |
| `modalite` | one row per factor level; the code is the level's name, which is what the design sheets write and what the platform matches a treatment on |
| `ed_<type>` | one sheet per type of object, shaped like STAR's plots: `<type>_id`, `xp_trt_code`, `parent_id`, `cultivar_name`, `<type>_x`/`_y` (the initial move), `<type>_desc`, then `object_type`, `object_uri` and the type's own properties |
| `data_<type>`, `data_meteo` | one column per variable; repetitions (several values of one variable on one target at one date) become rows, as the template writes several leaves read on the same plot; the facilities' observations go to `data_meteo` |
| `dictionary_variables` | code (the alternative name when there is one), unit symbol, R class from the datatype, the ontology term matched or the variable's URI |
| `dictionary_metadata` | every other column written, described as the reference template describes it (`star/dictionary_metadata.tsv`, a copy of its dictionary); the columns this module adds carry no URI, since the standard has none for them |

The standard allows added columns — "add columns freely, and declare them in the dictionary" — and
that is the rule followed for everything STAR has no column for. Values measured on something that
is neither an object nor a facility of the experiment (a device, say) are counted in the readme
rather than dropped in silence.

The front end offers it as a third tab of the first screen, *Export as STAR*: the platform's
`ExperimentSelector`, and the platform's `downloadFilefromService`, which carries the token.

### `api` — the REST surface

`AiImportAPI` (579 lines), `@Path("/ai-import")`, every endpoint `@ApiProtected`.

| Verb   | Path                                                | Purpose                                                        |
|--------|-----------------------------------------------------|----------------------------------------------------------------|
| `GET`  | `/profiles`                                         | the file families recognised                                   |
| `GET`  | `/assistant`                                        | whether the model is configured and answers (no token spent)   |
| `POST` | `/sessions`                                         | multipart upload; opens a conversation and returns the analysis |
| `GET`  | `/sessions/{id}`                                    | structure, report, mapping and history                         |
| `DELETE` | `/sessions/{id}`                                  | close and forget                                               |
| `POST` | `/messages`                                         | ask a question (`session_id` in the body)                      |
| `GET`  | `/sessions/{id}/report`                             | the validation report                                          |
| `GET`  | `/sessions/{id}/mapping`                            | the column mapping and its type issues                         |
| `POST` | `/sessions/{id}/revalidate`                         | recompute against the instance, keeping the conversation       |
| `GET`  | `/sessions/{id}/creation-requirements?target=`      | fields, suggestions and blockers                               |
| `POST` | `/create`                                           | create a project or experiment, or insert the data             |
| `GET`  | `/sessions/{id}/object-sheets`                      | the object sheets: rows, type, mapping, what the type contradicts |
| `POST` | `/object-sheets`                                    | choose a sheet's type, leave it out, map its columns           |
| `GET`  | `/object-types/properties?type=`                    | what a scientific object type accepts                          |
| `POST` | `/object-sheets/validate`                           | check every object sheet against the platform, writing nothing |
| `GET`  | `/star?experiment=`                                 | the experiment as a STAR workbook (`StarExportAPI`)            |

> **A generator constraint worth knowing.** The TypeScript client generator cannot express a request
> that has both a body and a path parameter — it emits `method(body?: X, sessionId: string)`, which is
> invalid TypeScript. No endpoint in OpenSILEX combines the two; every update carries its identifiers
> inside the body. `POST /messages` and `POST /create` follow that, which is why `session_id` travels
> in the body while the read-only endpoints keep it in the path.

An oversized or wrongly-typed upload is refused before the file is opened, so a mistake gives a clear
answer rather than a parser error.

### Front end

The creation card renders **the form components OpenSILEX already ships** rather than raw inputs:
`opensilex-InputForm`, `opensilex-TextAreaForm`, `opensilex-DateForm`, `opensilex-CheckboxForm`,
and — where a field designates a referential — the very selector the matching screen uses
(`opensilex-ExperimentSelector`, `opensilex-ProjectSelector`, `opensilex-EntitySelector`,
`opensilex-CharacteristicSelector`, `opensilex-MethodSelector`, `opensilex-UnitSelector`). Every
`.vue` under `opensilex-front/front/src/components` is registered globally as
`opensilex-<FileName>`, so a module uses them by tag, with nothing to import.

This is what `RequiredField.resource` is for: the server says *which referential* a field
designates, and the card hands it to the selector that knows how to search it. A URI is chosen from
what exists instead of pasted, the field looks the way it looks everywhere else in the instance, and
the "offer choices, not blank fields" requirement is met by components that already do it. The
selectors differ in their model prop — `experiments`, `projects`, `selected` — so the card carries a
small table of those names rather than assuming one.


`front/` builds a UMD bundle the host loads with a `<script>` tag and then hands to
`app.use(...)`. Four constraints of the `vue3` branch shape it, all verified in the source:

1. **`window.Vue` used to expose four functions only.** A module bundle with `vue` external fails as
   soon as a component reaches for `onMounted` or `watch`. `opensilex-front/front/src/main.ts` now
   exposes the whole namespace, which also keeps a single Vue instance in the page — what
   `inject('$opensilex')` across the module boundary depends on.
2. **`initAsyncComponents` has no caller**, so a plugin's `components` field is never read. The
   plugin registers its own components with `app.component(id, …)` inside `install()`.
3. **`loadTranslations` has no caller** either. Messages are merged in `install()` through
   `$opensilex.$i18n.mergeLocaleMessage`, and templates use `$t(...)` — not `useI18n()`, which would
   need a bundled copy of `vue-i18n` and lose the host's instance.
4. **`exports: 'default'`** in the Rollup output, because the host does
   `app.use(window[moduleId])`: the global has to *be* the plugin, not the module namespace.

naive-ui is used **by tag, never imported**. `main.ts` now registers the components it already
imported but never passed to `create()` — dead imports until then. Verified: naive-ui does not appear
in the module bundle, so there is no second runtime.

Component ids follow `{module}-{Name}`, which
`ModuleComponentDefinition.fromString` splits on the last dash — so a component name must carry none
of its own.

| Component                  | Role                                                            |
|----------------------------|-----------------------------------------------------------------|
| `AiImportView`             | the page; owns the session and coordinates the panels           |
| `AiChatPanel`              | the conversation and the input                                  |
| `AiChatMessage`            | one turn; renders the assistant's markdown with `marked`        |
| `ResolutionReportPanel`    | what exists and what does not, per category                     |
| `MappingPanel`             | column → entity, expected versus observed type, offending cells |
| `CreationProposalCard`     | a drafted creation, editable, confirmed in the conversation     |
| `WorkbookStructurePanel`   | sheets, headers and a sample of rows                            |

> **A styling trap.** `opensilex-front/front/src/styles/common.scss` sets
> `.btn-sm { width: 32px }` globally, for the icon-only action buttons in its tables. Any labelled
> `btn-sm` is clipped. That rule is load-bearing across the application and is left alone; each
> component of this module neutralises it with a scoped `.btn-sm { width: auto }`, whose specificity
> is enough without `!important`.

### Tests

330 tests, **90.5 % of the module's lines covered**, and the build checks it: under the platform's
coverage profile the module adds a `jacoco:check` rule that fails below 90 %.

```
mvn -pl opensilex-ai-import -Pwith-test-report verify
```

The report lands in `site/opensilex-ai-import/jacoco/`. None of the tests needs a real language
model: `StubLlmEndpoint` stands in for it and replays the tool calls each test scripts. The
integration suites extend the core's `AbstractMongoIntegrationTest` — RDF4J in memory, an embedded
MongoDB. The REST suite, `AiImportAPITest`, runs against the assistant switched on by the module's
test configuration (`src/main/resources/config/test/opensilex.yml`, read under the `test` profile
only), which points it at the stub on a fixed port.

| Suite                                | Covers                                                                 |
|--------------------------------------|------------------------------------------------------------------------|
| `ExcelValueParserTest` (12)          | `NA`, decimal comma, invisible characters, both date systems           |
| `HeaderMatcherTest` (4)              | accents, separators, versioned header names                            |
| `WorkbookReaderTest` (10)            | 18 sheets, the `ReadMe` staying prose, the workbook's date system      |
| `VitisExplorerProfileTest` (21)      | roles, catalogue, the three anomalies, observation extraction          |
| `StarProfileTest` (27)               | both revisions, sheet names, facility targets, events, applications, the pinned observation counts |
| `StarDictionaryTest` (12)            | content-driven reading, declared types, variable flags                 |
| `PlotIdReconciliationTest` (5)       | the identifier mismatch and what it recomposes                         |
| `MiappeProfileTest` (9)              | recognition, documentation rows, mandatory fields, the empty template  |
| `FilledMiappeSubmissionTest` (8)     | a filled submission: sections, variable components, events             |
| `MappingServiceTest` (15)            | roles, observed kinds, type mismatches, proposed datatypes             |
| `AiImportCreationServiceTest` (18)   | required fields, every blocker, the confirmation checkbox, importing from a shared instance |
| `ProposalBuilderTest` (9)            | invented fields, malformed dates, missing required values              |
| `LlmServiceTest` (11)                | request shape, bearer header, tool-call parsing, every failure path    |
| `AiImportSessionCacheTest` (6)       | ownership isolation, expiry, prompt replacement                        |
| `PromptBudgetTest` (5)               | the prompt stays within its measured budget                            |
| `TranslationKeyTest` (5)             | every key the report emits exists in both language files; target and row-error keys derived from their enums |
| `NameSimilarityTest` (9)             | typos, transpositions, short codes, digits, bounded distance           |
| `NearMatchFinderTest` (6)            | ranking, the cap of three, one resource under two names                |
| `ConfirmedMatchesTest` (7)           | a confirmed name resolves; only a suggestion made for it is accepted   |
| `ResolutionQueryTest` (3)            | fragment search and portable regex escaping                            |
| `PlatformValidationAdapterTest` (10) | platform validation errors brought back to workbook sheet, row and column; batches; duplicates in the file versus in the instance |
| `CorrectionStoreTest` (7)            | taught corrections on a real in-memory RDF4J store: provenance, replacement, deletion, injection |
| `HeaderRoleDictionaryTest` (11)      | the header-to-entity mapping, its near-misses and its stated limits    |
| `ScientificObjectBulkImportTest` (5) | the CSV the object importer receives, from a real STAR workbook; a treatment the experiment lacks refused on its row |
| `DataBulkImportTest` (9)             | the CSV the data import receives; every refusal made before anything is written |
| `DataBulkImportPlatformTest` (4)     | **integration**: the platform's real data import — written with a batch history, a wrong type refused on its row with nothing written, batches, an error in the last batch stopping all |
| `AiImportAPITest` (21)               | **integration**: the REST surface end to end — upload, analysis, questions with tool calls, drafts confirmed into a project and an experiment, cancellation, confirmations, corrections taught and forgotten, binding of created resources, stored sessions, every refusal and every unknown conversation |
| `VariableAndPersonResolutionTest` (8) | **integration**: variables by ontology identifier, alternative name, components resolved or suggested, confirmation, learned correction; people by email and by close name |
| `CreationServicePlatformTest` (5)    | **integration**: events written on known targets, experiments linked to a project, a wrong project refused on its field |
| `ToolsTest` (10)                     | every tool the model may call: its answers, its refusals, never an exception |
| `DtoRoundTripTest` (4)               | every DTO written with the platform's JSON mapper and read back, snake_case names asserted |
| `SharedResourceVariableLookupTest` (3) | the lookup on shared instances, against a stand-in instance and an unreachable one |
| `GenericTabularProfileTest` (3), `PersonCandidateTest` (4), `CreationModelsTest` (4) | the fallback profile, a person's keys and name split, the outcome and field models |
| `ResolutionServiceTest` (12)         | **integration**: the whole resolution on a seeded instance — exact, synonym, confirmed, learned, near, experiments before plots, binding a created resource by URI |
| `AiImportSessionStoreTest` (6)       | **integration**: a stored conversation comes back whole; owner isolation; the file kept and deleted with it; expiry after the retention |
| `SessionResumptionTest` (1)          | **integration**: a stored session rebuilt from the real VitisExplorer file, report recomputed, no model call |

`StubLlmEndpoint` is a chat completion endpoint built on the JDK's own HTTP server — no test
framework dependency was added. `TestConfig` implements the configuration interfaces directly, which
is all a proxied-from-YAML interface needs.

The workbooks live in `src/test/resources/`: the VitisExplorer file, both STAR revisions, and the
MIAPPE v1.1 training spreadsheet. A *filled* MIAPPE submission is built in the test rather than
shipped — the training spreadsheet ships empty, and what needs testing there is the reading rules,
the file format being already covered by the tests that read the real workbook.

The VitisExplorer workbook carries three real anomalies, and the tests
assert that each is reported: the 1904 date system against a template prescribing 1900, a cartouche
declaring season 2018 while every stage sheet declares 2020, and a `Souche_HE` column the catalogue
calls `Cep_HE`.

### Environment

- **Java 17 or later** (`opensilex-parent` sets `java.compiler.version` to 17). A JDK, not a JRE.
- Build: `mvn -pl opensilex-ai-import -am install`.
- The front bundle is produced by the module's own `vite build`; `node_modules` resolves from the
  repository root, as it does for `opensilex-core`.
- A local Ollama is enough for acceptance testing. Nothing in the module requires a hosted provider.

### Saying it twice: keys for the interface, English for the model

The report's sentences have two audiences with opposite needs. The interface is French for a French
user, so it needs a translation key and its parameters. The same sentences go into the system
prompt, where English is right and a key would be meaningless — the model cannot resolve
`AiImport.report.hint.experimentMissing`, and the prompt already tells it to answer in the user's
language.

`ReportMessage` carries both: a key, its parameters, and the English text. The Java keeps the
English sentence inline rather than deriving it from a key, so the sentence a reviewer reads in the
code is the sentence the model receives.

Every carrier of such a sentence — `ResolvedItem.hint`, the report's anomalies and warnings,
`ColumnMapping.suggestion`, `TypeIssue.problem` and its suggestion — holds a `ReportMessage`, while
keeping a `String` overload that wraps the text in `ReportMessage.plain`. A sentence with no key yet
still reaches the interface, in English, rather than being dropped; and the change needed no
sweep of the call sites that had nothing to gain from a key.

`TranslationKeyTest` runs the three profiles over their own workbooks, collects every key produced,
and fails when one is absent from either language file. Checked this way round on purpose: a test
that hunted English words in the output would fail on a proper noun and pass on a missing
translation, where this one asks the question that matters — will the interface find something to
show?

The distinction to hold: messages meant **for the model** — tool descriptions, profile context,
prompt rules — stay English and get no key. They are not shown to anyone.

## Token cost

Measured, not estimated. `PromptBudgetTest` builds the real prompt from the reference workbook
against an empty instance — the worst realistic case, where every name is missing and therefore
every entry carries a hint — and fails if it grows past a ceiling.

**System prompt: 20 786 characters, roughly 5 200 tokens.** It is rebuilt and resent on every
message, so this is a per-message cost.

It was **17 900 tokens** before the reduction described below. What changed, and what it bought:

| Section               | Before | After | How                                                                 |
|-----------------------|-------:|------:|---------------------------------------------------------------------|
| Rules                 |    600 |   600 | unchanged                                                            |
| Profile context       |    455 |   455 | unchanged                                                            |
| File structure        |  4 200 |   750 | shared columns declared once; samples for the first 3 sheets only    |
| `ReadMe` text         |    640 |   640 | unchanged — it is the format's specification and worth every token   |
| Resolution report     |  5 700 |   900 | counts and 8 examples per status, with the shared hint stated once   |
| Column mapping        |  6 260 | 1 400 | keyed by column not by sheet; 12 lines; offending cells counted      |
| Getting the detail    |      — |    90 | tells the model which tool returns the rest                          |
| Creation panel        |      — |   450 | what the page's own forms ask for, derived from the creation service |
| **Total**             | **17 900** | **5 200** | **−71%**                                                    |

The saving is a change of shape, not a trim: nothing was dropped, it moved behind three tools —
`get_report`, `get_mapping` and `get_sheet_preview` — and is fetched only when a question needs it.
A question about one column no longer pays for the other sixty.

The one section that grew is worth its 450 tokens. Without it the assistant answered "I cannot
create a project in OpenSILEX, you have to do it manually" and sent users to the general screens,
throwing away the pre-filled form this page had just built. It is generated from
`AiImportCreationService.requirementsFor`, the same computation the panel uses, so a required field
renamed in Java cannot leave the assistant describing a form that no longer exists.

**What a question costs now.** One user message is still several API calls; the difference is what
each one carries:

```
                                       before          after
call 1   system + question              17 950          4 800
call 2   + tool_calls + results         18 950          5 900
call 3   + tool_calls + results         19 950          7 000
                              total  ≈ 56 850       ≈ 17 700   prompt tokens
```

About $0.17 a question at $3 per million input tokens, down to about $0.05. On a local endpoint the
figure to watch is the prefill: ~57 000 tokens a question, down to ~18 000.

Two further levers are in place:

- **History compaction.** Tool results older than the last two turns are replaced by a one-line
  note, so a long conversation stops growing without bound. The message itself is kept, not removed:
  an OpenAI-compatible endpoint rejects a conversation where an assistant message asks for a tool
  call that no result answers.
- **The row sample is no longer a cost driver.** It applies to the first three sheets rather than
  all seventeen, so a row costs a few hundred characters instead of ~2 200. `PromptBudgetTest` pins
  that, and fails if the bound is ever removed.

**Measuring it in production.** Estimates are for catching regressions; the endpoint's own figures
are for telling an administrator what was spent. `TokenUsage` accumulates `prompt_tokens` and
`completion_tokens` per conversation from every `ChatResponse`, is logged at `INFO` at the end of
each turn, and is returned on `AiImportSessionDTO.token_usage`. Calls are counted too: `calls`
divided by the number of user messages is what says how chatty the tool loop is being on a given
model.

A cost figure is then `prompt × price_in + completion × price_out`, with the prices wherever the
instance keeps them — zero for a self-hosted endpoint, where the number to watch is latency.

## Remaining cost work

- **Prompt caching where the endpoint supports it.** The system prompt is stable within a session,
  which makes it the ideal cache target. Worth a configuration flag rather than an assumption, since
  support varies across OpenAI-compatible gateways.
- **A tool-call budget per question.** `maxToolIterations` bounds the loop at 6; a model that uses
  all six triples the cost of a question. Logging the distribution would say whether the ceiling
  should be lower.
- **Summaries scale with the file, not with the instance.** A workbook with 200 columns would push
  the mapping summary back up. Capping it by relevance rather than by count would hold the line.

## Limitations and improvements

- **No streaming.** There is no server-sent events anywhere in the repository. The REST contract is
  compatible with adding it later.
- **Sessions are in memory.** Nothing survives a restart. Acceptable while the module holds no state
  worth keeping, but it means a long analysis cannot be resumed tomorrow.
- **Scientific objects are sampled at 25.** A workbook with hundreds of plots is only partly checked,
  and the report says so. Data insertion is blocked while any sampled object is missing, so the
  sampling is safe, but a full check would be better.
- **Variables are never created.** The mapping proposes the datatype; creating the variable is a
  manual step. Copying one from a shared resource instance
  (`POST /core/variables/copy_from_shared_resource_instance`) is the obvious next addition.
- **The generic profile cannot insert data.** By design, but a guided mapping — the user naming the
  object and date columns themselves — would extend insertion to unknown files.
- **`cdb doctor` reports a false wiring problem** for this repository's `claude-db` hooks, unrelated
  to this module; noted here only because it confuses anyone running the diagnostics.

## Documentation

- `opensilex-doc/src/main/resources/how-to/modules.md` — creating an OpenSILEX module
- `opensilex-doc/src/main/resources/how-to/vuejs.md` — the front-end extension mechanism
- `opensilex-doc/src/main/resources/how-to/config.md` — the configuration system
- `opensilex-doc/src/main/resources/how-to/rest-api.md` — REST conventions
