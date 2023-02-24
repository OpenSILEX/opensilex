> **Description**: This document describe the contraints or inference rules which apply on Data insertion
> 
> **Author**: Renaud COLIN
> 
> **Date (last update)** : 24/02/2023
> 
> **Tags** : #data #mongodb #rdf #validation #provenance 
> 
> **Linked documents** : [[use_cases]]
 
# Summary
****
| Group               | Use case                                | Description |     |     |
|---------------------|-----------------------------------------|-------------|-----|-----|
| **Target**  | data_variable                           |             |     |     |
| **Value/Variable**  | data_variable_multiple_entity           |             |     |     |
| **Value/Variable**  | data_variable_multi_dim                 |             |     |     |
| **Value/Variable**  | data_variable_multiple_entity_multi_dim |             |     |     |
| **Target/Variable** | data_target_scientific_object           |             |     |     |
| **Target/Variable** | data_target_device                      |             |     |     |
| **Target/Variable** | data_target_facility                    |             |     |     |
| **Provenance**      | data_provenance_device                  |             |     |     |
| **Provenance**      | data_provenance_human_operator          |             |     |     |
| **Provenance**      | data_provenance_multiple_device         |             |     |     |
| **Provenance**      | data_experiment_insert                  |             |     |     |
****

# Target

## Existance

### data_target_exists

> Description

- The data target must exists inside RDF store

> Rule

- **Body** 
	- `rdf:type(?data,oeso:Data)`
	- `oeso:hasDataTarget(?data,?target)`
- **Head**
	- `rdf:type(?target,?targetType)

> Note

- The `targetType`  variable is an existentially quantified variable
- Here we wan't to check that the target has a type, without any other assertion on this type

# Variable

## Existance

### data_variable_exists

> Description

- The data variable must exists inside RDF store

> Rule

- **Body** 
	- `rdf:type(?data,oeso:Data)`
	- `oeso:hasVariable(?data,?variable)`
- **Head**
	- `rdf:type(?variable,oeso:Variable)

## Entity/Target

### variable_entity_target (NOT_IMPLEMENTED)

see [[use_cases#data_trigger_variable_observation_level]]

- The data target must be an instance of the variable entity

> Rule

- **Body**
    - `rdf:type(?data,oeso:Data)`
    - `oeso:hasDataTarget(?data,?target)`
    - `oeso:hasVariable(?data,?variable)`
    - `oeso:hasEntity(?variable?,?entity)`
- **Head**
    - `rdf:type(?target,?entity)`

## Datatype and value

### data_value_variable_datatype

> Description

- The datatype of the variable is specified with the `oeso:hasDataType` property
- The value of the data must have the same datatype as the data variable datatype

> Rule

- **Body** 
	- `rdf:type(?data,oeso:Data)`
	- `oeso:hasVariable(?data,?variable)`
	- `oeso:hasDataType(?variable?,?datatype)`
- **Head**
	- `oeso:hasDataType(?data,?datatype)`

## Link with provenance agent type

### provenance_agent_device_type

> Description

- Inside data provenance, if the agent is a device, the device must measure (`oeso:Measures`) the variable

> Rule

- **Head**
	- `rdf:type(?data,oeso:Data)`
	- `oeso:hasVariable(?data,?variable)`
	- `oeso:hasDataProvenance(?data,?dataProvenance)`
	- `prov:wasAssociatedWith(?dataProvenance?,?agent)`
	- `rdf:type(?agent,agentType)`
	- `rdfs:subClassOf(?agent,oeso:Device)`
- **Body**
	- `oeso:measures(?agent,?variable)`

> Note

- If using an aggregation between data provenance and device, then device type can be different between the RDF store and the data store.
- In this case, the device type is the device type in the triple store when the data were imported
- Other option ? update the variable by adding a relation between device and measured variable

# Provenance

## Existence

- See [prov-o](https://www.w3.org/TR/prov-o/) Ontology

### data_provenance_exists

> Description

- When using a global provenance inside a data-provenance, ensures that the provenance exists

> Rule (Constraint)

- **Body**
    - `rdf:type(?data,oeso:Data)`
    - `oeso:hasDataProvenance(?data,?dataProv)`
    - `oeso:hasProvenance(?dataProv,?provenance)`
- **Head**
    - `rdf:type(?provenance,prov:Activity)

> Note

- Usage of [prov:Activity](https://www.w3.org/TR/prov-o/#Activity) OWL Class as main class for provenance description
- #todo oeso:hasProvenance ?

## Agent

### data_provenance_agent_check

> Description

- If a global-provenance use some agents, any data-provenance derived from the global provenance, must reuse these agent
- In other terms, the data-provenance must not declare any agent in this case 

> Rule (Constraint)
 
- **Body**
    - `rdf:type(?data,oeso:Data)`
    - `oeso:hasDataProvenance(?data,?dataProv)`
    - `oeso:hasProvenance(?dataProv,?provenance)`
    - `prov:wasAssociatedWith(?provenance,?provenanceAgent)`
- **Head**
    - `! prov:wasAssociatedWith(?dataProv,?dataProvenanceAgent)

### data_provenance_agent_exists

> Description

- Ensure that any agent (device, software, person) which is declared as data-provenance agent, is declared with the good agent type inside the RDF database

> Rule (Inference)

- **Body** 
	- **[Mongo]**
		- `rdf:type(?data,oeso:Data)`
		- `oeso:hasDataProvenance(?data,?dataProv)`
		- `prov:wasAssociatedWith(?dataProv,?provenanceAgent)`
	- **[RDF]**
		- `rdf:type(?provenanceAgent,?agentType)` 
- **Head**
	- **[Mongo]**
		- `rdf:type(?provenanceAgent,?agentType)`

- The agent type in MongoDB is derived from agent type in RDF store

## Experiment

### data_experiment_exists

> Description

- Ensure that the experiment provided  inside the data-provenance, exists in the RDF database

> Rule (Constraint)

- **Body**
    - `rdf:type(?data,oeso:Data)`
    - `oeso:hasDataProvenance(?data,?dataProv)`
    - `oeso:hasExperiment(?dataProv,?experiment)`
- **Head**
    - `rdf:type(?experiment,oeso:Experiment)`

### data_experiment_access

> Description

- Ensure that the user which create data, has the right access for the experiment

> Rule

#todo 

### data_target_in_data_provenance_experiment

> Description

- When declaring data inside an experiment, depending on the target type, the target must be handled inside the experiment (ex: object inside an experiment)

> Rule (Constraint)

- **Head**
  - `rdf:type(?data,oeso:Data)`
  - `oeso:hasDataProvenance(?data,?dataProvenance)`
  - `oeso:withinExperiment(?dataProvenance,?experiment)`
  - `oeso:hasDataTarget(?data,?target)`
  - `rdf:type(?target,oeso:ScientificObject)`
- **Body**
  - `oeso:withinExperiment(?target,?experiment)`

or 
- `rdf:type(?target,?targetType)`
- `oeso:isExperimentHandled(?targetType)`

### target_in_experiment_by_name (CSV only)

> Description

- When declaring data inside an experiment about a scientific object, the object can be declared by his name 
- Only available for CSV import (since for users, it's easier to deal with name instead of URI)

> Note

- Since the name of an object inside an experiment is unique, there is no ambiguity about object resolution
- This don't work outside of experimental context, since multiple object can have the same name in global context. So we must check that the target has an URI as identifier if the experiment is not provided for data insertion.



