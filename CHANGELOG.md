<!-- TOC -->
* [Changelog](#changelog)
  * [[1.2.3]](#123)
    * [Highlight](#highlight)
    * [Changes and new features](#changes-and-new-features)
    * [Fixed or optimized](#fixed-or-optimized)
  * [[1.2.2]](#122)
    * [Fixed](#fixed)
  * [[1.2.1]](#121)
    * [Fixed](#fixed-1)
  * [[1.2.0] - Caramelized Crystal](#120---caramelized-crystal)
    * [Highlight](#highlight-1)
    * [Changes and new features](#changes-and-new-features-1)
    * [Fixed or optimized](#fixed-or-optimized-1)
    * [Versions changes](#versions-changes)
    * [Technical changes](#technical-changes)
      * [Updated Dependencies](#updated-dependencies)
  * [[1.1.0] - Blazing Basalt](#110---blazing-basalt)
    * [Changes and new features](#changes-and-new-features-2)
    * [Fixed or optimized](#fixed-or-optimized-2)
  * [[1.0.1] - Ambitious Amber](#101---ambitious-amber)
    * [Fixed or optimized](#fixed-or-optimized-3)
      * [API](#api)
      * [Web client](#web-client)
  * [[1.0.0] - Ambitious Amber](#100---ambitious-amber)
    * [New features](#new-features)
    * [Fixed or optimized](#fixed-or-optimized-4)
      * [API](#api-1)
      * [Web client](#web-client-1)
  * [[1.0.0-rc+7]](#100-rc7)
    * [New features](#new-features-1)
    * [Fixed or optimized](#fixed-or-optimized-5)
  * [[1.0.0-rc+6]](#100-rc6)
    * [Fixed or optimized](#fixed-or-optimized-6)
    * [Added or changed](#added-or-changed)
  * [[1.0.0-rc+5.2]](#100-rc52)
    * [Fixed or optimized](#fixed-or-optimized-7)
  * [[1.0.0-rc+5.1]](#100-rc51)
    * [Fixed or optimized](#fixed-or-optimized-8)
  * [[1.0.0-rc+5]](#100-rc5)
    * [Added or changed](#added-or-changed-1)
    * [Fixed or optimized](#fixed-or-optimized-9)
  * [[1.0.0-rc+4.1] - 2022-07-13](#100-rc41---2022-07-13)
    * [Fixed](#fixed-2)
  * [[1.0.0-rc+4] - 2022-06-13](#100-rc4---2022-06-13)
    * [Added or changed](#added-or-changed-2)
    * [Fixed](#fixed-3)
  * [[1.0.0-rc+3] - 2022-05-05](#100-rc3---2022-05-05)
    * [Added or changed](#added-or-changed-3)
    * [Fixed](#fixed-4)
  * [[1.0.0-rc+2] - 2022-01-23](#100-rc2---2022-01-23)
    * [Added](#added)
    * [Added in pre-production](#added-in-pre-production)
    * [Fixed or improvement](#fixed-or-improvement)
    * [Some Known Issues](#some-known-issues)
  * [[1.0.0-rc+1] - 2021-11-02](#100-rc1---2021-11-02)
    * [Added](#added-1)
    * [Fixed or optimized](#fixed-or-optimized-10)
  * [[1.0.0-rc] - 2021-10-08](#100-rc---2021-10-08)
    * [Added](#added-2)
    * [Fixed or optimized](#fixed-or-optimized-11)
    * [Changed](#changed)
    * [Major bugs identified](#major-bugs-identified)
  * [[1.0.0-beta+2.1] - 2021-07-27](#100-beta21---2021-07-27)
    * [Added](#added-3)
    * [Fixed](#fixed-5)
  * [[1.0.0-beta+2] - 2021-05-07](#100-beta2---2021-05-07)
    * [Added](#added-4)
    * [Changed](#changed-1)
  * [[1.0.0-beta+1] - 2021-04-27](#100-beta1---2021-04-27)
    * [Fixed](#fixed-6)
  * [[1.0.0-beta] - 2021-04-20](#100-beta---2021-04-20)
    * [Added](#added-5)
    * [Fixed](#fixed-7)
  * [[1.0.0-alpha] - 2020-11-30](#100-alpha---2020-11-30)
    * [Added](#added-6)
<!-- TOC -->

# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.2.3]

>  ⚠️ WARNING : upgrading to this new version require manual operations. Please
> see the [versioning notes](https://github.com/OpenSILEX/opensilex/blob/master/opensilex-doc/src/main/resources/release/1.2.3.md).

### Highlight

- Some data-related pages and services have been **greatly optimized**, especially for OpenSILEX instances where a 
  lot of data are declared (> 10M).
- You can now configure a **custom notification banner** that will be displayed at the top of the web interface. This 
  can be used to inform your users of maintenances, changes, etc. See the [configuration instructions](opensilex-doc/src/main/resources/installation/configuration/notification-pannel.md).

### Changes and new features

- (!1167) Changed type of publication date and last modification date from `Integer` to `Date` when it wasn't already the case. This improves  
  uniformity and client support.
  - Concerned services :
    - Data
    - Provenance
    - Datafile
- (!1168) Improvement on the "move" type event declaration.
  - Additional information for the user in the form.
  - Dynamic attribute `required` for location fields.
  - An error message is now returned if the move is incorrectly declared, even though nothing was indicated and nothing happened before.
- (!1175) Added a message notification system to users.
  - Message can be translated into different languages.
  - Possibility of adding HTML tags in the message.
  - Several color themes available.
  - Possibility of defining an end date for display.
  - See the [configuration instructions](opensilex-doc/src/main/resources/installation/configuration/notification-pannel.md).
- (!1178) You can no longer select an end date that is earlier than the start date during Event creation. The target 
  column of the Event list now redirects to the resource in question upon click.
- (!1180) The Event import system now works the same way as ScientificObjects and Devices, the headers of columns are now short uris instead of labels.
- (!1185) Replaced simple germplasm selector in the ScientificObjects tab of Experiments with a pop-up one.
- (!1186) Creation of a new default order for ontology properties : properties from higher up in the hierarchy get shown first, all the way down to properties from the lowest domain in the hierarchy. Within the sublist of properties from a single domain, they are ordered alphabetically (by name).
  - Usage of this new order, if no order has ever been defined for CSV template generation.
  - Usage of this new order, if no order has ever been defined for Ontology based forms.
- (!1195) Germplasm and Germplasm Group filter no longer treated as a logical OR with the targets filter
- (!1203) The details tab is now the first and default tab for facilities instead of monitoring

### Fixed or optimized

- (!1166) Fix concerning the dates of an event.
  - If the event is instantaneous, the end date is now required.
  - If it is on a time interval, at least one of the two fields (start / end) is required.
- (!1169) Fix concerning the data import. If the standard provenance is not created, it is no longer possible to insert data by selecting it, a specific error message is returned to the user in this scenario.
- (!1183) Visibility of areas and devices on the map is now stable
- (!1190) Data and datafile table pages now render correctly when every data or datafile in the table has an undefined target
- (!1194) In the create germplasm view, when loading a heavy CSV file, the table is now filled in less than 2 seconds
  and is showing a loader.
- (!1197) Optimisation of data search.
  - A new optimized `searchDataListByTargets` service is available. This service no longer compute the `totalCount` of data and just indicate if a next page exists.
  - All data search service no longer return detailed information about data publisher (only the publisher URI is returned).
  - Two new MongoDB indexes are created on data experiment and data provenance URI.
- (!1198) fixed a bug that prevented Move from being declared if no position information was provided
- (!1207) Inserting scientific objects in an experiment, when they were already present in another experiment,
  using an import file now works correctly again.
- (!1212) Device filter no longer ignored during data export.

## [1.2.2]

### Fixed

- [Doc] Changelog and versioning notes was changed to avoid deployment and configuration issues

## [1.2.1]

### Fixed

- [Map] When we open the "map" module, the focus is now on the correct extent of the scientific objects of the experiment.
- [Experiment] Scientific objects are now displayed in a hierarchy again by default, instead of a flat table.
  Flatten display will now only trigger when searching using certain filters, such as data criteria.

## [1.2.0] - Caramelized Crystal

> WARNING : upgrading to this new version requires manual operations. Please
> see the [versioning notes](https://github.com/OpenSILEX/opensilex/blob/master/opensilex-doc/src/main/resources/release/1.2.0.md).
> Also available [locally](opensilex-doc/src/main/resources/release/1.2.0.md).

### Highlight

- [General] Every resource now has associated metadata linking to its creator (account), its date of creation and its 
  last update date. This metadata is usually displayed in the detailed view of a resource. -> bf721199
- [Scientific Object] You can now search scientific objects by applying mathematical filters on their associated 
  data. For example, you can search "all scientific objects with recorded data for the variable 'air_temperature' where
  the value is between 10°C and 20°C". -> b4d06f44
- [Person] You can now create a person based on an existing ORCID account. All relevant data will be automatically 
  fetched to fill the person's info. -> 3361a1c8
- [RGPD] You can now display your RGPD file as a PDF in the web interface. See [configuration instructions](opensilex-doc/src/main/resources/installation/configuration/GDPR_config.md) -> cc2a416b

### Changes and new features

- [Person] Enhanced the ergonomy of the "phone number" field for the person form. -> c9238820
- [Data] You can now search data by the germplasm group of their target. -> 500c2f9e
- [Ergonomy] You can now choose the number of results displayed in search result tables. -> 26158449
- [Ergonomy] In selectors, you can now expand the displayed results that were previously capped at 10 items. -> 3ccabae5
- [Security] There are now credentials to manage accounts. -> 0865dd7c
- [Device] You can now filter the data in the device graphs by target. -> 22462def
- [Graphs] The default visualization date for data has been adjusted. -> 89c02765
  - For devices, variables or scientific objects graphs, the default beginning date is now 15 days before the current 
    date.
  - In the case of a finished experiment, the default dates are those of the experiment.
- [Analytics] You can now configure an OpenSILEX instance to send analytics data to a Matomo server. -> 90177a11
- [Germplasm] The germplasm model now supports 3 additional properties, `has_parent`, `has_parent_m` and 
  `has_parent_f`. You can also search germplasms using these properties as filters. -> 1f107ee0
- [Dashboard] You can now configure your dashboard to display a custom title to your graph. See [configuration instructions](opensilex-doc/src/main/resources/installation/configuration/dashboard.md#configuration-of-additional-information-for-the-data-visualization) -> a1e37f00
- [Map] You can now export elements from the map in the GeoJSON format. -> da13f442
- [Documentation] The installation documentation was updated. -> 9368839e

### Fixed or optimized

- [Ergonomy] When going back to a previously visited list, the pagination is kept. -> 62a6fb26
- [Metrics] The metrics service now returns correctly the variable names. -> 4ab1fa1a
- [Variable group] The "variable group" selector now works correctly for multiple selection. -> 40dfd0fc
- [BrAPI] Missing mappings for BrAPI 1.3 were added, and other mappings that returned an incorrect format were fixed.
  -> 3721ad9f
- [Table list] In table lists where items can be selected, the selection count is now correct even when a selected 
  item is deleted. -> a45111a7
- [Germplasm] You can now correctly import multiple germplasms with the same name using a CSV file. -> 16228e14
- [Data] Importing data when the target column is the last column now works correctly. -> fded1f4d
- [Site] The link to a hosted organization from a site now redirects correctly. -> b2c8ef07
- [Account] You can no longer delete an account while it is associated to other data. -> 3abe2506

### Versions changes

> Java 8 to Java 11

- [**WARNING**] This requires to have an installed JRE with a version >= 11 on the server host which run OpenSILEX
- Ensures that the JRE is well installed before upgrading your OpenSILEX version

- You can check the documentation [here](https://www.digitalocean.com/community/tutorials/how-to-install-java-with-apt-on-ubuntu-18-04-fr)
if your need more information or contact our team for support.

> Docker mongodb

- The dockerfile for the usage mongodb in a development environnement now rely on the
  version [6.0.4](https://hub.docker.com/layers/library/mongo/6.0.4/images/sha256-3c8dd1b08f8a2ec0338902affd432b40130e5acf49d6e3a1ca05ff5168100059?context=explore)
  of mongodb
- See the
  dockerfile :  [mongo-with-transactions.docker](opensilex-dev-tools/src/main/resources/docker/mongo-with-transactions.docker)

### Technical changes

- [GraphQL] A service wa added to generate a Staple API configuration file. -> 73b2d8ad

#### Updated Dependencies

> Maven plugins

| Dependency                                                                         | Old version | New version | Notes                          |
|------------------------------------------------------------------------------------|-------------|-------------|--------------------------------|
| [deploy](https://maven.apache.org/plugins/maven-deploy-plugin/)                    | 3.0.0       | 3.1.1       |                                |
| [install](https://maven.apache.org/plugins/maven-install-plugin/)                  | 3.0.0       | 3.1.1       |                                |
| [enforcer](https://maven.apache.org/plugins/maven-enforcer-plugin/)                | 3.0.0       | 3.1.1       |                                |
| [surefire](https://maven.apache.org/plugins/maven-surefire-plugin/)                | 3.0.0       | 3.1.1       |                                |
| [failsafe](https://maven.apache.org/plugins/maven-failsafe-plugin/)                | 3.0.0       | 3.1.1       |                                |
| [site](https://maven.apache.org/plugins/maven-site-plugin/)                        | 3.8.2       | 3.12.1      |                                |
| [version](https://maven.apache.org/plugins/maven-version-plugin/)                  | 2.7         | 2.16.0      |                                |
| [plugin-api](https://mvnrepository.com/artifact/org.apache.maven/maven-plugin-api) | 3.6.0       | 3.9.0       | Maven plugin in swagger module |

> Others Dependencies

| Dependency                                                                                           | Old version | New version | Notes                                    |
|------------------------------------------------------------------------------------------------------|-------------|-------------|------------------------------------------|
| [caffeine](https://mvnrepository.com/artifact/com.github.ben-manes.caffeine/caffeine/)               | 3.1.1       | 3.1.8       |                                          |
| [bcrypt](https://mvnrepository.com/artifact/at.favre.lib/bcrypt/0.10.2)                              | 0.9.0       | 0.10.2      |                                          |
| [jwt](https://mvnrepository.com/artifact/com.auth0/java-jwt)                                         | 3.11.0      | 4.4.0       |                                          |
| [picocli](https://mvnrepository.com/artifact/com.auth0/java-jwt)                                     | 4.6.3       | 4.7.5       |                                          |
| [jena.query.builder](https://mvnrepository.com/artifact/org.apache.jena/jena-querybuilderwt)         | 4.5.0       | 4.9.0       |                                          |
| [bytebuddy](https://mvnrepository.com/artifact/org.apache.jena/jena-querybuilderwt)                  | 1.10.18     | 4.9.0       |                                          |
| [httpcore](https://mvnrepository.com/artifact/org.apache.httpcomponents/httpcore)                    | 4.4.14      | 4.4.16      |                                          |
| [httpclient](https://mvnrepository.com/artifact/org.apache.httpcomponents/httpclient)                | 4.5.13      | 4.5.14      |                                          |
| [apache-tika](https://mvnrepository.com/artifact/org.apache.tika/tika-core)                          | 2.3.0       | 2.9.1       |                                          |
| [apache-commons-io](https://mvnrepository.com/artifact/commons-io/commons-io)                        | 2.11.0      | 2.13.0      |                                          |
| [mongodb-driver-sync](https://mvnrepository.com/artifact/org.mongodb/mongodb-driver-sync/)           | 4.5.0       | 4.11.0      |                                          |
| [mongodb-embed (for-test)](https://mvnrepository.com/artifact/org.mongodb/mongodb-driver-sync/)      | 3.4.2       | 3.5.4       | Update mongo test version 5.0.6 to 6.0.2 |
| [glassfish-jersey*](https://mvnrepository.com/artifact/org.glassfish.jersey.core)                    | 2.3.5       | 2.3.7       |                                          |
| [swagger.jersey2.jaxrs](https://mvnrepository.com/artifact/io.swagger/swagger-jersey2-jaxrs)         | 1.6.2       | 1.6.5       |                                          |
| [git-commit-id-plugin](https://mvnrepository.com/artifact/pl.project13.maven/git-commit-id-plugin)   | 4.0.0       | 4.0.5       |                                          |
| [tomcat-embed-core](https://mvnrepository.com/artifact/org.apache.tomcat.embed/tomcat-embed-core)    | 9.0.39      | 9.0.82      |                                          |
| [jackson*](https://mvnrepository.com/artifact/com.fasterxml.jackson)                                 | 2.13        | 2.15.3      |                                          |
| [janino](https://mvnrepository.com/artifact/org.codehaus.janino/janino)                              | 3.1.6       | 3.1.9       |                                          |
| [awaitility](https://mvnrepository.com/artifact/org.awaitility/awaitility)                           | 4.1.1       | 4.2.0       |                                          |
| [logstash-encoder](https://mvnrepository.com/artifact/net.logstash.logback/logstash-logback-encoder) | 7.0.1       | 7.4         |                                          |

> Removed dependencies

| Name                                                                                                           | Note |
|----------------------------------------------------------------------------------------------------------------|------|
| [medeia-validator-jackson](https://mvnrepository.com/artifact/com.worldturner.medeia/medeia-validator-jackson) |      |

## [1.1.0] - Blazing Basalt

>  ⚠️ WARNING : upgrading to this new version require manual operations. Please
> see the [versioning notes](https://github.com/OpenSILEX/opensilex/blob/master/opensilex-doc/src/main/resources/release/1.1.0.md).
> Also available [here](opensilex-doc%2Fsrc%2Fmain%2Fresources%2Frelease%2F1.1.0.md)

### Changes and new features

- [Map] You can now visualize data in a chart by selecting an object in the map view
- [Person] You can now search persons by their ORCID
- [User] The User API is now deprecated
- [Account] The form for creating accounts has been simplified
- [Experiment] Associated facilities are now part of the available facilities for scientific objects of the experiments
- [Experiment] The "Visualization" tab of a scientific object in an experiment has been removed
- [Experiment] Scientific object count is now displayed on the scientific objects tab
- [Germplasm group] Added documentation that you can read by hovering the question mark

### Fixed or optimized

- [Front] Minor visual fixes
- [Experiment] On the "Data" page of an experiment, targets are now always correctly displayed
- [Data] Importing multiple variables in one CSV no longer duplicates devices in provenance
- [Data] Adding annotation during data import now uses the correct date and time
- [Map] Fixed some issues with the zoom
- [Organization] Fixed an issue where you had to click twice on "create organization" or "create site" to perform the action
- [Facility] Tweaked the "Monitoring" page of a facility for better ergonomy
- [Charts] Tweaked the display of graphs with multi-scale axis for better ergonomy
- [Charts] Fixed a bug where data with the value 0 where sometimes displayed as undefined
- [Charts] Charts no longer aggregate data points
- [PHIS] PHIS logo is now displayed if the dashboard chart is not configured

## [1.0.1] - Ambitious Amber

### Fixed or optimized

#### API

- [Person/User] Removed User and Person delete web services

#### Web client

- [Person/Account] Removed Account and Person delete buttons
- [Profiles] Added translation for Dataverse credentials menu

## [1.0.0] - Ambitious Amber

> WARNING : upgrading to this new version require manual operations. Please.
> See the [versioning notes](https://github.com/OpenSILEX/opensilex/blob/master/opensilex-doc/src/main/resources/release/1.0.0.md).
> Also available [here](opensilex-doc/src/main/resources/release/1.0.0.md).

### New features

- [MongoDB] You can now specify credentials to connect to a MongoDB server -> d02b3c8d
- [Facility] Environmental data can be visualized in the facility detail page, including mean and median graphs ->
  d9a705bf
- [Data] Added new filters for data search : devices, facilities, operators -> ea0cc36b
- [Data] Scientific objects can now be annotated during data import by adding an "annotation" column in the CSV ->
  816f5ecf
- [Map] Various enhancements -> 89ce4fa6
- [Map] Scientific objects can now be exported as Shapefiles with the selected properties -> fa9fb97e
- [Germplasm] Germplasm groups can be created to manage a set of germplasms at the same type -> bc29624d
- [User / Person / Account] The web interface now reflects the changes in the API, where the `User` concept was split in
  two `Person` and `Account` concepts -> b8457f7b
- [Person] A person can now have an ORCID, a phone number and an organization -> 0718f5e5
- [Person] Several links to Users are replaced by links to Persons (for projects, experiments, provenances and devices)
- [Variable] Added a "not included in group" filter for searching variables -> 98fde231
- [Experiment] Added a germplasm filter for searching scientific objects in an experiment -> 25f5c93f
- [API] All resources now have a publisher, publication date and last update date -> 2a3140bd
- [Dashboard] You can now display a media in the dashboard if the graph component is not configured -> d1aa16ef
- [Recherche Data Gouv] Added the possibility to create a `Dataset` draft on Recherche Data Gouv from OpenSILEX ->
  63251bc2

### Fixed or optimized

#### API

- [Data] Fixed export when sending too many parameters (e.g. a variable URI list) -> 1bc02a68
- [Data] Exporting data with a "long" format without selecting a variable first now correctly works -> 1bc02a68
- [API] Services returning a paginated list now only returns the list once -> e1b01648
- [Event] When importing a CSV file containing events, the beginning and end date are now checked to be coherent ->
  94cf74db
- [Organization] Organization and site search now uses an internal cache to improve performance -> 2f077837
- [Variable] SKOS references are now included in detailed export -> aa969f05
- [Variable] Fixed some errors when using variables from a shared resource instance -> b18d55fd, cd848c61
- [SPARQL] Namespaces defined in the triple store are now taken into account by the app -> 2df69a7e
- [Facility] Facilities can now have a custom geometry -> 152efe01
- [Ontology] The `GET /uri_label` service new returns a 404 when the URI or label is not found ->  4c664cdf

#### Web client

- Multiple ergonomic changes -> 05162e38, 2bc8dce7, b4f72a5a

- [Experiment] Fixed "isPartOf" relation when creating a Scientific Object with the "+" button in experiment -> 46211dbc
- [Visualization] Graphs now display even if the data series contains only NaN values -> 4e1ce841
- [Forms] Date selectors now checks that the end date is after the beginning date -> 2d8c1da0
- [Details] Incoming custom properties are now clearly displayed in a distinct section (for scientific objects and
  facilities) -> 167130fb
- [Variable] Changing the source instance now reloads all the filters correctly -> e5997c5d
- [Data] Added an indication to the CSV template to specify the expected format when using a boolean variable ->
  e318a0ea
- [Datafile] Image datafiles can now be visualized without problems -> 28c38f05
- [Map] When zooming out, scientific objects are grouped into clusters to enhance performance and visibility -> c394c7fc
- [Person] Enhanced display of persons of contact (for accounts, projects, experiments, devices and provenances) ->
  70daab17
- [Germplasm] Searching a germplasm by URI now works correctly -> aad496dc

## [1.0.0-rc+7]

> WARNING : upgrading to this new version require manual operations. Please
> see the [versioning notes](https://github.com/OpenSILEX/opensilex/blob/master/opensilex-doc/src/main/resources/release/1.0.0-rc+7.md).
> Also available [here](opensilex-doc/src/main/resources/release/1.0.0-rc%2B7.md)

### New features

- **[Dashboard]** Add a dashboard functionality for visualizing data, scientific objects, devices and germplasm
  statistics -> 38970ef6
- **[Variables]** Allow to browse and import variables from a remote instance ("shared resource instance") -> 5aa3e0482
- **[UI]** Indication of elements number in tabs -> 5eb24aac4
- **[UI]** Enter Key press no longer necessary in auto-complet fields -> 7dc8ff333
- **[UI]** Add an OpenSILEX neutral UI theme -> fd2706c1
- **[Germplasm]** Append search by attribute values on front -> a1425b118
- **[Position]** Display the Devices/OS LastPosition on detail and map page -> 3cf14f8e2
- **[Facilities]** Sites can now be associated to a facility from the facility creation or update form -> 845f3d1d
- **[Data/Image]** : Visualization of Images associated to the data since from experiment -> 169cbc31d
- **[Users]** Data model has been changed, old users are now split into accounts and persons -> 8ed0303a
- **[Users]** Add a command for creating a guest account -> fd2706c1

### Fixed or optimized

- **[Ontology]** move some subclass and properties from oeso-ext ontology (phis) to oeso-core ontology -> ac1a451c3
- **[Data]** Fix bad redirection of scientific object inside experimental context. Fix display error in case of data
  with no target defined -> d9830177d
- **[Experiment/Visualisation]** Fix graphic visualizations in experiment module -> 43dbfe29d
- **[Provenance]** Fix creation of provenance with an operator -> abd7ba2a9
- **[Event]** Append Fertilization event french translation -> 2dd981f06
- **[Scientific Object]** fix storage/search of name composed only of digit in global graph -> 3268a4bf3
- **[Scientific Object]** fix germplasm and custom properties export -> 0ede9443d
- **[Move/Location]** Enhance OS positions historic -> 89553d5e0
- **[Scientific Object]** new rooting for SO tabs / fix links redirect -> c37bc6273
- **[Scientific Objects]** Fix SO form if it contains subtype properties -> 4d8b93899
- **[FileSystem]** Fix import from local files to mongodb import -> 9436ce94d
- **[Scientific Object]** fix no display of object name for each experiment (if name are equals in global and
  experimental context) -> 1eeff0fcd
- **[Germplasm]** Fix search by attributes and attributes list display -> 7b71920d
- **[Scientific Object]** Fix form if it contains custom/subtype properties -> 4d8b9389
- **[FileSystem]** Fix import from local files to MongoDB import -> 9436ce94
- **[Germplasm]** Fix search by attributes and attributes list display -> 7b71920d
- **[Document]** Fix error on Document creation when using a fixed URI -> 6e766fbe
- **[Facility]** Fix display of associated device -> 465c364f
- **[User/Account]** Extract Account and Person from User -> 8ed0303a
- **[Facility]** Add tabs for facility view -> 2239fb02
- **[Germplasm]** Allow duplicate names for accessions -> 42399304
- **[Organizations]** The "create facility" button on site details now works as expected -> 845f3d1d

## [1.0.0-rc+6]

### Fixed or optimized

- **[Installation]** The dockerized version of rdf4j-workbench provided with OpenSILEX has been upgraded to version
  3.7.7 -> e6bbbaf3
- **[Server CLI]** It is now impossible to create duplicate users by CLI -> d8e10669
- **[Ontology]** Fixed the deletion of custom properties, which would not delete the restriction and cause an error on
  the next launch -> 44aa61aa
- **[Web Client]** Removed moment.js dependency -> 0a42b846
- **[Web Client]** Selector fields now use the whole line to display item names -> 46957e0f
- **[Web Client]** Selectors that allow to select multiple items no longer reset their selection after performing a
  second search -> 46957e0f
- **[Web Client]** The base type is now selectable on multiple forms (scientific objects, events, devices) -> 44aa61aa
- **[Web Client]** Fixed several issues with the modal selector -> 564539a7
- **[Geospatial]** Creation, visualization, modification and deletion of areas now functions properly -> f8416c7c
- **[Geospatial]** Services for managing temporal areas were simplified and manage the associated events. It is no
  longer needed to call the event
  service manually. -> f8416c7c
- **[Data Visualization]** Graphs now take the optimal amount of screen space -> 66428cf5
- **[Data Visualization]** If there is no data to plot, a message is shown and an empty graph is still displayed ->
  66428cf5
- **[Variable]** It is no longer possible to delete a variable component if it is associated to a variable -> fbc69422
- **[Variable]** The update of SKOS properties of a variable now works properly -> fbc69422
- **[Variable]** Fixed a wrong link in the variable modal list -> fbc69422
- **[Organization]** The organization / facility / site access check system has been reworked to function properly ->
  540f2adc
- **[Germplasm]** Germplasm names no longer need to be unique -> a92e43e3
- **[Germplasm]** More than 20 germplasms can now be exported at the same time -> 4c1e68c2
- **[Event]** CSV template columns are now correctly generated when the event type is `oeev:Move` -> 6c7a8043
- **[Vocabulary]** Default class is now displayed -> 44aa61aa
- **[Data]** Data creation with an incomplete `provUsed` is no longer possible -> 786b4a9a
- **[Scientific Object]** CSV import has been fixed and optimized -> fc5b3041

### Added or changed

- **[Authentication]** SAML protocol can now be used for authentication to supported federations (e.g. RENATER) ->
  701f988c
- **[File Storage]** Amazon S3 can now be used for file storage -> 5386906b
- **[Geospatial]** Devices are now displayed on the map -> 93b2c8b7
- **[Data Visualization]** Images associated with data can now be visualized from a data graph -> a48a59a6
- **[Data Visualization]** Filter forms are now reduced after launching the visualization action -> 46957e0f
- **[Experiment / Variable]** Germplasm-related fields are no longer displayed if the germplasm menu is excluded on the
  instance -> 3652ae43
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
> (with automatic URI generation), their URI would end up being the same. That may cause problems if you intended to
> create
> distinct scientific objects, such as some scientific objects being associated with the wrong data.
>
> This bug has been fixed in this version. If you suspect that may have happened to you, please contact
> us ([opensilex-help@groupes.renater.fr](mailto:opensilex-help@groupes.renater.fr)).

### Added or changed

- **Web client** : Major aesthetics and ergonomics improvement -> 9f4eaf90
- **Web client** : Visualisation of pictures associated to data -> 45166d2b
- **Web client** : Visualisation of experiment data from scientific object and variables c924d260 -> aecfd721
- **Web client** : New tab with associated document into variable details page -> ab7b6bb0
- **Web client** : Variables selection and contextual actions into variable search page -> da82201c
- **Web client**:  Add loader on scientific object tree view loading -> cf864292

- **API/Web client** : A software (Device) can be linked to a variable -> 8d8cfb43
- **API** : Prevent deletion of OS in case of associated experiment, children, data or datafile -> e17caea7
- **API** : When storing position for a move, we now use a String representation of x,y,z fields instead of
  a previous Integer representation -> 9b3a4a27.
- **API** : Add the "Operating procedure" as a new type of technical document -> e39e3c6b
- **API** : Remove opensilex-mobile module from default OpenSILEX modules -> 252e4565

### Fixed or optimized

- **Web client** : Link for "reset password" in the login page now always redirect to the correct page -> f33941bc
- **Web client** : Fixed a problem with a "germplasm" field when editing a scientific object -> df025f7d
- **Web client** : optimize loading of variables and variables group tab. Optimize loading of variables and variable
  groups in forms -> b00c53ba
- **Web client** : [Data visualization] Optimizations on date formatting into data visualisation -> 687bfcb3
- **Web client** : several fixes for the map visualisation -> 9b3a4a27
- **Web client** : [Geospatial] Display calibration position & enhance multi select -> 73811bcb
- **Web client** : Fix variable name display on tabular data list -> fd3ea6be

- **API** : Variables associated with a variable of type "date" are now stored and exported correctly -> 624e99dd
- **API** : Better error handling in case of Invalid character when evaluating a SPARQL REGEX -> 326be84c
- **API** : Fix URI generation when providing a URI on scientific object creation and CSV import -> e17caea7 , a77a3629
- **API** : Remove no duplicate name constraint when importing OS into global graph -> 45bcf04a
- **API** : Optimize search and loading for entities, characteristics, methods, units, variables and variable groups ->
  b00c53ba
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
- The variable selector has been enhanced; variables are now selected through a modal form where they can be filtered by
  name, entity, characteristic, etc.
- Variables can now be filtered by datatype and time interval.
- When a groups of variables are created or Updated, there are a redirection on detail part of the group.
- Associated experiments are now listed in the project description.

### Fixed

- Events can now be correctly filtered by end date (even if multiple events have the same end date).
- Germplasm search and export have been optimized.
- Fixed some variable bugs: filters, display of species, datatype and time interval translation.
- Data exportation bug about column offset.
- Fix germplasm export: optimize search and export in order to avoid timeout interruption.
- Improve specific properties management of scientific objects.
- Fix bug on ResetOntologies for not completed classes, properties ou restrictions.

## [1.0.0-rc+3] - 2022-05-05

Be careful, updating to this new version need some manual operations to be done.
See [versioning notes](https://github.com/OpenSILEX/opensilex/blob/master/opensilex-doc/src/main/resources/release/1.0.0-rc+3.md).
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
- Documents can now be stored using the GridFS Mongo Connector 
  (see [versioning notes](https://github.com/OpenSILEX/opensilex/blob/master/opensilex-doc/src/main/resources/release/1.0.0-rc+3.md))
- Events and annotations can now be added on a device
- Facilities are now part of their own menu entry
- A new document type "Archive" was added to represent a compressed set of files, where each can be described as a
  document (but they cannot be downloaded)

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

Be carful, updating to this new version need to execute some actions.
See [Versioning notes](https://github.com/OpenSILEX/opensilex/blob/master/opensilex-doc/src/main/resources/release/1.0.0-rc+2.md).
New installations don't need updating action.

### Added

- Concept of "Site" with an address : use W3C vcard specification.
- Filter on variables with filter in entity, entity of interest, characteristic, method, unit, group of variables
- Search in germplasm's attributs in Webapp
- Service in Ontology API to rename an URI.
- New filter on variable in Search Device service, for provenance's agent.
- Append FOAF.rdf ontology
- OpenSILEX migration modules with command.
- Command org.opensilex.migration.GraphAndCollectionMigration for SPARQL graph and MongoDB collection renaming after URI
  generation update

### Added in pre-production

- Visualisation og device value by variable. See. Variable / visualisation
- Species become optional in an experiment

### Fixed or improvement

- **WARNING:** Update model graph from sets/<model_name> to set/<model_name>. Update URI generation by using "id"
  instead of "set" : **this improvement need an action**.
  See [Versioning notes](https://github.com/OpenSILEX/opensilex/blob/master/opensilex-doc/src/main/resources/release/1.0.0-rc+2.md)
  if updating.
- **WARNING:** Organization and Facilities ontologies.
  See [Versioning notes](https://github.com/OpenSILEX/opensilex/blob/master/opensilex-doc/src/main/resources/release/1.0.0-rc+2.md)
  if updating
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

- Problem of authentification when migrate. It's due to FOAF import. You need to
  remove `<http://xmlns.com/foaf/0.1/Agent> rdfs:label "Agent"`
- When installing other version, PHIS ontology is imported. You need to
  remove <http://www.opensilex.org/vocabulary/oeso-ext> context
- Some installation have problem with geospatial visualisation.

## [1.0.0-rc+1] - 2021-11-02

### Added

- Variable:
  - add concept of **Entity Of Interest** to represent which is characterized by variable.
  - add **Export Variables** functionalities in CSV.

- User documentation at homepage.

- Impove **Organization** in progress:
  - Association of Groups to Organizations: you can now associate some groups with an organization at the creation or
    update of it.

### Fixed or optimized

- Provenance : fix no device selector render when selecting oeso:SensingDevice's Agents .
- Ergonomic mistakes fixed

## [1.0.0-rc] - 2021-10-08

### Added

- Data management:
  - Import, export in CSV format
  - A standard provenance for usual provenances or methods
  - Ability to put data on other entities such as sites, areas, factors, etc. except people. So now, you have to use
    target instead of scientificObject in the API. This is also the case for importing data via the web interface.
    See [Versioning notes](https://github.com/OpenSILEX/opensilex/blob/master/opensilex-doc/src/main/resources/release/1.0.0-rc.md)
    when upgrading.
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

- Added system details in the administration menu : instance version, loaded modules, link to web API documentation,
  etc.

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

Link to the upgrade
scripts: [1.0.0-rc migration](https://github.com/OpenSILEX/opensilex/blob/master/opensilex-doc/src/main/resources/release/1.0.0-rc.md)

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
- Data vizualisation on device, object in or out of experiments : enriched with events view, manual data annotation and
  event addition.
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
