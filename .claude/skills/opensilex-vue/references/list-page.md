# List page

## Contents

- [Structure](#structure): who owns what between the view and the list
- [The list](#the-list): filters, table, row actions
- [The view](#the-view): create button and modal form
- [Points to watch](#points-to-watch)
- [Detail page and tabs](#detail-page-and-tabs)

## Structure

The common pattern of migrated lists (`DeviceList`, `ProjectList`, `OrganizationList`, `DocumentList`, `EventList`) is
the following. **The view** owns the Create button and the modal form. **The list** owns the filters, the table and the
row actions:

```
XxxView.vue
├── CreateButton                      → xxxForm.showCreateForm()
├── XxxList.vue
│   ├── n-layout has-sider
│   │   ├── SearchFiltersSidebar      (v-model:filtersCollapsed, slot = filters)
│   │   └── n-layout-content
│   │       └── TableAsyncView        (fields, searchMethod, #cell(...))
│   └── emits edit(dto)               → xxxForm.showEditForm(dto)
└── XxxForm.vue (Modal + useModalFormLogic) → @onCreate / @onUpdate → xxxList.refresh()
```

The example below starts from `ProjectList.vue` / `ProjectsView.vue`, with their deviations corrected. The APIs
(`ProjectsService.searchProjects`, `deleteProject`), the credentials, the route `/project/details/:uri` and the i18n keys
are those of the repository; check them again if the example no longer builds.

## The list

```vue
<template>
  <n-layout has-sider>
    <SearchFiltersSidebar
      v-model:filtersCollapsed="filtersCollapsed"
      :activeFiltersCount="activeFiltersCount"
      searchButtonLabelTranslationKey="component.project.search-projects"
      @refresh="refresh"
      @reset="resetFilters"
    >
      <n-form-item :label="t('component.common.name')">
        <n-input
          v-model:value="filter.name"
          clearable
          :placeholder="t('component.project.filter-label-placeholder')"
          @keydown.enter.prevent.stop="refresh"
        />
      </n-form-item>
      <n-form-item :label="t('component.common.keywords')">
        <n-input
          v-model:value="filter.keyword"
          clearable
          :placeholder="t('component.project.filter-keywords-placeholder')"
          @keydown.enter.prevent.stop="refresh"
        />
      </n-form-item>
    </SearchFiltersSidebar>

    <n-layout-content>
      <TableAsyncView
        ref="tableRef"
        :fields="fields"
        :searchMethod="searchProjects"
        defaultSortBy="start_date"
        :defaultSortDesc="true"
      >
        <template #cell(name)="{ data }">
          <UriLink
            :uri="data.item.uri"
            :value="data.item.name"
            :to="{ path: '/project/details/' + encodeURIComponent(data.item.uri) }"
          />
        </template>

        <template #cell(actions)="{ data }">
          <n-button-group size="small">
            <EditButton v-if="canModify" label="component.project.update" :small="true" @click="emit('edit', data.item)" />
            <DeleteButton v-if="canDelete" label="component.project.delete" :small="true" @click="deleteProject(data.item)" />
          </n-button-group>
        </template>
      </TableAsyncView>
    </n-layout-content>
  </n-layout>
</template>

<script setup lang="ts">
import { computed, inject, ref, useTemplateRef } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { useStore } from 'vuex'
import { NButtonGroup, NFormItem, NInput, NLayout, NLayoutContent } from 'naive-ui'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import type { OpenSilexStore } from '@/models/Store'
import type HttpResponse from 'opensilex-core/HttpResponse'
import type { OpenSilexResponse } from 'opensilex-core/HttpResponse'
import type { ProjectGetDTO, ProjectsService } from 'opensilex-core/index'
import type { TableField } from '@/components/common/views/TableField'
import SearchFiltersSidebar from '@/components/common/filters/SearchFiltersSidebar.vue'
import TableAsyncView from '@/components/common/views/TableAsyncView.vue'
import UriLink from '@/components/common/views/UriLink.vue'
import EditButton from '@/components/common/buttons/EditButton.vue'
import DeleteButton from '@/components/common/buttons/DeleteButton.vue'

/** List filters. Every field has a control in the sidebar. */
interface ProjectListFilter {
  name: string | undefined
  keyword: string | undefined
}

/** Parameters passed by TableAsyncView to searchMethod (currentPage starts at 0). */
interface TableSearchOptions {
  orderBy: string[]
  currentPage: number
  pageSize: number
}

//#region Public
const emit = defineEmits<{
  edit: [project: ProjectGetDTO]
}>()
//#endregion

//#region Private

//#region Plugins & services
const { t } = useI18n()
const route = useRoute()
const store = useStore() as OpenSilexStore
const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const projectsService = $opensilex.getService<ProjectsService>('opensilex.ProjectsService')
//#endregion

//#region Template refs
const tableRef = useTemplateRef<InstanceType<typeof TableAsyncView>>('tableRef')
//#endregion

//#region Data & Computed
const filter = ref<ProjectListFilter>(getEmptyFilter())
// Before mounting, so that the first TableAsyncView search already sees the URL filters.
$opensilex.updateFiltersFromURL(route.query, filter.value)

const filtersCollapsed = ref(true)

const activeFiltersCount = computed(
  () => Object.values(filter.value).filter((v) => v !== undefined && v !== null && String(v).trim() !== '').length,
)

const canModify = computed(() =>
  store.state.user.hasCredential(store.state.credentials.CREDENTIAL_PROJECT_MODIFICATION_ID),
)
const canDelete = computed(() =>
  store.state.user.hasCredential(store.state.credentials.CREDENTIAL_PROJECT_DELETE_ID),
)

/** Labels are i18n keys: TableAsyncView translates them itself. */
const fields: TableField[] = [
  { key: 'name', label: 'component.common.name', sortable: true },
  { key: 'shortname', label: 'component.project.shortname', sortable: true },
  { key: 'start_date', label: 'component.common.date-time.startDate', sortable: true },
  { key: 'actions', label: 'component.common.actions', resizable: false, naiveProps: { width: 100 } },
]
//#endregion

//#region Methods
function getEmptyFilter(): ProjectListFilter {
  return { name: undefined, keyword: undefined }
}

function searchProjects(options: TableSearchOptions): Promise<HttpResponse<OpenSilexResponse<ProjectGetDTO[]>>> {
  return projectsService.searchProjects(
    filter.value.name,
    undefined,
    filter.value.keyword,
    undefined,
    options.orderBy,
    options.currentPage,
    options.pageSize,
  )
}

/** Runs the search again from page 1 and writes the filters into the URL. */
function refresh() {
  $opensilex.updateURLParameters(filter.value)
  tableRef.value?.setPage(1)
  tableRef.value?.refresh()
  filtersCollapsed.value = true
}

function resetFilters() {
  filter.value = getEmptyFilter()
  refresh()
}

/** DeleteButton has already asked for confirmation. */
async function deleteProject(project: ProjectGetDTO) {
  try {
    await projectsService.deleteProject(project.uri)
    $opensilex.showSuccessToast(
      `${t('component.project.project')} ${project.name} ${t('component.common.success.delete-success-message')}`,
    )
    tableRef.value?.refresh()
  } catch (error) {
    $opensilex.errorHandler(error)
  }
}
//#endregion

//#endregion

defineExpose({
  refresh,
})
</script>
```

There is neither `<style>` nor `<i18n>`: all keys are global. If the list has its own labels, add an `<i18n>` block
`ProjectList:` in en and fr.

The `#cell(...)` slot of `TableAsyncView` is not typed (`data.item` is `any` in the template). That is the boundary: the
called handler (`deleteProject(project: ProjectGetDTO)`) carries the precise type.

## The view

```vue
<template>
  <n-space vertical>
    <n-space justify="end">
      <CreateButton v-if="canModify" label="component.project.add" @click="projectForm?.showCreateForm()" />
    </n-space>

    <ProjectList ref="projectList" @edit="onProjectListEdit" />

    <ProjectForm
      v-if="canModify"
      ref="projectForm"
      createTitle="component.project.add"
      editTitle="component.project.update"
      @onCreate="projectList?.refresh()"
      @onUpdate="projectList?.refresh()"
    />
  </n-space>
</template>
```

`createTitle` and `editTitle` are **keys**: `useModalFormLogic` calls `t()` on them. The handler
`onProjectListEdit(project: ProjectGetDTO)` calls `projectForm.value?.showEditForm(…)`. If the form expects an update
DTO, convert with `DTOConverter.extractURIFromResourceProperties(project)`.

## Points to watch

- **`v-model:filtersCollapsed`, not `:filtersCollapsed`.** The sidebar uses `defineModel`. With a one-way binding, the
  parent never sees the panel open, and its `filtersCollapsed.value = true` after a search closes nothing. Several lists
  have this defect; `ExperimentScientificObjects.vue` is correct.
- **Filters**: an interface, a `getEmptyFilter()` factory reused by the reset (no copied literal), a computed
  `activeFiltersCount`. No field without a control in the sidebar. Put in it only values that should count as an active
  filter: a default `deprecated: false` makes "1 filter" show permanently (`DocumentList`).
- **`@reset`** clears **and** runs the search again. Existing lists disagree on this; this behaviour is the one kept.
- **Text filters**: `n-input clearable` + `@keydown.enter.prevent.stop`. `StringFilter` is the older form, in
  Bootstrap, with a `<form>` nested in the sidebar's `n-form`.
- **Selectors in the sidebar** (`TypeForm`, selectors built on `FormSelector`): **without** an `n-form-item` around
  them. They carry their own label. The CSS hack `:deep(.compact-form-item)` of `DeviceList` / `EventList` only hides the
  empty label of that duplicate.
- **Use only the exposed API** of `TableAsyncView` (`refresh`, `setPage`, `getSelected`, `resetSelection`,
  `toggleOnlySelected`, `getPaginationInfo`…). `DeviceList` and `EventList` read `onlySelected`, `selectAll`,
  `totalRow`, which are not exposed and are therefore `undefined`. The `refreshed` and `selectall` events are declared
  but **never emitted**, and the `defaultPageSize` prop is not read.
- **Counter**: `TableAsyncView` already shows "n to m of N" (`showCount`, true by default). If the page shows its own
  counter in the action bar, pass `:showCount="false"`.
- **Selection**: `:isSelectable="true"`, `selectedCount = computed(() => tableRef.value?.getSelected().length ?? 0)`.
  The action menu on the selection is an `n-dropdown` with `DropdownOption[]` computed from the credentials (example in
  `bootstrap-to-naive.md`).
- **CSV export**: `$opensilex.downloadFilefromPostService(path, filename, 'csv', { uris }, lang)` with
  `lang = computed(() => store.getters.language)`.
- **Translations**: pass **keys** to components that translate by themselves (button `label`, `Card`, `StringView`,
  `TableField.label` of `TableAsyncView`, `createTitle` / `editTitle`, `searchButtonLabelTranslationKey`, the
  `placeholder` of `StringFilter` and `TypeForm`). Pass **translated text** (`t(…)`) to `n-*` props, to `TableView` and
  to the `placeholder` of `FormSelector`. Mixing them gives double translations, with a missing-key warning in the
  console.
- **Secondary modals** in a list (add an event, a document on the selection): lazy `v-if`, then
  `nextTick(() => ref.value?.show())`, as `DeviceList` and `EventList` do.

## Detail page and tabs

- Description: `Card` (`#rightHeader` for `FavoriteButton` / `EditButton` / `DeleteButton`, `#body` for `StringView` /
  `UriView` / `DateView` / `MetadataView`). Edit: `form.showEditForm(DTOConverter.extractURIFromResourceProperties(dto))`.
  Delete: toast, then `router.push(<list>)`.
- Tabs: the cleanest existing form is `ExperimentView.vue`, with child routes declared in `opensilex.front.yml` and
  `<router-view v-slot="{ Component }">`. Its tab links are still `router-link class="nav-link"`. In new code, use
  `n-tabs` driven by the route: `:value` = current tab, `@update:value` → `router.push`, and the content in
  `<router-view>`. Do not reproduce the home-made tabs of `DeviceDetails.vue` (`<button class="tab">`, `localStorage`,
  parsing of `route.path`).
