# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.0.0-rc+1] - 2021-10-28

### Added

- User documentation : 
  - Add a user PDF documentation at homepage 

- Refine organiszation groups:
  - Groups can only be created in the Administration/Groups menu, and they can then be associated with one or more organisations in   the Scientific Organisation/Organisation menu. At the organisation level, it is possible to choose the list of groups associated with the organisation when creating or updating it.

###  Fixed or optimized
 
- Provenance : fix no device selector render when selecting agent which is a type of oeso:SensingDevice. 
- Ergonomic bugs fixes


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
- Organisation (beta version)
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
