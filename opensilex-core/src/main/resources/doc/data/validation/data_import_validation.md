| Validation Group | Validation Sub-Group | Rule name              | Description | Link                        |
|------------------|----------------------|------------------------|-------------|-----------------------------|
| Variable         | Entity and target    | variable_entity_target |             | [[#variable_entity_target]] |
|                  |                      |                        |             |                             |
|                  |                      |                        |             |                             |

# Service parameters

- Experiment (optional) : Experiment in which data are produced
- Provenance (required) : Global provenance reused inside inside Data Provenance

# Existence

- **Experiment** : 
  - Check that experiment exist
- **Provenance** : 
  - Check that provenance exists
- **Variables** : 
  - Check that the declared variable exists
- **Device**: 
  - Check that device from DataProvenance exists
  - Retrieve device `rdf:type` and check that device is a SensingDevice or a Software
- **Target** : 
  - Check that target exists in database
  - Retrieve target `rdf:type`

# Variable

## Entity and target

### variable_entity_target

- The data target must be an instance of the variable entity

> Rule

- **Head**
    - `rdf:type(?data,oeso:Data)`
    - `oeso:hasDataTarget(?data,?target)`
    - `oeso:hasVariable(?data,?variable)`
    - `oeso:hasEntity(?variable?,?entity)`
- **Body**
    - `rdf:type(?target,?entity)`

## Datatype and value

> Description
- The datatype of the variable is specified with the `oeso:hasDataType` property
- The value of the data must have the same datatype as the data variable datatype

> Rule

- **Head**
  - `rdf:type(?data,oeso:Data)`
  - `oeso:hasVariable(?data,?variable)`
  - `oeso:hasDataType(?variable?,?datatype)`
- **Body**
  - `oeso:hasDataType(?data,?datatype)`

> Example

## Link with provenance agent type

### provenance_agent_type

- Inside data provenance, the device must be an instance of `oeso:SensingDevice`
- Device must measure the data variable

> Rule

- **Head**
  - `rdf:type(?data,oeso:Data)`
  - `oeso:hasVariable(?data,?variable)`
  - `oeso:hasDataProvenance(?data,?dataProvenance)`
  - `prov:prov_was_associated_with(?dataProvenance?,?device)`
  - `rdf:type(?device,oeso:Device)`
- **Body**
  - `rdfs:subClassOf(?device,oeso:SensingDevice)`
  - `oeso:measures(?device,?variable)`

Modeling and implementation note:
- If using an aggregation between data provenance and device, then device type can be different between the RDF store and the data store.
- In this case, the device type is the device type in the triple store when the data 
were imported. 

# Target

## Experimental context handling

### target_in_experiment

- When declaring data inside an experiment, depending on the target type, the target must be handled inside the experiment (ex: object inside an experiment)

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


### target_in_experiment_by_name

- When declaring data inside an experiment about a scientific object, the object can be declared by his name 
- Only available for CSV import (since for users, it's easier to deal with name instead of URI)

**Notes**
- Since the name of an object inside an experiment is unique, there is no ambiguity about object resolution
- This don't work outside of experimental context, since multiple object can have the same name in global context. So we must check that the target has an URI as identifier if the experimen is not provided for data insertion.

# JSON

```json
{
  "variable": ":plant_temperature_celcius",
  "value" : 51,
  "target" : ":plant1",
  "provenance" : {
      "experiment" : "",
      "provWasAssociatedWith" : [
        {
          "uri" : ":temperature_sensor_1",
          "rdfType" : "oeso:TemperatureCaptor"
        }
      ]
  }
}
```

# API

## JSON

## CSV


