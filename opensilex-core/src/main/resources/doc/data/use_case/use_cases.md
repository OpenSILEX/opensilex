# Description

## Definition and example

### Data 

Acquisition of a variable's value, concerning some object, at a T time, according a provenance

> Example

The **12 October 2022 at 12am,** the **temperature of the plant** **:plant/1** is **30° celcius**.
This value has been acquired by the **temperature sensor :/DS18B20** inside the experiment **phenoarch**

> Representation

-   `value` : 30
-   `date` : 12 October 2022 at 12am
-   `variable` : Plant temperature in celcius
-   `target` : the plant **:plant/1**
-   `provenance` :  A data acquisition with a temperature sensor

> Variable type

The value can represent an arbitrary datatype : integer, decimal, float, date, string, array or a nested object

The data value type depends of the variable `datatype`

- Datatypes
	- **Numeric** : Temperature, humidity
	- **String** : orientation (WEST, EAST, NORTH, SOUTH)
	- **ID/reference** : ID of some resource/object (scientific object)
	- **Multi-dimensional/Object** : RGB component of a color
	- **Multi-valued**:  a DNA sequence

The data model must allow to represent this diversity in datatype and if possible by allowing the definition of an additional scheme for better data-quality/validation.

### Provenance

- https://www.w3.org/TR/prov-o/

Data acquisition context, according several level of precision/granularity

> General informations

- `name`: Provenance name
- `experiment`: Experiment in which data has been produced

> Agents

- `agents` (**Agent**) : Object/Person which/who acquire the value
	- A software
	- An operator/person
	- A sensing device

> Activity

- `activity` (**Activity**): Description of data acquisition activity
	- Temperature measured with a temperature sensor
	- A software compute
	- Mesure of a plant height by a person

# Value/Variable

## Mono dimensional variable

### data_variable

> Description

- Declare data acquisition concerning a simple variable (one entity, one simple value)

> Example

- Ex: The temperature of the plant is `35`° Celsius
- Ex: The height of the plant is `24.5` cm

> Knowledge representation



### data_variable_multiple_entity

> Description

- Declare data acquisition with a variable which have several entity

> Example

- Ex: mesure the distance (Euclidian distance) between a plant and a leaf
- Two entity 
	- Plant 
	- Leaf


## Multi-dimensional variable

### data_variable_multi_dim

> Description

- Declare data acquisition with multi-dimensional variable

> Example

- Measure the RGB color of a plant
	- Dimensions of the color
		- R : red parameter
		- G : green parameter
		- B : blue paremeter
- Other example : the orientation of some field
	- Dimensions
		- Angle (ex: 25°)
		- Orientation : WEST

### data_variable_multiple_entity_multi_dim

> Description

- Declare data acquisition with a variable which have several entity, and which have several component

> Example

- Ex: Declare the directed distance between two tree, done by some sheep
- Three entity 
	- The starting point : a tree
	- The destination point : another tree
	- The sheep
- Dimensions 
	- Distance (ex: 8km)
	- Direction (ex: 52°)

# Target/Variable

## Data on one target

### data_target_scientific_object

> Description

- Declare data acquisition about one plant

> Example

- Ex: the temperature of the plant **:plant/p1** is 35 °Celsius
- Target 
	- type : Plant
	- id: **:plant/p1
		
### data_target_device

> Description

- Declare data about some device/sensor, can be usefull when the experimental study is the sensor/device itself

> Example

- Ex calibration of a temperature sesnor 
- Target:
	- type : temperature sensor
	- id : **:sensing_device/1**

### data_target_facility

> Description

- Declare data about a facility

> Example

- The temperature inside the greenhouse `:greenhouse/a` is 34°c
- Target:
	- type : GreenHouse (Facility)
	- id : **:greenhouse/a

## Data on multiple target

see [[#data_variable_multiple_entity]]

# Provenance

> Description

## Data acquired by one device

### data_provenance_device

> Description

- Declare measures which have been acquired automatically by a sensing-device or a software

> Example

- The temperature of the plant has been acquired by the temperature sensor  **:sensing_device/1**
- Provenance Agent : 
		- type : Temperature sensor 
		- id:  **:sensing_device/1


## Data acquired by one or multiple human operator

### data_provenance_human_operator

> Description

- Declare measures which have been acquired by an human operator

> Example

- The plant height has been measured by Alice
- The temperature of the plant has been acquired with a thermometer by **bob**
	- Provenance Agent : 
		- type : Person
		- id:  **bob

- In this case several agents participates to the data acquisition:
	- The two human operator
	- The thermometer 

> **Notes**

How to represent the difference between a device used by an operator, and a device which acquire a mesure ? 
- The provenance activity is different : 
- Provenance agent ? 

## Data acquired by multiple device

### data_provenance_multiple_device

- The temperature of the plant has been acquired by the temperature sensor  **:sensing_device/1** and **:sensing_device/2** 

**Notes** 
- Multiple data acquisition or one data with multiple device ?

## Data produced inside an experiment

### data_experiment_insert

> Description

- Describe in which experiment (`oeso:Experiment`) the data has been acquired

> Example

- The temperature of the plant was measured inside the **:xp1** experiment

# TimeStamp

# Write workflow

Describe the workflow which is executed when inserting data

## Update variable observation level

### data_trigger_variable_observation_level

> Description

- When inserting data about a variable on several target type, the variable  observation level must be updated with all new target type
- The old observation level are keep, only new level are added

> Example

- Mesure plant height on some parcel
- Mesure plant height on some sub-parcel
- The plant height must have parcel and sub-parcel as new observation level 

> Rule

- **Head**
	- `rdf:type(?data,oeso:Data)`
	- `oeso:hasVariable(?data,?variable)`
	- `rdf:type(?target,?targetType)`
	- `oeso:hasDataTarget(?data,?target)`
- **Body**
	- `oeso:hasObservationLevel(?variable,?targetType)`

> Note

- Instead of appying validation about target type and variable entity, the variable level is updated
	- This is done like this, in order to allow the reuse of variable, on several target type
-  The update of variable observation level must be done only if data insertion is successfull
	- Moreover, it's ensure that only one SPARQL UPDATE query is performed by data chunk/batch (avoid one write/data)

# Summary
****
| Group               | Use case                                | Description |     |     |
|---------------------|-----------------------------------------|-------------|-----|-----|
| **Value/Variable**  | data_variable                           |             |     |     |
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