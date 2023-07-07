# Specifications : facilities

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

- Developers :
  - Valentin Rigolle (valentin.rigolle@inrae.fr)
- Date : 2023-05-23
- OpenSILEX version : 1.0.0-rc+7.2 (develop)

## Needs

Facilities represent the different installations that can be used for experiments. That includes for example
fields, greenhouses or growth chambers.

A facility can be characterised by its geometry. For example, a field has bounds defined by coordinates.

A facility can have an address.

- Use case #1 : As a user, I want to specify the geometry of my facility.
- Use case #2 : As a user, I want to define an address for my facility.
- Use case #3 : As a user, I want to use the address of my facility as geometry (as a lat/long point).

## Solution

Like other types, a facility can have an associated geometry, represented by a GeoJson.

The address is represented by a resource of type vcard:Address (from the vcard ontology).

If a facility is created using an address and no geometry, a set of coordinates will be deduced from the address using
a geocoding tool. The coordinates will be set as the geometry of the facility.

For convenience, the facility form will try to autocomplete the address field using a geocoding tool.

## Technical specifications

### Definitions

- **Geocoding** : the process of getting geospatial coordinates from an address. This is usually done by calling an
  external tool, like the [Nominatim API from OpenStreetMap](https://nominatim.openstreetmap.org).

### Detailed explanations

#### API

The facility API expects the following types on creation or update of a facility :

- For the address, a `FacilityAddressDTO`
- For the geometry, a `GeoJsonObject`

If a geometry is provided, it will be used as the facility geometry. If only an address is provided, the 
`GeocodingService` is called to transform the address into a geometry (a lat/long point). For now, the only implemented
geocoding service is the [Nominatim API](https://nominatim.openstreetmap.org).

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