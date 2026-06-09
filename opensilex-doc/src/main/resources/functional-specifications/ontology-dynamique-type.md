
# Specifications : [ontology] dynamic type and core ontology extension

**Document history (please add a line when you edit the document)**

| Date       | Editor(s)          | OpenSILEX version   | Comment           |  
|------------|--------------------|---------------------|-------------------|  
| 07/05/2026 | yvan.roux@inrae.fr | 1.5.1 Freaky Fossil | Document creation |  

> ⚠️ _WARNING_ : This document is incomplete ! You can help by expanding it. ⚠️
>
> Missing topics :
>
> - dynamic properties
> - linking properties and dynamic properties to a dynamic type

## Table of contents

<!-- TOC -->
* [Specifications : [ontology] dynamic type and core ontology extension](#specifications--ontology-dynamic-type-and-core-ontology-extension)
  * [Definitions](#definitions)
  * [functional requirements](#functional-requirements)
  * [Business logic](#business-logic)
  * [Usage precautions](#usage-precautions)
  * [Documentation](#documentation)
<!-- TOC -->

## Definitions

- **Ontology** : a structured model that formally defines the concepts, relationships, and categories used to organize and interpret data within an information system.
- **Type** : in an ontology, each concept is represented as a type, which defines the properties and relationships associated with that concept.
- **Dynamic type** : a new type of facility, device, or else... that is not in the core ontology but was added by a user thanks to the web interface or the API.
- **Core ontology** : the ontology that is provided with OpenSILEX and that contains the basic types and relationships for describing scientific data.

## functional requirements

OpenSILEX has a core ontology that can automatically be extended by specific implementations of OpenSILEX.  
Even with this extension mechanism, we sometimes need concepts that are specific to an experiment or an implementation.
but is not aimed to be used in other implementations.

In this case, we can add dynamic types that are subtypes of one of the following core types :
- `vocabulary:Facility`
- `vocabulary:Device`
- `vocabulary:ScientificObject`
- `ooev:Event`
- `vocabulary:FactorCategory`

- Use case #1: As an experiment's supervisor, I want to add some kind of specific events I want to track during the experiment.
- Use case #2: As an experiment's supervisor, I want to add some kind of specific devices I will use to track data during the experiment.
- Use case #3: As an experiment's supervisor, I want to add some kind of specific facilities that will host the experiment.

## Business logic

### Rights
Only admin users can create, delete, or update dynamic types.

### Creation
⚠️ **WARNING**: When creating a type, first check in other ontologies if you can find a matching concept with a satisfying definition.  
If you find one, then use its URI when creating your type in OpenSILEX.


- Dynamic types can be created through the web interface or the API.
- When creating a dynamic type, the user should provide :
    - a URI for the new type (the URI should be unique in the database and should not already exist in the core ontology)
    - a Parent, that could be a type of the core ontology or one of its subtypes (including other dynamic types)
    - english and French names and descriptions
- user may provide an icon by selecting one of the list

### Deletion
Dynamic types can be deleted through the web interface or the API.

Trying to delete a dynamic type with instances will return an error. All instances of this type should be deleted before deleting the type itself.

For example, if I create a subtype of `vocabulary:Facility` with uri `http://my/new/type/of/facilities`, I can delete this new dynamic type until I create a new facility with this type.

## Usage precautions
⚠️ **WARNING**: This feature is an advanced feature. Do not use it if you are not sure about what you want to do. In case of doubt, contact the support team for advanced advice. Here are some **important points to understand before going further in**

### Incompatibility with other instances
When adding dynamic types and properties, you dynamically modify your data structure. This means that you will create data and metadata significantly different from other OpenSILEX instances. This can lead to future difficulties if you aim to share data between many instances or to import your data into other instances.

If you think you could be in one of these situations later, see the next section. Maybe this is not the solution you need.

### Do you really need it ?
The core ontology can also be extended by creating a new implementation of OpenSILEX. This can lead to more work in the beginning but greatly simplify the ontology extension mechanism when dealing with multiple instances.

If you are not using the base OpenSILEX but a named implementation of OpenSILEX. You surely already have a core ontology extension. Contact your administrator or the OpenSILEX support team to discuss your need. Maybe other instances of your implementation can have similar needs. Maybe it could be interesting to extend the general ontology of your implementation and not just modify it dynamically for your instance only.

### Database dependant
Using this feature means that you dynamically modify your data structure. These modifications are stored in the database itself. Doing so leads to loss of your dynamic ontology modification (dynamic types and properties) in case of data reset (restarting an instance from a fresh database without any data or metadata).


### Do not create your own vocabulary
As warned in the `Creation` section : when creating a type, first check in other ontologies if you can find a matching concept with a satisfying definition.

Take time to deeply understand your need and to specify it. Search in other ontologies but also in other openSILEX implementations if someone has not already gone through the same issue.

## Documentation

- see the [technical document](../technical-documentation/opensilex-core/ontology/ontology-extension-system-API.md)