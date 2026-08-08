# Migrate from Swagger 2 (io.swagger) to OpenAPI 3.1.1 (io.swagger.v3)

## Background

The OpenSilex project currently uses **Swagger 2** (`io.swagger:swagger-jersey2-jaxrs:1.6.16`) for REST API documentation annotations and **Swagger Codegen 2** (`io.swagger:swagger-codegen-maven-plugin:2.4.10`) for TypeScript client generation. This plan migrates to **OpenAPI 3.1.1** (`io.swagger.v3.oas` v2.2.53 and `openapi-generator-maven-plugin` v7.x) following the reference article and project-specific constraints.

### Impact Summary

| Area | Files Affected | Complexity |
|------|---------------|------------|
| Annotations in API classes | **~215 Java files** across 14 modules | High (bulk, repetitive, migrated 1-by-1) |
| Core infrastructure | **5 critical files** | High (requires rewrite) |
| Maven dependencies (pom.xml) | **3 pom files** | Medium |
| TypeScript generator | Official `openapi-generator-maven-plugin` (v7.x) | Medium |
| Architecture & Docs | `openapi-management.md`, `README.md`, `main.md` | Medium |
| Automated Tests | `OpenApiValidationTest.java` | Medium |

---

## User Review Required

> [!IMPORTANT]
> **User Decisions Applied**:
> 1. **OpenAPI Generator**: Replacing the forked `opensilex-swagger-codegen-maven-plugin` with official `org.openapitools:openapi-generator-maven-plugin` (version 7.x).
> 2. **Maven Plugin Strategy**: Retaining official plugin configured with `<skip>` parameters for modules without API specifications.
> 3. **Rollout Strategy**: **Module-by-module (1-by-1)** migration order:
>    - Step 1: `opensilex-parent` POM dependencies & `opensilex-main` infrastructure
>    - Step 2: `opensilex-security`
>    - Step 3: `opensilex-core`
>    - Step 4: Extension submodules (`opensilex-iado`, `opensilex-front`, `opensilex-faidare`, `opensilex-brapi`, `opensilex-momac`, `opensilex-data-analysis`, `opensilex-sparql`, `opensilex-graphql`, `opensilex-dev-tools`, `opensilex-dataverse`)
> 4. **Architecture Documentation**: Documented in [openapi-management.md](file:///home/charleroy/GIT/Antigravity/opensilex-dev/opensilex-doc/src/main/resources/technical-documentation/architecture/openapi-management.md) and [main.md](file:///home/charleroy/GIT/Antigravity/opensilex-dev/opensilex-doc/src/main/resources/technical-documentation/architecture/main.md).
> 5. **Validation Test Suite**: Adding `OpenApiValidationTest` to validate spec completeness and schema integrity.

---

## Proposed Changes

### Phase 1: Maven Dependencies & Plugin Management

#### [MODIFY] [pom.xml (parent)](file:///home/charleroy/GIT/Antigravity/opensilex-dev/opensilex-parent/pom.xml)

**Replace Swagger 2 dependency with OpenAPI 3.1.1 (swagger-v3 2.2.53):**

```xml
<properties>
    <swagger.v3.version>2.2.53</swagger.v3.version>
    <openapi.generator.version>7.11.0</openapi.generator.version>
</properties>

<dependencies>
    <dependency>
        <groupId>io.swagger.core.v3</groupId>
        <artifactId>swagger-jaxrs2</artifactId>
        <version>${swagger.v3.version}</version>
    </dependency>
    <dependency>
        <groupId>io.swagger.core.v3</groupId>
        <artifactId>swagger-annotations</artifactId>
        <version>${swagger.v3.version}</version>
    </dependency>
</dependencies>
```

**Replace Swagger Codegen 2 plugin with official OpenAPI Generator plugin:**

```xml
<plugin>
    <groupId>org.openapitools</groupId>
    <artifactId>openapi-generator-maven-plugin</artifactId>
    <version>${openapi.generator.version}</version>
    <configuration>
        <skip>${skipTypeScriptLibBuild}</skip>
    </configuration>
    <executions>
        <execution>
            <phase>compile</phase>
            <goals>
                <goal>generate</goal>
            </goals>
            <configuration>
                <inputSpec>${project.basedir}/front/src/lib/openapi.json</inputSpec>
                <generatorName>typescript-inversify</generatorName>
                <output>${project.basedir}/front/src/lib/</output>
                <skipIfSpecIsUnchanged>true</skipIfSpecIsUnchanged>
            </configuration>
        </execution>
    </executions>
</plugin>
```

---

### Phase 2: Core Infrastructure (5 critical files in `opensilex-main`)

#### [MODIFY] [SwaggerAPIGenerator.java](file:///home/charleroy/GIT/Antigravity/opensilex-dev/opensilex-main/src/main/java/org/opensilex/utils/SwaggerAPIGenerator.java)

Rewrite spec generator using OpenAPI 3.1 `io.swagger.v3.oas.models.OpenAPI`.

#### [MODIFY] [RestApplication.java](file:///home/charleroy/GIT/Antigravity/opensilex-dev/opensilex-main/src/main/java/org/opensilex/server/rest/RestApplication.java)

Register OpenAPI 3 `OpenApiResource` and `SecurityScheme` (`Bearer`).

#### [MODIFY] [GeoJsonConverter.java](file:///home/charleroy/GIT/Antigravity/opensilex-dev/opensilex-main/src/main/java/org/opensilex/server/rest/serialization/GeoJsonConverter.java)

Rewrite model converter to populate OpenAPI 3.1 `Schema` components.

#### [MODIFY] [SwaggerExtension.java](file:///home/charleroy/GIT/Antigravity/opensilex-dev/opensilex-main/src/main/java/org/opensilex/SwaggerExtension.java)

Rename/Alias to `OpenApiExtension`.

---

### Phase 3: Module-by-Module Annotation Migration

Using the migration script [migrate_annotations.py](file:///home/charleroy/GIT/Antigravity/opensilex-dev/opensilex-doc/src/main/resources/technical-documentation/swagger-to-openapi/migrate_annotations.py):

#### Migration Sequence (1-by-1):
1. **`opensilex-main`** (8 files: `ErrorDTO`, `MultipleErrorDTO`, etc.)
2. **`opensilex-security`** (34 files: `ApiProtected`, `ProfileAPI`, `UserAPI`, DTOs)
3. **`opensilex-core`** (141 files: all domain APIs & DTOs)
4. **Submodules**:
   - `opensilex-iado` (9 files)
   - `opensilex-front` (6 files)
   - `opensilex-faidare` (6 files)
   - `opensilex-brapi` (4 files)
   - `opensilex-momac` (3 files)
   - `opensilex-data-analysis` (2 files)
   - `opensilex-sparql` (1 file)
   - `opensilex-graphql` (1 file)
   - `opensilex-dev-tools` (1 file)
   - `opensilex-dataverse` (1 file)

---

### Phase 4: Validation Test Suite (`OpenApiValidationTest.java`)

#### [NEW] [OpenApiValidationTest.java](file:///home/charleroy/GIT/Antigravity/opensilex-dev/opensilex-main/src/test/java/org/opensilex/unit/rest/OpenApiValidationTest.java)

Unit test that automates migration verification:

```java
package org.opensilex.unit.rest;

import org.junit.Test;
import static org.junit.Assert.*;
import org.opensilex.unit.test.AbstractUnitTest;
import org.opensilex.utils.SwaggerAPIGenerator;
import io.swagger.v3.oas.models.OpenAPI;
import javax.ws.rs.Path;

public class OpenApiValidationTest extends AbstractUnitTest {

    @Test
    public void testOpenApiSpecGeneration() throws Exception {
        OpenAPI openAPI = SwaggerAPIGenerator.getFullApi(opensilex.getReflections());
        assertNotNull("Generated OpenAPI object should not be null", openAPI);
        assertNotNull("OpenAPI paths should not be null", openAPI.getPaths());
        assertTrue("OpenAPI should contain registered endpoints", openAPI.getPaths().size() > 0);
        assertNotNull("OpenAPI components schemas should not be null", openAPI.getComponents().getSchemas());
    }

    @Test
    public void testAllRestEndpointsHaveOpenApiAnnotations() {
        var pathClasses = opensilex.getReflections().getTypesAnnotatedWith(Path.class);
        for (Class<?> clazz : pathClasses) {
            assertTrue("Class " + clazz.getName() + " should be annotated with @Tag",
                    clazz.isAnnotationPresent(io.swagger.v3.oas.annotations.tags.Tag.class));
        }
    }
}
```

---

### Phase 5: Architecture & Documentation

#### [NEW] [openapi-management.md](file:///home/charleroy/GIT/Antigravity/opensilex-dev/opensilex-doc/src/main/resources/technical-documentation/architecture/openapi-management.md)

Detailed technical architecture documentation on OpenAPI 3.1.1 management.

#### [MODIFY] [architecture/main.md](file:///home/charleroy/GIT/Antigravity/opensilex-dev/opensilex-doc/src/main/resources/technical-documentation/architecture/main.md)

Updated architecture guide sections to reference OpenAPI 3.1.1.

---

## Verification Plan

### Automated Tests
```bash
# 1. Run unit test suite including OpenApiValidationTest
mvn test -Dtest=OpenApiValidationTest

# 2. Build full project module-by-module
mvn clean install -DskipFrontBuild=true -DskipTests

# 3. Build with OpenAPI TypeScript generation
mvn clean install -DskipFrontBuild=false

# 4. Validate generated spec using OpenAPI CLI
npx @openapitools/openapi-generator-cli validate -i opensilex-core/front/src/lib/openapi.json
```

### Manual Verification
- Access `/rest/openapi.json` and `/api-docs` on running server.
- Verify TypeScript client build in Vue.js frontend (`front/`).
