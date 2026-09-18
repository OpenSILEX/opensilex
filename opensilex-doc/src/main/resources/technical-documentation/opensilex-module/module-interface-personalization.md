# Technical documentation : [module front-end] Extend and modify OpenSILEX's front-end

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
* [Definitions](#definitions)
* [Functional requirements](#functional-requirements)
* [Create a new module for opensilex](#create-a-new-module-for-opensilex-)
    * [1. Create a directory in ``opensilex`` directory with the name of the module, here {module_name} ``Example : inrae-sixtine``.](#1-create-a-directory-in-opensilex-directory-with-the-name-of-the-module-here-module_name-example--inrae-sixtine)
    * [2. Module skeleton](#2-module-skeleton)
    * [3. Add a pom file to configure the maven project **pom.xml** in module directory ``opensilex/{module_name}``](#3-add-a-pom-file-to-configure-the-maven-project-pomxml-in-module-directory-opensilexmodule_name)
    * [4. Add a class with the module name which will describe interfaces, services and config that it implements.](#4-add-a-class-with-the-module-name-which-will-describe-interfaces-services-and-config-that-it-implements)
    * [5. Update global ***pom.xml definition**](#5-update-global-pomxml-definition)
* [Documentation for next steps](#documentation-for-next-steps)
<!-- TOC -->

## Functional requirements

Personalizing Opensilex's front-end .

This could be useful for:
- extending the core ontologie, see [ontology-module-extension-system.md](ontology-module-extension-system.md)
- extending the API, see [module-api-and-interface-extension.md](module-api-and-interface-extension.md)
- extending the front-end by adding new pages, see [module-api-and-interface-extension.md](module-api-and-interface-extension.md)
- modifying the front-end by overloading the default theme, see [module-theme-personalization.md](module-theme-personalization.md)
- overriding default components, see [overriding-defaults-components.md](overriding-defaults-components.md)

## Create a new module for opensilex

### 1. Create a directory in ``opensilex`` directory with the name of the module, here {module_name} ``Example : inrae-sixtine``.

```
opensilex
├── {module_name}
├── opensilex-main
├── opensilex-core
├── opensilex-dev-tools
├── opensilex-doc
├── opensilex-front
├── opensilex-fs
├── {module_name}
├── opensilex-nosql
├── opensilex-parent
├── opensilex-release
├── opensilex-security
├── opensilex-sparql
├── opensilex-swagger-codegen-maven-plugin
```

### 2. Module skeleton

How to create module front part:

Notes : *We use these naming conventions as examples, but **they are not mandatory.***
```bash
# module_name  => .e.g : inrae-sixtine
# Module_name  => .e.g : Sixtine
# short_module_name  => .e.g : sixtine
{module_name} # module
├── front # front
│   ├── babel.config.js # translation config
│   ├── package.json # module javascript packages description
│   ├── src # javascript sources
│   │   ├── components # vue components
│   │   │   └── layout
│   │   │       ├── {Module_name}FooterComponent.vue
│   │   │       ├── {Module_name}HeaderComponent.vue
│   │   │       ├── {Module_name}HomeComponent.vue
│   │   │       ├── {Module_name}LoginComponent.vue
│   │   │       └── {Module_name}MenuComponent.vue
│   │   ├── index.ts # register vue components
│   │   ├── lang # lang translation
│   │   │   ├── {short_module_name}-en.json
│   │   │   └── {short_module_name}-fr.json
│   │   ├── lib # need to build archive
│   │   └── shims-vue.d.ts # ??
│   ├── theme # theme files imgs, scss variables, fonts etc...
│   │   └── {short_module_name}
│   │       ├── {short_module_name}.yml
│   │       ├── fonts
│   │       ├── images
│   │       └── variables.scss
│   ├── tsconfig.json # typescript config
│   ├── vue.config.js # vue config
│   └── yarn.lock # yarn packages
├── pom.xml  # module pom file
├── src # back end java sources
│   └── main
│       ├── java
│       │   └──org.opensilex.{module_name}
│       │       └── {module_name}Module.java
│       └── resources
```

