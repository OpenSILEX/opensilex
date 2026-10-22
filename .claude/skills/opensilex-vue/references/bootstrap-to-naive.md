# Remplacer Bootstrap par Naive UI

Tout composant Bootstrap écrit dans le gabarit, ou recopié de l'ancien composant Vue 2, se remplace par son équivalent Naive UI. Deux exceptions :

- **une brique `common/` existe déjà** (bouton d'action, `Card`, `Modal` de formulaire, `TableAsyncView`) : on l'utilise, même si elle est en Bootstrap à l'intérieur ;
- **les utilitaires d'espacement et d'alignement** (`mb-2`, `me-1`, `p-3`, `d-flex`, `text-muted`…) : ils sont tolérés. Pour une mise en page, préférer quand même `n-space` / `n-flex`.

Rappel : importer chaque composant utilisé, `import { NCard, NGrid, NGridItem } from 'naive-ui'`. Seuls quelques-uns sont globaux (`main.ts`, et la liste varie selon la branche).

| Bootstrap (classes ou `data-bs-*`) | Naive UI | Notes |
|---|---|---|
| `container-fluid` / `container` | rien, ou `n-layout-content` | le conteneur de page est fourni par le layout |
| `row` + `col-*` / `col-lg-6` | `n-grid` + `n-grid-item` | `:cols="2"` ou réactif : `cols="1 m:2"` avec `responsive="screen"` ; `:x-gap`, `:y-gap` |
| alignement en ligne (`d-flex gap-2`) | `n-space` / `n-flex` | `n-space vertical` pour empiler |
| page avec panneau de filtres | `n-layout has-sider` + `SearchFiltersSidebar` + `n-layout-content` | voir `list-page.md` |
| `card`, `card-header`, `card-body` | `n-card` | slots `header`, `header-extra`, défaut, `footer`, `action`. Pour une page de détail, `Card` de `common/` |
| `btn btn-*` | `n-button` | `type="primary" \| "info" \| "success" \| "warning" \| "error"`, `secondary`, `tertiary`, `quaternary`, `ghost`, `size="small"`, `circle`. Pour créer, éditer, supprimer ou voir le détail : `CreateButton`, `EditButton`, `DeleteButton`, `DetailButton` |
| `btn-group`, `btn-group-sm` | `n-button-group size="small"` | **sans** remettre `class="btn-group"` dessus |
| `dropdown`, `dropdown-menu`, `data-bs-toggle="dropdown"` | `n-dropdown` | `:options="DropdownOption[]"`, `@select="(key: string) => …"`, `trigger="click"` |
| `table`, `table-striped` | `n-data-table` | serveur paginé : `TableAsyncView` ; petite table statique : `n-data-table` + `DataTableColumns<T>` ; table de mise en page : `n-table` |
| `pagination` | `n-pagination` | inutile avec `TableAsyncView`, qui l'intègre |
| `form-group` + `label` + `form-control` | `n-form-item` + `n-input` | ou `InputForm` dans un `n-form-item path` sans `label` (voir `common-components.md`) |
| `form-select`, `<select>` | `n-select` | `:options="SelectOption[]"` ; pour une ressource métier, le sélecteur bâti sur `FormSelector` |
| `form-check` (case) | `n-checkbox` | `CheckboxForm` dans un formulaire |
| `form-check` (radio) | `n-radio-group` + `n-radio` / `n-radio-button` | |
| `form-switch` | `n-switch` | plusieurs : `SwitchGroup` |
| `input-group` | `n-input-group` | |
| `badge`, `badge-pill` | `n-tag size="small" round` (étiquette ou compteur seul) ; `n-badge :value` (pastille sur un élément) | |
| `alert alert-*` | `n-alert type="info \| success \| warning \| error"` | |
| `nav nav-tabs`, `nav-link` | `n-tabs` + `n-tab-pane` | `type="line"` ou `"card"`, `v-model:value` |
| `list-group`, `list-group-item` | `n-list` + `n-list-item` | |
| `collapse`, accordéon | `n-collapse` + `n-collapse-item` ; `n-collapse-transition` | |
| `modal` (balisage Bootstrap) | formulaire : `Modal` de `common/` + `useModalFormLogic` ; simple dialogue : `n-modal preset="card"` ou `preset="dialog"` | |
| confirmation | `DeleteButton` (confirme lui-même) ; sinon `n-popconfirm` | |
| `data-bs-toggle="tooltip"`, `title` | `n-tooltip` | slot `trigger` |
| `spinner-border` | `n-spin` | autour du contenu : `<n-spin :show="loading">` |
| `progress` | `n-progress` | |
| `toast` | `$opensilex.show*Toast(...)` | ne pas créer de toast |
| `offcanvas` | `n-drawer` + `n-drawer-content` | |

## Exemple : menu d'actions sur la sélection

Avant (Vue 2 / Bootstrap-Vue) :

```html
<div class="btn-group btn-group-sm">
  <b-dropdown size="sm" :text="$t('component.common.actions')">
    <b-dropdown-item-button @click="clickOnlySelected()">{{ $t('component.common.selected-only') }}</b-dropdown-item-button>
    <b-dropdown-item-button @click="resetSelected()">{{ $t('component.common.resetSelected') }}</b-dropdown-item-button>
  </b-dropdown>
  <span class="badge badge-pill greenThemeColor">{{ selectedCount }}</span>
</div>
```

Après :

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
