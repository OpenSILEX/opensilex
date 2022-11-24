# Graph Organization

**Description**: This document describes how and where are stored instances of main concept of opensilex. <br>
**Author**: Renaud COLIN (MISTEA INRAE) <br>
**Date**: 24/11/2022 <br>


# Introduction

OpenSILEX rely on RDF named graph in order to organize storage of different concepts instances.
The two main reasons are : 
- A better organization, which allow advanced users and developers to navigate through graph in order to visualize
and understand data. Which is not possible if all triple are into a single graph
- An optimization for read/search, since SPARQL queries which applies only on some concept, 
can apply on a limited number of data.


# Storage

## Ontologies

Each ontology used by an OpenSILEX module is stored into its own graph, the following table list the ontologies
used, which module import it, and the corresponding graph in which ontology is stored

| Ontology                        | Prefix             | Graph                                    | OpenSILEX module   |
|---------------------------------|--------------------|------------------------------------------|--------------------|
| OpenSILEX ontology              | oeso or vocabulary | http://www.opensilex.org/vocabulary/oeso | opensilex-core     |
| Ontology of Experimental Events | oeev               | http://www.opensilex.org/vocabulary/oeev | opensilex-core     |
| OpenSILEX - Security            | os-sec             | http://www.opensilex.org/security        | opensilex-security |
| Web Annotation Ontology         | oa                 | http://www.w3.org/ns/oa                  | opensilex-core     |
| Time Ontology                   | time               | http://www.w3.org/2006/time              | opensilex-core     |
| OWL2 Ontology                   | owl                | http://www.w3.org/2002/07/owl            | opensilex-core     |
| Dublin core vocabulary          | dc                 | http://purl.org/dc/terms/                | opensilex-core     |
| FOAF ontology                   | foaf               | http://xmlns.com/foaf/0.1/               | opensilex-security |


## Main concept storage

OpenSILEX store each concept into a separate graph, the URI of the graph depends on the baseURI defined
into the OpenSILEX configuration.

Example considering the following configuration for SPARQL : 

```yaml
ontologies:
    baseURI: http://www.opensilex.org/
    baseURIAlias: opensilex
```

Each graph will have the following form : `http://www.opensilex.org/set/<graph_suffix_for_concept>`

The following table list all graph used by OpenSILEX

| Concept                | Concept URI                 | Graph suffix      | Graph URI                                      |
|------------------------|-----------------------------|-------------------|------------------------------------------------|
| **User**               | foaf:Agent                  | user              | http://www.opensilex.org/set/user              |
| **Group**              | os-sec:Group                | group             | http://www.opensilex.org/set/group             |
| **Profile**            | os-sec:Profile              | profile           | http://www.opensilex.org/set/profile           |
| **Group user profile** | os-sec:GroupUserProfile     | group             | http://www.opensilex.org/set/group             |
| **Project**            | vocabulary:Project          | project           | http://www.opensilex.org/set/project           |
| **Experiment**         | vocabulary:Experiment       | experiment        | http://www.opensilex.org/set/experiment        |                                  |
| **Facility**           | vocabulary:Facility         | organization      | http://www.opensilex.org/set/organization      |                                  |
| **Organization**       | foaf:Organization           | organization      | http://www.opensilex.org/set/organization      |                                  |
| **Variable**           | vocabulary:Variable         | variable          | http://www.opensilex.org/set/variable          |                                  |
| **Entity**             | vocabulary:Entity           | variable          | http://www.opensilex.org/set/variable          |                                  |
| **Method**             | vocabulary:Method           | variable          | http://www.opensilex.org/set/variable          |                                  |
| **Characteristic**     | vocabulary:Characteristic   | variable          | http://www.opensilex.org/set/variable          |                                  |
| **Unit**               | vocabulary:Unit             | variable          | http://www.opensilex.org/set/variable          |                                  |
| **Variable group**     | vocabulary:VariablesGroup   | variablesGroup    | http://www.opensilex.org/set/variablesGroup    |                                  |
| **Species**            | vocabulary:Species          | gerplasm          | http://www.opensilex.org/set/gerplasm          |                                  |
| **Germplasm**          | vocabulary:Germplasm        | germplasm         | http://www.opensilex.org/set/gerplasm          |                                  |
| **Factor**             | vocabulary:Factor           | factor            | http://www.opensilex.org/set/factor            |                                   |
| **Factor level**       | vocabulary:FactorLevel      | factor            | http://www.opensilex.org/set/factor            |                                  |
| **Document**           | vocabulary:Document         | document          | http://www.opensilex.org/set/document          |                                  |
| **Event**              | oeev:Event                  | event             | http://www.opensilex.org/set/event             |                                  |
| **Annotation**         | oa:Annotation               | annotation        | http://www.opensilex.org/set/annotation        |                                  |
| **Scientific Object**  | vocabulary:ScientificObject | scientific-object | http://www.opensilex.org/set/scientific-object |                                  |
| **Device**             | vocabulary:Device           | device            | http://www.opensilex.org/set/device            |                                  |
| **Area**               | vocabulary:Area             | area              | http://www.opensilex.org/set/area              |                                  |

## Experimental context

In addition of the previous rules, OpenSILEX use a graph by experiment, in order to store scientific object properties
which depends on an experimental context.

In other word, for each object involved into an experiment, all object properties are stored inside the experiment graph.
The relation for the two following properties : `rdf:type` and `rdfs:label` are also present into the global Scientific Object graph. 

See `opensilex-core/src/main/resources/doc/scientific_objects/uri_generation/uri_generation.md` for more information about 
scientific object management
