# [Map] Display graph from the map

| Date     |Author|Developer(s)| Version OpenSilex | Comment              |
|----------|------|------------|-------------------|----------------------|
| 28/07/23 |Alexia Chiavarino|Alexia Chiavarino| 1.0.0-rdg| created Spec|

## Table of contents
* [Needs](#needs)
* [Solution](#solution)
* [Technical specifications](#technical-specifications)

## Needs

- Use case # 1 : As a user, I want to display a chart of variable data from selected scientific objects on the map.

## Solution

Add a button "Display graph" above the map. By default, the button is disabled. It becomes selectable when at least one scientific object is selected on the map.

Clicking on the button opens a modal with the visualization search form for choosing variables to be displayed, the period, provenance and event display. The selection of scientific objects can be modified using the scientific object selector.

A begin date in the form is set by default. The maximum number of scientific objects that can be selected is 15 and 2 for variables.

Clicking on "Visualize" button displays the graph in the modal and closes the search form.

## Technical specifications
### Front
When several elements are selected on the map (scientific objects, devices, zones), a filter is applied to the selection, keeping only "scientific object" type elements. The number of selectable scientific objects is limited to 15. Beyond this limit, the button is disabled again.


### API
The `searchScientificObjectsWithGeometryListByUris` service get first the geometry of scientific objects in Mongodb and then get the details in RDF4J. These two  sources of information are merged and returned.
In the visualization form, the scientific objects selected on the map are listed in the scientific object selector.
In the scientific object search mode, with the list of scientific objects in the experiment, the pre-selected scientific objects on the map are checked, and clicking on the "show>only selected" button lists all pre-selected scientific objects.

To achieve this, the code for this [merge request in II.3.B ](https://forgemia.inra.fr/OpenSILEX/opensilex-dev/-/merge_requests/1020) (for germplasm groups) has been partially implemented and adapted.
### Tests

Scientific objects tests are located in `ScientificObjectApiTest`.

The following tests check the correct behaviour when searching scientific objects with geometry :

- `testSearchScientificObjectsWithGeometryListByUris` : test if the service only get scientific objects with a geometry in the right experiment.
- `testSearchScientificObjectsWithGeometryListByUrisWithoutExperimentFail` : test if a request without experiment URI as parameter returns an error.

