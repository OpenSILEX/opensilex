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
    * [Technical definitions](#technical-definitions)
    * [Detailed explanations](#detailed-explanations)
    * [Tests](#tests)
    * [Environment](#environment)
  * [Limitations and improvements](#limitations-and-improvements)
  * [Documentation](#documentation)
<!-- TOC -->

## Definitions

- **germplasm** : genetic material of plant cells.
- **data import** : in this case we talk about importing hundreds of germplasms from a CSV file.

### Non-functional requirements

- **Performance** : rework of germplasm's importation was done to improve performance. For more details on improvements see [the benchmark document](https://forgemia.inra.fr/OpenSILEX/opensilex-dev-tools/-/blob/master/benchmark/germplasmes/import.md?ref_type=heads)

## Solution

To improve performance, the importation of germplasms was reworked. We now use a web service to import many germplasms at once, with a minimal number of database calls.

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

### Detailed explanations

{Describe the files, classes, methods, algorithms and architectural choices that are essential to the
solution. You can divide this section in subsections to keep it organized. For example, you can have
an API and Front-end subsections, but that is not mandatory.}

### Tests

{Describe the automatic tests related to this feature, where they are located and what they are supposed
to check.}

### Environment

{Describe the packages and libraries required for the solution, and the specific version if needed.}

## Limitations and improvements

{Describe the known limits of the solution. If you have potential solutions to suggest, you
can specify them here.}

## Documentation

{List internal and external documentations relevant to the feature. For example, configuration
instructions or an external library documentation website}.