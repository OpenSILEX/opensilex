
<h1> SPARQL Graph Storage</h1>

**Description** : This document describe how OpenSILEX store models with the usage of SPARQL graph <br>
**Author** : Renaud COLIN <br>
**Date** : 19/01/2022

# Define storage graph for a SPARQLResource

For any model which inherit from `SPARQLResourceModel`, we can configure where (in which graph/context) these models should be stored.

## Use a generated graph

OpenSILEX can use a generated URI as graph storage. 

```java
@SPARQLResource(
    ontology = Time.class,
    resource = "Instant",
    graph = "instant"
)
public class InstantModel extends SPARQLResourceModel {}
```

* Default storage into `http://opensilex.dev/set/instant` (`dev:set/instant`) graph
  
### Graph generation

The generated graph URI is the concatenation of three parts : `<baseURI>/set/<graph_name>`

* **baseURI** : property into config file : `ontologies.baseURI`
    ```yaml:
    ontologies
        baseURI: http://opensilex.dev/
        baseURIAlias: dev
    ```
* Graph name : described with `@SPARQLResource.graph` annotation property
    ```java
    @SPARQLResource(
          graph = "instant"
    )
  public class InstantModel extends SPARQLResourceModel {}
    ```
## Use an absolute graph

OpenSILEX can also use an absolute URI for graph storage. 
In this case the value for `@SPARQLResource.graph` annotation property must be a **parsable** URI.

* Expanded URI 

```java
@SPARQLResource(
    ontology = OA.class,
    resource = "Motivation",
    graph = "http://www.w3.org/ns/oa"
)
public class MotivationModel extends SPARQLNamedResourceModel<MotivationModel> { 
    
}
```

Here, the default storage graph is `http://www.w3.org/ns/oa`


* Prefixed URI 

```java
@SPARQLResource(
    ontology = Oeso.class,
    resource = "concept",
    graph = "opensilex:graph"
)
public class ExampleModel extends SPARQLNamedResourceModel<ExampleModel> { 
    
}
```
Here, the default storage graph is `opensilex:graph`


## Use no graph

If no graph is specified then model instance are stored into the global repository.

```java
@SPARQLResource(
    ontology = Oeso.class,
    resource = "concept"
)
public class ExampleModel extends SPARQLNamedResourceModel<ExampleModel> { 
    
}
```

If we consider `http://opensilex.dev/` as value for `ontologies.baseURI` property, <br>
then the default storage graph is `http://opensilex.dev/`.

Even if it works, always specify the default graph whenever possible, for two reasons : 
* **Organisation** : it's easier for humans to find some model description inside a particular graph
* **Performance** : usually, it's also easier for the triplestore to search inside one particular graph 
instead of searching into the whole repository (especially when additional graph/context indexes are specified)


# Configure graph for SPARQLProperty

When linking any resource (subject) to another resource (object) with a property, <br>
we may want to configure in which graph this object is stored.

The `SPARQLProperty.useDefaultGraph` annotation boolean property allow to configure the object storage. <br>

Behaviors according values are : 
* `true` (default value) : object is stored inside it's own default graph
* `false` : the object is stored within the same graph as the subject

Annotation Class link : (**opensilex-sparql**) `org.opensilex.sparql.annotation.SPARQLProperty`

## Use the default object graph

Here two equivalent examples : 

<table border="0">

<tr>
    <td>useDefaultGraph = true</td>
    <td>useDefaultGraph default value</td>
</tr>
  <tr>
    <td>

```java
@SPARQLResource(
        ontology = Oeev.class,
        resource = "Event",
        graph = "event"
)
public class EventModel extends SPARQLResourceModel {
    
      @SPARQLProperty(
              ontology = Time.class,
              property = "hasBeginning",
              useDefaultGraph = true
      )
      private InstantModel start;
}
```
  </td>
  <td>

  ```java
@SPARQLResource(
        ontology = Oeev.class,
        resource = "Event",
        graph = "event"
)
public class EventModel extends SPARQLResourceModel {

      @SPARQLProperty(
              ontology = Time.class,
              property = "hasBeginning"
      )
      private InstantModel start;
}
  ```
  </td>
  </tr>
</table>

* Here we consider that `InstantModel` for the property start (`time:hasBeginning`) 
  are stored into default `InstantModel` graph (example : `dev:set/instant`)

### Behaviors

Considering any relation R :  **(Subject,Predicate,Object)**

**Read** 

* Subject relations are stored into the subject graph
  * This one is used for subject-objects matching (**object-linking** in get/search)
* Object relations are stored into object graph.
  * The object graph is not auto-generated into the SPARQL query when using `SPARQLService` get/search 
  * You must explicitly add this graph inside SPARQL query if you need it 
    for filtering on some object property, excepted `uri` and `name` (**nested-filtering**).

**Write**

* Recursive object creation (null URI) :
  * Use object default graph to search uri duplicate
* Link to existing object (not null URI)
  * Use object default graph to check object existence
  

## Use default object graph

If you want to store some object within the subject default graph, you just have to
set `@SPARQLProperty.useDefaultGraph` to `false`

```java
@SPARQLResource(
        ontology = Oeev.class,
        resource = "Event",
        graph = "event"
)
public class EventModel extends SPARQLResourceModel {

      @SPARQLProperty(
              ontology = Time.class,
              property = "hasBeginning",
              useDefaultGraph = false
      )
      private InstantModel start;
}
```
* Here we consider that `InstantModel` for the property start (`time:hasBeginning`) are stored into default `EventModel` graph (example : `dev:set/event`)


### Behaviors

Considering any relation R :  **(Subject,Predicate,Object)**

**Read**
* Subject relations are stored into the object graph
  * This one is used for subject-objects matching (**object-linking** in get/search)
* Object relations are stored into subject graph.
  * Since the subject-graph is already generated by the SPARQL query you can put your filters
    inside this graph (**nested-filtering**), if you need to use them on some object property.
    
**Write**
* Recursive object creation (null URI) :
  * Use model default graph to search uri duplicate
* Link to existing object (not null URI)
  * Use model default graph to check object existence