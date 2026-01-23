# # Specifications : [Scientific Objects] Search by criteria on Data

**Document history (please add a line when you edit the document)**

| Date       | Editor(s)                | OpenSILEX version       | Comment                                                         |
|------------|--------------------------|-------------------------|-----------------------------------------------------------------|
| 27/07/2023 | maximilian.hart@inrae.fr | 1.0.1 Ambitious Amber   | Document creation                                               |
| 10/12/2025 | yvan.roux@inrae.fr       | 1.4.9 Explosive Emerald | refactoring document to follow new template and file management |

<!-- TOC -->
* [# Specifications : [Scientific Objects] Search by criteria on Data](#-specifications--scientific-objects-search-by-criteria-on-data)
  * [functional requirements](#functional-requirements)
  * [Documentation](#documentation)
<!-- TOC -->

## functional requirements

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

## Documentation

- see the [technical document](../../technical-documentation/opensilex-core/scientific-object/criteria-search-on-scientific-object.md)