<template>
  <n-modal
    v-model:show="visible"
    :mask-closable="false"
    preset="dialog"
    :show-icon="false"
    :style="{ width: '1400px', maxWidth: '95vw' }"
    @after-enter="emit('shown')"
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
                  :placeholder="t('ScientificObjectModalList.filter.name')"
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
                  :label="t('ScientificObjectModalList.filter.experiments')"
                  class="searchFilter"
                  @update:experiments="filter.experiment = $event"
                  @handlingEnterKey="refresh"
                />
              </n-form-item>

              <!-- Type -->
              <n-form-item :label="$t('component.common.type')" :show-feedback="false" class="compact-form-item">
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
                  <n-form-item :label="t('ScientificObjectModalList.filter.existenceDate')" :show-feedback="false">
                    <opensilex-DateForm
                      :value="filter.existenceDate"
                      class="searchFilter"
                      @update:value="filter.existenceDate = $event"
                    />
                  </n-form-item>
                  <br>
                  <!-- Creation date -->
                  <n-form-item :label="t('ScientificObjectModalList.filter.creationDate')" :show-feedback="false">
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
            @select="emit('select', $event)"
            @unselect="emit('unselect', $event)"
            @selectall="emit('selectall', $event)"
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
import { ref, reactive, computed, watch } from 'vue'
import { NModal, NButton, NSpace, NLayout, NLayoutSider, NLayoutContent, NForm, NFormItem, NCollapse, NCollapseItem } from 'naive-ui'
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

const props = defineProps<{
  searchFilter?: ScientificObjectFilter
}>()

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

watch(
  () => props.searchFilter,
  (newValue) => {
    Object.assign(filter, defaultFilter(), newValue || {})
  },
  { immediate: true, deep: true }
)

const filtersCollapsedProxy = computed({
  get: () => !searchFiltersToggle.value,
  set: (collapsed: boolean) => {
    searchFiltersToggle.value = !collapsed
  }
})

function emitFilterUpdate() {
  emit('update:searchFilter', { ...filter })
}

function selectItem(row: any) {
  soList.value?.onItemSelected?.(row)
}

function unSelect(row: any) {
  soList.value?.onItemUnselected?.(row)
}

function show() {
  visible.value = true
}

function hide(validate: boolean) {
  visible.value = false
  if (validate) {
    emit('onValidate', soList.value?.getSelected?.() ?? [])
  } else {
    emit('onClose')
  }
}

function refresh() {
  emitFilterUpdate()
  soList.value?.refresh?.()
}

function reset() {
  Object.assign(filter, defaultFilter())
  criteriaSearchCreateModal.value?.resetCriteriaListAndSave?.()
  emitFilterUpdate()
  refresh()
}

function refreshWithKeepingSelection() {
  emitFilterUpdate()
  soList.value?.refreshWithKeepingSelection?.()
}

function searchFiltersPanel() {
  return t('searchfilter.label')
}

defineExpose({
  show,
  hide,
  refresh,
  reset,
  refreshWithKeepingSelection,
  selectItem,
  unSelect
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
}

.so-layout {
  background: transparent;
  overflow: scroll;
}

.so-sider {
  background: #fff;
}

.so-content {
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

.modal-body {
  padding: 8px 0;
  height: 72vh;
  overflow: hidden;
}

.so-layout {
  height: 100%;
}

.so-sider,
.so-content {
  height: 100%;
}

.so-sider {
  overflow: auto;
}

.so-content {
  overflow: auto;
  padding-left: 12px;
}

/* neutralisation des classes injectées par naive dans les <n-form-item> qui créent des espaces indésirés entre les champs */
:deep(.compact-form-item) {
  --n-label-height: 0px !important;
  --n-label-padding: 0 !important;
}
</style>


<i18n>
en:
    ScientificObjectModalList:
        filter:
            experiments: Experiment(s)
            name: Enter name,
            creationDate: Creation date
            existenceDate: Object exists

fr:
    ScientificObjectModalList:
        filter:
            experiments: Expérimentation(s)
            name: Saisir un nom
            creationDate: Date de création
            existenceDate: Date d'existence
            
</i18n>