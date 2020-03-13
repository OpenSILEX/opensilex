
# Create a new opensilex module for development purpose

1. Create a directory in opensilex-dev directory with the name of the module, here {module_name}.

```
opensilex-dev
├── opensilex-{module_name}
├── opensilex
├── opensilex-core
├── opensilex-dev-tools
├── opensilex-doc
├── opensilex-front
├── opensilex-fs
├── opensilex-module
├── opensilex-nosql
├── opensilex-parent
├── opensilex-release
├── opensilex-rest
├── opensilex-sparql
├── opensilex-swagger-codegen-maven-plugin
```

2. Add a pom file to configure the maven project

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

    <artifactId>opensilex-{module_name}</artifactId>
    <packaging>jar</packaging>
    <name>opensilex-{module_name}</name>

    <parent>
        <groupId>org.opensilex</groupId>
        <artifactId>opensilex-module</artifactId>
        <version>${revision}</version>
        <relativePath>../opensilex-module/pom.xml</relativePath>
    </parent>
</project>
```

3. Add a class with the module name which will describe interfaces, services and config that it implements.

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

# Use new opensilex module for development in the global build stage

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
        <module>opensilex</module>
        <module>opensilex-sparql</module>
        <module>opensilex-nosql</module>
        <module>opensilex-fs</module>
        <module>opensilex-rest</module>
        <module>opensilex-module</module>
        <module>opensilex-core</module>
        <module>opensilex-front</module>
        
        <!-- Extension modules -->
        <module>opensilex-module</module>

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
            <artifactId>opensilex</artifactId>
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
            <artifactId>opensilex-rest</artifactId>
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
            <artifactId>opensilex-module</artifactId>
            <version>${revision}</version>
        </dependency>
    </dependencies>
    [....]
```