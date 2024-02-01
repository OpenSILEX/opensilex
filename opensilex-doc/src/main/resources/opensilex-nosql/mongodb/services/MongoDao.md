---
title: MongoDB DAOs and Service
description: Description of MongoDB DAO and service
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

- **OLTP** : Online Transactional Processing
- **OLAP** : Online Analytical processing
- **P99 Latency** : The maximum latency observed for 99% of request
  - Ex a P99 latency of 10ms imply that 99% of the request take less thant 10ms

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


## Non-functional requirements

### Performance and reliability

#### SLA (service-level-agreement)

- The table below describe the expected performance for commons read and write operations 
- This only includes DAOs operation (it doesn't include business logic or the whole request performed inside a single API method)
- The operation don't expect multi-threading, this means that latency can be reduced if multi-threading is available
- Of course, it could be greatly optimized, but it's the minimal acceptable for an acceptable user experience

| **Operation**                                   | **Size** | **P99 Latency** | **Notes** |
|-------------------------------------------------|----------|-----------------|-----------|
| Insert an unique element                        | 1        | 100 ms          |           |
| Get by unique identifier                        | 1        | 100 ms          |           |
| Update an unique element                        | 1        | 100 ms          |           |
| Delete an unique element                        | 1        | 100 ms          |           |
| Insert an unique element                        | 1        | 100 ms          |           |
| Get by unique identifier                        | 1        | 100 ms          |           |
| Update an unique element                        | 1        | 100 ms          |           |
| Delete an unique element                        | 1        | 100 ms          |           |
| Search 1000 element (with index-covered query)  | 1000     | 100 ms          |           |
| Search 10000 element (with index-covered query) | 10000    | 5 s             |           |
| Insert 1000 element                             | 1000     | 1 s             |           |
| Insert 10000 element                            | 1000     | 5 s             |           |
| Delete 1000  element (with index-covered query) | 1000     | 1 s             |           |

- This SLA should be respected for any small dataset (<10M of document on a MongoDB single server without replication)
- Of course, they are variant regarding of the nature of the dataset, the size of the document and the underlying hardware
but for these basic operation on very-small data the P99 latency should keep this magnitude

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