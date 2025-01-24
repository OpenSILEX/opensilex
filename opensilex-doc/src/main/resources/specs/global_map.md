| Date       |Author|Developer(s)| Version OpenSilex | Comment                               |
|------------|------|------------|-------------------|---------------------------------------|
| 16/08/2024 |Alexia Chiavarino|Alexia Chiavarino| 1.3.0      | created spec - global map with site   |
| 04/11/2024  |Alexia Chiavarino|Alexia Chiavarino| 1.3.0      | add facilities to global map |

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
- Use case # 2 : As a user, I need to locate all the sites and the facilities in my instance on the map, even if the map is zoomed out.
- Use case # 3 : As a user, I need to see information about each site and facility.
- Use case # 4 : As a user, I need to filter sites and facilities.

## Solution

A spatial module is added to the main menu. On click, a global map is displayed, with all sites in the instance by default and focus on.

If the extent of the sites is above a certain scale, they are clustered according to the distance between them. The number of sites represented by the dot is written inside. Clicking on a cluster zooms in on the extent of all the sites represented by the selected cluster.
The same behavior is applied to facilities.

Above the map, buttons allow you to select the element to be displayed (layer). For the moment, only the site and facility layers are displayed. 
There are 2 others buttons to focus on selected elements and to change the map background.

On the left, a layer management menu is open (by default on sites). You can manage its visibility and color.
It is divided into 2 tabs: one to view the element list with details (click on the 'green eye') and the second to filter the layer. There are 3 filters : projects, facilities and year. There are linked to experiments hosted by displayed elements.

If you select an element from the list, it will automatically be selected on the map, and vice versa. From the map, you can only select elements that are not clustered, below a certain scale.

## Technical specifications
### API
#### Site
Added a specific `getSitesWithLocation` service to `SiteAPI`. This service allows to get only sites with address (so with spatial coordinates).
To improve performance, we get only the minimum information : site URI/name, facility list associated and its location. Displayed details are get when a site is selected on the menu of the global map.

To filter sites, we call the `searchExperiments` service because the filters (projects, species and year) are linked to the experiments that are made in facilities that are themselves hosted in sites (sites > facilities > experiments).
For the moment, we can't link sites and experiments without facilities. So we can't filter out sites without facilities and sites whose experiments are not linked to facilities.
#### Facility
Added a specific `getFacilitiesWithGeometry` service to `FacilityAPI`. This service allows to get only the last location (date as parameter) of facilities with spatial coordinates.
As with sites, to improve performance, we get only the minimum information : facility URI/name and its last location.

As with sites, to filter facilities, we call the `searchExperiments` service and filter with the facility list to search for related experiments.
### Front-end

### Tests
#### Site
- `testGetSites()` in `SiteAPITest` : test the service to search only sites with address.
#### Facility
- `testGetFacilities()` in `FacilityAPITest` : test the service to search the last location of facilities.

## Limitations and Improvements
### Limitations
### Improvements

- add others elements (scientific objects, ...)
- improve links between sites and experiments: add a site to the experiment form or create automatic links when moving from the OS to the installations, etc.
    