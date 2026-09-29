# Technical documentation : [`architecture`] `opensilex-phis` module

**Document history (please add a line when you edit the document)**

| Date       | Editor(s)        | OpenSILEX version | Comment           |
|------------|------------------|-------------------|-------------------|
| 2026-09-29 | Arnaud Charleroy | BUILD-SNAPSHOT    | Document creation |

> ⚠️ _WARNING_ : written from the `develop` branch (commit `6725c2912`) by reading the code; nothing was built or run.
> The module's `pom.xml` and [../architecture/README.md](../architecture/README.md) describe it as "legacy PHIS web
> services". There are none: this page records what the module really contains.

## Table of contents

<!-- TOC -->
* [Technical documentation : [`architecture`] `opensilex-phis` module](#technical-documentation--architecture-opensilex-phis-module)
  * [Table of contents](#table-of-contents)
  * [Definitions](#definitions)
  * [Solution](#solution)
    * [Module identity](#module-identity)
    * [What the module contains](#what-the-module-contains)
  * [Tests](#tests)
  * [Limitations and improvements](#limitations-and-improvements)
  * [Documentation](#documentation)
<!-- TOC -->

## Definitions

- **`oeso-phis`** : an OWL file that extends the OpenSILEX vocabulary with phenotyping concepts, mostly `oeev` event types.
- **Species seed** : `species.ttl`, an AGROVOC-based list of species loaded at install time.

## Solution

### Module identity

- Module class: [PhisWsModule](../../../../../../opensilex-phis/src/main/java/org/opensilex/phis/PhisWsModule.java) extends
  `OpenSilexModule` and implements `APIExtension` and `SPARQLExtension`. There is no configuration class (no config id).
- Two Java files in total: `PhisWsModule` and `ontology/OesoPhis` (vocabulary constants).
- The parent is `opensilex-module`, and the `pom.xml` declares `opensilex-core`. It sets `skipFrontTypesGeneration` to `true`
  and hard-codes `<revision>BUILD-SNAPSHOT</revision>`.
- It is **not** in the built-in load order (see the [modules overview](../architecture/modules-overview.md#runtime-load-order)),
  so it is loaded after `opensilex-front`.

### What the module contains

- **Ontology.** `getOntologiesFiles()` registers `ontologies/oeso-phis.owl` (namespace `OesoPhis.NS`, prefix `oeso`, flagged
  for the Staple API). The file declares about 124 `owl:Class`, mostly events.
- **Species seed.** `install()` loads `ontologies/species.ttl` into the germplasm graph.
- **Front end.** Two layout components, `PhisLoginComponent` and `PhisHeaderComponent`, and a complete theme
  (`front/theme/phis/phis.yml` with its SCSS, images and a user guide PDF). They are activated by configuration, not
  automatically:

```yaml
front:
  theme: opensilex-phis#phis
  loginComponent: opensilex-phis-PhisLoginComponent
  headerComponent: opensilex-phis-PhisHeaderComponent
```

`OpenSilexVuePlugin` resolves the component id `opensilex-phis-PhisLoginComponent` to module `opensilex-phis` by splitting at
the last `-` (see [opensilex-front](../opensilex-front/module-architecture.md#registering-a-module-in-the-vue-application)).

## Tests

- `GermplasmAPITest` (12 tests): a reduced copy of the `opensilex-core` test of the same name, built on core's
  `BaseGermplasmAPITest`.
- `OntologyStoreCoreTest`: entirely commented out, no live test.
- `InstallTest` in `opensilex-dev-tools` asserts that the graph of `OesoPhis.NS` exists after an install.

## Limitations and improvements

- The module name and the `pom.xml` header comment promise "web services" that do not exist.
- `GermplasmAPITest` exists in `core`, `phis` and `faidare` under the same simple name.
- The class is named `PhisWsModule` ("WS" for web services) although it contains none.

## Documentation

- [../architecture/modules-overview.md](../architecture/modules-overview.md) : where the module sits and its load order.
- [../opensilex-front/module-architecture.md](../opensilex-front/module-architecture.md) : themes and layout components.
- [../architecture/java-naming-conventions.md](../architecture/java-naming-conventions.md) : naming rules.
