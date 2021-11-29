Opensilex Migration Commands
============================

Author : Renaud COLIN (MISTEA INRAE) <br>
Date : 2021/11/29

This document describe migration commands into OpenSILEX, the list of available Commands and how to add a new one.


# Introduction

Opensilex provide a way to easily performs some update/migration operation by just executing commands.

- Move to the directory which contains your OpenSILEX executable .jar file.

```bash
cd /opensilex-dev/opensilex-release/target/opensilex/
```

- Execute migration command

```bash
java -jar opensilex.jar CONFIG-FILE=<config_file> system run-update <opensilex_command>
```

`<opensilex_command>` <b>(required)</b> : name of a valid migration command (see the list just below). <br>
`CONFIG-FILE=<config_file>` : <b>(optionnal)</b> : option to specify path to your custom opensilex config (if set, then command will run on an Opensilex instance running with the given config)


## Commands


### Index


| Date       | Name                                                     | Description                                  | Usage                                                                     |
|------------|---------------------------------------------------------|----------------------------------------------|---------------------------------------------------------------------------|
| 2021/11/29 | org.opensilex.migration.GraphAndCollectionMigration | SPARQL graph and MongoDB collection renaming | system run-update org.opensilex.migration.GraphAndCollectionMigration |
|            |                                                         |                                              |                                                                           |
|            |                                                         |                                              |                                                                           |

### org.opensilex.migration.GraphAndCollectionMigration

This command performs SPARQL graph and MongoDB collection renaming, after URI/graph/collection renaming. <br>

```bash
java -jar opensilex.jar CONFIG-FILE=<config_file> system run-update org.opensilex.migration.GraphAndCollectionMigration
```	

<b>[IMPORTANT]</b> : Run `sparql reset-ontologies` command in order to refresh ontologies.

```bash
java -jar opensilex.jar CONFIG-FILE=<config_file> sparql reset-ontologies
```	

## Add a command

Each Opensilex command implements the 
<b>`org.opensilex.update.OpenSilexModuleUpdate`
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

#### Example
 
`org.opensilex.migration.GraphAndCollectionMigration` from `opensilex-migration` module


```java

package org.opensilex.migration;

public class GraphAndCollectionMigration implements OpenSilexModuleUpdate {

    private OpenSilex opensilex;

    @Override
    public OffsetDateTime getDate() {
        return OffsetDateTime.now();
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
        // graph and collection migration code here
    }
}
```
