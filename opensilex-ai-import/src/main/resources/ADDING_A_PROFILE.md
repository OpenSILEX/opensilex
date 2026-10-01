# Technical documentation : [`data import`] Adding an import profile to `opensilex-ai-import`

**Document history (please add a line when you edit the document)**

| Date       | Editor(s)        | OpenSILEX version | Comment           |
|------------|------------------|-------------------|-------------------|
| 2026-09-29 | Arnaud Charleroy | BUILD-SNAPSHOT    | Document creation |

> A step-by-step guide for a developer who wants the assistant to understand a new family of
> spreadsheets. Every step is illustrated with the STAR profile, the most complete one shipped.
> What the module does is described in [`ARCHITECTURE.md`](ARCHITECTURE.md); how it is built, in
> [`DESIGN.md`](DESIGN.md).

## Table of contents

<!-- TOC -->
* [Technical documentation : [`data import`] Adding an import profile to `opensilex-ai-import`](#technical-documentation--data-import-adding-an-import-profile-to-opensilex-ai-import)
  * [Table of contents](#table-of-contents)
  * [What a profile is, and what it is not](#what-a-profile-is-and-what-it-is-not)
  * [The contract](#the-contract)
  * [Step 1 — recognise the file](#step-1--recognise-the-file)
  * [Step 2 — tell the assistant the conventions](#step-2--tell-the-assistant-the-conventions)
  * [Step 3 — extract the names](#step-3--extract-the-names)
  * [Step 4 — say what each column is](#step-4--say-what-each-column-is)
  * [Step 5 (optional) — objects, treatments, events, data](#step-5-optional--objects-treatments-events-data)
  * [Step 6 — register the profile](#step-6--register-the-profile)
  * [Step 7 — test it](#step-7--test-it)
  * [Checklist](#checklist)
<!-- TOC -->

## What a profile is, and what it is not

A profile is **the knowledge of one family of import files**: which sheets it has, what its columns
mean, and how they map onto OpenSILEX concepts (experiment, facility, scientific object, variable…).

A profile **reads**. It never looks anything up in the instance and never writes. The module's rule
applies to it like to everything else:

> The language model proposes; Java resolves, validates and writes; a person confirms.

So a profile returns **names, in the file's own words**. Turning a name into a URI is the job of the
resolution (`resolve` package), and creating what is missing is the job of the creation service
(`create` package). A profile that is unsure returns nothing rather than guessing.

Four profiles are shipped, in [`profile/`](../java/org/opensilex/aiimport/profile):

| Id               | Class                   | File family                                         |
|------------------|-------------------------|-----------------------------------------------------|
| `vitis-explorer` | `VitisExplorerProfile`  | Grapevine observation template, one sheet per stage |
| `star`           | `StarProfile`           | STAR agronomic trial model, one entity per sheet    |
| `miappe`         | `MiappeProfile`         | MIAPPE v1.1 checklist spreadsheet                   |
| `generic`        | `GenericTabularProfile` | Anything else — the fallback                        |

The STAR profile is split over a few classes in
[`profile/star/`](../java/org/opensilex/aiimport/profile/star), a good layout to copy:

- `StarProfile` — the `ImportProfile` implementation;
- `StarSheets` — finds the sheets of the workbook by name or prefix;
- `StarDictionary` and `StarDictionaryEntry` — read the file's own column dictionary;
- `PlotIdReconciliation` — reconciles plot identifiers written two ways.

## The contract

A profile implements
[`ImportProfile`](../java/org/opensilex/aiimport/profile/ImportProfile.java). Five methods are
required; the others have a default that says "nothing to offer", so start without them.

| Method                | Required | What it feeds                                                  |
|-----------------------|----------|----------------------------------------------------------------|
| `getId`               | yes      | A stable identifier, used by the API and stored in sessions    |
| `getLabel`            | yes      | A short human label shown to the user                          |
| `match`               | yes      | The choice of the profile for an uploaded file                 |
| `getPromptContext`    | yes      | The system prompt: the conventions of the family, in prose     |
| `extract`             | yes      | The resolution report: names to look up, notes, anomalies      |
| `roleOf`              | no       | The column mapping panel and the type checks                   |
| `extractObjectRows`   | no       | Bulk creation of scientific objects                            |
| `objectSheetDefaults` | no       | The starting column → property mapping of each object sheet    |
| `extractFactorLevels` | no       | Creation of the experiment's factors and levels                |
| `extractEvents`       | no       | Creation of events (sprayings, incidents…)                     |
| `extractDataPoints`   | no       | Insertion of observation data                                  |

Every method receives a `WorkbookStructure` (package `workbook`): the file already read, sheet by
sheet, as `SheetStructure` objects holding headers and rows as strings. A profile never opens the
file itself.

A minimal skeleton:

```java
public class MyTemplateProfile implements ImportProfile {

    public static final String ID = "my-template";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getLabel() {
        return "My institute's trial template";
    }

    @Override
    public int match(WorkbookStructure structure) { … }

    @Override
    public String getPromptContext(WorkbookStructure structure) { … }

    @Override
    public ExtractedImportPlan extract(WorkbookStructure structure) { … }
}
```

## Step 1 — recognise the file

`match` returns a score: **0 means "not mine"**, and the highest score wins.
`ImportProfileRegistry.select` asks every profile and keeps the best. The generic profile always
scores 1, so it only wins when nothing else recognises the file.

STAR looks for its two signature sheets, and leaves room for a better match when only one is there:

```java
@Override
public int match(WorkbookStructure structure) {
    StarSheets sheets = new StarSheets(structure);
    boolean experiment = sheets.experiment().isPresent();
    boolean dictionary = sheets.hasDictionary();

    if (experiment && dictionary) {
        return 100;
    }
    // One of the two renamed is still a strong signal, but leave room for a better match.
    if (experiment || dictionary) {
        return 40;
    }
    return 0;
}
```

**Tip — find sheets by prefix, not by exact name.** Templates get revised: STAR's field sheet was
`parcelle`, then `ed_parcelle`, then `field…`, and its single `dictionary` sheet was split in two.
`StarSheets` keeps all of this in one place with prefixes (`ed_`, `data_`, `field`, `dictionary`),
so the rest of the profile asks for "the field sheet" and never cares how a revision spells it:

```java
public boolean hasDictionary() {
    for (SheetStructure sheet : workbook.getSheets()) {
        if (sheet.getName().toLowerCase(Locale.ROOT).startsWith(DICTIONARY_PREFIX)) {
            return true;
        }
    }
    return false;
}
```

## Step 2 — tell the assistant the conventions

`getPromptContext` returns **prose** inserted in the system prompt. Describe what the sheets and
columns mean and how they map onto OpenSILEX; do not describe the data, which the model receives
separately (and on demand, through its tools).

An excerpt of STAR's:

```java
return String.join("\n", Arrays.asList(
        "This workbook follows the STAR data model: normalised, one entity per sheet, and",
        "self-describing. Sheet names carry meaning through their prefix:",
        "",
        "- 'expe': the experiment itself — identifier, objective, description, start and end",
        "  dates, design plan, and the project it belongs to.",
        …
        "Mapping onto OpenSILEX, settled by the STAR to ELOA alignment:",
        "- 'expe' becomes the experiment. …",
        "- the field sheet becomes a facility, with its commune as address and its latitude",
        "  and longitude as location.",
        …
));
```

Keep it short: the system prompt is resent with every message, and `PromptBudgetTest` fails when it
goes over 6 500 estimated tokens.

## Step 3 — extract the names

`extract` returns an
[`ExtractedImportPlan`](../java/org/opensilex/aiimport/profile/ExtractedImportPlan.java): the names
to look up in the instance, sorted by kind. Each list becomes a category of the resolution report.

| Plan accessor                | Report category    |
|------------------------------|--------------------|
| `getExperimentNames()`       | experiments        |
| `getProjectNames()`          | projects           |
| `getGermplasmNames()`        | germplasm          |
| `getScientificObjectNames()` | scientific objects |
| `getFacilityNames()`         | facilities         |
| `getPersons()`               | persons            |
| `getOrganizationNames()`     | organisations      |
| `getVariables()`             | variables          |

Two maps add detail: `getOrganizationParents()` (a unit → its parent organisation) and
`getFacilityDetails()` (a facility → what the file says of it, to prefill the creation form).

Besides names, the plan carries:

- **notes**, with `note(key, value)`: facts worth telling the assistant, such as an objective or a
  design plan;
- **anomalies**, with `addAnomaly(ReportMessage.of(key, english))`: inconsistencies found while
  reading. The key is shown to the user in their language, so it must exist in both
  [`ai-import-en.json`](../../../front/src/lang/ai-import-en.json) and
  [`ai-import-fr.json`](../../../front/src/lang/ai-import-fr.json); the English text goes to the
  model.

STAR's `extract` is a list of small readers, one per entity, then the consistency checks:

```java
@Override
public ExtractedImportPlan extract(WorkbookStructure structure) {
    ExtractedImportPlan plan = new ExtractedImportPlan().setProfileId(ID);
    StarSheets sheets = new StarSheets(structure);
    StarDictionary dictionary = new StarDictionary(structure);

    readExperiment(sheets, plan);
    readField(sheets, plan);
    readPlots(sheets, plan);
    readTreatments(sheets, plan);
    readVariables(dictionary, plan);

    dictionary.getNotes().forEach(plan::addAnomaly);
    checkUndeclaredDataColumns(sheets, dictionary, plan);
    checkDataSheetsNameTheirTarget(sheets, plan);
    checkPlotIdentifiers(sheets, plan);

    return plan;
}
```

One reader, with its anomaly when the sheet is missing:

```java
private void readExperiment(StarSheets sheets, ExtractedImportPlan plan) {
    Optional<SheetStructure> found = sheets.experiment();
    if (!found.isPresent()) {
        plan.addAnomaly(ReportMessage.of(ANOMALY + "experimentSheetMissing",
                "The 'expe' sheet is missing, so the experiment this file describes cannot be "
                        + "identified."));
        return;
    }
    SheetStructure sheet = found.get();

    plan.getExperimentNames().addAll(sheet.distinctValues(COLUMN_EXPERIMENT));
    plan.getProjectNames().addAll(sheet.distinctValues(COLUMN_PROJECT));

    note(plan, "objective", sheet, COLUMN_OBJECTIVE);
    …
    readOrganizations(sheet, plan);
    …
}
```

Its key, `AiImport.report.anomaly.star.experimentSheetMissing`, is in both language files.

**Tip — put column names in constants.** `StarProfile` declares every column it reads as a public
constant (`COLUMN_EXPERIMENT = "expe_id"`, `COLUMN_FIELD = "field_id"`…). The STAR export
(`export/StarWorkbookBuilder`) reuses them, so reading and writing cannot drift apart.

## Step 4 — say what each column is

`roleOf` gives each column a `ColumnRole` (package `mapping`): `OBJECT`, `TRIAL`, `PROJECT`,
`GERMPLASM`, `FACTOR_LEVEL`, `LOCATION`, `POSITION`, `PARENT_OBJECT`, `OBJECT_PROPERTY`, `SEASON`,
`DATE`, `OBSERVER`, `PERSON`, `VARIABLE`, `COMMENT` or `UNKNOWN`. The column mapping panel and the
type checks are built on it.

The default recognises the headers listed in `HeaderRoleDictionary` (common agronomic headers, in
French and English) and calls everything else a `VARIABLE`. Override it as soon as the template is
known. STAR lets the file's own dictionary speak first, then its known columns:

```java
@Override
public ColumnRole roleOf(WorkbookStructure structure, String sheetName, String header) {
    if (header == null || header.isEmpty()) {
        return ColumnRole.UNKNOWN;
    }
    String lower = header.toLowerCase(Locale.ROOT);

    // The dictionary is the file's own statement of what a column is, so it wins.
    StarDictionary dictionary = new StarDictionary(structure);
    if (dictionary.isVariable(header)) {
        return ColumnRole.VARIABLE;
    }

    switch (lower) {
        case COLUMN_EXPERIMENT:
            return ColumnRole.TRIAL;
        case COLUMN_PLOT:
            return ColumnRole.OBJECT;
        case COLUMN_CULTIVAR:
            return ColumnRole.GERMPLASM;
        …
    }
    …
    // Described but not declared a variable, and not a column this profile knows: say so rather
    // than calling it a measurement.
    return dictionary.get(header).isPresent() ? ColumnRole.COMMENT : ColumnRole.UNKNOWN;
}
```

Prefer `UNKNOWN` to a wrong role: an unknown column is shown to the user, a wrong one is silently
used.

## Step 5 (optional) — objects, treatments, events, data

Each of these methods unlocks one feature. Keep the default (an empty result) and the feature is
simply not offered for the file.

**Scientific objects** — `extractObjectRows` returns one `ObjectRow` per object, with the sheet and
the row number as the user sees them (headers on row 1, so the first data row is 2). `setCells`
keeps every cell, so the user can map any column to a property of the type chosen for the sheet:

```java
for (SheetStructure sheet : sheets.objectSheets()) {
    String idColumn = StarSheets.identifierColumn(sheet);
    …
    rows.add(new ObjectRow(sheet.getName(), i + 2, name)
            .setGermplasm(cultivar.isEmpty() ? fieldCultivar : cultivar)
            .setFactorLevel(sheet.cell(row, COLUMN_TREATMENT))
            .setFacility(field)
            .setPosition(sheet.cell(row, prefix + "_x"), sheet.cell(row, prefix + "_y"))
            .setCells(sheet.getHeaders(), row));
}
```

`objectSheetDefaults` gives the starting point of that mapping, sheet by sheet: the name column,
a target per column (an `ObjectTargets` constant such as `NAME`, `GERMPLASM` or `X`, or a property
URI), and a suggested type when the file states one:

```java
return new ObjectSheetDefaults(idColumn, targets, suggested);
```

**Treatments** — `extractFactorLevels` returns one `FactorLevelCandidate` per level (STAR reads its
`modalite` sheet). They become the experiment's factors, which must exist before an object can name
its level.

**Events** — `extractEvents` returns `EventCandidate` objects (STAR reads `evenement` and `ppp`).

**Data** — `extractDataPoints` returns one `DataPoint` per value: the target's name, its
`DataPoint.TargetKind` (a scientific object, or a facility for weather measured at the field),
a date, a variable key and the raw value. Here more than anywhere, **return nothing rather than
guess**: a wrong identifier column files measurements against the wrong plots.

## Step 6 — register the profile

**Inside this module**, add it to the constructor of
[`ImportProfileRegistry`](../java/org/opensilex/aiimport/profile/ImportProfileRegistry.java):

```java
loaded.add(new VitisExplorerProfile());
loaded.add(new StarProfile());
loaded.add(new MiappeProfile());
loaded.add(new GenericTabularProfile());
```

**From another module**, without touching this one: the registry also loads profiles through
`ServiceLoader`. Add to that module a file
`src/main/resources/META-INF/services/org.opensilex.aiimport.profile.ImportProfile` holding the
fully qualified name of the class, one per line:

```text
org.example.mymodule.MyTemplateProfile
```

A contributed profile whose id is already taken is ignored.

## Step 7 — test it

1. Put a **real workbook** of the family in `src/test/resources` — ideally one per template
   revision, as STAR does with `STAR_standard.xlsx` and `STAR_exemple.xlsx`.
2. Add a loader to [`WorkbookFixture`](../../test/java/org/opensilex/aiimport/WorkbookFixture.java),
   which reads each file once and shares it between tests.
3. Write a `…ProfileTest` modelled on
   [`StarProfileTest`](../../test/java/org/opensilex/aiimport/profile/star/StarProfileTest.java):
   - the file is recognised, and the registry picks your profile for it;
   - a file of another family is **not** claimed (`match` returns 0);
   - `extract` finds the expected names, notes and anomalies;
   - `roleOf` gives the expected roles;
   - each optional extraction you implemented returns the expected rows.

   ```java
   @Test
   public void theRegistryPicksTheRightProfileForEachFile() throws Exception {
       ImportProfileRegistry registry = new ImportProfileRegistry();

       assertEquals(StarProfile.ID, registry.select(standard).getId());
       assertEquals(StarProfile.ID, registry.select(example).getId());
       assertEquals("a grapevine observation file must still get its own profile",
               VitisExplorerProfile.ID, registry.select(WorkbookFixture.vitis()).getId());
   }
   ```

4. Add your profile and its workbook to
   `TranslationKeyTest.everyKeyTheReportEmitsExistsInBothLanguages`, so a missing translation of one
   of your anomaly keys fails the build.
5. The module enforces **90 % line coverage** (`jacoco:check` in the `with-test-report` profile):
   new code comes with its tests.

## Checklist

- [ ] A class implementing `ImportProfile`, in its own package under `profile/`.
- [ ] `match` returns 0 for the other families, checked against the existing workbooks.
- [ ] Sheets found by prefix in a helper like `StarSheets`; column names in constants.
- [ ] A short `getPromptContext`; `PromptBudgetTest` still green.
- [ ] `extract` fills the plan; each anomaly key present in both language files.
- [ ] `roleOf` overridden; unknown columns say `UNKNOWN`.
- [ ] Optional extractions only where the file is unambiguous.
- [ ] Registered in `ImportProfileRegistry`, or through `META-INF/services`.
- [ ] A real workbook, a `…ProfileTest`, the profile added to `TranslationKeyTest`, coverage ≥ 90 %.

Traps met while writing the STAR profile:

- **Read by name, not by position.** Columns move from one revision to the next; STAR finds the
  identifier and the date columns by name and through its dictionary, never by index.
- **Support the earlier revisions.** A file in the wild is rarely the latest template; STAR reads
  both of its revisions, and tests both.
- **Say when you recompose.** STAR's data sheets write plot identifiers differently from its design
  sheet; `PlotIdReconciliation` recomposes them and records a note, so the user confirms it before
  anything is written.
- **Never default a type the file does not state.** STAR does not say what kind of scientific object
  a plot is: the profile raises the question (`askForTheObjectType`) and the user picks the type.
