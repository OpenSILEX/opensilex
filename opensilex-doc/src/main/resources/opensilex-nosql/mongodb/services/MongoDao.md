---
title: MongoDB DAOs and Service
date: 15/12/2023
tags:
  - MongoDB
---

# Specifications : MongoDB DAOs and Service

**Document history (please add a line when you edit the document)**

| Date       | Editor(s)    | OpenSILEX version | Comment           |
|------------|--------------|-------------------|-------------------|
| 15/12/2023 | Renaud COLIN |                   | Document creation |


# Table of contents

<!-- TOC -->
* [Specifications : MongoDB DAOs and Service](#specifications--mongodb-daos-and-service)
* [Table of contents](#table-of-contents)
* [Definitions](#definitions)
* [Analysis](#analysis)
  * [Non-functional requirements](#non-functional-requirements)
    * [Performance](#performance)
    * [Reliability](#reliability)
* [Solution](#solution)
  * [Business logic](#business-logic)
  * [Technical specifications](#technical-specifications)
  * [Technical definitions](#technical-definitions)
  * [Detailed explanations](#detailed-explanations)
    * [DAOs and Services](#daos-and-services)
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

OpenSILEX rely on [MongoDB](https://www.mongodb.com/fr-fr) for the storage and the interrogation of several's kind
of data models.
The use of MongoDB is guided by the following reasons : 
- A **semi-structured** data-model which provides flexibility for developers for updating data-models and allow
to store several data-models inside the same database
- A **good performance** for OLTP and OLAP workload
- MongoDB provide **horizontal scalability** with **sharding** and **replication**
- MongoDB offers all the common functionality of a DBMS (**authentification**, **transaction**, **performance-tuning**, **database management**)
- A strong **community** and a **stable team**


{Briefly describe the user needs}

- Use case #{number}: As a {user category}, I want to {action}.

## Non-functional requirements

{Describe all non-functional requirements in this section, and give precise metrics if possible.
Below are some common examples.}

### Performance

### Reliability

# Solution

{Describe the solution we chose in OpenSILEX. You can explain why this solution was chosen, which
other solutions were considered and why they were not kept.}

## Business logic

{If some specific business rules are applicable in the solution, describe them extensively in this
section. Business logic also includes authorization rules.}

## Technical specifications

## Technical definitions

- **{Term}** : {definition}

## Detailed explanations

### DAOs and Services


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