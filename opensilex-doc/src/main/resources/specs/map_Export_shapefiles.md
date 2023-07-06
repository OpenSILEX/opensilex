# [Map] Export shapefiles

|Date|Author|Developer(s)|Version OpenSilex|Comment|
|----|------|------------|-----------------|-------|
|05/07/2023|Alexia Chiavarino|Alexia Chiavarino|1.0.0-rc+7.2|created spec |

## Table of contents
* [Needs](#needs)
* [Definitions](#definitions)
* [Solution](#solution)
* [Technical specifications](#technical-specifications)
  * [Frontend](#frontend)
  * [API](#api)
  * [Environment changes](#environment-changes)
* [Limitations and Improvements](#limitations-and-improvements)
  * [Limitations](#limitations)
  * [Improvements](#possible-improvements)


## Needs

The map makes easier to locate and select items (scientific objects, areas, devices) in particular for retrieve and analyze associated data on other specific tools (e.g. SIG).

- Use case #1 : As a user, I need to export a selection of items from the map in shapefiles .
- Use case #2 : As a user, I want to choose the scientific object properties to be exported.


## Definitions

**Shapefile**:

Is a vector data storage format for archiving the location, shape and attributes of geographic features. It is stored as a set of associated files, usually added to a .zip file. The .zip file must contain at least 4 files, components of the shapefile :

 - .shp : stores geographic features. This is the shapefile itself.
 - .shx : stores indexes of ".shp" file records.
 - .dbf (DataBaseFile): stores attribute data (can be viewed in Excel).
 - .prj : stores the associated projection.

Shapefiles often contain large entities with a lot of associated data and were historically used in GIS office applications.

**GIS (geographic information system)**:

Is an information system designed to collect, store, process, analyze, manage and present all types of spatial and geographic data.

**CRS (coordinate reference system)**:

also known as projections.

The earth has an approximately spherical shape, which poses a problem when it comes to visualizing its surface on 2D.
A CRS is a mathematical model for locating any point on the earth's surface on a flat surface. Many different projection systems exist.

**GeoJson **:

is a simple geospatial dataset encoding format using the JSON standard.

     {  
        "type: "FeatureCollection"
        "features": [
          {
           "type": "Feature",
           "geometry": 
              {
                 "type": "Point",
                 "coordinates": [...]
              },
            "properties: {
              ...
            }
      }

**WKT (Well-known text)**:

is a standard text-based format used to represent vector-based geometric objects.
     
     POINT(6 10)
     LINESTRING(3 4,10 50,20 25)
     POLYGON((1 1,5 1,5 5,1 5,1 1))

## Solution

A "Save the map" button, above the map, opens a modal offering to save the map in 3 formats (3 buttons): pdf, png and shapefile. The "shapefile" option has been customized to suit these use cases.

The "shapefile" export concerns scientific objects, devices and areas. The properties selection concerns scientific objects only.

For all of them, 4 basic properties are exported - label, uri, rdf type and rdf type label - and 3 geometry types can be to exported : polygon, line and point.

The selection of map items is exported. Without any selection, all items visible in the "panel menu" are exported.

For each type of item (scientific objects, devices, areas) exported, a zip file is created. A shapefile is created for each type of geometry because some GIS software cannot import shapefiles with multiple geometries.

Items are exported in the "World Geodetic System 1984" CRS (WGS 84 - EPSG:4326).


## Technical specifications

### Front-end

From the "Save the map" modal, clicking on the "shapefile" button produces several events:

- if there are devices and/or areas visible (and selected):

Items are converted and downloaded in-browser as zip files. The basic library used is "shp-write". 

For both items, the file name is the same ("download") but it contains files with specific names ("areas" and "devices"). It contains the shapefiles (composed of 4 files: .shp, .shx, .dbf and .prj) specific to each type of geometry.

- if there are scientific objects visible (and selected):

A new modal window is opened with a selector of scientific object properties. By default, all properties are already selected.

The `Geometry` property converts the scientitific object coordinates into geoJson and WKT (2 columns are created in in the DataBaseFile).

For improved performance, conversion to shapefiles is managed in the back-end, using the GeoTools library. 

Finally, a zip file is downloaded in-browser, containing an "OS_shp" file. It contains the shapefiles (composed of 4 files: .shp, .shx, .dbf and .prj) for each type of geometry of the scientific objects recorded.

### API

The `exportShp` service of the `ScientifObjectAPI` class is called when scientific objects are selected on the map.

The GeoTools library converts the selected scientific objects into the `ShapeFileExporter` class :

1- Create feature types : Define features type (shapefile columns) according to the selected properties and geometry (polygon, line, point). GIS software limit the size of the column names (10 max), so property names have to be formatted.
2- Writing collections with the corresponding feature type.
3- Create a new shapefile for each feature type and its corresponding collection saved in the same file.

Shapefiles are then zipped and returned.

### Environment changes

**shp-write (0.2.3)**: to convert and download items in the front-end.

The "shp-write" library has been fork and modified by Alexandre Juan to suit the needs of OpenSilex.

- Fork:
https://github.com/Mitix-EPI/shp-write
- Basic library:
https://github.com/mapbox/shp-write


**Geotools (v29-RC1)**: Add tools to convert scientific objects coordinates and properties into shapefiles.
https://geotools.org/
https://docs.geotools.org/latest/javadocs/index.html


## Limitations and Improvements

### Limitations

The scientific objects export is limited to 10 000 objects because beyond this size, processing time exceeds 2 min and causes an error.

### Improvements

- Write tests.
- Add a selector to choose variables for scientific objects to be exported.
- Add selectors to choose properties and vairables for devices to be exported
- Convert and export devices and zones from the back-end.
- Create an "export shapefile" button separate from other options ( png, pdf).
