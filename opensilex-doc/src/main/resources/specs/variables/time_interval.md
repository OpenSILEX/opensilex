# Specifications : [Variables] {Time interval}

**Document history (please add a line when you edit the document)**

| Date       | Editor(s)          | OpenSILEX version | Comment           |
|------------|--------------------|-------------------|-------------------|
| 2024-01-05 | yvan.roux@inrae.fr | 1.0.3             | Document creation |


## Table of contents

<!-- TOC -->
* [Specifications : [Variables] {Time interval}](#specifications--variables-time-interval)
  * [Table of contents](#table-of-contents)
  * [Needs](#needs)
    * [Non-functional requirements](#non-functional-requirements)
  * [Solution](#solution)
  * [Technical specifications](#technical-specifications)
    * [Detailed explanations](#detailed-explanations)
    * [Tests](#tests)
  * [Limitations and improvements](#limitations-and-improvements)
<!-- TOC -->

## Needs

Allow the user to define the time between two data recording

### Non-functional requirements

For now not much, before this documentation the feature was largely hardcoded in the front. What we are looking for now is a functional feature with complete translation system. 

## Solution

A web service is available to get all the accepted values of time interval. This service send the values as an object containing 'id' and 'label' fields. The 'id' field never change, that what we have to store in the database. The 'label' field change with the language asked in the request. If any language is asked then it will be sent in english. 

For now there is no check for values when a variable is created nor updated.

## Technical specifications

### Detailed explanations

The different values of time intervals are given thanks to the `TimeIntervalEnum.java` Enum. This Enum allow developers to work easily with these values and to easily add or delete some values.

For translation, we use `ResourceBundle` and `Locale` classes withe `.properties` files. The translation keys are directly determined by the Enum itself.

### Tests

**TO DOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO**
{Describe the automatic tests related to this feature, where they are located and what they are supposed
to check.}

## Limitations and improvements

For now different values are given thanks to an Enum, it may be better if we get it from an existing ontology and directly store the URI rather than the actual key.

Even the translations could be stored thanks to ontology.