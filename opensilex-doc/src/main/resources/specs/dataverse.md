******
* This document follows the Specification Guidelines draft (as of 2023-06-08)
* Author : gabriel.besombes@inrae.fr
* Date : 2023-06-08
******

# Specifications : dataverse

This document describes briefly the functional and technical specifications of the dataverses in OpenSILEX.

## Needs

Dataverse is an often used system to store data long-term and share it. The users would like a way to lower the 
redundancy created by using both OpenSILEX and a Dataverse.
A more specific case for INRAE is the use of Recherche Data Gouv as it is recommended for all public research.

## Solution

### Business logic

An OpenSILEX user that has a Recherche Data Gouv account can create a Dataset draft from the metadata on OpenSILEX.

For now this is a proof of concept/minimum viable product and limited to Recherche Data Gouv instead of  integrating 
with generic Dataverses.

## Technical specifications

### Definitions

[Dataverse project](https://dataverse.org/)
__Dataset__: A dataset is a file containing data and its associated metadata.
__Dataset draft__: A dataset that isn't published yet.

[Recherche Data Gouv](https://entrepot.recherche.data.gouv.fr/)

### Detailed explanations

The `DataverseAPI` class contains 3 webservices:
* `availableDatasetLanguages()` returns the available dataset languages from the opensilex config
* `availableDatasetMetadataLanguages()` returns the available dataset metadata languages from the opensilex config
* `createDataset()` creates a Dataset draft on a Dataverse from metadata extracted from an experiment and links it with an 
OpenSILEX Document. To do so it passes the information through a `DataverseAPIPostDatsetDTO` to a `DataverseClient` 
which calls the Dataverse's API. A new document is created using a `DocumentModel` and the identifier returned by the 
Dataverse API is used to link this document and the Dataset draft.
__Note__: This draft doesn't contain a file. Only some metadata is present and the rest of the process will have 
to happen on the respective Dataverse.

For now these are the only functionalities so datasets can't be updated, deleted or synchronised on both sides.

The frontend rights are in the config file ("credentials.yml) but should eventually be moved to the opensilex-dataverse 
module but that would necessitate to adapt the ExtraCredentialService.

No generic frontend is available, only a specific one for Recherche Data Gouv.
The Recherche Data Gouv Base Path setup for the instance can be retrieved using the corresponding webservice.
Another limitation is that there is no loading symbol during the form's validation for the frontend.
This may be a problem for slower connections or if the Recherche Data Gouv service is unavailable.

### Tests

The tests are very limited due to our architecture.
For now the only test is DataverseClientTest.testCreateAFacade() which tests the construction of the Dataset facade.

### Aditional informations

To include this module you need to :
* remove or comment out this line in the opensilex.yml config file :
```yaml
        org.opensilex.opensilex-dataverse.DataverseModule: opensilex-dataverse.jar
```
* complete the config for your dataverse. ex :
```yaml
dataverse:
  externalAPIKey: "****-***-***-****"
  rechercheDataGouvBasePath: "https://data-preproduction.inrae.fr"
  dataverseAlias: "opensilex-tests"
  dataverseLanguages:
    - en
    - fr
  datasetMetadataLanguages:
    - en
    - fr
```
* uncomment these dependencies in the root pom.xml :
```xml
        <!-- Dataverse module -->
        <!--<module>opensilex-dataverse</module>-->
```
```xml
        <!-- Dataverse dependency -->
        <!--<dependency>
            <groupId>org.opensilex</groupId>
            <artifactId>opensilex-dataverse</artifactId>
            <version>${revision}</version>
        </dependency>-->
```
* Change this in main.ts at row 483 :
```ts
$opensilex.loadModules([
  "opensilex-security",
  "opensilex-core"
])
```
to :
```ts
$opensilex.loadModules([
  "opensilex-security", 
  "opensilex-core",
  "opensilex-dataverse"
])
```
This last point is suboptimal, but it is the only quick fix I found for the translation files not being loaded when the 
application starts but only once you click on the respective menu.