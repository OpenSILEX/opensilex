- **Description**: Conception and implementation of the strategy of SPARQL results fetching
- **Author** : Renaud COLIN (INRAE MISTEA)
- **Date** : 23/08/2023
- **Tags**: `[#SPARQL]`


# Many-to-Many relationship

The fetching of values for Many-to-Many relationship are handled inside OpenSILEX.
By Many-to-Many fetching we mean : 

- The retrieval of all the values for a multivalued property `p` for `n` models of type `t`

In order to optimize the fetching of the values, we need to define a strategy which avoid the following performance bottlenecks/issues : 

- Avoid **N+1 query problem**. Example an API call which concerns n resources and which lead to n query executed to the database
- Ensure that pagination is well implemented
- Avoid to running a too complex query which retrieve all data in one step but which not yell executed by the database
- Limit memory consumption, either if we use garbage-collected language, we need to take care on this point, especially in the case
of a high READ charge

## Relation fetching with VALUES clause

### Description

- Execute the first SPARQL query `Q_mono` which retrieve a list of mono-valued data/object properties relations 
  - This query handle the filtering according search criteria and pagination
  - Results are stored inside a list `R`
- Extract `IDs` :  the set of id from `R`
- Execute a second SPARQL query `Q_Multi` which retrieve a list of multivalued data/object properties relations
  - The query get relation for each model with an id present in `IDs` with a `VALUES` clause
  - This query use the `GROUP_CONCAT` SPARQL operator in order to group all relation with a given seperator
  - This implies to `GROUP BY` elements by ID
- Read results from the `Q_Multi` query, and update each initial results from `R`

### Example 

Considering a type T which have the following properties : 

| Field name  | Predicate      | Cardinality | Type   |
|-------------|----------------|-------------|--------|
| name        | `rdfs:label`   | [0,1]       | String |
| description | `rdfs:comment` | [0,1]       | String |
| parent      | `:hasParent`   | [1,n]       | URI    |
| children    | `:hasChild`    | [0,n]       | URI    |


- The first query (`Q_mono`) is executed by a `SPARQLService` search method

```sparql
SELECT ?uri ?name ?description
WHERE {
    ?uri rdfs:label ?name
    OPTIONAL {
        ?uri rdfs:comment ?comment
    }
}
```

> `Q_mono` results example


| uri    | name    | description    |
|--------|---------|----------------|
| :uri_1 | "name1" | "description1" |
| :uri_2 | "name2" |                |


- The second query (`Q_Multi`) will fetch the multivalued field `parent` and `children`
- Here we use a space as a seperator

```sparql
SELECT ?uri 
    (group_concat(?parent;separator=" ") as ?parent__opensilex__concat)  
    (group_concat(?children;separator=" ") as ?children__opensilex__concat)
    
WHERE {
    ?uri :hasParent ?parent
    OPTIONAL {
        ?uri :hasChild ?children
    }
    VALUES ?uri (:uri_1 :uri_2)
}
GROUP BY ?uri
```

> `Q_Multi` results example

| uri      | parent__opensilex__concat | children__opensilex__concat         |
|----------|---------------------------|-------------------------------------|
| `:uri_1` | :parent_1 :parent_2       | :children_1                         |
| `:uri_2` | :parent_2                 | :children_1 :children_2 :children_3 |

- Then we just have to join by the `uri` field, the two result list.

> Final results example

| uri      | name    | description    | parent__opensilex__concat | children__opensilex__concat                  |
|----------|---------|----------------|---------------------------|----------------------------------------------|
| `:uri_1` | "name1" | "description1" | [`:parent_1`,`:parent_2`] | [`:children_1`]                              |
| `:uri_2` | "name2" |                | [`:parent_2`]             | [`:children_1`,`:children_2`,`:children_3` ] |


### Implementation

Implementation of this algorithm is made inside the `SPARQLListFetcher` (from `opensilex-sparql`) class
