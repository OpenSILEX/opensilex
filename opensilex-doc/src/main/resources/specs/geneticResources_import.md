# Specifications : geneticResources import

**Document history (please add a line when you edit the document)**

| Date       | Editor(s)          | OpenSILEX version     | Comment           |
|------------|--------------------|-----------------------|-------------------|
| 19/08/2024 | yvan.roux@inrae.fr | 1.3.3 - Dizzy Diamond | Document creation |

## Table of contents

<!-- TOC -->
* [Specifications : geneticResources import](#specifications--geneticResources-import)
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

- **geneticResource** : genetic material of plant cells.
- **data import** : in this case we talk about importing hundreds of geneticResources from a CSV file.

### fonctional requirements

- **Importation of geneticResources** : it should be possible to import many geneticResources at once from a CSV file with an upsert operation.

### Non-functional requirements

- **Performance** : rework of geneticResource's importation was done to improve performance. For more details on improvements see [the benchmark document](https://forgemia.inra.fr/OpenSILEX/opensilex-dev-tools/-/blob/master/benchmark/geneticResourcees/import.md?ref_type=heads)

## Solution

To improve performance, the importation of geneticResources was reworked. We now use a web service to import many geneticResources at once, with a minimal number of database calls.

Moreover, the importation is an upsert operation, meaning that if a geneticResource already exists in the database, it will be updated with the new information from the CSV file. If it does not exist, it will be created.

One of the requirements of the importation is to have a very detailed error return, containing each error for each geneticResource.

### Business logic

The business logic is the same as the creation web service, any geneticResource present in the CSV file should :
- have a unique URI (unique in the database and in the CSV file)
- have a valid RDF type (a type that exists in ontology and is a subclass of `OESO:GeneticResource`)
- sometimes have a species, variety or accession, according to the following rules :
  - if it is a species, no other information is needed
  - if it is a variety, it should have a species
  - if it is an accession, it should have a variety or a species
  - if it has another type, it should have an accession, a variety or a species
- if it has species, variety or accession, they should exist in the database (URI exists with the right RDF type)
- coherency between the species, variety and accession (if they exist) should validate the following rules :
  - if geneticResource 'A' has a variety and a species, the variety should have the same species as geneticResource 'A'
  - if geneticResource 'A' has an accession and a species, the accession should have the same species as geneticResource 'A'
  - if geneticResource 'A' has an accession and a variety, the accession should have the same variety as geneticResource 'A'
- Coherency of relations *** ⚠️ _WARNING_ : this part is not actually very clear (see GeneticResourceAPI::getGeneticResourceModelsAndValidateRelations ) *** 

## Technical specifications

GeneticResources are sent as a list of `GeneticResourceDTO` objects, which are created from the CSV file by the web interface with JS.

### Upsert Service

Classic service creation with Swagger path = `/core/geneticResource/import`

All the business logic is done in the `GeneticResourceLogic.java` class, which is called by the service. Check the `GeneticResourceLogic::checkBeforeCreateOrUpdate` method to see how the business logic is implemented.

Checking the business logic does not throw an exception. The aim is to be able to continue the check right to the end.
This is done to collect and return all the errors contained in the geneticResources.

### Multiple errors return

`MultipleErrorException` is used to return multiple errors in a single response. It extends `WebApplicationException` so it is automatically handled by the OpenSILEX system through the `ExceptionJsonMapper`.

During checking for buisiness logic errors, use `MultipleErrorObjectList` to easily collect all the errors. Use `MultipleErrorObjectList::ToDTO` to convert it to DTO and pass it to the `MultipleErrorException` constructor.


### Tests

Most of the tests are in `core/geneticResource/api/GeneticResourceAPITest.java`. They test only one geneticResource at a time, but it is still enough to ensure that the business logic is respected.


## Limitations and improvements

Despite the improvements, the importation of geneticResources is still not as fast as we would like, especially for the update part. The main bottleneck is the `SPARQLService::update(Node graph, List<T> instances)` method.