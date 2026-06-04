<template>
  <!-- Boutons manipulation liste  -->
  <!-- <n-space class="listActionButtons" :class="{ 'filtersCollapsed' : (!filtersCollapsed)}"> -->
  <n-space class="listActionButtons" :class="[filtersCollapsed ? 'filtersNotCollapsed' : 'filtersCollapsed']">

    <!-- Premier dropdown (gestion sélection) -->
    <n-dropdown
      :options="selectionOptions"
      trigger="hover"
      @select="handleSelectionAction"
    >
      <n-button size="small" :class="'greenThemeColor'">
        {{ t('VariableList.display') }}
      </n-button>
    </n-dropdown>

    <!-- Deuxième dropdown (gestion actions) -->
    <n-dropdown
      :options="dropdownOptions"
      trigger="hover"
      :disabled="selectedCount === 0"
      @select="handleDropdownAction"
    >
      <n-button
        size="small"
        :disabled="selectedCount === 0"
        :class="selectedCount === 0 ? 'btn-disabled' : 'greenThemeColor'"
      >
        {{ t('component.common.actions') }}
      </n-button>
    </n-dropdown> 


    <!-- Affichage Counts  -->
    <div class="displayAndListSelectionCount">
      <div v-if="showCount">
        <div v-if="hasResults">
          <strong>
            <span class="ml-1">
              {{ t('component.common.list.pagination.nbEntries', {
                limit: start,
                offset: end,
                totalRow: n(total)
              }) }}
            </span>
          </strong>
        </div>
        <div v-else>
          <strong>
            <span class="ml-1">{{ t('component.common.list.pagination.noEntries') }}</span>
          </strong>
        </div>
      </div>
      <span> | </span>
      <n-p>
        {{ t('VariableList.selected') }} :
        <span class="badge badge-pill greenThemeColor">{{ selectedCount }}</span>
      </n-p>
    </div>
    <!-- FIN affichage counts -->
  </n-space>
  <!-- FIN Boutons manipulation liste  -->


<n-layout has-sider class="vars-layout">
  <n-space class="mb-2 me-1" align="top">
    <n-button 
      quaternary 
      circle 
      @click="filtersCollapsed = !filtersCollapsed" 
      :title="t('VariableList.label-filter')"
      :class="{ 'greenThemeColor' : (filtersCollapsed)}"
      class="globalFiltersSearchButton"
    >
      <i class="bi bi-search filtersGlobalSearchIcon"></i>

      <div v-show="filtersCollapsed && activeFilters > 0" class="filters-count-badge"
        >
        ( {{ activeFilters }} )
      </div>

    </n-button>
  </n-space>
  
  <!-- SIDEBAR / FILTRES -->
  <n-layout-sider
    v-model:collapsed="filtersCollapsed"
    :collapsed-width="0"
    :width="360"
    collapse-mode="width"
    show-trigger
    bordered
    class="vars-sider"
  >

    <n-space class="p-3" vertical>

      <n-form label-placement="top" size="small" @submit.prevent="applyFilters">
        <!-- Nom -->
        <n-form-item :label="t('component.common.name')">
          <n-input
            v-model:value="filter.name"
            :placeholder="t('VariableList.name-placeholder')"
            @keydown.enter.prevent.stop="applyFilters"
            clearable
          />
        </n-form-item>

        <!-- Entité -->
        <n-form-item :label="t('component.variable.entity.entity')" :show-feedback="false">
          <opensilex-EntitySelector
            v-model:selected="filter.entity"
            @handlingEnterKey="applyFilters"
            :placeholder="$t('component.variable.entity.entity-placeholder')"
          />
        </n-form-item>
        <br>

        <!-- Caractéristique -->
        <n-form-item :label="t('component.variable.characteristic.characteristic')" :show-feedback="false">
          <opensilex-CharacteristicSelector
            v-model:selected="filter.characteristic"
            @handlingEnterKey="applyFilters"
            :placeholder="$t('component.variable.characteristic.characteristic-placeholder')"
          />
        </n-form-item>

        <!-- Group of variables -->
        <n-form-item :label="t('component.variable.groupVariable.groupVariable')" :show-feedback="false">
          <opensilex-GroupVariablesSelector
            v-if="!withoutGroup"
            v-model:variableGroup="filter.includedGroup"
            :sharedResourceInstance="filter.sharedResourceInstance"
            class="searchFilter"
            @handlingEnterKey="applyFilters"
          />
          <opensilex-GroupVariablesSelector
            v-else
            v-model:variableGroup="filter.notIncludedGroup"
            :sharedResourceInstance="filter.sharedResourceInstance"
            class="searchFilter"
            @handlingEnterKey="applyFilters"
          />
        </n-form-item>

        <!-- <n-form-item :show-feedback="false"> -->
          <opensilex-CheckboxForm
            :title="t('VariableList.withoutGroup')"
            :helpMessage="t('VariableList.withoutGroup-info')"
            v-model:value="withoutGroup"
            class="searchFilter"
          />
        <!-- </n-form-item> -->

        <!-- Advanced -->
        <n-collapse
          v-model:expanded-names="expandedNames"
          :accordion="false"
          @update:expanded-names="onCollapseUpdate"
          class="advancedFiltersSearch"
        >
          <n-collapse-item :title="t('component.common.advanced-search-title')" name="adv">
            <n-form-item :label="t('component.variable.entityOfInterest.entityOfInterest')" :show-feedback="false">
              <opensilex-InterestEntitySelector
                v-model:selected="filter.entityOfInterest"
                @handlingEnterKey="applyFilters"
                :placeholder="$t('component.variable.entityOfInterest.entityOfInterest-placeholder')"
              />
            </n-form-item>

            <n-form-item :label="t('component.variable.method.method')" :show-feedback="false">
              <opensilex-MethodSelector
                v-model:selected="filter.method"
                @handlingEnterKey="applyFilters"
                :placeholder="$t('component.variable.method.method-placeholder')"
              />
            </n-form-item>

            <n-form-item :label="t('component.variable.unit.unit')" :show-feedback="false">
              <opensilex-UnitSelector
                v-model:selected="filter.unit"
                @handlingEnterKey="applyFilters"
                :placeholder="$t('component.variable.unit.unit-placeholder')"
              />
            </n-form-item>

            <n-form-item :label="t('component.variable.dataType.data-type')" :show-feedback="false">
              <opensilex-VariableDataTypeSelector
                v-model:selected="filter.dataType"
                @handlingEnterKey="applyFilters"
                :placeholder="$t('component.variable.dataType.datatype-placeholder')"
              />
            </n-form-item>

            <n-form-item :label="t('component.variable.timeInterval.time-interval')" :show-feedback="false">
              <opensilex-VariableTimeIntervalSelector
                v-model:selected="filter.timeInterval"
                @handlingEnterKey="applyFilters"
                :placeholder="$t('component.variable.timeInterval.time-interval-placeholder')"
              />
            </n-form-item>

            <n-form-item :label="t('component.variable.species.species')">
              <opensilex-SpeciesSelector
                v-model:selected="filter.species"
                :multiple="true"
                :placeholder="$t('component.variable.species.select-multiple-placeholder')"
              />
            </n-form-item>
          </n-collapse-item>
        </n-collapse>

        <n-space justify="end" class="mt-2">
          <!-- Boutons Filtres Recherche  -->
          <opensilex-Button
          class="resetButton"
            :label="t('component.common.search.clear-button')"
            icon="bi-x-lg"
            @click="resetFilters"
          />
          <opensilex-Button 
            class="greenThemeColor"
            :label="t('component.common.search.search-button')"
            icon="bi-search"
            @click="applyFilters"
          >
          </opensilex-Button>
        </n-space>
      </n-form>
    </n-space>
  </n-layout-sider>

  <!-- CONTENU : barre d’actions + table -->
  <n-layout-content class="vars-content">

  <n-data-table
    :remote="!onlySelected"                
    :columns="columns"
    :data="tableData"
    :row-key="rowKey"
    :pagination="pagination"
    :expanded-row-keys="expandedRowKeys"
    v-model:checked-row-keys="checkedRowKeys"
    @update:expanded-row-keys="handleExpandedRowChange"
    @update:page="onPageChange"
    @update:page-size="onPageSizeChange"
    v-model:sorter="sorterState"       
    @update:sorter="onSortChange"        
  />
    </n-layout-content>
</n-layout>

  <opensilex-GroupVariablesModalList
    ref="groupVariableSelection"
    :required="true"
    :multiple="true"
    @onValidate="editGroupVariable"
    @onValidateEmpty="$opensilex?.showWarningToast($t('component.group.no-group-selected'))"
  />

  <opensilex-ModalForm
    ref="groupVariablesForm"
    :component="formComponent"
    :createTitle="'component.variable.groupVariable.add-groupVariable'"
    :editTitle="'component.variable.groupVariable.edit'"
    :create-action="create"
    :update-action="update"
    :success-message="successMessage"
    v-if="loadGroupVariablesForm"
  />
</template>

<script lang="ts" setup>
import { ref, h, inject, reactive, onMounted, resolveComponent, computed, watch, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import { NButton, NTag, NDataTable, DataTableRowKey, NInput, NForm, NFormItem, NSelect, NCheckbox, NCollapse, NCollapseItem, NLayout, NLayoutSider, NLayoutContent, NSpace } from 'naive-ui'
import { VariablesService } from 'opensilex-core'
import { VariableGetDTO } from 'opensilex-core/model/variableGetDTO'
import OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import ModalForm from '@/components/common/forms/ModalForm.vue'
import GroupVariablesForm from '../groupVariable/GroupVariablesForm.vue'

/** Refs UI */
const groupVariableSelection = ref()
const groupVariablesForm = ref<InstanceType<typeof ModalForm>>()
const formComponent = groupVariablesForm

/** Services & i18n */
const { t, n } = useI18n()
const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')
const $service = ref<VariablesService | null>(null)

/** components */
const EditButton = resolveComponent('opensilex-EditButton')
const DetailButton = resolveComponent('opensilex-DetailButton')
const InteroperabilityButton = resolveComponent('opensilex-InteroperabilityButton')
const DeleteButton = resolveComponent('opensilex-DeleteButton')
const UriLink = resolveComponent('opensilex-UriLink')

/** Données */
const variables = ref<Array<{ item: VariableGetDTO }>>([])  // page courante (serveur)
const serverTotal = ref(0)                                  // total côté API

/** Sélection globale (toutes pages) */
const allSelected = ref(false)
const selectedSet = ref<Set<DataTableRowKey>>(new Set())
const unselectedSet = ref<Set<DataTableRowKey>>(new Set())

const filtersCollapsed = ref(false) // true = replié, false = visible


const checkedRowKeys = ref<DataTableRowKey[]>([])
let prevPageChecked = new Set<DataTableRowKey>()

const selectedCount = computed(() =>
  allSelected.value ? serverTotal.value - unselectedSet.value.size : selectedSet.value.size
)

/** Mode "Sélection seulement" (vue locale) */
const onlySelected = ref(false)
const selectedItems = ref<Array<{ item: VariableGetDTO }>>([])  // dataset complet sélectionné (cache)
const tableData = computed(() => {
  if (!onlySelected.value) return variables.value
  // pagination locale (slice) en mode sélection
  const start = (pagination.value.page - 1) * pagination.value.pageSize
  const end = start + pagination.value.pageSize
  return selectedItems.value.slice(start, end)
})

/** Props */
const props = defineProps({
  showCount: { type: Boolean, default: true }
})

/** Row key */
const rowKey = (row: { item: VariableGetDTO }) => row.item.uri

/** Pagination */
const pagination = ref({
  page: 1,
  pageSize: 10,
  pageSizes: [10, 20, 50, 100],
  showSizePicker: true,
  itemCount: 0
})

/** TRI SERVEUR : état + mapping colonne->clé API */
// En mode onlySelected (remote=false), Naive applique le tri client grâce aux sorter: déjà définis dans les colonnes. En mode normal, le tri est serveur.
const sorterState = ref<{ columnKey?: string; order?: 'ascend' | 'descend' }>({
  columnKey: 'item.name',
  order: 'ascend'
})

const sortMap: Record<string, string> = {
  'item.name': 'name',
  'item.entity.name': 'entity',
  'item.characteristic.name': 'characteristic',
  'item.method.name': 'method',
  'item.unit.name': 'unit'
}

// sortMap (+ buildOrderBy()) pour convertir la colonne Naive en order_by pour l’API.
function buildOrderBy(): string[] {
  const columnKey = sorterState.value?.columnKey
  const variablesOrder = sorterState.value?.order
  const apiKey = columnKey ? sortMap[columnKey] : undefined
  if (!apiKey || !variablesOrder) return []
  return [`${apiKey}=${variablesOrder === 'descend' ? 'desc' : 'asc'}`]
}

/** Fetch d'une page (pour passer le tri à l'API) */
async function fetchVariablesPage() {
  $service.value = $opensilex!.getService<VariablesService>('opensilex.VariablesService')
  const pageIndex = pagination.value.page - 1
  const pageSize  = pagination.value.pageSize
  const orderBy = buildOrderBy()          // on transmet le tri

const response = await $service.value.searchVariables(
  filter.name,              // name
  filter.entity,            // entity
  filter.entityOfInterest,  // entity_of_interest
  filter.characteristic,    // characteristic
  filter.method,            // method
  filter.unit,              // unit
  filter.includedGroup,     // group_of_variables
  filter.notIncludedGroup,  // not_included_in_group_of_variables
  filter.dataType,          // data_type
  filter.timeInterval,      // time_interval
  filter.species,           // species
  undefined,                // withAssociatedData
  filter.experiment,        // experiments
  filter.objects,           // scientific_objects
  filter.devices,           // devices
  orderBy,                  // order_by
  pageIndex,                // page (0-based)
  pageSize,                 // page_size
  filter.sharedResourceInstance // sharedResourceInstance
)

  const result = response.response.result || []
  serverTotal.value = response.response.metadata?.pagination?.totalCount ?? result.length

  variables.value = result.map(variable => ({ item: variable }))
  pagination.value.itemCount = serverTotal.value

  // Ré-appliquer la sélection globale à la page courante
  const pageKeys = variables.value.map(result => result.item.uri)
  checkedRowKeys.value = pageKeys.filter(key => isGloballySelected(key))
  prevPageChecked = new Set(checkedRowKeys.value)
}

/** handler de tri */
function onSortChange(state: any) {
  sorterState.value = state
  pagination.value.page = 1
  if (!onlySelected.value) {
    fetchVariablesPage() // tri serveur
  }
  // en mode onlySelected (remote=false), Naive gère le tri en client via les `sorter:` de colonnes
}

/** Helpers sélection globale */
function isGloballySelected(key: DataTableRowKey): boolean {
  return allSelected.value ? !unselectedSet.value.has(key) : selectedSet.value.has(key)
}

watch(checkedRowKeys, (now) => {
  const nowSet = new Set(now)
  // Ajouts sur cette page
  for (const key of nowSet) {
    if (!prevPageChecked.has(key)) {
       emit('select', { uri: String(key), name: variables.value.find(row => row.item.uri === key)?.item.name })
    
      if (allSelected.value) {
        unselectedSet.value.delete(key)
      } else {
        selectedSet.value.add(key)
      }
    }
  }
  // Retraits sur cette page
  for (const key of prevPageChecked) {
    if (!nowSet.has(key)) {
       emit('unselect', { uri: String(key), name: variables.value.find(row => row.item.uri === key)?.item.name })
      if (allSelected.value) {
        unselectedSet.value.add(key)
      } else {
        selectedSet.value.delete(key)
      }
    }
  }
  prevPageChecked = nowSet
})

/** Select All = sélection globale virtuelle */
async function handleSelectAllClick() {
  allSelected.value = true
  selectedSet.value.clear()
  const pageKeys = variables.value.map(result => result.item.uri)
  checkedRowKeys.value = pageKeys.filter(key => !unselectedSet.value.has(key))
  prevPageChecked = new Set(checkedRowKeys.value);
  emit(
    'selectall',
    variables.value
      .filter(row => !unselectedSet.value.has(row.item.uri))
      .map(row => ({ uri: row.item.uri, name: row.item.name }))
  )
}

/** Reset sélection */
function resetSelection() {
  allSelected.value = false
  selectedSet.value.clear()
  unselectedSet.value.clear()
  checkedRowKeys.value = []
  prevPageChecked = new Set()
  if (onlySelected.value) {
    onlySelected.value = false
  }
  fetchVariablesPage()
}

/** Mode “Sélection seulement” */
async function toggleOnlySelected() {
  onlySelected.value = !onlySelected.value
  pagination.value.page = 1

  if (onlySelected.value) {
    selectedItems.value = await fetchAllSelectedItems()
    pagination.value.itemCount = selectedItems.value.length
    syncCheckedForOnlySelectedPage()
  } else {
    await fetchVariablesPage()
  }
}

/** Récupère toutes les variables sélectionnées (items complets) */
async function fetchAllSelectedItems(): Promise<Array<{ item: VariableGetDTO }>> {
  const items: Array<{ item: VariableGetDTO }> = []
  const pageSize = 300
  let page = 0

  while (true) {
    const resp = await $service.value!.searchVariables(
      undefined, undefined, undefined, undefined, undefined,
      undefined, undefined, undefined, undefined, undefined,
      undefined, undefined, undefined, undefined,
      undefined,
      [], page, pageSize, undefined
    )
    const result: VariableGetDTO[] = resp.response.result || []
    if (result.length === 0) break

    for (const variable of result) {
      const key = variable.uri
      if (allSelected.value) {
        if (!unselectedSet.value.has(key)) items.push({ item: variable })
      } else {
        if (selectedSet.value.has(key)) items.push({ item: variable })
      }
    }

    const total = resp.response.metadata?.pagination?.totalCount ?? items.length
    if ((page + 1) * pageSize >= total) break
    page += 1
  }
  return items
}

/** Détails/Groupes */
const variableGroupsList = reactive<Record<string, { uri: string; name: string }[]>>({})
const expandedRowKeys = ref<string[]>([])

const toggleExpand = async (uri: string) => {
  const index = expandedRowKeys.value.indexOf(uri)
  if (index === -1) {
    expandedRowKeys.value.push(uri)
    await handleExpandedRowChange([...expandedRowKeys.value])
  } else {
    expandedRowKeys.value.splice(index, 1)
  }
}

const handleExpandedRowChange = async (keys: string[]) => {
  expandedRowKeys.value = keys

  for (const uri of keys) {
    if (!variableGroupsList[uri]) {
      try {
        const svc = $opensilex!.getService<VariablesService>('opensilex.VariablesService')
        const response = await svc.searchVariablesGroups(undefined, uri, ['name=asc'])
        variableGroupsList[uri] = (response?.response.result || []).map(group => ({
          uri: group.uri,
          name: group.name
        }))
      } catch (error) {
        $opensilex?.errorHandler(error)
      }
    }
  }
}

/** Colonnes */
const emit = defineEmits<{
  (e: 'edit', uri: string): void
  (e: 'delete', item: VariableGetDTO): void
  (e: 'select', item: { uri: string; name?: string }): void
  (e: 'unselect', item: { uri: string; name?: string }): void
  (e: 'selectall', items: Array<{ uri: string; name?: string }>): void
}>()

function createColumns(t: Function, emit: Function, loadVariablesGroupFromVariable: Function) {
  return [
    { type: 'selection' },
    {
      type: 'expand',
      expandable: () => true,
      renderExpand: (row: any) => {
        const uri = row.item.uri
        const groups = variableGroupsList[uri] || []
        if (groups.length === 0) {
          return h('p', {}, t('VariableList.not-used-in-variablesGroup'))
        }
        return h('div', {}, [
          h('p', {}, t('VariableList.variablesGroup')),
          h('ul', {}, groups.map(group =>
            h('li', { key: group.uri }, `${group.name} (${group.uri})`)
          ))
        ])
      }
    },
    {
      title: t('component.common.name'),
      key: 'item.name',
      sortable: true,
      resizable: true,
      sorter: (a, b) => a.item.name.localeCompare(b.item.name),
      render(row: any) {
        return h('div', {}, [
          h(
            UriLink,
            {
              uri: row.item.uri,
              value: row.item.name,
              to: { path: `/variable/details/${encodeURIComponent(row.item.uri)}` },
              allowCopy: true,
              class: 'uri-in-table',
              inTable: true
            },
            { default: () => row.item.name }
          ),
          h('br'),
          h('small', { class: 'text-muted' }, row.item.alternative_name)
        ])
      }
    },
    {
      title: t('component.variable.entity.entity'),
      key: 'item.entity.name',
      sortable: true,
      resizable: true,
      sorter: (a, b) => (a.item.entity?.name || '').localeCompare(b.item.entity?.name || ''),
      render: row => row.item.entity?.name
    },
    {
      title: t('component.variable.characteristic.characteristic'),
      key: 'item.characteristic.name',
      sortable: true,
      resizable: true,
      sorter: (a, b) => (a.item.characteristic?.name || '').localeCompare(b.item.characteristic?.name || ''),
      render: row => row.item.characteristic?.name
    },
    {
      title: t('component.variable.method.method'),
      key: 'item.method.name',
      sortable: true,
      resizable: true,
      sorter: (a, b) => (a.item.method?.name || '').localeCompare(b.item.method?.name || ''),
      render: row => row.item.method?.name
    },
    {
      title: t('component.variable.unit.unit'),
      key: 'item.unit.name',
      sortable: true,
      resizable: true,
      sorter: (a, b) => (a.item.unit?.name || '').localeCompare(b.item.unit?.name || ''),
      render: row => row.item.unit?.name
    },
    {
      title: t('component.common.actions'),
      key: 'actions',
      render(row) {
        return h('div', { class: 'btn-group btn-group-sm', role: 'group' }, [
          h(DetailButton, {
            label: 'component.common.details-label',
            small: true,
            detailVisible: expandedRowKeys.value.includes(row.item.uri),
            onClick: () => loadVariablesGroupFromVariable(row.item.uri)
          }),
          h(EditButton, {
            label: 'component.common.list.buttons.update',
            small: true,
            onClick: () => emit('edit', row.item.uri)
          }),
          h(InteroperabilityButton, {
            label: 'component.common.list.buttons.interoperability',
            small: true,
            onClick: () => emit('onInteroperability', row.item.uri)
          }),
          h(
            DeleteButton,
            {
              label: 'component.common.list.buttons.delete',
              small: true,
              onClick: () => emit('delete', row.item.uri)
            },
            t('component.common.delete')
          )
        ])
      }
    }
  ]
}

const columns = computed(() => createColumns(t, emit, toggleExpand))

const dropdownOptions = computed(() => [
  {
    type: 'group',
    label: t('VariableList.group-actions'),
    key: 'actions',
    children: [
      { label: t('VariableList.add-groupVariable'), key: 'addVariablesToGroups' },
      { label: t('VariableList.add-newGroupVariable'), key: 'showGroupVariablesCreateForm' }
    ]
  },
  { type: 'divider' },
  {
    type: 'group',
    label: t('VariableList.import-export-actions'),
    key: 'importExport',
    children: [
      { label: t('VariableList.export-variables'), key: 'classicExportVariables' },
      { label: t('VariableList.export-variables-details'), key: 'detailsExportVariables' },
      { label: t('VariableList.import-variables-from-shared-resources'), key: 'importVariablesOnLocal' }
    ]
  }
])


function handleDropdownAction(key: string) {
  switch (key) {
    case 'addVariablesToGroups':
      groupVariableSelection.value?.show()
      break
    case 'showGroupVariablesCreateForm':
      showGroupVariablesCreateForm()
      break
  }
}

const selectionOptions = computed(() => [
  { label: onlySelected.value ? t('VariableList.selected-all') : t('component.common.selected-only') + t(' (' + selectedCount.value + ')'), key: 'toggleOnlySelected' },
  { type: 'divider' },
  { label: t('component.common.resetSelected'), key: 'resetSelection' },
  { type: 'divider' },
  { label: t('component.common.select-all') + t(' (' + total.value + ')'), key: 'selectAll' }
])

function handleSelectionAction(key: string) {
  switch (key) {
    case 'toggleOnlySelected':
      toggleOnlySelected()
      break
    case 'resetSelection':
      resetSelection()
      break
    case 'selectAll':
      handleSelectAllClick()
      break
  }
}

/** “Créer un groupe” prérempli avec les sélectionnés */
function getSelectedUris(): string[] {
  if (allSelected.value) {
    return Array.from(globalSelectedUrisFast())
  } else {
    return Array.from(selectedSet.value) as string[]
  }
}

/** Vue rapide des URIs sélectionnées (sans re-fetch) */
function* globalSelectedUrisFast(): Generator<string> {
  if (allSelected.value) {
    for (const r of variables.value) {
      if (!unselectedSet.value.has(r.item.uri)) yield r.item.uri
    }
  } else {
    for (const uri of selectedSet.value) yield String(uri)
  }
}

/** Variante “vraiment toutes les URIs”*/
async function fetchAllVariableUris(): Promise<string[]> {
  const uris: string[] = []
  const pageSize = 500
  let page = 0
  while (true) {
    const resp = await $service.value!.searchVariables(
      undefined, undefined, undefined, undefined, undefined,
      undefined, undefined, undefined, undefined, undefined,
      undefined, undefined, undefined, undefined,
      undefined,
      [], page, pageSize, undefined
    )
    const items = resp.response.result || []
    if (items.length === 0) break
    for (const v of items) uris.push(v.uri)

    const total = resp.response.metadata?.pagination?.totalCount ?? uris.length
    if ((page + 1) * pageSize >= total) break
    page += 1
  }
  return uris
}

function showGroupVariablesCreateForm() {
  loadGroupVariablesForm.value = true

  nextTick(async () => {
    let selectedUris: string[]
    if (allSelected.value) {
      const allUris = await fetchAllVariableUris()
      const filtered = allUris.filter(u => !unselectedSet.value.has(u))
      selectedUris = filtered
    } else {
      selectedUris = Array.from(selectedSet.value) as string[]
    }

    const labelMap = new Map(variables.value.map(variable => [variable.item.uri, variable.item.name]))
    const selected = selectedUris.map(uri => ({ uri, name: labelMap.get(uri) || uri }))

    const form = GroupVariablesForm.getEmptyForm()
    form.variables = selectedUris

    groupVariablesForm.value?.setSelectorsToFirstTimeOpenAndSetLabels(selected)
    groupVariablesForm.value?.showCreateForm(form)
  })
}

/** Pagination events */
function onPageChange(page: number) {
  pagination.value.page = page
  if (onlySelected.value) {
    syncCheckedForOnlySelectedPage()
    return
  }
  fetchVariablesPage()
}

function onPageSizeChange(size: number) {
  pagination.value.pageSize = size
  pagination.value.page = 1
  if (onlySelected.value) {
    pagination.value.itemCount = selectedItems.value.length
    syncCheckedForOnlySelectedPage()
    return
  }
  fetchVariablesPage()
}

function syncCheckedForOnlySelectedPage () {
  if (!onlySelected.value) return
  const start = (pagination.value.page - 1) * pagination.value.pageSize
  const end = start + pagination.value.pageSize
  const slice = selectedItems.value.slice(start, end)
  const keys = slice.map(selected => selected.item.uri)
  checkedRowKeys.value = keys
  prevPageChecked = new Set(keys)
}

/** Bandeau “Affiche X à Y des Z éléments” */
const paginationInfo = computed(() => {
  const total = onlySelected.value ? selectedItems.value.length : serverTotal.value
  const page = pagination.value.page
  const pageSize = pagination.value.pageSize
  const start = total === 0 ? 0 : (page - 1) * pageSize + 1
  const end = Math.min(page * pageSize, total)
  return { start, end, total, hasResults: total > 0 }
})
const start = computed(() => paginationInfo.value.start)
const end = computed(() => paginationInfo.value.end)
const total = computed(() => paginationInfo.value.total)
const hasResults = computed(() => paginationInfo.value.hasResults)

/** Mount */
onMounted(() => {
  fetchVariablesPage()
})

async function editGroupVariable(selection: Array<{ uri: string; variables?: Array<{ uri: string }> }>) {
  try {
    if (!selection || selection.length === 0) {
      $opensilex?.showWarningToast(t('component.group.no-group-selected'))
      return
    }

    // URIs des variables sélectionnées dans la table (gère "Tout sélectionner")
    let selectedVariableUris: string[]
    if (allSelected.value) {
      const all = await fetchAllVariableUris()
      selectedVariableUris = all.filter(u => !unselectedSet.value.has(u))
    } else {
      selectedVariableUris = Array.from(selectedSet.value) as string[]
    }


    // Pour chaque groupe choisi → charger le groupe, fusionner, puis update
    for (const group of selection) {
      const selectedGroup = await $service.value.getVariablesGroup(group.uri)
      const selectedVariableGroup = selectedGroup.response.result

      // variables déjà présentes dans le groupe
      const currentUris = new Set<string>((selectedVariableGroup.variables || []).map((v: any) => v.uri))

      // Si la modale a déjà retourné la liste "variables" sur la ligne du groupe (comme en Vue2),
      // on les merge aussi par sécurité
      if (group.variables && group.variables.length > 0) {
        for (const variable of group.variables) currentUris.add(variable.uri)
      }

      // Ajouter la sélection courante
      for (const uri of selectedVariableUris) currentUris.add(uri)

      // Construire le payload d’update (même shape qu’avant : dto du groupe + variables: string[])
      const form: any = {
        ...selectedVariableGroup,
        variables: Array.from(currentUris)
      }

      await updateVariableGroup(form)
    }

    // Refresh UI
    if (expandedRowKeys.value.length > 0) {
      for (const uri of expandedRowKeys.value) {
        delete (variableGroupsList as any)[uri]
      }
      await handleExpandedRowChange([...expandedRowKeys.value])
    }

    if (!onlySelected.value) {
      await fetchVariablesPage()
    } else {
      // mode “sélection seulement” -> on regénère le cache local
      selectedItems.value = await fetchAllSelectedItems()
      syncCheckedForOnlySelectedPage()
    }

    $opensilex?.showSuccessToast(t('component.group.group-updated'))
  } catch (err) {
    console.error('[editGroupVariable] error', err)
    $opensilex?.errorHandler(err)
  }
}

// wrapper update comme avant
async function updateVariableGroup(form: any) {
  try {
    await $service.value.updateVariablesGroup(form)
  } catch (e) {
    $opensilex?.errorHandler(e)
    throw e
  }
}


function create() {/* no-op */}
function update() {/* no-op */}
function successMessage() { return '' }
const loadGroupVariablesForm = ref(false)

// ---------------------------------------------------------------------------

// Retourner la sélection courante
function getSelected(): Array<{ uri: string; name?: string }> {
  const selectedUris = allSelected.value
    ? variables.value.filter(r => !unselectedSet.value.has(r.item.uri)).map(r => r.item.uri)
    : Array.from(selectedSet.value) as string[]
  // on renvoie (uri, name) si dispo
  const nameByUri = new Map(variables.value.map(v => [v.item.uri, v.item.name]))
  return selectedUris.map(uri => ({ uri, name: nameByUri.get(uri) }))
}


// Hooks pour que la modale puisse cocher/décocher des lignes
function onItemSelected(row: any) {
  const key = typeof row === 'string' ? row : row?.uri || row?.id
  if (!key) return
  if (allSelected.value) {
    // en mode "tout", on enlève l'exception si présente
    unselectedSet.value.delete(key)
  } else {
    selectedSet.value.add(key)
  }
  // refléter sur la page courante si la ligne est visible
  if (variables.value.some(r => r.item.uri === key)) {
    if (!checkedRowKeys.value.includes(key)) {
      checkedRowKeys.value = [...checkedRowKeys.value, key]
    }
  }
}

function onItemUnselected(row: any) {
  const key = typeof row === 'string' ? row : row?.uri || row?.id
  if (!key) return
  if (allSelected.value) {
    // en mode "tout", on ajoute une exception
    unselectedSet.value.add(key)
  } else {
    selectedSet.value.delete(key)
  }
  checkedRowKeys.value = checkedRowKeys.value.filter(k => k !== key)
}

function normalizeUri(uri: string): string {
  // uniformise : si l'URI est abrégée, on reconstruit le http://phenome.inrae.fr/...
  if (!uri) return ''
  if (uri.startsWith('http')) return uri
  // ajuste le préfixe selon ton contexte (ici "m3p:id/variable" → "http://phenome.inrae.fr/m3p/id/variable")
  return uri.replace(/^m3p:id/, 'http://phenome.inrae.fr/m3p/id')
}


async function setInitiallySelectedItems(items: Array<{ uri: string }>) {
  allSelected.value = false
  selectedSet.value = new Set(
    items.map(i => normalizeUri(i.uri))
  )
  unselectedSet.value.clear()
}

// Refreshs attendus par la modale
function refresh() {
  if (onlySelected.value) {
    syncCheckedForOnlySelectedPage()
  } else {
    fetchVariablesPage()
  }
}
function refreshWithKeepingSelection() {
  refresh()
}

async function applySelectionToPage() {
  await nextTick()
  await new Promise(r => setTimeout(r, 100))

  const pageKeys = variables.value.map(r => normalizeUri(r.item.uri))
  const selectedUris = Array.from(selectedSet.value).map(normalizeUri)
  checkedRowKeys.value = pageKeys.filter(key => selectedUris.includes(key))
  prevPageChecked = new Set(checkedRowKeys.value)

  await nextTick()


  console.log("applySelectionToPage:", { 
  pageKeys, 
  checkedRowKeys: checkedRowKeys.value, 
  selectedSet: Array.from(selectedSet.value) 
})

}

/** ---------------- Filtres (panneau latéral) ---------------- */
const showFilters = ref(false)
const loadAdvancedSearchFilters = ref(false)

const filter = reactive({
  sharedResourceInstance: undefined as string | undefined,
  name: undefined as string | undefined,
  entity: undefined as string | undefined,
  entityOfInterest: undefined as string | undefined,
  characteristic: undefined as string | undefined,
  method: undefined as string | undefined,
  unit: undefined as string | undefined,
  includedGroup: undefined as string | undefined,
  notIncludedGroup: undefined as string | undefined,
  dataType: undefined as string | undefined,
  timeInterval: undefined as string | undefined,
  experiment: undefined as string | undefined,
  objects: undefined as string | undefined,
  devices: undefined as string | undefined,
  species: [] as string[]
})

const withoutGroup = ref(false)

watch(withoutGroup, (val) => {
  if (val) {
    filter.notIncludedGroup = filter.includedGroup
    filter.includedGroup = undefined
  } else {
    filter.includedGroup = filter.notIncludedGroup
    filter.notIncludedGroup = undefined
  }
})

function resetFilters () {
  const sri = filter.sharedResourceInstance
  Object.assign(filter, {
    sharedResourceInstance: sri,
    name: undefined,
    entity: undefined,
    entityOfInterest: undefined,
    characteristic: undefined,
    method: undefined,
    unit: undefined,
    includedGroup: undefined,
    notIncludedGroup: undefined,
    dataType: undefined,
    timeInterval: undefined,
    experiment: undefined,
    objects: undefined,
    devices: undefined,
    species: []
  })
  withoutGroup.value = false
  // on repart page 1
  pagination.value.page = 1
  fetchVariablesPage()
}

function applyFilters () {
  console.log("APPLY FILTER")
  pagination.value.page = 1
  fetchVariablesPage()
  filtersCollapsed.value = true
}

// proxy pour binder un seul v-model selon "withoutGroup"
const variableGroupProxy = computed<string | undefined>({
  get() {
    return withoutGroup.value ? filter.notIncludedGroup : filter.includedGroup
  },
  set(v) {
    if (withoutGroup.value) {
      filter.notIncludedGroup = v
    } else {
      filter.includedGroup = v
    }
  }
})

const expandedNames = ref<string[]>([])

function onCollapseUpdate(names: string[]) {
  expandedNames.value = names
  if (names.includes('adv')) {
    loadAdvancedSearchFilters.value = true
  }
}

function isSet(v: any): boolean {
  if (v == null) return false
  if (typeof v === 'string') return v.trim().length > 0
  if (Array.isArray(v)) return v.length > 0
  return true
}

const activeFilters = computed(() => {
  let count = 0
  for (const [key, val] of Object.entries(filter)) {
    // skip sharedResourceInstance ?
    if (key === 'sharedResourceInstance') continue

    // cas spécial pour les groupes : si un des deux est rempli,
    // on compte seulement +1
    if (key === 'includedGroup' || key === 'notIncludedGroup') {
      if (isSet(filter.includedGroup) || isSet(filter.notIncludedGroup)) {
        count++
      }
      continue
    }

    if (isSet(val)) count++
  }
  return count
})

// Recalque visuel des cases à cocher quand les lignes changent
watch(
  () => variables.value.map(row => row.item.uri),   // réagit quand la page change
  () => {
    const pageKeys = variables.value.map(row => row.item.uri)
    checkedRowKeys.value = pageKeys.filter(key => isGloballySelected(key))
    prevPageChecked = new Set(checkedRowKeys.value)
  },
  { immediate: true }
)


defineExpose({
  getSelected,
  onItemSelected,
  onItemUnselected,
  setInitiallySelectedItems,
  refresh,
  refreshWithKeepingSelection,
  applySelectionToPage
})
// ---------------------------------------------------------------------------

</script>

<style>
.btn-disabled {
  background-color: #e0e0e0 !important;
  color: #2e2e2e !important;
  border: none !important;
  cursor: not-allowed;
}

.vars-layout {
  background: transparent;
}
.vars-sider {
  background: #fff;
}
.vars-content {
  padding-left: 12px;
}
.sider-title {
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: .5rem;
}

.n-layout .n-layout-scroll-container {
  height: auto;
  }


.listActionButtons{ 
  position: relative;
  display: flex;
  gap: 0 !important; /* espace entre boutons */
}


.filtersNotCollapsed {
  margin-left: 55px
}

.filtersCollapsed {
  margin-left: 415px
}

.filtersGlobalSearchIcon {
  font-size: 1.2em;
}

.globalFiltersSearchButton {
  width: 40px;  
  height: 55px;  
}

.globalFiltersSearchButton span{
  display: block !important;
}

.globalFiltersSearchButton div {
  margin-top: 5px; /* espace entre l’icône et le compteur */
}

.advancedFiltersSearch {
  margin-top: 10px;
}

</style>

<i18n>
en:
  VariableList:
    name-placeholder: Enter variable name
    label-filter: Search variables
    label-filter-placeholder: "Search variables, plant height, plant, humidity, image processing, percentage, air.*humidity, etc.
            This filter apply on variable name."
    selected: Selected Variables
    add-groupVariable: Add to an existing group of variables
    add-newGroupVariable: Add to a new group of variables
    export-variables: Export variable list
    export-variables-details: Export detailed variable list
    import-variables-from-shared-resources: Import from the shared source
    variablesGroup: Variable used in one or many groups of variables
    not-used-in-variablesGroup: Variable not used in any group of variables
    selected-all: All variables
    display: Display
    withoutGroup: Not in group
    withoutGroup-info: Select the checkbox to filter the variables that are not included in the selected group
    group-actions: Groups
    import-export-actions: Import / Export
fr:
  VariableList:
    name-placeholder: Entrer un nom de variable
    label-filter: Chercher une variable
    label-filter-placeholder: "Rechercher des variables : Hauteur de plante, plante, humidité, analyse d'image, pourcentage, air.*humidité, etc.
            Ce filtre s'applique au nom d'une variable."
    selected: Variables Sélectionnées
    add-groupVariable: Ajouter à un groupe de variables existant
    add-newGroupVariable: Ajouter à un nouveau groupe de variables
    export-variables: Exporter la liste de variables
    export-variables-details: Exporter la liste détaillée de variables
    import-variables-from-shared-resources: Importer depuis la source partagée
    variablesGroup: Variable utilisé dans un ou plusieurs groupe de variables
    not-used-in-variablesGroup: La variable n'est utilisé dans aucun groupe de variables
    selected-all: Toutes les variables
    display: Affichage
    withoutGroup: Pas dans ce groupe
    withoutGroup-info: Cocher la case pour filtrer les variables qui n'appartiennent pas au groupe sélectionné
    group-actions: Groupes
    import-export-actions: Import / Export
</i18n>
