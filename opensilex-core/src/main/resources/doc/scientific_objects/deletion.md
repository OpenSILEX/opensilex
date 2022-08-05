Scientific object deletion rules
===================================
**Description**: This document describes which rules apply in case of a scientific object deletion. <br>
**Author**: Renaud COLIN (MISTEA INRAE) <br>
**Date**: 29/07/2022 <br>
**Tags**: [**ScientificObject**, **Delete**]

# Rules
## Introduction

Scientific object can be handled in two context : 
- **Experimental context** : the object is attached into one or multiple experiment and the object properties are stored 
into each corresponding experiment RDF graph. These relations can change from a experiment to another experiment.
- **Global context** : the global scientific object RDF graph (`/set/scientific-objects`) store each scientific object global properties (uri, type and name). 
These relations are stored when creating an object into experiment (name and type are copied into global graph), or when directly creating
a scientific object into the global graph.

## In experimental context

If you use the **DELETE** `/core/scientific_object/{uri}` service by providing the `experiment` param, then we consider
the case of deletion in the experimental context. Then the following rules apply specifically :

- An object can't be deleted if it has child. Considering an experiment <xp> An object <child> is a child of the object <parent> if there exist 
a RDF triple `<child> vocabulary:isPartOf <parent>` into the experiment <xp>
- An object can't be deleted if there exists some data associated (within the experiment) to this object into the MongoDB `data` collection
- An object can't be deleted if there exists some data-file associated (within the experiment) to this object into the MongoDB `files` collection

## In global context

If you use the **DELETE** `/core/scientific_object/{uri}` service without providing the `experiment` param, then we consider
the case of deletion in global context. Then the following rules apply specifically : 

- An object can't be deleted if it's used into any experiment
- An object can't be deleted if there exists some data associated (without any experimental) to this object into the MongoDB `data` collection
- An object can't be deleted if there exists some data-file associated (without any experimental) to this object into the MongoDB `files` collection

## Implementation

Implementation of these rules are into the `ScientificObjectAPI.deleteScientificObject` method