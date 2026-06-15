# Specifications : [Data-Annotation] Annotation column during data imports

**Document history (please add a line when you edit the document)**

| Date       | Editor(s)          | OpenSILEX version       | Comment                                   |
|------------|--------------------|-------------------------|-------------------------------------------|
| 10/12/2025 | yvan.roux@inrae.fr | 1.4.9 Explosive Emerald | Document ordering : creation date unknown |

## Table of contents

<!-- TOC -->
* [Specifications : [Data-Annotation] Annotation column during data imports](#specifications--data-annotation-annotation-column-during-data-imports)
  * [Table of contents](#table-of-contents)
  * [functional requirements](#functional-requirements)
    * [Confirmation message after successfull import](#confirmation-message-after-successfull-import)
  * [Documentation](#documentation)
<!-- TOC -->

## functional requirements

- Add an optional column to data imports to be able to import annotations.
- These annotations point towards the scientific object or target provided in the corresponding column
(we are annotating objects and not data).
- A validation has to be done to make sure we are not trying to create an annotation that points to nowhere.
- A confirmation message should show up stating how many annotations were imported, if any.


### Confirmation message after successfull import

If at least one annotation is imported during data import, a confirmation message pops up to let the user know.
To do this these components were modified : 
- `ResultModalView.vue` : Made additions that are basically the same as what was already present for the data import 
confirmation message. In the same way that the data message revolves around `nbLinesImported`,
our message revolves around the new attribute `nbAnnotationsImported`. Pretty much the same code but with a different message.

## Documentation

- see the [technical document](../technical-documentation/opensilex-core/data/annotation-during-data-imports.md)