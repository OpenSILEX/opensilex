# Technical documentation : [module front-end] How to override default components, usage and technical workflow

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
* [Technical documentation : [module front-end] How to override default components, usage and technical workflow](#technical-documentation--module-front-end-how-to-override-default-components-usage-and-technical-workflow)
  * [Table of contents](#table-of-contents)
  * [Component override mechanism (FrontConfig)](#component-override-mechanism-frontconfig)
  * [Module export format](#module-export-format)
  * [Component rendering in App.vue](#component-rendering-in-appvue)
<!-- TOC -->

## What is the component override mechanism ?

The component override mechanism allows replacing the default components of the front-end with custom ones.
This is useful for instances that want to personalize some pages of the application.

For exemple, Phis uses a custom login component to add its own logo and images.

You can find the list of overridable components in the Java interface `FrontConfig.java` file at `opensilex-front/src/main/java/org/opensilex/front/FrontConfig.java`

## How to override components ?

The components to load are defined in the YAML configuration file under the `front:` block.
Each component has a default value defined in the Java interface `FrontConfig.java`, which can be overridden per instance.

Example of a configuration using the Phis login and header components:
```yaml
front:
    theme: opensilex-phis#phis
    loginComponent: opensilex-phis-PhisLoginComponent
    headerComponent: opensilex-phis-PhisHeaderComponent
```
> Note : the theme key of this exemple is used to load the CSS theme from phis module. This part is better explained in the [module-theme-personalization.md](module-theme-personalization.md)

## Technical workflow

The `GET /rest/vuejs/config` endpoint exposes the configuration's information via a `FrontConfigDTO` object.

The frontend then parses each component identifier using `ModuleComponentDefinition.fromString()`, which splits the
string into `{module}` and `{componentName}`. For example:
- `"opensilex-phis-PhisLoginComponent"` → module=`"opensilex-phis"`, component=`"PhisLoginComponent"`

The module name determines which module's JavaScript bundle will be loaded (via `/vuejs/extension/js/{module}.js`),
and the full `{module}-{ComponentName}` identifier is used as the Vue component name.

### Module export format

Each front-end module exports a Vue plugin in its `front/src/index.ts` file. The plugin must expose an `install()`
method and a `components` map that registers Vue components:

```typescript
export default {
    install(Vue, options) {
    },
    components : {
        "opensilex-phis-PhisLoginComponent" : PhisLoginComponent,
        "opensilex-phis-PhisHeaderComponent" : PhisHeaderComponent,
    }
};
```

When the main app loads the module via `<script src="/vuejs/extension/js/opensilex-phis.js">`, Vite wraps the module
in a UMD bundle. The global variable `window.opensilex-phis` becomes the plugin object.

`OpenSilexVuePlugin.loadModule()` calls `this.app.use(plugin)` to install the plugin, which registers all components
defined in the `components` map into the Vue application.

### Component rendering in App.vue

The main `App.vue` component renders the configured components dynamically using Vue's `<component :is="...">` directive:

```vue
<component :is="loginComponent"></component>
<component :is="headerComponent" ...></component>
<component :is="menuComponent"></component>
<component :is="footerComponent"></component>
```

The component names are read from the config via `opensilex.getConfig().loginComponent`, etc.