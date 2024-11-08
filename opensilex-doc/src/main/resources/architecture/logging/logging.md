---
title: Description of Logging inside OpenSILEX
tags:
    - log
    - structured logging
contact: Renaud COLIN, Arnaud CHARLEROY
---

<!-- TOC -->
* [Log entries scheme](#log-entries-scheme)
  * [Default log attributes](#default-log-attributes)
* [Log types](#log-types)
  * [Common properties](#common-properties)
  * [HTTP request](#http-request)
    * [Log entries](#log-entries)
    * [Properties](#properties)
    * [Implementation](#implementation)
  * [Databases operations](#databases-operations)
    * [Log entries](#log-entries-1)
    * [Properties](#properties-1)
  * [MongoDB](#mongodb)
  * [Log entries](#log-entries-2)
    * [Properties](#properties-2)
    * [Implementation](#implementation-1)
  * [SPARQL](#sparql)
* [OpenSILEX logback configuration](#opensilex-logback-configuration)
* [Example](#example-)
  * [Search logging](#search-logging)
* [Documentation](#documentation)
<!-- TOC -->

# Log entries scheme

This section describe which log attributes are written inside OpenSILEX application

## Default log attributes

- These log attributes are written inside each log entry written for a given thread
- This mean that when an HTTP request is accepted by the server, each log write performed by the thread associated with this request, inherits of these fields inside the log entry
- This is implemented inside `AuthentificationFilter.filter(ContainerRequestContext)` method
  - This is enabled with the call to `org.slf4j.MDC.put(key, value)` 

| **Log key**         | **Description**                                                  | **Example**          |
|---------------------|------------------------------------------------------------------|----------------------|
| `request-id`        | An UUID which describe the request                               |                      |
| `client-id`         | The IP address of the HTTP client which run the HTTP request     |                      |
| `host-name`         | Hostname of the OpenSILEX server                                 |                      |
| `user-id`           | The email associated with the account which run the HTTP request | `user@opensilex.org` |
| `http_request_path` | The API path of the HTTP request                                 | `/core/data`         |
| `http_request_uri`  | The full URI of the incoming HTTP request                        |                      |

- See [Log types](#log-types) for the list of available operations type values associated with the `LOG_TYPE` key in Json log entry

# Log types

## Common properties

This section describe attributes which are common to multiple log entry types

| **Log key**   | **Description**                                                                                        | Values               |
|---------------|--------------------------------------------------------------------------------------------------------|----------------------|
| `log_type`    | The type of operation which is logged                                                                  |                      |
| `log_status`  | The operation status                                                                                   | `[START, OK, ERROR]` |
| `duration_ms` | The duration of the operation (only available for finished operations) in millisecond                  |                      |

## HTTP request

This section describes log operations at HTTP request level

### Log entries

| **Description**                 | **Level** | **log_type**   | Properties                                |
|---------------------------------|-----------|----------------|-------------------------------------------|
| Incoming http request           | `INFO`    | `http_request` | `http_request_uri`, `http_request_method` |
| Incoming http request headers   | `DEBUG`   | `http_headers` | `http_request_header`                     |
| Incoming http request JSON body | `DEBUG`   | `http_body`    | `http_request_body`                       |

### Properties

| **Log key**           | **Description**                           |
|-----------------------|-------------------------------------------|
| `http_request_uri`    | The full URI of the incoming HTTP request |
| `http_request_method` | HTTP method                               |
| `http_request_header` | Hostname of the OpenSILEX server          |
| `http_request_body`   | The JSON body of the request              |

### Implementation

- `AuthentificationFilter.filter(ContainerRequestContext)`

## Databases operations

### Log entries

This section describe log entries which can be produced by operations related to a database operation

- The column `Start properties` define which attributes (key,value) are written during the start of a given operation
- The column `Success properties` define which attributes (key,value) are written during the success of a given operation
- These operation are logged with the INFO level with the following fields : 
  - `log_type`
  - `log_status`
  - `duration_ms` (when finished)

- The following operations are logged :
  - write operations :
    - create (one or multiple elements)
    - update
    - delete (one or many)
  - read operations :
    - count
    - search
    - distinct
    - aggregation

| **log_type**    | **Description**                                    | **Start properties**               | **Success properties**                  | **Level** |
|-----------------|----------------------------------------------------|------------------------------------|-----------------------------------------|-----------|
| `insert_one`    | Insert one element                                 |                                    | `duration_ms`, `uri`                    | `INFO`    |
| `insert_many`   | Insert multiple element                            | `insert_many_count`                | `duration_ms`, `insert_many_count`      | `INFO`    |
| `update_one`    | Update one element                                 | `uri`                              | `duration_ms`, `uri`                    | `INFO`    |
| `delete_one`    | Delete one element                                 | `uri`                              | `duration_ms`, uri`                     | `INFO`    |
| `delete_many`   | Delete multiple elements                           | `filter`, `delete_many_count`      | `duration_ms`, `delete_many_count`      | `INFO`    |
| `count`         | Count elements                                     | `filter`                           | `duration_ms`, `filter`, `count_result` | `INFO`    |
| `search`        | Count, evaluation and fetching                     | `filter`                           | `duration_ms`, `filter`, `result_count` | `INFO`    |
| `search_stream` | Count and evaluation                               | `filter`                           | `duration_ms`, `filter`, `result_count` | `INFO`    |
| `distinct`      | Compute distinct element (Evaluation and fetching) | `distinct_field_log_msg`, `filter` | `duration_ms`, `filter`, `result_count` | `INFO`    |
| `transaction`   | Run a transaction                                  |                                    | `duration_ms`,                          | `INFO`    |

### Properties

| **Property**           | Description                                                                                            |
|------------------------|--------------------------------------------------------------------------------------------------------|
| `insert_many_count`    | The number of inserted elements                                                                        |
| `delete_many_count`    | The number of deleted elements                                                                         |
| `count_result`         | The number of counted elements                                                                         |
| `result_count`         | The number of element returned by a search/distinct/aggregation                                        |
| `filter`               | A String representation of the filter used for some count, search, distinct or aggregate operation     |
| `distinct_field`       | The name of the field asked for distinct operations                                                    |
| `uri`                  | The URI of a single element which is created/updated/deleted                                           |
| `timeout_ms`           | The timeout in millisecond of an operation                                                             |

## MongoDB

The following subsections describes log entries and associated properties which can be produced by a read/write on a MongoDB server

## Log entries

| **Operation type**              | **Description**                                                  | **Start properties**   | **Success properties** | **Level** |
|---------------------------------|------------------------------------------------------------------|------------------------|------------------------|-----------|
| `aggregate`                     | Compute aggregation (evaluation of aggregation on database side) | `aggregation_pipeline` |                        | `INFO`    |
| `create_index`                  | Create an index                                                  | `index`                | `index`                | `INFO`    |
| `mongo_server_check_connection` | Check connection with MongoDB server                             | `timeout_ms`           |                        | `INFO`    |

### Properties

| **Property**           | Description                                                                                            |
|------------------------|--------------------------------------------------------------------------------------------------------|
| `collection`           | The name of the MongoDB collection on which operation are done (only available for MongoDB operations) |                      |
| `aggregation_pipeline` | A String representation of an aggregation Pipeline                                                     |
| `uri`                  | The URI of a single element which is created/updated/deleted                                           |
| `index`                | The index of a collection                                                                              |
| `timeout_ms`           | The timeout in millisecond of an operation                                                             |

> Notes

- The property/value `collection` are logged for each operation (read/write) on a collection

### Implementation

- MongoDB related operation logging attributes and method are implemented inside the `MongoLogger` (`org.opensilex.nosql.mongodb.logging`) class
- The `MongoReadWriteDao` and `MongoDBServiceV2` classes write log entry according the described schema

## SPARQL

The following subsections describes log entries and associated properties which can be produced by a read/write on a SPARQL database

# OpenSILEX logback configuration

# Example 

## Search logging


```json
[
  {
    "@timestamp": "2024-02-23T13:16:42.599+01:00",
    "@version": "1",
    "message": "log_type=search status=START",
    "logger_name": "org.opensilex.nosql.mongodb.dao.MongoReadWriteDao",
    "thread_name": "http-nio-8666-exec-8",
    "level": "INFO",
    "level_value": 20000,
    "host-name": "localhost",
    "client-id": "Direct:127.0.0.1",
    "request-id": "07141f4a-aeb2-4618-b89e-7c031ede4e77",
    "http_request_path" : "https://localhost:8666/rest/core/data/search",
    "http_request_method" : "POST",
    "user-id": "admin@opensilex.org",
    "log_type": "search",
    "status": "START",
    "collection": "data",
    "filter": "{}"
  },
  {
    "@timestamp": "2024-02-23T13:16:42.661+01:00",
    "@version": "1",
    "message": "log_type=search status=OK",
    "logger_name": "org.opensilex.nosql.mongodb.dao.MongoReadWriteDao",
    "thread_name": "http-nio-8666-exec-8",
    "level": "INFO",
    "level_value": 20000,
    "host-name": "localhost",
    "client-id": "Direct:127.0.0.1",
    "request-id": "07141f4a-aeb2-4618-b89e-7c031ede4e77",
    "user-id": "admin@opensilex.org",
    "http_request_path" : "https://localhost:8666/rest/core/data/devices",
    "http_request_method" : "GET",
    "log_type": "search",
    "status": "OK",
    "collection": "data",
    "duration_ms": 61,
    "filter": "{}",
    "result_count": 20
  },
  {
    "@timestamp": "2024-02-23T13:16:42.661+01:00",
    "@version": "1",
    "message": "log_type=insert_many status=OK",
    "logger_name": "org.opensilex.nosql.mongodb.dao.MongoReadWriteDao",
    "thread_name": "http-nio-8666-exec-4",
    "level": "INFO",
    "level_value": 20000,
    "host-name": "localhost",
    "client-id": "Direct:127.0.0.1",
    "request-id": "07141f4a-aeb2-4618-b89e-7c031ede4e77",
    "user-id": "admin@opensilex.org",
    "http_request_path" : "https://localhost:8666/rest/core/data",
    "http_request_method" : "POST",
    "log_type": "insert_many",
    "status": "OK",
    "collection": "data",
    "duration_ms": 1649,
    "filter": "{}",
    "insert_many_count": 10000
  },
  {
    "@timestamp": "2024-02-23T13:16:42.661+01:00",
    "@version": "1",
    "message": "log_type=insert_many status=OK",
    "logger_name": "org.opensilex.nosql.mongodb.dao.MongoReadWriteDao",
    "thread_name": "http-nio-8666-exec-7",
    "level": "INFO",
    "level_value": 20000,
    "host-name": "localhost",
    "client-id": "Direct:127.0.0.1",
    "request-id": "07141f4a-aeb2-4618-b89e-7c031ede4e77",
    "user-id": "admin@opensilex.org",
    "http_request_path" : "https://localhost:8666/rest/core/data",
    "http_request_method" : "POST",
    "log_type": "insert_many",
    "status": "OK",
    "collection": "data",
    "duration_ms": 1423,
    "filter": "{}",
    "insert_many_count": 10000
  }
]
```
# Documentation

- https://www.innoq.com/en/blog/2019/05/structured-logging/
- https://bearded-developer.com/posts/the-benefits-of-structured-logging/