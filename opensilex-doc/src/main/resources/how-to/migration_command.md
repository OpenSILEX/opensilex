Opensilex Migration Commands
============================

Author : Renaud COLIN (MISTEA INRAE) <br>
Date : 2021/11/29

This document describes how to execute migration commands into OpenSILEX, the list of available migrations and how to add a new one.

# Introduction

OpenSILEX allow to easily perform some update/migration operation by just executing commands. <br>
There are two-way to run an update through OpenSILEX command mechanism.

## Running command line on OpenSILEX executable .jar file (User/Admin oriented)

- From the directory which contains your OpenSILEX executable .jar file

```bash
java -jar opensilex.jar --CONFIG_FILE=<config_file> system run-update <opensilex_command>
```

- `<opensilex_command>` <b>(required)</b> : name of a valid migration command (see list below)
  . <br>
- `--CONFIG_FILE=<config_file>` : <b>(optional)</b> : option to specify the path to your custom OpenSILEX config (if set, then
  command will run on an OpenSILEX instance running with the given config)

## Running update inside IDE with opensilex-dev-tools module (For developers)

Execute `main` from the `RunUpdate` class (`opensilex-dev-tools` module) with some OpenSILEX update name (see list below) as program argument.

Example :

```bash
org.opensilex.migration.GraphAndCollectionMigration
```

# List

- Name must be unique and match to a JAVA `Class` which implements `org.opensilex.update.OpenSilexModuleUpdate`
- The `Class` path must be resolvable by the OpenSILEX executable .jar file
- To be resolvable, the `Class` must exist in one OpenSILEX module (embedded or custom module).


| Date       | Name (id)                                                  | Description                                  | 
|------------|---------------------------------------------------------|----------------------------------------------|
| 2021/11/29 |<b>org.opensilex.migration.GraphAndCollectionMigration</b> | SPARQL graph and MongoDB collection renaming after URI generation update |  |
|            |                                                         |                                              |                                                                          

# Descriptions

## org.opensilex.migration.GraphAndCollectionMigration

### Description

This update performs SPARQL graph and MongoDB collection renaming, after major changes on URI generation and
graph/collection renaming
(commit <b>`46a276118a6b8c47669997c7cb5ee4aca4524141`</b> on `master` branch). <br>

### Usage

```bash
java -jar opensilex.jar --CONFIG_FILE=<config_file> system run-update org.opensilex.migration.GraphAndCollectionMigration
```

<b>[IMPORTANT]</b> : After update success, run `sparql reset-ontologies` command in order to refresh ontologies.

```bash
java -jar opensilex.jar --CONFIG_FILE=<config_file> sparql reset-ontologies
```

# Create an update command (For developers)

Each Opensilex update implements the <b>`org.opensilex.update.OpenSilexModuleUpdate`
</b> interface (from `opensilex-main` module).

```java
public interface OpenSilexModuleUpdate {

    /**
     * Date of update creation for auto execution.
     *
     * @return Date of update creation
     */
    OffsetDateTime getDate();

    /**
     * Description of the update.
     *
     * @return Description
     */
    String getDescription();

    /**
     * Update logic to implement.
     */
    void execute() throws OpensilexModuleUpdateException;

    /**
     * @param opensilex the Opensilex instance
     */
    void setOpensilex(OpenSilex opensilex);
}
```
To add a new update, just add inside one OpenSILEX module some `Class` which implements `org.opensilex.update.OpenSilexModuleUpdate`

#### Example

`org.opensilex.migration.GraphAndCollectionMigration` from `opensilex-migration` module

```java

package org.opensilex.migration;

import java.time.OffsetDateTime;

public class GraphAndCollectionMigration implements OpenSilexModuleUpdate {

    private OpenSilex opensilex;

    @Override
    public OffsetDateTime getDate() {
        return OffsetDateTime.parse("2021/11/29");
    }

    @Override
    public String getDescription() {
        return "Rename SPARQL graph and mongo collections";
    }

    @Override
    public void setOpensilex(OpenSilex opensilex) {
        this.opensilex = opensilex;
    }

    @Override
    public void execute() throws OpensilexModuleUpdateException {

        // access to OpenSILEX SPARQL and MongoDB services/configs
        SPARQLServiceFactory factory = opensilex.getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
        SPARQLService sparql = factory.provide();
        SPARQLConfig sparqlConfig = opensilex.getModuleConfig(SPARQLModule.class, SPARQLConfig.class);

        MongoDBService mongodb = opensilex.getServiceInstance(MongoDBService.DEFAULT_SERVICE, MongoDBService.class);
        MongoDBConfig mongoDBConfig = opensilex.loadConfigPath(MongoDBConfig.DEFAULT_CONFIG_PATH, MongoDBConfig.class);

        // graph and collection migration code here
    }
}
```
