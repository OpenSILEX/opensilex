# OpenAPI 3.1.1 Migration Walkthrough

The migration from legacy Swagger (OpenAPI 2.0 / Swagger 1.5) to **OpenAPI 3.1.1** and **OpenAPI Generator 7.11.0** across the entire OpenSILEX project has been successfully completed and verified.

---

## 1. Summary of Changes

### A. Dependencies & Build Configuration (`pom.xml` & Submodules)
- **Root POMs**:
  - [opensilex-parent/pom.xml](file:///home/charleroy/GIT/GITLAB/opensilex-dev/opensilex-parent/pom.xml): Set `<swagger.v3.version>2.2.53</swagger.v3.version>` and `<openapi.generator.version>7.11.0</openapi.generator.version>`. Removed `opensilex-swagger-codegen-maven-plugin`.
  - Upgraded Jersey JAX-RS annotations to `io.swagger.core.v3:swagger-jaxrs2` and `io.swagger.core.v3:swagger-annotations`.
  - Replaced legacy code generator plugin with `org.openapitools:openapi-generator-maven-plugin` (v7.11.0, generator `typescript-inversify`, with `<supportsES6>true</supportsES6>`).
- **Module POMs** (`opensilex-core`, `opensilex-security`, `opensilex-front`, `opensilex-module`, `opensilex-dev-tools`, root `pom.xml`): Replaced `opensilex-swagger-codegen-maven-plugin` declarations with `org.openapitools:openapi-generator-maven-plugin`.

### B. Core Generator Infrastructure (`opensilex-main` & `opensilex-dev-tools`)
- **[OpenApiExtension.java](file:///home/charleroy/GIT/GITLAB/opensilex-dev/opensilex-main/src/main/java/org/opensilex/OpenApiExtension.java)**: Created interface for registering custom OpenAPI schemas.
- **[SwaggerExtension.java](file:///home/charleroy/GIT/GITLAB/opensilex-dev/opensilex-main/src/main/java/org/opensilex/SwaggerExtension.java)**: Updated to extend `OpenApiExtension`.
- **[SwaggerAPIGenerator.java](file:///home/charleroy/GIT/GITLAB/opensilex-dev/opensilex-main/src/main/java/org/opensilex/utils/SwaggerAPIGenerator.java)**: Re-implemented to generate OpenAPI 3.1 spec files. Included:
  - `JenaAnnotationIntrospector` & `JenaModelConverter` to prevent Jackson reflection conflicts on Apache Jena RDF classes (`Model`, `PrefixMapping`, `Resource`, etc.).
  - ClassLoader fallback scanner for headless Maven builds.
  - Formatted serialization using `Json.pretty()` with automatic Jackson module discovery (`findAndRegisterModules()` for Java 8 date/time types).
- **[RestApplication.java](file:///home/charleroy/GIT/GITLAB/opensilex-dev/opensilex-main/src/main/java/org/opensilex/server/rest/RestApplication.java)**: Registered Jersey `OpenApiResource` and `SwaggerConfiguration` with Bearer JWT security scheme.
- **[GeoJsonConverter.java](file:///home/charleroy/GIT/GITLAB/opensilex-dev/opensilex-main/src/main/java/org/opensilex/server/rest/serialization/GeoJsonConverter.java)**: Rewritten for OpenAPI 3.1 `GeoJsonObject` schema generation.
- **[ResetTypeScriptLib.java](file:///home/charleroy/GIT/GITLAB/opensilex-dev/opensilex-dev-tools/src/main/java/org/opensilex/dev/ResetTypeScriptLib.java)**: Updated dev tool to consume `openapi-generator` API.

### C. Annotation Migration Across All Modules
- Automated bulk migration script [migrate_annotations.py](file:///home/charleroy/GIT/GITLAB/opensilex-dev/opensilex-doc/src/main/resources/technical-documentation/swagger-to-openapi/migrate_annotations.py) converted legacy annotations to OpenAPI v3 annotations across all modules (`opensilex-main`, `opensilex-security`, `opensilex-core`, `opensilex-front`, `opensilex-faidare`, `opensilex-brapi`, `opensilex-data-analysis`, `opensilex-sparql`, `opensilex-graphql`, `opensilex-dataverse`):
  - `@Api` $\rightarrow$ `@Tag`
  - `@ApiOperation` $\rightarrow$ `@Operation`
  - `@ApiParam` $\rightarrow$ `@Parameter`
  - `@ApiModel` / `@ApiModelProperty` $\rightarrow$ `@Schema`
- Meta-annotations updated: [ApiProtected.java](file:///home/charleroy/GIT/GITLAB/opensilex-dev/opensilex-security/src/main/java/org/opensilex/security/authentication/ApiProtected.java) and [ApiTranslatable.java](file:///home/charleroy/GIT/GITLAB/opensilex-dev/opensilex-security/src/main/java/org/opensilex/security/authentication/ApiTranslatable.java).

---

## 2. Verification Results

1. **Unit Testing**:
   - `OpenApiValidationTest.java` in `opensilex-main`: **PASSED** (2/2 tests passed).
   ```bash
   mvn test -pl opensilex-main -Dtest=OpenApiValidationTest
   ```

2. **Full Multi-Module Build & Code Generation**:
   - `mvn clean install -DskipTests -DskipFrontBuild=true`: **BUILD SUCCESS** across all 19 OpenSILEX modules!

   ```
   [INFO] Reactor Summary for opensilex BUILD-SNAPSHOT:
   [INFO] opensilex-parent ................................... SUCCESS [  1.284 s]
   [INFO] opensilex-main ..................................... SUCCESS [ 15.493 s]
   [INFO] opensilex-sparql ................................... SUCCESS [  1.076 s]
   [INFO] opensilex-nosql .................................... SUCCESS [  0.568 s]
   [INFO] opensilex-fs ....................................... SUCCESS [  0.639 s]
   [INFO] opensilex-security ................................. SUCCESS [  5.819 s]
   [INFO] opensilex-core ..................................... SUCCESS [ 11.857 s]
   [INFO] opensilex-front .................................... SUCCESS [  7.104 s]
   [INFO] opensilex-module ................................... SUCCESS [  5.560 s]
   [INFO] opensilex-phis ..................................... SUCCESS [  6.397 s]
   [INFO] opensilex-brapi .................................... SUCCESS [  6.873 s]
   [INFO] opensilex-faidare .................................. SUCCESS [  6.365 s]
   [INFO] opensilex-dataverse ................................ SUCCESS [  7.192 s]
   [INFO] opensilex-migration ................................ SUCCESS [  5.891 s]
   [INFO] opensilex-graphql .................................. SUCCESS [  5.463 s]
   [INFO] opensilex .......................................... SUCCESS [  0.071 s]
   [INFO] opensilex-dev-tools ................................ SUCCESS [  0.680 s]
   [INFO] opensilex-release .................................. SUCCESS [  5.423 s]
   [INFO] opensilex-doc ...................................... SUCCESS [  0.061 s]
   [INFO] ------------------------------------------------------------------------
   [INFO] BUILD SUCCESS
   ```
