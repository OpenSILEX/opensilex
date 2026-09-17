# Technical documentation : [ontology module] Ontology extension system using module

**Document history (please add a line when you edit the document)**

| Date       | Editor(s)              | OpenSILEX version   | Comment           |
|------------|------------------------|---------------------|-------------------|
| 30/06/2026 | yvan.roux@opensilex.fr | 1.5.0 Freaky Fossil | Document creation |

> ⚠️ _WARNING_ : This document is about module ontology extension system and does not cover API extension system. ⚠️
>
> If you are looking for an explanation about the way you can extend the core ontology using the OpenSILEX API see [ontology API extension system](/src/main/resources/technical-documentation/opensilex-core/ontology/ontology-extension-system-API.md).

## Table of contents

<!-- TOC -->
  * [Definitions](#definitions)
  * [Functional requirements](#functional-requirements)
  * [Technical specifications](#technical-specifications)
  * [Creating a new OpenSILEX module](#creating-a-new-opensilex-module)
    * [Create an ontology file](#create-an-ontology-file)
    * [Register the new ontology](#register-the-new-ontology)
    * [Run the reset](#run-the-reset)
    * [Follow your path](#follow-your-path)
  * [Limitations and improvements](#limitations-and-improvements)
  * [Documentation](#documentation)
<!-- TOC -->

## Definitions

- **Ontology** : An ontology is a formal representation of a set of concepts and the relationships between those concepts. It describes a data model that is used in OpenSILEX to represent concepts and manage data.
- **Class** : In ontology, a class is a type of concept, for example, "Plant" or "Experiment". A class `ClassA` represent objets that has the relation `object rdf:type ClassA`.
- **Type** : The word type is often used as a synonym of class from a user perspective. In the interface we define new types of events rather than new subclasses of event.
- **Property** : In ontology, a property describes a relationship between concepts. For example, `rdfs:label` is a property that links a concept to a string that is its label (sort of name).

## Functional requirements

Creating a new module that extends the ontology allows the user to statically add new concepts to the ontology at compile time. 

This could be useful for:
- Creating new subtypes of scientific objects or events with special data linked to it
- Extending existing classes with new properties or relationships
- Easily sharing this ontology modification with other instances of OpenSILEX
- creating a whole new concept that you will use in the module to extend the API with new web services and maybe new front-end pages.

## How to implement a new ontology in a new module

## Creating a new OpenSILEX module

To create and register a new module, follow the instructions in the [module documentation](/src/main/resources/technical-documentation/opensilex-module/modules.md).

### Create an ontology file

This document does not aim to explain how to write an ontology file, it is technical documentation about implementation of the ontology extension system.

In your module, create a file `src/main/resources/ontologies/{your-ontology-name}.owl` that contains the ontology definition.
You can see examples of ontology files in the `opensilex-phis` module

### Register the new ontology

Make your module class (the one extending `OpenSilexModule` class) implement the `SPARQLExtension` interface.

Override the `getOntologiesFiles()` method to return the list of ontology files you want to register. Once again you can see examples of this in the `opensilex-phis` module. You can now compile the OpenSILEX project with your module.

### Run the reset
Once OpenSILEX is compiled with your module configured in the POM.xml (see [module documentation](/src/main/resources/technical-documentation/opensilex-module/modules.md)), you can run the `sparql reset-ontologies` command to load the ontology in the database.

Once done, you should be able to see your new ontology's concepts and contexts in the RDF4J Triple Store database.

Try launching the OpenSILEX server as some error can appear at launch if the ontology file is not well-formed or not compatible with the current version of the ontology.

### Follow your path

If you have declared subtypes of concepts as scientific objects, devices, events, etc. you should now be able to create new scientific objects of your new type using the OpenSILEX API and front-end without any further work.

You should also be able to use your new concepts to extend the API with new web services and store your new concepts in the database. For now there is no documentation about this.

## Frontend implementation of ontology extension system

- Creating a new subtype of event, device, scientific object or facility will allow you to choose this type in their respective forms.
- Adding properties to this new subtype will automatically extend the form with new fields for the new properties.
- Adding a cardinality constraint more than one will modify the new fieldset to allow multiple values. (works only on object properties and not on datatype properties like string or integer)

- Creating a new subtype og germplasm will also allow you to choose this type in the germplasm creation interface, but it will not automatically add the properties as new columns in the table.



## Limitations and improvements

- As this is an extension system, it is not possible to modify the core ontology or to delete existing concepts. This behaviour is not planned to be changed and ensures that the ontology is always consistent.
- The cardinality system is not totally implemented for datatype in the frontend.

## Documentation

- how to create a new module : [module documentation](/src/main/resources/technical-documentation/opensilex-module/modules.md)
- writing API and front-end in a new module : [ontology-module-extension-system.md](/src/main/resources/technical-documentation/opensilex-module/ontology-module-extension-system.md)

**ontology extension system with API and not module :**
- [ontology API extension system](/src/main/resources/technical-documentation/opensilex-core/ontology/ontology-extension-system-API.md)
- [ontology API](/src/main/resources/technical-documentation/opensilex-core/ontology/ontology-api.md)
