# Install OpenSILEX in production

<!-- TOC -->
* [Install OpenSILEX in production](#install-opensilex-in-production)
* [Using the ready to use Docker](#using-the-ready-to-use-docker)
* [Pre-requisites](#pre-requisites)
  * [Operating System](#operating-system)
  * [Java](#java)
  * [Set up MongoDB](#set-up-mongodb)
  * [Set up a triplestore](#set-up-a-triplestore)
    * [RDF4J](#rdf4j)
    * [GraphDB](#graphdb)
* [Installation](#installation)
  * [Set up a user](#set-up-a-user)
  * [Create directories](#create-directories)
  * [Download & extract OpenSILEX production release](#download--extract-opensilex-production-release)
  * [Configuration](#configuration)
    * [Create main configuration file](#create-main-configuration-file)
    * [Configure logging](#configure-logging)
  * [Initialize database and check configuration](#initialize-database-and-check-configuration)
    * [Create a script to access instructions](#create-a-script-to-access-instructions)
    * [Add an alias](#add-an-alias)
    * [Initialize your triplestore](#initialize-your-triplestore)
  * [Start OpenSILEX](#start-opensilex)
  * [Stop OpenSILEX](#stop-opensilex)
  * [Add a Reverse Proxy Nginx to redirect application on port 80](#add-a-reverse-proxy-nginx-to-redirect-application-on-port-80)
<!-- TOC -->

# Using the ready to use Docker

Before reading this document, you can check the [docker container for OpenSILEX ](https://github.com/OpenSILEX/opensilex-docker-compose)
This docker container allows you to install and configure OpenSILEX and all required databases (RDF4J and MongoDB) within only a few steps.

If you prefer a fine tuned control on the installed databases or if you prefer a custom installation, you are at the right place !

# Pre-requisites

## Operating System

OpenSILEX should work on any system where the required pieces of software are available, but we recommend using Linux for a production installation.

The commands in this document should work on any Debian-like distribution (configured with sudo) but can be easily adapted for any Linux distribution.

## Java

You need at least [Java JDK 11+](https://jdk.java.net/) installed on the server.

You can install it on Linux with the following command:

```
sudo apt install openjdk-11-jdk openjdk-11-jre
```

You can check Java installation and version with the following command:

```
java --version
```

## Set up MongoDB

Please follow MongoDB's official install documentation specific to your operating system.

[Install MongoDB 4.2+](https://docs.mongodb.com/manual/installation/)

/!\ **CAUTION** /!\

You must start a MongoDB Replica Set to allow transaction usage with MongoDB. This requires the use of a MongoDB server with a version >= 4.2.
To do so you need to start the MongoDB daemon with the option `--replSet opensilex`. `opensilex` stands for your replica set name and can be modified at your own will.
Look at this [documentation](https://docs.mongodb.com/manual/tutorial/deploy-replica-set/#considerations-when-deploying-a-replica-set)
for more information about replica set.

We recommend the use of this [MongoDB Docker image](https://hub.docker.com/_/mongo). More
documentation for this image [here](https://www.mongodb.com/compatibility/deploying-a-mongodb-cluster-with-docker)

> Linux example command

```
docker network create mongoCluster
docker run -d --rm -p 27017:27017 --name mongo_opensilex --network mongoCluster mongo:5 mongod --replSet opensilex --bind_ip localhost,mongo_opensilex
```

- The MongoDB exposed port is `27017`
- A replica set named `opensilex` is created
- The docker container name is `mongo_opensilex`
- The docker container uses a network called `mongoCluster`

## Set up a triplestore

You can use [RDF4J](https://rdf4j.org/) or [GraphDB](http://graphdb.ontotext.com/) for storing semantic data.

You can find a quick comparison of both triplestore engines [here](https://db-engines.com/en/system/GraphDB%3BRDF4J).

Please refer to their websites for more information.

### RDF4J

For RDF4J you first need to set up and configure a Servlet Container server like [Tomcat](http://tomcat.apache.org/).
And then follow [RDF4J's documentation](https://rdf4j.org/documentation/server-workbench-console/) .

You can also use this docker image: [eclipse/rdf4j-workbench](https://hub.docker.com/r/eclipse/rdf4j-workbench)

> Linux example command

```bash
sudo docker run -d -p 8080:8080 -e JAVA_OPTS="-Xms4g -Xmx4g" \
	-v data:/var/rdf4j -v logs:/usr/local/tomcat/logs eclipse/rdf4j-workbench:3.7.7
```

- The RDF4J exposed port is `8080`
- RDF4J data will be stored in `/var/rdf4j` in the docker host
- Tomcat server logs in `/usr/local/tomcat/logs` in the docker host
- `4GB` of RAM memory are allocated for the JVM's heap

### GraphDB

A free version of GraphDB is available and can be used as an alternative to RDF4J.

Please follow their [documentation](https://graphdb.ontotext.com/documentation/) to install it.

You can also use this docker image: [ontotext/graphdb](https://hub.docker.com/r/ontotext/graphdb)

> Linux example command:

```bash
sudo docker run -d ontotext/graphdb
```

# Installation

All directories and usernames in this installation procedure can be changed, but you need to change the configuration accordingly in order to make it work.

## Set up a user

Create a user with its home directory:

```
sudo useradd -s /bin/bash -d /home/opensilex/ -m opensilex
```

Set up a password for your new user:

```
sudo passwd opensilex
```

Give sudo permissions to this user :

```
sudo usermod -a -G sudo opensilex
```

Connect with this username and the defined password:

```
su - opensilex
```

## Create directories

Directory for OpenSILEX binaries:

```
mkdir -p /home/opensilex/bin
```

Directory for OpenSILEX configuration file:

```
mkdir -p /home/opensilex/config
```

Directory for OpenSILEX data file storage:

```
mkdir -p /home/opensilex/data
```

Directory for OpenSILEX file logs:

```
mkdir -p /home/opensilex/logs
```

## Download & extract the OpenSILEX production release

Please download the OpenSILEX latest release archive
on [GitHub](https://github.com/OpenSILEX/opensilex/releases/latest).

In this paragraph, `<X.Y.Z>` corresponds to the specific OpenSILEX release version.
Extract the downloaded ZIP file into `/home/opensilex/bin`

> Linux example commands:

```
cd /home/opensilex/bin
wget https://github.com/OpenSILEX/opensilex/releases/download/X.Y.Z/opensilex-X.Y.Z.zip
unzip opensilex-X.Y.Z.zip
```

For the latest version - go to [Latest version](https://github.com/OpenSILEX/opensilex/releases/latest)

Example with version 1.1.1:

```bash
cd /home/opensilex/bin
wget https://github.com/OpenSILEX/opensilex/releases/download/1.1.1/opensilex-release-1.1.1.zip
unzip opensilex-release-1.1.1.zip
```

You should get the following directory structure:

```
/home/opensilex/
+-- bin/
    +-- <X.Y.Z>/
        +-- opensilex.jar
        +-- logback.xml
        +-- modules/
            +-- opensilex-core.jar
            +-- opensilex-front.jar
            +-- opensilex-fs.jar
            +-- opensilex-nosql.jar
            +-- opensilex-security.jar
            +-- opensilex-sparql.jar
            +-- opensilex-phis.jar
+-- config/
+-- data/
+-- logs/
```

## Configuration

### Create main configuration file

Create a YML file in `/home/opensilex/config` named `opensilex.yml` for example.
If you want to work with multiple configurations you can name this file differently.

Here is a minimal configuration example, containing all values which must be adapted to your setup:

```yml
ontologies:
  baseURI: http://www.opensilex.org/
  baseURIAlias: os
  sparql:
    config:
      serverURI: http://localhost:8080/rdf4j-server/
      repository: opensilex

file-system:
  fs:
    config:
      basePath: /home/opensilex/data

big-data:
  mongodb:
    config:
      host: localhost
      port: 27017
      database: opensilex
```

**N.B.** The ontologies OESO and OEEV are stored in [opensilex-core/src/main/resources/ontologies](https://forgemia.inra.fr/OpenSILEX/opensilex-dev/-/tree/master/opensilex-core/src/main/resources/ontologies). Other specific ontologies can be stored in each module.

### Configure logging

Edit the file `/home/opensilex/bin/<X.Y.Z>/logback.xml`

Set the path property to reflect your installation:

```
<property name="log.path" value="/home/opensilex/logs"/>
```

For Logging we use the Java Logback library in our application. Please read their [documentation](http://logback.qos.ch/manual/) to tailor its configuration to your needs.

By default, the logs will be printed to the console output and written into a rotating daily log file stored for 30 days.

## Initialize database and check configuration

### Create a script to access instructions

- Create

```
nano /home/opensilex/bin/<X.Y.Z>/opensilex.sh
```

- Content

```
#!/bin/bash

SCRIPT_DIR="$(dirname "$(readlink -f "$0")")"

CONFIG_FILE="/home/opensilex/config/opensilex.yml"

cd $SCRIPT_DIR

java -jar $SCRIPT_DIR/opensilex.jar --BASE_DIRECTORY=$SCRIPT_DIR --CONFIG_FILE=$CONFIG_FILE "$@"
```

- Activation

```
chmod +x /home/opensilex/bin/<X.Y.Z>/opensilex.sh
```

### Add an alias

- Edit

```
nano ~/.bash_aliases
```

- Content

```
alias opensilex="/home/opensilex/bin/<X.Y.Z>/opensilex.sh"
```

- Activation

```
source  ~/.bashrc
```

- Verification

```
opensilex help
```

### Initialize your triplestore

```bash
opensilex system install
```

This instruction creates the repository (with the name defined in the configuration file). It also creates the default administrator and imports the ontologies.

**GraphDB**

If you want to use GraphDB, you must update the following parts of your `opensilex.yml` config file:

```yml
ontologies:
  baseURI: http://opensilex.dev/
  baseURIAlias: os
  sparql:
    implementation: org.opensilex.sparql.rdf4j.graphdb.OntotextGraphDBServiceFactory
    config:
      serverURI: http://localhost:7200
      repository: opensilex
      repositoryType: graphdb:FreeSailRepository
```

**Note :** the value for the setting `repositoryType` depends on your GraphDB version (free, standard or entreprise).

You can follow these links for more details about repository configuration :

- https://graphdb.ontotext.com/documentation/9.11/standard/configuring-a-repository.html#configuration-parameters
- https://graphdb.ontotext.com/documentation/9.11/enterprise/configuring-a-repository.html#configuration-parameters
- https://graphdb.ontotext.com/documentation/9.11/free/configuring-a-repository.html#configuration-parameters

Now you can run

```bash
opensilex system install
```

**Import ontologies**

```bash
opensilex sparql reset-ontologies
```

**Create default administrator**

This instruction creates a user "admin@opensilex.org" with the password "admin"

```
opensilex user add --admin
```

More information about the command

```
opensilex user add --help
```

## Start openSilex

```
opensilex server start --host=192.168.178.31 --port=8081 --adminPort=4081
```

## Stop openSilex

```
opensilex server stop --host=192.168.178.31 --adminPort=4081
```

## Add a Reverse Proxy Nginx to redirect the application on port 80

Instructions

```
sudo apt install nginx
sudo nano /etc/nginx/sites-enabled/default
```

Content

```
        location / {
                #comment the following line to avoid an error and enable proxy
                #try_files $uri $uri/ =404;
                #add proxy settings
                include proxy_params;
                proxy_pass http://127.0.0.1:8081;
        }
```

Activation

```
sudo systemctl restart nginx
```
