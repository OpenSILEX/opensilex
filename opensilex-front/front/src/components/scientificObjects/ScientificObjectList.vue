<template>
  <n-space class="listActionButtons">
    <n-dropdown
      :options="selectionOptions"
      trigger="hover"
      @select="handleSelectionAction"
    >
      <n-button size="small" class="greenThemeColor">
        {{ t('ScientificObjectList.display') }}
      </n-button>
    </n-dropdown>

    <n-dropdown
      v-if="!noActions"
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

    <n-button
      v-if="!noActions"
      size="small"
      class="greenThemeColor"
      :disabled="serverTotal === 0"
      @click="exportCSV(true)"
    >
      {{ t('ScientificObjectList.export-all') }}
    </n-button>

    <div class="displayAndListSelectionCount">
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
      <span> | </span>
      <span>
        {{ t('ScientificObjectList.selected') }} :
        <span class="badge badge-pill greenThemeColor">{{ selectedCount }}</span>
      </span>
    </div>
  </n-space>

  <n-data-table
    :remote="!onlySelected"
    :columns="columns"
    :data="tableData"
    :row-key="rowKey"
    :pagination="pagination"
    :expanded-row-keys="expandedRowKeys"
    v-model:checked-row-keys="checkedRowKeys"
    v-model:sorter="sorterState"
    @update:expanded-row-keys="handleExpandedRowChange"
    @update:page="onPageChange"
    @update:page-size="onPageSizeChange"
    @update:sorter="onSortChange"
  />
</template>

<script setup lang="ts">
import { ref, h, inject, computed, watch, onMounted, onBeforeUnmount, resolveComponent } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { useStore } from 'vuex'
import {
  NButton,
  NDropdown,
  NDataTable,
  NP,
  NSpace,
  type DataTableRowKey
} from 'naive-ui'
import { ScientificObjectsService } from 'opensilex-core'
import type { ExperimentGetDTO } from 'opensilex-core'
import OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import {CriteriaDTO} from "@/components/scientificObjects/CriteriaSearchModalCreator.vue";

export type ScientificObjectFilter = {
  name?: string
  experiment?: string
  germplasm?: string[]
  variables?: string[]
  devices?: string[]
  factorLevels?: string[]
  types?: string[]
  existenceDate?: string
  creationDate?: string
  criteriaDto?: CriteriaDTO
  facility?: string
}

type ScientificObjectDTO = {
  uri: string
  name: string
  rdf_type_name?: string
  creation_date?: string
  destruction_date?: string
}

type ScientificObjectRow = {
  item: ScientificObjectDTO
}

function defaultFilter() {
  return {
    name: '',
    experiment: undefined,
    germplasm: [],
    variables: [],
    devices: [],
    factorLevels: [],
    types: [],
    existenceDate: undefined,
    creationDate: undefined,
    criteriaDto: {}
  }
}

const props = withDefaults(defineProps<{
  isSelectable?: boolean
  noActions?: boolean
  pageSize?: number
  noUpdateURL?: boolean
  maximumSelectedRows?: number
  variables?: string[]
  devices?: string[]
  searchFilter?: ScientificObjectFilter
}>(), {
  isSelectable: false,
  noActions: false,
  pageSize: 20,
  noUpdateURL: false
})

const emit = defineEmits<{
  (e: 'update:searchFilter', value: ScientificObjectFilter): void
  (e: 'select', item: { uri: string; name?: string }): void
  (e: 'unselect', item: { uri: string; name?: string }): void
  (e: 'selectall', items: Array<{ uri: string; name?: string }>): void
  (e: 'createDocument'): void
  (e: 'createEvents'): void
  (e: 'createMoves'): void
  (e: 'update', uri: string): void
}>()

const { t, n, locale } = useI18n()
const route = useRoute()
const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')
const $service = ref<ScientificObjectsService | null>(null)

const DetailButton = resolveComponent('opensilex-DetailButton')
const EditButton = resolveComponent('opensilex-EditButton')
const DeleteButton = resolveComponent('opensilex-DeleteButton')
const UriLink = resolveComponent('opensilex-UriLink')

const filter = ref<ScientificObjectFilter>({
  ...defaultFilter(),
  ...props.searchFilter || {}
})

watch(
  () => props.searchFilter,
  (value) => {
    filter.value = {
      ...defaultFilter(),
      ...value
    }
  },
  { deep: true }
)

watch(
  filter,
  (value) => emit('update:searchFilter', value),
  { deep: true }
)

const rows = ref<ScientificObjectRow[]>([])
const serverTotal = ref(0)

/** Sélection globale */
const allSelected = ref(false)
const selectedSet = ref<Set<DataTableRowKey>>(new Set())
const unselectedSet = ref<Set<DataTableRowKey>>(new Set())

const checkedRowKeys = ref<DataTableRowKey[]>([])
let prevPageChecked = new Set<DataTableRowKey>()

/**
 * Calcule le nombre réel d’éléments sélectionnés.
 * Gère les cas de sélection normale et de sélection globale.
 */
const selectedCount = computed(() =>
  allSelected.value ? serverTotal.value - unselectedSet.value.size : selectedSet.value.size
)

/** Mode "Sélection seulement" */
const onlySelected = ref(false)
const selectedItems = ref<Array<ScientificObjectRow>>([])
const selectedRowsByUri = ref<Map<string, ScientificObjectRow>>(new Map())

const tableData = computed(() => {
  if (!onlySelected.value) return rows.value
  const start = (pagination.value.page - 1) * pagination.value.pageSize
  const end = start + pagination.value.pageSize
  return selectedItems.value.slice(start, end)
})

const rowKey = (row: ScientificObjectRow) => row.item.uri

const pagination = ref({
  page: 1,
  pageSize: props.pageSize,
  pageSizes: [5, 10, 20, 50, 100],
  showSizePicker: true,
  itemCount: 0
})

const sorterState = ref<{ columnKey?: string; order?: 'ascend' | 'descend' }>({
  columnKey: 'name',
  order: 'ascend'
})

const sortMap: Record<string, string> = {
  name: 'name',
  rdf_type_name: 'rdf_type_name',
  creation_date: 'creation_date',
  destruction_date: 'destruction_date'
}

/**
 * Construit le paramètre de tri attendu par l’API à partir de l’état du tableau.
 */
function buildOrderBy(): string[] {
  const columnKey = sorterState.value?.columnKey
  const order = sorterState.value?.order
  const apiKey = columnKey ? sortMap[columnKey] : undefined
  if (!apiKey || !order) return []
  return [`${apiKey}=${order === 'descend' ? 'desc' : 'asc'}`]
}

function isGloballySelected(key: DataTableRowKey): boolean {
  return allSelected.value ? !unselectedSet.value.has(key) : selectedSet.value.has(key)
}

/**
 * Charge une page d’objets scientifiques depuis l’API.
 * Met à jour les lignes, le total, la pagination et les cases cochées.
 */
async function fetchScientificObjectsPage() {
  $service.value = $opensilex!.getService<ScientificObjectsService>('opensilex.ScientificObjectsService')

  const pageIndex = pagination.value.page - 1
  const pageSize = pagination.value.pageSize
  const orderBy = buildOrderBy()

  const response = await $service.value.searchScientificObjects(
    filter.value.experiment,
    filter.value.types,
    filter.value.name,
    undefined,
    filter.value.germplasm,
    filter.value.factorLevels,
    undefined,
    props.variables,
    props.devices,
    filter.value.existenceDate,
    filter.value.creationDate,
    JSON.stringify(filter.value.criteriaDto ?? {}),
    orderBy,
    pageIndex,
    pageSize
  )

  const result = response.response.result || []
  serverTotal.value = response.response.metadata?.pagination?.totalCount ?? result.length

  rows.value = result.map((item: ScientificObjectDTO) => ({ item }))

  for (const row of rows.value) {
    if (selectedSet.value.has(row.item.uri)) {
        selectedRowsByUri.value.set(row.item.uri, row)
    }
  }


  pagination.value.itemCount = serverTotal.value

  const pageKeys = rows.value.map(row => row.item.uri)
  checkedRowKeys.value = pageKeys.filter(key => isGloballySelected(key))
  prevPageChecked = new Set(checkedRowKeys.value)

  onRefreshed()
}

/**
 * Applique un nouveau tri, réinitialise la page courante et recharge les données.
 */
function onSortChange(state: any) {
  sorterState.value = state
  pagination.value.page = 1
  if (!onlySelected.value) {
    fetchScientificObjectsPage()
  }
}

watch(checkedRowKeys, (now) => {
  const nowSet = new Set(now)

for (const key of nowSet) {
  if (!prevPageChecked.has(key)) {
    const row = rows.value.find(row => row.item.uri === key)

    emit('select', {
      uri: String(key),
      name: row?.item.name
    })

    if (allSelected.value) {
      unselectedSet.value.delete(key)
    } else {
      if (
        props.maximumSelectedRows &&
        selectedSet.value.size >= props.maximumSelectedRows &&
        !selectedSet.value.has(key)
      ) {
        checkedRowKeys.value = checkedRowKeys.value.filter(k => k !== key)
        continue
      }
      selectedSet.value.add(key)
      if (row) {
        selectedRowsByUri.value.set(String(key), row)
      }
    }
  }
}

for (const key of prevPageChecked) {
  if (!nowSet.has(key)) {
    emit('unselect', {
      uri: String(key),
      name: rows.value.find(row => row.item.uri === key)?.item.name
    })

    if (allSelected.value) {
      unselectedSet.value.add(key)
    } else {
      selectedSet.value.delete(key)
      selectedRowsByUri.value.delete(String(key))
    }
  }
}

  prevPageChecked = new Set(checkedRowKeys.value)
})

async function handleSelectAllClick() {
  allSelected.value = true
  selectedSet.value.clear()
  const pageKeys = rows.value.map(row => row.item.uri)
  checkedRowKeys.value = pageKeys.filter(key => !unselectedSet.value.has(key))
  prevPageChecked = new Set(checkedRowKeys.value)

  emit(
    'selectall',
    rows.value
      .filter(row => !unselectedSet.value.has(row.item.uri))
      .map(row => ({ uri: row.item.uri, name: row.item.name }))
  )
}

function resetSelection() {
  allSelected.value = false
  selectedSet.value.clear()
  unselectedSet.value.clear()
  selectedRowsByUri.value.clear()
  checkedRowKeys.value = []
  prevPageChecked = new Set()

  if (onlySelected.value) {
    onlySelected.value = false
  }

  fetchScientificObjectsPage()
}

/**
 * Bascule entre la vue complète et la vue contenant uniquement les éléments sélectionnés.
 */
async function toggleOnlySelected() {
  onlySelected.value = !onlySelected.value
  pagination.value.page = 1

  if (onlySelected.value) {
    if (allSelected.value) {
      selectedItems.value = await fetchAllSelectedItems()
    } else {
      selectedItems.value = Array.from(selectedSet.value)
        .map(uri => selectedRowsByUri.value.get(String(uri)))
        .filter((row): row is ScientificObjectRow => !!row)
    }

    pagination.value.itemCount = selectedItems.value.length
    syncCheckedForOnlySelectedPage()
  } else {
    await fetchScientificObjectsPage()
  }
}

/**
 * Récupère tous les objets réellement sélectionnés en parcourant les pages serveur.
 */
async function fetchAllSelectedItems(): Promise<Array<ScientificObjectRow>> {
  const items: Array<ScientificObjectRow> = []
  const pageSize = 300
  let page = 0

  while (true) {
    const resp = await $service.value!.searchScientificObjects(
      filter.value.experiment,
      filter.value.types,
      filter.value.name,
      undefined,
      filter.value.germplasm,
      filter.value.factorLevels,
      undefined,
      props.variables,
      props.devices,
      filter.value.existenceDate,
      filter.value.creationDate,
      JSON.stringify(filter.value.criteriaDto ?? {}),
      [],
      page,
      pageSize
    )

    const result: ScientificObjectDTO[] = resp.response.result || []
    if (result.length === 0) break

    for (const scientificObject of result) {
      const key = scientificObject.uri
      if (allSelected.value) {
        if (!unselectedSet.value.has(key)) items.push({ item: scientificObject })
      } else {
        if (selectedSet.value.has(key)) items.push({ item: scientificObject })
      }
    }

    const total = resp.response.metadata?.pagination?.totalCount ?? items.length
    if ((page + 1) * pageSize >= total) break
    page += 1
  }

  return items
}

/**
 * Synchronise les lignes cochées avec la page affichée en mode "sélection seulement".
 */
function syncCheckedForOnlySelectedPage() {
  if (!onlySelected.value) return

  const start = (pagination.value.page - 1) * pagination.value.pageSize
  const end = start + pagination.value.pageSize
  const slice = selectedItems.value.slice(start, end)
  const keys = slice.map(selected => selected.item.uri)
  checkedRowKeys.value = keys
  prevPageChecked = new Set(keys)
}

function refresh() {
  if (onlySelected.value) {
    onlySelected.value = false
  }
  pagination.value.page = 1

  if (!props.noUpdateURL) {
    $opensilex?.updateURLParameters(filter.value)
  }

  fetchScientificObjectsPage()
}

function onRefreshed() {
  setTimeout(() => {
    if (allSelected.value && selectedItems.value.length !== 0 && selectedItems.value.length !== serverTotal.value) {
      allSelected.value = false
    }
  }, 1)
}

function refreshWithKeepingSelection() {
  if (!props.noUpdateURL) {
    $opensilex?.updateURLParameters(filter.value)
  }

  if (onlySelected.value) {
    if (allSelected.value) {
      fetchAllSelectedItems().then(items => {
        selectedItems.value = items
        pagination.value.itemCount = items.length
        syncCheckedForOnlySelectedPage()
      })
    } else {
      selectedItems.value = Array.from(selectedSet.value)
        .map(uri => selectedRowsByUri.value.get(String(uri)))
        .filter((row): row is ScientificObjectRow => !!row)

      pagination.value.itemCount = selectedItems.value.length
      syncCheckedForOnlySelectedPage()
    }
  } else {
    fetchScientificObjectsPage()
  }
}

function reset() {
  filter.value = defaultFilter()
  refresh()
}

/** Stocke les détails d’usage par expérimentation pour chaque objet développé. */
const objectDetails = ref<Record<string, Array<{ uri: string; name: string }>>>({})
const expandedRowKeys = ref<string[]>([])


 /**Gère l’ouverture des lignes étendues et charge leurs détails si nécessaire. */
async function handleExpandedRowChange(keys: string[]) {
  expandedRowKeys.value = keys

  for (const uri of keys) {
    if (!objectDetails.value[uri]) {
      try {
        $opensilex?.disableLoader()
        const svc = $opensilex!.getService<ScientificObjectsService>('opensilex.ScientificObjectsService')
        const http = await svc.getScientificObjectDetailByExperiments(uri)

        const objectExperiments: Array<{ uri: string; name: string }> = []
        for (const objectDetail of http.response.result || []) {
          if (objectDetail.experiment != null) {
            objectExperiments.push({
              uri: objectDetail.experiment,
              name: objectDetail.experiment_name
            })
          }
        }
        objectDetails.value[uri] = objectExperiments
      } catch (error) {
        $opensilex?.errorHandler(error)
      } finally {
        $opensilex?.enableLoader()
      }
    }
  }
}

async function loadObjectDetail(row: ScientificObjectRow) {
  const uri = row.item.uri
  const index = expandedRowKeys.value.indexOf(uri)

  if (index === -1) {
    expandedRowKeys.value.push(uri)
    await handleExpandedRowChange([...expandedRowKeys.value])
  } else {
    expandedRowKeys.value.splice(index, 1)
  }
}

// const store = computed(() => $opensilex?.getStore?.())
const store = useStore()
const user = computed(() => store.state?.user)
const credentials = computed(() => store.state?.credentials)
const lang = computed(() => store.state?.lang ?? locale.value)


function hasCredential(key?: string): boolean {
  if (!key) return false
  return !!user.value?.hasCredential?.(key)
}

/**
 * Exporte la liste courante ou uniquement les éléments sélectionnés au format CSV.
 */
function exportCSV(exportAll: boolean) {
  const path = '/core/scientific_objects/export'
  const today = new Date()
  const filename =
    'export_scientific_objects_' +
    today.getFullYear() +
    '' +
    today.getMonth() +
    '' +
    today.getDate() +
    '_' +
    today.getHours() +
    '' +
    today.getMinutes() +
    '' +
    today.getSeconds()

  const exportDto: Record<string, any> = {
    experiment: filter.value.experiment,
    rdf_types: filter.value.types,
    name: filter.value.name,
    facility: filter.value.facility,
    germplasm: filter.value.germplasm,
    factor_levels: filter.value.factorLevels,
    existence_date: filter.value.existenceDate,
    creation_date: filter.value.creationDate,
    excluded_uris: undefined,
    order_by: buildOrderBy()
  }

  if (!exportAll) {
    const objectURIs = []
    for (const select of getSelected()) {
      objectURIs.push(select.uri)
    }
    Object.assign(exportDto, {
      uris: objectURIs
    })
  }

  $opensilex?.downloadFilefromPostService(
    path,
    filename,
    'csv',
    exportDto,
    lang.value
  )
}

async function deleteScientificObject(uri: string) {
  try {
    const scientificObjectsService = $opensilex!.getService<ScientificObjectsService>('opensilex.ScientificObjectsService')
    await scientificObjectsService.deleteScientificObject(uri)
    checkedRowKeys.value = checkedRowKeys.value.filter(key => key !== uri)
    selectedSet.value.delete(uri)
    unselectedSet.value.delete(uri)
    selectedRowsByUri.value.delete(uri)
    refresh()
  } catch (error) {
    $opensilex?.errorHandler(error)
  }
}

/**
 * Retourne les éléments actuellement sélectionnés avec leur URI et leur nom si connu.
 */
function getSelected(): Array<{ uri: string; name?: string }> {
  const selectedUris = allSelected.value
    ? rows.value.filter(r => !unselectedSet.value.has(r.item.uri)).map(r => r.item.uri)
    : Array.from(selectedSet.value) as string[]

  const nameByUri = new Map(rows.value.map(v => [v.item.uri, v.item.name]))
  return selectedUris.map(uri => ({ uri, name: nameByUri.get(uri) }))
}

function onItemUnselected(row: any) {
  const key = typeof row === 'string' ? row : row?.uri || row?.id
  if (!key) return

  if (allSelected.value) {
    unselectedSet.value.add(key)
  } else {
    selectedSet.value.delete(key)
    selectedRowsByUri.value.delete(String(key))
  }

  checkedRowKeys.value = checkedRowKeys.value.filter(k => k !== key)
}

function onItemSelected(row: any) {
  const key = typeof row === 'string' ? row : row?.uri || row?.id
  if (!key) return

  if (allSelected.value) {
    unselectedSet.value.delete(key)
  } else {
    if (
      props.maximumSelectedRows &&
      selectedSet.value.size >= props.maximumSelectedRows &&
      !selectedSet.value.has(key)
    ) {
      return
    }
    selectedSet.value.add(key)

    const currentRow = rows.value.find(r => r.item.uri === key)
    if (currentRow) {
      selectedRowsByUri.value.set(String(key), currentRow)
    }
  }

  if (rows.value.some(r => r.item.uri === key)) {
    if (!checkedRowKeys.value.includes(key)) {
      checkedRowKeys.value = [...checkedRowKeys.value, key]
    }
  }
}

/**
 * Initialise la sélection à partir d’une liste d’éléments fournie par le parent.
 */
function setInitiallySelectedItems(initiallySelectedItems: Array<{ uri: string; name?: string }>) {
  allSelected.value = false
  selectedSet.value = new Set(initiallySelectedItems.map(item => item.uri))
  unselectedSet.value.clear()

  selectedRowsByUri.value.clear()
  for (const item of initiallySelectedItems) {
    selectedRowsByUri.value.set(item.uri, {
      item: {
        uri: item.uri,
        name: item.name ?? item.uri
      }
    })
  }
}

function onPageChange(page: number) {
  pagination.value.page = page
  if (onlySelected.value) {
    syncCheckedForOnlySelectedPage()
    return
  }
  fetchScientificObjectsPage()
}

function onPageSizeChange(size: number) {
  pagination.value.pageSize = size
  pagination.value.page = 1

  if (onlySelected.value) {
    pagination.value.itemCount = selectedItems.value.length
    syncCheckedForOnlySelectedPage()
    return
  }
  fetchScientificObjectsPage()
}

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

const columns = computed(() => {
  const base: any[] = []

  if (props.isSelectable) {
    base.push({ type: 'selection' })
  }

  base.push(
    {
      type: 'expand',
      expandable: () => true,
      renderExpand: (row: ScientificObjectRow) => {
        const experiments = objectDetails.value[row.item.uri] || []
        if (experiments.length === 0) {
          return h('div', {}, t('ScientificObjectList.not-used-in-experiments'))
        }

        return h('div', {}, [
          h('div', {}, t('ScientificObjectList.experiments') + ':'),
          h(
            'ul',
            {},
            experiments.map((xp, index) =>
              h('li', { key: `${xp.uri}-${index}` }, [
                h(UriLink, {
                  uri: xp.uri,
                  value: xp.name,
                  to: { path: '/experiment/details/' + encodeURIComponent(xp.uri) }
                })
              ])
            )
          )
        ])
      }
    },
    {
      title: t('component.common.name'),
      key: 'name',
      sortable: true,
      sorter: (a: ScientificObjectRow, b: ScientificObjectRow) =>
        (a.item.name || '').localeCompare(b.item.name || ''),
      render(row: ScientificObjectRow) {
        return h(UriLink, {
          uri: row.item.uri,
          value: row.item.name,
          to: {
            path: '/scientific-objects/details/' + encodeURIComponent(row.item.uri)
          }
        })
      }
    },
    {
      title: t('component.common.type'),
      key: 'rdf_type_name',
      sortable: true,
      sorter: (a: ScientificObjectRow, b: ScientificObjectRow) =>
        (a.item.rdf_type_name || '').localeCompare(b.item.rdf_type_name || ''),
      render: (row: ScientificObjectRow) => row.item.rdf_type_name
    },
    {
      title: t('ScientificObjectList.creationDate'),
      key: 'creation_date',
      sortable: true,
      sorter: (a: ScientificObjectRow, b: ScientificObjectRow) =>
        (a.item.creation_date || '').localeCompare(b.item.creation_date || ''),
      render: (row: ScientificObjectRow) => row.item.creation_date
    },
    {
      title: t('ScientificObjectList.destructionDate'),
      key: 'destruction_date',
      sortable: true,
      sorter: (a: ScientificObjectRow, b: ScientificObjectRow) =>
        (a.item.destruction_date || '').localeCompare(b.item.destruction_date || ''),
      render: (row: ScientificObjectRow) => row.item.destruction_date
    }
  )

  if (!props.noActions) {
    base.push({
      title: t('component.common.actions'),
      key: 'actions',
      render(row: ScientificObjectRow) {
        return h('div', { class: 'btn-group btn-group-sm', role: 'group' }, [
          h(DetailButton, {
            label: 'component.common.details-label',
            detailVisible: expandedRowKeys.value.includes(row.item.uri),
            small: true,
            onClick: () => loadObjectDetail(row)
          }),
          hasCredential(credentials.value?.CREDENTIAL_SCIENTIFIC_OBJECT_MODIFICATION_ID)
            ? h(EditButton, {
                label: 'ExperimentScientificObjects.edit-scientific-object',
                small: true,
                onClick: () => emit('update', row.item.uri)
              })
            : null,
          hasCredential(credentials.value?.CREDENTIAL_SCIENTIFIC_OBJECT_DELETE_ID)
            ? h(DeleteButton, {
                label: 'ExperimentScientificObjects.delete-scientific-object',
                small: true,
                onClick: () => deleteScientificObject(row.item.uri)
              })
            : null
        ].filter(Boolean))
      }
    })
  }

  return base
})

const selectionOptions = computed(() => [
  {
    label: onlySelected.value
      ? t('ScientificObjectList.selected-all')
      : t('component.common.selected-only'),
    key: 'toggleOnlySelected'
  },
  { type: 'divider' },
  { label: t('component.common.resetSelected'), key: 'resetSelected' },
  { type: 'divider' },
  { label: `${t('component.common.select-all')} (${total.value})`, key: 'selectAll' }
])

const dropdownOptions = computed(() => {
  const options: any[] = []

  if (hasCredential(credentials.value?.CREDENTIAL_DOCUMENT_MODIFICATION_ID)) {
    options.push({
      label: t('component.common.addDocument'),
      key: 'createDocument'
    })
  }

  options.push({
    label: 'Export CSV',
    key: 'exportCSVSelected'
  })

  if (hasCredential(credentials.value?.CREDENTIAL_EVENT_MODIFICATION_ID)) {
    options.push(
      { label: t('Event.add-multiple'), key: 'createEvents' },
      { label: t('Move.add'), key: 'createMoves' }
    )
  }

  return options
})

function handleSelectionAction(key: string) {
  switch (key) {
    case 'toggleOnlySelected':
      toggleOnlySelected()
      break
    case 'resetSelected':
      resetSelection()
      break
    case 'selectAll':
      handleSelectAllClick()
      break
  }
}

function handleDropdownAction(key: string) {
  switch (key) {
    case 'createDocument':
      emit('createDocument')
      break
    case 'exportCSVSelected':
      exportCSV(false)
      break
    case 'createEvents':
      emit('createEvents')
      break
    case 'createMoves':
      emit('createMoves')
      break
  }
}

let langUnwatcher: (() => void) | undefined

onMounted(() => {
  if (!props.noUpdateURL) {
    $opensilex?.updateFiltersFromURL?.(route.query, filter.value)
  }

  fetchScientificObjectsPage()

  if (store.value?.watch) {
    langUnwatcher = store.value.watch(
      () => store.value.getters.language,
      () => {
        refreshWithKeepingSelection()
      }
    )
  }
})

onBeforeUnmount(() => {
  langUnwatcher?.()
})

watch(
  () => rows.value.map(row => row.item.uri),
  () => {
    const pageKeys = rows.value.map(row => row.item.uri)
    checkedRowKeys.value = pageKeys.filter(key => isGloballySelected(key))
    prevPageChecked = new Set(checkedRowKeys.value)
  },
  { immediate: true }
)

defineExpose({
  getSelected,
  onItemUnselected,
  onItemSelected,
  setInitiallySelectedItems,
  refresh,
  refreshWithKeepingSelection,
  clickOnlySelected: toggleOnlySelected,
  resetSelected: resetSelection
})
</script>

<style scoped>
.btn-disabled {
  background-color: #e0e0e0 !important;
  color: #2e2e2e !important;
  border: none !important;
  cursor: not-allowed;
}

.listActionButtons {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 12px;
}

.displayAndListSelectionCount {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
}
</style>

<i18n>
en:
  ScientificObjectList:
    selected: Selected Scientific Objects
    selected-all: All scientific objects
    description: Manage and configure scientific objects
    display: Display
    advancedSearch: Advanced search
    propetiesConfiguration: Properties to display
    not-used-in-experiments: Scientific object not used in any experiments
    experiments: Scientific object used in experiment(s)
    column:
      alias: Nom
      experiments: Experiment(s)
      type: Type
      uri: URI
      parents: Parent(s)
      actions: Actions
    placeholder:
      experiments: All Experiments
      germplasm: All Germplasm
      types: All Types
      uri: All URI
      isPartOf: All Alias / URI
      position: All Spacial Positions
      factors: All Factors
      dates: All Dates
    filter:
      label: Search for Scientific Objects
      experiments: Filter by Experiments
      germplasm: Filter by Germplasm
      types: Filter by Type(s)
      uri: Filter by  URI
      isPartOf: isPartOf (Alias ou URI)
      position: Filter by Spatial Position
      factors: Filter by Factors
      dates: Filter by Dates
    creationDate: Creation date
    destructionDate: Destruction date
    existenceDate: Object exists
    visualize: Visualize
    export-all: Export all
fr:
  ScientificObjectList:
    selected: Objets Scientifiques Sélectionnés
    selected-all: Tout les objets scientifiques
    description: Gérer et configurer les objets scientifiques
    display: Affichage
    advancedSearch: Recherche avancée
    propetiesConfiguration: Propriétés à afficher
    not-used-in-experiments: L'objet scientifique n'est utilisé dans aucune expérimentation
    experiments: L'objet scientifique est utilisé dans le/les expérimentation(s)
    column:
      alias: Name
      experiments: Expérimentation(s)
      type: Type
      uri: URI
      parents: Parent(s)
      actions: Actions
    placeholder:
      experiments: Toutes les Expérimentations
      germplasm: Tous les Matériels Génétiques
      types: Tous les Types
      uris: Toutes les URI
      isPartOf: Tous les Alias / URI
      position: Toutes les Positions Spaciales
      factors: Tous les Facteurs
      dates: Toutes les Dates
    filter:
      label: Rechercher des Objets Scientifiques
      experiments: Filtrer par Expérimentation(s)
      germplasm: Filtrer par Matériel Génétiques
      types: Filtrer par Type(s)
      uri: Filtrer par URI
      isPartOf: isPartOf (Alias ou URI)
      position: Filtrer par Position Spaciale
      factors: Filtrer par Facteurs
      dates: Filtrer par Dates
    creationDate: Date de création
    destructionDate: Date de destruction
    existenceDate: Date d'existence
    visualize: Visualiser
    export-all: Tout exporter
</i18n>