# Technical documentation : [module front-end] Why and how to extend and modify OpenSILEX's front-end

**Document history (please add a line when you edit the document)**

| Date       | Editor(s)               | OpenSILEX version   | Comment           |
|------------|-------------------------|---------------------|-------------------|
| 17/09/2026 | yvan.roux@opensilex.fr  | 1.5.0 Freaky Fossil | Document creation |

> ⚠️ _WARNING_ : This document is about personalizing OpenSILEX's front-end by creating a new module.
>
> You first need to follow the first steps of creating a new module: [modules.md](modules.md)
>

## Table of contents

<!-- TOC -->
* [Technical documentation : [module front-end] Why and how to extend and modify OpenSILEX's front-end](#technical-documentation--module-front-end-why-and-how-to-extend-and-modify-opensilexs-front-end)
  * [Table of contents](#table-of-contents)
  * [Context and links to other documents](#context-and-links-to-other-documents)
  * [Creating the front-end part of a new module](#creating-the-front-end-part-of-a-new-module)
    * [Vite config and dependencies](#vite-config-and-dependencies)
    * [Create your first components](#create-your-first-components)
    * [index.ts : export your module as a plugin](#indexts--export-your-module-as-a-plugin)
<!-- TOC -->

## Context and links to other documents

This document explains how to create the front-end part of a new module.

Creating a front-end for your module could be useful for:
- extending the front-end by adding new pages. See [module-api-and-interface-extension.md](module-api-and-interface-extension.md)
- modifying the front-end by overloading the default theme. See [module-theme-personalization.md](module-theme-personalization.md)
- overriding default components to replace them. See [overriding-defaults-components.md](overriding-defaults-components.md)

## Creating the front-end part of a new module

After following the steps in [modules.md](modules.md), you should have a new module with at least the following structure:

```bash
# module_name  => .e.g : inrae-sixtine
{module_name} # module
├── front # front
├── pom.xml  # module pom file
├── src # back end java sources
│   └── ...
```
We will work only in the front folder of the module.

### Vite config and dependencies

The front-end of a module is built by Vite as a library. The simplest way to start is to copy the
`vite.config.ts` of the phis module and adapt it. Three things matter:

- `build.lib.entry` must point to `src/index.ts`, the file that exports your module as a plugin (described in a section below);
- `build.lib.name` must be the module name, for example `opensilex-phis`, because it becomes the name of the
  global variable through which the main application picks up your module;
- `build.lib.fileName` must produce `{module_name}.{format}.min.js`, with `formats: ['umd']` and
  `minify: true`. The back-end serves the UMD file under that exact name, so a different naming scheme simply
  results in a module that is never found.

Your module must **not** bundle its own copy of Vue and vue-i18n: it has to reuse the instances of the main
application. This is what the `rollupOptions` do:

```typescript
rollupOptions: {
  external: ['vue', 'vue-i18n'],
  output: {
    globals: {
      vue: 'Vue',
      'vue-i18n': 'VueI18n',
    },
  },
},
```

`opensilex-front/front/src/main.ts` exposes its own runtime as `window.Vue` and `window.VueI18n`, and the
`globals` mapping tells rollup to read the external dependencies from there. Declaring `vue-i18n` this way is
what gives your components access to the translation keys already loaded by opensilex-front. Beware that
omitting a mapping is not harmless: rollup then falls back to a guessed global name that does not exist, and
the UMD bundle throws while being evaluated, before your module can be registered.

On the dependency side, the `package.json` of the module mainly needs its `name`, and the `build` and
`dev:build` scripts that run Vite, as they are the ones called by Maven and by `StartServerWithFront.java`.
Keep the libraries provided by the main application (Vue, vue-i18n) out of `dependencies`, since they are
external at runtime.

### Create your first components

By convention, components live in the `front/src/components` directory of the module. They are regular Vue 3
single-file components, with no specific constraint.

You can depend on `opensilex-front`, `opensilex-core` and `opensilex-security` without any problem: import
what you need with a relative path and Vite will compile it into your bundle.

The `opensilexVuePlugin` injection is provided by `opensilex-front/front/src/main.ts` and gives your component access
to the whole application: API services, configuration, current user, and so on.
```typescript
const opensilex = inject<OpenSilexVuePlugin>("$opensilex");
```

For translations, use `useI18n()` from vue-i18n. Called without arguments it works in the global scope, which
means every key already loaded by opensilex-front is available to your component. Those keys come from the
message files `opensilex-front/front/src/lang/message-en.json` and `message-fr.json`. You will also be able to use 
traduction keys defined in components of other modules. 

To define your own keys, ship your own message files in the module and declare them in the `lang` property of
the exported plugin. The dataverse module does exactly this:

```typescript
import fr from "./lang/dataverse-fr.json";
import en from "./lang/dataverse-en.json";

export default {
  install(app, options) {
  },
  lang: {
    fr: fr,
    en: en,
  }
} as OpensilexModulePlugin;
```

When the module is loaded, its `lang` property is merged into the messages of the application, so your own
keys end up next to the ones of opensilex-front and are used in exactly the same way.

### index.ts : export your module as a plugin

Finally, `front/src/index.ts` must default-export your module as a Vue plugin. It declares an `install()`
method, which can be left empty, and the `components` map listing the components the module makes available
to the application. Here is the phis module:

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

Prefixing each component identifier with the module name keeps it unique across the whole application, and is
what lets an instance select your components from its YAML configuration.

For the details of how this plugin is then loaded and how its components are registered, see
[module-frontend-loading.md](module-frontend-loading.md).
