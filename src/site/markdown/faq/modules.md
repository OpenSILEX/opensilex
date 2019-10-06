OpenSILEX Modules - FAQ
================================================================================

# what is an OpenSILEX module ?
--------------------------------------------------------------------------------

Main OpenSILEX module is `opensilex` which provide:

- CLI
- Extensible typed configuration in YAML
- Module management
- Configurable service management
- Web Server with a secured and extensible REST API
- SPARQL request generation for annotated models
- Big data mapping with annotated models
- File Storage Service
- Update system
- Unit Testing
- Logging system
- (Angular integration with a plugin system)

An OpenSILEX module is a set of extensions of these functionnalities, they can:

- Define new CLI commands
- Extends configuration
- Define their own extensions interface
- Define new configurable services
- Extends REST API with new web services
- Define annotaded models mapping for SPARQL
- Define annotaded models mapping for Big Data
- Access to file storage
- Define new update class
- Define unit and integration tests
- (Define new Angular plugins)


# How to create a new module quickly ?
--------------------------------------------------------------------------------

Before creating a you should define some parameters:

- {groupId}: This is an identifier for your organisation, by example: org.opensilex (this one is reserved for official OpenSILEX modules)
- {artifactId}: This is the name of your module, by example: my-module (the format should be lower case with dash beetween words)
- {modulePackagePrefix}: This is the base package for any Java classes in this module, it's generally derived from {artifactId} and {groupId}, by example: org.opensilex.myModule

// TODO MAVEN COMMAND

Now when you build or run your project, your module will be automatically included and loaded.

For now it doesn't do anything, please see other FAQ entries for adding more functionalities.


# How to create a new module manually ?
--------------------------------------------------------------------------------

A module is a particular type of Maven module (which simply is a folder with a pom.xml file in it)

1. Create base folder

In the opensilex-dev folder, create a new folder called {artifactId}

2. Create pom.xml

In opensilex-dev/{articfactId} folder, create a pom.xml file with this content:
```
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>{groupId}</groupId>
    <artifactId>{artifactId}</artifactId>
    <name>{artifactId}</name>

    <packaging>jar</packaging>
    
    <url>https://www.opensilex.org/</url>
    
    <parent>
        <groupId>org.opensilex</groupId>
        <artifactId>opensilex-module</artifactId>
        <version>${revision}</version>
        <relativePath>../opensilex-module/pom.xml</relativePath>
    </parent>
    
<!-- Add your module specific dependencies in this section
    <dependencies>
        <dependency>
            <groupId>org.example</groupId>
            <artifactId>my-lib</artifactId>
            <version>1.2.3</version>
        </dependency>
    </dependencies>
-->    
</project>
```

3. Create the Java base source package

In opensilex-dev/{articfactId} folder, create the path corresponding to src/main/java/{modulePackagePrefix}

In our example it will be: src/main/java/org/opensilex/myModule

4. Create OpenSILEX module Class

In opensilex-dev/{articfactId}/src/main/java/{modulePackagePrefix} create a class like "MyModule" and make it extend "OpenSilexModule" class like in the following example:
```
package {modulePackagePrefix};

import org.opensilex.module.OpenSilexModule;

/**
 * My module implementation
 */
public class MyModule extends OpenSilexModule {

}
```

5. Reference your new module in opensilex-dev

Edit the pom.xml file in opensilex-dev folder and reference your new module as a module to build and as a dependency.

You can see an example bellow, note that you should leave ${revision} for the version of your module.
```
<project>
	...
	<modules>
        	<!-- Required modules -->
	        <module>opensilex-parent</module> 
		...
		<module>{articfactId}</module> 
	</modules>

	...

	<dependencies>
        	<!--Required dependencies--> 
	        <dependency>
			<groupId>org.opensilex</groupId>
	            	<artifactId>opensilex</artifactId>
	            	<version>${revision}</version>
        	</dependency>
		...
	        <dependency>
			<groupId>{groupId}</groupId>
	            	<artifactId>{articfactId}</artifactId>
	            	<version>${revision}</version>
        	</dependency>
	<dependencies>
<project>
```

6. Build & Run

Now when you build or run your project, your module will be automatically included and loaded.

For now it doesn't do anything, please see other FAQ entries for adding more functionalities.


# How to use features from a module into another ?
--------------------------------------------------------------------------------

To access features and classes from a module A into another module B,
add A as a dependency in the pom.xml file of the module B.
```
<project>
        <groupId>{moduleB_groupId}</groupId>
        <artifactId>{moduleB_artifactId}</artifactId>
        
	...

	<dependencies>
	        <dependency>
			<groupId>{moduleA_groupId}</groupId>
	            	<artifactId>{moduleA_artifactId}</artifactId>
	            	<version>${revision}</version>
        	</dependency>
	<dependencies>
<project>
```
