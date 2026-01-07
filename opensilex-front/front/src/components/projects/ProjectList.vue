<template>
  <!-- BARRE ACTIONS LISTE -->
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
        {{ t('ProjectList.display') }}
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
                {{ t('component.common.list.pagination.nbEntries', {
                    limit: paginationInfo.start,
                    offset: paginationInfo.end,
                    totalRow: n(paginationInfo.total)
                }) }}
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
        {{ t('ProjectList.selected') }} :
        <span class="badge badge-pill greenThemeColor">{{ selectedCount }}</span>
      </n-p>
    </div>
  </n-space>

  <!-- LAYOUT -->
  <n-layout has-sider class="project-layout">
    <!-- Bouton loupe -->
    <n-space class="mb-2 me-1" align="top">
      <n-button
        quaternary
        circle
        @click="filtersCollapsed = !filtersCollapsed"
        :title="t('ProjectList.label-filter')"
        :class="{ greenThemeColor: filtersCollapsed }"
        class="globalFiltersSearchButton"
      >
        <i class="bi bi-search filtersGlobalSearchIcon"></i>

        <div v-show="filtersCollapsed && activeFiltersCount > 0" class="filters-count-badge">
          ( {{ activeFiltersCount }} )
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
      class="project-sider"
    >
      <n-space class="p-3" vertical>
        <!-- IMPORTANT : empêcher le submit natif -->
        <n-form label-placement="top" size="small" @submit.prevent.stop="applyFilters">
          <n-form-item :label="t('component.common.name')">
            <n-input
              v-model:value="filter.name"
              clearable
              :placeholder="t('component.project.filter-label-placeholder')"
              @keydown.enter.prevent.stop="applyFilters"
            />
          </n-form-item>

          <n-form-item :label="t('component.common.year')">
            <n-input
              v-model:value="filter.year"
              clearable
              type="number"
              :placeholder="t('component.project.filter-year-placeholder')"
              @keydown.enter.prevent.stop="applyFilters"
            />
          </n-form-item>

          <n-form-item :label="t('component.common.keyword')">
            <n-input
              v-model:value="filter.keyword"
              clearable
              :placeholder="t('component.project.filter-keywords-placeholder')"
              @keydown.enter.prevent.stop="applyFilters"
            />
          </n-form-item>

          <n-form-item :label="t('component.project.financialFunding')">
            <n-input
              v-model:value="filter.financial"
              clearable
              :placeholder="t('component.project.filter-financial-placeholder')"
              @keydown.enter.prevent.stop="applyFilters"
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

    <!-- CONTENU -->
    <n-layout-content class="project-content">
      <opensilex-TableAsyncView
        ref="tableRef"
        :searchMethod="loadData"
        :fields="fields"
        defaultSortBy="start_date"
        :defaultPageSize="pageSize"
        :isSelectable="isSelectable"
        :showHeaderCount="false"
        labelNumberOfSelectedRow="component.project.selectedLabel"
        @select="emit('select', $event)"
        @unselect="emit('unselect', $event)"
        @selectall="emit('selectall', $event)"
      >
        <template #cell(name)="{ data }">
          <opensilex-UriLink
            :uri="data.item.uri"
            :value="data.item.name"
            :to="{ path: '/project/details/' + encodeURIComponent(data.item.uri) }"
          />
        </template>

        <template #cell(start_date)="{ data }">
          <opensilex-DateView :value="data.item.start_date" />
        </template>

        <template #cell(end_date)="{ data }">
          <opensilex-DateView :value="data.item.end_date" />
        </template>

        <template #cell(state)="{ data }">
          <i
            v-if="!isEnded(data.item)"
            class="bi bi-activity badge-icon badge-info-opensilex"
            :title="t('component.project.common.status.in-progress')"
          />
          <i
            v-else
            class="bi bi-archive badge-icon badge-light"
            :title="t('component.project.common.status.finished')"
          />
        </template>

        <template #cell(actions)="{ data }">
          <n-button-group size="small">
            <opensilex-EditButton
              v-if="user.hasCredential(credentials.CREDENTIAL_PROJECT_MODIFICATION_ID)"
              @click="emit('onEdit', data.item)"
              label="component.project.update"
              :small="true"
            />
            <opensilex-DeleteButton
              v-if="user.hasCredential(credentials.CREDENTIAL_PROJECT_DELETE_ID)"
              @click="deleteProject(data.item.uri)"
              label="component.project.delete"
              :small="true"
            />
          </n-button-group>
        </template>
      </opensilex-TableAsyncView>

      <opensilex-ModalForm
        v-if="user.hasCredential(credentials.CREDENTIAL_PROJECT_MODIFICATION_ID)"
        ref="documentForm"
        component="opensilex-DocumentForm"
        createTitle="component.common.addDocument"
        modalSize="lg"
        :initForm="initForm"
        icon="ik#ik-file-text"
      />
    </n-layout-content>
  </n-layout>
</template>

<script setup lang="ts">
import { computed, inject, nextTick, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { useStore } from 'vuex'
import {
  NLayout, NLayoutSider, NLayoutContent,
  NForm, NFormItem, NInput,
  NButton, NDropdown, NSpace, NButtonGroup
} from 'naive-ui'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import { ProjectsService } from 'opensilex-core/index'

const emit = defineEmits<{
  (e: 'onEdit', project: any): void
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
const service = $opensilex.getService<ProjectsService>('opensilex.ProjectsService')

const tableRef = ref<any>(null)
const documentForm = ref<any>(null)

const filtersCollapsed = ref(true)

const user = computed(() => store.state.user)
const credentials = computed(() => store.state.credentials)

/** filtre */
const filter = ref({
  year: '' as any,
  name: '',
  keyword: '',
  financial: ''
})

onMounted(() => {
  // init depuis URL
  $opensilex.updateFiltersFromURL(route.query, filter.value)
})

/** compteur filtres actifs */
const activeFiltersCount = computed(() => {
  return [filter.value.name, filter.value.keyword, filter.value.financial, filter.value.year]
    .filter(v => v !== undefined && String(v).trim() !== '').length
})

/** champs TableAsyncView */
const fields = computed(() => {
  const tableFields: any[] = [
    { key: 'name', label: 'component.common.name', sortable: true },
    { key: 'shortname', label: 'component.project.shortname', sortable: true },
    { key: 'start_date', label: 'component.common.startDate', sortable: true },
    { key: 'end_date', label: 'component.common.endDate', sortable: true },
    { key: 'financial_funding', label: 'component.project.financialFunding', sortable: true },
    { key: 'state', label: 'component.common.state' }
  ]
  if (!props.noActions) tableFields.push({ key: 'actions', label: 'component.common.actions' })
  return tableFields
})

/** sélection / counts */
const selectedCount = computed(() => (tableRef.value?.getSelected?.() ?? []).length)

const paginationInfo = computed(() => {
  return tableRef.value?.getPaginationInfo?.() ?? { start: 0, end: 0, total: 0, hasResults: false }
})


/** actions filtres */
function applyFilters() {
  // 1) page 1
//   tableRef.value?.changeCurrentPage?.(1)
tableRef.value?.setPage?.(1)

  // 2) URL
  if (!props.noUpdateURL) $opensilex.updateURLParameters(filter.value)

  // 3) refresh
  nextTick(() => tableRef.value?.refresh?.())

  // 4) refermer sidebar
  filtersCollapsed.value = true
}

function resetFilters() {
  filter.value.name = ''
  filter.value.year = ''
  filter.value.keyword = ''
  filter.value.financial = ''

//   tableRef.value?.changeCurrentPage?.(1)
tableRef.value?.setPage?.(1)
  if (!props.noUpdateURL) $opensilex.updateURLParameters(filter.value)
  nextTick(() => tableRef.value?.refresh?.())
}

/** refresh externe (ex: parent) */
function refresh() {
//   tableRef.value?.changeCurrentPage?.(1)
  tableRef.value?.setPage?.(1)
  if (!props.noUpdateURL) $opensilex.updateURLParameters(filter.value)
  nextTick(() => tableRef.value?.refresh?.())
  console.log("REFRESH - pagi ", paginationInfo.value)
}

function updateSelectedProject() {
  if (!props.noUpdateURL) $opensilex.updateURLParameters(filter.value)
  nextTick(() => tableRef.value?.refresh?.())
}

/** backend */
async function loadData(options: any) {
  // options.currentPage est 0-based dans TableAsyncView

  const http = await service.searchProjects(
    filter.value.name,
    filter.value.year,
    filter.value.keyword,
    filter.value.financial,
    options.orderBy,
    options.currentPage,
    options.pageSize
  )

  return http
}


function isEnded(project: any) {
  if (project?.end_date) return new Date(project.end_date).getTime() < Date.now()
  return false
}

function deleteProject(uri: string) {
  service.deleteProject(uri)
    .then(() => {
      refresh()
      emit('onDelete', uri)
      $opensilex.showSuccessToast(
        `${t('ProjectList.name')} ${uri} ${t('component.common.success.delete-success-message')}`
      )
    })
    .catch($opensilex.errorHandler)
}

function getSelected() {
  return tableRef.value?.getSelected?.() ?? []
}

function initForm() {
  const targetURI: string[] = []
  for (const select of getSelected()) targetURI.push(select.uri)
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

/** dropdown display */
const onlySelected = computed(() => !!tableRef.value?.onlySelected)

const displayDropdownOptions = computed(() => [
  { label: onlySelected.value ? t('ProjectList.selected-all') : t('component.common.selected-only'), key: 'onlySelected' },
  { type: 'divider', key: 'd1' },
  { label: t('component.common.resetSelected'), key: 'resetSelected' }
])


function onDisplayDropdownSelect(key: string) {
  switch (key) {
    case 'onlySelected':
      tableRef.value?.toggleOnlySelected?.()
      break
    case 'resetSelected':
      tableRef.value?.resetSelection?.()
      break
  }
}

/** dropdown actions */
const actionsDropdownOptions = computed(() => [
  { label: t('component.common.addDocument'), key: 'addDocument' }
])

function onActionsDropdownSelect(key: string) {
  if (key === 'addDocument') documentForm.value?.showCreateForm?.()
}

defineExpose({
  refresh,
  resetFilters,
  getSelected,
  updateSelectedProject
})
</script>

<style>
.btn-disabled {
  background-color: #e0e0e0 !important;
  color: #2e2e2e !important;
  border: none !important;
  cursor: not-allowed;
}

/* .project-layout {
  background: transparent;
}
.project-sider {
  background: #fff;
}
.project-content {
  padding-left: 12px;
} */

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

.filtersNotCollapsed { margin-left: 55px; }
.filtersCollapsed { margin-left: 415px; }

.filtersGlobalSearchIcon { font-size: 1.2em; }

.globalFiltersSearchButton {
  width: 40px;
  height: 55px;
}
.globalFiltersSearchButton span { display: block !important; }
.globalFiltersSearchButton div { margin-top: 5px; }
</style>

<i18n>
en:
  ProjectList:
    name: The project
    selected: Selected Project(s)
    selected-all: All projects
    display: Display
    label-filter: Search projects
fr:
  ProjectList:
    name: Le projet
    selected: Projet(s) sélectionné(s)
    selected-all: Tous les projets
    display: Affichage
    label-filter: Rechercher des projets
</i18n>
