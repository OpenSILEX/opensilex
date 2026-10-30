---
name: opensilex-vue
description: Creates, migrates from Vue 2, modifies and reviews Vue 3 components of the OpenSILEX front so that they follow the team conventions of the technical documentation page vuejs-file-structure.md - block order, script setup with typed defineEmits, defineProps and defineModel, Public and Private regions, typed template refs, event handlers, no any, Naive UI instead of Bootstrap, reuse of the shared common/ components (TableAsyncView, SearchFiltersSidebar, FormSelector, action buttons), en/fr i18n and removal of dead code. Ships a conventions checker. Use whenever a .vue component or its TypeScript is written, ported, fixed or reviewed in opensilex-front or in an extension module (list, filters, form, modal, detail page, selector), even when the word "conventions" is not used. Also trigger on French requests such as "crée un composant", "migre ce composant en Vue 3", "porte cette liste", "relis ce composant", "revue du composant Vue", "respecte les conventions Vue".
---

# OpenSILEX Vue components

This skill sets **how a component is written**. The file structure (regions and their order, event handlers, typing,
JSDoc) comes from the technical documentation page
[`vuejs-file-structure.md`](../../../opensilex-doc/src/main/resources/technical-documentation/opensilex-front/vuejs-file-structure.md).
That page is the reference; this skill adds the rules for UI, i18n, reuse and migration. When the page changes, align
this skill with it.

The host front lives in `opensilex-front/front/src`, the shared building blocks in `components/common/`. The platform
side (the `$opensilex` plugin, routes and menus in `opensilex.front.yml`, extension modules, build) is documented next to
that page (`plugin.md`, `router.md`, `vite.md`) and in `opensilex-module/module-frontend-loading.md`.

Everything below needs only git, Python 3 and the Node toolchain of the repository.

## Pick the mode

| Request | Steps |
|---|---|
| **Create** a component | 1. Look for an existing building block: `references/common-components.md`, then `git grep -l <Name> -- opensilex-front/front/src`. 2. Start from `references/component-skeleton.md`, and from `references/list-page.md` for a list page. 3. Write, then run "Verify". |
| **Migrate** a Vue 2 component | 1. Read the original from a branch that still holds the Vue 2 front (`git show develop:<path>`; check first that `git show develop:package.json` still declares `vue` 2) and list what is actually used: filters, columns, actions, events, props. 2. Drop dead code instead of porting it. 3. Rewrite with the conventions and the Vue 3 building blocks (`references/bootstrap-to-naive.md` for the markup). 4. Verify, then list what was dropped on purpose. |
| **Modify** a component | Write the new code to the conventions. Do not reformat the whole file unasked, so that the diff stays reviewable: report the remaining deviations (checker output) in the summary. During a migration, the whole file must comply. |
| **Review** a component or a branch | Follow `references/review-checklist.md`: checker, manual grid, report sorted by severity. Report only; fix when asked. |

## Conventions

### File layout

- Blocks in this order: `<template>`, `<script setup lang="ts">`, `<style scoped lang="scss">`, `<i18n>`. Leave out a
  block that would be empty.
- In `<script setup>`, after the imports and local types, use these regions with these exact names. The repository
  already has dozens of spellings; a single one keeps IDE folding and the checker's order test working.
  ```ts
  //#region Public
  //   defineEmits → defineProps (withDefaults) → defineModel
  //#endregion

  //#region Private
  //#region Plugins & services     useI18n, useRoute, useStore, inject('$opensilex'), getService
  //#region Template refs
  //#region Data & Computed
  //#region Hooks & watchers
  //#region Event handlers         on<Child><Event>(payload: Type)
  //#region Methods
  //#endregion

  defineExpose({ … })
  ```
  Close every sub-region with its `//#endregion`. A short component can keep only `Public` and `Private`, as long as
  elements of the same kind stay together.
- **`defineEmits`** uses the named-tuple syntax: `defineEmits<{ toto: [payload: string]; closed: [] }>()`. It is the
  shortest form that types the payload; avoid the call-overload form `(e: 'toto', payload: string): void` and the
  untyped `defineEmits(['toto'])`. Emit only declared events, declare only emitted ones.
- **Event handlers**: event `foo` of a child `Child` is handled by `onFoo` or `onChildFoo(payload: Type)` in the
  `Event handlers` region, with a typed payload. A one-line handler may stay inline in the template when it reads
  clearly (`@updated="emit('saved', item)"`). When the handler also needs a variable of the template scope, an arrow
  function calls the method: `@changed="(payload) => onNestedChanged(payload, data)"`.
- **Template refs**: `useTemplateRef<InstanceType<typeof TableAsyncView>>('table')`; for a naive-ui component with an
  instance type, `useTemplateRef<FormInst>('formRef')`. `ref<any>(null)`, `ref(null)` and `useTemplateRef<any>` lose the
  types of the exposed API.
- **Injection**: `const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!`, with this name, this key and the `!`
  (the host always provides the plugin). Services: `$opensilex.getService<XService>('opensilex.XService')`, once, at
  `setup` level.
- A method or variable whose role is not obvious gets a JSDoc comment (`/** … */`).
- Displayed text goes through i18n, in **en and fr**. The `<i18n>` block is YAML, prefixed with the component name. A
  key from another component's local `<i18n>` block does not exist in this one.
- **Key or translated text?** OpenSILEX components that translate by themselves take a **key**: button `label`, `Card`,
  `StringView`, `TableField.label` of `TableAsyncView`, `createTitle` / `editTitle` of `useModalFormLogic`,
  `searchButtonLabelTranslationKey`. `n-*` props, `TableView` and the `placeholder` of `FormSelector` take `t(…)`. Open
  the child component before passing `t(…)` to it: mixing both gives double translations and missing-key warnings.

### Typing: no `any`

The `tsconfig` is lenient (`noImplicitAny: false`, `strictNullChecks: false`) and the build does not check types, so
nothing stops an `any`: the discipline is manual.

- **DTOs and services**: those of the generated client, as `import type` (`opensilex-core/index`,
  `opensilex-security/index`, `opensilex-core/api/<x>.service`, `opensilex-core/HttpResponse`).
- **UI types**: those of naive-ui (`DropdownOption`, `DataTableColumns<T>`, `SelectOption`, `TreeSelectOption`,
  `FormRules`, `FormInst`, `DataTableRowKey`).
- **Store**: `useStore() as OpenSilexStore` (`@/models/Store`).
- **Local interface** when no existing type matches the handled shape: list filters, `searchMethod` options, an
  aggregated row.
- **Targeted assertion** only **at the boundary** of a weakly typed shared component (`searchMethod: Function` of
  `TableAsyncView`, `data.item` of a `#cell` slot, the `any` payload of a `select`). It does not spread: the handler's
  parameter carries the precise type, and the rest of the component only sees that type.

### UI: Naive UI, not Bootstrap

- Replace Bootstrap components (grid `row`/`col`, `btn`, `card`, `badge`, `table`, `form-group`, `dropdown`, `modal`,
  `nav-tabs`, `data-bs-*`) with their Naive UI equivalent: `references/bootstrap-to-naive.md`.
- Leave no Bootstrap class on a naive component (`<n-button-group class="btn-group btn-group-sm">`). Spacing utilities
  (`mb-2`, `p-3`, `d-flex`) are fine.
- **Import every `n-*`** from `naive-ui` in the component (`import { NCard, NTag } from 'naive-ui'`). Only a few are
  registered globally, in `create({ components: […] })` of `opensilex-front/front/src/main.ts`, and the list differs
  between branches: read it on the current branch before calling an `n-*` global. A tag that is neither imported nor
  global renders nothing, without any error.

### Reuse without modifying

- Search `components/common/` before writing anything: server-side table (`TableAsyncView`), filter panel
  (`SearchFiltersSidebar`), selectors (`FormSelector`, `InfiniteScrollDropdown` and the business selectors built on them), action buttons
  (`CreateButton`, `EditButton`, `DeleteButton`, `DetailButton`), `Card`, `Modal` + `useModalFormLogic`, `*Form` fields.
  Catalogue and API: `references/common-components.md`.
- **Do not modify these building blocks** unless asked: migrated lists and views depend on them. Report a defect found
  there instead of fixing it on the side.
- **Open a child component's template before wrapping it.** A selector built on `FormSelector` or `InfiniteScrollDropdown` already renders its label
  and its `n-form-item path`, so it is not wrapped. `*Form` fields (`InputForm`…) go in an `n-form-item path` **without**
  `label`: the field carries the label. Full table in `references/common-components.md`.
- Use only what a building block **exposes** (`defineExpose`) and actually **emits**. Reading non-exposed internal state
  returns `undefined`, without error.

### Dead code

A migration is not a bug-for-bug copy. Remove what the old component carried along:

- unused props, refs, imports, computed values or methods;
- filter fields without a matching UI control;
- props passed to a child that does not declare them, often inherited from the old `opensilex-ModalForm`
  (`initForm`, `successMessage`, `modalSize`);
- commented-out code and `console.log`;
- dead Vue 2 syntax (`.sync`, `this.$refs`, `$listeners`).

Say in the summary what was removed, so that the user can object.

### Migrated code is an API source, not a style model

The existing code is heterogeneous: many migrated components still use overload emits, `ref<any>(null)` or `any`, and
few have the `Public` region. Read a migrated component for its **API** (which props, which events), not for its style.
Shortcuts found in migrated lists, not to be reproduced:

- `:filtersCollapsed="…"` instead of `v-model:filtersCollapsed` on `SearchFiltersSidebar`, so the panel never closes
  after a search;
- `n-form-item` around `TypeForm` or a selector built on `FormSelector` / `InfiniteScrollDropdown`, plus the CSS hack `:deep(.compact-form-item)`
  to hide the empty label;
- reading non-exposed `TableAsyncView` state (`onlySelected`, `selectAll`, `totalRow`) and listening to `@refreshed` /
  `@selectall`, which are never emitted;
- `t(...)` passed to a component that translates by itself (double translation);
- `class="btn-group btn-group-sm"` on `n-button-group`, `badge badge-pill`, `row` / `col`;
- `ref<any>(null)`, `useTemplateRef<any>`, `as any`, `null as any`;
- an `n-*` used without import;
- filters reset by copying the literal again instead of a `getEmptyFilter()` factory.

Check what is correct instead of copying the neighbouring file.

## Verify

1. **Conventions**, on the touched files:
   ```bash
   python3 .claude/skills/opensilex-vue/scripts/check_vue_conventions.py <files.vue>
   ```
   Without arguments it checks the `.vue` files changed against `HEAD` and the untracked ones. No `ERROR` may remain in
   the code written; each `WARN` deserves a look.
2. **Build**, with Node 22, the version pinned in `opensilex-parent/pom.xml` (older versions break the vite/rolldown
   build; check `node --version` first):
   ```bash
   cd opensilex-front/front && npm run build
   ```
   `vue`, `vite` and `naive-ui` are declared in the **root** `package.json`. If the build fails on
   `Cannot find module 'vite'` or `'naive-ui'`, the dependencies are not installed: say so and suggest `npm install` at
   the root rather than running it without agreement.
3. **Types: say it plainly.** `npm run build` (`vite build`) does not check types: it strips `import type`, so a wrong
   type path or an `any` gets through. `vue-tsc` is not a project dependency, and
   `opensilex-core/front/types/opensilex-core.d.ts`, referenced by the `tsconfig`, is generated by the Maven build and
   may be missing. Never claim a full type check; write "build OK, type check (vue-tsc) not available in the project".
4. **Browser**: the user checks the real behaviour (rendering, filters, actions, selection), unless they explicitly ask
   for it to be done here. End by listing what they should look at.

## References

Read only the file the task needs.

- `references/component-skeleton.md`: when creating a component. A complete compliant component on a concrete case, a
  short one, and the modal form pattern.
- `references/list-page.md`: for a list page. Filter panel, server-side table, row actions, the view that owns the form,
  detail page and tabs.
- `references/common-components.md`: before using or wrapping a `common/` building block. API, which component renders
  its own label, global naive-ui components, known defects.
- `references/bootstrap-to-naive.md`: when the markup has Bootstrap classes, usually during a migration.
- `references/review-checklist.md`: for a review. Steps, manual grid, report format.
- `scripts/check_vue_conventions.py`: run it, no need to read it. Mechanical convention check.
