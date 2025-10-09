# DataService

The `DataService` class is a core component designed to manage and process experimental data. It supports importing data
from CSV files, validating the data against various criteria, and storing it into a database. The class ensures data
integrity, handles errors gracefully, and provides robust mechanisms for caching and validation.

---

## Table of Contents

1. [Features](#features)
2. [Dependencies](#dependencies)
3. [Key Methods](#key-methods)
4. [Usage](#usage)

---

## Features

- **CSV Data Import**: Supports importing CSV files containing experimental data.
- **Validation**: Validates CSV headers, rows, and data values to ensure they meet the expected format and criteria.
- **Parallel Processing**: Efficiently validates and processes large datasets using multithreading.
- **Error Management**: Captures and handles errors, including invalid data, duplicate entries, and type mismatches.
- **Data Storage**: Integrates with MongoDB and SPARQL services for storing validated data.
- **Caching**: Caches validation results to enhance performance for subsequent operations.
- **Customizable Validation Rules**: Provides methods to validate headers, devices, scientific objects, targets, and
  variables.

---

## Dependencies

The `DataService` class relies on the following services and libraries:

1. **MongoDBService**: For NoSQL database interactions.
2. **SPARQLService**: For RDF and SPARQL-based querying.
3. **FileStorageService**: For file-related operations.
4. **Caffeine Cache**: For caching validation results.
5. **Other Libraries**:
    - `LoggerFactory`: For logging operations.
    - `CsvParser`: For parsing CSV files.
    - `UUID`, `DateFormat`: For generating unique keys and timestamps.

---

## Key Methods

### 1. `importCSVDataV2`

Imports data from a CSV file after validation and stores it in the database.

- **Parameters**:
    - `provenance`: URI of the provenance.
    - `experiment`: URI of the experiment.
    - `file`: Input stream of the CSV file.
    - `validationKey`: Key for cached validation data.

- **Returns**: `DataCSVValidationDTO` with the import status.
- **Throws**: Exception if any error occurs during the import.

---

### 2. `validateWholeCsvV2`

Validates the entire CSV file.

- **Parameters**:
    - `provenance`: URI of the provenance.
    - `experiment`: URI of the experiment.
    - `file`: Input stream of the CSV file.

- **Returns**: `DataCSVValidationModel` with validation results.
- **Throws**: Exception if validation fails.

---

### 3. `validateCSVRowsInParallel`

Processes CSV rows in parallel for validation.

- **Parameters**:
    - `provenance`: Provenance data model.
    - `allRows`: List of CSV rows.
    - Other context and configuration parameters.

- **Throws**: InterruptedException if thread execution fails.

---

### 4. `generateValidationKey`

Generates a unique key for caching validation data.

- **Returns**: A unique string based on user, timestamp, and UUID.

---

### 5. `handleDataInsertion`

Handles insertion of validated data into the database.

- **Parameters**:
    - `dataLogic`: Logic component for data operations.
    - `validation`: Validation model containing data to be inserted.

- **Throws**: Exceptions for duplicate or invalid data.

---

### 6. `validateHeaders`, `validateDeviceColumn`, `validateDateColumn`, `validateScientificObjectColumn`,

`validateTargetColumn`, `validateExperimentColumn`

These methods validate specific aspects of the CSV data (e.g., headers, device, date, etc.).

- **Returns**: Updates the validation context with errors or valid results.

---

## Usage

Here’s how to use the `DataService` class:

### Initialization

```java
MongoDBService mongoDBService = new MongoDBService();
SPARQLService sparqlService = new SPARQLService();
FileStorageService fileStorageService = new FileStorageService();
AccountModel user = new AccountModel();

DataService dataService = new DataService(mongoDBService, sparqlService, fileStorageService, user);
```

### Importing Data

```java
URI provenance = URI.create("http://example.com/provenance");
URI experiment = URI.create("http://example.com/experiment");
InputStream csvFile = new FileInputStream("data.csv");
String validationKey = "unique-validation-key";

DataCSVValidationDTO importResult = dataService.importCSVDataV2(provenance, experiment, file, validationKey);
``` 

### Validating Data

```java          
DataCSVValidationModel validationResult = dataService.validateWholeCsvV2(provenance, experiment, file);
```

--- 
