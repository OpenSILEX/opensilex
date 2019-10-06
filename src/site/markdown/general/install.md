OpenSILEX - Installation
================================================================================

# What is a OpenSILEX ?
--------------------------------------------------------------------------------

OpenSILEX is an Hybrid Semantic Data Management System 
build with concepts of semantic web, big data and massive image storage
for life science research.

# How to try it ?
--------------------------------------------------------------------------------

You can play with it on http://www.opensilex.org/

# How to test it on my computer ?
--------------------------------------------------------------------------------

## Install Docker & Docker Compose

Download and install Docker (Minimal version 18.09) for your operating system.

[Docker installation](https://docs.docker.com/install/)

Download and install Docker Compose (Minimal version 1.17.1) for your operating system.

[Docker Compose installation](https://docs.docker.com/compose/install/)

Download opensilex-docker on GitHub // TODO LIEN

Run the script // TODO COMMANDE + PARAMETRES

Open your favorite browser at // TODO ADRESSES


# Which technolgies OpenSILEX is made of ?
--------------------------------------------------------------------------------

OpenSILEX is based on Java the following software:

- RDF4J: semantic data in triplestore
- MongoDB: massive data storage
- PostgreSQL: legacy data support

And for development purpose:

- Docker: container manager
- Git: version manager
- Maven: dependency manager


# How to install latest version ?
--------------------------------------------------------------------------------

## Install Java

Download and install Java JDK (Minimal version 11) for your operating system.

 `<JAVA_BIN>` variable will be used in this document to indicate the path to java executable.

[Java installation](https://jdk.java.net/java-se-ri/11)


## Install and setup databases 

// TODO LIEN

## Install OpenSilex

Download zip //TODO LIEN

Unzip // TODO COMMANDE

## Define configuration

// TODO


# How to run OpenSilex ?
--------------------------------------------------------------------------------

Main entry point for the application is a Command Line Interface (CLI) run by:
```
java -jar opensilex.jar
```
We will use `opensilex` as an alias to this command in this documentation.

To start OpenSILEX: `opensilex server start`

You should now access it at: // TODO LIEN

Default admin login: `admin@opensilex.org`

Default admin password: `admin`

To stop OpenSILEX: `opensilex server stop`

Other commands are available depending of loaded modules.

You can get list and help for all commands using 
`opensilex help` or `--help` flag on commands.

See CLI section for more details.


# How to setup development environment for OpenSILEX ?
--------------------------------------------------------------------------------

## Install Git

Download and install git (Minimal version 2.17) for your operating system

[git installation](https://git-scm.com/book/en/v2/Getting-Started-Installing-Git)

## Install Maven

Download and install Maven (Minimal version 3.5) for your operating system.

[Maven installation](https://maven.apache.org/download.cgi)

## Install Docker & Docker Compose

Download and install Docker (Minimal version 18.09) for your operating system.

[Docker installation](https://docs.docker.com/install/)

Download and install Docker Compose (Minimal version 1.17.1) for your operating system.

[Docker Compose installation](https://docs.docker.com/compose/install/)

## IDE

We use NetBeans as main IDE but you can use any supporting Maven.

## Install from sources

Get the sources: git clone // TODO COMMANDE

Initialize databases: // TODO COMMANDE DOCKER

## Build application

Basic maven command: `mvn clean install`

Resulting application is packaged in the file `./opensilex-release/target/opensilex-release-X.Y.Z-SNAPSHOT.zip`

Uncompressed archive is automatically availbale in folder `./opensilex-release/target/opensilex-release-X.Y.Z-SNAPSHOT/`

Version number can be changed in file `./opensilex-parent/pom.xml` changing `revision` property.

Contact us in case of failure // TODO MAIL

