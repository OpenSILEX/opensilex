Install OpenSILEX in production
===============================

# Pre-requesites

## Operating System

OpenSILEX should work on any system where the required softwares are available but we recommand using Linux for production installation.

Commands in this document should work on any Debian-like distribution but should be easly adapted for any Linux distributions

## Java 

You need at least [Java JDK 8+](https://jdk.java.net/) installed on the server operating system.

## Setup MongoDB

Please follow MongoDB official install documentation for your operating system.

[Install MongoDB 4.2+](https://docs.mongodb.com/manual/installation/)

## Setup a triplestore

You can use [RDF4J](https://rdf4j.org/) or [GraphDB](http://graphdb.ontotext.com/) for storing semantic data.

You can find [here](https://db-engines.com/en/system/GraphDB%3BRDF4J) a quick comparison of both triplestores engine.

Please refer to their websites for more information.

### RDF4J

For RDF4J you need to setup and configure a Servlet Container server like [Tomcat](http://tomcat.apache.org/) first.

And then follow their [documentation](https://rdf4j.org/documentation/server-workbench-console/) .


### GraphDB

Ontotext - GraphDB exist in free version and can be used as an alternative for RDF4J.

Please follow their [documentation](http://graphdb.ontotext.com/documentation/free/installation.html) to install it.

# Installation

All directories and user names in this installation procedure can be changed but you need to change the configuration accordlingly to make it work.

## Setup a user

Create a user with it's home directory:

```sudo useradd -s /bin/bash -d /home/opensilex/ -m opensilex```

Setup a password to your new user:

```sudo passwd opensilex```

Connect with this user:

```su - opensilex```

## Create directories

Directory for OpenSilex binaries:
```mkdir -p /home/opensilex/bin```

Directory for OpenSilex configuration file:
```mkdir -p /home/opensilex/config```

Directory for OpenSilex data file storage:
```mkdir -p /home/opensilex/data```

Directory for OpenSilex file logs:
```mkdir -p /home/opensilex/logs```

## Download & extract OpenSilex production release

Please download OpenSILEX latest release archive on [Github](https://github.com/OpenSILEX/opensilex-dev/releases)

Extract the downloaded zip file into ```/home/opensilex/bin```

You should get the following directory structure (`X.Y.Z` meaning the OpenSILEX release version):

/home/opensilex/
+-- bin/
    +-- X.Y.Z/
        +-- opensilex.jar
        +-- logback-prod.xml
        +-- modules/
            +-- opensilex-core.jar
            +-- opensilex-front.jar
            +-- opensilex-fs.jar
            +-- opensilex-nosql.jar
            +-- opensilex-rest.jar
            +-- opensilex-sparql.jar
            +-- phis2ws.jar
+-- config/
+-- data/
+-- logs/

## Create configuration file

Create a YML file in `/home/opensilex/config` named `opensilex.yml` by example.
It could be any name you want if you want to work with multiple configurations.

Here is a minimal example of configuration content, all values between `<???>` must be adapted to your setup:

```yml
ontologies:
    # Base URI domain for every generated URI by this OpenSILEX instance
    baseURI: <http://www.opensilex.org/>

    # Base URI domain alias for short URI
    baseURIAlias: <os>

    # Triple store configuration
    sparql:
        rdf4j:
            # Connection URI to RDF4J or GraphDB instance (they use the same API)
            serverURI: <http://localhost:8080/rdf4j-server/>

            # Triple store repository name
            repository: opensilex

file-system: 
    # Path to root file data storage directory
    storageBasePath: </home/opensilex/data>

big-data:   
    nosql:
        # MongoDB connection configuration
        mongodb:
            # MongoDB server host
            host: <localhost>

            # MongoDB server port
            port: <27017>

            # MongoDB database name
            database: <opensilex>

phisws:
    vocabulary: http://www.opensilex.org/vocabulary/oeso
    infrastructure: opensilex
    
    layerFileServerDirectory: /var/www/html/layers
    layerFileServerAddress: http://localhost/layers
    
    uploadImageServerDirectory: /var/www/html/images
    imageFileServerDirectory: http://localhost/images
```