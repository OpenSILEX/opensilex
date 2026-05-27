<template>
  <div class="container-fluid">
    <opensilex-PageContent>
      <!-- Barre Actions / Counts / Selection -->
      <n-space
        class="listActionButtons"
        :class="[filtersCollapsed ? 'filtersNotCollapsed' : 'filtersCollapsed']"
      >
        <!-- Dropdown Affichage / sélection -->
        <n-dropdown
          trigger="hover"
          placement="bottom-end"
          :options="displayDropdownOptions"
          @select="onDisplayDropdownSelect"
        >
          <n-button size="small" class="greenThemeColor">
            {{ t('ProvenanceList.display') }}
          </n-button>
        </n-dropdown>

        <!-- Dropdown Actions -->
        <n-dropdown
          v-if="!noActions"
          trigger="hover"
          placement="bottom-end"
          :options="actionsDropdownOptions"
          :disabled="selectedCount === 0"
          @select="onActionsDropdownSelect"
        >
          <n-button
            size="small"
            :disabled="selectedCount === 0"
            :class="selectedCount === 0 ? 'btn-disabled' : 'greenThemeColor'"
          >
            {{ t('component.common.actions') }}
          </n-button>
        </n-dropdown>

        <!-- Affichage Counts / sélection -->
        <div class="displayAndListSelectionCount">
          <div v-if="showCount">
            <div v-if="paginationInfo.hasResults">
              <strong>
                <span class="ml-1">
                  {{
                    t('component.common.list.pagination.nbEntries', {
                      limit: paginationInfo.start,
                      offset: paginationInfo.end,
                      totalRow: n(paginationInfo.total)
                    })
                  }}
                </span>
              </strong>
            </div>
            <div v-else>
              <strong>
                <span class="ml-1">
                  {{ t('component.common.list.pagination.noEntries') }}
                </span>
              </strong>
            </div>
          </div>
          <span> | </span>
          <n-p>
            {{ t('ProvenanceList.selected') }} :
            <span class="badge badge-pill greenThemeColor">{{ selectedCount }}</span>
          </n-p>
        </div>
      </n-space>

      <n-layout has-sider class="provenance-layout">
        <!-- Bouton loupe -->
        <n-space class="mb-2 me-1" align="top">
          <n-button
            quaternary
            circle
            @click="filtersCollapsed = !filtersCollapsed"
            :title="t('ProvenanceList.label-filter')"
            :class="{ greenThemeColor: filtersCollapsed }"
            class="globalFiltersSearchButton"
          >
            <i class="bi bi-search filtersGlobalSearchIcon"></i>

            <div
              v-show="filtersCollapsed && activeFiltersCount > 0"
              class="filters-count-badge"
            >
              ( {{ activeFiltersCount }} )
            </div>
          </n-button>
        </n-space>

        <!-- Sidebar / Filtres -->
        <n-layout-sider
          v-model:collapsed="filtersCollapsed"
          :collapsed-width="0"
          :width="360"
          collapse-mode="width"
          show-trigger
          bordered
          class="provenance-sider"
        >
          <n-space class="p-3" vertical>
            <n-form label-placement="top" size="small" @submit.prevent.stop="applyFilters">
              <!-- Name -->
              <n-form-item :label="t('ProvenanceList.name')" class="compact-form-item">
                <n-input
                  v-model:value="filter.name"
                  clearable
                  :placeholder="t('ProvenanceList.name-placeholder')"
                  @keydown.enter.prevent.stop="applyFilters"
                />
              </n-form-item>

              <!-- Activity type -->
              <n-form-item class="compact-form-item">
                <opensilex-TypeForm
                  v-model:type="filter.activity_type"
                  :baseType="Prov.ACTIVITY_TYPE_URI"
                  :label="t('ProvenanceList.activity_type')"
                  :placeholder="t('ProvenanceList.activity_type-placeholder')"
                  class="searchFilter"
                  @handlingEnterKey="applyFilters"
                />
              </n-form-item>

              <!-- Agent type -->
              <n-form-item class="compact-form-item">
                <opensilex-AgentTypeSelector
                  v-model:selected="filter.agent_type"
                  :multiple="false"
                  :key="lang"
                  class="searchFilter"
                  @clear="clearAgents"
                  @select="clearAgents"
                  @handlingEnterKey="applyFilters"
                />
              </n-form-item>

              <!-- Agents -->
              <n-form-item
                v-if="filter.agent_type === 'vocabulary:Operator'"
                label='Agent'
              >
                <opensilex-PersonSelector
                  v-model:persons="filter.agents"
                  :multiple="true"
                  class="searchFilter"
                  @handlingEnterKey="applyFilters"
                />
              </n-form-item>

              <n-form-item
                v-else-if="filter.agent_type"
                label='Agent'
              >
                <opensilex-DeviceSelector
                  ref="deviceSelector"
                  v-model:value="filter.agents"
                  :multiple="true"
                  :type="filter.agent_type"
                  class="searchFilter"
                  @handlingEnterKey="applyFilters"
                />
              </n-form-item>

              <n-space justify="end" class="mt-2">
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
                />
              </n-space>
            </n-form>
          </n-space>
        </n-layout-sider>

        <!-- Contenu Liste -->
        <n-layout-content class="provenance-content">
          <opensilex-TableAsyncView
            ref="tableRef"
            :searchMethod="loadData"
            :fields="fields"
            :fieldKeyToSortableModelLabelMap="fieldKeyToModelFieldMap"
            defaultSortBy="name"
            :defaultPageSize="pageSize"
            :isSelectable="isSelectable"
            :showHeaderCount="false"
            labelNumberOfSelectedRow="ProvenanceList.selected"
            iconNumberOfSelectedRow="bi#bi-bullseye"
            @select="emit('select', $event)"
            @unselect="emit('unselect', $event)"
            @selectall="emit('selectall', $event)"
            @refreshed="onRefreshed"
          >
            <template #cell(name)="{ data }">
              <opensilex-UriLink
                :uri="data.item.uri"
                :value="data.item.name"
                :to="{ path: '/provenances/details/' + encodeURIComponent(data.item.uri) }"
                :allowCopy="true"
              />
            </template>

            <template #cell(activity_type)="{ data }">
              {{
                data.item.prov_activity != null && data.item.prov_activity.length > 0
                  ? activityTypes[data.item.prov_activity[0].rdf_type]
                  : null
              }}
            </template>

            <template #cell(activity_start_date)="{ data }">
              <opensilex-DateView
                :value="
                  data.item.prov_activity != null && data.item.prov_activity.length > 0
                    ? data.item.prov_activity[0].start_date
                    : null
                "
              />
            </template>

            <template #cell(activity_end_date)="{ data }">
              <opensilex-DateView
                :value="
                  data.item.prov_activity != null && data.item.prov_activity.length > 0
                    ? data.item.prov_activity[0].end_date
                    : null
                "
              />
            </template>

            <template #cell(actions)="{ data }">
              <n-button-group size="small">
                <opensilex-EditButton
                  v-if="user.hasCredential(credentials.CREDENTIAL_PROVENANCE_MODIFICATION_ID)"
                  @click="emit('onEdit', data.item.uri)"
                  :label="t('ProvenanceList.update')"
                  :small="true"
                />
                <opensilex-DeleteButton
                  v-if="user.hasCredential(credentials.CREDENTIAL_PROVENANCE_DELETE_ID)"
                  @click="deleteProvenance(data.item.uri)"
                  :label="t('ProvenanceList.delete')"
                  :small="true"
                />
              </n-button-group>
            </template>
          </opensilex-TableAsyncView>

          <!-- Formulaire Creation Document -->
          <opensilex-ModalForm
            ref="documentForm"
            component="opensilex-DocumentForm"
            createTitle="component.common.addDocument"
            modalSize="lg"
            :initForm="initForm"
            icon="bi#bi-file-earmark-text"
          />
        </n-layout-content>
      </n-layout>
    </opensilex-PageContent>
  </div>
</template>

<script setup lang="ts">
import { computed, inject, nextTick, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { useStore } from 'vuex'
import {
  NLayout,
  NLayoutSider,
  NLayoutContent,
  NForm,
  NFormItem,
  NInput,
  NButton,
  NDropdown,
  NSpace,
  NButtonGroup
} from 'naive-ui'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import Prov from '../../ontologies/Prov'

const emit = defineEmits<{
  (e: 'onEdit', uri: string): void
  (e: 'onDelete', uri: string): void
  (e: 'select', v: any): void
  (e: 'unselect', v: any): void
  (e: 'selectall', v: any): void
}>()

const props = withDefaults(defineProps<{
  isSelectable?: boolean
  noActions?: boolean
  pageSize?: number
  noUpdateURL?: boolean
  showCount?: boolean
}>(), {
  isSelectable: true,
  noActions: false,
  pageSize: 20,
  noUpdateURL: false,
  showCount: true
})

const { t, n } = useI18n()
const route = useRoute()
const store = useStore()

const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const service = $opensilex.getService<any>('opensilex.DataService')
const ontologyService = $opensilex.getService<any>('opensilex.OntologyService')

const tableRef = ref<any>(null)
const deviceSelector = ref<any>(null)
const documentForm = ref<any>(null)

const filtersCollapsed = ref(true)

const user = computed(() => store.state.user)
const credentials = computed(() => store.state.credentials)
const lang = computed(() => store.getters.language)

const filter = ref({
  name: '',
  activity_type: undefined as string | undefined,
  agent_type: undefined as string | undefined,
  agents: [] as any[],
  operator: undefined as any
})

const fieldKeyToModelFieldMap = {
  activity_type: 'activity.rdfType',
  activity_start_date: 'activity.startDate',
  activity_end_date: 'activity.endDate'
}

const activityTypes = ref<Record<string, string>>({})

onMounted(() => {
  $opensilex.updateFiltersFromURL(route.query, filter.value)
  loadActivityTypes()
})

const activeFiltersCount = computed(() => {
  const values = [
    filter.value.name,
    filter.value.activity_type,
    filter.value.agent_type
  ]

  let count = values.filter(v => v !== undefined && String(v).trim() !== '').length

  if (Array.isArray(filter.value.agents) && filter.value.agents.length > 0) {
    count++
  }

  return count
})

const fields = computed(() => {
  const tableFields: any[] = [
    { key: 'name', label: 'component.common.name', sortable: true },
    { key: 'activity_type', label: t('ProvenanceList.activity_type'), sortable: true },
    { key: 'activity_start_date', label: t('ProvenanceList.activity_start_date'), sortable: true },
    { key: 'activity_end_date', label: t('ProvenanceList.activity_end_date'), sortable: true }
  ]

  if (!props.noActions) {
    tableFields.push({ key: 'actions', label: 'component.common.actions' })
  }

  return tableFields
})

const selectedCount = computed(() => (tableRef.value?.getSelected?.() ?? []).length)

const paginationInfo = computed(() => {
  return tableRef.value?.getPaginationInfo?.() ?? {
    start: 0,
    end: 0,
    total: 0,
    hasResults: false
  }
})

const onlySelected = computed(() => !!tableRef.value?.onlySelected)

function clearAgents() {
  filter.value.agents = []
  filter.value.operator = undefined
}

function resetFilterValues() {
  filter.value.name = ''
  filter.value.activity_type = undefined
  filter.value.agent_type = undefined
  filter.value.agents = []
  filter.value.operator = undefined
}

function applyFilters() {
  tableRef.value?.setPage?.(1)
  if (!props.noUpdateURL) {
    $opensilex.updateURLParameters(filter.value)
  }
  nextTick(() => tableRef.value?.refresh?.())
  filtersCollapsed.value = true
}

function resetFilters() {
  resetFilterValues()
  tableRef.value?.setPage?.(1)
  if (!props.noUpdateURL) {
    $opensilex.updateURLParameters(filter.value)
  }
  nextTick(() => tableRef.value?.refresh?.())
}

function refresh() {
  updateSelectedProvenance()
  tableRef.value?.setPage?.(1)
}

function updateSelectedProvenance() {
  if (!props.noUpdateURL) {
    $opensilex.updateURLParameters(filter.value)
  }
  if (tableRef.value?.onlySelected) {
    tableRef.value.onlySelected = false
  }
  nextTick(() => tableRef.value?.refresh?.())
}

async function loadData(options: any) {
  return await service.searchProvenance(
    filter.value.name,
    undefined, // description
    undefined, // activity
    filter.value.activity_type,
    filter.value.agents,
    filter.value.agent_type,
    options.orderBy,
    options.currentPage,
    options.pageSize
  )
}

function getSelected() {
  return tableRef.value?.getSelected?.() ?? []
}

function initForm() {
  const targetURI: string[] = []

  for (const select of getSelected()) {
    targetURI.push(select.uri)
  }

  return {
    description: {
      uri: undefined,
      identifier: undefined,
      rdf_type: undefined,
      title: undefined,
      date: undefined,
      description: undefined,
      targets: targetURI,
      authors: undefined,
      language: undefined,
      deprecated: undefined,
      keywords: undefined
    },
    file: undefined
  }
}

function deleteProvenance(uri: string) {
  tableRef.value?.checkSelectedItems?.(uri)
  emit('onDelete', uri)
}

function onRefreshed() {
  setTimeout(() => {
    if (
      tableRef.value?.selectAll === true &&
      tableRef.value?.selectedItems?.length !== tableRef.value?.totalRow
    ) {
      tableRef.value.selectAll = false
    }
  }, 1)
}

function loadActivityTypes() {
  ontologyService
    .getSubClassesOf(Prov.ACTIVITY_TYPE_URI, true)
    .then((http: any) => {
      const map: Record<string, string> = {}

      for (const item of http.response.result ?? []) {
        map[item.uri] = item.name
      }

      activityTypes.value = map
    })
    .catch($opensilex.errorHandler)
}

/** dropdown display */
const displayDropdownOptions = computed(() => [
  {
    label: onlySelected.value
      ? t('ProvenanceList.selected-all')
      : t('component.common.selected-only'),
    key: 'onlySelected'
  },
  { type: 'divider', key: 'd1' },
  { label: t('component.common.resetSelected'), key: 'resetSelected' }
])

function onDisplayDropdownSelect(key: string) {
  switch (key) {
    case 'onlySelected':
      tableRef.value?.toggleOnlySelected?.() ?? tableRef.value?.clickOnlySelected?.()
      break
    case 'resetSelected':
      tableRef.value?.resetSelection?.() ?? tableRef.value?.resetSelected?.()
      break
  }
}

/** dropdown actions */
const actionsDropdownOptions = computed(() => [
  { label: t('component.common.addDocument'), key: 'addDocument' }
])

function onActionsDropdownSelect(key: string) {
  if (key === 'addDocument') {
    documentForm.value?.showCreateForm?.(initForm())
  }
}

defineExpose({
  refresh,
  resetFilters,
  getSelected,
  updateSelectedProvenance
})
</script>

<style scoped lang="scss">
// neutralisation des classes injectées par naive dans les <n-form-item> qui créent des espaces indésirés entre les champs
:deep(.compact-form-item) {
  --n-label-height: 0px !important;
  --n-label-padding: 0 !important;
}

.btn-disabled {
  background-color: #e0e0e0 !important;
  color: #2e2e2e !important;
  border: none !important;
  cursor: not-allowed;
}

.provenance-layout {
  background: transparent;
}

.provenance-content {
  padding-left: 12px;
}

.listActionButtons {
  position: relative;
  display: flex;
  gap: 0 !important;
}

.displayAndListSelectionCount {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filtersNotCollapsed {
  margin-left: 55px;
}

.filtersCollapsed {
  margin-left: 415px;
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

.globalFiltersSearchButton div {
  margin-top: 5px;
}
</style>

<i18n>
en:
  ProvenanceList:
    selected: Selected provenances
    update: Update provenance
    delete: Delete provenance
    selected-all: All provenances
    display: Display
    name: Name
    name-placeholder: Enter provenance name
    activity_type-placeholder: Select a type of activity
    label-filter: Search provenances
    activity_type: Activity type
    activity_start_date: Start date
    activity_end_date: End date

fr:
  ProvenanceList:
    selected: Provenances sélectionnées
    update: Modifier la provenance
    delete: Supprimer la provenance
    selected-all: Toutes les provenances
    display: Affichage
    name: Nom
    name-placeholder: Entrer un nom de provenance
    activity_type-placeholder: Selectionner un type d'activité
    label-filter: Rechercher des provenances
    activity_type: Type d'activité
    activity_start_date: Date de début
    activity_end_date: Date de fin

</i18n>