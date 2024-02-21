# Specifications : [Variables] Sample interval

**Document history (please add a line when you edit the document)**

| Date       | Editor(s)          | OpenSILEX version | Comment           |
|------------|--------------------|-------------------|-------------------|
| 2024-02-15 | yvan.roux@inrae.fr | 1.2.1             | Document creation |


## Table of contents

<!-- TOC -->
* [Specifications : [Variables] Sample interval](#specifications--variables-sample-interval)
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

Allow the user to define the granularity of sampling
granularity accept following values :
- millimeter
- centimeter
- meter
- kilometer
- Field
- Region

### Non-functional requirements

Use the ontology to store the values of sample.

## Solution

A web service is available to get all the accepted values of sample interval. This values correspond to the subtype of the `vocabulary:SampleInterval` class. The label is the displayable name of the sample interval and can be translated in different languages, english is returned by default (no language asked or translation not found in the asked language).

## Technical specifications

### Detailed explanations

The different values of sample intervals are stored in the ontology as subtypes of the `vocabulary:SampleInterval` class. The translation is also stored in the ontology as `skos:prefLabel`. For now labels are available in english and French.

For now, every type are created in the oeso ontology, but we should reuse other ontology to define the sample interval.

## Front

In the Vue-JS client, sample intervals are loaded in the store at the start of the client and reloaded each time the user will change the language (to get the correct translation label).

Label is displayed and URI is sent to back (for variable creation and update)

### Tests

For now there is only one unit test on `SPARQLService::searchAndReturnUrisAndPrefLabel` method. It tests if the method returns the right values for a given parent type. It does not test for label search yet.