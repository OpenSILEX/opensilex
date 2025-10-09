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
  * [fonctional requirements](#fonctional-requirements)
  * [Non-functional requirements](#non-functional-requirements)
  * [Business logic](#business-logic)
<!-- TOC -->

## Definitions

- **germplasm** : genetic material of plant cells.
- **data import** : in this case we talk about importing hundreds of germplasms from a CSV file.

## fonctional requirements

- **Importation of germplasms** : it should be possible to import many germplasms at once from a CSV file with an upsert operation.

## Non-functional requirements

- **Performance** : rework of germplasm's importation was done to improve performance. For more details on improvements see [the benchmark document](https://forgemia.inra.fr/OpenSILEX/opensilex-dev-tools/-/blob/master/benchmark/germplasmes/import.md?ref_type=heads)

## Business logic

The business logic is the same as the creation web service, any germplasm present in the CSV file should :
- have a unique URI (unique in the database and in the CSV file)
- have a valid RDF type (a type that exists in ontology and is a subclass of `OESO:Germplasm`)
- sometimes have a species, variety or accession, according to the following rules :
  - if it is a species, no other information is needed
  - if it is a variety, it should have a species
  - if it is an accession, it should have a variety or a species
  - if it has another type, it should have an accession, a variety or a species
- if it has species, variety or accession, they should exist in the database (URI exists with the right RDF type)
- coherency between the species, variety and accession (if they exist) should validate the following rules :
  - if germplasm 'A' has a variety and a species, the variety should have the same species as germplasm 'A'
  - if germplasm 'A' has an accession and a species, the accession should have the same species as germplasm 'A'
  - if germplasm 'A' has an accession and a variety, the accession should have the same variety as germplasm 'A'
- Coherency of relations *** ⚠️ _WARNING_ : this part is not actually very clear (see GermplasmAPI::getGermplasmModelsAndValidateRelations ) ***