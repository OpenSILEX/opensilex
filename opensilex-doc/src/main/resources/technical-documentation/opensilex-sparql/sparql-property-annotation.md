# Sparql properties annotation

**Document history (please add a line when you edit the document)**

| Date       | Editor(s)          | OpenSILEX version       | Comment           |
|------------|--------------------|-------------------------|-------------------|
| 18/09/2025 | yvan.roux@inrae.fr | 1.4.9 Explosive Emerald | Document creation |

> ⚠️ _WARNING_ : This document is incomplete ! You can help by expanding it. ⚠️
>
> Currently covered topics :
>
> - @IgnoreUpdateIfNull annotation
>
> Missing topics or incomplete:
>
> - @AutoUpdate annotation
> - @CascadeDelete annotation
> - @Inverse
> - @Required
 

## @IgnoreUpdateIfNull

### Use case

When updating a SPARQLResourceModel, if a field is null, it means that the user want to delete the corresponding triple. 
In some cases, we want to ignore null values during update, meaning that if the field is null, we want to keep the existing triple.

It could be usefull to make update easier for the user by allowing partial update of a resource.

It is not used to ensure that the field will always have a values even when updated, for that, use @Required annotation.

### Technical explanation

The way to preserve existing triples is to load old model (the entire model) before any delete or create operation and setting the old value of the field to the new model if the new value is null.

For optimization purpose, we don't load every model but only models that contains at least one field with @IgnoreUpdateIfNull annotation and null value.

This is done in `SPARQLService#update` method and more specifically in `SPARQLClassObjectMapper.updateInstanceFromOldValues` method and `SPARQLService#loadOnlyOldNeededInstances` method for partial loading.

### Improvements idea

A possible improvement would be to handle this by ignoring concerned fields (triplets) during delete and create operation instead of loading old values. 
This would be way more efficient. Actually this is possible for create operation and could be done in `SPARQLClassQueryBuilder#addCreateBuilder` method by ignoring concerned fields.
But delete operation is actually a general request and it is actually not possible to ignore fields just for specific models (URIS).

Another improvement idea would be to load only concerned fields of concerned models instead of loading the entire model.
But to be efficient, this should be done in a single SPARQL query for all model (or for a batch of models).

## @AutoUpdate
> ⚠️ **Warning:** Auto update is recursive. For example, imagine a Germplasm A with a Parent field referencing Germplasm B.
> If both the parent and child fields are set to auto update, updating A will automatically update its parent B, which will then automatically update its child A, and so on, potentially leading to an infinite loop.>

## @CascadeDelete
> ⚠️ **Warning:** Cascade delete is recursive. For example, imagine a Germplasm A with a Parent field referencing Germplasm B.
> If both the parent and child fields are set to cascade delete, deleting A will first automatically delete its parent B, which will first automatically delete its child A, and so on, potentially leading to an infinite loop.