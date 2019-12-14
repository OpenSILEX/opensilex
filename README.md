OpenSILEX Developper's installation
=======================================================

This repository contains source code for Phenotyping Hybrid Information System (PHIS) as an OpenSILEX instance

# Pre-requesite softwares

First you need to have these software installed :

- [Java JDK 8](https://jdk.java.net/java-se-ri/8)
- [Maven 3.6.2](https://maven.apache.org/install.html)
- [Git 2.17.1](https://git-scm.com/book/en/v2/Getting-Started-Installing-Git)
- [docker 19.03.5](https://docs.docker.com/install/)
- [docker-compose 1.24.1](https://docs.docker.com/compose/install/)

The install process is tested with these software versions but it should work with newer versions.

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

# Setup configuration

Edit ```<BASE_DIR>/opensilex-dev/opensilex-dev-tools/src/main/resources/config/opensilex.yml```

Be careful if you change host and port of databases as you will have to update docker-compose configuration file accordingly.

Be sure to configure properly read and write rights for your user on configured folders.

The only mandatory options to setup are:
- opensilex.storageBasePath: Base directory for file storage
- phisws.uploadFileServerUsername: Name of your Linux user running the application
- phisws.uploadFileServerPassword: Password of your Linux user running the application

For usage with phis-webapp you should also configure these options if your app is not installed in /var/www/html:
- layerFileServerDirectory: Folder to store layer files used by phis-webapp
- layerFileServerAddress: Base uri for accessing layerFileServerDirectory folder through apache
- uploadImageServerDirectory: Folder to store images files used by phis-webapp
- imageFileServerDirectory:  Base uri for accessing uploadImageServerDirectory folder through apache

# Install Databases with docker

```
cd <BASE_DIR>
cd opensilex-dev/opensilex-dev-tools/src/main/resources/docker
sudo docker-compose up -d
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
cd <BASE_DIR>
cd opensilex-dev/opensilex-dev-tools
mvn "-Dexec.args=-classpath %classpath:../opensilex:../opensilex-core:../opensilex-front:../phis-ws/phis2-ws org.opensilex.dev.Install" -Dexec.executable=/usr/bin/java  org.codehaus.mojo:exec-maven-plugin:1.6.0:exec
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

# Other maven build profiles and options

## Skip unit and integration tests

Avoid to do it, but it could be usefull if you need to rebuild very often at some point.

```
mvn install -DskipTests=true
```

## Skip Vue js build parts

This could be usefull if you work only on web services to speed up build.

```
mvn install -DskipFrontBuild=true
```

## Generate documentation

Make sure the environment variable ```JAVA_HOME``` is defined.

```
cd <BASE_DIR>/opensilex-dev
mvn site -Pwith-test-report
```

## Generate documentation with security report audit

This buid may be very very very long because it as to download a lot of stuff from internet (OWASP report).

You should better configure it periodicaly on a automated platform.

```
mvn site -Pwith-test-report -Pwith-security-check
```

## Special profile for eclipse

If you are using eclipse you may need to enable this profile to avoid build errors
```
mvn install -Pfor-eclipse
```
