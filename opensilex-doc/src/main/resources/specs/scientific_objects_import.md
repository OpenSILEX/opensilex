# Specifications : scientific objects import

| Date       | Editor(s)     | OpenSILEX version | Comment                                |
|------------|---------------|-------------------|----------------------------------------|
| 04/08/2025 | Dhruthi NOOKA | 1.4.8-rdg         | Document creation                      |
| 08/10/2025 | Max Hart      | 1.4.8-rdg         | Modified for type update development   |

## Table of contents

<!-- TOC -->
* [Specifications : scientific objects import](#specifications--scientific-objects-import)
  * [Table of contents](#table-of-contents)
  * [Definitions](#definitions)
  * [Needs](#needs)
  * [Solution](#solution)
    * [Business logic](#business-logic)
  * [Possible scenarios](#possible-scenarios)
  * [Technical specifications](#technical-specifications)
    * [Detailed explanations](#detailed-explanations)
  * [`AbstractCsvImporter.java`](#abstractcsvimporterjava)
  * [`ScientificObjectCsvImporter.java`](#scientificobjectcsvimporterjava)
  * [`ScientificObjectDAO.java`](#scientificobjectdaojava)
    * [Exceptions/Errors thrown](#exceptionserrors-thrown)
  * [Errors shown in UI](#errors-shown-in-ui)
    * [Tests](#tests)
  * [Limitations and improvements](#limitations-and-improvements)
<!-- TOC -->

## Definitions

- **Scientific Object** : object of a study (plant, population, etc.), placed in experimental conditions and associated with factors.
- **Custom relation** : A custom relation is special type of property that can be dynamically defined on a type in OpenSILEX. For example, we could define a unique property called seedSize that only applies to the Seed ScientificObject type.

## Needs

- Use case #1: As a user, I want to import a CSV with multiple Scientific Objects to insert them globally or in an experiment.
- Use case #2: As a user, I would like to update the fields of a scientific object (except for the type) as part of an XP, or globally
  so that I can make bulk additions and modifications to objects.
- Use case #3: As a user, I would like to be able to update the types of Scientific objects globally if they are present in 0 experiments.
- Use case #4: As a user, I would like to be able to update the types of Scientific objects inside an experiment if they are present in no other experiments. If the update passes, then i would also like it to automatically update the type to match globally. Any new custom relation values should also be copied.

## Solution

- We have added the logic for the insertion of multiple Scientific Objects by importing a SO CSV file in global and in an experiment.
- Also added the logic for the modification of multiple Scientific Objects by importing a SO CSV file in an experiment
- All this logic is defined in the special import logic class : `ScientificObjectCsvImporter.java`
- We have also added a few validations to handle the scenarios like duplicate name, duplicate URI, empty SO name, incorrect URI within CSV

### Business logic

- In global, we check if there is URI or not for each record in the CSV, we add these objects into filledUrisToIndexesInChunk map.
  If not, we generate the URI and add them into generatedUrisToIndexesInChunk map
- We've enabled the modification of multiple SOs in bulk by transforming the existing logic which would validate based on multiple conditions
and segregate them to two separate lists, one for creation and the other for modification of those SOs.

Any scientific object in the CSV file should :
- have a unique SO name 
- not have an empty SO name 
- have a unique URI
- not have an incorrect URI

## Possible scenarios for each line in the CSV
- If the URI entered in CSV doesn't exist in both XP and in global, there's no SO with the same name in XP -> insert the SO both in XP and in global
- If the URI entered in CSV doesn't exist in XP but exists in global, and there's no SO with the same name in XP -> insert the SO only in XP
- If the URI is empty in CSV and there's no SO with the same name in XP -> insert the SO
- If the URI entered in CSV exist in XP -> update the SO
- If the URI is empty in CSV and there's a SO with the same name in XP -> update the SO

## Possible scenarios upon type update attempt in CSV import
- **Globally**, we try to update the type of a Scientific Object that is present in at least one experiment, this is not permitted so an error is shown.
- **Globally**, we try to update the type of a Scientific Object that is present in 0 experiments, this is permitted.
- **In an experiment**, we try to update the type of a Scientific Object that is also present in at least one other experiment, this is not permitted so an error is shown.
- **In an experiment**, we try to update the type of a Scientific Object that is present in NO other experiment, this is permitted, in this scenario we will also update the same object globally. In this secondary global update, we only update the type field and any other custom relations unique to the new type.

*Note:* The decision was made to be able to update type/custom relations of global when importing in experiment, but NOT the other way around, because there are some unique, currently mal-documented experiment-type relations, like factor levels.

## Technical specifications
- Renamed the existing `create(CSVValidationModel validation, List<T> models)` to `upsert(CSVValidationModel validation, List<T> models, List<T> modelChunkToUpdate)`
  in `AbstractCsvImporter.java` by passing another parameter `List<T> modelChunkToUpdate` to handle the modification.
- Added a `List<SPARQLResourceModel> objectsToUpdate` in `CSVValidationModel.java` class which will be used to call the update method


### Detailed explanations by class
## `AbstractCsvImporter.java`
- split the modelChunk list into two parts: modelChunkToCreate (for creation) and modelChunkToUpdate (for modification)
- Moved the code block mapping URIs, checking the uniqueness of URIs in separate methods: handleURIMapping, checkUrisUniqueness 
  so that ScientificObjectCsvImporter can override these methods and define its own implementations

## `ScientificObjectCsvImporter.java`
- Overloaded the `handleURIMapping()` method to validate multiple scenarios possible (listed under Business Logic section) and then map the URIs in an exp
- Added the `checkIfSONameIsNull()` method to check whether the OS name is empty or not.
- Added the `checkUriExistInXP()` method to verify whether if the entered URI in the CSV file exists in the exp or not.
- Overridden the `checkUrisUniqueness()` method to verify the uniqueness of generated URIs in an exp. 
  For global, we are checking the uniqueness of both generated and filled URIs.
- Instead of using the `checkUniqueNameByGraph()` method, we have now added two validations in the `customBatchValidation()` method in an exp:
  . `checkLocalDuplicateNames()` - checks for duplicate names within a CSV
  . `checkLocalDuplicateURIs()` - checks for duplicate URIs within a CSV
- Added the `update()` method to modify multiple SOs in bulk by passing the
- Renamed the existing `create(CSVValidationModel validation, List<T> models)` to `upsert(CSVValidationModel validation, List<T> models, List<T> modelChunkToUpdate)`
  by passing another parameter `List<T> modelChunkToUpdate` to handle the modification. 
  We also fetch the `geospatialModels` and split into `geospatialModelsToCreate` and `geospatialModelsToUpdate` to call the create and update methods respectively.

## `ScientificObjectDAO.java`
- `checkLocalURIDuplicates()` - Added to check whether URIs in CSV are duplicated or not in an exp.

### Exceptions/Errors thrown
`DuplicateURIListException` is used to handle Duplicate URIs in CSV
`DuplicateNameListException` is used to handle Duplicate SO names in CSV

## Errors shown in UI
- If there are duplicate SO names in CSV in an Exp, we display the error 
  'Invalid value. Column: 'rdfs:label' - Value: 'Plant1' - Details: 'Object name <SO1> must be unique. dev:id/scientific-object/so-example1 has the same name'
- If SO name is empty, the error ‘Missing required value. Column: rdfs:label - Object name must be present’ is displayed
- If there are duplicate URIs in CSV in an Exp, we display the error 'Invalid value. Column: 'uri' - Value: 'dev:id/scientific-object/so-example1' - Details: 'Object URI <dev:id/scientific-object/so-example1> must be unique.'
- If the entered URI is incorrect in an Exp, we display the error 'Invalid URI. Column: 'uri' - Value: 'Not a valid (absolute) IRI'

### Tests

Tests are included in `ScientificObjectCsvImportTest.java` to test the insertion, modification logic of SOs

## Limitations and improvements

- Need to improve the performance of the update logic to modify multiple SOs in bulk
- Refactor and move the code to the corresponding layer (Business, API, Data) accordingly
- Allow updating of type globally for an OS that's present in a single experiment, to do this the specifications need to be decided about the wierd type-experiment relations, like factor levels.