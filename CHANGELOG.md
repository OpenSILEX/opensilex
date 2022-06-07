# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.0.0-rc+4] - 2022-XX-XX
### Added or changed

- Add metrics on experiments and system [metrics services configuration](https://github.com/OpenSILEX/opensilex/blob/master/opensilex-doc/src/main/resources/installation/configuration/metrics.md).
## [1.0.0-rc+3] - 2022-05-05

Be careful, updating to this new version need some manual operations to be done. See [versioning notes](https://github.com/OpenSILEX/opensilex/blob/master/opensilex-doc/src/main/resources/release/1.0.0-rc+3.md).
New installations don't need further action.

### Added or changed

- Display of menu entries can now be configured via user profiles
- Species of an experiment are now automatically deduced by the germplasms of scientific objects
- The geocoding service for address autocompletion can now be specified in the configuration file
- Factor levels can now be exported from the factor detail page
- In the facilities list, facilities can now be filtered by name
- In an experiment or in the global context, data can now be filtered by target uris
- Documents can now reference an external resource by URL instead of uploading a file
- Species can now be updated like any other germplasm
- Documents can now be stored using the GridFS Mongo Connector (see [versioning notes](https://github.com/OpenSILEX/opensilex/blob/master/opensilex-doc/src/main/resources/release/1.0.0-rc+3.md))
- Events and annotations can now be added on a device
- Facilities are now part of their own menu entry
- A new document type "Archive" was added to represent a compressed set of files, where each can be described as a document (but they cannot be downloaded)

### Fixed

- Updated factor categories to be more understandable
- Fixed some inaccurate controls based on user profile and credentials
- Some columns in the event CSV template that were present twice are now shown only once
- The scientific object filter for experiment data now correctly shows the scientific objects of the experiment
- Fixed empty line after adding factor level via CSV
- The address field in facility form now works correctly if the facility has specific properties
- Data visualization from a device now shows variables correctly
- Uniqueness check on data import now takes the device into account
- Fixed a problem on new installations where it was impossible to log in
- Factor levels can now be correctly exported from a set of scientific objects
- Optimized loading of the map, fixed the problem where it would sometimes freeze
- Data can now correctly be filtered by a big number of targets (API : POST /core/data/by_targets)
- Provenances can also be filtered by a big number of targets (API : POST /core/data/provenances/by_targets)
- Filtering documents by keyword now correctly works
- Importing germplasms with specific properties via CSV now works properly; no line are skipped
- Improved data visualization and fixed some display problems
- Fixed the creation of scientific objects with an associated facility (with the property "isHosted")
- Optimized data import, changed data URI generation on import
- Fixed error shown when opening an experiment hosted by at least one facility
- Fixed "reset" button on project list filters 
- Fixed cookie management that could cause multiple problems; including "Request header too long" errors on requests
- Variables can now correctly be sorted in the table view
- Some visual and ergonomic changes

## [1.0.0-rc+2] - 2022-01-23

Be carful, updating to this new version need to execute some actions. See [Versioning notes](https://github.com/OpenSILEX/opensilex/blob/master/opensilex-doc/src/main/resources/release/1.0.0-rc+2.md).
New installations don't need updating action.

### Added
- Concept of "Site" with an address : use W3C vcard specification.
- Filter on variables with filter in entity, entity of interest, characteristic, method, unit, group of variables
- Search in germplasm's attributs in Webapp
- Service in Ontology API to rename an URI.
- New filter on variable in Search Device service, for provenance's agent.
- Append FOAF.rdf ontology
- OpenSILEX migration modules with command.
- Command org.opensilex.migration.GraphAndCollectionMigration for SPARQL graph and MongoDB collection renaming after URI generation update

### Added in pre-production
- Visualisation og device value by variable. See. Variable / visualisation
- Species become optional in an experiment

###  Fixed or improvement
- **WARNING:** Update model graph from sets/<model_name> to set/<model_name>. Update URI generation by using "id" instead of "set" : **this improvement need an action**. See [Versioning notes](https://github.com/OpenSILEX/opensilex/blob/master/opensilex-doc/src/main/resources/release/1.0.0-rc+2.md) if updating.
- **WARNING:** Organization and Facilities ontologies. See [Versioning notes](https://github.com/OpenSILEX/opensilex/blob/master/opensilex-doc/src/main/resources/release/1.0.0-rc+2.md) if updating
- Control access in the Web interface, in progress
- Variable and device association on data creation
- Protect Device from removing when data exists
- Manage ambiguity of variable / device association at data creation
- API JSON message for duplicated attributes, or any kind of errors
- Remove experiment name using into URI generation
- Scientific Object tree filter, in progress
- Bugs fixed :
  - germplasm uri redirecting
  - all device type are linked to its root device type
  - others bugs

### Some Known Issues
- Problem of authentification when migrate. It's due to FOAF import. You need to remove `<http://xmlns.com/foaf/0.1/Agent> rdfs:label "Agent"`
- When installing other version, PHIS ontology is imported. You need to remove <http://www.opensilex.org/vocabulary/oeso-ext> context
- Some installation have problem with geospatial visualisation.

## [1.0.0-rc+1] - 2021-11-02

### Added

- Variable:
  - add concept of **Entity Of Interest** to represent which is characterized by variable.
  - add **Export Variables** functionalities in CSV.

- User documentation at homepage.

- Impove **Organization** in progress:
  - Association of Groups to Organizations: you can now associate some groups with an organization at the creation or update of it.

###  Fixed or optimized

- Provenance : fix no device selector render when selecting oeso:SensingDevice's Agents .
- Ergonomic mistakes fixed


## [1.0.0-rc] - 2021-10-08

### Added

- Data management:
  - Import, export in CSV format
  - A standard provenance for usual provenances or methods
  - Ability to put data on other entities such as sites, areas, factors, etc. except people.  So now, you have to use target instead of scientificObject in the API. This is also the case for importing data via the web interface. See [Versioning notes](https://github.com/OpenSILEX/opensilex/blob/master/opensilex-doc/src/main/resources/release/1.0.0-rc.md) when upgrading.
  - Ability to mention raw data of a measure (i.e. for repetition)


- Variable:
  - Add a species attribute to link a variable to a specific species.
  - Ability to create groups of variables. Add a default group for environmental variables.
  - Prevent incorrect update of variables with data attached
  - Ability to add a document to methods


- Scientific Object:
  - Unicity of a scientific object name in an experiment.


- Map:
  - Ability to specify a geospatial event on Map
  - Ability to see objects over time on Map
  - Export Map in PNG, PDF and Shapefile

- Event:
  - Add a document to an event (image, video, etc.)


- Added system details in the administration menu : instance version, loaded modules, link to web API documentation, etc.


###  Fixed or optimized

- Geospatial filters on the Map
- Optimized vocabulary menu
- Optimized data sorting and pagination
- Improved multiple selection, now you can select 10000 scientific objects
- Improved export and import
- Fixed brapi error
- Various data bug fixes
- Various translation mistakes

### Changed
- Data Model : "scientificObject": "target"
- DataFile Model : "scientificObject": "target"

Link to the upgrade scripts: [1.0.0-rc migration](https://github.com/OpenSILEX/opensilex/blob/master/opensilex-doc/src/main/resources/release/1.0.0-rc.md)


### Major bugs identified

- Problem with filters
- Specific properties of Event are not manage in Map
- Specific properties of Event are not apply on Forms.


********************************************************************************
## [1.0.0-beta+2.1] - 2021-07-27

### Added

- Add global provenances management interface allows to create and visualize them

###  Fixed

- Issue on device association
- Miscellaneous data bugs fixed

********************************************************************************

## [1.0.0-beta+2] - 2021-05-07

### Added

- Geospatial vizualisation improvement with control panel and simple filters
- Global data management : tabular view and datafile view
- Data vizualisation on device, object in or out of experiments : enriched with events view, manual data annotation and event addition.
- Add CHANGLOG.md file to track changes

### Changed

- Data API : list to single scientific object association
- Events improvement : specific properties for class management and optimisation.

********************************************************************************

## [1.0.0-beta+1] - 2021-04-27

### Fixed

- Vocabulary description and Bug correction

********************************************************************************

## [1.0.0-beta] - 2021-04-20

Pre-release of OpenSILEX. New features and improvement. Device, Event, Move, Import and Export Data, etc.

### Added

- Device
- Event
- Global Scientific Object management
- Raw data file management
- Data Import and Export
- All these functionalities are in beta version

### Fixed

- Geospatial visualisation improvement
- Structural Area managment
- Graphic Visualization of a Scientific Object or Device Data
- Organization (beta version)
- Web Services have been homogenised: semantic, fields order, paths, etc.

********************************************************************************

## [1.0.0-alpha] - 2020-11-30

Tag for phis field testers.

### Added

- Users, Groups
- Project, Experiment
- Variable
- Germplasm
- Others functionalities
