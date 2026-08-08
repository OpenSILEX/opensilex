# OpenSILEX OpenAPI 3.1 Migration Plan & Implementation Summary

## 1. Overview & Objectives
The goal of this migration is to update OpenSILEX from legacy Swagger (OpenAPI 2.0 / Swagger 1.5) and Swagger-codegen to **OpenAPI 3.1** using **Swagger v3 annotations (`io.swagger.v3.oas.annotations`)**, **`openapi-generator-maven-plugin` (v7.11.0)**, and Java 21 compatibility.

Key goals achieved:
- Replaced outdated `io.swagger:swagger-jersey2-jaxrs` with `io.swagger.core.v3:swagger-jaxrs2` (v2.2.53).
- Replaced `swagger-codegen-maven-plugin` with `org.openapitools:openapi-generator-maven-plugin`.
- Updated REST controllers and DTOs across all OpenSILEX modules (`opensilex-core`, `opensilex-security`, `opensilex-brapi`, etc.) to OpenAPI 3.1 annotations.
- Fixed Java 21 build issues during `openapi.json` generation and TypeScript client code compilation.

---

## 2. Table of Modified Files & Modification Purpose

| Modified File | Module | Primary Purpose of Modification |
| :--- | :--- | :--- |
| `opensilex-main/.../GeoJsonConverter.java` | `opensilex-main` | **Import Fix**: Added missing `import java.util.Map;` to fix compilation failure on example payload maps. |
| `opensilex-main/.../SwaggerAPIGenerator.java` | `opensilex-main` | **Build Fallback Fix**: Implemented fallback `Reflections` scan using current thread ClassLoader URLs when ServiceLoader hasn't generated `META-INF/services` during the Maven `compile` phase. |
| `opensilex-main/.../RestApplication.java` | `opensilex-main` | **OpenAPI v3 Registration**: Integrated Jersey `OpenApiResource` and `SwaggerConfiguration`, setting up JWT Bearer security scheme definitions. |
| `opensilex-parent/pom.xml` | `opensilex-parent` | **Build System Upgrade**: Updated properties `<swagger.v3.version>` (2.2.53) and `<openapi.generator.version>` (7.11.0), configured `exec-maven-plugin` and `openapi-generator-maven-plugin`. |
| `opensilex-dev-tools/.../ResetTypeScriptLib.java` | `opensilex-dev-tools` | **Dev Tool Fix**: Corrected `SwaggerAPIGenerator.getModuleApi()` invocation to pass project ID string instead of `Class<?>`. |
| `opensilex-dev-tools/pom.xml` | `opensilex-dev-tools` | **Dependency Addition**: Added `org.openapitools:openapi-generator` dependency to resolve missing `org.openapitools.codegen.*` packages. |
| `opensilex-core/pom.xml` | `opensilex-core` | **Plugin Declaration**: Configured `exec-maven-plugin` to generate `openapi.json` and `openapi-generator-maven-plugin` to build `typescript-inversify` client library. |
| `opensilex-security/pom.xml` | `opensilex-security` | **Plugin Declaration**: Configured OpenAPI spec and client generation for security API resources. |
| `opensilex-core/.../api/*API.java` | `opensilex-core` | **Annotation Migration**: Converted `@Api` → `@Tag` and `@ApiOperation` → `@Operation` across core REST endpoints. |
| `opensilex-core/.../api/*DTO.java` | `opensilex-core` | **DTO Annotation Migration**: Converted `@ApiModel` / `@ApiModelProperty` → `@Schema` across data transfer objects. |
| `opensilex-security/.../api/*API.java` & DTOs | `opensilex-security` | **Annotation Migration**: Replaced legacy Swagger annotations with OpenAPI v3 annotations in security controllers and DTOs. |
| `opensilex-brapi/.../api/*API.java` & DTOs | `opensilex-brapi` | **Annotation Migration**: Migrated BrAPI endpoints and DTO definitions to OpenAPI v3 annotations. |

---

## 3. Global Implementation Plan (Phase-by-Phase Roadmap)

### Phase 1: Parent & Build System Configuration
1. **Update POM Properties & Dependencies**:
   - Upgrade Java target version to Java 17/21 compatible properties.
   - Replace Swagger v1.x/2.x properties with `<swagger.v3.version>2.2.53</swagger.v3.version>` and `<openapi.generator.version>7.11.0</openapi.generator.version>`.
   - Remove legacy `io.swagger` code-generator dependencies and introduce `org.openapitools:openapi-generator-maven-plugin`.
2. **Plugin Management Setup**:
   - Configure `exec-maven-plugin` to run `org.opensilex.utils.SwaggerAPIGenerator` during the `compile` phase.
   - Configure `openapi-generator-maven-plugin` using template `typescript-inversify` to generate front-end client SDKs into `front/src/lib/`.

### Phase 2: Core Server & Serialization Infrastructure
1. **REST Application & OpenAPI Resource Integration**:
   - Update `RestApplication.java` to register `io.swagger.v3.jaxrs2.integration.resources.OpenApiResource` and `SwaggerConfiguration`.
   - Configure Bearer JWT security scheme definition via `io.swagger.v3.oas.models.security.SecurityScheme`.
2. **Generator Utility (`SwaggerAPIGenerator.java`)**:
   - Re-implement API generator to produce OpenAPI 3.1 compliant `openapi.json`.
   - Implement custom `ModelConverter` for Jena / RDF models.
   - Add custom schema injectors (e.g. `GeoJsonConverter.injectGeoJsonSchema`).
   - Add classpath-aware fallback `Reflections` mechanism so modules can generate OpenAPI specifications cleanly during headless Maven builds even before ServiceLoader mappings are finalized.

### Phase 3: Module REST Endpoint & DTO Migration
1. **Annotation Mapping**:
   | Legacy Swagger Annotation | OpenAPI 3.1 Annotation | Package / Usage |
   | :--- | :--- | :--- |
   | `@Api(value = "...")` | `@Tag(name = "...")` | Class level controller tagging |
   | `@ApiOperation(value = "...")` | `@Operation(summary = "...")` | Method level endpoint documentation |
   | `@ApiParam(value = "...")` | `@Parameter(description = "...")` | Query / Path / Header parameters |
   | `@ApiModel(value = "...")` | `@Schema(description = "...")` | Class level DTO documentation |
   | `@ApiModelProperty(...)` | `@Schema(...)` | Field level property documentation |
   | `@ApiResponse(...)` | `@ApiResponse(...)` | Response status code and content schema |

2. **Modules Updated**:
   - `opensilex-main`
   - `opensilex-core`
   - `opensilex-security`
   - `opensilex-brapi`
   - `opensilex-dev-tools`

---

## 4. Specific Applied Changes & Code Fixes

### 4.1. `GeoJsonConverter.java` Import Fix
- **File**: `opensilex-main/src/main/java/org/opensilex/server/rest/serialization/GeoJsonConverter.java`
- **Change**: Added missing `import java.util.Map;` to fix compilation error when initializing example GeoJSON payload maps.

### 4.2. Classpath Fallback for `SwaggerAPIGenerator.java`
- **File**: `opensilex-main/src/main/java/org/opensilex/utils/SwaggerAPIGenerator.java`
- **Change**: Enhanced `main()` to initialize a fallback `Reflections` instance scanning `OpenSilex.getClassLoader()` if `instance.getReflections()` yields no classes for a module (due to Maven build lifecycle timing where `serviceloader-maven-plugin` runs in `process-classes` after `compile`).

### 4.3. Dev Tools API & Dependency Fixes
- **File**: `opensilex-dev-tools/src/main/java/org/opensilex/dev/ResetTypeScriptLib.java`
- **Change**: Updated call `SwaggerAPIGenerator.getModuleApi(ClassUtils.getProjectIdFromClass(module.getClass()), ...)` to pass project ID string instead of raw `Class<?>`.
- **File**: `opensilex-dev-tools/pom.xml`
- **Change**: Added `org.openapitools:openapi-generator` dependency (version `${openapi.generator.version}`).

---

## 5. Verification & Validation Commands

To build and verify OpenAPI generation under Java 21:

```bash
# Set Java 21 environment
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk use java 21.0.12-tem

# Clean build opensilex-core (with dependencies)
mvn clean install -pl opensilex-core -am -DskipTests -DskipFrontBuild=true

# Inspect generated openapi.json
cat opensilex-core/front/src/lib/openapi.json | head -n 20

# Compile dev tools
mvn clean compile -pl opensilex-dev-tools -am -DskipTests -DskipFrontBuild=true
```
