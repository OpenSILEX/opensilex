Vue.js How-to
=============

# How to import a new library and use it

- Run `yarn add <library_name>`. It should add a line with your dependency in the root package.json.
- Globally declare the new components you will need in the main.ts file (front module) as follow :

```typescript
import <component> from '<library_name>'
Vue.use(component)
```