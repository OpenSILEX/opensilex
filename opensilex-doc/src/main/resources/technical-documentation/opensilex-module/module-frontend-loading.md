# Technical documentation : [module front-end] How OpenSILEX load front-end modules.

**Document history (please add a line when you edit the document)**

| Date       | Editor(s)               | OpenSILEX version   | Comment           |
|------------|-------------------------|---------------------|-------------------|
| 17/09/2026 | yvan.roux@opensilex.fr  | 1.5.0 Freaky Fossil | Document creation |


## Table of contents

<!-- TOC -->
* [Technical documentation : [module front-end] How OpenSILEX load front-end modules.](#technical-documentation--module-front-end-how-opensilex-load-front-end-modules)
  * [Table of contents](#table-of-contents)
  * [Context](#context)
  * [Building the front-end](#building-the-front-end)
    * [general workflow](#general-workflow)
    * [development startup and hot reload](#development-startup-and-hot-reload)
<!-- TOC -->

## Context and links to other documents

This document only describes the technical part of the front-end module loading.

Further steps could be to :
- create a new module from zero, see [modules.md](modules.md)
- extending the front-end by adding new pages, see [module-front-end-extension.md](module-api-and-interface-extension.md)
- modifying the front-end style by overloading the default theme, see [module-theme-personalization.md](module-theme-personalization.md)
- modifying some frontend pages by overriding default components, see [module-front-end-extension.md](module-api-and-interface-extension.md)

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

When running mvn clean install, Maven should build the front-end. Meaning that all the files in the src directory are
transpiled into a single JavaScript file named {module_name}.umd.min.js, placed in the dist directory.
Maven includes this build file in the final jar file at `target/classes/front`.

When launching the front-end, the `OpenSilexVuePlugin.ts` modify the main build (opensilex-front/front) to add into it
a `<script>` balise for each module to load.

Each script balise has a src attribute that calls the `/vuejs/extension/js/{module}.js` API endpoint and so calls the
`FrontAPI#getExtension` method which returns the module.umd.min.js file.

for exemple, the phis module will generate the following script balise on a local instance:
`<script src="http://localhost:8666/rest/vuejs/extension/js/opensilex-phis.umd.min.js"></script>`

### development startup and hot reload

When running the front-end in development mode, the `StartServerWithFront.java` class builds each front-end module
with hot reload enabled and copy and paste the resulting files into `target/classes/front`.

Each build file is watched by the `StartServerWithFront.java` class just so when a front-end module is modified, the
build is changed by the hot reload, the watcher is notified and triggers the hot reload of the main front-end process.

To trigger the general hot reload, `StartServerWithFront.java` class modify the `opensilex.dev.ts` file.
This file is in the `opensilex-front/front/src` directory so its changes automatically trigger the hot reload of the
main front-end process.

The way the main front-end process includes the front-end modules is the same as the general workflow described above.