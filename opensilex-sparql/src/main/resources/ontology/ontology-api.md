# OntologyAPI

- Subject : This document describe the OpenSILEX Ontology API
- Author : Renaud COLIN
- Date : 09/02/2022

# Class

## RDF model

## Services 
### `getRDFType(type,ancestor)`

**Description** : return the description of a given class

**Path** : `/ontology/rdf_type`

**Parameters** :
- **type** : URI of the class (_required_)
- **ancestor**: URI of a class ancestor. If set then the service must check that `type` is 
a descendant/sub-class of `ancestor`(_optional_)

### `searchSubClassesOf(root,namePattern)`

**Description** : return a tree-view of all sub-classes of a given class

**Path** : `/ontology/subclasses_of`

**Parameters** : 
- **root** : URI of the root class (_required_)
- **namePattern**: Regex on class name (_optional_)


# Property 

## RDF model

Properties : 
- **rdfs:label** :
- **rdfs:comment** : 
- **rdfs:domain** :
- **rdfs:range** : 
- **rdfs:subPropertyOf**

## Services

### `getProperty(property,property_type,domain)`

**Description** : return the description of a given class

**Path** : `/ontology/property`

**Parameters** :
- **property** : URI of the property (_required_)
- **property_type**: URI of a class ancestor. If set then the service must check that `type` is
  a descendant/sub-class of `ancestor`(_optional_)
- **domain** : URI of the property domain. If set then the service must check that `property` has the given `domain` or one of its descendant as domain (_optional_) 


### `getProperties(domain,namePattern,includeSubClasses)`

**Description** : return a tree-view of all properties concerning the given domain

**Path** : `/ontology/properties/{domain}`

**Parameters** :
- **domain** : URI of the property domain. The service return all properties which have `domain` or one of its descendant as domain (_required_)
- **namePattern**: Regex on property name (_optional_)
- **includeSubClasses** : Flag whi (_optional_) 
