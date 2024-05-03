# Specifications : metadata

- Developers :
  - Hamza Ikiou (hamza.ikiou@inrae.fr)
- Date : 2023-07-13
- OpenSILEX version : 1.0.1 (develop)

## Needs

All the elements in OpenSILEX are different and have different usage. But we can associate to them some generic
attributes, such as :

- The publisher
- The publication date
- The date of the last modification

Those information will be available on the detail page of all the element of OpenSILEX. It will be displayed as a
sentence following the pattern : "Published at {_publication_date_}, by {_publisher_}, modified at {_last_updated_date_}"

Furthermore, some element on OpenSILEX have a creator. The creator here was meant as a publisher, so it will be a
replacement.

## Solution

The generic models will be updated by replacing the creator with a publisher and by adding the publication date and the
date of the last modification.

The generic methods of creation and update will be updated, so they automatically set the two date in real time.

All the models will be set with the current user as the publisher.

## Technical specifications

### Definitions

- **Publisher** : the person who put the resource on OpenSILEX,
see the ontology definition [DC:Publisher](https://www.dublincore.org/specifications/dublin-core/dcmi-terms/terms/publisher/)
- **Publication date** : the date when the resource is put on OpenSILEX, it is different from the creation date,
see the ontology definition [DC:Issued](https://www.dublincore.org/specifications/dublin-core/dcmi-terms/terms/issued/)
- **Date of the last update** : the date when the resource has been modified for the last time,
see the ontology definition [DC:Modified](https://www.dublincore.org/specifications/dublin-core/dcmi-terms/terms/modified/)

### Detailed explanations

#### API

Add of the three attributes on the class `SPARQLResourceModel` and `MongoModel`.
To ensure that the two dates are automatically set up for the `MongoModel`, the methods `create()` and `update()`
in the `MongoDBService` have been updated. Same process for the `SPARQLResourceModel`, the methods `create()` in the
`SPARQLService` and `updateInstanceFromOldValues()` in the `SPARQLClassObjectMapper` have been updated.

In both case, at the creation, the publication date is set up but not the date of last update. At the update, the
publication date is not modified and the date of the last update is set up (it changes at every update).

The publisher is set up on the different API classes using the current user and in the classes `AbstractCsvImporter` and
`ScientificObjectCsvImporter`.

The type of the two dates are _Instant_ for the `MongoModel` and _OffsetDateTime_ for the `SPARQLResourceModel`. The 
publisher is stored as URI in both cases. In the DTO he is stored as `UserGetDTO`. Whenever we need to set the publisher
on a DTO, we do it on the API class calling the current user to set up the `UserGetDTO`.

There is some exceptions (ScientificObject, Device,...) that are not using generic methods in `SPARQLService`, in those
cases, we set up the different attributes on their DAO and API classes.

#### Front

A new component `MetadataView` has been added, it has the three attributes as `Prop()` and it builds the sentence
"Published at {_publication_date_}, by {_publisher_}, modified at {_last_updated_date_}"

This component is added in all the detail page of OpenSILEX, it is displayed if the publisher is not undefined and not null.

### Tests

The metadata are tested in the classes `SPARQLMetadataTest` and `MongoMetadataTest`. Those classes have the same content,
one check MongoDB data and the other check RDF data.

The following tests check the correct behaviour on creation and update of a `MongoModel` and a `SPARQLResourceModel` :

- `create` : The model must have a publisher and a publication date, and must not have a date of last modification
- `updateAfterCreate` : The model must have the same publication date as before and a date of last modification
- `updateAfterUpdate` : The model must have the same publication date as before and a different date of last modification