# ⚙️ Vite Configuration for OpenSilex

The OpenSilex project uses distinct Vite configurations to manage:

- **The main application** (`opensilex-front/front`)
- **Reusable modules** (e.g. `opensilex-core/front`)

These configurations are optimized for development, bundling, internationalization, library packaging, and inclusion into other projects.

---

## 📁 Structure

| File | Main Role |
|-------------------------------------|--------------------------------------------------|
| `opensilex-front/front/vite.config.ts` | Complete Vue application, with components and routing |
| `opensilex-core/front/vite.config.ts`  | Vue module exported as a reusable library |

---

## ⚙️ `opensilex-front/front/vite.config.ts`

### 🔌 Plugins Used

- `@vitejs/plugin-vue`: Vue 3 SFC (Single File Component) support
- `vite-plugin-vue-devtools`: Vue DevTools plugin during development
- `@intlify/unplugin-vue-i18n`: YAML/JSON support for i18n inside `.vue` files
- `@hey-api/vite-plugin`: Automated regeneration of TypeScript API SDK client from `openapi.json`

```ts
plugins: [
  heyApiPlugin({ ... }),
  vue(),
  ...(mode === 'development' ? [vueDevTools()] : []),
  VueI18nPlugin({ defaultSFCLang: "yaml" })
],
```

### 🔗 Aliases & Resolution

```ts
resolve: {
  alias: {
    '~bootstrap': resolve(__dirname, 'node_modules/bootstrap'),
    '@': resolve(__dirname, 'src'),
    'opensilex-security': resolve(__dirname, '../../opensilex-security/front/src/index.ts'),
    'opensilex-core': resolve(__dirname, '../../opensilex-core/front/src/index.ts')
  }
}
```
Allows clean imports of Bootstrap and OpenSilex module components.

### 📦 Build

```ts
build: {
  outDir: 'dist'
}
```

### 🌐 Dev Server

```ts
server: {
  port: 8080
},
base: '/app/'
```
- The dev server starts on port `8080`
- The base path (`/app/`) is used in static resource URLs

---

## 🧩 `opensilex-core/front/vite.config.ts`

### 🧱 Purpose
Compiles a reusable Vue library for use in other modules.

### 🔌 Plugins Used
`plugins: [vue()]`
No i18n or devtools here: lighter configuration targeted for library compilation.

### 📦 Library Build Target

```ts
lib: {
  entry: resolve(__dirname, 'src/lib/index.ts'),
  name: 'opensilex-core',
  fileName: (format) => `opensilex-core.${format}.min.js`,
  formats: ['es', 'umd']
}
```
📦 Generates a library bundle compatible with ES Modules and UMD.

### 🔗 Aliases
```ts
resolve: {
  alias: {
    vue: resolve(__dirname, 'node_modules/vue')
  }
}
```
Ensures `vue` resolves to the local module installation.

### 🧪 Optimization
```ts
optimizeDeps: {
  include: ['vue']
}
```
Pre-bundles Vue to prevent slow reloads and improve dev performance.

---

## ❓ Developer FAQ

### ➕ How to add a new Vite plugin?

1. Install it:
`npm install vite-plugin-myplugin`

2. Declare it in the `plugins` array:
```ts
import myPlugin from 'vite-plugin-myplugin';

plugins: [
  vue(),
  myPlugin()
]
```

### 🧭 Where to define a new path alias?

In the `resolve.alias` section:
```ts
resolve: {
  alias: {
    '@components': resolve(__dirname, 'src/components')
  }
}
```
Then import using the alias:
```ts
import MyComponent from '@components/MyComponent.vue';
```

### 🧪 How to enable library mode in `opensilex-front`?

Uncomment the following lines in `vite.config.ts`:
```ts
lib: {
  entry: resolve(__dirname, 'src/lib/index.ts'),
  name: 'opensilex-front',
  fileName: (format) => `opensilex-front.${format}.js`,
  formats: ['es', 'umd']
}
```
And add an explicit `export` in `src/lib/index.ts`.

### 🌍 How to integrate internationalization (i18n) inside `.vue` files?
```ts
VueI18nPlugin({
  defaultSFCLang: "yaml"
})
```
This enables inline i18n blocks inside SFCs:
```vue
<i18n lang="yaml">
en:
  hello: "Hello"
fr:
  hello: "Bonjour"
</i18n>
```

### 📝 Best Practices
- 🔒 Never include `vue` in library bundles → it must be external.
- ✅ Use base `'`/app/`'` to ensure proper root path resolution.
- 🧹 Keep plugins separated according to context (devtools for the main app, not for submodules).

---

## 🐞 HTTP Client API Debug Mode

When Vite runs in development mode (`import.meta.env.DEV`), the API HTTP client (`@hey-api/client-fetch`) displays detailed debug logs in the browser developer console when the `?debug` query parameter is present in the URL.

### 🔍 API Debug Features

- **Collapsible console groups** (`console.groupCollapsed`) compatible with Firefox, Chrome, Brave, Edge, and Safari.
- **Request Details**: HTTP method, URL, headers (with token sanitization for `Authorization: Bearer`), body payload, and query parameters.
- **Response Details**: HTTP status code, execution duration in milliseconds, and JSON response payload.

### ⚙️ Activation Rules

API Debug mode activates **ONLY in development mode** (`import.meta.env.DEV === true`) when the URL contains `?debug` (e.g., `http://localhost:8080/app/?debug`) or via `localStorage`. In production builds (`import.meta.env.DEV === false`), debug mode is disabled for security reasons.

You can also control it programmatically in JS or from the browser console:

```ts
import { setApiDebug, isDebugApiEnabled } from "opensilex-front/src/api/client";

// Enable API debug mode (in dev environment)
setApiDebug(true);

// Disable API debug mode
setApiDebug(false);

// Reset to default behavior (Vite DEV + URL ?debug)
setApiDebug(null);

// From browser console (dev environment only):
localStorage.setItem("opensilex_debug_api", "true");
```
