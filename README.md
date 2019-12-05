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
```java --version```
```mvn --version```
```git --version```
```docker --version```
```docker-compose --version```

# Download sources

```
cd <BASE_DIR>
git clone --recurse-submodules https://github.com/vincentmigot/opensilex-dev.git
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

For usage with phis-webapp you should also configure these options (if your app is not installed in /var/www/html:
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

# Initialize system data

## By command line
```
cd <BASE_DIR>
cd opensilex-dev/opensilex-dev-tools
mvn exec:exec "-Dexec.args=-classpath %classpath org.opensilex.dev.Install" -Dexec.executable=java org.codehaus.mojo:exec-maven-plugin:1.6.0:exec
```

## With Netbeans

Just right-click on org.opensilex.dev.Install class in opensilex-dev-tools projet and select "run"

# Start OpenSILEX

## By command line

```
cd <BASE_DIR>
cd opensilex-dev/opensilex-debug
mvn exec:exec "-Dexec.args=-classpath %classpath org.opensilex.dev.StartServer" -Dexec.executable=java org.codehaus.mojo:exec-maven-plugin:1.6.0:exec
```

## With Netbeans

Just right-click on org.opensilex.dev.StartServer class in opensilex-dev-tools projet and select "run" or "debug"

# Start OpenSILEX with Vue.js Application

## By command line

```
cd <BASE_DIR>
cd opensilex-dev/opensilex-debug
mvn exec:exec "-Dexec.args=-classpath %classpath org.opensilex.dev.StartServerWithFront" -Dexec.executable=java org.codehaus.mojo:exec-maven-plugin:1.6.0:exec
```

## With Netbeans

Just right-click on org.opensilex.dev.StartServerWithFront class in opensilex-dev-tools projet and select "run" or "debug"

# Access to OpenSilex

If you use the default configuration, you can now access the OpenSILEX API at: [http://localhost:8666/](http://localhost:8666/)
If you start server with Vue.js, you can access the main application at: [http://localhost:8666/app](http://localhost:8666/app)

Default Super Admin user is created with login: ```admin@opensilex.org`` and password: ```admin``` which will give you access to all web services


