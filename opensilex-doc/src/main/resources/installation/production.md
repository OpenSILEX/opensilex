# Install OpenSILEX in production

- [Install OpenSILEX in production](#install-opensilex-in-production)
- [Pre-requesites](#pre-requesites)
  - [Operating System](#operating-system)
  - [Java](#java)
  - [Set up MongoDB](#set-up-mongodb)
  - [Set up a triplestore](#set-up-a-triplestore)
    - [RDF4J](#rdf4j)
    - [GraphDB](#graphdb)
- [Installation](#installation)
  - [Set up a user](#set-up-a-user)
  - [Create directories](#create-directories)
  - [Download \& extract OpenSILEX production release](#download--extract-opensilex-production-release)
  - [Configuration](#configuration)
    - [Create main configuration file](#create-main-configuration-file)
    - [Configure logging](#configure-logging)
  - [Initialize database and check configuration](#initialize-database-and-check-configuration)
    - [Create a script to access instructions](#create-a-script-to-access-instructions)
    - [Add an alias](#add-an-alias)
    - [Initialize your triplestore](#initialize-your-triplestore)
  - [Start openSilex](#start-opensilex)
  - [Stop openSilex](#stop-opensilex)
  - [Add a Reverse Proxy Nginx to redirect application on port 80](#add-a-reverse-proxy-nginx-to-redirect-application-on-port-80)

# Pre-requesites

## Operating System

OpenSILEX should work on any system where the required softwares are available but we recommend using Linux for production installation.

Commands in this document should work on any Debian-like distribution (with sudo configured) but should be easily adapted for any Linux distributions

## Java

You need at least [Java JDK 8+](https://jdk.java.net/) installed on the server operating system.

You can install it on linux with the following command:

```
sudo apt install openjdk-11-jdk
```

You can check java installation and version with the following command:

```
java --version
```

## Set up MongoDB

Please follow MongoDB official install documentation for your operating system.

[Install MongoDB 4.2+](https://docs.mongodb.com/manual/installation/)

/!\ CAUTION /!\

You must start a MongoDB Replica Set to allow transaction usage with MongoDB.

So you need to start MongoDB daemon with option `--replSet opensilex`.

`opensilex` meaning the name of your replica set and it can be changed.

Look to this [documentation](https://docs.mongodb.com/manual/tutorial/deploy-replica-set/#considerations-when-deploying-a-replica-set) for more information about replica set.

You can also use this docker image: [mongo](https://hub.docker.com/_/mongo)

Linux example command:

```
sudo docker run -d mongo:4.2.3-bionic --replSet opensilex
```

## Set up a triplestore

You can use [RDF4J](https://rdf4j.org/) or [GraphDB](http://graphdb.ontotext.com/) for storing semantic data.

You can find [here](https://db-engines.com/en/system/GraphDB%3BRDF4J) a quick comparison of both triplestores engine.

Please refer to their websites for more information.

### RDF4J

For RDF4J you first need to set up and configure a Servlet Container server like [Tomcat](http://tomcat.apache.org/).

And then follow their [documentation](https://rdf4j.org/documentation/server-workbench-console/) .

You can also use this docker image: [eclipse/rdf4j-workbench](https://hub.docker.com/r/eclipse/rdf4j-workbench)

Linux example command:

```bash
sudo docker run -d eclipse/rdf4j-workbench
```

### GraphDB

Ontotext - GraphDB exists in a free version and can be used as an alternative for RDF4J.

Please follow their [documentation](http://graphdb.ontotext.com/documentation/free/installation.html) to install it.

You can also use this docker image: [ontotext/graphdb](https://hub.docker.com/r/ontotext/graphdb)

Linux example command:

```bash
sudo docker run -d ontotext/graphdb
```

# Installation

All directories and user names in this installation procedure can be changed but you need to change the configuration accordingly to make it work.

## Set up a user

Create a user with it's home directory:

```
sudo useradd -s /bin/bash -d /home/opensilex/ -m opensilex
```

Set up a password to your new user:

```
sudo passwd opensilex
```

Give sudo permissions to this user :

```
sudo usermod -a -G sudo opensilex
```

Connect with this user and the defined password:

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

## Download & extract OpenSILEX production release

Please download the OpenSILEX latest release archive on [Github](https://github.com/OpenSILEX/opensilex/releases/latest)
In this paragraph, `<X.Y.Z>` means the OpenSILEX release version.
Extract the downloaded zip file into `/home/opensilex/bin`

Linux example commands:

```
cd /home/opensilex/bin
wget https://github.com/OpenSILEX/opensilex/releases/download/X.Y.Z/opensilex-X.Y.Z.zip
unzip opensilex-X.Y.Z.zip

```

For latest version - go to [Latest version](https://github.com/OpenSILEX/opensilex/releases/latest)

Exemple with 1.0.0-rc+7 version

```bash
cd /home/opensilex/bin
wget https://github.com/OpenSILEX/opensilex/releases/download/1.0.0-rc+7/opensilex-release-1.0.0-rc+7.zip
unzip opensilex-release-1.0.0-rc+7.zip
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
It could be any name you want if you want to work with multiple configurations.

Here is a minimal example of configuration content, where all values must be adapted to your setup:

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

We use Java Logback library for logging in our application, please read their [documentation](http://logback.qos.ch/manual/) to configure it.

By default logs will be printed to the console output and writen into a rotating daily log file stored for 30 days.

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

If your want to use graphDB, you must update some part of your `opensilex.yml` config file as following :

```yml
ontologies:
  baseURI: http://opensilex.dev/
  baseURIAlias: test
  sparql:
    implementation: org.opensilex.sparql.rdf4j.graphdb.OntotextGraphDBServiceFactory
    config:
      serverURI: http://localhost:7200
      repository: opensilex
      repositoryType: graphdb:FreeSailRepository
```

Note the value for the setting `repositoryType` depends on your graphdb version (free, standard or entreprise).

You can see the following links for more details about repository configuration :

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

More about user

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

## Add a Reverse Proxy Nginx to redirect application on port 80

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
