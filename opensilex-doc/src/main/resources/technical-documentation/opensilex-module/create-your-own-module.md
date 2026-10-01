# Technical documentation : [module] Create a new minimal module for OpenSilex

**Document history (please add a line when you edit the document)**

| Date       | Editor(s)                     | OpenSILEX version   | Comment                                                |
|------------|-------------------------------|---------------------|--------------------------------------------------------|
| 27/04/2020 | arnaud.charleroy@opensilex.fr |                     | Document creation                                      |
| 30/06/2026 | yvan.roux@opensilex.fr        | 1.5.0 Freaky Fossil | precisions, formatting and link to other documentation |



## Table of contents

<!-- TOC -->
* [Technical documentation : [module] Create a new minimal module for OpenSilex](#technical-documentation--module-create-a-new-minimal-module-for-opensilex)
  * [Table of contents](#table-of-contents)
  * [Definitions](#definitions)
  * [What this document is about](#what-this-document-is-about)
  * [Create your module's directory](#create-your-modules-directory)
  * [2. Minimal module skeleton](#2-minimal-module-skeleton)
    * [pom.xml file](#pomxml-file)
    * [Module java class](#module-java-class)
  * [Add your module to OpenSILEX](#add-your-module-to-opensilex)
      * [A. On an official OpenSILEX release build :](#a-on-an-official-opensilex-release-build-)
      * [B. During development : Update global ***pom.xml definition**](#b-during-development--update-global-pomxml-definition)
  * [Complete module skeleton](#complete-module-skeleton)
  * [Going further](#going-further)
<!-- TOC -->

## Definitions

- **Ontology** : An ontology is a formal representation of a set of concepts and the relationships between those concepts. It describes a data model that is used in OpenSILEX to represent concepts and manage data.
- **Class** : In ontology, a class is a type of concept, for example, "Plant" or "Experiment". A class `ClassA` represent objets that has the relation `object rdf:type ClassA`.
- **Type** : The word type is often used as a synonym of class from a user perspective. In the interface we define new types of events rather than new subclasses of event.
- **Property** : In ontology, a property describes a relationship between concepts. For example, `rdfs:label` is a property that links a concept to a string that is its label (sort of name).

## What this document is about

Creating a new module allows personalizing OpenSILEX's ontology, API and front-end.

This could be useful for:
- extending the core ontologie. See [ontology-module-extension-system.md](ontology-module-extension-system.md)
- extending the API. See [module-api-and-interface-extension.md](module-api-and-interface-extension.md)
- personalizing the front-end. See [interface-personalization-how-to.md](interface-personalization-how-to.md)

This documentation helps you to create a new minimal module for OpenSilex and load it in OpenSilex. At the end of this document,
your module will not modify OpenSILEX behavior, interface or ontology. When you minimal module is ready, please follow
one of the links above to personalize OpenSILEX thanks to your new module.

## Create your module's directory

Create a directory in `opensilex` directory with the name of the module, here {module_name} `Example : inrae-sixtine`.

```
opensilex
├── {module_name}
├── opensilex-main
├── opensilex-core
├── opensilex-dev-tools
├── opensilex-doc
├── opensilex-front
├── opensilex-fs
├── opensilex-nosql
├── opensilex-parent
├── opensilex-release
├── opensilex-security
├── opensilex-sparql
├── opensilex-swagger-codegen-maven-plugin
```

## 2. Minimal module skeleton

The minimum file structure for a new module contains only two files.

Notes : *We use these naming conventions as examples, but **they are not mandatory.***
```
{module_name} # module
│── pom.xml
├── src # back end java sources
│   └── main
│       ├── java
│       │   └──org.opensilex.{module_name}
│       │       └── {module_name}Module.java
```
- `pom.xml` is the maven configuration file for the module.
- `{module_name}Module.java` is the main class of the module.

We will describe this two files in more details in the next sections.

### pom.xml file

Add a pom file to configure the maven project **pom.xml** in module directory `opensilex/{module_name}`

```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<!--
******************************************************************************
 OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 Copyright © INRAE 2020
 Contact: anne.tireau@inra.fr, arnaud.charleroy@inrae.fr
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

Here again, this is a minimal configuration file. You can add build scripts or dependencies to other modules. But you can't
run the module without at least this configuration.

### Module java class

Add a class named `{module_name}Module.java` which will describe interfaces, services and config that it implements.

The minimal content of this class is the following :

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

Extending OpenSilexModule is mandatory as it allows the module to be listed with the others modules. This is how the
main process (opensilex-main) retrieves the list of modules to load.

## Add your module to OpenSILEX

Once you have created your module, you need to add it to OpenSILEX so that it can be loaded and used.
There are two ways to do this:
- When you are developing your module, the easiest way is to modify the global pom.xml. See how in the step B.
- Once the module is ready for production, build it following the instructions of the step A.

#### A. On an official OpenSILEX release build :

- Compile your module with `mvn clean install` (launch the command in the opensilex-dev/{module_name} directory)
- Copy the jar file from `opensilex-dev/{module_name}/target/{module_name}-{version}.jar`
- rename the jar file to `{module_name}.jar`
- Copy the jar file in the `modules` directory of the release build folder (you can fint zip file of release build folders on our GitHub repository)

#### B. During development : Update global ***pom.xml definition**

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

      <!-- ... etc .......................................  -->

      <!-- Extension modules -->
        <module>{module_name}</module>

        <!-- Development module -->
        <module>opensilex-dev-tools</module>

      <!-- ... etc .......................................  -->  

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

      <!-- ... etc .......................................  -->


      <!--Other extension modules must be declared as dependencies-->
        <dependency>
            <groupId>org.opensilex</groupId>
            <artifactId>{module_name}</artifactId>
            <version>${revision}</version>
        </dependency>
    </dependencies>
  <!-- ... etc .......................................  -->  
```

## Complete module skeleton
example of what you can find in the skeleton of a complete module :

```
# module_name  => .e.g : inrae-sixtine
# short_module_name  => .e.g : sixtine
{module_name} # module
├── front
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
## Going further
Now you should b eready to personalize OpenSilex with your new module.

See the [What this document is about](#what-this-document-is-about) section of the documentation for the next steps.
