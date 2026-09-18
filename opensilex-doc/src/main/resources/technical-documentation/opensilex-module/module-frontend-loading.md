# Technical documentation : [module front-end] How OpenSILEX load front-end modules.

**Document history (please add a line when you edit the document)**

| Date       | Editor(s)               | OpenSILEX version   | Comment           |
|------------|-------------------------|---------------------|-------------------|
| 17/09/2026 | yvan.roux@opensilex.fr  | 1.5.0 Freaky Fossil | Document creation |

## Table of contents

<!-- TOC -->
* [Technical documentation : [module front-end] How OpenSILEX load front-end modules.](#technical-documentation--module-front-end-how-opensilex-load-front-end-modules)
  * [Table of contents](#table-of-contents)
  * [Context and links to other documents](#context-and-links-to-other-documents)
  * [Building the front-end](#building-the-front-end)
    * [general workflow](#general-workflow)
    * [development startup and hot reload](#development-startup-and-hot-reload)
  * [Modules are Vue plugins](#modules-are-vue-plugins)
  * [Loading the components of a module](#loading-the-components-of-a-module)
<!-- TOC -->

## Context and links to other documents

This document only describes the technical part of the front-end module loading.

Further steps could be to :
- create a new module from zero, see [modules.md](modules.md)
- extending the front-end by adding new pages, see [module-front-end-extension.md](module-api-and-interface-extension.md)
- modifying the front-end style by overloading the default theme, see [module-theme-personalization.md](module-theme-personalization.md)
- modifying some frontend pages by overriding default components, see [overriding-defaults-components.md](overriding-defaults-components.md)

## Building the front-end

### general workflow

Below is the simplified directory structure of the front-end of an OpenSILEX module.
```bash
# module_name  => .e.g : inrae-sixtine
{module_name}
├── front
│   ├── dist 
│   ├── src # javascript sources
│   │   └── (new components, pages, layout or typescript files)
```

When running `mvn clean install`, Maven builds the front-end using the `frontend-maven-plugin` which runs
`npm run build` (which in turn invokes Vite). The TypeScript/JavaScript sources in the `src` directory are
transpiled and bundled into a single JavaScript file named `{module_name}.umd.min.js`, placed in the `dist` directory.

Maven then includes this build file in the final JAR at `target/classes/front` via the `maven-resources-plugin`
(copy from `front/dist` to `target/classes/front`).

The build is activated by the Maven profile `with-vue-app`, which is auto-activated when a `front/package.json`
file exists in the module.

When launching the front-end, the `OpenSilexVuePlugin.ts` modify the main build (opensilex-front/front) to add into it
a `<script>` balise for each module to load.

Each script balise has a src attribute that calls the `/vuejs/extension/js/{module}.js` API endpoint and so calls the
`FrontAPI#getExtension` method which returns the module.umd.min.js file.

for exemple, the phis module will generate the following script balise on a local instance:
`<script src="http://localhost:8666/rest/vuejs/extension/js/opensilex-phis.js"></script>`

### development startup and hot reload

When running the front-end in development mode, the `StartServerWithFront.java` class builds each front-end module
using `npm run dev:build` (which runs `vite build --watch`). This produces the bundle file `{module_name}.umd.min.js`
in the module's `front/dist/` directory.

A `FileAlterationMonitor` watches the `dist/` directory. when a front-end module is modified, the bundle file (the build)
is modified thanks to Vite's native HMR (Hot Module Replacement). When the bundle file is created or modified, it is copied
to `{module}/target/classes/front/` by the `StartServerWithFront.java` class that will trigger the hot reload of the main front-end process.

To trigger hot reload of the main front-end, `StartServerWithFront.java` writes a timestamp to
`opensilex-front/front/src/opensilex.dev.ts`. This file is imported in `main.ts` so that
any change to it triggers Vite's native HMR (Hot Module Replacement) in the main app.

The way the main front-end process includes the front-end modules is the same as the general workflow described above.

## Modules are Vue plugins

A front-end module is not just a bundle of files: it is built as a **Vue plugin**. The file used as the Vite
library entry point (`front/src/index.ts`, declared under `build.lib.entry` in the module's `vite.config.ts`)
default-exports that plugin, and the UMD bundle exposes it as a global variable named after the module, for
example `window["opensilex-phis"]`.

That export is described by the `OpensilexModulePlugin` type, declared in
`opensilex-front/front/src/models/OpensilexModulePlugin.ts`. It is Vue's own `Plugin` type extended with two
optional properties, `components` and `lang`. Being a Vue plugin means the export must be either an install
function or an object exposing an `install()` method, which is what Vue requires to register it.

Here is an exemple of how the phis module could export its plugin:

```typescript
export default {
    install(Vue, options) {
    },
    components : {
        "opensilex-phis-PhisLoginComponent" : PhisLoginComponent,
        "opensilex-phis-PhisHeaderComponent" : PhisHeaderComponent
    }
} as OpensilexModulePlugin;
```

Once the `<script>` tag described above has been evaluated, the `loadModule` method of `OpenSilexVuePlugin.ts`
reads the global variable and unwraps its `default` export.

Then, `loadModule` hands the plugin over to Vue with `app.use(plugin)`, which runs the module's
`install()` method. This is where a module can enrich the application on its own, for instance, by binding
services into the dependency injection container.

Note that `install()` can be empty, as it is for phis: a module that only ships components does not need it,
because components are registered separately, as described in the next section.

## Loading the components of a module

A module that provides Vue components declares them in the `components` property of the plugin it exports (the
`components` map of the phis example above). The map associates a component identifier with the imported Vue
component. By convention, the identifier is prefixed with the module name, which keeps it unique across the whole
application: `opensilex-phis-PhisLoginComponent`.

Right after `app.use(plugin)`, the `loadModule` method of `OpenSilexVuePlugin.ts` goes through that map twice:

- it calls `loadComponentTranslations` on each component, so that the translations defined in the component's
  `<i18n>` block are merged into the locale messages of the application;
- it passes the whole map to `initAsyncComponents`, which registers every entry with Vue's `app.component()`.

Because `app.component()` registers them globally, the components of a module become available in any template
of the application, referenced by their identifier only. 

For a better understanding of why and how to override default components with custom module ones, see
[overriding-defaults-components.md](overriding-defaults-components.md).