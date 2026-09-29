# Technical documentation : [`architecture`] `opensilex-front` module

**Document history (please add a line when you edit the document)**

| Date       | Editor(s)        | OpenSILEX version | Comment           |
|------------|------------------|-------------------|-------------------|
| 2026-09-29 | Arnaud Charleroy | BUILD-SNAPSHOT    | Document creation |

> ⚠️ _WARNING_ : written from the `develop` branch (commit `6725c2912`) by reading the code; nothing was built or run.
> On `develop` the front end is a **Vue 2** application built with vue-cli. The `vue3/*` branches migrate it to Vue 3 with
> Vite; they are not described here. The Vue components themselves are covered by
> [components.md](./components.md) and the other documents of this folder; this page describes the Java module and the
> way the application is served and extended.

## Table of contents

<!-- TOC -->
* [Technical documentation : [`architecture`] `opensilex-front` module](#technical-documentation--architecture-opensilex-front-module)
  * [Table of contents](#table-of-contents)
  * [Definitions](#definitions)
  * [Solution](#solution)
    * [Module identity](#module-identity)
    * [Package map](#package-map)
    * [How the application is served](#how-the-application-is-served)
    * [Extension mechanisms](#extension-mechanisms)
    * [Front API](#front-api)
    * [Ontology-driven forms](#ontology-driven-forms)
    * [Build integration](#build-integration)
    * [Layout of the front directory](#layout-of-the-front-directory)
    * [Registering a module in the Vue application](#registering-a-module-in-the-vue-application)
    * [The generated TypeScript client](#the-generated-typescript-client)
  * [Tests](#tests)
  * [Limitations and improvements](#limitations-and-improvements)
  * [Documentation](#documentation)
<!-- TOC -->

## Definitions

- **Front module** : the Vue side of an OpenSILEX module, the `front/` directory of its Maven module.
- **Theme** : a set of stylesheets, fonts and images selected with `front.theme`.
- **Owl extension** : the per-class and per-property display settings (icon, component, order) stored with the ontology.

## Solution

### Module identity

- Module class: [FrontModule](../../../../../../opensilex-front/src/main/java/org/opensilex/front/FrontModule.java),
  config id `front`. Implements `ServerExtension` and `APIExtension`; it does **not** implement `SPARQLExtension`.
- Maven dependencies: `opensilex-main`, `opensilex-security`, `opensilex-core` (plus jsass and yuicompressor to compile
  the SCSS themes).
- `FrontConfig` keys: `loginComponent`, `homeComponent`, `notFoundComponent`, `headerComponent`, `menuComponent`,
  `footerComponent`, `theme` (default `opensilex-front#opensilex`), `menuExclusions`, `customMenu`, `geocodingService`,
  `versionLabel`, `applicationName`, `connectAsGuest`, `dashboard`, `matomo`, the `notification*` keys and `agroportal`.

### Package map

Root package `org.opensilex.front`; 58 Java files in `src/main/java`.

| Package                                | Files | Role                                                                              |
|----------------------------------------|-------|-----------------------------------------------------------------------------------|
| `front` (root)                         | 6     | `FrontModule`, `FrontConfig` and helpers                                           |
| `front/api`                            | 11    | `FrontAPI` (`/vuejs`) and its DTOs                                                 |
| `front/config`                         | 9     | Routing, menu and configuration classes read from `opensilex.front.yml`            |
| `front/theme`                          | 4     | `ThemeBuilder` and theme configuration                                             |
| `front/vueOwlExtension`                | 1 + 6 api + 3 dal | `VueOwlExtensionAPI` (`/vuejs/owl_extension`) and the persisted extension models |
| `front/vueOwlExtension/types`          | 3     | `VueOntologyType` and its data/object specialisations                              |
| `front/vueOwlExtension/types/data`     | 8     | One class per literal data type (`VueString`, `VueDate`, ...)                       |
| `front/vueOwlExtension/types/object`   | 7     | One class per object type (`VueUser`, ...)                                          |

### How the application is served

`FrontModule.initServer` (the `ServerExtension` hook) calls `Server.initApp(pathPrefix + "/app", "/", "/front",
FrontModule.class)`: the `/front` resource root of the jar (or the folder, in development mode) is mounted as a Tomcat
context. A `FrontRewriteValve` then rewrites `/osfront/...` to `/...` and sends any path that is not a file to
`/index.html`, so the single-page application handles its own routes. The `publicPath` of the build is `./osfront` in
production and `/app` in development.

### Extension mechanisms

- **Routes and menu.** A module may ship `front/opensilex.front.yml` with `menu` and `routes`. `FrontModule.getConfigDTO` reads
  it from every module; only `opensilex-front` ships one.
- **Themes.** `front/theme/<id>/<id>.yml` declares `extend`, `favicon`, `stylesheets`, `excludes`, `fonts` and an icon map;
  `ThemeBuilder` compiles the SCSS to CSS. A theme is selected with `front.theme: <module-id>#<theme-id>`. The layout
  is documented in [code-organization.md](../architecture/code-organization.md).
- **Layout components.** The login, header, menu, footer, home and not-found components can each be replaced through the
  matching `*Component` configuration key. `opensilex-phis` does this for the login and header.
- **Vue components of a module** are registered by the module's `front/src/index.ts` (see below).

### Front API

`FrontAPI` is mounted at `/vuejs` and none of its endpoints is protected:

| Endpoint                                   | Returns                                                            |
|--------------------------------------------|--------------------------------------------------------------------|
| `GET /config`, `GET /user_config`          | The front configuration, and the user-dependent part of it          |
| `GET /extension/js/{module}.js`            | The module's `front/{module}.umd.min.js`, with an ETag              |
| `GET /extension/css/{module}.css`          | The module's stylesheet                                             |
| `GET /theme/{module}/{theme}/config`, `/style.css`, `/resource` | The theme configuration, compiled CSS and resources |

### Ontology-driven forms

`VueOwlExtensionAPI` (`/vuejs/owl_extension`) reads and writes the display settings of ontology classes and properties:
`GET rdf_type` requires authentication, and creating, updating or deleting an extension requires an administrator.

`VueOntologyType` is a `ServiceLoader` service (registered by `opensilex-module`), not an `OpenSilexExtension`. Each
implementation maps an RDF datatype or object type to an input component and a view component, with optional aliases
(`getTypeUri`, `getInputComponent`, `getViewComponent`, `getTypeUriAliases`, `isDisabled`); `VueOntologyDataType` adds a label
key. `VueOwlExtensionDAO.buildTypeLists` loads them. The models `VueClassExtensionModel` and `VueClassPropertyExtensionModel`
persist the per-class and per-property settings in the `oeso-owl` vocabulary, which is defined in the `oeso-core.owl` file
shipped by `opensilex-core`.

### Build integration

- The profile `with-vue-app` (activated by the presence of `front/package.json`) runs the frontend-maven-plugin: Yarn
  install, `run build:types`, `run build`, and outdated and audit checks. The `dist` output is copied to
  `target/classes/front`. Node is `v14.19.0` and Yarn `v1.22.10` (properties in `opensilex-parent/pom.xml`).
- The profile `with-vue-config` copies `front/opensilex.front.yml`; the theme folder is copied by a resources execution.
- The pom overrides `<resources>` to `front`, with everything excluded from the default resource handling.

### Layout of the front directory

`opensilex-front/front` contains `src/`, `theme/opensilex`, `public/`, `opensilex.front.yml`, `vue.config.js`,
`tsconfig.json`, `package.json` and `yarn.lock`. Under `src/`:

| Path                | Content                                                                                       |
|---------------------|-----------------------------------------------------------------------------------------------|
| `main.ts`, `App.vue`| Bootstraps the plugins, the validation rules and `i18n` (`en`, `fr`), then loads `opensilex-security` and `opensilex-core` |
| `components/`       | Vue components, about 37 folders by concept, and an `index.ts`                                 |
| `models/`           | `OpenSilexVuePlugin.ts`, `Store.ts` and other shared classes                                   |
| `services/`, `ontologies/`, `styles/` | Shared services, ontology helpers, styles                                     |
| `lang/`             | `message-en.json`, `message-fr.json`                                                           |
| `lib/`, `types/`    | Generated (ignored by git): the TypeScript API client and its typings                          |

### Registering a module in the Vue application

Each module's `front/src/index.ts` default-exports `install(Vue, options)`, an optional `components` map whose keys are
`<module-id>-<ComponentName>`, and an optional `lang` map. `opensilex-phis` registers components; `opensilex-security` and
`opensilex-brapi` only bind the API client. `OpenSilexVuePlugin.loadModule` injects the module's script and stylesheet
from the `/vuejs/extension` endpoints, calls `Vue.use`, merges the translations and registers the components. A component
id is split at its **last** `-`, so `opensilex-phis-PhisLoginComponent` resolves to module `opensilex-phis`.

### The generated TypeScript client

At `compile`, `SwaggerAPIGenerator` writes `front/src/lib/swagger.json`, then the swagger-codegen plugin generates
`typescript-inversify` sources into `front/src/lib`, using the templates of `opensilex-main`. `yarn run build:types`
writes the typings to `front/types/<module>.d.ts`. Both directories are git-ignored.

## Tests

`opensilex-front/src/test` holds `OntologyAPITest` (593 lines), which tests the **core** `OntologyAPI`, not
`VueOwlExtensionAPI`, and `FrontTest`, an abstract class with a placeholder test that never runs. The Vue code has no unit
tests in this module.

## Limitations and improvements

- `FrontModule.getConfigDTO` compares `URI` values with `!=`, so it always recomputes, and it keeps per-user values (language,
  OpenID title) in fields shared by all requests.
- `UserConfigService` contains `if (false && ...)`, which disables its menu cache.
- `VueUser.getInputComponent()` returns null, with a TODO.
- `vue.config.js` and `tsconfig.json` alias `opensilex-security`, `opensilex-core` and `opensilex-phis`, but not
  `opensilex-brapi` or `opensilex-faidare`.
- `src/main/webapp/META-INF/context.xml` (context `/opensilex-angular`) and `src/main/resources/WEB-INF/web.xml` are legacy
  files; the second is excluded by the pom.

## Documentation

- [components.md](./components.md), [form_selector.md](./form_selector.md), [paginations.md](./paginations.md),
  [experiments.md](./experiments.md), [component-guidelines-template.md](./component-guidelines-template.md) : Vue-side
  documents.
- [../architecture/code-organization.md](../architecture/code-organization.md) : theme and front module layout.
- [../architecture/modules-overview.md](../architecture/modules-overview.md), [../architecture/java-naming-conventions.md](../architecture/java-naming-conventions.md).
