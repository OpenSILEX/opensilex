# Install OpenSilex for Development

- [Install OpenSilex for Development](#install-opensilex-for-development)
- [Introduction](#introduction)
- [Required softwares](#required-softwares)
  - [Java](#java)
  - [Maven](#maven)
  - [Git](#git)
  - [Docker & Docker compose](#docker--docker-compose)

# Introduction

You can install OpenSILEX either for development and for production.
This documentation focus on installing a developement environment.
You should be able to run this environment on any operating system where the required software are available.
We only tried it on Linux (Ubuntu & Debian) and on Windows 10.
If you simply want to try OpenSILEX please go to our website [www.opensilex.org](http://www.opensilex.org/)
If you want to install it in a production please go the [dedicated documentation](./production)

# Required softwares

Please follow the respective documentation of these softwares for your operating system:

## Java

Java is our development language, the project is automatically tested with JDK versions 8, 11, 13 and 14.

You need a JDK installed: [Java JDK 8+](https://jdk.java.net/)

## Maven

Maven is a build system and a dependency manager for Java projects.

[Install Maven 3.5+](https://maven.apache.org/install.html)

For more information on Maven, please read our [documentation](../tools/maven)

## Git

Git is our version control system.

The code during development is hosted on [INRA Forgemia](https://forgemia.inra.fr/OpenSILEX/opensilex) and publicly available on [Github](https://github.com/OpenSILEX/opensilex-dev).

[Install Git 2.17.1+](https://git-scm.com/book/en/v2/Getting-Started-Installing-Git)

## Docker & Docker compose

Docker and docker-compose are a virtualization container system used to run the required databases for development.

We DO NOT recommend to use the provided development configuration in production without modification and security audit.

[Install docker 19.03.1+](https://docs.docker.com/install/)

[Install docker-compose 1.24.1+](https://docs.docker.com/compose/install/)
