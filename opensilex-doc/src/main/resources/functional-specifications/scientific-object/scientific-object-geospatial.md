# Specifications : Scientific object - geospatial information

**Document history (please add a line when you edit the document)**

| Date       | Editor(s)                                    | OpenSILEX version | Comment           |
|------------|----------------------------------------------|-------------------|-------------------|
| 22/04/2026 | Valentin Rigolle <valentin.rigolle@inrae.fr> | 1.5.1             | Document creation |

> ⚠️ _WARNING_ : This document is incomplete ! You can help by expanding it. ⚠️
>
> Currently covered topics :
>
> - Basic needs
> - API compatibility
>
> Missing topics :
>
> - everything else

## Table of contents

<!-- TOC -->
* [Specifications : Scientific object - geospatial information](#specifications--scientific-object---geospatial-information)
  * [Table of contents](#table-of-contents)
  * [Definitions](#definitions)
  * [functional requirements](#functional-requirements)
  * [API](#api)
    * [Compatibility](#compatibility)
<!-- TOC -->

## Definitions

- **{Term}** : {definition}

## functional requirements

Geospatial information is often essential metadata regarding scientific objects : a plant may grow faster if it's in
the sunny side of the field, plots near the river may be flooded, etc. Geospatial information can be defined in
different manners :

- Spatial geometry (like geojson)
- Containing facility
- Arbitrary coordinates
- Arbitrary information (e.g. "back row of the greenhouse near the door")

Geospatial information can change over time (the plant may be displaced inside the greenhouse).

## API

### Compatibility

The API services previously used a `geometry` property / parameter to attach geometry information to a scientific
object. Now that we only use move events to define geospatial information, the `geometry` property does not match
the underlying model anymore. However, for compatibility reasons, we choose to keep in the API services :

- GET : Services that used to return the `geometry` property of scientific objects will still return it. The value
  will be a copy of `location.geojson`.
- POST : Setting `geometry` for object creation will result in the creation of a move event targeting this object. 
  The move will be an instantaneous event, dated at the creation date of the object. If the creation date is not
  defined, the current date will be used instead. Trying to set the `geometry` property together with the
  `location` property is not allowed.
- PUT : Setting `geometry` in a PUT request will result in an error. Updating the position of a scientific object can
  only be done by adding a move event, or modifying the existing ones.