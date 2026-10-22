# Page de liste

Le patron commun des listes migrées (`DeviceList`, `ProjectList`, `OrganizationList`, `DocumentList`, `EventList`) est le suivant. **La vue** possède le bouton Créer et le formulaire modal. **La liste** possède les filtres, la table et les actions de ligne :

```
XxxView.vue
├── CreateButton                      → xxxForm.showCreateForm()
├── XxxList.vue
│   ├── n-layout has-sider
│   │   ├── SearchFiltersSidebar      (v-model:filtersCollapsed, slot = filtres)
│   │   └── n-layout-content
│   │       └── TableAsyncView        (fields, searchMethod, #cell(...))
│   └── émet edit(dto)               → xxxForm.showEditForm(dto)
└── XxxForm.vue (Modal + useModalFormLogic) → @onCreate / @onUpdate → xxxList.refresh()
```

L'exemple ci-dessous part de `ProjectList.vue` / `ProjectsView.vue`, corrigé de leurs écarts. Les API (`ProjectsService.searchProjects`, `deleteProject`), les credentials, la route `/project/details/:uri` et les clés i18n existaient dans le dépôt au 2026-10-09.

## La liste

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

/** Filtres de la liste. Chaque champ a un contrôle dans la sidebar. */
interface ProjectListFilter {
  name: string | undefined
  keyword: string | undefined
}

/** Paramètres passés par TableAsyncView à searchMethod (currentPage commence à 0). */
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
// Avant le montage : la première recherche de TableAsyncView voit déjà les filtres de l'URL.
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

/** Libellés = clés i18n : TableAsyncView les traduit lui-même. */
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

/** Relance la recherche depuis la page 1 et reporte les filtres dans l'URL. */
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

/** DeleteButton a déjà demandé confirmation. */
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

Il n'y a ni `<style>` ni `<i18n>` : toutes les clés sont globales. Si la liste a ses propres libellés, ajouter un bloc `<i18n>` `ProjectList:` en fr et en.

Le slot `#cell(...)` de `TableAsyncView` n'est pas typé (`data.item` est `any` dans le gabarit). C'est la frontière : le gestionnaire appelé (`deleteProject(project: ProjectGetDTO)`) porte le type précis.

## La vue

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

`createTitle` et `editTitle` sont des **clés** : `useModalFormLogic` appelle `t()` dessus. Le gestionnaire `onProjectListEdit(project: ProjectGetDTO)` appelle `projectForm.value?.showEditForm(…)`. Si le formulaire attend un DTO de mise à jour, convertir avec `DTOConverter.extractURIFromResourceProperties(project)`.

## Points d'attention

- **`v-model:filtersCollapsed`, pas `:filtersCollapsed`.** La sidebar utilise `defineModel`. En liaison à sens unique, le parent ne voit jamais l'ouverture du panneau, et son `filtersCollapsed.value = true` après la recherche ne referme rien. Neuf listes ont ce défaut ; `ExperimentScientificObjects.vue` est correct.
- **Filtres** : une interface, une fabrique `getEmptyFilter()` réutilisée par la réinitialisation (pas de littéral recopié), un `activeFiltersCount` calculé. Pas de champ sans contrôle dans la sidebar. N'y mettre que des valeurs qui doivent compter comme filtre actif : un `deprecated: false` par défaut fait afficher « 1 filtre » en permanence (`DocumentList`).
- **`@reset`** remet à vide **et** relance la recherche. Les listes existantes divergent là-dessus ; c'est ce comportement qui est retenu.
- **Filtres texte** : `n-input clearable` + `@keydown.enter.prevent.stop`. `StringFilter` est l'ancienne forme, en Bootstrap, avec un `<form>` imbriqué dans le `n-form` de la sidebar.
- **Sélecteurs dans la sidebar** (`TypeForm`, sélecteurs bâtis sur `FormSelector`) : **sans** `n-form-item` autour. Ils portent leur libellé. Le hack CSS `:deep(.compact-form-item)` de `DeviceList` / `EventList` ne sert qu'à masquer le libellé vide de ce doublon.
- **N'utiliser que l'API exposée** de `TableAsyncView` (`refresh`, `setPage`, `getSelected`, `resetSelection`, `toggleOnlySelected`, `getPaginationInfo`…). `DeviceList` et `EventList` lisent `onlySelected`, `selectAll`, `totalRow`, qui ne sont pas exposés et valent donc `undefined`. Les événements `refreshed` et `selectall` sont déclarés mais **jamais émis**, et la prop `defaultPageSize` n'est pas lue.
- **Compteur** : `TableAsyncView` affiche déjà « n à m sur N » (`showCount`, vrai par défaut). Si la page affiche son propre compteur dans la barre d'actions, passer `:showCount="false"`.
- **Sélection** : `:isSelectable="true"`, `selectedCount = computed(() => tableRef.value?.getSelected().length ?? 0)`. Le menu d'actions sur la sélection est un `n-dropdown` avec des `DropdownOption[]` calculés selon les credentials (exemple dans `bootstrap-to-naive.md`).
- **Export CSV** : `$opensilex.downloadFilefromPostService(path, filename, 'csv', { uris }, lang)` avec `lang = computed(() => store.getters.language)`.
- **Traductions** : passer des **clés** aux composants qui traduisent eux-mêmes (`label` des boutons, `Card`, `StringView`, `TableField.label` de `TableAsyncView`, `createTitle` / `editTitle`, `searchButtonLabelTranslationKey`, `placeholder` de `StringFilter` et `TypeForm`). Passer des **textes traduits** (`t(…)`) aux props `n-*`, à `TableView` et au `placeholder` de `FormSelector`. Le mélange donne des doubles traductions, avec un avertissement de clé manquante dans la console.
- **Modales secondaires** dans une liste (ajout d'événement, de document sur la sélection) : `v-if` paresseux, puis `nextTick(() => ref.value?.show())`, comme `DeviceList` et `EventList`.

## Page de détail et onglets

- Description : `Card` (`#rightHeader` pour `FavoriteButton` / `EditButton` / `DeleteButton`, `#body` pour `StringView` / `UriView` / `DateView` / `MetadataView`). Édition : `form.showEditForm(DTOConverter.extractURIFromResourceProperties(dto))`. Suppression : toast puis `router.push(<liste>)`.
- Onglets : la forme la plus propre existante est `ExperimentView.vue`, avec des routes enfants déclarées dans `opensilex.front.yml` et `<router-view v-slot="{ Component }">`. Les liens d'onglets y sont encore en `router-link class="nav-link"`. Dans du code neuf, utiliser `n-tabs` piloté par la route : `:value` = onglet courant, `@update:value` → `router.push`, et le contenu dans `<router-view>`. Ne pas reproduire les onglets maison de `DeviceDetails.vue` (`<button class="tab">`, `localStorage`, analyse de `route.path`).
