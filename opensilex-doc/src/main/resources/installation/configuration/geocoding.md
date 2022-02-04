# Address autocompletion with geocoding

In order to provide autocompletion when typing an Address in a form (aka typeahead), OpenSilex calls an external service. 
This service is defined in the `front` section of the configuration file :

```yaml
front:
    geocodingService: Photon
```

For the moment, two services are supported : Photon and Adresse. If none is specified, Photon will be used.

## Photon

[photon.komoot.io](https://photon.komoot.io/)

Photon is an open-source typeahead service based on OpenStreetMap data. It works decently for most of the world,
but may provide approximate guesses in some cases (depending on local OpenStreetMap data).

## Adresse

[adresse.data.gouv.fr](https://adresse.data.gouv.fr/)

Adresse is an open-source geocoding and typeahead service based on a French national database.
Using this service allows for precise address guesses within France, but doesn't work for any other
country.