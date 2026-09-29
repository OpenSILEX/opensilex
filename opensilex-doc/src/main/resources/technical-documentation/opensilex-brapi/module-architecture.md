# Technical documentation : [`architecture`] `opensilex-brapi` module

**Document history (please add a line when you edit the document)**

| Date       | Editor(s)        | OpenSILEX version | Comment           |
|------------|------------------|-------------------|-------------------|
| 2026-09-29 | Arnaud Charleroy | BUILD-SNAPSHOT    | Document creation |

> ⚠️ _WARNING_ : written from the `develop` branch (commit `6725c2912`) by reading the code; nothing was built or run.
> The endpoint-by-endpoint mapping to OpenSILEX concepts is in [BrAPIV1.md](./BrAPIV1.md); this page describes how the
> module is built.

## Table of contents

<!-- TOC -->
* [Technical documentation : [`architecture`] `opensilex-brapi` module](#technical-documentation--architecture-opensilex-brapi-module)
  * [Table of contents](#table-of-contents)
  * [Definitions](#definitions)
  * [Solution](#solution)
    * [Module identity](#module-identity)
    * [Package map](#package-map)
    * [How a call is declared](#how-a-call-is-declared)
    * [Endpoints and the models behind them](#endpoints-and-the-models-behind-them)
    * [Naming style](#naming-style)
    * [Security](#security)
  * [Tests](#tests)
  * [Limitations and improvements](#limitations-and-improvements)
  * [Documentation](#documentation)
<!-- TOC -->

## Definitions

- **BrAPI** : the Breeding API specification. This module implements a read-only subset of versions 1.2 and 1.3.
- **Call** : one BrAPI endpoint, declared by a method carrying `@Path` and `@BrapiVersion`.

## Solution

### Module identity

- Module class: [BrapiModule](../../../../../../opensilex-brapi/src/main/java/org/opensilex/brapi/BrapiModule.java) extends
  `OpenSilexModule` and implements `APIExtension` with an **empty body**: no configuration class, no configuration key, no
  `install` or `startup`. Its REST resources are found by the default scan of every `@Path` class.
- The module declares no Maven dependency on another `opensilex-*` module. It inherits main, sparql, nosql, fs, core,
  security and front from its parent `opensilex-module`, and its Java code imports `core` heavily (about 70 import lines),
  as well as `sparql`, `security` and `nosql`.
- No main resources and no tests. `front/` holds only an `index.ts` that binds the generated client, which the Vue application
  does not load.

### Package map

Root package `org.opensilex.brapi`; 38 Java files.

| Package            | Files | Content                                                                                        |
|--------------------|-------|------------------------------------------------------------------------------------------------|
| `brapi` (root)     | 3     | `BrapiModule`, `BrapiPaginatedListResponse`, `BrapiDataResponsePart` (the BrAPI `metadata` + `result.data` envelope) |
| `brapi/api`        | 6     | `CallsAPI`, `GermplasmAPI`, `StudiesAPI`, `VariablesAPI`, `BrapiCall`, `BrapiVersion`           |
| `brapi/model`      | 20    | The `BrAPIv1*DTO` classes                                                                       |
| `brapi/responses`  | 9     | `BrAPIv1*ListResponse` and `BrAPIv1SingleStudyResponse` classes                                  |

### How a call is declared

Each API class is annotated `@Api("BRAPI")` and `@Path("/brapi/")` and extends the package-private abstract class `BrapiCall`.
Each method carries `@Path("v1/...")` and `@BrapiVersion("1.3")` (a runtime annotation). `BrapiCall.getBrapiCallsInfo()`
scans the subclasses by reflection and builds the answer of `GET /brapi/v1/calls` from the path, the HTTP verb, the
`@Produces` value and the version of each method: the list of calls is generated, not written by hand.

### Endpoints and the models behind them

| Endpoint                                                             | Backed by                                              |
|----------------------------------------------------------------------|--------------------------------------------------------|
| `GET /brapi/v1/calls`                                                | The reflection scan above                              |
| `GET /brapi/v1/germplasm`                                            | `GermplasmDAO.brapiSearch`                             |
| `GET /brapi/v1/studies`, `studies-search` (version 1.2), `studies/{id}`, and its `observations`, `observationvariables`, `observationunits` sub-resources | `ExperimentDAO` and `ExperimentModel`, `DataModel`, `ScientificObjectLogic` |
| `GET /brapi/v1/variables` and `variables/{id}`                       | `VariableDAO`                                          |

Other mappings: location is `FacilityModel`, contact is `PersonModel`, method and scale are `MethodModel` and `UnitModel`.
Conversion is a static `fromModel(...)` on each DTO.

### Naming style

The DTOs follow the **external specification**, not the platform convention: they are named `BrAPIv1<Name>DTO`, sit in a
`model` package, and their fields keep the BrAPI names (`germplasmDbId`, `germplasmPUI`, `defaultDisplayName`), almost
without `@JsonProperty`. Query parameters mix styles, for example `page_size` on germplasm and `pageSize` on `calls`.
The rules in [java-naming-conventions.md](../architecture/java-naming-conventions.md) apply to the native modules, not to this
one and `opensilex-faidare`.

### Security

Every endpoint except `calls` is `@ApiProtected`. No method carries `@ApiCredential`, so any logged-in user can call them.
`CallsAPI` carries an `@ApiCredentialGroup` (`brapi-calls`) that registers nothing, since no method declares a credential.

## Tests

None in this module.

## Limitations and improvements

- The module is read-only and covers a small part of BrAPI.
- `GermplasmAPI`, `StudiesAPI` and `CallsAPI` exist in both `brapi` and `faidare` (and `GermplasmAPI` also in `core`), which
  makes stack traces and imports easy to confuse.
- The `pom.xml` of this module configures a generated `ServiceLoader` file for `org.opensilex.brapi.api.CallsAPI`, a class that
  nothing loads through `ServiceLoader`.
- Because of the inherited dependencies, `brapi` can depend on any `core` class without declaring it.

## Documentation

- [BrAPIV1.md](./BrAPIV1.md) : endpoint mapping.
- [../opensilex-faidaire/module-architecture.md](../opensilex-faidaire/module-architecture.md) : the module that reuses its
  response classes.
- [../architecture/modules-overview.md](../architecture/modules-overview.md).
