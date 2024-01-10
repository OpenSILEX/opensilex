# Install OpenSilex for Development

- [Install OpenSilex for Development](#install-opensilex-for-development)
- [Introduction](#introduction)
- [Required pieces of software](#Required-pieces-of-software)
  - [Java](#java)
  - [Maven](#maven)
  - [Git](#git)
  - [Docker & Docker compose](#docker--docker-compose)

# Introduction

You can install OpenSILEX either for development or for production.   
This documentation focuses on installing a development environment.  
You should be able to run this environment on any operating system where the required software is available.
We have only tested it on Linux (Ubuntu & Debian) and on Windows 10.
If you simply want to try OpenSILEX please go to our website [www.opensilex.org](http://www.opensilex.org/)   
If you want to install it for production purposes please go the [dedicated documentation](./production)   

# Required pieces of software

Please follow the respective documentation of these pieces of software for your operating system.

## Java

Java is the language used for the backend, the project is automatically tested with JDK versions 11 and 14.

You will need a JDK installed: [Java JDK 11+](https://jdk.java.net/)

## Maven

Maven is a build system and a dependency manager for Java projects.

[Install Maven 3.5+](https://maven.apache.org/install.html)

## Git

Git is used for version control.

The code during development is hosted on [INRAE Forgemia](https://forgemia.inra.fr/OpenSILEX/opensilex-dev) and publicly available on [Github](https://github.com/OpenSILEX/opensilex).

[Install Git 2.17.1+](https://git-scm.com/book/en/v2/Getting-Started-Installing-Git)

## Docker & Docker compose

Docker and docker-compose are part of a virtualization container system used to run the required databases for development.

We DO NOT recommend the use of the provided development configuration in production without modification and security audit.

[Install docker 19.03.1+](https://docs.docker.com/install/)

[Install docker-compose 1.24.1+](https://docs.docker.com/compose/install/)
