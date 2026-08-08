# Technical Architecture: OpenAPI Management in OpenSILEX

**Document history**

| Date | Editor(s) | OpenSILEX version | Comment |
|------|-----------|-------------------|---------|
| 2026-08-05 | OpenSilex Core Team | 1.0.0-SNAPSHOT | Technical architecture document for OpenAPI 3.1.1 management |

## Overview

This document details the architectural design and lifecycle of **OpenAPI 3.1.1** specification management, runtime serving, and TypeScript client generation in OpenSILEX.

---

## High-Level Architecture Diagram

```
+-------------------------------------------------------------------------+
|                              OPENSILEX                                  |
|                                                                         |
|  +---------------------+   +---------------------+   +---------------+  |
|  |   Java API Classes  |   |     DTO Models      |   | GeoJson schema|  |
|  | (@Tag, @Operation)  |   |      (@Schema)      |   |   (Converter) |  |
|  +---------------------+   +---------------------+   +---------------+  |
|             \                         |                         /       |
|              \                        |                        /        |
|               v                       v                       v         |
|  +-------------------------------------------------------------------+  |
|  |                       SwaggerAPIGenerator                         |  |
|  |             (Reflections scanner & OpenAPI Builder)               |  |
|  +-------------------------------------------------------------------+  |
|                                   |                                     |
|                                   v                                     |
|                      [ front/src/lib/openapi.json ]                     |
|                                   |                                     |
|            +----------------------+----------------------+              |
|            |                                             |              |
|            v                                             v              |
|  +-----------------------------------+    +--------------------------+  |
|  | openapi-generator-maven-plugin    |    | Jersey OpenApiResource   |  |
|  | (Generates TypeScript Inversify)  |    | (Serves REST endpoint)   |  |
|  +-----------------------------------+    +--------------------------+  |
|            |                                             |              |
|            v                                             v              |
|   [ Vue.js Web Frontend ]                     [ Swagger UI / External ] |
+-------------------------------------------------------------------------+
```

---

## 1. Specification Generation Lifecycle

OpenSILEX manages OpenAPI specs through two complementary mechanisms:

### A. Build-time Specification Generation (`SwaggerAPIGenerator`)

During Maven `compile` phase, the `exec-maven-plugin` invokes `org.opensilex.utils.SwaggerAPIGenerator`:

1. **Reflection Scanning**: Scans registered modules implementing `APIExtension`.
2. **Annotation Extraction**: Processes `@Tag`, `@Operation`, `@ApiResponse`, `@Schema`, and `@Parameter` annotations.
3. **Extension DTOs**: Queries modules implementing `OpenApiExtension` for supplementary DTO definitions not directly bound to an endpoint parameter.
4. **Spec Serialization**: Outputs `front/src/lib/openapi.json` for each module.

### B. Runtime Specification Endpoint (`RestApplication`)

When the embedded Tomcat server starts:

1. `RestApplication` (extending Jersey `ResourceConfig`) initializes the OpenAPI scanner.
2. Registers `io.swagger.v3.jaxrs2.integration.resources.OpenApiResource`.
3. Configures global `SecurityScheme` (`Bearer` JWT token).
4. Serves the dynamic specification at `GET /rest/openapi.json` and interactive docs at `/api-docs`.

---

## 2. Extension Points

### `APIExtension`

Modules implementing `APIExtension` declare Java package prefixes to be scanned for REST resource classes:

```java
public interface APIExtension {
    List<String> getPackagesToScan();
    List<String> apiPackages();
    void initRestApplication(ResourceConfig resourceConfig);
    void bindServices(AbstractBinder binder);
}
```

### `OpenApiExtension` (formerly `SwaggerExtension`)

Modules can register additional DTO models (e.g. abstract base classes or generic DTOs) into the generated schema components:

```java
public interface OpenApiExtension {
    List<Class<?>> getAdditionalOpenApiDefinitions();
}
```

---

## 3. Security & Meta-Annotations

Authentication and header parameters are managed via meta-annotations:

- **`@ApiProtected`**: Annotated endpoints automatically require the `Bearer` security requirement in OpenAPI output and inject the `Authorization` header parameter.
- **`@ApiTranslatable`**: Injects the `Accept-Language` header parameter into the endpoint specification.

In `RestApplication`, security schemes are declared globally:

```java
SecurityScheme bearerScheme = new SecurityScheme()
        .type(SecurityScheme.Type.HTTP)
        .scheme("bearer")
        .bearerFormat("JWT");
```

---

## 4. Custom Model Converters (`GeoJsonConverter`)

Complex geospatial types (such as `de.grundid.opendatalab:geojson-jackson` objects) cannot be automatically introspected by standard reflection.

OpenSILEX uses custom `ModelConverter` / `OpenApiReader` implementations (e.g., `GeoJsonConverter`) to register custom schema definitions (`GeoJsonObject`) into `OpenAPI.getComponents().getSchemas()`.

---

## 5. TypeScript Client Generation

OpenSILEX uses `org.openapitools:openapi-generator-maven-plugin` (v7.x):

- **Target Language**: `typescript-inversify`
- **Input Spec**: `${project.basedir}/front/src/lib/openapi.json`
- **Output Directory**: `${project.basedir}/front/src/lib/`
- **Configuration Options**:
  - `packageName`: Module ID (e.g., `opensilex-core`)
  - `usePromise`: `true`
  - `modelPropertyNaming`: `original`
  - `<skip>`: Enabled dynamically per module if no API endpoints exist.

---

## 6. Automated Validation & Testing

To ensure schema correctness during builds and refactoring, automated tests check:

1. **Schema Validation**: Parses `openapi.json` with Swagger Parser (`OpenAPIResolver`) to assert 0 syntax/validation errors.
2. **Endpoint Completeness**: Asserts all `@Path` annotated classes are present under `paths`.
3. **DTO Schema Inspection**: Asserts all `@Schema` annotated DTOs compile to valid schema components under `components/schemas`.
