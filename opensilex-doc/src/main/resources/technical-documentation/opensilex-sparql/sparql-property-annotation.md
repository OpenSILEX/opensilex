# Sparql properties annotation

**Document history (please add a line when you edit the document)**

| Date       | Editor(s)          | OpenSILEX version       | Comment                                                           |
|------------|--------------------|-------------------------|-------------------------------------------------------------------|
| 18/09/2025 | yvan.roux@inrae.fr | 1.4.9 Explosive Emerald | Document creation                                                 |
| 31/12/2025 | yvan.roux@inrae.fr | 1.4.9 Explosive Emerald | new @IgnoreUpdateIfNUll behaviour : changed technical explanation |

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

The first way to preserve existing triples was to load old model (the entire model) before any delete or create operation
and setting the old value of the field to the new model if the new value is null.

We changed this mechanism for optimization reason : SELECT request to load old model has heavy cost. new mechanism is to
ignore concerned fields (triplets) during delete operation.

For full delete request exemple see `SPARQLClassQueryBuilder#getDeleteBuilder`.

In `SPARQLClassQueryBuilder#getDeleteBuilderForUpdateCases`, for each field annotated with @IgnoreUpdateIfNull,
if the new value is null, we add the field to the ignore list passed to `getDeleteBuilder`.

Create operation does not need special behaviour because if the new value is null, no triple will be created.

## @AutoUpdate
> ⚠️ **Warning:** Auto update is recursive. For example, imagine a Germplasm A with a Parent field referencing Germplasm B.
> If both the parent and child fields are set to auto update, updating A will automatically update its parent B, which will then automatically update its child A, and so on, potentially leading to an infinite loop.>

## @CascadeDelete
> ⚠️ **Warning:** Cascade delete is recursive. For example, imagine a Germplasm A with a Parent field referencing Germplasm B.
> If both the parent and child fields are set to cascade delete, deleting A will first automatically delete its parent B, which will first automatically delete its child A, and so on, potentially leading to an infinite loop.