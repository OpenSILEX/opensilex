| Date       | Author             | Developer(s)      | Version OpenSilex       | Comment                                                                      |
|------------|--------------------|-------------------|-------------------------|------------------------------------------------------------------------------|
| 7/08/2024  | Alexia Chiavarino  | Alexia Chiavarino | 1.3.0                   | created spec - global and site case                                          |
| 20/09/2024 | Alexia Chiavarino  | Alexia Chiavarino | 1.3.0                   | updated spec - add featureOfInterest field in Mongo + add Improvements       |
| 04/10/2024 | Alexia Chiavarino  | Alexia Chiavarino | 1.3.0                   | updated spec - add facility case + definitions                               |
| 39/09/2025 | yvan.roux@inrae.fr | Yvan Roux         | 1.4.9 Explosive Emerald | Split document into Business logic (spec) and Technical specifications (dev) |

## Table of contents
<!-- TOC -->
  * [Table of contents](#table-of-contents)
  * [Definitions](#definitions)
  * [Business logic](#business-logic)
    * [Site](#site)
    * [Facility](#facility)
<!-- TOC -->

## Definitions

- **Location**: The act of determining the location of a thing, phenomenon or its origin.
- **Position**: Place where a thing is positioned in relation to a whole (in a coordinate system, the orientation of an object, for example: facing east).
- **Geometry**: Science of space and the figures that can occupy it (shape and size of spatial objects).
- **Spatial coordinates**: Numerical representation of the position of an object in space, expressed in various forms according to the spatial coordinate system (sexagesimal or decimal degrees, longitude and latitude).
- **Move**: Oriented distance separating the starting point from the finishing point, in a straight line over a given time.
- **Trajectory**: The trajectory of a moving object is the set of positions it has occupied throughout its movement. A line describes the object's movement with a time dimension (x positions at x times).

## Business logic

For facilities, we can enter an address and spatial coordinates. The feature behavior is explained in the
[facility specification](facilities.md).

In SOSA Ontology, an observation collection of a property must be unique for each feature of interest. For example, a
person (feature of interest) can have only one observation collection of his height (property : height) and only one 
observation collection of his location (property : location).

In OpenSilex, the concept of an observation collection is currently only used for the location property. Thus, a location
observation collection is linked to the feature of interest by a unique URI.

### Site

As a site is only located by one address, the localization model must be adjusted:

- a site can have only one address, so the observation collection for a site must contain only one observation.
- no date can be linked to a site, so location observation don't need to store the date.
- an address can only be converted to spatial coordinates as a point type in the location model.


     {
        “observationCollection” : URI,
        "featureOfInterest": URI,
        “hasGeometry” : boolean,
        “location”: {
              "geometry" : {
                  "type": Point,
                  "coordinates": [ X , Y ]
                  },
              },
    }

### Facility
A facility can be located by an address and positions (at different times):

- if the facility is only located by an address, no date can be associated with it, as in the site case.
- if the facility has one or more positions, each position will be associated with a time (instantaneous or interval) and spatial coordinates. Each position is an observation of the facility geometry at time "i". The position must have at least a "endDate".
- as before, if facility has an address and positions, the address spatial coordinates will be replaced by position coordinates.


    {
        “observationCollection” : URI,
        "featureOfInterest": URI,
        “hasGeometry” : boolean,
        “location”: {
              "geometry" : {
                  "type": Point, Line or Polygon
                  "coordinates": [ X , Y ]
              },
        },
        "endDate" : 2024-10-15T06:48:15.777+00:00,
        "startDate" : 2024-11-15T06:48:15.777+00:00
    }
















