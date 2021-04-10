OpenSILEX Developper's installation
=======================================================

This repository contains source code for Phenotyping Hybrid Information System (PHIS) as an OpenSILEX instance

# Pre-requesite softwares

First you need to have these software installed :

- [Java JDK 9+](https://jdk.java.net/) (Our project is tested with JDK versions 8, 9, 11, 13 and 14)
- [Maven 3.5+](https://maven.apache.org/install.html)
- [Git 2.17.1+](https://git-scm.com/book/en/v2/Getting-Started-Installing-Git)
- [docker 19.03.1+](https://docs.docker.com/install/)
- [docker-compose 1.24.1+](https://docs.docker.com/compose/install/)

Maybe you need to install npm package

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
git clone https://github.com/OpenSILEX/opensilex.git
cd opensilex
```

# Build project

```
cd <BASE_DIR>/opensilex
mvn install
```

# Create opensilex command alias

## Linux

Create file `.bash_aliases` in your home folder if it doesn't exists.

Add this line in it replacing <BASE_DIR> variable:

```
alias opensilex=<BASE_DIR>/opensilex/opensilex-release/target/opensilex/opensilex.sh
```

Reload bash aliases and test it:
```
cd ~
source .bash_aliases
opensilex help
```

## Windows

Add the following directory to your PATH environment variable replacing <BASE_DIR> variable:

```
path %PATH%;<BASE_DIR>\opensilex\opensilex-release\target\opensilex\
opensilex help
```

# Setup configuration

Edit ```<BASE_DIR>/opensilex/opensilex-dev-tools/src/main/resources/config/opensilex.yml```

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

# Setup Databases with docker

On linux, you should add your current user `<USER>` to docker group to avoid using sudo:
```
sudo usermod -aG docker <USER>
```

You need to log out and log in again to make it work.

```
cd <BASE_DIR>
cd opensilex/opensilex-dev-tools/src/main/resources/docker
docker-compose up -d
```

Docker containers will be automatically started on your machine startup.

You can change the "restart" parameter in "docker-compose.yml" file if you don't want this behavior 
but you will have to run the preious command after each restart manually.


# Initialize system data

## With Netbeans

Right-click on opensilex project and select "Open Required Projects" --> "Open All Projects"

Then right-click on ```org.opensilex.dev.Install``` class in opensilex-dev-tools projet and select "run" or "debug"

If you want to reset all your database, you can do the same with class ```org.opensilex.dev.InstallReset```

## With command line

```
opensilex dev install
```

If you want to reset all your database use `-r` flag with the previous command


# Start OpenSILEX development server with Netbeans

## For web services only (with compiled Vue.js code)

Right-click on ```org.opensilex.dev.StartServer``` class in opensilex-dev-tools projet and select "run" or "debug"

## For webservices and Vue.js hot reload server

Right-click on ```org.opensilex.dev.StartServerWithFront``` class in opensilex-dev-tools projet and select "run" or "debug"

# Start OpenSILEX development server with command line

With command line you need to recompile all modules to have your change in Java source code taking into account (but not for Vue-JS code if using hot relaod).

## For web services only (with compiled Vue.js code)

```
opensilex dev start --no-front-dev
```

## For webservices and Vue.js hot reload server

```
opensilex dev start
```


# Access to OpenSilex & tools

## OpenSilex Appllication

- OpenSilex Vue App is accessible at: [http://localhost:8666/](http://localhost:8666/)

- OpenSILEX API is accessible at: [http://localhost:8666/api-docs](http://localhost:8666/api-docs)

If you start server with Vue.js, the hot reload server tell you on which port it's accessible (probably 8080 depending of which is available).

Default Super Admin user which will give you access to all web services is created with:
- login: admin@opensilex.org
- password: admin 

## RDF4J workbench

RDF4J workbench is accessible by default at: [http://localhost:8667/rdf4j-workbench](http://localhost:8667/rdf4j-workbench)

At first connection, you must configure RDF4J server URL with this value: `http://opensilex-rdf4j:8080/rdf4j-server`

## MongoDB

MongoDB is accessible on port 8668

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
cd <BASE_DIR>/opensilex
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
