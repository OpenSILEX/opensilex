Opensilex Migration Commands
============================

Author : Renaud COLIN (MISTEA INRAE) <br>
Date : 2021/11/29

This document describes how to execute migration commands into OpenSILEX, the list of available migrations and how to add a new one.

<!-- TOC -->
* [Opensilex Migration Commands](#opensilex-migration-commands)
* [Introduction](#introduction)
  * [Running command line on OpenSILEX executable .jar file (User/Admin oriented)](#running-command-line-on-opensilex-executable-jar-file--useradmin-oriented-)
  * [Running update inside IDE with opensilex-dev-tools module (For developers)](#running-update-inside-ide-with-opensilex-dev-tools-module--for-developers-)
* [List](#list)
* [Descriptions](#descriptions)
  * [org.opensilex.migration.GraphAndCollectionMigration](#orgopensilexmigrationgraphandcollectionmigration)
    * [Description](#description)
    * [Usage note](#usage-note)
  * [org.opensilex.migration.MongoCustomCoordinatesDataTypeUpdate](#orgopensilexmigrationmongocustomcoordinatesdatatypeupdate)
    * [Description](#description-1)
  * [org.opensilex.migration.ScientificObjectNameIntegerConvertMigration](#orgopensilexmigrationscientificobjectnameintegerconvertmigration)
    * [Description](#description-2)
    * [Notes](#notes)
  * [org.opensilex.migration.AgentsMigrateToAccountAndPersons](#orgopensilexmigrationagentsmigratetoaccountandpersons)
    * [Description](#description-3)
  * [org.opensilex.migration.ObjectMigrationFromAccountToPerson](#orgopensilexmigrationobjectmigrationfromaccounttoperson)
    * [Description](#description-4)
  * [org.opensilex.migration.AddAccountCredentialsToProfilWithUserCredential](#orgopensilexmigrationaddaccountcredentialstoprofilwithusercredential)
    * [Description](#description-5)
  * [org.opensilex.migration.MongoDbIndexesMigration](#orgopensilexmigrationmongodbindexesmigration)
    * [Description](#description-6)
  * [org.opensilex.migration.UpdateOntologyContexts](#orgopensilexmigrationupdateontologycontexts)
    * [Description](#description-7)
* [Create an update command (For developers)](#create-an-update-command--for-developers-)
      * [Example](#example)
<!-- TOC -->

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


| Date       | Name (id)                                                                               | Tag          | Commit ID | 
|------------|-----------------------------------------------------------------------------------------|--------------|-----------|
| 2021/11/29 | <b>org.opensilex.migration.GraphAndCollectionMigration</b>                              |              | 46a27611  |
| 2022/08/03 | <b>org.opensilex.migration.MongoCustomCoordinatesDataTypeUpdate</b>                     |              |           |                                                                          
| 2023/01/24 | <b>org.opensilex.migration.ScientificObjectNameIntegerConvertMigration</b>              | 1.0.0-rc+6.5 |           |                                                                          
| 2023/03/17 | <b>org.opensilex.migration.AgentsMigrateToAccountAndPersons</b>                         | 1.0.0-rc+7   | 8ed0303a  |
| 2023/06/26 | <b> org.opensilex.migration.ObjectMigrationFromAccountToPerson </b>                     | 1.0.0        | 613f6d59  |
| 2024/04/09 | <b> org.opensilex.migration.MongoDbIndexesMigration </b>                                | 1.2.3        |           |
| 2024/03/20 | <b> org.opensilex.migration.UpdateOntologyContexts </b>                                 | 1.3.0        | 2e4f0cbe  |                                                                            |              |           |
| 2024/08/07 | <b> org.opensilex.migration.UpdateSitesWithLocationObservationCollectionModel </b>      | 1.3.0 |   |
| 2024/11/04 | <b> org.opensilex.migration.UpdateFacilitiesWithLocationObservationCollectionModel </b> | 1.3.0 |   |

# Descriptions

## org.opensilex.migration.GraphAndCollectionMigration

### Description

This update performs SPARQL graph and MongoDB collection renaming, after major changes on URI generation and
graph/collection renaming
(commit <b>`46a276118a6b8c47669997c7cb5ee4aca4524141`</b> on `master` branch). <br>

### Usage note

<b>[IMPORTANT]</b> : After update success, run `sparql reset-ontologies` command in order to refresh ontologies.

```bash
java -jar opensilex.jar --CONFIG_FILE=<config_file> sparql reset-ontologies
```


## org.opensilex.migration.MongoCustomCoordinatesDataTypeUpdate

### Description

This update changes datatype of any existing `PositionModel` (stored into custom  the **moves** MongoDB collection), from `integer` to `String`


## org.opensilex.migration.ScientificObjectNameIntegerConvertMigration

### Description

When creating or updating a scientific object with a name composed only of digit, inside the RDF database,
the name will be stored as an integer (ex: the name 7964 is not stored as "7964") instead of a String.
This issue only affect the name present in the global scientific object context (ok inside all experiment)

Update the datatype of all scientific object name which are stored inside the global graph, as integer to string. <br>
It only applies for object with a name which is fully composed of digit. <br>

### Notes

To check if data in your opensilex SPARQL repository is affected by this issue, before running the migration command,
you can run the following SPARQL query :

```sparql
PREFIX vocabulary: <http://www.opensilex.org/vocabulary/oeso#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

ASK WHERE { 
    ?type (rdfs:subClassOf)* vocabulary:ScientificObject
    
    # !! Replace this graph by your instance base URI graph
    GRAPH <http://www.phenome-fppn.fr/set/scientific-object> { 
        ?uri  a           ?type ;
             rdfs:label  ?name
        FILTER ( ! ( datatype(?name) = xsd:string ) )
        BIND(str(?name) AS ?nameString)
    }
}
```

## org.opensilex.migration.AgentsMigrateToAccountAndPersons

### Description

This update changes the old Data model for User, by separating users in account and person.
Without this migration, users will not be able to log in.


## org.opensilex.migration.ObjectMigrationFromAccountToPerson

### Description

This migration change the predicate (link) of many subjects, in order to have Person and not Account as object.
So, many subjects which were linked to accounts, are now linked to persons.
This migration ensure that your actual data changes with the data model.

following predicates are concerned :
- projects : Oeso:hasAdministrativeContact, Oeso:hasScientificContact, Oeso:hasCoordinator
- experiment : Oeso:hasScientificSupervisor, Oeso:hasTechnicalSupervisor
- device : Oeso:personInCharge
- Provenance : operator (this is not a predicate since it's store in the NoSQl database, but it also has to be migrated)

## org.opensilex.migration.AddAccountCredentialsToProfilWithUserCredential

### Description

This migration make a list of all profiles that has a credential on user. Then, it adds the same credential (show, add/update or delete) but for the accounts.
This migration was done because Users credentials was replaced by account credentials in the web Interface, so it is necessary to migrate credentials, otherwise some people may have the surprise to not be able to reach the "account menu' anymore after the last deployment of OpenSilex 1.2.


## org.opensilex.migration.MongoDbIndexesMigration

### Description

- This migration ensures that the database has the indexes specified by OpenSILEX
- **Note**: Executing this migration cause the deletion of indexes which are not registered by OpenSILEX


## org.opensilex.migration.UpdateOntologyContexts

### Description

For this version, the migration process updates contexts representing external ontologies from the triple store. The concepts previously used from these external ontologies have been integrated into our core ontology.

Consequently, the migration script `UpdateOntologyContexts` removes the following external ontology contexts:

- `http://www.opensilex.org/security`
- `http://www.w3.org/ns/org`
- `http://xmlns.com/foaf/0.1/`
- `http://www.w3.org/2006/vcard/ns`
- `http://www.w3.org/ns/oa`
- `http://www.opensilex.org/vocabulary/oeev`
- `http://www.w3.org/2002/07/owl`
- `http://www.w3.org/2006/time`
- `http://purl.org/dc/terms/`

Additionally, it removes the `oeso-ext` graph, which has been renamed to `oeso-phis`.

## org.opensilex.migration.UpdateSitesWithLocationObservationCollectionModel

### Description
This migration refactors site locations with the new location model:

- In RDF4J, add ObservationCollection properties for each Site with address.
- In MongoDB, get sites from the Geospatial collection and copy them to the new Location collection with the new model and observationCollection URI.## org.opensilex.migration.UpdateSitesWithLocationObservationCollectionModel

## org.opensilex.migration.UpdateFacilitiesWithLocationObservationCollectionModel

### Description
This migration refactors facility locations with the new location model:

- In RDF4J, add ObservationCollection properties for each facility with address or spatial coordinates.
- In MongoDB, get facilities from the Geospatial collection and copy them to the new Location collection with the new model and observationCollection URI. For spatial coordinates not from an address, a default date is added (01/01/1970).

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
