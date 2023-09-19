# OntologyAPI

- Subject : This document describes the OpenSILEX Ontology API
- Author : Renaud COLIN
- Date : 09/02/2022

# Class

This section describes operations related to class management

## Services 

### `GET /ontology/rdf_type`

**Description** : return the description of a given class

**Method** : `OntologyAPI.getRDFType(type,ancestor)`

**Parameters** :
- `type` : URI of the class (_required_)
- `ancestor`: URI of a class ancestor. If set then the service must check that `type` is 
a descendant/sub-class of `ancestor`(_optional_)



### `GET /ontology/subclasses_of`

**Description** : return a tree-view of all sub-classes of a given class

**Method** : ``OntologyAPI.searchSubClassesOf(root,namePattern)``

**Parameters** : 
- **root** : URI of the root class (_required_)
- **namePattern**: Regex on class name (_optional_)

### `DELETE /vuejs/owl_extension/rdf_type/{uri}`

**Description** : Delete the class

**Method** : `VueOwlExtensionAPI.deleteRDFType(classURI)`

**Parameters** :
- **classURI** : URI of the class to delete(_required_)

**Notes** : 
- The deletion of a class works if the following condition are respected : 
  - The class is a custom class, i.e. a class not present directly into ontologies used by OpenSILEX, but added via services.
  Base classes can't be removed, or must be managed through ontologies with the help of domain experts
  - No resource is instance of this class, into database
  - The class doesn't have any subclasses, else you must delete them first
  - No data/object-property linked with the given class as `rdfs:domain` exist into the database.
- The deletion of a class trigger the deletion of all OWL restrictions between an `owl:DataTypeProperty` or `owl:ObjectProperty` and this class.


# Property 

This section describes operations related to property management

## Services

### `GET /ontology/property`

**Description** : return the description of a given class

**Path** : `OntologyAPI.getProperty(property,property_type,domain)`

**Parameters** :
- **property** : URI of the property (_required_)
- **property_type**: URI of the property type. If set then the service must check that the property has the given`type`.
Only `owl:DatatypeProperty`or `owl:ObjectProperty` types are accepted (_optional_)
- **domain** : URI of the property domain. If set then the service must check that `property` has the given `domain` or one of its descendant as domain (_optional_) 


### `GET /ontology/properties/{domain}`


**Description** : return a tree-view of all properties concerning the given domain

**Path** : `OntologyAPI.getProperties(domain,namePattern,includeSubClasses)` 

**Parameters** :
- **domain** : URI of the property domain. The service return all properties which have `domain` or one of its descendant as domain (_required_)
- **namePattern**: Regex on property name (_optional_)
- **includeSubClasses** : Boolean flag which indicate if the service must also return property which concerns only a domain descendant (_optional_) 


### `DELETE /ontology/property`

**Description** : Delete the property

**Method** : `OntologyAPI.deleteProperty(propertyURI,propertyType)`

**Parameters** :
- **property** : URI of the property to delete(_required_)
- **property_type**: URI of the property type. If set then the service must check that the property has the given`type`.
  Only `owl:DatatypeProperty`or `owl:ObjectProperty` types are accepted (_required_)

**Notes** :

- The deletion of a property works if the following condition are respected : 
  - This property is not used into any relation as a property/predicate by any resource
  - The class doesn't have any sub-properties, else you must delete them first
- The deletion of a property trigger the deletion of all OWL restrictions between an `owl:Class` and this property.
- **TODO** : The service actually don't check if the property is a "custom" property or not. To performs this, several way could be possible :
  - Check if the property is inside a SPARQL graph which is associated to an Ontology loaded inside an opensilex Module.
  - Save an additional triple or information (like a `VueClassExtensionModel`/`vocabulary:ClassExtension` for custom class), which allow to known if a property 
is custom or not.

# Restrictions

This section describes services related to OWL restrictions between class and properties.

## Model

In the OWL2 ontology (https://www.w3.org/TR/owl-ref/), restrictions are intended to express a rich semantic for associating
properties/attributes to class. Here some RDF triple pattern used to express some OWL functionality that we use.

- Add restriction to class : `:class rdfs:subClassOf :restriction`
- Add restriction on property : `:restriction owl:onProperty :property`
- Link individual from class to other class individual : `:restriction owl:onClass :value_class`
- Link individual from class to literal value : `:restriction owl:onDataRange :literal_class`
- Make the property value required : `:restriction owl:minQualifiedCardinality 1`.
- Make the property mono-valued :  `:restriction owl:maxQualifiedCardinality 1`
- Make the property multivalued : `:restriction owl:maxQualifiedCardinality 2` . Can be more than 2. OpenSILEX also consider as multi-valued, 
restriction with a min cardinality defined but no max cardinality defined.

## Services

### `GET /vuejs/owl_extension/rdf_type_properties/`

**Description** : Return the description of the type and the list of data and object properties linked with the type through OWL restrictions.

**Path** : `VueOwlExtensionAPI.getRDFTypeProperties(type,ancestor)`

**Parameters** :
- **type** : URI of the class (_required_)
- **ancestor**: URI of a class ancestor. If set then the service must check that `type` is a descendant/subclass of `ancestor`(_optional_)
  
**Notes** : 
- This service include for all data/object properties, which VueJs component can be associated (input component, view component), in order 
  to dynamically handle properties from ontology with low code in client side. 
- This service return properties which are linked to the class through OWL restriction. So if some property has `type` as domain,
but is not related to the class through restriction, then the property will not be included in service results.
- Instead, you must use the `getProperties(domain,namePattern,includeSubClasses)` service if you want to retrieve all properties with `type` as domain.

# Other

This section regroup other services present in the Ontology API.

## Services

### `GET /ontology/uri_label/`

**Description** : Returns the label associated with the given URI, or a NOT FOUND response if the URI does not exist
or has no label.

**Path** : `OntologyAPI.getURILabel`

**Parameters** :
- **uri** : URI of the resource (_required_)

### `GET /ontology/uris_labels/`

**Description** : Returns labels associated with the given URIs

**Path** : `OntologyAPI.getURILabelsList`

**Parameters** :
- **uri** : URIs of resources to get labels from (_required_)
- **context** : URI of a specific context to look for labels. Used to retrieve labels of scientific objects in an
  experiment for example.
- **searchDefault** : If **context** is specified, look first in the given context and then in the whole graph. Allows
  to find labels specific to a context and labels of global resources in one request.

**Notes** :
- For each given URI that is non-existant, or for which no label were found, a warning will be sent in the response
  metadata.