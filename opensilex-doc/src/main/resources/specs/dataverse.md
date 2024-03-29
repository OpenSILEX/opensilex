******
* This document follows the Specification Guidelines draft (as of 2023-06-08)
* Author : gabriel.besombes@inrae.fr
* Date : 2023-06-08
******

# Specifications : dataverse

This document briefly describes the functional and technical specifications of `dataverses` in OpenSILEX.

## Needs

Dataverse is a system often used to store data long-term and share it. The users would like a way to lower the 
redundancy created by using both OpenSILEX and a Dataverse.

A more specific case for INRAE is the use of "Recherche Data Gouv" as it is recommended for all public research in France.

## Solution

### Business logic

An OpenSILEX user that has a Recherche Data Gouv account can create a Dataset draft from the metadata on OpenSILEX.

For now this is a proof of concept/minimum viable product and limited to Recherche Data Gouv instead of integrating 
with generic Dataverses.

## Technical specifications

### Definitions

[Dataverse project](https://dataverse.org/)

__Dataset__: A dataset is a file containing data and its associated metadata.

__Dataset draft__: A dataset that isn't published yet. It has a reserved DOI but isn't public yet.

[Recherche Data Gouv](https://entrepot.recherche.data.gouv.fr/)

### Detailed explanations

The `DataverseAPI` class contains 3 webservices:
* `availableDatasetLanguages()` returns the available dataset languages from the opensilex config
* `availableDatasetMetadataLanguages()` returns the available dataset metadata languages from the opensilex config
* `createDataset()` creates a Dataset draft on a Dataverse from metadata extracted from an experiment and links it with an 
OpenSILEX Document. To do so it passes the information through a `DataverseAPIPostDatsetDTO` to a `DataverseClient` 
which calls the Dataverse API. A new document is created using a `DocumentModel` and the identifier returned by the 
Dataverse API is used to link this document and the Dataset draft.

__Note__: This draft doesn't contain a file. Only some metadata is present and the rest of the process will have 
to happen on the respective Dataverse.

For now these are the only functionalities so datasets can't be updated, deleted or synchronised on both sides.

The frontend rights are in the config file ("credentials.yml") but should eventually be moved to the opensilex-dataverse 
module but that would necessitate to adapt the `ExtraCredentialService`.

No generic frontend is available, only a specific one for Recherche Data Gouv.
The Recherche Data Gouv Base Path setup for the instance can be retrieved using the corresponding webservice.

#### Dataverse client

In order to query the dataverse API, we originally used the Java dataverse client provided by IQSS (https://github.com/IQSS/dataverse-client-java).
However, using it as a dependency triggered a deployment error on some servers. So, fully integrated the code of the
client into OpenSILEX under the `com.researchspace` package.

As the dataverse client was originally designed with Lombok, we had to "delombok" it in order not to add another dependency
to OpenSILEX. See the [relevant documentation](https://projectlombok.org/features/delombok).

> WARNING : The original work is licensed under the Apache 2.0 License. That means that any change to the original 
source files must be marked in some way, for example by adding a comment on top of the file.

### Tests

The tests are very limited due to our architecture.
For now the only test is `DataverseClientTest.testCreateAFacade()` which tests the construction of the Dataset facade.
