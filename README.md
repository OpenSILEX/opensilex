OpenSILEX Developper's installation
=======================================================

This repository contains source code for Phenotyping Hybrid Information System (PHIS) as an OpenSILEX instance

# Pre-requesite softwares

First you need to have these software installed :

- [Java JDK 8+](https://jdk.java.net/) (Our project is tested with JDK versions 8, 11, 13 and 14)
- [Maven 3.5+](https://maven.apache.org/install.html)
- [Git 2.17.1+](https://git-scm.com/book/en/v2/Getting-Started-Installing-Git)
- [docker 19.03.1+](https://docs.docker.com/install/)
- [docker-compose 1.24.1+](https://docs.docker.com/compose/install/)

Note: the ```<BASE_DIR>``` variable referenced in this documentation is the root folder of your installation whee your user must have read and write permissions.

# Check your installated softwares

Following commands should work from everywhere in your system without errors:

```java -version```

```mvn --version```

```git --version```

```docker --version```

```docker-compose --version```


# Download sources

```
cd <BASE_DIR>
git clone --recurse-submodules https://github.com/OpenSILEX/opensilex-dev.git
cd opensilex-dev/phis-ws
git pull origin modularity
cd ..
```

# Build project

```
cd <BASE_DIR>/opensilex-dev
mvn install
```

# Create opensilex command alias

## Linux

Create file `.bash_aliases` in your home folder if it doesn't exists.

Add this line in it replacing <BASE_DIR> variable:

```
alias opensilex=<BASE_DIR>/opensilex-dev/opensilex-release/target/opensilex/opensilex.sh
```

Reload bash aliases and test it:
```
cd ~
source .bash_aliases
opensilex help
```

## Windows

In a console terminal type the following command replacing <BASE_DIR> variable:

```
setx PATH "%PATH%;<BASE_DIR>\opensilex-dev\opensilex-release\target\opensilex\"
opensilex help
```

# Setup configuration

Edit ```<BASE_DIR>/opensilex-dev/opensilex-dev-tools/src/main/resources/config/opensilex.yml```

Be careful if you change host and port of databases as you will have to update docker-compose configuration file accordingly.

Be sure to configure properly read and write rights for your user on configured folders.

The only mandatory options to setup are:
- file-system.storageBasePath: Base directory for file storage
- phisws.uploadFileServerUsername: Name of your Linux user running the application
- phisws.uploadFileServerPassword: Password of your Linux user running the application

For usage with phis-webapp you should also configure these options if your app is not installed in /var/www/html:
- layerFileServerDirectory: Folder to store layer files used by phis-webapp
- layerFileServerAddress: Base uri for accessing layerFileServerDirectory folder through apache
- uploadImageServerDirectory: Folder to store images files used by phis-webapp
- imageFileServerDirectory:  Base uri for accessing uploadImageServerDirectory folder through apache

# Install Databases with docker

First you should add your current user `$USER` to docker group:
```
sudo usermod -aG docker $USER
```

You need to log out and log in again to make it work.

```
cd <BASE_DIR>
cd opensilex-dev/opensilex-dev-tools/src/main/resources/docker
docker-compose up -d
```

Docker containers will be automatically started on your machine startup.

You can change the "restart" parameter in "docker-compose.yml" file if you don't want this behavior 
but you will have to run the preious command after each restart manually.

# Initialize system data

## With Netbeans

Right-click on opensilex-dev project and select "Open Required Projects" --> "Open All Projects"

Then right-click on ```org.opensilex.dev.Install``` class in opensilex-dev-tools projet and select "run" or "debug"

## With command line

```
opensilex dev install
```

# Start OpenSILEX development server with Netbeans

## For web services only

Right-click on ```org.opensilex.dev.StartServer``` class in opensilex-dev-tools projet and select "run" or "debug"

## For webservices and Vue.js hot reload server

Right-click on ```org.opensilex.dev.StartServerWithFront``` class in opensilex-dev-tools projet and select "run" or "debug"


# Access to OpenSilex & tools

If you use the default configuration, you can now access the OpenSILEX API at: [http://localhost:8666/](http://localhost:8666/)

If you start server with Vue.js, the hot reload server tell you on which port it's accessible.

Default Super Admin user which will give you access to all web services is created with:
- login: admin@opensilex.org
- password: admin 

RDF4J workbench is accessible by default at: [http://localhost:8667/rdf4j-workbench](http://localhost:8667/rdf4j-workbench)

MongoDB is accessible on port 8669

PGAdmin is accessible by default at: [http://localhost:8670/](http://localhost:8670/)
- PGAdmin default user: admin@opensilex.org
- PGAdmin default password: opensilex

# Generate documentation

```
cd <BASE_DIR>
mvn site -Pwith-test-report
```

All documentation will be available in <BASE_DIR>/opensilex-doc/src/main/doc folder.

# Other maven build profiles and options

## Generate release

```
mvn install -Drevision=X.Y.Z
```

Zip for revision will be available in <BASE_DIR>/opensilex-release/target/opensilex-release-X.Y.Z.zip

Uncompressed version is available in  <BASE_DIR>/opensilex-release/target/opensilex-release-X.Y.Z/

## Skip unit and integration tests

Avoid to do it, but it could be usefull if you need to rebuild very often at some point.

```
mvn install -DskipTests=true
```

## Skip Vue js build parts

This could be usefull if you work only on web services to speed up build.

```
mvn install -DskipFrontBuild
```

## Generate documentation

Make sure the environment variable ```JAVA_HOME``` is defined.

```
cd <BASE_DIR>/opensilex-dev
mvn site -Pwith-test-report -DskipFrontBuild
```

## Generate documentation with security report audit

This buid may be very very very long because it as to download a lot of stuff from internet (OWASP report).

You should better configure it periodicaly on a automated platform.

```
mvn site -Pwith-test-report -Pwith-security-check -DskipFrontBuild
```

## Check javascript security issues

```
mvn verify -DskipFrontAudit=false
```

## Special profile for eclipse

If you are using eclipse you may need to enable this profile to avoid build errors
```
mvn install -Pfor-eclipse
```
