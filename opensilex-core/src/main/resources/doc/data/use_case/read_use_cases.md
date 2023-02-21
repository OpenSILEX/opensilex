- **Description**:  This document describes the uses cases about data read.
- **Author**: Renaud COLIN
- **Tags**: #data #use_case

# Value

## Mono-valued data

### data_mono_valued_read_equality

- Retrieve all data with a value equals to a given value
	- Ex: All temperature == 30°

### data_mono_valued_read_relation_order

- Retrieve all data according an order relation
	- Ex : all temperature > 30°
 **Note**
 - Only work if an order relation is applicable to the value

### ### data_mono_valued_read_in_interval

- Retrieve all data with a value inside an interval
	- Ex: all temperature > 20° and <= 30°
	
 **Note**
 - This use cases is an extension of **data_mono_valued_read_relation_order** use case
 
### data_mono_valued_read_in_value_set

- Retrieve all data with a value which is inside a value set
	- Ex: all temperature in [10,20,30]
	
 **Note**
 - This use cases is an extension of **data_mono_valued_read_equality** use case
 
## Multi-valued data

## Multi-dimensional data

### data_multi_dim_read_one_dimension_value_equality

### data_multi_dim_read_multiple_dimension_value_equality

# Target

## Data on one target

### data_target_read

- Find all data about a target
	- ex :  all data about concerning the plant **:plant/p1**

### data_target_read_type

- Find all data about a plant 
	-  ex :  all data concerning a plant

## Data on multiple target

### data_multiple_target_by_entity

- Find all data about a plant
- 
# Provenance

## Data acquired by one device


## Data acquired by one or multiple human operator


## Data acquired by multiple device


## Data produced inside an experiment

### data_experiment_read

- Retrieve data acquired inside a given experiment