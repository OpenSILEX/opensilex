- **Description** : Description of visualization features
- **Date** : 07/04/2023
- **Author** : CHARLEROY Arnaud(MISTEA), COLIN Renaud (MISTEA), PRADO Sebastien (MISTEA)
- **Tags** :  [#visualization #data #event #annotation]

# Specifications

TODO

# API call

Several API calls are performed for visualization

## Data

The list of data for each selected scientific object is fetched (EventSerie)

## Annotations

For each data, we want to know if the agent used for the data acquisition (`data.provenance.provUsed.uri`), 
has annotation or not.

In order to make this operation faster the following steps are done : 

- `[data_agents_compute]`  :
  - Compute the Set of unique agents from the data list
  - This works well if we assume that the number of agent (device or operator) is relatively small, compared 
  to the number of data acquired by this agent
- `[agents_annotation_checking]` :
  - A custom method is used in order to compute the subset of agents which are annotated (Don't need to fetch the annotation list for the agent set)
  - We use a custom POST API `/core/annotations/hasAnnotations` which return for a list of URIs, which of these URIs has annotation or not

## Events

The list of events for each selected scientific object is fetched (DataSerie)

For these event we want to get : 
- The rdfType and rdfType name
- The start and end date

Several optimisation can be done for the fetching of these events
- Don't fetch other field between OpenSILEX and database (and potentially between OpenSILEX and the Client)
- Group several API call in one CALL ? 
