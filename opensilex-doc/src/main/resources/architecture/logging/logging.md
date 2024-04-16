---
title: Description of Logging inside OpenSILEX
tags:
    - log
    - structured logging
contact: Renaud COLIN, Arnaud CHARLEROY
---

# Log entries scheme

This section describe which log attributes are written inside OpenSILEX application

## Default log attributes

- These log attributes are written inside each log entry written for a given thread
- This mean that when an HTTP request is accepted by the server, each log write performed by the thread associated with this request, inherits of these fields inside the log entry
- This is implemented inside `AuthentificationFilter.filter(ContainerRequestContext)` method

| **Log key**  | **Description**                                                  |
|--------------|------------------------------------------------------------------|
| `client-id`  | The IP address of the HTTP client which run the HTTP request     |
| `request-id` | An UUID which describe the request                               |
| `host-name`  | Hostname of the OpenSILEX server                                 |
| `user-id`    | The email associated with the account which run the HTTP request |

## Connexion log entries

- These log entries are written during the initial read of an HTTP-request
- This is implemented inside `AuthentificationFilter.filter(ContainerRequestContext)` method
- These log entries are written with the DEBUG level

| **Log key**           | **Description**                                                  | **Written in**                                                                   |
|-----------------------|------------------------------------------------------------------|----------------------------------------------------------------------------------|
| `http_request_uri`    | URI of the incoming HTTP request                                 | as `http_request_uri` field (grouped in the same log with `http_request_method`) |
| `http_request_method` | HTTP method                                                      | as `http_request_method` field (grouped in the same log with `http_request_uri`) |
| `http_request_header` | Hostname of the OpenSILEX server                                 | as `http_request_header`                                                         |
| `http_request_body`   | The email associated with the account which run the HTTP request | as `http_request_body`                                                           |

## Common log attributes

| **Log key**   | **Description**                                                                                        | **Written in**                                |
|---------------|--------------------------------------------------------------------------------------------------------|-----------------------------------------------|
| `log_type`    | The type of operation which is logged                                                                  | Inside `message` field, as `log_type` field   |
| `log_status`  | The operation status                                                                                   | Inside `message` field, as `log_status` field |
| `duration_ms` | The duration of the operation (only available for ended operations) in millisecond                     | as `duration_ms` field                        |
| `collection`  | The name of the MongoDB collection on which operation are done (only available for MongoDB operations) | as `collection` field                         |

- See [Log types](#log-types) for the list of available operations type values associated with the `LOG_TYPE` key in Json log entry
- For each operation, an information of the status is available with the key `log_status`. Possibles values are [`START`, `OK`, `ERROR`]

## Log types

### MongoDB

This section describe log entries which can be produced by operations related to MongoDB

- The first column of the table define the value corresponding with the `log_type` key
- The column `Start properties` define which attributes (key,value) are written during the start of a given operation
- The column `Success properties` define which attributes (key,value) are written during the success of a given operation
- These operation are logged with the INFO level with the following fields : 
  - `log_type`
  - `log_status`
  - `duration_ms` (when finished)
  - `collection`

| **Operation type**              | **Description**                                                  | **Start properties**               | **Success properties**   |
|---------------------------------|------------------------------------------------------------------|------------------------------------|--------------------------|
| `insert_one`                    | Insert one element                                               |                                    | `uri`                    |
| `insert_many`                   | Insert multiple element                                          | `insert_many_count`                | `insert_many_count`      |
| `update_one`                    | Update one element                                               | `uri`                              | `uri`                    |
| `delete_one`                    | Delete one element                                               | `uri`                              | `uri`                    |
| `delete_many`                   | Delete multiple elements                                         | `filter`, `delete_many_count`      | `delete_many_count`      |
| `count`                         | Count elements                                                   | `filter`                           | `filter`, `count_result` |
| `search`                        | Count, evaluation and fetching                                   | `filter`                           | `filter`, `result_count` |
| `search_stream`                 | Count and evaluation                                             | `filter`                           | `filter`, `result_count` |
| `distinct`                      | Compute distinct element (Evaluation and fetching)               | `distinct_field_log_msg`, `filter` | `filter`, `result_count` |
| `aggregate`                     | Compute aggregation (evaluation of aggregation on database side) | `aggregation_pipeline`             |                          |
| `transaction`                   | Run a transaction                                                |                                    |                          |
| `create_index`                  | Create an index                                                  | `index`                            | `index`                  |
| `mongo_server_check_connection` | Check connection with MongoDB server                             | `timeout_ms`                       |                          |

## Additional keys

### MongoDB

This section describe properties which can be logged for MongoDB operation

| **Property**           | Description                                                                                        |
|------------------------|----------------------------------------------------------------------------------------------------|
| `insert_many_count`    | The number of inserted elements                                                                    |
| `delete_many_count`    | The number of deleted elements                                                                     |
| `count_result`         | The number of counted elements                                                                     |
| `result_count`         | The number of element returned by a search/distinct/aggregation                                    |
| `filter`               | A String representation of the filter used for some count, search, distinct or aggregate operation |
| `distinct_field`       | The name of the field asked for distinct operations                                                |
| `aggregation_pipeline` | A String representation of an aggregation Pipeline                                                 |
| `uri`                  | The URI of a single element which is created/updated/deleted                                       |
| `index`                | The index of a collection                                                                          |
| `timeout_ms`           | The timeout in millisecond of an operation                                                         |

# Implementation

## MongoDB

- MongoDB related operation logging attributes and method are implemented inside the `MongoLogger` (`org.opensilex.nosql.mongodb.logging`) class
- The `MongoReadWriteDao` and `MongoDBServiceV2` classes write log entry according the described schema
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
- For all of these operation the collection name is logged (wth `collection` field)
- When finished, the duration  (wth `durationMs` field) of the operation is ms is logged

> Example : Search logging


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
    "log_type": "search",
    "status": "OK",
    "collection": "data",
    "duration_ms": 61,
    "filter": "{}",
    "result_count": 20
  }
]
```
# Documentation

- https://www.innoq.com/en/blog/2019/05/structured-logging/
- https://bearded-developer.com/posts/the-benefits-of-structured-logging/