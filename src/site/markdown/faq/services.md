OpenSILEX Services - FAQ
================================================================================

## What is a service ?
--------------------------------------------------------------------------------

A service is an interface to an external functionnality.

Services used implementations could be changed by configuration.

Services are injectable automatically in all REST API classes.

4 Main services are provided by default whith an implementation:

- SPARQL Service: RDF4J Service --> Automatically generate SPARQL requests from annotations on models
- Big Data Service: MongoDB Service --> Allow annotation mapping of models
- File Storage Service: Local file storage Service --> Allow file and directories management


## How to create a new service ?
--------------------------------------------------------------------------------

// TODO
