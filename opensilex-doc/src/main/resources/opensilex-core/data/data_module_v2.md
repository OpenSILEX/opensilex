# Specifications : [Data Module v2]

**Document history (please add a line when you edit the document)**

| Date       | Editor(s)      | Developer(s)   | OpenSILEX version | Comment           |
|------------|----------------|----------------|-------------------|-------------------|
| 27/01/2025 | KOURDI Marouan | KOURDI Marouan | 1.0.0             | Document creation |

>
> Currently covered topics :
>
> - Structure of the DataV2 Module
> - New APIs Implemented
> - Updates to Existing APIs
>
> Missing topics :
>
> - Additional tests and improvements

## Table of contents

<!-- TOC -->

* [Specifications : [Data Module v2]](#specifications--category-title)
    * [Table of contents](#table-of-contents)
    * [Needs](#needs)
    * [Solution](#solution)
        * [Business logic](#business-logic)
    * [Technical specifications](#technical-specifications)
        * [Detailed explanations](#detailed-explanations)
        * [Tests](#tests)
    * [Limitations and improvements](#limitations-and-improvements)
    * [Documentation](#documentation)

<!-- TOC -->

## Needs

The DataV2 module aims to enhance the performance and usability of CSV file processing APIs, introducing new features
and optimizations for validation, insertion, and batch management.

## Solution

The DataV2 module introduces a streamlined flow for CSV file validation, insertion, and batch management.

### Business logic

Validation and insertion processes are optimized to minimize latency, while batch traceability ensures better data
management and accountability.

## Technical specifications

### Detailed explanations

#### Structure of the DataV2 Module

The module is divided into the following sub-packages:

- **API**: Manages entry points for the new APIs.
- **Service**: Contains business logic and interacts with the DAO layer.
- **DTO, Model, and Factory**: Provides tools and structures for processing CSV data.

#### New APIs Implemented

1. **`POST | core/data-v2/import_validation_v2`**
    - Dedicated to CSV validation.
    - **Features and optimizations**:
        - Multithreading improves validation performance.
        - Refactored code enhances maintainability and readability.
        - Validated data is cached for 5 minutes using **Caffeine**, with each object linked to a unique
          `ValidationKey`.

2. **` POST | core/data-v2/import_v2`**
    - Handles validated data insertion.
    - **Features and optimizations**:
        - Avoids revalidation by checking the `ValidationKey` at the API entry.
        - Tracks inserted data with a `batchId` attribute.
        - Inserts data in batches of 1,000 records into **MongoDB**.
        - Creates a `BatchHistory` collection to trace inserted batches.
        - Compresses inserted files in ZIP format and saves them as system documents.

3. **` GET | core/data-v2/batch_history`**
    - Retrieves inserted data batches.
    - **Features**:
        - Retrieve all `batchId` values.
        - Filter results by date range.

4. **` DELETE | core/data-v2/batch_history/{uri}`**
    - Deletes data batches.
    - **Feature**:
        - Deletes a batch using its unique URI.

#### Updates to Existing APIs

1. **`POST | core/data/search`**
    - Added support for searching data by `batchId`.

2. **`DELETE | core/data`**
    - Added support for deleting data based on `batchId`.

### Tests

- `DataServiceTest.java` : Unit tests to verify multithreading in CSV validation.

## Limitations and improvements

- **Known limitation**: Data insertion is limited to a fixed batch size of 1,000 records

## Documentation

See :

- [Data_validation_import.md](./data_validation_import_csv.md)
- [summary_import.png](./summary_import.png)