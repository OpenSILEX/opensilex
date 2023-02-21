- **Description**:  This document describes the uses cases about data read.
- **Author**: Renaud COLIN
- **Tags**: #data #use_case 
- **Links**: [[use_cases]] [[analysis_use_cases]]

# Variable

See [[use_cases#Variable]]

## Mono dimensional variable

### data_variable_mono_equality

- Find all data concerning a given variable
	- Find all data about **plant temperature** 

## Multi-dimensional variable

# Value

## Mono-valued data

### data_mono_valued_read_equality

- Retrieve all data with a value equals to a given value
	- Ex: All temperature == 30°

### data_mono_valued_read_relation_order

- Retrieve all data according an order relation
	- Ex : all temperature > 30°
 **Note**
 - Only work if a relation order is applicable to the value

### data_mono_valued_read_in_interval

- Retrieve all data with a value inside an interval
	- Ex: all temperature > 20° and <= 30°
	
 **Note**
 - This use cases is an extension of **data_mono_valued_read_relation_order** use case
 
### data_mono_valued_read_in_value_set

- Retrieve all data with a value which is inside a value set
	- Ex: all temperature in **{10,20,30}**
	
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

This use case is linked to [[use_cases#Data on multiple target]]

In case of variable with multiple entity, we may want to filter data according one of these entity.
To perform this, we must pre-filter the variable, in order to have a variable with multiple entity

### data_multiple_target_by_entity

- Find all data with a multiple-entity variable, concerning a given entity
	- Ex: find data about leaf-tree distance, on a given leaf
	- Ex: find data about leaf-tree distance, on a given tree
	
# Provenance

## Data acquired by one device

### data_provenance_read_one_device

- Find all data acquired by a given device
	- Ex: find all data produced by the device **:sensing_device/1**

## Data acquired by one or multiple human operator


## Data acquired by multiple device

### data_provenance_read_device_type

- Find all data acquired by a given device type
	- Ex: find all data produced by a **temperature sensor**

### data_provenance_read_multiple_device_type

- Find all data acquired by one device and another (OR operator)
	-  Ex: find all data produced by the device **:sensing_device/1** and **:sensing_device/2**

## Data produced inside an experiment

### data_experiment_read

- Retrieve data acquired inside a given experiment
	- Ex: find all data produced inside the **phenoarch** experiment

# Timestamp


# Facility

#todo voir avec Brice