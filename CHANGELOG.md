# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0-rc+6]

### Fixed or optimized

- **[Installation]** The dockerized version of rdf4j-workbench provided with OpenSILEX has been upgraded to version 3.7.7 -> e6bbbaf3
- **[Server CLI]** It is now impossible to create duplicate users by CLI -> d8e10669
- **[Ontology]** Fixed the deletion of custom properties, which would not delete the restriction and cause an error on the next launch -> 44aa61aa
- **[Web Client]** Removed moment.js dependency -> 0a42b846
- **[Web Client]** Selector fields now use the whole line to display item names -> 46957e0f
- **[Web Client]** Selectors that allow to select multiple items no longer reset their selection after performing a second search  -> 46957e0f
- **[Web Client]** The base type is now selectable on multiple forms (scientific objects, events, devices) -> 44aa61aa
- **[Web Client]** Fixed several issues with the modal selector -> 564539a7
- **[Geospatial]** Creation, visualization, modification and deletion of areas now functions properly -> f8416c7c
- **[Geospatial]** Services for managing temporal areas were simplified and manage the associated events. It is no longer needed to call the event
service manually. -> f8416c7c
- **[Data Visualization]** Graphs now take the optimal amount of screen space -> 66428cf5
- **[Data Visualization]** If there is no data to plot, a message is shown and an empty graph is still displayed -> 66428cf5
- **[Variable]** It is no longer possible to delete a variable component if it is associated to a variable -> fbc69422
- **[Variable]** The update of SKOS properties of a variable now works properly -> fbc69422
- **[Variable]** Fixed a wrong link in the variable modal list -> fbc69422
- **[Organization]** The organization / facility / site access check system has been reworked to function properly -> 540f2adc
- **[Germplasm]** Germplasm names no longer need to be unique -> a92e43e3
- **[Germplasm]** More than 20 germplasms can now be exported at the same time -> 4c1e68c2
- **[Event]** CSV template columns are now correctly generated when the event type is `oeev:Move` -> 6c7a8043
- **[Vocabulary]** Default class is now displayed -> 44aa61aa
- **[Data]** Data creation with an incomplete `provUsed` is no longer possible -> 786b4a9a
- **[Scientific Object]** CSV import has been fixed and optimized -> fc5b3041

### Added or changed

- **[Authentication]** SAML protocol can now be used for authentication to supported federations (e.g. RENATER) -> 701f988c
- **[File Storage]** Amazon S3 can now be used for file storage -> 5386906b
- **[Geospatial]** Devices are now displayed on the map -> 93b2c8b7
- **[Data Visualization]** Images associated with data can now be visualized from a data graph -> a48a59a6
- **[Data Visualization]** Filter forms are now reduced after launching the visualization action -> 46957e0f
- **[Experiment / Variable]** Germplasm-related fields are no longer displayed if the germplasm menu is excluded on the instance -> 3652ae43
- **[Experiment]** Added "record author" field on the experiment description -> 3652ae43
- **[Facility]** Associated experiments and devices can now be viewed in the facility details -> 91758cb9

## [1.0.0-rc+5.2]

### Fixed or optimized

[Scientific Object] Fix CSV export on column size -> 9c88f33
[Documentation] Update migration information for OpenSILEX 1.0.0+rc-2 version -> 9c88f33
[Modularity] Fix Extended module build -> 9c88f33
[TypeScript] Remove http import -> a27cf7e
[Documentation] Update Changelog -> a27cf7e

## [1.0.0-rc+5.1]

### Fixed or optimized

Fix OpenSILEX commands initialization and append test scripts -> cd19dd5

## [1.0.0-rc+5]

> Warning : upgrading to this new version may require manual operations. Please
> see the [versioning notes](https://github.com/OpenSILEX/opensilex/blob/master/opensilex-doc/src/main/resources/release/1.0.0-rc+5.md).

> Warning: since version 1.0.0-rc+2, if you have created scientific objects with the same name in different experiments
> (with automatic URI generation), their URI would end up being the same. That may cause problems if you intended to create
> distinct scientific objects, such as some scientific objects being associated with the wrong data.
>
> This bug has been fixed in this version. If you suspect that may have happened to you, please contact us ([opensilex-help@groupes.renater.fr](mailto:opensilex-help@groupes.renater.fr)).

### Added or changed

- **Web client** : Major aesthetics and ergonomics improvement -> 9f4eaf90
- **Web client** : Visualisation of pictures associated to data -> 45166d2b
- **Web client** : Visualisation of experiment data from scientific object and variables c924d260 -> aecfd721
- **Web client** : New tab with associated document into variable details page -> ab7b6bb0
- **Web client** : Variables selection and contextual actions into variable search page -> da82201c
- **Web client**:  Add loader on scientific object tree view loading -> cf864292

- **API/Web client** : A software (Device) can be linked to a variable -> 8d8cfb43
- **API** : Prevent deletion of OS in case of associated experiment, children, data or datafile  -> e17caea7
- **API** : When storing position for a move, we now use a String representation of x,y,z fields instead of
a previous Integer representation -> 9b3a4a27.
- **API** : Add the "Operating procedure" as a new type of technical document -> e39e3c6b
- **API** : Remove opensilex-mobile module from default OpenSILEX modules -> 252e4565

### Fixed or optimized

- **Web client** : Link for "reset password" in the login page now always redirect to the correct page -> f33941bc
- **Web client** : Fixed a problem with a "germplasm" field when editing a scientific object -> df025f7d
- **Web client** : optimize loading of variables and variables group tab. Optimize loading of variables and variable groups in forms -> b00c53ba
- **Web client** : [Data visualization] Optimizations on date formatting into data visualisation -> 687bfcb3
- **Web client** : several fixes for the map visualisation -> 9b3a4a27
- **Web client** : [Geospatial] Display calibration position & enhance multi select -> 73811bcb
- **Web client** : Fix variable name display on tabular data list -> fd3ea6be

- **API** : Variables associated with a variable of type "date" are now stored and exported correctly -> 624e99dd
- **API** : Better error handling in case of Invalid character when evaluating a SPARQL REGEX -> 326be84c
- **API** : Fix URI generation when providing a URI on scientific object creation and CSV import -> e17caea7 , a77a3629
- **API** : Remove no duplicate name constraint when importing OS into global graph -> 45bcf04a
- **API** : Optimize search and loading for entities, characteristics, methods, units, variables and variable groups -> b00c53ba
- **API** : Fix deletion and existence checking for gridfs file-system connection -> e1034199

## [1.0.0-rc+4.1] - 2022-07-13

### Fixed

- Fix opensilex install command
- Install will now create a repository with graphdb triplestore

## [1.0.0-rc+4] - 2022-06-13

Warning : upgrading to this new version requires to update the ontologies. Please run the following command
to make sure they are up-to-date :

```shell
# From the directory which contains your OpenSILEX executable .jar file
java -jar opensilex.jar --CONFIG_FILE=<config_file> sparql reset-ontologies
```

### Added or changed

- Add Device type and specifics properties management driven by ontologies: import template generation and driven forms.
- The variable selector has been enhanced; variables are now selected through a modal form where they can be filtered by name, entity, characteristic, etc.
- Variables can now be filtered by datatype and time interval.
- When a groups of variables are created or Updated, there are a redirection on detail part of the group.
- Associated experiments are now listed in the project description.

### Fixed

- Events can now be correctly filtered by end date (even if multiple events have the same end date).
- Germplasm search and export have been optimized.
- Fixed some variable bugs: filters, display of species, datatype and time interval translation.
- Data exportation bug about  column offset.
- Fix germplasm export: optimize search and export in order to avoid timeout interruption.
- Improve specific properties management of scientific objects.
- Fix bug on ResetOntologies for not completed classes, properties ou restrictions.

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

### Fixed or improvement

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

### Fixed or optimized

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

### Fixed or optimized

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

### Fixed

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
