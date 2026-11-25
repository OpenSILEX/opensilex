# Specifications : [Data] Graphics

**Document history (please add a line when you edit the document)**

| Date       | Editor(s)       | OpenSILEX version   | Comment           |
|------------|-----------------|---------------------|-------------------|
| 25/11/2026 | Maximilian Hart | Vue 3 migration     | Document creation |

> ⚠️ _WARNING_ : This document is incomplete ! You can help by expanding it. ⚠️
>
> Currently covered topics :
>
> - the graphics of the Visualization tabs (experiment, scientific object, device, variable)
> - the graphic of the home dashboard
> - the graphic of the Facility Monitoring tiles
> - the interactions shared by every graphic
>
> Missing topics :
>
> - the graphic opened from the experiment map (the map is not migrated to Vue 3 yet)
> - the access rights needed to add an event or an annotation from a graphic

## Table of contents

<!-- TOC -->
* [Specifications : [Data] Graphics](#specifications--data-graphics)
  * [Table of contents](#table-of-contents)
  * [Definitions](#definitions)
  * [functional requirements](#functional-requirements)
  * [Business logic](#business-logic)
    * [Data displayed](#data-displayed)
    * [Locations of the graphics](#locations-of-the-graphics)
    * [Series, colors and symbols](#series-colors-and-symbols)
    * [Events](#events)
    * [Images](#images)
    * [Interactions](#interactions)
    * [Menu](#menu)
    * [Changes from the previous version](#changes-from-the-previous-version)
  * [Documentation](#documentation)
<!-- TOC -->

## Definitions

- **Graphic** : drawing of numeric data over time, one point per data: the date of the data on the X axis, its value
  on the Y axis.
- **Series** : the points drawn with the same color and symbol, and named once in the legend. For example, the data
  of one variable measured on one scientific object.
- **Event pin** : a pin drawn at the bottom of the graphic at the date of an event, with the first letter of the event
  type inside.
- **Image pin** : a pin marked "I" drawn on a data point whose value was measured from an image.
- **Image panel** : the row of images displayed above the graphic, filled by clicking image pins.

## functional requirements

Users need to see how measured values change over time, compare several elements (scientific objects, devices) or two
variables on the same graphic, and relate the values to what happened (events) or to what was measured (images).

- Use case #1: As a user, I want to draw the data of some scientific objects of an experiment, for one or two
  variables, between two dates.
- Use case #2: As a user, I want to draw the data of one variable measured on a scientific object.
- Use case #3: As a user, I want to draw the data of one variable measured by a device, optionally only on some
  scientific objects.
- Use case #4: As a user, I want to draw the data of a variable measured by some of the devices that measured it.
- Use case #5: As a user, I want to see on the home page the recent data of a variable chosen by the administrator.
- Use case #6: As a user, I want to see the environmental conditions of a facility (see the
  [facility monitoring specifications](facility/facility-monitoring.md)).
- Use case #7: As a user, I want to see the events of the drawn elements on the graphic.
- Use case #8: As a user, I want to see the images a value was measured from, and annotate them.
- Use case #9: As a user, I want to see the details (provenance, data, annotations) of a point, and add an event or
  an annotation from it.
- Use case #10: As a user, I want to zoom on a period, switch between points and lines, display the graphic in
  fullscreen and download it as an image.

## Business logic

### Data displayed

- Only the data of **numeric variables** (decimal or integer) can be drawn. With another datatype, a message names
  the variable and its datatype, and nothing is drawn.
- The data is drawn by date, oldest first.
- At most **50 000** data are loaded per series. Above, a warning gives the total number of data and the name of the
  series: only the first 50 000 are drawn.
- When no data matches the filters, a message says so and the graphic is empty.
- Filters given empty are not applied (e.g. no end date: until today).
- The graphic is only drawn when the search is launched. The filters panel then closes.

### Locations of the graphics

| Location                                  | Filters                                                                                                                           | Default dates                                                    |
|-------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------|
| Experiment, Visualization tab             | scientific objects (required, max 15), variables (required, max 2, only those having data on the experiment and the selected objects), dates, provenance | the whole experiment if it is over, otherwise the last 15 days |
| Scientific object, Visualization tab      | variable (required, 1, only those having data on the object), dates, provenance                                                  | last 15 days                                                     |
| Device, Visualization tab                 | variable (required, 1, only those having data from the device), scientific objects (max 5), dates, provenance                    | last 15 days                                                     |
| Variable, Device associated data tab      | devices (required, several, only those that measured the variable), dates, provenance                                            | last 15 days                                                     |
| Home dashboard                            | period (day, week, month, year, until now) and devices (all by default), in the settings of the graphic                          | last day                                                         |
| Facility, Monitoring tab (tile graphic)   | period and "all provenances", in the settings of the graphic                                                                     | last 7 days                                                      |

- A search without its required filters shows a message instead of drawing.
- The home dashboard graphic only exists when a variable is set in the front configuration
  (`dashboard.graph1.variable`). Its header shows the text set in `dashboard.graph1.dataLocationInformations`, and a
  badge with the current period.
- The Facility Monitoring graphic draws the series computed by the facility monitoring (see its specifications): the
  first computed series is displayed, the others and the series of each provenance can be displayed from the legend.
  Its points are computed values, not stored data: they have no details and no right-click menu.

### Series, colors and symbols

- **Experiment**: one series per (scientific object, variable). One color per scientific object, one symbol per
  variable. With two variables, the series are named "object / variable" and the second variable has its own Y axis
  on the right.
- **Scientific object**: one series, named after the object.
- **Device**: one series per scientific object the data was measured on, named after the object. The data without
  scientific object is named after the variable.
- **Variable** and **home dashboard**: one series per (scientific object, device). One color per scientific object,
  one symbol per device. With several devices, the series are named "object / device". The data without scientific
  object is named after its device, with a color of its own.
- The colors and symbols come from fixed lists, in the order of the elements; they start again from the first one
  after the last one.
- The Y axis is named after the variable, followed by its unit between brackets.

### Events

- The "Show events" switch is in the filters of the four Visualization tabs. Next to it, a number gives how many
  events the drawn elements have between the selected dates. It is counted again when the elements or the dates
  change, and after an event is created from the graphic.
- The events are only loaded with the data, when the search is launched with the switch on.
- An event is drawn at its start date, or at its end date for an instant event.
- Pins that would overlap are stacked on top of each other. They are placed again after each zoom and each change of
  the size of the graphic.
- **Experiment**: one legend entry per scientific object (`Events -> object`), with the color of the object.
- **Scientific object** and **device**: one "Events" legend entry, one color per event type.
- **Variable**: one legend entry per device (`Events -> device`), one color per event type.
- Hovering a pin shows the event type and its dates: begin and end, "Ongoing" when it has no end, or a single date
  for an instant event.

### Images

Only on the Experiment and Scientific object Visualization tabs.

- A point whose data provenance references an image (first "prov_used" entry of the provenance) gets an image pin.
  The pin is **red** when the image has annotations.
- The image pins of each series have their own legend entry, so they can be hidden.
- Clicking an image pin adds the image to the image panel, if it isn't there yet, and scrolls the panel to it.
- Hovering a pin outlines the card of its image in the panel, and the other way round.
- Each card shows the thumbnail, the image type, its target and its date, with a red dot when the image has
  annotations. Clicking the thumbnail shows the image in a bigger size.
- The menu of a card allows to:
    - see the details of the image (description and provenance);
    - see its annotations (read only);
    - annotate it: its card gets the red dot at once, its pin turns red at the next search;
    - hide it: the image only leaves the panel, nothing is deleted.
- A new search empties the panel.

### Interactions

- **Hover** a data point: a tooltip shows the series, the date and the value, and the other series are dimmed.
- **Click** a data point: the point turns black, and its provenance, its data and its annotations are displayed
  under the graphic. Clicking it again hides them. Clicking another point or an event pin replaces them.
- **Click** an event pin: the type, dates, description and creator of the event are displayed under the graphic.
- **Right-click** a data point opens a menu:
    - **Add an event**: opens the event creation form, with the date of the data and its target (or, for data
      without target, the device of its provenance). On the Experiment tab, the event is created in the context of
      the experiment. After the creation, the number of events is updated, and the graphic too when it displays the
      events.
    - **Annotate data**: opens the annotation creation form on the data. After the creation, the annotations
      displayed under the graphic are updated when they are the ones of this data.
- **Zoom**: drag horizontally across the graphic to zoom on a period, or use the mouse wheel. A "Reset zoom" button
  then displays the whole period again.
- **Legend**: clicking a name hides or shows its series (data, image pins or events).
- **Help**: the "?" button of the header opens the help of the graphic, in the language of the user.
- The details under the graphic are not displayed in fullscreen. Opening a form or the help leaves the fullscreen
  first.

### Menu

The menu (☰) of the header allows to:

- switch between **points** and **lines** connecting the points of each series. The zoom and the hidden series are
  kept. The graphics of the home dashboard, of the Variable tab and of the Facility Monitoring tiles open with lines,
  the others with points, as in the previous version;
- display the graphic in **fullscreen**;
- **download** the graphic as a PNG image. The image has a fixed size, whatever the screen, and keeps the zoom, the
  points or lines mode and the hidden series. Its title gives the name and the URI of the element (experiment,
  variable...). The clicked point is not drawn in black. The file is named `YYYY-MM-DD_graphic_opensilex.png`.

### Changes from the previous version

Compared to the Vue 2 version of OpenSILEX:

- Every graphic uses the same component, with the same interactions.
- Experiment: one symbol per variable (it was one per series, chosen automatically).
- Device: one series per scientific object (all the objects were merged into one series).
- Variable and home dashboard: one color per scientific object and one symbol per device (it was one color per
  device).
- Zoom with the mouse wheel and "Reset zoom" button added; the navigator bar under the graphic was removed.
- Download: no limit on the number of points (it was refused above 1 500 points).
- Image pins: an image is added to the panel by a click (it was by hovering), and the card menu "hides" the image
  from the panel.

## Documentation

- see the help of the graphic, opened by its "?" button
- see the [facility monitoring specifications](facility/facility-monitoring.md)
