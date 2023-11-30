# Specifications : [#mongodb] MongoDB DAOs and Service

**Document history (please add a line when you edit the document)**

| Date       | Editor(s)    | OpenSILEX version | Comment           |
|------------|--------------|-------------------|-------------------|
| 29/01/2023 | Renaud COLIN |                   | Document creation |

> ⚠️ _WARNING_ : This document is incomplete ! You can help by expanding it. ⚠️
>
> Currently covered topics :
>
> - {topic 1}
>
> Missing topics :
>
> - {topic 2}

# Table of contents

<!-- TOC -->
* [Specifications : [#mongodb] MongoDB DAOs and Service](#specifications--mongodb-mongodb-daos-and-service)
* [Table of contents](#table-of-contents)
* [Definitions](#definitions)
* [Analysis](#analysis)
  * [Non-functional requirements](#non-functional-requirements)
* [Solution](#solution)
  * [Business logic](#business-logic)
* [Technical specifications](#technical-specifications)
  * [Technical definitions](#technical-definitions)
  * [Detailed explanations](#detailed-explanations)
  * [Tests](#tests)
    * [Ensure that the MongoDBService is stateless](#ensure-that-the-mongodbservice-is-stateless)
    * [Test concurrents calls to MongoDBService](#test-concurrents-calls-to-mongodbservice-)
    * [Ensure that ClientSession is well closed in case of error](#ensure-that-clientsession-is-well-closed-in-case-of-error)
    * [Read Data Access Objects](#read-data-access-objects)
    * [Write Data Access Objects](#write-data-access-objects)
  * [Environment](#environment)
* [Limitations and improvements](#limitations-and-improvements)
* [Documentation](#documentation)
  * [MongoDB](#mongodb)
  * [Design-Patterns](#design-patterns)
<!-- TOC -->

# Definitions

- **{Term}** : {definition}

# Analysis

{Briefly describe the user needs}

- Use case #{number}: As a {user category}, I want to {action}.

## Non-functional requirements

{Describe all non-functional requirements in this section, and give precise metrics if possible.
Below are some common examples.}

- **{Performance}** : {limit of the acceptable performances (latency, execution time, etc.) for the feature}
- **{Security}** : {access restriction (which user should have access to the feature, are there potential
  vulnerabilities, etc.)}
- **{Ergonomy}** : {how accessible the feature should be (visual or textual representations, number of
  clicks needed to perform an action)}
- **{Reliability}** : {how resilient the feature should be (what happens if the user enters an invalid input,
  or if a service is unavailable)}

# Solution

{Describe the solution we chose in OpenSILEX. You can explain why this solution was chosen, which
other solutions were considered and why they were not kept.}

## Business logic

{If some specific business rules are applicable in the solution, describe them extensively in this
section. Business logic also includes authorization rules.}

# Technical specifications

## Technical definitions

- **{Term}** : {definition}

## Detailed explanations

{Describe the files, classes, methods, algorithms and architectural choices that are essential to the
solution. You can divide this section in subsections to keep it organized. For example, you can have
an API and Front-end subsections, but that is not mandatory.}


## Tests

### Ensure that the MongoDBService is stateless

### Test concurrents calls to MongoDBService 

### Ensure that ClientSession is well closed in case of error

### Read Data Access Objects

### Write Data Access Objects

## Environment

{Describe the packages and libraries required for the solution, and the specific version if needed.}

# Limitations and improvements

{Describe the known limits of the solution. If you have potential solutions to suggest, you
can specify them here.}

# Documentation

## MongoDB

## Design-Patterns

{List internal and external documentations relevant to the feature. For example, configuration
instructions or an external library documentation website}.