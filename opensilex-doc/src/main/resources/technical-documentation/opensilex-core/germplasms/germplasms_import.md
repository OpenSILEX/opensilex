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
  * [functional requirements](#functional-requirements)
  * [Non-functional requirements](#non-functional-requirements)
  * [Solution](#solution)
  * [Technical specifications](#technical-specifications)
    * [Upsert Service](#upsert-service)
    * [Multiple errors return](#multiple-errors-return)
    * [Tests](#tests)
  * [Limitations and improvements](#limitations-and-improvements)
<!-- TOC -->

## Definitions

- **germplasm** : genetic material of plant cells.
- **data import** : in this case we talk about importing hundreds of germplasms from a CSV file.

## functional requirements

**functional specification** : to understand the purpose of the feature and its precise rules see relative functional specification file :
- [germplasms_import_specs.md](../../../functional-specifications/germplasms_import_specs.md)

## Non-functional requirements

- **Performance** : rework of germplasm's importation was done to improve performance. For more details on improvements see [the benchmark document](https://forgemia.inra.fr/OpenSILEX/opensilex-dev-tools/-/blob/master/benchmark/germplasmes/import.md?ref_type=heads)

## Solution

To improve performance, the importation of germplasms was reworked. We now use a web service to import many germplasms at once, with a minimal number of database calls.

Moreover, the importation is an upsert operation, meaning that if a germplasm already exists in the database, it will be updated with the new information from the CSV file. If it does not exist, it will be created.

One of the requirements of the importation is to have a very detailed error return, containing each error for each germplasm.

## Technical specifications

Germplasms are sent as a list of `GermplasmDTO` objects, which are created from the CSV file by the web interface with JS.

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