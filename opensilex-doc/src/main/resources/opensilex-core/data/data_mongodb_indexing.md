---
title: MongoDB indexation on data collection
---

<!-- TOC -->
* [Indexes list](#indexes-list)
  * [Simple index](#simple-index)
  * [Access by one field for equality, sort by date](#access-by-one-field-for-equality-sort-by-date)
  * [Access by multiple field for equality, sort by date](#access-by-multiple-field-for-equality-sort-by-date)
  * [Access by multiple field : unique index](#access-by-multiple-field--unique-index)
* [Covered queries](#covered-queries)
  * [target_1_date_-1](#target_1_date_-1)
  * [provenance.experiments_1_date_-1](#provenanceexperiments_1_date_-1)
  * [variable_1_date_-1](#variable_1_date_-1)
  * [provenance.provWasAssociatedWith.uri_1_date_-1](#provenanceprovwasassociatedwithuri_1_date_-1)
  * [provenance.uri_1_date_-1](#provenanceuri_1_date_-1)
<!-- TOC -->

# Indexes list

## Simple index

| **Name**  | **Description** | **Tri**     | **Query example** | **Options** |
|-----------|-----------------|-------------|-------------------|-------------|
| `date_-1` | Access by date  | date (DESC) |                   |             |
| `uri_1`   | Access by URI   | date (DESC) |                   | Unique      |

## Access by one field for equality, sort by date

| **Name**                                         | **Description**             | **Sort**    | **Query example** | **Options** |
|--------------------------------------------------|-----------------------------|-------------|-------------------|-------------|
| `target_1_date_-1`                               | Access by target            | date (DESC) |                   |             |
| `provenance.experiments_1_date_-1`               | Access by experiment        | date (DESC) |                   |             |
| `variable_1_date_-1`                             | Access by variable          | date (DESC) |                   |             |
| `provenance.provWasAssociatedWith.uri_1_date_-1` | Access by agent             | date (DESC) |                   |             |
| `provenance.uri_1_date_-1`                       | Access by global provenance | date (DESC) |                   |             |

## Access by multiple field for equality, sort by date

| **Name**                                                  | **Description**                        | **Sort**    | **Query example** | **Options** |
|-----------------------------------------------------------|----------------------------------------|-------------|-------------------|-------------|
| `provenance.experiments_1_variable_1_target_1_date_-1`    | Access by experiment, variable, target | date (DESC) |                   |             |
| `provenance.provWasAssociatedWith.uri_1_target_1_date_-1` | Access by agent, target                | date (DESC) |                   |             |
| `variable_1_target_1_date_-1`                             | Access by variable, target             | date (DESC) |                   |             |

## Access by multiple field : unique index

| **Name**                                   | **Description** | **Tri**     | **Query example** | **Options** |
|--------------------------------------------|-----------------|-------------|-------------------|-------------|
| `variable_1_provenance_1_target_1_date_-1` | Unicity index   | date (DESC) |                   |             |

# Covered queries

## target_1_date_-1

```javascript
db.data.find({"target": "opensilex:target_1"}).sort({date: -1})
```

## provenance.experiments_1_date_-1

```javascript
db.data.find({"provenance.experiment": "opensilex:experiment_1"}).sort({date: -1})
```

## variable_1_date_-1

```javascript
db.data.find({"variable": "opensilex:variable_1"}).sort({date: -1})
```

## provenance.provWasAssociatedWith.uri_1_date_-1

```javascript
db.data.find({"provenance.provWasAssociatedWith": "opensilex:device_1"}).sort({date: -1})
```

## provenance.uri_1_date_-1

```javascript
db.data.find({"provenance.uri": "opensilex:provenance_1"}).sort({date: -1})
```

## provenance.experiments_1_variable_1_target_1_date_-1

```javascript
db.data.find({"provenance.experiment": "opensilex:experiment_1", "variable": "opensilex:variable_1"}).sort({date: -1})

db.data.find({
  "provenance.experiment": "opensilex:experiment_1",
  "variable": "opensilex:variable_1",
  "target": "opensilex:target_1"
}).sort({date: -1})
```

## provenance.provWasAssociatedWith.uri_1_target_1_date_-1

```javascript
db.data.find({
  "provenance.provWasAssociatedWith": "opensilex:device_1",
  "target": "opensilex:target_1"
}).sort({date: -1})
```

## variable_1_target_1_date_-1

```javascript
db.data.find({
  "variable": "opensilex:variable_1",
  "target": "opensilex:target_1"
}).sort({date: -1})
```

