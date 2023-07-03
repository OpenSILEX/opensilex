# [Map] Clustered scientific objects on map

|Date|Author|Developer(s)|Version OpenSilex|Comment|
|----|------|------------|-----------------|-------|
|03/07/2023|Alexia Chiavarino|Alexia Chiavarino|1.0.0-rc+7.2|Spec creation|

<!-- TOC -->
## Table of contents
* [Needs](#needs)
* [Definitions](#definitions)
* [Solution](#solution)
* [Technical specifications](#technical-specifications)

<!-- TOC -->

## Needs

According to the map display scale, some scientific objects are no longer visible. For example, in an experiment, when severals scientific objects are far apart, we need to zoom out to see the whole but the scientific objects shape aren't large enough to be visible at this scale.

- Use case # 1 : As a user, I need to locate all the scientific objects of my experiment on the map, even if the map is displayed on large scale.

## Definitions

**Cluster**:

Dot on the map representing severals scientific objects close to each others.  

## Solution

Above a certain scale, the scientific objects are represented by dots (the geometry type is "point") and are clutered according to the distance between them. The number of scientific objects represented by the dot is written inside the dot. 

Clicking on a cluster zooms in on the extent of all the scientific objects represented by the selected cluster.

Only visible scientific objects are clustered. In the panel menu, if a scientif object type is unchecked/checked, clusters are updated.

## Technical specifications

Use the VueLayers Library.

In the map component, above a  certain scale, display a "cluster" layer with visible scientific objects as the data source.

Clicking on a cluster to calculate the zoom-in extent, retrieve the centroid for each type of scientific object geometry to simplify calculations. 
