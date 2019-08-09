OpenSILEX Development Installation
================================================================================

# 1. Pre-requestites

## Git

Download and install git (Minimal version 2.17) for your operating system
[git installation](https://git-scm.com/book/en/v2/Getting-Started-Installing-Git)

## Java

Download and install Java JDK (Minimal version 8) for your operating system.
 `<JAVA_BIN>` variable will be used in this document to indicate the path to java executable.
[Java installation](https://jdk.java.net/java-se-ri/8)

## Maven

Download and install Maven (Minimal version 3.5) for your operating system.
[Maven installation](https://maven.apache.org/download.cgi)


## Docker & Docker Compose

Download and install Docker (Minimal version 18.09) for your operating system.
[Docker installation](https://docs.docker.com/install/)

Download and install Docker Compose (Minimal version 1.17.1) for your operating system.
[Docker Compose installation](https://docs.docker.com/compose/install/)

# 2. Installation

## Get project sources

Go to the folder where you want to save sources (named `<BASE_DIR>` in this document) and type the following command:
`
git clone --recurse-submodules https://github.com/OpenSILEX/opensilex-dev.git
`

## Initialize databases

Use the following commands to setup databases with docker:
`
cd <BASE_DIR>/opensilex-dev/opensilex-dev-tools/src/main/docker
docker-compose up -d
`

## Install project dependencies

Use the following commands to install project dependencies:
`
cd <BASE_DIR>/opensilex-dev
mvn clean install
`

## Load initial data

Use the following commands to load initial data:
`
cd <BASE_DIR>/opensilex-dev/opensilex-dev-tools
mvn "-Dexec.args=-classpath %classpath org.opensilex.dev.Install" -Dexec.executable=<JAVA_BIN> org.codehaus.mojo:exec-maven-plugin:1.5.0:exec
`

## Start server

Use the following commands to start OpenSILEX server:
`
cd <BASE_DIR>/opensilex-dev/opensilex-dev-tools
mvn "-Dexec.args=-classpath %classpath org.opensilex.dev.StartServer" -Dexec.executable=<JAVA_BIN> org.codehaus.mojo:exec-maven-plugin:1.5.0:exec
`

# 3. Usage with IDE

## NetBeans



## Eclipse