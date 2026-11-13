# Reviewing an OpenSILEX Vue component

## Contents

- [Steps](#steps)
- [Manual grid](#manual-grid): structure, typing, UI, reuse, behaviour
- [Report format](#report-format)

## Steps

1. **Scope.** The files the user named, otherwise the branch diff against its merge request target:
   `git diff --name-only <target>...HEAD -- '*.vue' '*.ts'`.
2. **Run the mechanical check** on those files:
   ```bash
   python3 .claude/skills/opensilex-vue/scripts/check_vue_conventions.py <files.vue>
   ```
   It reports block order, `<script setup lang="ts">`, `<style scoped lang="scss">`, the `defineEmits` syntax, template
   refs, `any`, Vue 2 leftovers, Bootstrap classes, regions and the place of `defineExpose`. Its `ERROR` lines are certain
   violations. Its `WARN` lines need a look: a `ref<InstanceType<…>>` can be a legitimate data ref, but rarely is.
3. **Review by hand** with the grid below. This is where the real problems are found: the script does not read what the
   code means.
4. **Write the report** (format at the bottom), in the user's language. Report only; fix when asked.

## Manual grid

### Structure

- [ ] Blocks in the order `<template>` → `<script setup lang="ts">` → `<style scoped lang="scss">` ; no empty
  block.
- [ ] `Public` region: `defineEmits`, `defineProps`, `defineModel`, in this order.
- [ ] `Private` region with its sub-regions in order: Plugins & services → Template refs → Data & Computed → Hooks &
  watchers → Event handlers → Methods. A small file may keep only Public / Private.
- [ ] `defineExpose()` last, exposing only what a parent really calls (`git grep` the ref name on the parent side).
- [ ] Handlers named `on<Event>` or `on<Child><Event>`, typed payload, in the `Event handlers` region. An inline handler
  in the template is one clear line; an arrow function is used only to pass a template-scope variable along with the
  payload.
- [ ] A JSDoc comment on every method or variable whose role is not obvious.

### Typing

- [ ] No `any`: no `: any`, no `as any`, no untyped `ref()` whose inference gives `any`, no `Record<string, any>`.
- [ ] DTOs and services come from the generated client (`opensilex-core/index`, `opensilex-security/index`,
  `opensilex-core/api/<x>.service`). UI types come from naive-ui (`DropdownOption`, `DataTableColumns<T>`,
  `SelectOption`, `TreeSelectOption`, `FormRules`, `FormInst`, `DataTableRowKey`).
- [ ] A local interface exists when no type matches the handled shape.
- [ ] A targeted type assertion appears **only at the boundary** of a weakly typed shared component (`any` payload of
  `select`, `searchMethod: Function` of `TableAsyncView`). It does not spread: the rest of the component handles the
  precise type.
- [ ] `defineEmits` in named-tuple syntax: `toto: [payload: string]`.
- [ ] Template refs: `useTemplateRef<InstanceType<typeof Component>>('name')`, or the naive-ui instance type
  (`FormInst`…). No `ref<any>(null)`, no `ref(null)`.

### UI

- [ ] No Bootstrap component left (grid `row`/`col`, `btn`, `card`, `badge`, `table`, `form-group`, `dropdown`, `modal`,
  `nav-tabs`, `data-bs-*`). Each Naive UI equivalent is in `bootstrap-to-naive.md`. Spacing utilities (`mb-2`, `p-3`,
  `d-flex`) are fine.
- [ ] No leftover Bootstrap class on a Naive UI component (`<n-button-group class="btn-group btn-group-sm">`).
- [ ] In the host, every `n-*` used is imported from `naive-ui` (or listed in `create({ components })` of `main.ts` on
  the current branch). An **extension module** does not import naive-ui, so the tag must be in that global list.
  Otherwise the tag stays inert, without any error.

### Reuse

- [ ] Nothing reinvents what `components/common/` provides: `TableAsyncView`, `SearchFiltersSidebar`, `FormSelector`, `InfiniteScrollDropdown` and
  the business selectors, the `*Button`s, `Card`, `Modal`, the `*Form` fields (catalogue in `common-components.md`).
- [ ] No shared component modified without an explicit request. A bug found there is **reported**, not fixed on the
  side.
- [ ] No selector built on `FormSelector` or `InfiniteScrollDropdown` wrapped in an `n-form-item`: it already renders its label (`FormField`) and
  its `n-form-item path`. The duplicate gives two labels and two validation messages.
- [ ] Filters are passed as the slot of `SearchFiltersSidebar`; the component has no second Search or Reset button.
- [ ] The shortcuts of an already migrated component were not copied as is (see "Migrated code is an API source" in
  `SKILL.md`).

### Behaviour

- [ ] API calls go through `$opensilex.getService<…>` and errors through `$opensilex.errorHandler(error)`. No `catch` is
  left empty.
- [ ] Rights are checked with `user.hasCredential(credentials.CREDENTIAL_…_ID)` for every update or delete action.
- [ ] No hard-coded text in the template: every key exists in **en and fr**, in `lang/message-*.json` for shared keys.
- [ ] Watchers and `onMounted` do not run the same search twice on load.
- [ ] No debug `console.log`.

## Report format

Findings sorted from most to least severe, one per line (fictional example):

```
[Bloquant] components/foo/FooList.vue:120  template-ref  ref<any>(null) for the table → useTemplateRef<InstanceType<typeof TableAsyncView>>('tableRef')
[Important] components/foo/FooList.vue:42  bootstrap  <span class="badge"> → <n-tag size="small" round>
[Suggestion] components/foo/FooList.vue:88  dead-code  filters.brand has no control in the sidebar → remove it or add the field
```

- **Bloquant**: breaks behaviour, breaks typing (`any`, untyped template ref), or modifies a shared component without a
  request.
- **Important**: a convention is broken (structure, Bootstrap, emits, regions, missing i18n).
- **Suggestion**: readability, naming, missing JSDoc on a non-obvious method.

End with what was checked and what was not. Does the build pass? Could a type check run? Rendering in the browser is
left to the user.
