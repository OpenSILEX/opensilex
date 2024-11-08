---
title: Data POST JSON service validation 
---

<!-- TOC -->
* [Data scheme](#data-scheme)
  * [Required fields](#required-fields)
  * [Rules](#rules)
    * [Provenance agents/activity reuse](#provenance-agentsactivity-reuse)
    * [Device/Variable association](#devicevariable-association)
      * [Example](#example)
* [Validations](#validations)
  * [variable, value](#variable-value)
  * [target, provenance.experiments](#target-provenanceexperiments)
  * [(provenance](#provenance)
* [Implementations](#implementations)
  * [TODO](#todo)
* [API returns](#api-returns)
<!-- TOC -->

# Data scheme

> Data creation JSON example

```json
[
    {
        "uri": "http://opensilex.dev/id/data/1598857852858",
        "date": "2020-08-21T00:00:00+01:00",
        "timezone": "ECT",
        "target": "http://plot01",
        "variable": "http://opensilex.dev/variable#variable.2020-08-21_11-21-23entity6_method6_quality6_unit6",
        "value": "4810",
        "confidence": 0.5,
        "provenance": {
            "uri": "http://opensilex.dev/id/provenance/provenancelabel",
            "prov_was_associated_with": [
                {
                    "uri": "opensilex-device:device-1"
                }
            ],
            "prov_used": [
              {
                "uri": "opensilex-document:document-1"
              }
            ],
            "experiments": [
                "opensilex-xp:1"
            ]
        },
        "metadata": "{ \"LabelView\" : \"side90\",\n\"paramA\" : \"90\"}",
        "raw_data": [
            {}
        ]
    }
]
```
## Required fields

The following fields are required : 

- `date`
- `provenance.uri`
- `value`

The following fields are required depending on your input

- `provenance.prov_was_associated_with.uri` : If you define any provenance agent with the `prov_was_associated_with` property, 
then each element of the array must have the uri field defined. Moreover, the URI must match an existing device or account
- `provenance.prov_used.uri` : If you define any provenance activity with the `prov_used` property,
then each element of the array must have the uri field defined. Moreover, the URI must match an existing item (no restriction on type)

## Rules

### Provenance agents/activity reuse

When you define a provenance (with `provenance.uri` field), if the "global" provenance has activity/agents and the data "local" provenance has no activity/agent,
then the activities/agents from the global provenance are used

> Example

Supposing the provenance 

```json
{
  "uri": "http://opensilex.dev/id/provenance/provenancelabel",
  "prov_was_associated_with": [{"uri": "opensilex-device:device-1"}],
  "prov_used": [{"uri": "opensilex-document:document-1"}]
}
```

If you insert a data with no provenance agent, then the activities/agent from the global provenance `opensilex-device:device-1` and `opensilex-document:document-1` are used

```json
[
    {
        "uri": "http://opensilex.dev/id/data/1598857852858",
        "date": "2020-08-21T00:00:00+01:00",
        "target": "http://plot01",
        "variable": "http://opensilex.dev/variable#variable.2020-08-21_11-21-23entity6_method6_quality6_unit6",
        "value": "4810",
        "provenance": {
            "uri": "http://opensilex.dev/id/provenance/provenancelabel",
            "experiments": ["opensilex-xp:1"]
        }
    }
]
```

So the final data document is 

```json
[
    {
        "uri": "http://opensilex.dev/id/data/1598857852858",
        "date": "2020-08-21T00:00:00+01:00",
        "target": "http://plot01",
        "variable": "http://opensilex.dev/variable#variable.2020-08-21_11-21-23entity6_method6_quality6_unit6",
        "value": "4810",
        "provenance": {
          "uri": "http://opensilex.dev/id/provenance/provenancelabel",
          "experiments": ["opensilex-xp:1"],
          "prov_was_associated_with": [{"uri": "opensilex-device:device-1"}],
          "prov_used": [{"uri": "opensilex-document:document-1"}]
        }
    }
]
```

In case you define a `prov_was_associated_with`/`prov_used` inside your data, then this one is used

### Device/Variable association

When you insert a data with a device declared as a provenance agent (`provenance.prov_was_associated_with`),
a relationship between the device and the data variable is added inside the device graph (if not already existing).

The relation use the `vocabulary:measures` property from the `Oeso` ontology.

#### Example

If you insert the following document

```json
[
    {
        "uri": "http://opensilex.dev/id/data/1598857852858",
        "date": "2020-08-21T00:00:00+01:00",
        "target": "http://plot01",
        "variable": "http://opensilex.dev/variable#variable.2020-08-21_11-21-23entity6_method6_quality6_unit6",
        "value": "4810",
        "provenance": {
          "uri": "http://opensilex.dev/id/provenance/provenancelabel",
          "experiments": ["opensilex-xp:1"],
          "prov_was_associated_with": [{"uri": "opensilex-device:device-1"}],
          "prov_used": [{"uri": "opensilex-document:document-1"}]
        }
    }
]
```
then the following quad is added inside the triple store (here we suppose the use of the `opensilex` SPARQL prefix)

```sparql
opensilex:set/devices, opensilex-device:device-1, vocabulary:measures, <http://opensilex.dev/variable#variable.2020-08-21_11-21-23entity6_method6_quality6_unit6>)
```

# Validations

The following validations are performed when inserting data as JSON format


## variable, value

- Check that the variable URI exist
- Check that the variable datatype is the same as the value type

## target, provenance.experiments

- if `provenance.experiments` is defined
    - Check that the experiment exists
    - Check that the user has the permissions on the given experiment
    - if `target` is defined :
        - Check that the target is a Scientific object which belongs to the experiment
        - Or check that the target is a Facility
- else if `target` is defined
        - Check that the target is a Scientific object or a Facility

## (provenance
- Check that the provenance URI exists
- Check that any agent declared inside `provenance.wasAssociatedWith` field exists
    - These agent must be an Account or a Device
- Check that any activity declared inside `provenance.provUsed` field exists

# Implementations

- Validation of JSON data is done inside `public List<URI> insertMany(List<DataModel>)` from the `DataLogic` class
- The validation are implemented inside `DataValidation` class

## TODO

- Allow to only retrieve Variable URI and datatype : this optimization minimize the amount of bytes exchanged between OpenSILEX and the RDF4J server

# API returns

| **Fields**                                     | **Validation fail reason**                                             | **HTTP code**     |
|------------------------------------------------|------------------------------------------------------------------------|-------------------|
| `variable`                                     | A variable doesn't exists                                              | 404 (NOT FOUND)   |
| `variable, value`                              | The value type is not the same as the variable datatype                | 400 (BAD REQUEST) |
| `target` (if `provenance.experiments` is null) | The target doesn't exists  (Must be a Scientific Object or a facility) | 404 (NOT FOUND)   |
| `provenance.experiments`                       | An experiment doesn't exists                                           | 404 (NOT FOUND)   |
| `provenance.experiments, target`               | API user don't have permissions for the given experiment               | 403 (FORBIDDEN)   |
| `provenance.experiments, target`               | The target don't belongs to the experiments                            | 404 (NOT FOUND)   |
| `provenance.uri`                               | The "global" provenance doesn't exists                                 | 404 (NOT FOUND)   |
| `provenance.wasAssociatedWith.uri`             | The provenance agent don't exists (Must be a device or an account)     | 404 (NOT FOUND)   |
| `provenance.provUsed.uri`                      | The provenance activity don't exists                                   | 404 (NOT FOUND)   |