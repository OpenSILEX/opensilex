# Technical documentation : [Data-Annotation] Annotation column during data imports

**Document history (please add a line when you edit the document)**

| Date       | Editor(s)          | OpenSILEX version       | Comment                                   |
|------------|--------------------|-------------------------|-------------------------------------------|
| 10/12/2025 | yvan.roux@inrae.fr | 1.4.9 Explosive Emerald | Document ordering : creation date unknown |

## Table of contents

<!-- TOC -->
* [Technical documentation : [Data-Annotation] Annotation column during data imports](#technical-documentation--data-annotation-annotation-column-during-data-imports)
  * [Table of contents](#table-of-contents)
  * [Back-end](#back-end)
    * [Verifications](#verifications)
    * [Generation of the AnnotationModel](#generation-of-the-annotationmodel)
  * [Front-end](#front-end)
    * [Template](#template)
    * [Confirmation message after successfull import](#confirmation-message-after-successfull-import)
  * [Documentation](#documentation)
<!-- TOC -->

## Back-end

Most of the changes were made to `DataAPI.java` as this is where most of the data import code is written.

### Verifications

Two types of verifications are performed : 

- A more global verification to test that there is a scientific object or a target column if there is an annotation column.
This is done in the `validateWholeCsv` function. If one is missing then a missingHeader error is added to the `CsvValidationModel`.

- Then a similar verification but for a single line performed in the function `validateCsvRow`.
If an annotation exists on a line where there is no Os or Target then an InvalidAnnotationError is added 
to the `DataCsvValidationModel`.

### Generation of the AnnotationModel

On every line of the csv (validateCsvRow), a temporary `AnnotationModel` is created if and when we hit the annotation column.
Then, at the end of the row, if a target or an Os was created, the Annotation is finalized with the correct target.
The motivation used for the annotation is `Oa.commenting` and the date is the same as the date of the imported data
(Instead of taking DateTime.now as a date). The created annotation is then added to a list in `DataCsvValidationModel`
to be added at the end.

In the `importCsvData` function, mongo and sparql transactions are used to ensure that if any annotation or data creation
fails, all created elements will be rolled-back.

## Front-end

### Template

Just like the other columns, the annotation column can be optionally added to the template to download.
For this, like the other columns, additions were made to this component : 
- `GenerateDataTemplateForm.vue`

### Confirmation message after successfull import

If at least one annotation is imported during data import, a confirmation message pops up to let the user know.
To do this these components were modified : 
- `ResultModalView.vue` : Made additions that are basically the same as what was already present for the data import 
confirmation message. In the same way that the data message revolves around `nbLinesImported`,
our message revolves around the new attribute `nbAnnotationsImported`. Pretty much the same code but with a different message.

## Documentation

- see the [specification document](../../../functional-specifications/annotation-during-data-imports.md)

