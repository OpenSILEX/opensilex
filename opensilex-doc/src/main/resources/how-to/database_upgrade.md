---
title: RDF4J database upgrade
authors:
  - CHARLEROY Arnaud
  - COLIN Renaud
---

# Migration of databases

## RDF4J

- Install  ` curl and wget `
- RDF4J: Identify the current version of RDF4J and the target version for the upgrade.

### Dump rdf4j specific repository

Make sure to back up your current RDF4J installation and data.

```bash
SERVER_IP= # Ex: localhost:8080
RDF_REPOSITORY= # Ex: opensilex-db
RDF_DUMP_PATH=  # Ex /home/user/dbdump
wget http://${SERVER_IP}/rdf4j-workbench/repositories/${RDF_REPOSITORY}/export?Accept=application%2Ftrig -O ${RDF_DUMP_PATH}/dump_rdf4-j${RDF_REPOSITORY}
```

### Update RDF4J database

### With docker 

Go to docker [directory](../../../../../opensilex-dev-tools/src/main/resources/docker)

```bash
docker compose down rdf4j
docker compose up --no-deps --build rdf4j -d
```

### Without docker 

This example works with tomcat 9.

- Stop the Tomcat server to ensure a smooth upgrade process.
- Backup RDF4J. 
  - Copy the RDF4J data directory to a safe location. The data directory is typically found in `/var/lib/tomcat9/webapps/rdf4j-server/data`.
  - Backup the current RDF4J WAR files and directories. This can be done with the application manager gui.

In command line :

```bash
sudo cp /var/lib/tomcat9/webapps/rdf4j-server.war /path/to/backup/location
sudo cp /var/lib/tomcat9/webapps/rdf4j-workbench.war /path/to/backup/location
```
- Remove the Old RDF4J Web Applications This can be done with the application manager gui.

```bash
sudo rm -rf rdf4j-server rdf4j-server.war rdf4j-workbench rdf4j-workbench.war
```

- Copy the new RDF4J WAR files to the Tomcat webapps directory. This can be done with the application manager gui.

```bash
sudo cp /path/to/downloaded/rdf4j-server.war /var/lib/tomcat9/webapps/
sudo cp /path/to/downloaded/rdf4j-workbench.war /var/lib/tomcat9/webapps/
```

- Start the Tomcat server to deploy the new RDF4J version. 

### Restore rdf4j specific repository

```bash 
curl -X PUT -H 'Content-Type: application/x-trig' --data "@${RDF_DUMP_PATH}/dump_rdf4-j${RDF_REPOSITORY}" "http://${SERVER_IP}/rdf4j-server/repositories/${RDF_REPOSITORY}/statements"
```

- Verify Data Integrity
  - Ensure that all your RDF data is intact and accessible.
  - Check the Tomcat logs for any errors or issues during the upgrade process.
  
```bash
sudo tail -f /var/log/tomcat9/catalina.out
```