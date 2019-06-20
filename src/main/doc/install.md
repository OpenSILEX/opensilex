OpenSilex Developper's Installation documentation
=================================================

# Pre-requesite

## Build softwares

First you need to have these software installed :

- [Java JDK 8](https://jdk.java.net/java-se-ri/8)
- [Maven 3.6.1](https://maven.apache.org/install.html)
- [Git 2.17.1](https://git-scm.com/book/en/v2/Getting-Started-Installing-Git)

The install process is tested with these versions of the software.

You should create also a directory where you have full read/write permissions which will be referenced as ```<BASE_DIR>``` in this documentation.

## Databases

### Install with docker

First install docker and docker-compose

- [docker installation](https://docs.docker.com/install/)
- [docker-compose installation](https://docs.docker.com/compose/install/)

Then download OpenSILEX docker repository and start images (the last command may be run with sudo) :

```
cd <BASE_DIR>
git clone https://github.com/OpenSILEX/opensilex-docker.git
cd opensilex-docker
docker-compose up -d
```

You now should have a running RDF4J and MongoDB servers.

### Install manually

Install RDF4J and MongoDB:

- [RDF4J](https://rdf4j.eclipse.org/documentation/server-workbench-console/)
- [MongoDB](https://docs.mongodb.com/manual/installation/)

Once MongoDB is installed you must [enable Replica Set feature](https://docs.mongodb.com/manual/tutorial/deploy-replica-set/)

# Download sources

```
cd <BASE_DIR>
git clone --recurse-submodules https://github.com/OpenSILEX/opensilex-dev.git
```

# Build project

```
cd <BASE_DIR>
cd opensilex-dev
mvn clean install
```

# Initialize configuration

// TODO 

This command will initialize a configuration file in ```<BASE_DIR>/opensilex-dev/opensilex-debug/src/main/config/localhost.yaml```:

```
cd <BASE_DIR>
cd opensilex-dev/opensilex-debug
mvn exec:exec "-Dexec.args=-classpath %classpath debug.opensilex.InitConfig" org.codehaus.mojo:exec-maven-plugin:1.6.0:exec
```

If you used the docker installation for databases you shoudn't have anything to change in the configuration file.
Otherwise you shoud adjust the parameters to fit your configuration.

# Start OpenSILEX

// TODO

Run the following command to start OpenSILEX server:

```
cd <BASE_DIR>
cd opensilex-dev/opensilex-debug
mvn exec:exec "-Dexec.args=-classpath %classpath debug.opensilex.Main" -Dexec.executable=java org.codehaus.mojo:exec-maven-plugin:1.6.0:exec
```

If you use the default configuration, you can now access the OpenSILEX API on: [http://localhost:8666/](http://localhost:8666/)
