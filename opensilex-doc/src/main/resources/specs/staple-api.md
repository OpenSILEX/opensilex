**History**

| Date       | Author           | Developer(s)     | OpenSILEX version | Comment           |
|------------|------------------|------------------|-------------------|-------------------|
| 2023-10-03 | Valentin Rigolle | Valentin Rigolle | 1.1.0             | Document creation |

# Specifications : Staple API

## Needs

The **Staple API** is a GraphQL Mesh adapter for SPARQL. In order to use it with OpenSILEX, it needs an "ontology" file
describing the concepts and relations in the triple store. This file should be in turtle format and follows specific
rules to be converted correctly to a GraphQL schema by Staple. The rules are described in the
[Staple API documentation](https://epistemik-co.github.io/staple-api/#/docs/).

OpenSILEX needs to provide this file.

## Solution

We choose to expose an API endpoint to provide the Staple API ontology file. The endpoint will return text in the turtle
format such as the Staple API can use it directly as an ontology file.

To generate the file, we will use the concepts and relations defined in the triple store.

### Business logic

The algorithm used to transform the concepts in the triple store into a valid Staple API ontology file is the following
:

1. Extract _root classes_ from selected ontology file. By default, selected ontology files are `oeso-core`, `oeev` and
   `os-sec`.
2. Retrieve all subclasses of _root classes_, including themselves, and put them in the _set of classes to analyze_.
3. For each _class to analyze_, perform the following actions :
   1. Create a _class definition_ in the destination file. The base definition includes the subclass relationship and
      labels and comment, if present.
   2. For each datatype property of the class, create a _property definition_ in the destination file.
   3. For each object property of the class, create a _property definition_ in the destination file. If the range of the
      property is a class that is not in the _set of classes to analyze_, add it to the set.
   4. For each property restriction of the class, create the corresponding declarations in the destination file (domain,
      range, cardinality). If the range is a class that is not in the _set of classes to analyze_, add it to the set.
4. Search in the destination file the properties that have either zero range, or more than one range. The Staple API
   only accepts properties that have exactly one range. For each property that has more than one range, try to merge
   the ranges into one by looking for a common superclass. If no unique range can be found, or if the property has zero
   range, delete it from the destination file.

## Technical specification

The endpoint is accessible from the `StapleAPI` class. The service builds a Jena Model, which is then serialized into
turtle syntax and return by the service.

Two classes are responsible for building the Jena Model of the Staple ontology file :

- `StapleApiUtils` is the entry point, providing a single method called `getStapleModel` which returns a Jena Model.
  This is an injectable class. It contains the logic for extracting the root classes from the selected ontology files.
- `StapleModelBuilder` contains the logic to build the Model from _root class URIs_ and a provided `OntologyStore`. It
  performs the steps 2 to 4 as described above.

### Technical choices

In some cases, it is not possible to create a destination model that perfectly reflects the source concepts and
relations. In these situations, we decided of ways to solve the issues :

- Properties that have no range defined in the ontology are given an range based on their nature :
  - Datatype properties are given the `xsd:xstring` range
  - Object properties are given the `owl:Thing` range
- Properties that have more than one range are resolved using an algorithm briefly described in the step 4 of the
  algorithm. In reality, the process is the following :
  - Let $[r_0, r_1, ..., r_n]$ the $n$ ranges of the property.
  - If there exists $i$ such as $r_i$ is a superclass of all $r_0, ..., r_n$, then $r_i$ is taken as the single range
    of the property.
  - If no such $i$ exists, then the property is deleted.