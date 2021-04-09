Modules How-to
==============

# Create a new module for opensilex 

1. Create a directory in ``opensilex`` directory with the name of the module, here {module_name} ``Example : inrae-sixtine``.

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

2. Module skeleton

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
│       └── resources
```
See [Theme subject](theme.md) for more details.

3. Add a pom file to configure the maven project **pom.xml** in module directory ``opensilex/{module_name}``

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
        <artifactId>{module_name}</artifactId>
        <version>${revision}</version>
        <relativePath>../{module_name}/pom.xml</relativePath>
    </parent>
</project>
```

4. Add a class with the module name which will describe interfaces, services and config that it implements.

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

5. Update global ***pom.xml definition**

You need to the new module to the full build stage you need to add it to the global pom.xml
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

