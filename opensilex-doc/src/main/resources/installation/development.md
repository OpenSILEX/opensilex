# Install OpenSilex for Development

- [Install OpenSilex for Development](#install-opensilex-for-development)
- [Introduction](#introduction)
- [Required pieces of software](#required-pieces-of-software)
  - [Install Java and maven](#install-java-and-maven)
    - [Install maven and java using sdkman](#install-maven-and-java-using-sdkman)
    - [Basic installation whitout skdman](#basic-installation-whitout-skdman)
      - [Java](#java)
      - [Maven](#maven)
  - [Git](#git)
  - [Docker \& Docker compose](#docker--docker-compose)

# Introduction

You can install OpenSILEX either for development or for production.   
This documentation focuses on installing a development environment.  
You should be able to run this environment on any operating system where the required software is available.
We have only tested it on Linux (Ubuntu & Debian) and on Windows 10.
If you simply want to try OpenSILEX please go to our website [www.opensilex.org](http://www.opensilex.org/)   
If you want to install it for production purposes please go the [dedicated documentation](./production)   

# Required pieces of software

Please follow the respective documentation of these pieces of software for your operating system.

## Install Java and maven

### Install maven and java using sdkman

[SDKMAN](https://sdkman.io/) has become an essential tool for developers, simplifying the complex process of managing development environments while maintaining flexibility and control over different SDK versions. Its user-friendly interface and comprehensive feature set make it an ideal solution for both beginners and experienced developers.

```bash
# Install SDKMAN
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"

# Install Java
sdk install java 17.0.14-tem

# Installing: java 17.0.14-tem
# Done installing!

# Do you want java 17.0.14-tem to be set as default? (Y/n): Y

# Setting java 17.0.14-tem as default.

# Install Maven
sdk install maven 3.9.9

#Installing: maven 3.9.9
#Done installing!

#Do you want maven 3.9.9 to be set as default? (Y/n): Y

#Setting maven 3.9.9 as default. 

# Switch between versions
# sdk use java 21.0.6-tem
# sdk use maven 3.9.9
```

### Basic installation whitout skdman

If you have already installed sdkman, you don't need to follow these steps. Go to Git installation step. 

#### Java

Java is the language used for the backend, the project is automatically tested with JDK versions 17 and will be tested with Java 21.

You will need a JDK installed: [Java JDK 17](https://jdk.java.net/)

#### Maven

Maven is a build system and a dependency manager for Java projects.

[Install Maven 3.9.9](https://maven.apache.org/install.html)


## Git

Git is used for version control.

The code during development is hosted on [INRAE Forgemia](https://forgemia.inra.fr/OpenSILEX/opensilex-dev) and publicly available on [Github](https://github.com/OpenSILEX/opensilex).

[Install Git 2.40.1+](https://git-scm.com/book/en/v2/Getting-Started-Installing-Git)

## Docker & Docker compose

Docker and docker compose are part of a virtualization container system used to run the required databases for development.

We DO NOT recommend the use of the provided development configuration in production without modification and security audit.

[Install docker 27.1.1+](https://docs.docker.com/engine/install/)

[Install docker-compose 2.33.1+](https://docs.docker.com/compose/install/)
