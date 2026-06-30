# Technical documentation : [module] Create a new module for OpenSilex

**Document history (please add a line when you edit the document)**

| Date       | Editor(s)                     | OpenSILEX version   | Comment                                                |
|------------|-------------------------------|---------------------|--------------------------------------------------------|
| 27/04/2020 | arnaud.charleroy@opensilex.fr |                     | Document creation                                      |
| 30/06/2026 | yvan.roux@opensilex.fr        | 1.5.0 Freaky Fossil | precisions, formatting and link to other documentation |



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

## Definitions

- **Ontology** : An ontology is a formal representation of a set of concepts and the relationships between those concepts. It describes a data model that is used in OpenSILEX to represent concepts and manage data.
- **Class** : In ontology, a class is a type of concept, for example, "Plant" or "Experiment". A class `ClassA` represent objets that has the relation `object rdf:type ClassA`.
- **Type** : The word type is often used as a synonym of class from a user perspective. In the interface we define new types of events rather than new subclasses of event.
- **Property** : In ontology, a property describes a relationship between concepts. For example, `rdfs:label` is a property that links a concept to a string that is its label (sort of name).

## Functional requirements

Creating a new module that extends the ontology allows the user to statically add new concepts to the ontology at compile time.

This could be useful for:
- Creating new subtypes of scientific objects or events with special data linked to it
- Extending existing classes with new properties or relationships
- Easily sharing this ontology modification with other instances of OpenSILEX
- creating a whole new concept that you will use in the module to extend the API with new web services and maybe new front-end pages.

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
See [Theme subject](theme.md) for more details.

### 3. Add a pom file to configure the maven project **pom.xml** in module directory ``opensilex/{module_name}``

```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<!--
******************************************************************************
 OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 Copyright © INRAE 2020
 Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
 
 PHIS pom.xml
 This module is {description} web services API integrate as a module of the new
 modular system.
******************************************************************************
-->
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <artifactId>{module_name}</artifactId>
    <packaging>jar</packaging>
    <name>{module_name}</name>

    <properties>
        <revision>BUILD-SNAPSHOT</revision>
        <skipFrontTypesGeneration>true</skipFrontTypesGeneration>
    </properties>

    <parent>
        <groupId>org.opensilex</groupId>
        <artifactId>opensilex-module</artifactId>
        <version>${revision}</version>
        <relativePath>../opensilex-module/pom.xml</relativePath>
    </parent>
</project>
```

### 4. Add a class with the module name which will describe interfaces, services and config that it implements.

The minimal OpenSilexModule configuration is the following :

```java
package org.opensilex.{module_name};

import org.opensilex.OpenSilexModule;
import org.opensilex.rest.extensions.APIExtension;

/**
 * {module_name} opensilex module implementation
 */
public class {module_name}Module extends OpenSilexModule implements APIExtension {

}
```

### 5. Update global ***pom.xml definition**

If you want your new module to be part of the OpenSilex build, you need to add it to the global pom.xml file in two places :
- In the `<module> </module>` section to include it in the build
- In the `<dependency> </dependency>` section to make it available for other modules

 ```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<!--
******************************************************************************
 OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 Copyright © INRAE 2020
 Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
 
 OpenSilex Development Environment main pom.xml
 If you add a new module, add it in the <modules> section and
 in the <dependencies> section in order to make it work.
******************************************************************************
-->
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <!-- [...] first properties skipped in this demo  -->
    <modules>
        <!-- Main OpenSilex modules -->
        <module>opensilex-parent</module>
        <module>opensilex-main</module>
        <module>opensilex-sparql</module>
        <module>opensilex-nosql</module>
        <module>opensilex-fs</module>
        <module>opensilex-security</module>
        <module>opensilex-core</module>
        <module>opensilex-front</module>
        
        <!-- Extension modules -->
        <module>{module_name}</module>

        <!-- Development module -->
        <module>opensilex-dev-tools</module>
        
        <!-- Release module -->
        <module>opensilex-release</module>
         
        <!-- Maven Plugin module -->
        <module>opensilex-swagger-codegen-maven-plugin</module>
        
        <!-- Documentation module -->
        <module>opensilex-doc</module>
    </modules>
    
    <dependencies>
        <!-- Plugin dependencies -->
        <dependency>
            <groupId>org.opensilex</groupId>
            <artifactId>opensilex-swagger-codegen-maven-plugin</artifactId>
            <version>${revision}</version>
        </dependency>
            
        <!-- OpenSilex build-in modules dependencies-->
        <dependency>
            <groupId>org.opensilex</groupId>
            <artifactId><artifactId>opensilex-main</artifactId></artifactId>
            <version>${revision}</version>
        </dependency>
        
        <dependency>
            <groupId>org.opensilex</groupId>
            <artifactId>opensilex-sparql</artifactId>
            <version>${revision}</version>
        </dependency>
        
        <dependency>
            <groupId>org.opensilex</groupId>
            <artifactId>opensilex-nosql</artifactId>
            <version>${revision}</version>
        </dependency>
                
        <dependency>
            <groupId>org.opensilex</groupId>
            <artifactId>opensilex-fs</artifactId>
            <version>${revision}</version>
        </dependency>
        
        <dependency>
            <groupId>org.opensilex</groupId>
            <artifactId>opensilex-security</artifactId>
            <version>${revision}</version>
        </dependency>
        
        <dependency>
            <groupId>org.opensilex</groupId>
            <artifactId>opensilex-core</artifactId>
            <version>${revision}</version>
        </dependency>
            
        <dependency>
            <groupId>org.opensilex</groupId>
            <artifactId>opensilex-front</artifactId>
            <version>${revision}</version>
        </dependency>

        <!--Other extension modules must be declared as dependencies-->
        <dependency>
            <groupId>org.opensilex</groupId>
            <artifactId>{module_name}</artifactId>
            <version>${revision}</version>
        </dependency>
    </dependencies>
    [....]
```

## Documentation for next steps

- extending core ontology : [ontology-module-extension-system.md](/src/main/resources/technical-documentation/opensilex-module/ontology-module-extension-system.md)
- extending API and front-end in a new module : [ontology-module-extension-system.md](/src/main/resources/technical-documentation/opensilex-module/ontology-module-extension-system.md)

