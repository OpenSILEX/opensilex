# Replacing Bootstrap with Naive UI

Every Bootstrap component written in the template, or copied from the old Vue 2 component, is replaced with its Naive
UI equivalent. Two exceptions:

- **a `common/` building block already exists** (action button, `Card`, form `Modal`, `TableAsyncView`): use it, even
  if it uses Bootstrap inside;
- **spacing and alignment utilities** (`mb-2`, `me-1`, `p-3`, `d-flex`, `text-muted`…) are fine. For a layout, prefer
  `n-space` / `n-flex` anyway.

Reminder: import every component used, `import { NCard, NGrid, NGridItem } from 'naive-ui'`. Only a few are global
(`main.ts`, and the list differs between branches).

| Bootstrap (classes or `data-bs-*`) | Naive UI | Notes |
|---|---|---|
| `container-fluid` / `container` | nothing, or `n-layout-content` | the layout provides the page container |
| `row` + `col-*` / `col-lg-6` | `n-grid` + `n-grid-item` | `:cols="2"` or responsive: `cols="1 m:2"` with `responsive="screen"`; `:x-gap`, `:y-gap` |
| inline alignment (`d-flex gap-2`) | `n-space` / `n-flex` | `n-space vertical` to stack |
| page with a filter panel | `n-layout has-sider` + `SearchFiltersSidebar` + `n-layout-content` | see `list-page.md` |
| `card`, `card-header`, `card-body` | `n-card` | slots `header`, `header-extra`, default, `footer`, `action`. For a detail page, `Card` from `common/` |
| `btn btn-*` | `n-button` | `type="primary" \| "info" \| "success" \| "warning" \| "error"`, `secondary`, `tertiary`, `quaternary`, `ghost`, `size="small"`, `circle`. To create, edit, delete or show details: `CreateButton`, `EditButton`, `DeleteButton`, `DetailButton` |
| `btn-group`, `btn-group-sm` | `n-button-group size="small"` | **without** putting `class="btn-group"` back on it |
| `dropdown`, `dropdown-menu`, `data-bs-toggle="dropdown"` | `n-dropdown` | `:options="DropdownOption[]"`, `@select="(key: string) => …"`, `trigger="click"` |
| `table`, `table-striped` | `n-data-table` | server-side paginated: `TableAsyncView`; small static table: `n-data-table` + `DataTableColumns<T>`; layout table: `n-table` |
| `pagination` | `n-pagination` | not needed with `TableAsyncView`, which includes it |
| `form-group` + `label` + `form-control` | `n-form-item` + `n-input` | or `InputForm` in an `n-form-item path` without `label` (see `common-components.md`) |
| `form-select`, `<select>` | `n-select` | `:options="SelectOption[]"`; for a business resource, the selector built on `FormSelector` or `InfiniteScrollDropdown` |
| `form-check` (checkbox) | `n-checkbox` | `CheckboxForm` in a form |
| `form-check` (radio) | `n-radio-group` + `n-radio` / `n-radio-button` | |
| `form-switch` | `n-switch` | several: `SwitchGroup` |
| `input-group` | `n-input-group` | |
| `badge`, `badge-pill` | `n-tag size="small" round` (label or bare counter); `n-badge :value` (dot on an element) | |
| `alert alert-*` | `n-alert type="info \| success \| warning \| error"` | |
| `nav nav-tabs`, `nav-link` | `n-tabs` + `n-tab-pane` | `type="line"` or `"card"`, `v-model:value` |
| `list-group`, `list-group-item` | `n-list` + `n-list-item` | |
| `collapse`, accordion | `n-collapse` + `n-collapse-item`; `n-collapse-transition` | |
| `modal` (Bootstrap markup) | form: `Modal` from `common/` + `useModalFormLogic`; simple dialog: `n-modal preset="card"` or `preset="dialog"` | |
| confirmation | `DeleteButton` (confirms by itself); otherwise `n-popconfirm` | |
| `data-bs-toggle="tooltip"`, `title` | `n-tooltip` | `trigger` slot |
| `spinner-border` | `n-spin` | around the content: `<n-spin :show="loading">` |
| `progress` | `n-progress` | |
| `toast` | `$opensilex.show*Toast(...)` | do not create a toast |
| `offcanvas` | `n-drawer` + `n-drawer-content` | |

## Example: action menu on the selection

Before (Vue 2 / Bootstrap-Vue):

```html
<div class="btn-group btn-group-sm">
  <b-dropdown size="sm" :text="$t('component.common.actions')">
    <b-dropdown-item-button @click="clickOnlySelected()">{{ $t('component.common.selected-only') }}</b-dropdown-item-button>
    <b-dropdown-item-button @click="resetSelected()">{{ $t('component.common.resetSelected') }}</b-dropdown-item-button>
  </b-dropdown>
  <span class="badge badge-pill greenThemeColor">{{ selectedCount }}</span>
</div>
```

After:

```vue
<n-space align="center" :size="8">
  <n-dropdown trigger="click" :options="actionOptions" @select="onActionsSelect">
    <n-button size="small">{{ t('component.common.actions') }}</n-button>
  </n-dropdown>
  <n-tag size="small" round class="greenThemeColor">{{ selectedCount }}</n-tag>
</n-space>
```

```ts
import { NSpace, NTag, type DropdownOption } from 'naive-ui'

type SelectionAction = 'selected-only' | 'reset-selection'

const actionOptions = computed<DropdownOption[]>(() => [
  { key: 'selected-only' satisfies SelectionAction, label: t('component.common.selected-only') },
  { key: 'reset-selection' satisfies SelectionAction, label: t('component.common.resetSelected') },
])

function onActionsSelect(key: SelectionAction) {
  if (key === 'selected-only') tableRef.value?.toggleOnlySelected()
  else tableRef.value?.resetSelection()
}
```
