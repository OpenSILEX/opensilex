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

## Context and links to other documents

This document explains how to create the front-end part of a new module.

Creating a front-end for your module could be useful for:
- extending the front-end by adding new pages. See [module-api-and-interface-extension.md](module-api-and-interface-extension.md)
- modifying the front-end by overloading the default theme. See [module-theme-personalization.md](module-theme-personalization.md)
- overriding default components. See [overriding-defaults-components.md](overriding-defaults-components.md)

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
We will work only on the front part of the module.

### Vite config and dependencies
//be concise about the configuration of vite. it should use the minify and respect the name of the build (just as phis). Use index.ts as entry point.
//about dependencies, speak quickly about package.json and the rollup options of the vite config (need to put vueI18N in the rollup options sinon on à pas accès aux clefs de traductions de opensilex-front).

### Create your first components
//par convention le créer dans le dossier src/components.
//On peut Créer des dépendances vers les modules opensilex-front / core / security sans problème.
//On peut utiliser les clefs de traductions des composants de opensilex-front ou des fichiers messages dans opensilex-front/front/src/lang.
//chercher dans les sources, normalement on peut définir notre propre fichier message-en et message-fr pour créer de nouvelles clefs de traductions.

### index.ts : export your module as a plugin
//give the exemple of phis
