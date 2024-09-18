# [Scientific Objects] Search by criteria on Data

Author: Maximilian Hart

Date: 27/07/2023

Developer(s): Maximilian Hart

Version: Ambitious Amber


<!-- TOC -->
* [[Scientific Objects] Search by criteria on Data](#scientific-objects-search-by-criteria-on-data)
  * [Definition of the feature](#definition-of-the-feature)
  * [Ontology](#ontology)
  * [Back-end](#back-end)
    * [Newly defined classes :](#newly-defined-classes-)
    * [Modification to searchScientificObjects service](#modification-to-searchscientificobjects-service)
    * [How the getScientificObjectsThatMatchDataCriteria(...) function in DataDAO works](#how-the-getscientificobjectsthatmatchdatacriteria-function-in-datadao-works)
  * [Front-end](#front-end)
    * [Modified files :](#modified-files-)
    * [New files :](#new-files-)
    * [CriteriaSearchModalCreator](#criteriasearchmodalcreator)
<!-- TOC -->

## Definition of the feature

Initially requested for the Vitis explorer project, the goal was to add a filter to the list of Scientific Object filters.
This filter would allow the user to retain only Objects that have data that validates an undefined amount of criteria connected
by And/Or logic.  

For the first version the criteria are only connected by an "And". For example : Give me all the plants that have a height 
bigger than 30cm AND a yield lesser than 50%.

The supported comparator **operators** are :

- `<` 
- `>`
- `<=`
- `>=` 
- `=`

The supported **variable types** are 
- Datetime
- Dates 
- Numbers (decimal or nay).

***

## Ontology

In `Oeso.owl` ontology file some new terms have been created to define the different operator types : 

- `Oeso:AndOperator` : And
- `Oeso:OrOperator` : Or
-` Oeso:LessThan` : <
- `Oeso:MoreThan` : >
- `Oeso:LessOrEqualThan` : <=
- `Oeso:MoreOrEqualThan` : >=
- `Oeso:EqualToo` : =

***

## Back-end

### Newly defined classes :

**CriteriaDTO** : A class to hold a list of `SingleCriteriaDTO` and a logical operator. For now every request will only 
have a single `CriteriaDTO` where the logical operator is a `Oeso:AndOperator`.

**SingleCriteriaDTO** : A single criteria that contains a variable uri, a comparator type and a value.

### Modification to searchScientificObjects service

- A new attribute of type `CriteriaDTO`. It is used separately before the main Sparql request as we are using Data
, therefore a Mongo request. 
- If a non null `CriteriaDTO` is present then the function `getScientificObjectsThatMatchDataCriteria(...)`
, defined in `DataDAO`, is called to create a list of `ScientificObject` uris that validate all the criteria. 
- This list is then added to the normal uris attribute of the `ScientificObject` search filter.

### How the getScientificObjectsThatMatchDataCriteria(...) function in DataDAO works

This function calls another to generate a list of Mongo Documents that are then applied to a mongo aggregation.

**Creation of the aggregation :**

The aggregation has 2 main steps :

- **1) Data Filtering** : Filter out all data that doesn't concern the variables given in the criteria list. Then for each variable the data's value
must also validate the criteria. All the criteria that concern the same variable are placed in an AND, then each of these ANDs
are connected by an OR. This can seem counter-intuitive but really isn't as a single Data can obviously not
concern multiple variables at the same time.

Example : If the criteria list is `VarA < 7 && VarB > 23/02/1996`, then the Data fetching part of the aggregation will be `{var = VarA && value < 7} || {var = VarB && value > 23/02/1996}`.

To parse the values I created a new function in the `DataValidateUtils` class called convertData(URI dataType, String value). 
This will parse the value using the same mechanism as for Data imports.

- **2) Object Filtering :** Once we have all the data that validates each criteria individually, we want to extract the Scientific Objects that
have at least one data for each AND of the previous step. To do this we do a group by target, during the group by operation we create a list of variables
that were found along-side the target. If the size of this variable list matches the quantity of ANDs of the previous step 
then the target can be retained


## Front-end

### Modified files :

`ScientificObjectsView` and `ExperimentScientificObjects`, where I added a criteriaDTO filter to
the filter list of each of these classes. For this filter 2 new files were added.

### New files :

- `CriteriaOperatorSelector`, a simple file containing a FormSelector that allows the user
to select a mathematical operator, from the ones mentioned in the Ontology section of this documentation (<,>,<=,>= and =).

- `CriteriaSearchModalCreator`, is a field that has a similar way of functioning as a FormSelector,
I decided not to use a FormSelector as the "request builder" idea behind this field is too diferent from the
"select some items" idea behind the FormSelector, even if the final display is quite similar.

### CriteriaSearchModalCreator

**Outer appearance :**

When the field is empty, so when no criteria have been created, it looks exactly the same as a FormSelector. 
Hitting the two little arrows will take us to a pop-up where we can start building are criteria.

When some criteria have been added, they are recapitulated in the same manor as a FormSelector. The variable's labels are
used instead of their uris, same for the mathmatical operator and then the value goes at the end. For example :
TailleDePlante > 12.5.

Finally, just like for a FormSelector, we can clear and clear-all. In fact for now this is the only way to remove criteria from the list,
the inner part of the modal only allows us to modify criteria already present in the list.

**Inside the modal :**

The used-cases of this pop-up are : 
- **Add a criteria** : adds an empty criteria to the list, shown by a box containing 3 empty fields.
This is done by hitting the "+" button.
- **Modify criteria** : By filling in, or modifying one of its fields. For the variable a VariableSelector is used, for
the operator a `CriteriaOperatorSelector` and for the value the field depends on the datatype of the variable. 
For example a Date selector if the variable datatype is a date. By default, a text field is used.
- **Validate criteria list**, the list is only taken into account for the scientific object search once this button has been hit.
- **Revert to last validation**, the criteria visible inside the modal aren't taken into account until the user validates. However
the inner display is still saved, this allows the user to close the pop-up and come back without losing the information. 
The revert button is useful if he/she realises he/she made a blunder since the last validation.