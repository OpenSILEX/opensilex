# Specifications : facilities

**Document history (please add a line when you edit the document)**

| Date       | Editor(s)                                     | OpenSILEX version | Comment               |
|------------|-----------------------------------------------|-------------------|-----------------------|
| 2023-05-23 | Valentin Rigolle <valentin.rigolle@inrae.fr>  | 1.0.0-rc+7.2      | Document creation     |
| 2026-04-22 | Valentin Rigolle <valentin.rigolle@inrae.fr>  | 1.5.0             | Updated geometry spec |

> This document is incomplete. You can help by expanding it.
>
> Covered topics :
>
> - Addresses and geometry
>
> Missing topics :
>
> - Access rights
> - Other fields of a facility
> - Relationship with other types
> - Other ?

## Table of contents

<!-- TOC -->
* [Specifications : facilities](#specifications--facilities)
  * [Table of contents](#table-of-contents)
  * [Needs](#needs)
  * [Solution](#solution)
    * [Compatibility](#compatibility)
  * [Technical specifications](#technical-specifications)
    * [Definitions](#definitions)
    * [Detailed explanations](#detailed-explanations)
      * [API](#api)
      * [Front-end](#front-end)
    * [Tests](#tests)
<!-- TOC -->

## Needs

Facilities represent the different installations that can be used for experiments. That includes for example
fields, greenhouses or growth chambers.

A facility can be characterized by its geometry. For example, a field has bounds defined by coordinates.

A facility can have an address.

- Use case #1 : As a user, I want to specify the geometry of my facility.
- Use case #2 : As a user, I want to define an address for my facility.
- Use case #3 : As a user, I want to use the address of my facility as geometry (as a lat/long point).
- Use case #4 : As a user, I want to update the geometry of my facility (which might be a movable greehouse, for example).

## Solution

Like other types, geospatial information is associated to facilities using `vocabulary:Move` events. These events may
contain geometry, but also arbitrary location data.

Previously, the `vocabulary:hasGeometry` property used to link a facility to its geometry directly. Please read the
[compatibility section](#compatibility) for more information.

The address is represented by a resource of type vcard:Address (from the vcard ontology).

If a facility is created using an address and no geometry, a set of coordinates will be deduced from the address using
a geocoding tool. The coordinates will be set as the geometry of the facility.

For convenience, the facility form will try to autocomplete the address field using a geocoding tool.

### Compatibility

From the 1.5.0 version, geospatial information is stored using moves instead of linking geometry to the facility
directly. In order to keep the existing infrastructures (e.g. scripts) from breaking, we chose to keep the `geometry`
parameter and property in some services :

- GET /facility/{uri} : `geometry` contains the same information as `lastPosition.geojson`
- GET /facilities/with_geometry : no change
- POST /geometry : The `geometry` parameter creates a move event with the given geometry. The date of the event is
  set to the date and time of the request. Using `geometry` together with the new `location` parameter is not allowed
  and returns an error.
- PUT /geometry : Setting the `geometry` parameter returns an error. To update a facility geospatial information, you
  should add a new move event of modify the existing ones.

## Technical specifications

### Definitions

- **Geocoding** : the process of getting geospatial coordinates from an address. This is usually done by calling an
  external tool, like the [Nominatim API from OpenStreetMap](https://nominatim.openstreetmap.org).

### Detailed explanations

#### API

The facility API expects the following types on creation or update of a facility :

- For the address, a `FacilityAddressDTO`
- For the locations, a list of `LocationObservationDTO`

If a location list is provided, it will be used as to create the move events of the facility. If only an address is
provided, the `GeocodingService` is called to transform the address into a geometry (a lat/long point). For now, the 
only implemented geocoding service is the [Nominatim API](https://nominatim.openstreetmap.org). The geometry is used to create a move event for the
facility.

#### Front-end

The facility form uses the `AddressForm` component to allow the user to specify the address of the facility. This
component can use an external service to autocomplete the user input. The service responsible for this is
`IGeocodingService`. For the moment, two services are available :

- **Photon**, based on OpenStreetMap data : https://photon.komoot.io
- **Adresse**, the French official geocoding API. Only works on French addresses : https://adresse.data.gouv.fr/api-doc/adresse

The service that will be used depends on the configuration. See the
[configuration documentation](../installation/configuration/geocoding.md) for more information on this topic.

### Tests

Facility tests are located in `FacilityApiTest`.

The following tests check the correct behaviour when creating a facility with an address or geometry :

- `testCreateWithAddress` : The created facility must have a geometry.
- `testCreateWithGeometry` : The created facility must have the same geometry as provided on creation.
- `testCreateWithAddressAndGeometry` : The created facility must have the same geometry as provided on creation, and
  not the geometry resolved from the address.
- `testCreateWithoutAddressOrGeometry` : The created facility must not have any geometry.