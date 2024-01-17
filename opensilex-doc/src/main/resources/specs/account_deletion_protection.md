# Specifications : Account Deletion Protection

**Document history (please add a line when you edit the document)**

| Date | Editor(s)             | OpenSILEX version | Comment          |
|------|-----------------------|-------------------|-------------------|
| 12/01/2023 | yvan.roux@inrae.fr | 1.0.2             | Document creation |

## Table of contents

<!-- TOC -->
* [Specifications : Account Deletion Protection](#specifications--account-deletion-protection)
  * [Table of contents](#table-of-contents)
  * [Definitions](#definitions)
  * [Needs](#needs)
    * [Non-functional requirements](#non-functional-requirements)
  * [Solution](#solution)
  * [Technical specifications](#technical-specifications)
    * [RDF](#rdf)
    * [MongoDB](#mongodb)
  * [Limitations and improvements](#limitations-and-improvements)
<!-- TOC -->

## Definitions

- **Account deletion protection** : A mechanism to prevent the deletion of an account that has already created other resources.

## Needs

The need for account deletion protection arises to prevent data loss and to prevent inconsistencies in databases and bugs in the API and the graphical interface.

The purpose is to prevent the deletion of an account that is referenced in the metadata of a resource and potentially displayed on its detail page.


### Non-functional requirements

- **Performance** : The mechanism should be able to check the linkage of an account with other resources in a reasonable time.
- **Flexibility** : The mechanism should be easy to implement if another type of ressource linked with accounts is created.

## Solution

Two strategies have been implemented to prevent the deletion of an account that has already created other resources. One for resources stored in RDF databases and one for those stored in MongoDB.

## Technical specifications

### RDF

The `SPARQLService::requireUriIsNotLinkedWithOtherRessourcesInRDF(instanceURI, predicatesUrisToExclude)` method allows to verify in a single query that the specified URI is not the object of any triplet.

It is possible to customize the query by excluding certain triplets for which the resource has the right to be an object. In the case of accounts, they can be deleted even if they are the object of the FOAF:account predicate.

### MongoDB

Iterating over each collection and each value of each attribute of each object in MongoDB would be too resource-intensive. Instead, a system has been implemented where each module is responsible for saying if a resource A (represented by its URI) is linked to the MongoDB resources of its module.

The `ModuleWithNosqlEntityLinkedToAccount` interface is inherited by all concerned modules (for the moment only CoreModule.java) and implements the `accountIsLinkedWithANosqlEntity(accountURI)` method which returns a boolean. Thus, thanks to the `OpenSilex::getModulesImplementingInterface` method, you can, without adding dependencies, access all the modules implementing this interface and iterate over them to know if our resource is linked to others in the MongoDB database.

For code and javadoc see [AccountDAO.java](../../../../../opensilex-security/src/main/java/org/opensilex/security/account/dal/AccountDAO.java).

## Limitations and improvements

The MongoDB solution has its limitations. It necessitates modifications to the `CoreModule::accountIsLinkedWithANosqlEntity` method or the implementation of the `ModuleWithNosqlEntityLinkedToAccount` interface in a new module whenever a new link between accounts and an entity in MongoDB is established.

If this strong coupling between the two database systems were to become more widespread, it would necessitate a deep refactoring to automate the process.