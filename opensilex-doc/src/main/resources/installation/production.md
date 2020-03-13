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
sudo docker run -d eclipse/rdf4j-workbench:amd64-3.1.1
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

```sudo useradd -s /bin/bash -d /home/opensilex/ -m opensilex```

Set up a password to your new user:

```sudo passwd opensilex```

Give sudo permissions to this user :

```sudo usermod -a -G sudo opensilex```

Connect with this user and the defined password:

```su - opensilex```

## Create directories

Directory for OpenSILEX binaries:
```mkdir -p /home/opensilex/bin```

Directory for OpenSILEX configuration file:
```mkdir -p /home/opensilex/config```

Directory for OpenSILEX data file storage:
```mkdir -p /home/opensilex/data```

Directory for OpenSILEX file logs:
```mkdir -p /home/opensilex/logs```

## Download & extract OpenSILEX production release

Please download the OpenSILEX latest release archive on [Github](https://github.com/OpenSILEX/opensilex-dev/releases)

In this paragraph, `<X.Y.Z>` means the OpenSILEX release version.

Extract the downloaded zip file into ```/home/opensilex/bin```

Linux example commands:

```
cd /home/opensilex/bin
wget https://github.com/OpenSILEX/opensilex-dev/releases/download/4.0.0-beta/opensilex-dev-X.Y.Z.zip
unzip opensilex-dev-X.Y.Z.zip
```
For latest version
```
cd /home/opensilex/bin
wget https://github.com/OpenSILEX/opensilex-dev/releases/download/4.0.0-beta/opensilex-dev-4.0.0-beta.zip
unzip opensilex-dev-4.0.0-beta.zip
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
            +-- opensilex-rest.jar
            +-- opensilex-sparql.jar
            +-- phis2ws.jar
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
    # Base URI domain for every generated URI by this OpenSILEX instance, 
    # it should be an URI representing your organisation (but not necessary a "real" URL pointing to a website)
    baseURI: http://www.opensilex.org/

    # Base URI domain alias for short URI
    baseURIAlias: os

    # Triple store configuration
    sparql:
        rdf4j:
            # Connection URI to RDF4J or GraphDB instance (they use the same API)
            # please adjust it to your local installation
            serverURI: http://localhost:8080/rdf4j-server/

            # Triple store repository name
            repository: opensilex

file-system: 
    # Path to root file data storage directory
    storageBasePath: /home/opensilex/data

big-data:
    nosql:
        # MongoDB connection configuration
        mongodb:
            # MongoDB server host
            host: localhost

            # MongoDB server port
            port: 27017

            # MongoDB database name
            database: opensilex

phisws:
    infrastructure: opensilex
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


### Initialize database
```
cd /home/opensilex/bin/<X.Y.Z>/

echo "Initialize databases structures"
java -jar opensilex.jar --CONFIG_FILE=/home/opensilex/config/opensilex.yml system install

echo "Create main admin user"
java -jar opensilex.jar --CONFIG_FILE=/home/opensilex/config/opensilex.yml user add --admin --email=<admin@opensilex.org> --firstName=<admin> --lastName=<admin>
```

### Check configuration


```
cd /home/opensilex/bin/<X.Y.Z>/

java -jar opensilex.jar --CONFIG_FILE=/home/opensilex/config/opensilex.yml system check
```

## Start / Stop server

### Start server

```
cd /home/opensilex/bin/<X.Y.Z>/

java -jar opensilex.jar --CONFIG_FILE=/home/opensilex/config/opensilex.yml server start --host=<localhost> --port=<8666> --adminPort=<8888> [--daemon] 
```


### Stop server

```
cd /home/opensilex/bin/<X.Y.Z>/

java -jar opensilex.jar --CONFIG_FILE=/home/opensilex/config/opensilex.yml server stop --host=<localhost> --adminPort=<8888>
```

## Install PHP Interface

### Install Apache and PHP

```
sudo apt update
sudo apt install curl apache2 php libapache2-mod-php
sudo systemctl restart apache2
```

### Get PHP application sources

```
cd /home/opensilex
git clone https://github.com/OpenSILEX/phis-webapp.git
cd phis-webapp
git checkout hybrid
```

### Install Composer

Please follow their [documentation](https://getcomposer.org/download/)

It should be something like:

```
cd /home/opensilex/phis-webapp

php -r "copy('https://getcomposer.org/installer', 'composer-setup.php');"
php -r "if (hash_file('sha384', 'composer-setup.php') === 'e0012edf3e80b6978849f5eff0d4b4e4c79ff1609dd1e613307e16318854d24ae64f26d17af3ef0bf7cfb710ca74755a') { echo 'Installer verified'; } else { echo 'Installer corrupt'; unlink('composer-setup.php'); } echo PHP_EOL;"
php composer-setup.php
php -r "unlink('composer-setup.php');"
```

### Install libraries

```
cd /home/opensilex/phis-webapp
php composer.phar install --ignore-platform-reqs
```

### Give user permissions

In this section we assume that your apache server is running with user in group `www-data`

```
echo "Add user opensilex to group www-data"
sudo usermod -a -G www-data opensilex
echo "Set group for all path to phis-webapp sources"
sudo chgrp www-data /home
sudo chgrp www-data /home/opensilex
sudo chgrp -R www-data /home/opensilex/phis-webapp
echo "Allow group to read and write file in phis-webapp directory"
sudo chmod -R g+srw /home/opensilex/phis-webapp
```

### Set up Web app configuration

You may have to change configuration file `/home/opensilex/phis-webapp/config/config.php`

Set the `$hostname` variable to adjust it to your configuration

You also may have to change host and port configuration in file `/home/opensilex/phis-webapp/config/web_services.php`

```
define('WS_PHIS_PATH', 'http://localhost:8666/rest/');
define('WS_PHIS_PATH_DOC', 'http://localhost:8666/api-docs');
define('WS_PHIS_APP_PATH', 'http://localhost:8666/app/');
```

### Set up Apache configuration

For a simple setup, just create a symbolic link to sources folder

```
sudo ln -s /home/opensilex/phis-webapp /var/www/html/
sudo chown opensilex:www-data /var/www/html/phis-webapp
```

For a more complexe setup please follow [Apache HTTP Server documentation](http://httpd.apache.org/docs/current/)

You can also setup HTTPS for Apache by example using [Let's Encrypt](https://letsencrypt.org/fr/)

## Check OpenSILEX Interface

Normally you should now be able to access to OpenSILEX interfaces with the following URL (adjusted to your setup):

- Website: http://localhost/phis-webapp
- Web services API: http://localhost:8666/api-docs

You should be able to connect with the admin user you create previously.

If not, please check that your OpenSILEX server is running and your PHP configuration.
