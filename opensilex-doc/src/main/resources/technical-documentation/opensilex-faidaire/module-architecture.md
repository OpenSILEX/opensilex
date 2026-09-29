# Technical documentation : [`architecture`] `opensilex-faidare` module

**Document history (please add a line when you edit the document)**

| Date       | Editor(s)        | OpenSILEX version | Comment           |
|------------|------------------|-------------------|-------------------|
| 2026-09-29 | Arnaud Charleroy | BUILD-SNAPSHOT    | Document creation |

> ⚠️ _WARNING_ : written from the `develop` branch (commit `6725c2912`) by reading the code; nothing was built or run.
> This folder is named `opensilex-faidaire` (a misspelling of the module name `opensilex-faidare`) because that is where
> [Faidarev1.md](./Faidarev1.md), the endpoint mapping table, already lives; the new page was put next to it.

## Table of contents

<!-- TOC -->
* [Technical documentation : [`architecture`] `opensilex-faidare` module](#technical-documentation--architecture-opensilex-faidare-module)
  * [Table of contents](#table-of-contents)
  * [Definitions](#definitions)
  * [Solution](#solution)
    * [Module identity](#module-identity)
    * [Package map](#package-map)
    * [How a call is declared](#how-a-call-is-declared)
    * [Mapping to OpenSILEX models](#mapping-to-opensilex-models)
    * [Naming style and security](#naming-style-and-security)
  * [Tests](#tests)
  * [Limitations and improvements](#limitations-and-improvements)
  * [Documentation](#documentation)
<!-- TOC -->

## Definitions

- **FAIDARE** : the consumer for which this module tailors a subset of BrAPI 1.3 (the `Faidarev1` types and the
  `/faidare/` paths carry its name).
- **Builder** : a class that turns OpenSILEX models into one `Faidarev1*DTO`.

## Solution

### Module identity

- Module class: [FaidareModule](../../../../../../opensilex-faidare/src/main/java/org/opensilex/faidare/FaidareModule.java)
  extends `OpenSilexModule` and implements `APIExtension` with an **empty body**; no configuration and no lifecycle code.
- Maven dependencies: `opensilex-brapi` and `opensilex-front`, plus everything inherited from `opensilex-module`. Both are
  really used: `brapi` supplies `BrapiPaginatedListResponse` (all six response classes) and `BrAPIv1AccessionWarning`;
  `front` is used by `StudiesAPI` (`FrontModule.getConfigDTO(...).getRoutes()` builds the experiment URL, with
  `ServerModule.getAppUrl()`).

### Package map

Root package `org.opensilex.faidare`; 42 Java files.

| Package             | Files | Content                                                                                     |
|---------------------|-------|---------------------------------------------------------------------------------------------|
| `faidare` (root)    | 2     | `FaidareModule`, `Countries` (reads `countries.json`)                                        |
| `faidare/api`       | 8     | `CallsAPI`, `GermplasmAPI`, `LocationsAPI`, `ObservationVariablesAPI`, `StudiesAPI`, `TrialsAPI`, `FaidareCall`, `FaidareVersion` |
| `faidare/builder`   | 9     | `Faidarev1*DTOBuilder`: the mapping is done by builders, not by static `fromModel` methods    |
| `faidare/dal`       | 1     | `Faidarev1GermplasmDAO` extends the core `GermplasmDAO` with an optimised SPARQL search (`faidareSearch`) |
| `faidare/model`     | 16    | `Faidarev1*DTO` and `Faidarev1GermplasmModel`                                                |
| `faidare/responses` | 6     | `Faidarev1*ListResponse`                                                                     |

### How a call is declared

The mechanism mirrors `opensilex-brapi`: classes are `@Path("/faidare/")`, methods `@Path("v1/...")` with
`@FaidareVersion("1.3")`, and `FaidareCall` builds the `calls` answer by reflection. Endpoints: `calls`, `germplasm`,
`locations`, `variables`, `studies` and `trials`. Unlike `brapi`, there are no `studies/{id}`, `observations` or
`observationunits` sub-resources.

### Mapping to OpenSILEX models

Trial comes from `ProjectDAO` and `ExperimentDAO`; study from experiments and `DataDAO`; location from `FacilityLogic`;
variable from `VariableDAO`; germplasm from `Faidarev1GermplasmDAO`. The table of fields is in [Faidarev1.md](./Faidarev1.md).

### Naming style and security

Types are `Faidarev1*` (lower-case `v`) with no `API` prefix, and follow the external specification, as in `brapi`.
Every endpoint, **including `calls`**, is `@ApiProtected` whereas BrAPI's `calls` is public in `opensilex-brapi`. The
`@Api` tag is the constant group name `Faidare`.

## Tests

Seventeen files in `src/test/java/org/opensilex/faidare/api`: `FaidareAPITest` (the base class, extending core's
`AbstractMongoIntegrationTest`), five `*APITest` (germplasm, locations, observation variables, studies, trials) and eleven
`Test*Builder` fixture classes. `CallsAPI` has no test.

## Limitations and improvements

- `GermplasmAPI`, `StudiesAPI` and `CallsAPI` share their simple names with the `brapi` classes.
- The module has no `front/` directory although its parent pre-wires the front build.
- The documentation folder name is misspelled (`opensilex-faidaire`).

## Documentation

- [Faidarev1.md](./Faidarev1.md) : endpoint and field mapping.
- [../opensilex-brapi/module-architecture.md](../opensilex-brapi/module-architecture.md) : the module whose response classes
  and call mechanism this one reuses.
- [../architecture/modules-overview.md](../architecture/modules-overview.md).
