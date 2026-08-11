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

- **Interactive API Documentation (`/api-docs`)**: Serves Swagger UI 5.32.8 via WebJars (`org.webjars:swagger-ui:5.32.8`), pointing to `/rest/openapi.json` with JWT token interceptors.

---

## 5. TypeScript Client Architecture & Hey API (`@hey-api/openapi-ts`)

OpenSILEX utilizes **Hey API** (`@hey-api/openapi-ts`) with `@hey-api/client-fetch` to generate strongly typed, modern TypeScript SDK client libraries directly from OpenAPI 3.1.1 JSON specifications.

### A. Generation Toolchain & Workflow

1. **OpenAPI Specification (`SwaggerAPIGenerator`)**:
   During the Maven `compile` phase, `SwaggerAPIGenerator` scans Java REST endpoint resources and serializes `front/src/lib/openapi.json` for each module.

2. **TypeScript SDK & Type Generation (`@hey-api/openapi-ts`)**:
   `exec-maven-plugin` executes `openapi-ts` (`@hey-api/openapi-ts`) with plugins:
   - Plugins: `--plugins @hey-api/client-fetch --plugins @hey-api/typescript --plugins @hey-api/sdk`
   - Input: `-i ${project.basedir}/front/src/lib/openapi.json`
   - Output: `-o ${project.basedir}/front/src/lib/generated`

3. **Vite Integration (`@hey-api/vite-plugin`)**:
   In `opensilex-front/front/vite.config.ts`, `heyApiPlugin()` accepts a configuration object specifying resolved `input` and `output` paths to automatically regenerate SDK services and TypeScript types whenever Vite starts (`vite serve`) or builds (`vite build`).

4. **Development Tooling (`ResetTypeScriptLib.java`)**:
   `org.opensilex.dev.ResetTypeScriptLib` provides programmatic regeneration by invoking the `openapi-ts` CLI command with `@hey-api/client-fetch`, `@hey-api/typescript`, and `@hey-api/sdk` plugins.

### B. Generated Assets in `front/src/lib/generated/`

Each module produces a client SDK directory in `front/src/lib/generated/` containing:

| File / Artifact | Purpose |
| --- | --- |
| `types.gen.ts` | Strict TypeScript interface definitions for DTO models and endpoint parameters/responses. |
| `sdk.gen.ts` | Type-safe API SDK service functions (e.g. `authenticate()`, `renewToken()`, `getConfig()`, `getUserConfig()`). |
| `client.gen.ts` | Base `@hey-api/client-fetch` HTTP client configuration. |
| `index.ts` | Entry-point barrel file re-exporting types, SDK functions, and client. |

### C. Module Aliases & Clean Imports

Module barrel files (`lib/index.ts` and `src/index.ts`) re-export `./generated`, allowing components to use clean, top-level module imports:

```typescript
// ✅ Clean Module Import via Path Alias
import { authenticate, renewToken } from 'opensilex-security';
import { getVersionInfo, searchCategories } from 'opensilex-core';
```

Aliases are configured in `vite.config.ts` and `tsconfig.json`:

```typescript
// vite.config.ts alias configuration
resolve: {
  alias: {
    'opensilex-security': resolve(__dirname, '../../opensilex-security/front/src/index.ts'),
    'opensilex-security/*': resolve(__dirname, '../../opensilex-security/front/src/*'),
    'opensilex-core': resolve(__dirname, '../../opensilex-core/front/src/index.ts'),
    'opensilex-core/*': resolve(__dirname, '../../opensilex-core/front/src/*')
  }
}
```

```json
// tsconfig.json path mappings
"paths": {
  "opensilex-security": ["../../opensilex-security/front/src/index.ts"],
  "opensilex-security/*": ["../../opensilex-security/front/src/*"],
  "opensilex-core": ["../../opensilex-core/front/src/index.ts"],
  "opensilex-core/*": ["../../opensilex-core/front/src/*"]
}
```

### D. Frontend Integration & Interceptors (`client.ts`)

Hey API uses pluggable request/response interceptors with `@hey-api/client-fetch` in `opensilex-front/front/src/api/client.ts`:

3. **Dynamic Client Registration (`registerOpenSilexClient`)**:
   - **`registerOpenSilexClient(client)`**: Dynamically configures any `@hey-api/client-fetch` instance with `baseUrl: getBaseApi()`, request interceptors (`Authorization: Bearer <token>`, `Accept-Language: fr`, `/rest` URL prefixing), and response interceptors (401 Unauthorized handling, `OpenSilexResponseError` status formatting).
   - **Plugin Integration**: `$opensilex.registerClient(client)` enables dynamically loaded Maven modules (`opensilex-security`, `opensilex-core`, custom modules) via `$opensilex.loadModules(...)` in `main.ts` to register their client instances automatically upon initialization.

4. **Typed Service SDK**:
   All API endpoints are accessible either via typed module SDK imports (`import { authenticate } from 'opensilex-security'`) or through typed helper namespaces on `api` (`api.Security.authenticate(authDTO)`), returning structured results and metadata.

### E. Migration Guide: Legacy Service Locator vs. Modern OpenAPI SDK Client

OpenSILEX is migrating from the legacy InversifyJS service locator pattern (`$opensilex.getService(...)`) to modern, strongly-typed ES module SDK functions generated by `@hey-api/openapi-ts` with `@hey-api/client-fetch`.

#### 1. Legacy Pattern (Inversify Service Locator)

In legacy components, API services were resolved dynamically at runtime using string keys registered in the `$opensilex` plugin service container:

```typescript
// 🔴 Legacy Approach (Inversify Service Locator pattern)
import { ref, inject } from "vue";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import { UriSearchService } from "opensilex-core";

const $opensilex = inject<OpenSilexVuePlugin>("$opensilex");
const uriSearchService = ref<UriSearchService>();

// Dynamic runtime lookup via string key
uriSearchService.value = $opensilex.getService("opensilex.UriSearchService");

// Asynchronous invocation returning Promise<HttpResponse>
uriSearchService.value.getUriTypes()
  .then((response: any) => {
    const result = response.result;
    console.log("URI Types:", result);
  })
  .catch((error: any) => {
    $opensilex.errorHandler(error);
  });
```

#### 2. Modern OpenAPI SDK Pattern (`@hey-api/client-fetch`)

In modern components, API endpoints are exported as direct, tree-shakable TypeScript functions from module packages (`opensilex-core`, `opensilex-security`, etc.). No service locator lookup or manual instantiation is required:

```typescript
// 🟢 Modern OpenAPI SDK Approach (Direct typed SDK functions & async/await)
import { getUriTypes, searchCategories } from "opensilex-core";
import { authenticate } from "opensilex-security";
import { api } from "@/api/client";

// Clean async/await syntax returning { data, error } tuple
try {
  const { data, error } = await getUriTypes();
  if (error || !data) {
    throw error;
  }
  const result = (data as any)?.result ?? data;
  console.log("URI Types:", result);
} catch (error: any) {
  $opensilex.errorHandler(error);
}

// Alternative: using typed helper methods on the central `api` instance
const { data, error } = await api.GET("/vuejs/config");
```

#### 3. Comparison Matrix

| Feature / Aspect | 🔴 Legacy Approach (`$opensilex.getService`) | 🟢 Modern OpenAPI SDK Approach (`@hey-api/client-fetch`) |
| --- | --- | --- |
| **Service Instantiation** | `uriSearchService.value = $opensilex.getService("opensilex.UriSearchService")` | No instantiation needed! Direct ESM import: `import { getUriTypes } from 'opensilex-core'` |
| **Type Safety & IDE Completion** | Dependent on string key mapping (`"opensilex.UriSearchService"`) | Full compile-time TypeScript type safety (`types.gen.ts` & `sdk.gen.ts`) |
| **Invocation Syntax** | Promise `.then()` / `.catch()` callbacks | Modern `async`/`await` returning `{ data, error }` response object |
| **Tree-shaking & Bundle Size** | Class-based service instances bundled in full | Pure ES module functions tree-shaken by Rollup / Vite |
| **Interceptors & Auth** | Manually configured per service instance | Global request/response interceptors in `client.ts` (`Authorization`, debug mode) |

---

## 6. Automated Validation & Testing

To ensure schema correctness and client functionality during builds and refactoring, automated tests check:

1. **Vitest Unit Tests**: Executes frontend client suite (`npm test` / `vitest run`) validating base URL resolution, interceptor execution, and service methods.
2. **Schema Validation**: Parses `openapi.json` with Swagger Parser (`OpenAPIResolver`) to assert 0 syntax/validation errors.
3. **Endpoint Completeness**: Asserts all `@Path` annotated classes are present under `paths`.
4. **DTO Schema Inspection**: Asserts all `@Schema` annotated DTOs compile to valid schema components under `components/schemas`.
