# Specifications : [Variables] Time interval

**Document history (please add a line when you edit the document)**

| Date       | Editor(s)          | OpenSILEX version | Comment           |
|------------|--------------------|-------------------|-------------------|
| 2024-02-15 | yvan.roux@inrae.fr | 1.2.1             | Document creation |


## Table of contents

<!-- TOC -->
* [Specifications : [Variables] Time interval](#specifications--variables-time-interval)
  * [Table of contents](#table-of-contents)
  * [Needs](#needs)
    * [Non-functional requirements](#non-functional-requirements)
  * [Solution](#solution)
  * [Technical specifications](#technical-specifications)
    * [Detailed explanations](#detailed-explanations)
  * [Front](#front)
    * [Tests](#tests)
<!-- TOC -->

## Needs

Allow the user to define the time between two data recording.

Proposed values are :
- millisecond
- second
- minute
- hour
- day
- week
- month
- year
- unique measure

### Non-functional requirements

Use the ontology to store the values of time interval.

## Solution

A web service is available to get all the accepted values of time interval. This values correspond to the subtype of the `time:TemporalUnit` class. The label is the displayable name of the time interval and can be translated in different languages, english is returned by default (no language asked or translation not found in the asked language).  

## Technical specifications

### Detailed explanations

The different values of time intervals are stored in the ontology as subtypes of the `time:TemporalUnit` class. The translation is also stored in the ontology as `skos:prefLabel`. Many translations are available for each time interval. The default language is english.

To correspond with our original values, we added to subtypes of `time:TemporalUnit` : `oeso:Milisecond` and `oeso:UniqueMeasure`. Both has english and French translations.

## Front

In the Vue-JS client, time intervals are loaded in the store at the start of the client and reloaded each time the user will change the language (to get the correct translation label).

Label is displayed and URI is sent to back (for variable creation or filter)

### Tests

For now there is only one unit test on `SPARQLService::searchAndReturnUrisAndPrefLabel` method. It tests if the method returns the right values for a given parent type. It does not test for label search yet.