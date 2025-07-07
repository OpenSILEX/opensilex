# Specifications : germplasms import

**Document history (please add a line when you edit the document)**

| Date       | Editor(s)          | OpenSILEX version     | Comment           |
|------------|--------------------|-----------------------|-------------------|
| 19/08/2024 | yvan.roux@inrae.fr | 1.3.3 - Dizzy Diamond | Document creation |

## Table of contents

<!-- TOC -->
* [Specifications : germplasms import](#specifications--germplasms-import)
  * [Table of contents](#table-of-contents)
  * [Definitions](#definitions)
    * [Non-functional requirements](#non-functional-requirements)
  * [Solution](#solution)
    * [Business logic](#business-logic)
  * [Technical specifications](#technical-specifications)
    * [Upsert Service](#upsert-service)
    * [Multiple errors return](#multiple-errors-return)
    * [Tests](#tests)
  * [Limitations and improvements](#limitations-and-improvements)
<!-- TOC -->

## Definitions

- **germplasm** : genetic material of plant cells.
- **data import** : in this case we talk about importing hundreds of germplasms from a CSV file.

### fonctional requirements

- **Importation of germplasms** : it should be possible to import many germplasms at once from a CSV file with an upsert operation.

### Non-functional requirements

- **Performance** : rework of germplasm's importation was done to improve performance. For more details on improvements see [the benchmark document](https://forgemia.inra.fr/OpenSILEX/opensilex-dev-tools/-/blob/master/benchmark/germplasmes/import.md?ref_type=heads)

## Solution

To improve performance, the importation of germplasms was reworked. We now use a web service to import many germplasms at once, with a minimal number of database calls.

Moreover, the importation is an upsert operation, meaning that if a germplasm already exists in the database, it will be updated with the new information from the CSV file. If it does not exist, it will be created.

One of the requirements of the importation is to have a very detail error return, containing each error for each germplasm.

### Business logic

The business logic is the same as the creation web service, any germplasm present in the CSV file should :
- has a unique URI (unique in the database and in the CSV file)
- has a valid RDF type (a type that exist in ontology and is a subclass of `OESO:Germplasm`)
- sometimes has a species, variety or accession, according to the following rules :
  - if it is a species, no other information is needed
  - if it is a variety, it should have a species
  - if it is an accession, it should have a variety or a species
  - if it has another type, it should have an accession, a variety or a species
- if it has species, variety or accession, they should exist in the database (URI exists with the right RDF type)
- coherency between the species, variety and accession (if they exist) should validate the following rules :
  - if germplasm 'A' has a variety and a species, the variety should have the same species as germplasm 'A'
  - if germplasm 'A' has an accession and a species, the accession should have the same species as germplasm 'A'
  - if germplasm 'A' has an accession and a variety, the accession should have the same variety as germplasm 'A'

## Technical specifications

Germplasms are send as a list of `GermplasmDTO` objects, which are created from the CSV fil by the web interface with JS.

### Upsert Service

Classic service creation with Swagger path = `/core/germplasm/import`

All the business logic is done in the `GermplasmLogic.java` class, which is called by the service. Check the `GermplasmLogic::checkBeforeCreateOrUpdate` method to see how the business logic is implemented.

Checking the business logic does not throw an exception. The aim is to be able to continue the check right to the end.
This is done to collect and return all the errors contained in the germplasms.

### Multiple errors return

`MultipleErrorException` is used to return multiple errors in a single response. It extends `WebApplicationException` so it is automatically handled by the OpenSILEX system through the `ExceptionJsonMapper`.

During checking for buisiness logic errors, use `MultipleErrorObjectList` to easily collect all the errors. Use `MultipleErrorObjectList::ToDTO` to convert it to DTO and pass it to the `MultipleErrorException` constructor.


### Tests

Most of the tests are in `core/germplasm/api/GermplasmAPITest.java`. They test only one germplasm at a time, but it is still enough to ensure that the business logic is respected.


## Limitations and improvements

Despite the improvements, the importation of germplasms is still not as fast as we would like, especially for the update part. The main bottleneck is the `SPARQLService::update(Node graph, List<T> instances)` method.