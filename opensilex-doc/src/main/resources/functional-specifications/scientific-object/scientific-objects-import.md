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

## functional requirements

### Main use-cases

- Use case #1: As a user, I want to import a CSV with multiple Scientific Objects to insert them globally or in an experiment.
- Use case #2: As a user, I would like to update the fields of a scientific object as part of an XP, or globally
  so that I can make bulk additions and modifications to objects. If no Location columns are present then i want to perform this update without modifying or deleting any Locations.
- Use case #3: As a user, I would like to be able to update the types of Scientific objects globally if they are present in 0 experiments.
- Use case #4: As a user, I would like to be able to update the types of Scientific objects inside an experiment if they are present in no other experiments. If the update passes, then i would also like it to automatically update the type to match globally. Any new custom relation values should also be copied.

### Geolocation use-cases

- Geo use case #1: As a user, I want to be able to import an **initial location** (also known as a Move in Opensilex), for each imported ScientificObject, all in the same single CSV. The extra columns for this are: start_date_of_Location, end_date_of_Location, x, y, z, coordinates(wkt format) and textualPosition.
- Geo use case #2: As a user, I want to be able to perform the previous use-case up to a **maximum of 1 time, for each Experiment**. (1 Initial Location per ScientificObject per Experiment)
- Geo use case #3: During an import, if an imported ScientificObject is registered as an **update** operation, then the Location columns are still taken into account. An fully empty Location will be considered a Deletion of initial Location (or no change if there is no current Location previously imported). A filled Location will either be counted as a Location creation if the ScientificObject has none already, or as an update operation if the ScientificObject already had a Location.
- Geo use case #4: The user can still update an initial Location of a ScientificObject that already has multiple Locations (Moves) associated with it. To do this the start_date_of_Location and end_date_of_Location column-values must match up exactly to an existing Location.


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
