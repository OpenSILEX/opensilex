# 🧠 Router Behavior & Reactivity (Vue 3 & Vuex)

This document details the specific behavior of the `OpenSilexRouter` instance regarding Vue 3's reactivity system and the Vuex store, as well as best practices to avoid integration issues (especially with Vue DevTools).

---

## ⚠️ The Vuex Reactivity Issue

In the OpenSilex architecture, the `OpenSilexRouter` instance (which contains the Vue router) is stored in the **Vuex Store** (`store.state.openSilexRouter`).

### Default Behavior
By default, Vuex makes any object stored in its `state` **deeply reactive** via Vue Proxies.

When Vue Router (which contains reactive properties like `currentRoute`) is stored in a reactive Vuex object:
1. Vue detects the router's internal `Release` (e.g., `currentRoute`).
2. The reactivity system "unwraps" these Refs.
3. `router.currentRoute` becomes a **plain object** instead of being a `Ref`.

### Consequences
- **Contract Breach:** Vue Router and tools like **Vue DevTools** expect `router.currentRoute` to be a `Ref`.
- **Crashes:** If DevTools tries to access `router.currentRoute.value`, it fails or returns `undefined` because the object is no longer a Ref.
- **Typical Errors:** `TypeError: Cannot read properties of undefined (reading 'matched')`.

---

## ✅ The Solution: `markRaw`

To correct this behavior and ensure the router remains a standard instance compliant with Vue 3 expectations, we use `markRaw`.

### 1. During Instance Creation (`Store.ts`)
We mark the `OpenSilexRouter` instance as "raw" before storing it in Vuex. This prevents Vuex from converting it into a reactive Proxy.

```typescript
// Store.ts
import { markRaw } from 'vue';

// ...
state.openSilexRouter = markRaw(new OpenSilexRouter(args.config.pathPrefix, args.app));
```

### 2. Within the Class (`OpenSilexRouter.ts`)
As an extra precaution, we also mark the internal Vue router instance as `markRaw`.

```typescript
// OpenSilexRouter.ts
constructor(pathPrefix: string, app: App) {
    // ...
    // Mark router as raw to prevent Vue from unwrapping its internal refs
    this.router = markRaw(this.createRouter(User.ANONYMOUS()));
}
```

---

## 📏 Best Practices

### Accessing the Current Route
Since the unwrapping issue is resolved, you can (and should) access the current route in the standard Vue 3 way:

```typescript
// Correct - Standard Vue 3 Access
const currentRoute = router.currentRoute.value;
console.log(currentRoute.path);
```

### Pure Functions for Menu Building
The `OpenSilexRouter` class has been refactored to use pure functions when generating routes, to avoid unexpected side effects during resets.

- `computeMenuRoutes(user)`: Returns a clean `RouteRecordRaw[]` array.
- `buildMenu(items, user)`: Returns a menu tree without modifying the original objects in place.
