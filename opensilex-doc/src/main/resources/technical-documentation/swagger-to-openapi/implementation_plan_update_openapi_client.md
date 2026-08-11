# Plan: Replace `openapi-fetch` with `@hey-api/client-fetch` + Typed SDK Services & `OpenSilexResponse<T>`

This document details the complete implementation plan to migrate from `openapi-fetch` to `@hey-api/client-fetch`, configure Vite integration, generate typed SDK service interfaces, enforce formalized `OpenSilexResponse<T>` returns, update technical documentation, and define explicit acceptance criteria and tests.

---

## 🎯 Desired Developer API Interface

Instead of raw URL paths and generic `api.POST("/security/authenticate", ...)` calls:

```typescript
// ❌ Old / Generic Approach
const { data, error } = await api.POST("/security/authenticate", {
  body: { identifier: form.value.email, password: form.value.password }
});

// ✅ New Desired SDK Approach
const response: OpenSilexResponse<TokenDTO> = await api.Security.authenticate({
  identifier: form.value.email,
  password: form.value.password
});

// Access strongly typed result and metadata:
const userToken: TokenDTO = response.result;
const pagination = response.metadata.pagination;
```

---

## 📋 Acceptance Criteria (AC)

> [!IMPORTANT]
> All acceptance criteria must be satisfied for the task to be marked complete.

| ID | Category | Acceptance Criteria |
| :--- | :--- | :--- |
| **AC-1** | **Dependencies** | `openapi-fetch` is completely removed from `package.json`. `@hey-api/client-fetch` and `@hey-api/vite-plugin` are installed and configured. |
| **AC-2** | **Vite & Generation** | Running `vite build` or `npm run gen:types` automatically executes `@hey-api/openapi-ts` and outputs `services.gen.ts`, `types.gen.ts`, and `client.gen.ts` cleanly with zero syntax/type errors. |
| **AC-3** | **SDK Interface** | Frontend components can call typed SDK methods (e.g. `api.Security.authenticate(authDTO)`) with full IDE autocompletion and strict TypeScript types instead of raw strings. |
| **AC-4** | **Response Format** | All API service calls return a formalized `OpenSilexResponse<T>` object containing `result: T` (typed DTO from `openapi-ts`) and `metadata: MetadataDTO`. |
| **AC-5** | **Auth & Interceptors** | Request interceptor automatically attaches `Authorization: Bearer <opensilex_token>` (when token exists) and `Accept-Language: fr`. Response interceptor catches `401 Unauthorized`, clears storage, and dispatches `opensilex:unauthorized` event. |
| **AC-6** | **Documentation** | Technical documentation ([openapi-management.md](file:///home/charleroy/GIT/GITLAB/opensilex-dev/opensilex-doc/src/main/resources/technical-documentation/architecture/openapi-management.md), [main.md](file:///home/charleroy/GIT/GITLAB/opensilex-dev/opensilex-doc/src/main/resources/technical-documentation/architecture/main.md), and [modules.md](file:///home/charleroy/GIT/GITLAB/opensilex-dev/opensilex-doc/src/main/resources/how-to/modules.md)) is updated to reflect `@hey-api/client-fetch`, `@hey-api/sdk`, Vite integration, and typed service interfaces. |
| **AC-7** | **No Regression** | Existing components ([DefaultLoginComponent.vue](file:///home/charleroy/GIT/GITLAB/opensilex-dev/opensilex-front/front/src/components/layout/DefaultLoginComponent.vue), [OpenSilexVuePlugin.ts](file:///home/charleroy/GIT/GITLAB/opensilex-dev/opensilex-front/front/src/models/OpenSilexVuePlugin.ts), [main.ts](file:///home/charleroy/GIT/GITLAB/opensilex-dev/opensilex-front/front/src/main.ts)) compile cleanly without breaking changes. |

---

## 🏗️ Architecture & Generation Pipeline

```
 ┌────────────────────────────────────────────────────────┐
 │ OpenAPI JSON (front/src/lib/openapi.json)              │
 └──────────────────────────┬─────────────────────────────┘
                            │ (@hey-api/openapi-ts + @hey-api/sdk)
                            ▼
 ┌────────────────────────────────────────────────────────┐
 │ Generated SDK Code (front/src/lib/generated/)           │
 │  ├── types.gen.ts     (AuthenticationCreationDTO, etc) │
 │  ├── services.gen.ts  (SecurityService, UserService)    │
 │  └── client.gen.ts    (@hey-api/client-fetch)          │
 └──────────────────────────┬─────────────────────────────┘
                            │
                            ▼
 ┌────────────────────────────────────────────────────────┐
 │ OpenSILEX Client & Services ([client.ts])              │
 │  ├── Interceptors (Auth JWT, Locale, 401 redirect)     │
 │  ├── Response Mapper -> OpenSilexResponse<T>           │
 │  └── Exported Namespaces (api.Security, api.Core, etc) │
 └──────────────────────────┘
```

---

## 🔑 Key Implementation Details

### 1. Hey API Configuration (`openapi-ts.config.ts`)
```typescript
import { defineConfig } from '@hey-api/openapi-ts';

export default defineConfig({
  input: 'opensilex-front/front/src/lib/openapi.json',
  output: 'opensilex-front/front/src/lib/generated',
  plugins: [
    '@hey-api/client-fetch',
    '@hey-api/typescript',
    '@hey-api/sdk'
  ]
});
```

### 2. Client & Service Binding ([client.ts](file:///home/charleroy/GIT/GITLAB/opensilex-dev/opensilex-front/front/src/api/client.ts))
```typescript
import { createClient } from "@hey-api/client-fetch";
import { OpenSilexResponse } from "../models/HttpResponse";
import * as GeneratedServices from "../lib/generated/services.gen";
import type * as DTOs from "../lib/generated/types.gen";

export const client = createClient({
  baseUrl: getBaseApi()
});

// Interceptor: Inject JWT token & locale
client.interceptors.request.use(async (request) => {
  const token = localStorage.getItem("opensilex_token");
  if (token) {
    request.headers.set("Authorization", `Bearer ${token}`);
  }
  if (!request.headers.has("Accept-Language")) {
    request.headers.set("Accept-Language", "fr");
  }
  return request;
});

// Interceptor: Handle 401 & transform response into OpenSilexResponse<T>
client.interceptors.response.use(async (response) => {
  if (response.status === 401) {
    localStorage.removeItem("opensilex_token");
    window.dispatchEvent(new CustomEvent("opensilex:unauthorized"));
  }

  if (!response.ok) {
    let errorPayload: any = {};
    try {
      errorPayload = await response.clone().json();
    } catch {
      errorPayload = { title: response.statusText };
    }
    const message = errorPayload?.message || errorPayload?.result?.message || response.statusText;
    throw new OpenSilexResponseError(message, response.status, errorPayload);
  }

  return response;
});

// Typed API Namespace Export
export const api = {
  Security: {
    authenticate: async (auth: DTOs.AuthenticationCreationDTO): Promise<OpenSilexResponse<DTOs.TokenDTO>> => {
      const res = await GeneratedServices.authenticate({ body: auth });
      return new OpenSilexResponse(res.data.result, res.data.metadata);
    }
  }
};
```

---

## 📁 Proposed Changes

#### [MODIFY] [package.json](file:///home/charleroy/GIT/GITLAB/opensilex-dev/package.json)
- Remove `openapi-fetch`
- Add `@hey-api/client-fetch` to dependencies
- Add `@hey-api/vite-plugin` to devDependencies
- Update `"gen:types"` script to include `@hey-api/sdk`

#### [NEW] [openapi-ts.config.ts](file:///home/charleroy/GIT/GITLAB/opensilex-dev/openapi-ts.config.ts)
- Define Hey API build config with `@hey-api/client-fetch`, `@hey-api/typescript`, and `@hey-api/sdk`.

#### [MODIFY] [opensilex-front/front/vite.config.ts](file:///home/charleroy/GIT/GITLAB/opensilex-dev/opensilex-front/front/vite.config.ts)
- Add `heyApiPlugin()` to Vite plugins array for automatic SDK regeneration during dev & build.

#### [MODIFY] [client.ts](file:///home/charleroy/GIT/GITLAB/opensilex-dev/opensilex-front/front/src/api/client.ts)
- Replace `openapi-fetch` with `@hey-api/client-fetch`.
- Implement `api.Security.authenticate(auth)` and other module service proxies.
- Return `OpenSilexResponse<T>` containing `result: T` and `metadata: MetadataDTO`.

#### [MODIFY] [openapi-management.md](file:///home/charleroy/GIT/GITLAB/opensilex-dev/opensilex-doc/src/main/resources/technical-documentation/architecture/openapi-management.md)
- Update Section 5 to document `@hey-api/client-fetch`, `@hey-api/sdk`, `@hey-api/vite-plugin`, typed service interfaces (`api.Security.authenticate`), and `OpenSilexResponse<T>`.

#### [MODIFY] [main.md](file:///home/charleroy/GIT/GITLAB/opensilex-dev/opensilex-doc/src/main/resources/technical-documentation/architecture/main.md)
- Update architectural diagrams and notes regarding front-end HTTP client generator and Vite integration.

#### [MODIFY] [modules.md](file:///home/charleroy/GIT/GITLAB/opensilex-dev/opensilex-doc/src/main/resources/how-to/modules.md)
- Update how-to guide for creating modules and calling typed SDK services.

#### [NEW] [client.spec.ts](file:///home/charleroy/GIT/GITLAB/opensilex-dev/opensilex-front/front/src/api/client.spec.ts)
- Unit tests for `@hey-api/client-fetch` request/response interceptors and `OpenSilexResponse<T>` mapping.

---

## 🧪 Verification & Test Suite Plan

### Automated Test Suite

1. **Test 1: OpenApi Generation Test (`npm run gen:types`)**
   - Asserts `openapi-ts` runs cleanly with `@hey-api/client-fetch`, `@hey-api/typescript`, and `@hey-api/sdk`.
   - Asserts `services.gen.ts`, `types.gen.ts`, and `client.gen.ts` are generated in `front/src/lib/generated/`.

2. **Test 2: Interceptor & Auth Unit Test (`client.spec.ts`)**
   - **Request Interceptor**: Asserts `Authorization: Bearer <token>` is added when token exists in `localStorage`, and omitted when absent.
   - **Response Interceptor (401)**: Asserts `401 Unauthorized` removes `opensilex_token` and triggers `opensilex:unauthorized` window event.
   - **Response Transformation**: Asserts `{ result: ..., metadata: ... }` response payload is instantiated into an `OpenSilexResponse<T>` object.

3. **Test 3: Typed SDK Invocation Test (`api.Security.authenticate`)**
   - Mocks server login response and calls `api.Security.authenticate({ identifier, password })`.
   - Asserts return type is `OpenSilexResponse<TokenDTO>` and `response.result.token` matches expected value.

4. **Test 4: Documentation Integrity Check**
   - Verifies `openapi-management.md`, `main.md`, and `modules.md` accurately document `@hey-api/client-fetch`, `@hey-api/sdk`, and `@hey-api/vite-plugin`.

5. **Test 5: Vite Build Compilation (`npm run build`)**
   - Runs `npx vite build --config opensilex-front/front/vite.config.ts`.
   - Asserts compilation completes with exit code 0 and zero TypeScript errors.

### Manual Verification
- Test login flow in [DefaultLoginComponent.vue](file:///home/charleroy/GIT/GITLAB/opensilex-dev/opensilex-front/front/src/components/layout/DefaultLoginComponent.vue) in dev mode (`npm run serve` / `vite serve`).
