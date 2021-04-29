Install OpenSILEX in production
===============================

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

```
sudo docker run -d eclipse/rdf4j-workbench:amd64-3.4.4
```

### GraphDB

Ontotext - GraphDB exists in a free version and can be used as an alternative for RDF4J.

Please follow their [documentation](http://graphdb.ontotext.com/documentation/free/installation.html) to install it.

You can also use this docker image: [ontotext/graphdb](https://hub.docker.com/r/ontotext/graphdb)

Linux example command:

```
sudo docker run -d ontotext/graphdb:9.1.1-se
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

Please download the OpenSILEX latest release archive on [Github](https://github.com/OpenSILEX/opensilex/releases)
In this paragraph, `<X.Y.Z>` means the OpenSILEX release version.
Extract the downloaded zip file into ```/home/opensilex/bin```

Linux example commands:

```
cd /home/opensilex/bin
wget https://github.com/OpenSILEX/opensilex/releases/download/X.Y.Z/opensilex-X.Y.Z.zip
unzip opensilex-X.Y.Z.zip

```
For latest version
```
cd /home/opensilex/bin
wget https://github.com/OpenSILEX/opensilex/releases/download/1.0.0-beta/opensilex-release-1.0.0-beta.zip
unzip opensilex-release-1.0.0-beta.zip
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

### Configure logging

Edit the file ```/home/opensilex/bin/<X.Y.Z>/logback.xml```

Set the path property to reflect your installation:

```                
<property name="log.path" value="/home/opensilex/logs"/>
```

We use Java Logback library for logging in our application, please read their [documentation](http://logback.qos.ch/manual/) to configure it.

By default logs will be printed to the console output and writen into a rotating daily log file stored for 30 days.


## Initialize database and check configuration

## Initialize sparql repositories

```
opensilex sparql reset-ontologies
```

## Create a script to access instructions

- Create
```
nano ~/opensilex/bin/<X.Y.Z>/opensilex.sh
```

- Content
```
#!/bin/bash

SCRIPT_DIR="$(dirname "$(readlink -f "$0")")"

cd $SCRIPT_DIR

java -jar $SCRIPT_DIR/opensilex.jar --BASE_DIRECTORY=$SCRIPT_DIR/bin --CONFIG_FILE="$SCRIPT_DIR/config/opensilex.yml" "$@"
```

- Activation
```
chmod +x ~/opensilex/bin/<X.Y.Z>/opensilex.sh
```

## Add an alias

- Edit 
```
nano ~/.bash_aliases
```

- Content
```
alias opensilex="~/opensilex/bin/<X.Y.Z>/opensilex.sh"
```

- Activation
```
source  ~/.bashrc
```

- Verification
```
opensilex help
```

## Create default administrator

This instruction create user "admin@opensilex.org" with the password "admin"

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
