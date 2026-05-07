# Specifications : [ontology] dynamique type and core ontology extension

**Document history (please add a line when you edit the document)**

| Date       | Editor(s)          | OpenSILEX version | Comment           |
|------------|--------------------|-------------------|-------------------|
| 07/05/2026 | yvan.roux@inrae.fr | 1.5.0             | Document creation |

## Table of contents

<!-- TOC -->
* [Specifications : [ontology] dynamique type and core ontology extension](#specifications--ontology-dynamique-type-and-core-ontology-extension)
  * [Table of contents](#table-of-contents)
  * [Definitions](#definitions)
  * [functional requirements](#functional-requirements)
  * [Business logic](#business-logic)
  * [Documentation](#documentation)
<!-- TOC -->

## Definitions

- **Ontology** : a structured model that formally defines the concepts, relationships, and categories used to organize and interpret data within an information system.
- **Type** : in an ontology, each concept is represented as a type, which defines the properties and relationships associated with that concept.
- **Dynamique type** : a new type of facility, device or else... , that is not in the core ontology but was added by a user thanks to the web interace or the API.
- **Core ontology** : the ontology that is provided with OpenSILEX and that contains the basic types and relationships for describing scientific data.

## functional requirements

OpenSILEX has a core ontology that can automatically be extended by specifics implementations of OpenSILEX.
Even with this extension mechanism, we sometimes need concepts that are specific to an experiment or an implementation,
but is not aimed to be used in other implementations.

In this case, we can add dynamique types that are subtypes of on of the following core types :
- `vocabulary:Facility`
- `vocabulary:Device`
- `vocabulary:ScientificObject`
- `ooev:Event`
- `vocabulary:FactorCategory`

- Use case #1: As an experiment's supervisor, I want to add some kind of specific events I want to track during the experiment.
- Use case #2: As an experiment's supervisor, I want to add some kind of specific devices I will use to track data during the experiment.
- Use case #3: As an experiment's supervisor, I want to add some kind of specific facilities that will host the experiment.

## Business logic

### Creation
⚠️ **WARNING**: When creating type, first check in other ontology if you can find a matching concept with a satisfying definition.
If you find one then use its URI when creating your type in OpenSILEX.


- Dynamique types can be created through the web interface or the API.
- When creating a dynamique type, user should provide :
  - a URI for the new type (the URI should be unique in the database and should not already exist in the core ontology)
  - a Parent, that could be a type of the core ontology or one of its subtypes (including other dynamique types)
  - english and French names and descriptions
- user may provide and icon by selecting on of the list

### Deletion
Dynamique types can be deleted through the web interface or the API.

Trying to delete a dynamique type with instances will return an error, all instances of this type should be deleted before deleting the type itself.

## Documentation

- see the [technical document]({path})
