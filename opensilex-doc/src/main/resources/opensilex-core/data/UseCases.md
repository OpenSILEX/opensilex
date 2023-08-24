# Atomic/Minimal use case

## Description

### Data/Measure

Acquisition of a variable's value, concerning some object, at a T time, according a provenance

- The concerned object is called `target`, and can either be a scientific-object, a device (example : device calibration
  measure) or a facility.

> Example

The **12 October 2022 at 12am,** the **temperature of the plant** **:plant/1** is **30° Celsius**.
This value has been acquired by the **temperature sensor :/DS18B20** inside the experiment **phenoarch**

- `value` : 30
- `date` : 12 October 2022 at 12am
- `variable` : Plant temperature in Celsius
- `target` : the plant **:plant/1**
- `provenance` :  A data acquisition with a temperature sensor, inside the experiment **phenoarch**

### Provenance

- https://www.w3.org/TR/prov-o/

Data acquisition context, according several level of precision/granularity

> General information

- `name`: Provenance name
- `experiment`: Experiment in which data has been produced

> Agent

Object/Person which/who acquire the value. Can be either :
- A software
- An operator/person
- A sensing device

> Activity

Description of data acquisition activity
- Temperature measured with a temperature sensor
- A software compute
- Measure of a plant height by a person

## Constraints

### data_constraint_value_variable_datatype
- Validation of  cohérence between data-type and variable expected data-type

### data_constraint_reference_exist
- Validation of the existence of the referenced objects  : target, agent, variable, experiment etc

#### data_contraint_reference_target_exist
#### data_contraint_reference_provenance_agent_exist
#### data_contraint_reference_variable_exist
#### data_contraint_reference_experiment_exist

###  data_contraint_target_type
- Validation of the target type (not mandatory)

###  data_contraint_target_experiment_access
- Ensure that scientific object is accessible by a user inside a given experiment

## Usages

 This section describes the use cases/search query that can be evaluated according to the given use case.

### data_date_read

What is the temperature the 12 October ->  access by date

- Relation interval, search inside a datetime range

### data_provenance_agent_read

- What measure has been taken by the sensor today (or another day) > access by agent (provenance)

### data_target_read
- What is the temperature of the plant -> access by target

### data_target_type_read
- What is the temperature of all my plants -> access by target type

### data_read_value_equality
- Strict equality : ex, temperature == 30°

### data_read_value_relation_order
- Get all measure with a temperature >= 30°

### data_read_value_in_interval
- Interval : ex temperature > 20 ° and <= 30 °

### data_read_value_in_set

- Membership in a value set : Ex: all temperature in {10,20,30}

### data_read_value_regex
- String matching : ex: labels matching the regex `/example.*/`

### data_read_provenance_experiment
- Get measure taken inside some the experiment -> access by experiment

# Multiple entities

## Description
Declare data acquisition on several target of interest

## Example
Measure the distance (Euclidean distance) between a plant and a leaf

## Usages

### data_multi_target_type_read

- What is the distance between my plants and their leaves -> access by target type (extension of Simple Example)

### data_multi_target_read

- What is the distance between the plant A and the leaf B -> access by target (extension of SimpleExample#access by
  target type)

# Multi-dimensional variable

## Description

Declare data acquisition with multi-dimensional variables.

- A multi-dimensional variable V is a tuple of k dimension : 
  - `(dim_1, …, dim_k)`
- For each data-acquisition, the value is a dictionary which map to a dimension of V, a value :
  - `(dim_1: value1, …, dim_k: value_k)`

## Examples

> Measure the RGB color of a plant

- Dimensions of the color: (R, G, B) where :
    - R : red component of the color
    - G : green component
    - B : blue component

> Count the number of adventices in a plot

- Dimensions : (S, C) where :
    - S: species of the adventice
    - C: count of the adventices of this species in the plot

## Constraints

- Validation of the existence of the dimension inside the variable.
    - (extension of [Validation of the existence of the variable contraint](#datacontraintreferencevariableexist)
- Validation of the coherence between data-type and variable dimension expected data-type
    - (extension of [Validation of the cohérence between data-type and variable expected data-type](#dataconstraintvaluevariabledatatype)

## Usage

### data_read_dimension

- What are the measures with a red component >= 200 ? -> access by value of a dimension (instead of the whole value)
    - Extension of access by value of all dimension
      
# Multiple provenance agents

## Description

Describe measures which been acquired by multiple agent

- The provenance is composed of multiple agents which are either devices or human operators
  Example
  The temperature of the plant has been acquired with a thermometer by bob.
  In this case we have two agents
  - A human operator : bob
  - A device/sensor : the thermometer

## Usage

### data_read_provenance_multi_agent

- filter on multiple provenance agent
    - (extension of access by agent)

# Multiple targets

## Description

A single measure concerns multiple targets

## Example

A sensor in the center of 4 plots measures the temperature. This data is relevant for each of the plots.  

# Environmental measures

## Description

A measure does not have a target

## Example
A sensor in a field measures a temperature value. The value is not directly relevant to the field but only to the device
and time of the measure.

## Usage

- Find all data-acquired inside a given area
    - In this case, we need to have a gps coordinate for the device (given by a move)

# Liens

https://forgemia.inra.fr/OpenSILEX/opensilex-dev/-/tree/###
data_documentation/opensilex-core/src/main/resources/doc/data