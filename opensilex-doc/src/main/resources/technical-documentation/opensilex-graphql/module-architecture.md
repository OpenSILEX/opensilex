# Technical documentation : [`architecture`] `opensilex-graphql` module

**Document history (please add a line when you edit the document)**

| Date       | Editor(s)        | OpenSILEX version | Comment           |
|------------|------------------|-------------------|-------------------|
| 2026-09-29 | Arnaud Charleroy | BUILD-SNAPSHOT    | Document creation |

> ⚠️ _WARNING_ : written from the `develop` branch (commit `6725c2912`) by reading the code; nothing was built or run.
> The Staple API is described from the user's side in [staple-api.md](./staple-api.md); this page covers the module.

## Table of contents

<!-- TOC -->
* [Technical documentation : [`architecture`] `opensilex-graphql` module](#technical-documentation--architecture-opensilex-graphql-module)
  * [Table of contents](#table-of-contents)
  * [Definitions](#definitions)
  * [Solution](#solution)
    * [Module identity](#module-identity)
    * [Endpoints](#endpoints)
    * [How the Staple model is built](#how-the-staple-model-is-built)
  * [Tests](#tests)
  * [Limitations and improvements](#limitations-and-improvements)
  * [Documentation](#documentation)
<!-- TOC -->

## Definitions

- **Staple API** : the export of the platform's ontology and of the mapping from RDF types to graphs, used to build a GraphQL
  schema over the triple store.
- **Staple ontology** : an ontology file registered with `addToStaple = true` in a `SPARQLExtension`.

## Solution

### Module identity

- Module class: [GraphQLModule](../../../../../../opensilex-graphql/src/main/java/org/opensilex/graphql/GraphQLModule.java)
  extends `OpenSilexModule` and implements `APIExtension`. It has no configuration class, no servlet and no `install` or
  `startup`. Its only code is `bindServices`, which binds `StapleApiUtils` as a request-scoped contract.
- Four Java files (about 600 lines): the module class, `StapleAPI`, `StapleApiUtils`, `StapleModelBuilder`. No resources, no tests.
- The `pom.xml` declares only `opensilex-sparql`, but the parent is `opensilex-module`, which adds core, security, front, nosql
  and fs; the declaration is redundant.

### Endpoints

`StapleAPI` is mounted at `/staple`:

| Endpoint                   | Returns                                                                    |
|----------------------------|----------------------------------------------------------------------------|
| `GET /staple/ontology_file` | The Staple ontology as Turtle                                              |
| `GET /staple/resource_graph`| A JSON object mapping each `rdf:type` to its default graph                  |

Neither method is `@ApiProtected`, so both are public.

### How the Staple model is built

`StapleApiUtils` (a service with field injection of `OpenSilex` and `SPARQLService`):

- `getStapleModel()` collects every `OntologyFileDefinition` flagged `addToStaple = true` by any `SPARQLExtension`, takes the
  `owl:Class` URIs they define as roots, and hands them with the ontology store to `StapleModelBuilder`.
- `getResourceGraphMap()` reads the class mapper index of `opensilex-sparql`.
- The mapping of `Time.Instant` to the graph of `EventModel` is hard-coded, which makes this module import a `core` class.

`StapleModelBuilder` builds a Jena `Model` from the ontology store using schema.org `domainIncludes` and `rangeIncludes`,
keeps four XSD datatypes only, excludes PROV `Person` and `Entity`, and resolves multi-range properties through a common
ancestor.

Files registered with `addToStaple = true` are `oeso-core.owl` (by `CoreModule`) and `oeso-phis.owl` (by `PhisWsModule`).

## Tests

None.

## Limitations and improvements

- [staple-api.md](./staple-api.md) says the default Staple ontologies are `oeso-core`, `oeev` and `os-sec`; in code only
  the files flagged `addToStaple` count, which are `oeso-core` and `oeso-phis` (the `oeev` and `os-sec` classes are defined
  inside `oeso-core.owl`).
- [../../how-to/dependency-injection.md](../../how-to/dependency-injection.md) shows `StapleApiUtils` with constructor
  injection and an unscoped binding; the code uses field injection and a request scope.
- The module declares a dependency on `opensilex-sparql` only, yet imports a `core` class (`EventModel`); it works through
  the dependencies inherited from `opensilex-module`.

## Documentation

- [staple-api.md](./staple-api.md) : the Staple API from the client's side.
- [../architecture/modules-overview.md](../architecture/modules-overview.md) : where the module sits.
- [../architecture/java-naming-conventions.md](../architecture/java-naming-conventions.md) : naming rules.
