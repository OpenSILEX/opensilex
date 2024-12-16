| Date      |Author|Developer(s)| Version OpenSilex | Comment                             |
|-----------|------|------------|-------------------|-------------------------------------|
| 16/08/2024 |Alexia Chiavarino|Alexia Chiavarino| 1.3.0      | created spec - global map with site |

## Table of contents
* [Needs](#needs)
* [Solution](#solution)
* [Technical specifications](#technical-specifications)
    * [API](#api)
    * [Frontend](#frontend)
* [Limitations and Improvements](#limitations-and-improvements)
    * [Limitations](#limitations)
    * [Improvements](#possible-improvements)

## Needs

- Use case # 1 : As a user, I need to locate different elements in my instance on the map, apart from experiments.
- Use case # 2 : As a user, I need to locate all the sites in my instance on the map, even if the map is zoomed out.
- Use case # 3 : As a user, I need to see information about each site.
- Use case # 4 : As a user, I need to filter sites.

## Solution

A spatial module is added to the main menu. On click, a global map is displayed, with all sites in the instance by default and focus on.

If the extent of the sites is above a certain scale, they are clustered according to the distance between them. The number of sites represented by the dot is written inside. Clicking on a cluster zooms in on the extent of all the sites represented by the selected cluster.

Above the map, buttons allow you to select the element to be displayed (layer). For the moment, only the site layer is displayed. 
There are 2 others buttons to focus on the elements and to change the map background.

On the left, a layer management menu is open by default on sites. You can manage its visibility and color.
It is divided into 2 tabs: one to view the element list with details (click on the 'green eye') and the second to filter the layer. There are 3 filters : projects, facilities and year.

If you select an element from the list, it will automatically be selected on the map, and vice versa. From the map, you can only select elements that are not clustered, below a certain scale.

## Technical specifications
### API
#### Site
Added a specific `getSites` service to `SiteAPI`. This service allows to get only sites with address (so with spatial coordinates) and facility list linked.

To filter sites, we call the `searchExperiments` service because the filters (projects, species and year) are linked to the experiments that are made in facilities that are themselves hosted in sites (sites > facilities > experiments).
For the moment, we can't link sites and experiments without facilities. So we can't filter out sites without facilities and sites whose experiments are not linked to facilities.

### Front-end

### Tests
#### Site
- `testGetSites()` in `SiteAPITest` : test the service to search sites with detail and only sites with address.

## Limitations and Improvements
### Limitations
### Improvements

- add others elements (scientific objects, facilities,...)
- improve links between sites and experiments: add a site to the experiment form or create automatic links when moving from the OS to the installations, etc.
    