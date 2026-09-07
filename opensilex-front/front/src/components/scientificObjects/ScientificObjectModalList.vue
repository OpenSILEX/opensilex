<template>
  <n-modal
    v-model:show="visible"
    :mask-closable="false"
    preset="dialog"
    :show-icon="false"
    :style="{ width: '1400px', maxWidth: '95vw' }"
    @after-enter="onModalShown"
  >
    <template #header>
      <div class="modal-title">
        <i class="bi bi-search"></i>
        {{ $t('component.project.filter-description') }}
      </div>
    </template>

    <div class="modal-body">
      <n-layout has-sider class="so-layout">
        <n-space class="mb-2 me-1" align="top">
          <n-button
            quaternary
            circle
            @click="searchFiltersToggle = !searchFiltersToggle"
            :title="searchFiltersPanel()"
            :class="{ greenThemeColor: searchFiltersToggle }"
            class="globalFiltersSearchButton"
          >
            <i class="bi bi-search filtersGlobalSearchIcon"></i>
          </n-button>
        </n-space>

        <!-- SIDEBAR / FILTRES -->
        <n-layout-sider
          v-model:collapsed="filtersCollapsedProxy"
          :collapsed-width="0"
          :width="360"
          collapse-mode="width"
          show-trigger
          bordered
          class="so-sider"
        >
          <n-space class="p-3" vertical>
            <n-form label-placement="top" size="small" @submit.prevent="refresh">
              <!-- Name -->
              <n-form-item :label="t('component.common.name')" class="compact-form-item">
                <opensilex-StringFilter
                  :filter="filter.name"
                  :placeholder="t('component.common.forms-generic-placeholders.name-placeholder')"
                  class="searchFilter"
                  @update:filter="filter.name = $event"
                  @handlingEnterKey="refresh"
                />
              </n-form-item>

              <!-- Experiment -->
              <n-form-item :show-feedback="false" class="compact-form-item">
                <opensilex-ExperimentSelector
                  :multiple="false"
                  :experiments="filter.experiment"
                  :label="t('component.experiment.view.experiment-experiments')"
                  class="searchFilter"
                  @update:experiments="filter.experiment = $event"
                  @handlingEnterKey="refresh"
                />
              </n-form-item>

              <!-- Type -->
              <n-form-item
                :label="$t('component.common.type')"
                :show-feedback="false"
                class="compact-form-item"
              >
                <opensilex-ScientificObjectTypeSelector
                  :types="filter.types"
                  :multiple="true"
                  class="searchFilter"
                  @update:types="filter.types = $event"
                  @handlingEnterKey="refresh"
                />
              </n-form-item>

              <!-- Advanced -->
              <n-collapse
                v-model:expanded-names="expandedNames"
                :accordion="false"
                class="advancedFiltersSearch"
              >
                <n-collapse-item :title="$t('component.common.advanced-search-title')" name="adv">
                  <!-- Germplasm -->
                  <n-form-item :show-feedback="false" class="compact-form-item">
                    <opensilex-GermplasmSelector
                      :multiple="false"
                      :germplasm="filter.germplasm"
                      :experiment="filter.experiment"
                      class="searchFilter"
                      @update:germplasm="filter.germplasm = $event"
                      @handlingEnterKey="refresh"
                    />
                  </n-form-item>

                  <!-- Factor levels -->
                  <n-form-item :show-feedback="false" class="compact-form-item">
                    <opensilex-FactorLevelSelector
                      :factorLevels="filter.factorLevels"
                      :multiple="true"
                      :required="false"
                      class="searchFilter"
                      @update:factorLevels="filter.factorLevels = $event"
                      @handlingEnterKey="refresh"
                    />
                  </n-form-item>

                  <!-- Existence date -->
                  <n-form-item
                    :label="t('component.scientificObjects.filters.existenceDate')"
                    :show-feedback="false"
                  >
                    <opensilex-DateForm
                      :value="filter.existenceDate"
                      class="searchFilter"
                      @update:value="filter.existenceDate = $event"
                    />
                  </n-form-item>

                  <br />

                  <!-- Creation date -->
                  <n-form-item
                    :label="t('component.common.date-time.creationDate')"
                    :show-feedback="false"
                  >
                    <opensilex-DateForm
                      :value="filter.creationDate"
                      class="searchFilter"
                      @update:value="filter.creationDate = $event"
                    />
                  </n-form-item>

                  <!-- Criteria search -->
                  <n-form-item :show-feedback="false">
                    <opensilex-CriteriaSearchModalCreator
                      ref="criteriaSearchCreateModal"
                      class="searchFilter"
                      :criteria_dto="filter.criteriaDto"
                      :required="false"
                      :requiredBlue="false"
                      @update:criteria_dto="filter.criteriaDto = $event"
                    />
                  </n-form-item>
                </n-collapse-item>
              </n-collapse>

              <n-space justify="end" class="mt-2">
                <n-button tertiary @click="reset">
                  {{ $t('component.common.search.clear-button') }}
                </n-button>
                <n-button type="primary" class="greenThemeColor" @click="refresh">
                  {{ $t('component.common.search.search-button') }}
                </n-button>
              </n-space>
            </n-form>
          </n-space>
        </n-layout-sider>

        <!-- CONTENU -->
        <n-layout-content class="so-content">
          <opensilex-ScientificObjectList
            ref="soList"
            :isSelectable="true"
            :noActions="true"
            :pageSize="10"
            :searchFilter="filter"
            :noUpdateURL="true"
            @select="onSelect"
            @unselect="onUnselect"
            @selectall="onSelectAll"
          />
        </n-layout-content>
      </n-layout>
    </div>

    <template #action>
      <n-space justify="end">
        <n-button tertiary @click="hide(false)">
          {{ $t('component.common.close') }}
        </n-button>
        <n-button type="primary" class="greenThemeColor" @click="hide(true)">
          {{ $t('component.common.validateSelection') }}
        </n-button>
      </n-space>
    </template>
  </n-modal>
</template>

<script setup lang="ts">
import { computed, nextTick, reactive, ref, watch } from 'vue'
import {
  NModal,
  NButton,
  NSpace,
  NLayout,
  NLayoutSider,
  NLayoutContent,
  NForm,
  NFormItem,
  NCollapse,
  NCollapseItem
} from 'naive-ui'
import CriteriaSearchModalCreator from './CriteriaSearchModalCreator.vue'
import { useI18n } from 'vue-i18n'

type ScientificObjectFilter = {
  name: string
  experiment: any
  germplasm: any
  factorLevels: any[]
  types: any[]
  existenceDate: any
  creationDate: any
  criteriaDto: { criteria_list: any[] }
}

type SelectableItem = {
  id: string
  label: string
  isDisabled?: boolean
  [key: string]: any
}

const props = defineProps<{
  searchFilter?: ScientificObjectFilter
  selected?: any[]
}>()

const emit = defineEmits<{
  (e: 'update:searchFilter', value: ScientificObjectFilter): void
  (e: 'onValidate', value: any[]): void
  (e: 'onClose'): void
  (e: 'shown'): void
  (e: 'select', value: any): void
  (e: 'unselect', value: any): void
  (e: 'selectall', value: any): void
}>()

const { t } = useI18n()

const soList = ref<any>(null)
const criteriaSearchCreateModal = ref<InstanceType<typeof CriteriaSearchModalCreator> | null>(null)

const visible = ref(false)
const searchFiltersToggle = ref(false)
const expandedNames = ref<string[]>([])

const filter = reactive<ScientificObjectFilter>(defaultFilter())
const selectedItems = ref<SelectableItem[]>([])

function defaultFilter(): ScientificObjectFilter {
  return {
    name: '',
    experiment: undefined,
    germplasm: undefined,
    factorLevels: [],
    types: [],
    existenceDate: undefined,
    creationDate: undefined,
    criteriaDto: { criteria_list: [] }
  }
}

watch(
  () => props.searchFilter,
  newValue => {
    Object.assign(filter, defaultFilter(), newValue || {})
  },
  { immediate: true, deep: true }
)

watch(
  () => props.selected,
  newSelected => {
    selectedItems.value = normalizeItems(newSelected || [])
  },
  { immediate: true, deep: true }
)

const filtersCollapsedProxy = computed({
  get: () => !searchFiltersToggle.value,
  set: (collapsed: boolean) => {
    searchFiltersToggle.value = !collapsed
  }
})

function normalizeItem(item: any): SelectableItem | null {
  if (!item) {
    return null
  }

  if (item.id && item.label) {
    return {
      ...item,
      id: item.id,
      label: item.label,
      uri: item.uri ?? item.id,
      name: item.name ?? item.label
    }
  }

  if (item.uri && item.name) {
    return {
      ...item,
      id: item.uri,
      label: item.name,
      uri: item.uri,
      name: item.name
    }
  }

  if (typeof item === 'string') {
    return {
      id: item,
      label: item,
      uri: item,
      name: item
    }
  }

  return null
}

function normalizeItems(items: any[]): SelectableItem[] {
  const byId = new Map<string, SelectableItem>()

  items.forEach(item => {
    const normalized = normalizeItem(item)

    if (normalized?.id) {
      byId.set(normalized.id, normalized)
    }
  })

  return Array.from(byId.values())
}

function getItemUri(item: any) {
  if (!item) {
    return undefined
  }

  if (typeof item === 'string') {
    return item
  }

  return item.uri ?? item.id
}

function emitFilterUpdate() {
  emit('update:searchFilter', { ...filter })
}

function show() {
  visible.value = true
}

function hide(validate: boolean) {
  visible.value = false

  if (validate) {
    emit('onValidate', selectedItems.value)
  } else {
    emit('onClose')
  }
}

async function refresh() {
  emitFilterUpdate()

  await nextTick()

  if (soList.value?.refreshWithKeepingSelection) {
    await soList.value.refreshWithKeepingSelection()
  } else {
    await soList.value?.refresh?.()
  }

  await applySelectionToPage()
}

async function refreshWithKeepingSelection() {
  await refresh()
}

async function reset() {
  Object.assign(filter, defaultFilter())
  selectedItems.value = []

  criteriaSearchCreateModal.value?.resetCriteriaListAndSave?.()

  emitFilterUpdate()

  await nextTick()
  await soList.value?.resetSelection?.()
  await soList.value?.refresh?.()
}

function searchFiltersPanel() {
  return t('searchfilter.label')
}

function upsertSelectedItem(item: any) {
  const normalized = normalizeItem(item)

  if (!normalized?.id) {
    return
  }

  const index = selectedItems.value.findIndex(selected => selected.id === normalized.id)

  if (index === -1) {
    selectedItems.value.push(normalized)
  } else {
    selectedItems.value[index] = {
      ...selectedItems.value[index],
      ...normalized
    }
  }
}

function removeSelectedItem(item: any) {
  const uri = getItemUri(item)

  if (!uri) {
    return
  }

  selectedItems.value = selectedItems.value.filter(selected => selected.id !== uri)
}

function onSelect(value: any) {
  upsertSelectedItem(value)
  emit('select', normalizeItem(value) ?? value)
}

function onUnselect(value: any) {
  removeSelectedItem(value)
  emit('unselect', normalizeItem(value) ?? value)
}

function onSelectAll(value: any) {
  const values = Array.isArray(value) ? value : [value]

  values.forEach(item => {
    upsertSelectedItem(item)
  })

  emit('selectall', selectedItems.value)
}

function selectItem(row: any) {
  const normalized = normalizeItem(row)

  if (!normalized) {
    return
  }

  upsertSelectedItem(normalized)
  soList.value?.onItemSelected?.(normalized)
}

function unSelect(row: any) {
  const normalized = normalizeItem(row)

  if (!normalized) {
    return
  }

  removeSelectedItem(normalized)
  soList.value?.onItemUnselected?.(normalized)
}

function setInitiallySelectedItems(items: any[]) {
  selectedItems.value = normalizeItems(items)
}

async function applySelectionToPage() {
  await nextTick()

  selectedItems.value.forEach(item => {
    soList.value?.onItemSelected?.(item)
  })
}

async function onModalShown() {
  emit('shown')

  await nextTick()
  await applySelectionToPage()
}

/**
 * Méthodes dédiées au mode "éléments sélectionnés seulement".
 * Si ScientificObjectList / TableAsyncView appelle ces méthodes,
 * elles permettent de récupérer la sélection complète multi-pages.
 */
function getSelected() {
  return selectedItems.value
}

function getSelectedItems() {
  return selectedItems.value
}

defineExpose({
  show,
  hide,
  refresh,
  reset,
  refreshWithKeepingSelection,
  selectItem,
  unSelect,
  setInitiallySelectedItems,
  applySelectionToPage,
  getSelected,
  getSelectedItems
})
</script>

<style scoped>
.modal-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.modal-body {
  padding: 8px 0;
  height: 72vh;
  overflow: hidden;
}

.so-layout {
  height: 100%;
  background: transparent;
}

.so-sider,
.so-content {
  height: 100%;
}

.so-sider {
  background: #fff;
  overflow: auto;
}

.so-content {
  overflow: auto;
  padding-left: 12px;
}

.filtersGlobalSearchIcon {
  font-size: 1.2em;
}

.globalFiltersSearchButton {
  width: 40px;
  height: 55px;
}

.globalFiltersSearchButton span {
  display: block !important;
}

.advancedFiltersSearch {
  margin-top: 10px;
}

/* neutralisation des classes injectées par naive dans les <n-form-item> qui créent des espaces indésirés entre les champs */
:deep(.compact-form-item) {
  --n-label-height: 0px !important;
  --n-label-padding: 0 !important;
}
</style>