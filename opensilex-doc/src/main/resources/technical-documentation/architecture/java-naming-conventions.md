# Technical documentation : [`architecture`] `Java naming conventions`

**Document history (please add a line when you edit the document)**

| Date       | Editor(s)        | OpenSILEX version | Comment           |
|------------|------------------|-------------------|-------------------|
| 2026-09-29 | Arnaud Charleroy | BUILD-SNAPSHOT    | Document creation |

> ⚠️ _WARNING_ : this is a **survey-based** document. The project has almost no written naming rules (see
> [Where the rules come from](#where-the-rules-come-from)), so most of what follows was derived from the code of the
> `develop` branch at commit `6725c2912` (1,011 main and 132 test Java files in the built modules), counted with
> regular expressions. Counts are indicative and will drift; they were not produced by a Java parser.
>
> Each rule carries a strength tag, defined in [How to read the rules](#how-to-read-the-rules). Nothing here was decided by
> a vote: a `[C]` or `[T]` rule is the majority practice, proposed for new code.

## Table of contents

<!-- TOC -->
* [Technical documentation : [`architecture`] `Java naming conventions`](#technical-documentation--architecture-java-naming-conventions)
  * [Table of contents](#table-of-contents)
  * [Definitions](#definitions)
  * [Where the rules come from](#where-the-rules-come-from)
  * [How to read the rules](#how-to-read-the-rules)
  * [Packages](#packages)
  * [Types](#types)
    * [Suffix per role](#suffix-per-role)
    * [DTO names](#dto-names)
    * [Acronym casing](#acronym-casing)
    * [Names that must match the layer](#names-that-must-match-the-layer)
  * [Methods](#methods)
    * [DAO and Logic methods](#dao-and-logic-methods)
    * [DTO and model conversion](#dto-and-model-conversion)
    * [Booleans, getters and setters](#booleans-getters-and-setters)
    * [REST resource methods](#rest-resource-methods)
  * [Fields and constants](#fields-and-constants)
    * [Constants](#constants)
    * [Credential constants](#credential-constants)
    * [Logger, enums, generics](#logger-enums-generics)
  * [JSON and REST naming](#json-and-rest-naming)
  * [RDF, ontology and MongoDB naming](#rdf-ontology-and-mongodb-naming)
  * [Tests](#tests)
  * [File headers and documentation comments](#file-headers-and-documentation-comments)
  * [Branches, commits and merge requests](#branches-commits-and-merge-requests)
  * [New concept checklist](#new-concept-checklist)
  * [Known deviations](#known-deviations)
  * [Limitations and improvements](#limitations-and-improvements)
  * [Documentation](#documentation)
<!-- TOC -->

## Definitions

- **Native modules** : `main`, `sparql`, `nosql`, `fs`, `security`, `core`, `front`, `phis`, `migration`, `graphql`,
  `dev-tools`. Excludes `brapi` and `faidare`, whose types mirror the naming of external specifications.
- **Concept** : a domain notion with its own `api` and `dal` packages (see the [core module page](../opensilex-core/module-architecture.md)).
- **DTO** : a data transfer object, the JSON shape of a REST message.

## Where the rules come from

| Source                                                            | What it says about naming                                                                                       | Strength      |
|-------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------|---------------|
| [code-organization.md](./code-organization.md)                    | The only document that prescribes Java layout and names: `MyConfig` and `MyModule` at the package root; `cli/ConceptCommands`; `concept/api` with `ConceptAPI`, `ConceptGetDTO`, `ConceptCreationDTO`; `concept/dal` with `ConceptModel`, `ConceptDAO`; tests mirror the packages. `dal` must not refer to `api` or DTOs | Prose         |
| [README.md](./README.md)                                          | The layer vocabulary: model, DAO, `Logic`, DTO, API, command, service, service connection, extension interface  | Prose         |
| [`how-to/config.md`](../../how-to/config.md)                      | The configuration interface is `MyModuleConfig`, next to `MyModule`                                              | Prose         |
| [`how-to/cli.md`](../../how-to/cli.md)                            | Command classes are `<MyGroupId>Commands`. It names a base class `CLIHelpPrinterCommand` that does not exist; the real one is `AbstractOpenSilexCommand` | Prose |
| [`CONTRIBUTING.md`](../../../../../../CONTRIBUTING.md)                        | "Common Java style", pointing to the Google Java Style guide for formatting; the OpenSILEX header must be added to each file. It admits that not all of the code base follows the style | Prose |
| Checkstyle, in `opensilex-parent/pom.xml`                         | An inline Sun-like ruleset. Its naming checks (`ConstantName`, `MethodName`, `PackageName`, `TypeName`, ...) use the **default** patterns; no custom format exists. Also `LineLength` 150 and `AvoidStarImport` | Warning only  |
| `.gitlab-ci.yml`, job `merge-request:check`                       | Branch names and merge request titles (see [Branches, commits and merge requests](#branches-commits-and-merge-requests)) | Hard gate on merge requests |

Checkstyle only runs in the `with-test-report` profile, which the `sonar-analysis` job activates on `develop` with
`allow_failure`; the merge request build has no style gate. There is no `.editorconfig`, no formatter configuration, no PMD
ruleset and no `sonar-project.properties` in the repository, and nothing in it constrains suffixes (`DTO`, `DAO`), acronym casing or
package layout.

## How to read the rules

| Tag   | Meaning                                                                                                  |
|-------|----------------------------------------------------------------------------------------------------------|
| `[P]` | Prescribed in a document or enforced by the build (see the table above)                                   |
| `[C]` | Convention: followed by roughly 85 % or more of the code that can follow it                              |
| `[T]` | Tendency: a majority practice that is far from unanimous; the counts are given so you can judge           |

Where the code has no majority, the rule says "follow the neighbours": use the style of the package you are editing.

## Packages

| Rule                                                                                                                             | Tag   | Evidence                                                                                             |
|----------------------------------------------------------------------------------------------------------------------------------|-------|------------------------------------------------------------------------------------------------------|
| The root package of a module is `org.opensilex.<module>`, without the `opensilex-` prefix (`org.opensilex.core`)                   | `[C]` | All modules; `main` is `org.opensilex`; `dev-tools` is `org.opensilex.dev`, not `devtools`; the codegen plugin is `io.swagger.codegen.plugin` |
| A concept has an `api` package and a `dal` package, plus a `bll` package when it has business rules                                 | `[P]` for `api`/`dal`, `[T]` for `bll` | 28 of the 90 top-level packages have both `api` and `dal` (22 of 32 in `core`, 5 of 11 in `security`); `bll` exists in 7 concepts of `core` and is not in [code-organization.md](./code-organization.md) |
| Infrastructure modules are organised by technical function (`service`, `config`, `cli`, `mapping`, `csv`, ...), not by `api`/`dal`  | `[C]` | `main`, `sparql`, `nosql`, `fs`, `migration`, `dev-tools` have no `api`/`dal` split                   |
| Package names are lower case, one word per segment, no underscore                                                                  | `[C]` | 200 of 214 directories that hold Java; the 14 exceptions cover 101 files (below)                      |

Layer packages and their leaf vocabulary: `api` (38 directories), `dal` (32), `bll` (7), `utils` (5), `model` (4), `config` (4),
`cli` (4), `service`, `extensions`, `exceptions`, `csv`, `validation`, `ontology` (3 each). Singular and plural are mixed:
`exceptions` (`main`, `nosql`, `sparql`) against `exception` (`core`), `response` (`main`, `sparql`) against `responses`
(`brapi`, `faidare`), `filters` against `filter`. Choose the form already used by the module.

Existing exceptions to the lower-case rule: camel-case segments in `core` (`scientificObject`, `variablesGroup`,
`germplasmGroup`, `uriSearch`, `sharedResource`, `dataImport`, `batchHistory`, `entityOfInterest`), `front`
(`vueOwlExtension`), `main` (`multipleError`), `sparql` (`schemaQuery`), `fs` (`transferManager`); the package
`org.opensilex.migration.one_point_five_ALL` (the only one with an underscore); and the typo `utils/functionnal`. Do not
copy them. Six files in `core/data/api/spectra` sit in a directory `spectra` but declare `package org.opensilex.core.data.api`.

## Types

### Suffix per role

One top-level type per file, and the file name is the type name. Counts are over the native and adapter modules' main sources.

| Role                          | Suffix or form          | Tag   | Count and where it lives                                                                    |
|-------------------------------|-------------------------|-------|---------------------------------------------------------------------------------------------|
| REST resource                 | `<Concept>API`          | `[C]` | 50 `API` against 2 `Api`; all under an `api` package                                         |
| Data access object            | `<Concept>DAO`          | `[C]` | 38 `DAO` against 7 `Dao`; all in `dal` except four MongoDB base types in `nosql/mongodb/dao` and `metadata` |
| Persisted model               | `<Concept>Model`        | `[C]` | 93; 70 in `dal`, the others in `sparql.model`, `migration`, `core.external`                  |
| Business logic                | `<Concept>Logic`        | `[C]` | 14, all in `core` `bll` packages                                                            |
| Message body                  | `<Concept><Kind>DTO`    | `[C]` | About 268 DTOs, see [DTO names](#dto-names)                                                  |
| Search parameters             | `<Concept>SearchFilter` | `[T]` | One per searchable concept, in `dal` (one, `GermplasmSearchFilter`, is in `api`)             |
| Module class                  | `<Name>Module`          | `[P]` | 14, one per module, at the root of the module package                                        |
| Module configuration          | `<Name>Config`          | `[P]` | 41 interfaces; a module's configuration interface sits beside its `*Module` class            |
| Command group                 | `<Group>Commands`       | `[P]` | 5 `Commands` against 3 `Command`, in `cli` packages                                          |
| Extension interface           | `<Name>Extension`       | `[C]` | 8, seven of them interfaces                                                                  |
| Exception                     | `<Description>Exception`| `[C]` | 58; 32 in `exceptions`, 14 in `exception`, 12 elsewhere                                       |
| Service and connection        | `<Name>Service`, `<Name>Connection` | `[T]` | 18 and 9; `fs` has seven connections                                                 |
| Helpers                       | `<Name>Utils`           | `[T]` | 10 `Utils`, 2 `Helper`, no `Util`                                                            |
| Template and null variants    | `Abstract<Name>`, `Base<Name>`, `Default<Name>`, `No<Name>` | `[T]` | 10, 7, 3 and a few; no `Impl` suffix and no `I` prefix anywhere |

Do not use an `Impl` suffix or an `I` prefix: neither occurs in the code base.

### DTO names

There is no single suffix vocabulary. The forms in use, over 268 DTO classes:

| Form                               | Count | Use it for                                                        |
|------------------------------------|-------|-------------------------------------------------------------------|
| `<Concept>GetDTO`                  | 44    | Reading one item                                                  |
| `<Concept>CreationDTO`             | 36    | Creating (never `CreateDTO`, which does not exist)                |
| `<Concept>UpdateDTO`               | 26    | Updating                                                          |
| `<Concept>DetailsDTO`              | 13    | A detailed read (`DetailDTO` also exists, 3 times: prefer `Details`) |
| `<Concept>ListDTO`, `SearchDTO`, `ExportDTO`, `ConfigDTO` | 4, 3, 5, 8 | List items, search bodies, exports, configuration |
| `<Concept>DTO`                     | 93 (with other forms) | A single shape used for read and write, or a neutral name |
| `BrAPIv1*`, `Faidarev1*`           | 33    | Mirror of an external API (in `brapi` and `faidare` only)         |

Rule `[C]` for new concepts: `<Concept>GetDTO`, `<Concept>CreationDTO`, `<Concept>UpdateDTO`, as prescribed by
[code-organization.md](./code-organization.md) for the first two.

A DTO is normally in an `api` package (212 of 268). Outside it: `brapi` and `faidare` `model` packages (35), `main.server.response`
(8), `sparql.response` (7), and a few in `core.utils`, `core.sharedResource` and `core.ontology`.

### Acronym casing

Names never mix two spellings of the same acronym; the choice is per name. Counts are over type names.

| Acronym              | Upper case | Mixed case | Rule                                                                       |
|----------------------|------------|------------|----------------------------------------------------------------------------|
| `DTO`                | 281        | 0          | `[C]` `DTO`                                                                |
| `API`                | 53         | 19         | `[C]` `API` (`GermplasmGroupApi` and `UriSearchApi` are the only `Api` classes) |
| `DAO`                | 40         | 11         | `[C]` `DAO`; the `Dao` group is the MongoDB family (`DataDaoV2`, `MongoReadDao`, ...) |
| `SPARQL`             | 64         | 15         | `[T]` `SPARQL`; the `Sparql` group is `SparqlNoProxyFetcher`, `SparqlUrisQuery`, `SparqlSchema`, ... |
| `URI`                | 22         | 21         | No majority: follow the neighbours                                          |
| `CSV`                | 11         | 20         | `[T]` `Csv`                                                                |
| `OWL`                | 1          | 6          | `[T]` `Owl`                                                                |
| `NoSQL`              | 6          | 3          | `[T]` `NoSQL`                                                              |
| `OpenSilex`          | 12 (`OpenSilex`) | 2 (`Opensilex`) | `[C]` `OpenSilex`; `OpenSILEX` is never used in a type name         |

Test classes follow the same split: `*APITest` 33 against `*ApiTest` 13, `*DAOTest` 8 against `*DaoTest` 2.

### Names that must match the layer

The name and the package agree in the whole code base, which is what makes a class findable:

- no `*DTO` in a `dal` package, no `*DAO` or `*Model` in an `api` package, no `*API` outside an `api` package;
- a `dal` package may hold exporters, contexts and filter builders next to DAOs and models (15 such types), and an `api` package
  holds 22 helper types (parsers, converters, CSV importers) next to APIs and DTOs.

## Methods

Counts come from 38 DAO classes (555 methods) and 52 API classes.

### DAO and Logic methods

| Purpose                     | Name                                         | Tag   | Evidence                                                                           |
|-----------------------------|----------------------------------------------|-------|------------------------------------------------------------------------------------|
| Read one item               | `get(URI ...)`                               | `[T]` | Exact name in 22 of 38 DAOs; `getByName` 3 times                                    |
| Read several items by URI   | `getList(List<URI>)`                         | `[T]` | 13 uses; the alternatives are `getByURIs`, `searchByURIs`, `getAll` (2 to 3 each)   |
| Paginated search            | `search(...)`, or `search<Something>(...)`   | `[C]` | 24 DAOs have a `search`; 26 of 29 return `ListWithPagination<T>`                     |
| Create, update, delete      | `create`, `update`, `delete`                 | `[C]` | 56, 45 and 41 methods; `add` (17) and `upsert` (3) are marginal                     |
| Existence                   | `exists(URI)` or `<thing>Exists`             | none  | `exists` 10 times, `exist` 7 (in `fs`), 11 `is<X>` methods: no majority              |
| Count                       | `count<...>`                                 | `[T]` | 11 in DAOs, 10 in APIs                                                              |
| Validation                  | `validate<...>`, `check<...>`                | `[T]` | 7 and 10                                                                            |

`find*` is not used in any DAO: use `get` or `search`. Private SPARQL helper methods are prefixed `append` (68 of them).

### DTO and model conversion

There is no mapper class: the conversion lives on the DTO.

| Method                                    | Count | Meaning                                                                                   |
|-------------------------------------------|-------|-------------------------------------------------------------------------------------------|
| `fromModel`                               | 89    | 48 are `static XxxDTO fromModel(XxxModel)`, 41 are instance overrides `void fromModel(T)`  |
| `toModel`                                 | 45    | Instance `void toModel(T model)` that fills an existing model                              |
| `newModel()` / `newModelInstance()`       | 24 / 23 | Creates a fresh model from a creation DTO; the second is the generic one in base DTOs   |
| `getDTOFromModel`                         | 23    | Static, polymorphic. `getDtoFromModel` also exists (6): use `DTO`                          |
| a DTO constructor taking the model        | 26    | For example `AnnotationGetDTO(AnnotationModel)`                                            |

`[T]` for a new DTO: a static `fromModel` for get DTOs, `newModel()` for creation DTOs.

### Booleans, getters and setters

- Boolean accessors: `is<X>` (114), then `has<X>` (14); `can<X>` is never used `[C]`.
- Avoid `getIs<X>` and `getHas<X>`, which exist but are the deviations (`PaginationDTO.getHasNextPage`,
  `ScientificObjectClassDTO.getIsAbstractClass`, `VueRDFTypeDTO.getIsAbstract`).
- Boolean fields are bare in 38 cases, `is`-prefixed in 31 and `has`-prefixed in 3; JSON names often start with `is_`.
- Setters return `void` in models (372 against 13 fluent) `[C]`. DTOs have 945 `void` setters and 191 fluent ones (23 DTOs,
  mostly in `faidare` and a few in `core`): use `void` `[T]`.

### REST resource methods

379 endpoints in 52 classes; in the native modules 311 of 363 (85.7 %) follow this mapping, the others being action names.

| HTTP verb | Method name prefix                                | Tag   |
|-----------|---------------------------------------------------|-------|
| `GET`     | `get<Entity>` for one item (`@Path("{uri}")`), `search<Entities>` for a paginated list, `count<...>`, `export<...>` | `[C]` |
| `POST`    | `create<Entity>`; `search<Entities>` for a search with a body; `import<...>`, `validate<...>`, `upload<...>`, `export<...>` | `[C]` |
| `PUT`     | `update<Entity>`                                  | `[C]` |
| `DELETE`  | `delete<Entity>`                                  | `[C]` |

By URIs: `get<Entities>ByURIs` on `GET by_uris` (14, several now deprecated) and `search<Entities>ByURIs` on `POST` (22). Twenty-three
paginated `GET` methods are named `get*` rather than `search*` (8 `brapi`, 6 `faidare`, 9 in `core`): prefer `search`.
In method names, `Uri(s)` (413) is more frequent than `URI(s)` (282), the reverse of type names: follow the neighbours.

## Fields and constants

### Constants

`static final` constants are UPPER_SNAKE_CASE in 93.8 % of the cases once the RDF vocabulary constants are set aside
(1,006 of 1,072) `[C]`. Vocabulary constants are `Property` and `Resource` objects and keep the RDF name (below).

Suffixes in use:

| Suffix         | Count | Meaning                                                                        |
|----------------|-------|--------------------------------------------------------------------------------|
| `_FIELD`       | 194   | Name of a field: model attributes, MongoDB fields (`URI_FIELD`)                  |
| `_KEY`         | 93    | Keys, mostly `_LABEL_KEY` (66) for translation keys                              |
| `_ID`          | 83    | Identifiers, mostly credentials                                                  |
| `_PATH`        | 47    | Paths                                                                            |
| `_NAME`        | 45    | Names, for example `<X>_COLLECTION_NAME` for MongoDB collections                 |
| `_VAR`         | 35    | SPARQL variable names                                                            |
| `_URI`         | 29    | URIs; examples in Swagger annotations are `*_EXAMPLE_URI` and `*_EXAMPLE_TYPE`   |
| `_PREFIX`, `_GRAPH`, `_MSG`, `_TYPE` | 28, 24, 20, 11 | Prefixes, graph names, messages, types           |

Each REST class that declares its route calls the constant `PATH` (22 of 52 classes), for example
`ExperimentAPI.PATH = "/core/experiments"`. `_API_PATH` is never used.

Deviations, not to be copied: `EventDAO` and `MoveEventDAO` hold about 30 camel-case constants (`uriVar`, `beginTriple`);
`Ontology` has `subClassAny`; `SingleCriteriaDTO.variableField` is used as a `@JsonProperty` value.

### Credential constants

Credentials are declared as constants in the API class, in this shape (from `ExperimentAPI`):

```java
@Api(ExperimentAPI.CREDENTIAL_EXPERIMENT_GROUP_ID)
public static final String CREDENTIAL_EXPERIMENT_GROUP_ID = "Experiments";
public static final String CREDENTIAL_EXPERIMENT_GROUP_LABEL_KEY = "credential-groups.experiments";
public static final String CREDENTIAL_EXPERIMENT_MODIFICATION_ID = "experiment-modification";
public static final String CREDENTIAL_EXPERIMENT_MODIFICATION_LABEL_KEY = "credential.default.modification";
```

There are 135 `CREDENTIAL_*` constants in 28 files. Group identifiers are PascalCase plurals, action identifiers are kebab-case
(41 of 46), and label keys are `credential-groups.<x>` or `credential.default.modification` / `credential.default.delete`
`[C]`. Five classes reverse the order to `CREDENTIAL_GROUP_<X>_ID` (`AccountAPI`, `UserAPI`, `PersonAPI`, `FacilityAPI`,
`OrganizationAPI`) and so does `GroupAPI`: use `CREDENTIAL_<X>_GROUP_ID`.

### Logger, enums, generics

- Logger: `private static final Logger LOGGER = LoggerFactory.getLogger(X.class);` with slf4j `[C]` (`LOGGER` in 93 of 110
  declarations, `logger` in 16). The only `java.util.logging` use is `MetricDTO`.
- Enum constants are UPPER_SNAKE (27 of 34); `MathematicalOperator` uses PascalCase.
- Generic type parameters: `T` (78 of 93 at class level, 161 of 192 at method level), then `F` for a filter type and `E` for
  an exception type. Multi-letter names with an underscore (`T_RESULT`, `T_JOINED`) exist in `MongoReadDao` and are rare.
- Fields are lowerCamelCase with no prefix: no `m` prefix, and only two leading underscores (`_INSTANCE`).
- `serialVersionUID` is declared in 2 of 65 exception classes: it is not a convention here.

## JSON and REST naming

| Rule                                                                                                                              | Tag   | Evidence                                                                                             |
|-----------------------------------------------------------------------------------------------------------------------------------|-------|------------------------------------------------------------------------------------------------------|
| Java fields are camelCase; the JSON name of a multi-word property is snake_case, set with `@JsonProperty` on each field            | `[C]` | No global naming strategy exists. About 250 snake_case literals against about 20 camel-case ones; 84.9 % of multi-word DTO fields are annotated |
| One-word properties need no annotation                                                                                              | `[C]` | About 230 one-word literals                                                                          |
| Some properties are renamed rather than converted                                                                                   | -     | `type` to `rdf_type`, `typeLabel` to `rdf_type_name`, `publicationDate` to `issued`, `lastUpdatedDate` to `modified` |
| Resource classes are mounted at `/<module>/<plural noun>`, with snake_case for several words                                         | `[C]` | 30 `/core/<plural>`, 6 `/security/*`; `scientific_objects`, `entities_of_interest`, `uri_search`. No kebab-case class path |
| A single item is at `{uri}`                                                                                                           | `[C]` | 102 uses; BrAPI uses `studyDbId` and similar                                                        |
| Method paths are one word, `{uri}`, or snake_case                                                                                     | `[T]` | 171 one word or `{uri}`, 74 snake_case, 9 kebab-case, 3 camelCase                                    |
| Query parameters are snake_case (`page_size`, `start_date`)                                                                           | `[T]` | 242 snake_case against 33 camelCase in native modules; `page_size` 52 against `pageSize` 4          |
| `@ApiOperation` is a capitalised imperative phrase without a final full stop                                                          | `[C]` | 97.4 % capitalised, 97.4 % without a dot. First words: Get 127, Search 55, Delete 38, Update 34, Add 25, Return 22, Create 10 |
| `@Api` takes the credential group constant                                                                                             | `[T]` | 37 of 52 classes; 6 use the security constants, 9 a literal                                          |
| Every REST method returns `javax.ws.rs.core.Response`, built with `new <Wrapper>(...).getResponse()`                                  | `[C]` | All 379 endpoints; wrappers `ErrorResponse`, `PaginatedListResponse`, `SingleObjectResponse`, `ObjectUriResponse`, `CreatedUriResponse` |

`brapi` and `faidare` follow the BrAPI specification: camel-case fields such as `germplasmDbId`, almost no `@JsonProperty`, and
camel-case query parameters.

## RDF, ontology and MongoDB naming

| Element                                | Rule                                                                                                                   | Tag   |
|----------------------------------------|------------------------------------------------------------------------------------------------------------------------|-------|
| `@SPARQLResource(resource = ...)`      | PascalCase string equal to the class name without `Model`                                                              | `[C]` 35 of 44; `AccountModel` is `OnlineAccount`, `InterestEntityModel` is `EntityOfInterest` |
| `@SPARQLResource(graph = ...)`         | The constant `<X>Model.GRAPH`, a lower-case singular string (`"experiment"`, `"variable"`)                              | `[C]` 32 of 36 use the constant; `"scientific-object"` and `"ObservationCollection"` are exceptions |
| `@SPARQLResource(prefix = ...)`        | A short lower-case code for generated URIs (`expe`, `prj`, `orga`, `grp`, `doc`, `device`, `so`)                          | `[T]` 17 uses |
| `@SPARQLProperty(property = ...)`      | A lowerCamel string (198 of 199); underscores only in external vocabularies                                             | `[C]` |
| Java field against RDF property name   | No rule: identical in 59 cases, `has`/`is` dropped in 37, different in 103                                              | none  |
| Vocabulary classes                     | RDF classes are PascalCase `Resource` constants (`Oeso.Experiment`), properties are lowerCamel `Property` constants (`Oeso.hasFactor`) | `[C]` |
| MongoDB models                         | `<X>Model extends MongoModel`, `<X>_FIELD` constants; field values are camelCase (`rdfType`) unlike the snake_case JSON      | `[C]` |

`@SPARQLProperty` has the attributes `ontology`, `property`, `required`, `inverse`, `ignoreUpdateIfNull`, `cascadeDelete`,
`autoUpdate` and `useDefaultGraph`. Vocabulary classes: `Oeso`, `Oeev`, `SOSA` and a copy of `Time` in `core.ontology`,
`Time` in `sparql.model.time`, `SHACL` and `Ontology` in `sparql.utils`, `SecurityOntology` in `security.authentication`,
`VueOwlExtension` in `front.vueOwlExtension`; `OesoSecurity` and `OesoPhis` hold strings only. The namespace constant is a
`Resource` in `Oeso` and `Oeev` but a `String` in `SecurityOntology` and `SHACL`. The two `Time` classes share a simple name.

## Tests

| Rule                                                                                                          | Tag   | Evidence                                                                 |
|---------------------------------------------------------------------------------------------------------------|-------|--------------------------------------------------------------------------|
| Test classes end in `Test` (never `Tests` or `IT`); `<Concept>APITest`, `<Concept>DAOTest`                     | `[C]` | 101 of 132 files; 33 `APITest` and 8 `DAOTest`                            |
| Test methods are `test<Something>`                                                                             | `[T]` | 501 of 654 (76.6 %); 123 start directly with a verb; 22 use underscores; none uses `should` or `given` |
| JUnit 4 (`org.junit.Test`); no JUnit 5                                                                         | `[C]` | 82 files, no Jupiter import                                              |
| A test lives in the same package as the class it tests                                                         | `[T]` | 114 of 132 files sit in a package of the same module's main sources      |
| Test resources are snake_case (`os_import_basic.csv`) in a folder named after the concept                       | `[T]` | 49 snake_case against 19 camelCase among 74 files                        |

Base classes: `AbstractUnitTest` (10 subclasses), `AbstractSecurityIntegrationTest` (25) and `AbstractMongoIntegrationTest` (37);
choose by what the test needs, as described in [tests.md](./tests.md). Fixture builders in `faidare` are `Test<Something>Builder`.
The DAO tests are integration tests, contrary to the `AbstractUnitTest` shown in [code-organization.md](./code-organization.md).

## File headers and documentation comments

- **Header.** 55.8 % of the main files carry a licence or copyright notice (522 AGPL banner, 42 copyright only); 166 still hold the
  NetBeans "To change this license header" placeholder and 280 have no header; there is no SPDX identifier. The dominant form
  (about 400 files) is:

```java
//******************************************************************************
//                          <File>.java          (optional)
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: <emails>
//******************************************************************************
```

  `[P]` by [CONTRIBUTING.md](../../../../../../CONTRIBUTING.md); use the same form for new files.
- **Language.** Comments are in English (about 3,600 lines against 6 in French) `[C]`.
- **Javadoc tags.** `@author` appears in 73 % of the files, with mixed identities (the same person appears under four names or
  logins); `@since` and `@version` are never used. Deprecation uses the `@Deprecated` annotation (70) more than the Javadoc
  `@deprecated` tag (36); use both.

## Branches, commits and merge requests

These are the only naming rules enforced by a hard gate. The job `merge-request:check` of `.gitlab-ci.yml` fails a merge
request that does not respect them:

| Rule                                                                                                                                   | Where                      |
|----------------------------------------------------------------------------------------------------------------------------------------|----------------------------|
| A merge request into `develop` or `release` must come from a branch whose name starts with `feature/`                                    | `.gitlab-ci.yml`           |
| A merge request into `master` must come from a branch whose name starts with `hotfix/`                                                   | `.gitlab-ci.yml`           |
| The merge request title follows Conventional Commits: `build`, `chore`, `ci`, `docs`, `feat`, `fix`, `perf`, `refactor`, `revert`, `style` or `test`, an optional scope in brackets, an optional `!`, then a colon and a description; `Draft:` is accepted before it | `.gitlab-ci.yml` |
| A changelog entry is required (or an explicit "ignore changelog" mark)                                                                    | `.gitlab-ci.yml`           |

So a branch called `feat/<something>` cannot be merged into `develop`: name it `feature/<something>`.

## New concept checklist

For a concept `Foo` in a native module, the names that follow from the rules above:

| File                                  | Package                         |
|---------------------------------------|---------------------------------|
| `FooAPI` (constants `PATH`, `CREDENTIAL_FOO_*`) | `<module>.foo.api`    |
| `FooGetDTO`, `FooCreationDTO`, `FooUpdateDTO`   | `<module>.foo.api`    |
| `FooModel` (constant `GRAPH`)         | `<module>.foo.dal`              |
| `FooDAO`, `FooSearchFilter`           | `<module>.foo.dal`              |
| `FooLogic` (only if there are business rules) | `<module>.foo.bll`      |
| `FooAPITest`, `FooDAOTest`            | the same packages, in `src/test/java` |

## Known deviations

The most visible departures from the rules above, for people who want to fix them (paths relative to the module):

| Where                                                                                       | What is off                                                                            |
|---------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------|
| `security/.../user/api/UserCreationWithExistantPersonDTO.java`                              | Empty file, and "Existant" is misspelled                                                |
| `core/.../provenance/api/JsonSchemaValidation.java`, `core/src/test/.../metrics/dal/MetricsDAOTest.java` | Entirely commented out, including the `package` line                          |
| `main/.../utils/functionnal/`, `main/.../UriFormater.java`                                  | Misspelled `functionnal` (package) and `Formater`                                       |
| `core/.../uriSearch/api/UriSearchApi.java`, `germplasmGroup/api/GermplasmGroupApi.java`     | `Api` instead of `API`                                                                  |
| `core/.../data/dal/DataDaoV2.java`, `DataFileDaoV2`, `ProvenanceDaoV2`, `nosql/.../MetaDataDaoV2` | `Dao` plus `V2`, next to `DataDAO`; `MetaData*` and `Metadata*` both exist       |
| `core/.../organisation/**`                                                                  | British `organisation` package, but 11 types named `Organization*`                      |
| `security/.../user/api/`                                                                    | The concept was renamed to account only in part (`UserAPI` with `AccountDAO`)           |
| `core/.../variable/api/VariableExportDTOClassic.java`, `VariableExportDTODetails.java`      | `DTO` in the middle of the name                                                         |
| `front/.../config/AbstractMenuItem.java`                                                    | An interface with an `Abstract` prefix                                                  |
| `main/.../OpenSilexModule` against `OpensilexCommandException`, `OpensilexModuleUpdateException` | Brand casing 12 against 2                                                        |
| `sparql/src/test/.../model/TEST_ONTOLOGY.java`                                               | A type named in UPPER_SNAKE                                                             |
| `core/.../organisation/api/site/SiteAPI.java`, `core/.../ontology/api/OntologyAPI.java`      | Path `core/sites` without a leading slash; `/ontology` without the `/core` prefix        |
| `core/.../data/utils/MathematicalOperator.java`, `brapi/.../BrAPIv1ObservationUnitDTO.java`  | PascalCase enum constants and the typo `EqualToo`; the typo `PLANTED_INDIVIDUAl`          |
| `RDFObjectDTO` against `SPARQLResourceModel` and `MongoModel`                                | `lastUpdatedDate` against `lastUpdateDate`                                               |
| `AccountModel`, `PersonModel`, `ScientificObjectModel`, `LocationObservationCollectionModel` | `AccountModel` and `PersonModel` share the graph `"user"`; `ScientificObjectModel` uses `"scientific-object"` and `LocationObservationCollectionModel` `"ObservationCollection"`, against lower-case singular names elsewhere |

## Limitations and improvements

- The survey tools were regular expressions at fixed indentation, so nested types are not counted and multi-line declarations
  are only partly covered.
- The SonarQube quality profile is configured on the server and could not be read; it may enforce rules that this page does
  not list.
- If the project wants enforceable rules, the cheapest step is a custom Checkstyle `format` for `TypeName`, `PackageName`
  and `ConstantName`, run in the merge request build instead of only on `develop`.
- The suffix vocabulary of DTOs (`Detail` against `Details`, plain `DTO`) is the loosest area and worth a decision.

## Documentation

- [code-organization.md](./code-organization.md) : the prescribed layout of a concept and of a module.
- [modules-overview.md](./modules-overview.md) : the modules and how they are loaded.
- [tests.md](./tests.md) : the test base classes.
- [CONTRIBUTING.md](../../../../../../CONTRIBUTING.md) : contribution rules.
- [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html) : the style guide `CONTRIBUTING.md` points to.
