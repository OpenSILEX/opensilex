# Specifications : [Scientific object] deletion rules for scientific objects

**Document history (please add a line when you edit the document)**

| Date       | Editor(s)             | OpenSILEX version       | Comment                                                         |
|------------|-----------------------|-------------------------|-----------------------------------------------------------------|
| 29/07/2022 | renaud.colin@inrae.fr | 1.0.0-rc+4.1            | Document creation                                               |
| 10/12/2025 | yvan.roux@inrae.fr    | 1.4.9 Explosive Emerald | refactoring document to follow new template and file management |

## Table of contents

<!-- TOC -->
* [Specifications : [Scientific object] deletion rules for scientific objects](#specifications--scientific-object-deletion-rules-for-scientific-objects)
  * [Table of contents](#table-of-contents)
  * [Business logic](#business-logic)
    * [Introduction](#introduction)
    * [In experimental context](#in-experimental-context)
    * [In global context](#in-global-context)
<!-- TOC -->

## Business logic
### Introduction

Scientific object can be handled in two context : 
- **Experimental context** : the object is attached into one or multiple experiment and the object properties are stored 
into each corresponding experiment RDF graph. These relations can change from a experiment to another experiment.
- **Global context** : the global scientific object RDF graph (`/set/scientific-objects`) store each scientific object global properties (uri, type and name). 
These relations are stored when creating an object into experiment (name and type are copied into global graph), or when directly creating
a scientific object into the global graph.

### In experimental context

If you use the **DELETE** `/core/scientific_object/{uri}` service by providing the `experiment` param, then we consider
the case of deletion in the experimental context. Then the following rules apply specifically :

- An object can't be deleted if it has child. Considering an experiment <xp> An object <child> is a child of the object <parent> if there exist 
a RDF triple `<child> vocabulary:isPartOf <parent>` into the experiment <xp>
- An object can't be deleted if there exists some data associated (within the experiment) to this object into the MongoDB `data` collection
- An object can't be deleted if there exists some data-file associated (within the experiment) to this object into the MongoDB `files` collection

### In global context

If you use the **DELETE** `/core/scientific_object/{uri}` service without providing the `experiment` param, then we consider
the case of deletion in global context. Then the following rules apply specifically : 

- An object can't be deleted if it's used into any experiment
- An object can't be deleted if there exists some data associated (without any experimental) to this object into the MongoDB `data` collection
- An object can't be deleted if there exists some data-file associated (without any experimental) to this object into the MongoDB `files` collection