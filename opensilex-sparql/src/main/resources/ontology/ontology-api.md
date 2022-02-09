# OntologyAPI

- Subject : This document describes the OpenSILEX Ontology API
- Author : Renaud COLIN
- Date : 09/02/2022

# Class


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
- **includeSubClasses** : Flag which indicate if the service must also returns property which concerns only a domain descendant (_optional_) 


# Restrictions

This section describes services related to OWL restrictions between class and properties.

# Model

In the OWL2 ontology (https://www.w3.org/TR/owl-ref/), restrictions are intended to express a rich semantic for associating
properties/attributes to class. Here some RDF triple pattern used to express some OWL functionality that we use.

- Add restriction to class : `:class rdfs:subClassOf :restriction`
- Add restriction on property : `:restriction owl:onProperty :property`
- Link individual from class to other class individual : `:restriction owl:onClass :value_class`
- Link individual from class to literal value : `:restriction owl:onDataRange :literal_class`
- Make the property value required : `:restriction owl:minQualifiedCardinality 1`.
- Make the property mono-valued :  `:restriction owl:maxQualifiedCardinality 1`
- Make the property multi-valued : `:restriction owl:maxQualifiedCardinality 2` . Can be more than 2. OpenSILEX also consider as multi-valued, 
restriction with a min cardinality defined but no max cardinality defined.

## Services

### `VueOwlExtensionAPI.getRDFTypeProperties(type,ancestor)`

**Description** : Return the description of the type and the list of data and object properties linked with the type through OWL restrictions.

**Path** : `/vuejs/owl_extension/rdf_type_properties/`

**Parameters** :
- **type** : URI of the class (_required_)
- **ancestor**: URI of a class ancestor. If set then the service must check that `type` is a descendant/sub-class of `ancestor`(_optional_)
  
**Notes** : 
- This service include for all data/object properties, which VueJs component can be associated (input component, view component), in order 
  to dynamically handle properties from ontology with low code in client side. 
- This service return properties which are linked to the class through OWL restriction. So if some property has `type` as domain,
but is not related to the class through restriction, then the property will not be included in service results.
- Instead you must use the `getProperties(domain,namePattern,includeSubClasses)` service if you want to retrieve all properties with `type` as domain.