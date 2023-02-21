
# Variable

## Mono dimensional variable


## Multi-dimensional variable

# Value

## Mono-valued data

### data_mono_valued_insert

- Multiple type : numeric, float, date, boolean, alphanumeric
- The value type, depends of the variable `datatype`

- The temperature of the plant is `53`° Celsius
- The height of the plant is `24.5` cm

[[read_use_cases]]
 
## Multi-valued data

## Multi-dimensional data

- The RGB color of the leaf is `{r: 55, g: 42, b:240}`

# Target

## Data on one target

- (Scientific Object) The temperature of the plant **:plant/p1** is 35 °Celsius
- (Device) Ex: calibration of <some_variable> of the sensing device **:sensing_device/1** is equal to 2
- (Facility) The temperature of the greenhouse `:greenhouse/a` is 34 °Celsius

## Data on multiple target

​￼￼￼Service parameters
- The distance between the `leaf and the tree` is 53cm 
- Must well define entities for the variable
  - Entity 1 : The Leaf
  - Entity 2 : The Tree

# Provenance

## Data acquired by one device

- The temperature of the plant has been acquired by the temperature sensor  **:sensing_device/1**

## Data acquired by one or multiple human operator

- The temperature of the plant has been acquired with a thermometer by **bob and alice**
- In this case several agents participates to the data acquisition:
	- The two human operator
	- The thermometer 

**Notes**
How to represent the difference between a device used by an operator, and a device which acquire a mesure ? 
- The provenance activity is different : 
- Provenance agent ? 

## Data acquired by multiple device

- The temperature of the plant has been acquired by the temperature sensor  **:sensing_device/1** and **:sensing_device/2** 

**Notes** 
- Multiple data acquisition or one data with multiple device ?

## Data produced inside an experiment

### data_experiment_insert

- The temperature of the plant was measured inside the **:xp1** experiment