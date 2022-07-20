MongoDB usage in OpenSILEX
=============================================

# Introduction

# Multiple data source transaction for write operation (create,update,delete)

## Introduction

For several resources, write operations are done on several data sources (TripleStore and MongoDB).
For all these resources, we proceed like in a distributed database, since each corresponding API/service has
to handle coherence upon multiple datasource by correctly handle transaction in case where one operation on a data source fail.

The list of resources which need this are :

- **Device** : description in RDF and metadata in mongodb
- **Germplasm** : description in RDF and metadata in mongodb
- **Scientific Objects** : description in RDF and geospatial in mongodb
- **Move (Event)** : description of event in RDF and position (coordinates, custom coordinates and text coordinates) in mongodb
- **Data** : data in mongodb and device association in RDF

## Analysis of transaction handling

### Introduction

We consider for any datasource 4 operations :
- Transaction start
- Update in transaction context (active transaction)
- Transaction commit
- Transaction rollback

### 2PC (two phase commit)

To handle these case the 2PC (two phase commit) protocol exists (https://www.techtarget.com/searchapparchitecture/definition/two-phase-commit-2PC).
- Prepare phase : the protocol ensure that all database have saved the transaction update, if one database operation fail, then all operation are rollback
- Commit phase : the protocol tells all database to commit transaction changes

Applied to our case :
- **Prepare phase** : SPARQL and MongoDB updates
    - SPARQL insertion fail -> rollback changes in MongoDB (if there are some changes)
    - MongoDB insertion fail -> rollback changes in SPARQL (if there are some changes)
- **Commit phase** :  SPARQL and MongoDB insertion OK -> commit SPARQL and MongoDB

This approach is well known and documented and have some drawbacks :
-  **synchronous (blocking)**
    - if TripleStore no respond, then MongoDB commit is blocked
    - if MongoDB no respond, then TripleStore commit is blocked
- **Errors during commit phase** (i.e: network errors, not enough RAM or disk space), then distributed database can be in an incoherent state
    - MongoDB commit fail and SPARQL commit OK -> can't rollback changes in SPARQL since transaction has been committed
    - SPARQL commit fail and MongoDB commit OK -> can't rollback changes in MongoDB since transaction has been committed

The second point can be blocking, even if it's probability of occurrence is low

### SAGA

The SAGA protocol (https://developer.ibm.com/articles/use-saga-to-solve-distributed-transaction-management-problems-in-a-microservices-architecture/)

 can resolve these two points :
- **synchronous (blocking)** : SAGA
- **Errors during commit phase** : allow developers to define a "compensation" transaction which define which action to performs in case of errors

The main drawback of this protocol is the complexity, since that for all operation then a "compensation" must be defined.
We can define the following compensation :

- **Create** : performs a delete
- **Update** : save the old model, delete the new model and insert the old model
- **Delete** : save the old model and insert it

### Links
- https://developers.redhat.com/blog/2018/10/01/patterns-for-distributed-transactions-within-a-microservices-architecture#possible_solutions
- https://stackoverflow.com/questions/128377/what-is-the-best-way-to-do-distributed-transactions-across-multiple-databases
- https://www.techtarget.com/searchapparchitecture/definition/two-phase-commit-2PC
- https://microservices.io/patterns/data/saga.html
- https://stackoverflow.com/questions/171876/how-do-two-phase-commits-prevent-last-second-failure
- https://developer.ibm.com/articles/use-saga-to-solve-distributed-transaction-management-problems-in-a-microservices-architecture/
- https://www.baeldung.com/cs/saga-pattern-microservices

# Architecture

## MongoDB Insertion

## Distributed transaction handling
