# [Map] Display graph from the map

| Date    |Author|Developer(s)| Version OpenSilex | Comment              |
|---------|------|------------|-------------------|----------------------|
|05/07/23|Alexia Chiavarino|Alexia Chiavarino| 1.0.0-rdg| created Spec|

## Table of contents
* [Needs](#needs)
* [Definitions](#definitions)
* [Solution](#solution)
* [Technical specifications](#technical-specifications)

## Needs

- Use case # 1 : As a user, I want to display a chart of variable data from selected scientific objects on the map.

## Definitions

**...**:



## Solution

Add a button "Display graph" above the map. By default, the button is disabled. It becomes selectable when at least one scientific object is selected on the map.

Clicking on the button opens a modal with the visualization search form for choosing variables to be displayed, the period, provenance and event display. The selection of scientific objects can be modified using the scientific object selector.

By default, the begin date in the form is one week before the current day. The maximum number of scientific objects that can be selected is 15 and 2 for variables.

Clicking on "Visualize" button displays the graph in the modal and closes the search form.

## Technical specifications

### Models



### API
### Tests
### Environment changes

## Limitations and Improvements
### Limitations
### Improvements

