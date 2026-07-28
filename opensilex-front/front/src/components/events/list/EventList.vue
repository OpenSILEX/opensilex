<template>
  <!-- Barre Actions -->
  <n-space
    class="listActionButtons"
    :class="[filtersCollapsed ? 'filtersNotCollapsed' : 'filtersCollapsed']"
  >
    <n-button
      v-if="user.hasCredential(modificationCredentialId)"
      size="small"
      class="greenThemeColor"
      @click="showForm"
    >
      {{ t('EventList.add') }}
    </n-button>

    <n-button
      v-if="user.hasCredential(modificationCredentialId)"
      size="small"
      class="greenThemeColor"
      @click="showCsvForm"
    >
      {{ t('EventList.import') }}
    </n-button>

    <n-button
      v-if="user.hasCredential(modificationCredentialId)"
      size="small"
      class="greenThemeColor"
      @click="showMoveCsvForm"
    >
      {{ t('EventList.move-csv-import-title') }}
    </n-button>


    <div class="displayAndListSelectionCount">
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
  </n-space>

  <!-- Layout -->
  <n-layout has-sider class="event-layout">
    <!-- Bouton loupe -->
    <n-space class="mb-2 me-1" align="top">
      <n-button
        quaternary
        circle
        @click="filtersCollapsed = !filtersCollapsed"
        :title="searchFiltersPanel"
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

    <!-- Sidebar filtres -->
    <n-layout-sider
      v-model:collapsed="filtersCollapsed"
      :collapsed-width="0"
      :width="360"
      collapse-mode="width"
      show-trigger
      bordered
      class="event-sider"
    >
      <n-space class="p-3" vertical>
        <n-form label-placement="top" size="small" @submit.prevent.stop="refresh">
          <n-form-item class="compact-form-item">
            <opensilex-TypeForm
              v-model:type="filter.type"
              :baseType="baseType"
              :ignoreRoot="false"
              :placeholder="t('EventList.type-placeholder')"
              class="searchFilter"
              @handlingEnterKey="refresh"
            />
          </n-form-item>

          <n-form-item
            v-if="displayTargetFilter"
            :label="t('EventList.targets')"
            class="compact-form-item"
          >
            <opensilex-StringFilter
              v-model:filter="filter.target"
              :placeholder="t('EventList.target-filter-placeholder')"
              class="searchFilter"
              @handlingEnterKey="refresh"
            />
          </n-form-item>

          <n-form-item
            :label="t('component.common.description')"
            class="compact-form-item"
          >
            <opensilex-StringFilter
              v-model:filter="filter.description"
              :placeholder="t('EventList.filter-label-placeholder')"
              class="searchFilter"
              @handlingEnterKey="refresh"
            />
          </n-form-item>

          <n-form-item :label="t('EventList.start')" class="compact-form-item">
            <opensilex-DateTimeForm
              v-model:value="filter.start"
              :max-date="filter.end ? filter.end : undefined"
              :required="false"
              class="searchFilter"
            />
          </n-form-item>

          <n-form-item :label="t('EventList.end')" class="compact-form-item">
            <opensilex-DateTimeForm
              v-model:value="filter.end"
              :min-date="filter.start ? filter.start : undefined"
              :minDate="filter.start"
              :maxDate="filter.end"
              :required="false"
              class="searchFilter"
            />
          </n-form-item>

          <n-space justify="end" class="mt-2">
            <opensilex-Button
              class="resetButton"
              :label="t('component.common.search.clear-button')"
              icon="bi-x-lg"
              @click="reset"
            />
            <opensilex-Button
              class="greenThemeColor"
              :label="t('component.common.search.search-button')"
              icon="bi-search"
              @click="refresh"
            />
          </n-space>
        </n-form>
      </n-space>
    </n-layout-sider>

    <!-- Contenu -->
    <n-layout-content class="event-content">
      <opensilex-TableAsyncView
        v-if="renderComponent"
        ref="tableRef"
        :searchMethod="search"
        :fields="fields"
        defaultSortBy=""
        :isSelectable="isSelectable"
        :showHeaderCount="false"
        @refreshed="onRefreshed"
        labelNumberOfSelectedRow="EventList.selected"
        iconNumberOfSelectedRow="bi#bi-layers"
      >
        <template #cell(rdf_type_name)="{ data }">
          <opensilex-UriLink
            v-if="data.item.rdf_type_name"
            :uri="$opensilex.getShortUri(data.item.rdf_type)"
            :value="data.item.rdf_type_name"
          />
        </template>

        <template #cell(start)="{ data }">
          <opensilex-TextView
            v-if="data.item.start && data.item.start.length > 0"
            :value="new Date(data.item.start).toLocaleString()"
          />
        </template>

        <template #cell(end)="{ data }">
          <opensilex-TextView
            v-if="data.item.end"
            :value="new Date(data.item.end).toLocaleString()"
          />
        </template>

        <template #cell(targets)="{ data }">
          <span
            v-for="(uri, index) in data.item.targets"
            :key="index"
          >
            <template v-if="index < 2">
              <opensilex-UriLink
                :uri="uri"
                :value="objectsLabels[uri]"
                :to="{
                  path: $opensilex.getTargetPath(uri, context, objectsPath[uri])
                }"
              />
              <span v-if="index < Math.min((data.item.targets?.length || 0), 2) - 1"> </span>
            </template>
            <template v-else-if="index === 2">
              ...
            </template>
          </span>
        </template>

        <template #cell(description)="{ data }">
          <opensilex-TextView :value="data.item.description" />
        </template>

        <template #cell(actions)="{ data }">
          <n-button-group size="small">
            <opensilex-DetailButton
              v-if="user.hasCredential(modificationCredentialId)"
              @click="showEventView(data.item)"
              label="component.events.details"
              :small="true"
            />
            <opensilex-EditButton
              v-if="user.hasCredential(modificationCredentialId)"
              @click="editEvent(data.item)"
              label="component.common.list.buttons.update"
              :small="true"
            />
            <opensilex-DeleteButton
              v-if="user.hasCredential(deleteCredentialId)"
              @click="deleteEvent(data.item.uri)"
              label="component.common.list.buttons.delete"
              :small="true"
            />
          </n-button-group>
        </template>
      </opensilex-TableAsyncView>

      <opensilex-EventModalView
        ref="eventModalViewRef"
        modalSize="lg"
        v-model:dto="selectedEvent"
        v-model:type="selectedEvent.rdf_type"
        :uriLabels="uriLabels"
        :uriPaths="uriPaths"
        :specificPropertiesLabels="specificPropertiesLabels"
        :specificPropertiesPaths="specificPropertiesPaths"
        :positionsUriLabels="positionsUriLabels"
        :positionsUriPaths="positionsUriPaths"
      />

      <opensilex-EventModalForm
        v-if="renderModalForm"
        ref="modalFormRef"
        :target="target"
        :context="context"
        @onCreate="displayAfterCreation"
        @onUpdate="updateSelectedEvent"
      />

      <opensilex-EventCsvForm
        v-if="renderCsvForm"
        ref="csvFormRef"
        :targets="[target]"
        @csvImported="onImport"
      />

      <opensilex-EventCsvForm
        v-if="renderMoveCsvForm"
        ref="moveCsvFormRef"
        :targets="[target]"
        :isMove="true"
        @csvImported="onImport"
      />

      <opensilex-ModalForm
        v-if="user.hasCredential(credentials.CREDENTIAL_DOCUMENT_MODIFICATION_ID)"
        ref="documentFormRef"
        component="opensilex-DocumentForm"
        createTitle="component.common.addDocument"
        modalSize="lg"
        :initForm="initForm"
        icon="bi#bi-file-text"
      />
    </n-layout-content>
  </n-layout>
</template>

<script setup lang="ts">
import { computed, inject, nextTick, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { useStore } from 'vuex'
import {
  NLayout,
  NLayoutSider,
  NLayoutContent,
  NForm,
  NFormItem,
  NButton,
  NDropdown,
  NSpace,
  NButtonGroup
} from 'naive-ui'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import type HttpResponse from '@/lib/HttpResponse'
import type { OpenSilexResponse } from '@/lib/HttpResponse'
import type { EventDetailsDTO } from 'opensilex-core/index'
import { EventsService } from 'opensilex-core/api/events.service'
import { OntologyService } from 'opensilex-core/api/ontology.service'
import {EventGetDTO} from "opensilex-core/model/eventGetDTO";
import {RowWithData} from "@/components/common/views/TableAsyncView.vue";

type EventFilter = {
  target: string | undefined
  type: string | undefined
  description: string | undefined
  start: string | undefined
  end: string | undefined
}

const props = withDefaults(defineProps<{
  isSelectable?: boolean
  modificationCredentialId?: string
  deleteCredentialId?: string
  enableActions?: boolean
  columnsToDisplay?: Set<string>
  maxPageSize?: number
  displayTargetFilter?: boolean
  displayTitle?: boolean
  maximizeFilterSize?: boolean
  target?: string
  context?: string
}>(), {
  isSelectable: false,
  enableActions: true,
  maxPageSize: 10,
  displayTargetFilter: true,
  displayTitle: false,
  maximizeFilterSize: false,
  columnsToDisplay: () => new Set(['type', 'start', 'end', 'targets', 'description'])
})

const emit = defineEmits<{
  (e: 'onDelete', uri: string): void
}>()

const { t, n } = useI18n()
const route = useRoute()
const store = useStore()

const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const eventService = $opensilex.getService<EventsService>('opensilex.EventsService')
const ontologyService = $opensilex.getService<OntologyService>('opensilex.OntologyService')

const tableRef = ref<any>(null)
const eventModalViewRef = ref<any>(null)
const modalFormRef = ref<any>(null)
const csvFormRef = ref<any>(null)
const moveCsvFormRef = ref<any>(null)
const documentFormRef = ref<any>(null)

const uriLabels = ref<Record<string, string>>({})
const uriPaths = ref<Record<string, string>>({})
const specificPropertiesLabels = ref<Record<string, string>>({})
const specificPropertiesPaths = ref<Record<string, string>>({})
const positionsUriLabels = ref<Record<string, string>>({})
const positionsUriPaths = ref<Record<string, string>>({})

const objectsPath = ref<Record<string, string>>({})
const objectsLabels = ref<Record<string, string>>({})

const filtersCollapsed = ref(true)
const renderComponent = ref(true)
const renderModalForm = ref(false)
const renderCsvForm = ref(false)
const renderMoveCsvForm = ref(false)

const selectedEvent = ref<EventDetailsDTO>({})
const baseType = ref<string>($opensilex.Oeev.EVENT_TYPE_URI)

const filter = ref<EventFilter>({
  target: undefined,
  type: undefined,
  description: undefined,
  start: undefined,
  end: undefined
})

const user = computed(() => store.state.user)
const credentials = computed(() => store.state.credentials)

const paginationInfo = computed(() => {
  return tableRef.value?.getPaginationInfo?.() ?? {
    start: 0,
    end: 0,
    total: 0,
    hasResults: false
  }
})

/**
* Depuis l'onglet d'un autre element, exemple un device, la target est le device par défaut, vérouillé,
* Donc on veut éviter d'afficher "(1)" par défaut dans le badge pour ce cas la
*/
const activeFiltersCount = computed(() => {
  const targetCount =
    props.displayTargetFilter &&
    filter.value.target &&
    filter.value.target !== '' &&
    filter.value.target !== props.target
      ? 1
      : 0

  const otherFiltersCount = [
    filter.value.type,
    filter.value.description,
    filter.value.start,
    filter.value.end
  ].filter(v => v !== undefined && v !== null && String(v).trim() !== '').length

  return targetCount + otherFiltersCount
})

const searchFiltersPanel = computed(() => t('searchfilter.label'))

const actionsDropdownOptions = computed(() => {
  const options: any[] = []

  if (user.value.hasCredential(credentials.value.CREDENTIAL_DOCUMENT_MODIFICATION_ID)) {
    options.push({
      label: t('component.common.addDocument'),
      key: 'addDocument'
    })
  }

  return options
})

const fields = computed(() => {
  const tableFields: any[] = []

  if (props.columnsToDisplay.has('type')) {
    tableFields.push({
      key: 'rdf_type_name',
      label: 'component.common.type',
      sortable: true
    })
  }
  if (props.columnsToDisplay.has('start')) {
    tableFields.push({
      key: 'start',
      label: t('EventList.start'),
      sortable: true
    })
  }
  if (props.columnsToDisplay.has('end')) {
    tableFields.push({
      key: 'end',
      label: t('EventList.end'),
      sortable: true
    })
  }
  if (props.columnsToDisplay.has('targets')) {
    tableFields.push({
      key: 'targets',
      label: t('EventList.targets'),
      sortable: true
    })
  }
  if (props.columnsToDisplay.has('description')) {
    tableFields.push({
      key: 'description',
      label: t('EventList.description'),
      sortable: true
    })
  }
  if (props.enableActions) {
    tableFields.push({
      key: 'actions',
      label: 'component.common.actions'
    })
  }

  return tableFields
})

onMounted(() => {
  $opensilex.updateFiltersFromURL(route.query, filter.value)
  if (props.target) {
    filter.value.target = props.target
  }
})

watch(
  () => props.target,
  () => {
    renderComponent.value = false
    nextTick(() => {
      if (props.target) {
        filter.value.target = props.target
      }
      renderComponent.value = true
    })
  }
)

function resetFilters() {
  filter.value = {
    target: props.target,
    type: undefined,
    description: undefined,
    start: undefined,
    end: undefined
  }
}

function reset() {
  resetFilters()
  refresh()
}

function cleanFilter() {
  if (props.target) {
    filter.value.target = props.target
  }
  if (filter.value.target === '') {
    filter.value.target = undefined
  }
  if (filter.value.start === '') {
    filter.value.start = undefined
  }
  if (filter.value.end === '') {
    filter.value.end = undefined
  }
  if (filter.value.description === '') {
    filter.value.description = undefined
  }
}

function refresh() {
  cleanFilter()
  $opensilex.updateURLParameters(filter.value)
  tableRef.value?.setPage?.(1)
  nextTick(() => tableRef.value?.refresh?.())
  filtersCollapsed.value = true
}

function updateSelectedEvent() {
  $opensilex.updateURLParameters(filter.value)
  nextTick(() => tableRef.value?.refresh?.())
}

function showForm() {
  renderModalForm.value = true
  nextTick(() => {
    modalFormRef.value?.showCreateForm?.()
  })
}

function showCsvForm() {
  renderCsvForm.value = true
  nextTick(() => {
    csvFormRef.value?.show?.()
  })
}

function showMoveCsvForm() {
  renderMoveCsvForm.value = true
  nextTick(() => {
    moveCsvFormRef.value?.show?.()
  })
}

function displayAfterCreation(event: any) {
  refresh()
  showEventView(event)
}

async function search(options: any) {
  cleanFilter()

  const http = await eventService.searchEvents(
    filter.value.type,
    filter.value.start,
    filter.value.end,
    filter.value.target,
    filter.value.description,
    options.orderBy,
    options.currentPage,
    options.pageSize
  )

  const targetUris = Array.from(
    new Set(
      (http.response.result ?? []).flatMap((event: EventDetailsDTO) => event.targets ?? [])
    )
  )

  if (targetUris.length > 0) {
    ontologyService.getURITypes(targetUris).then((httpObj: any) => {
      for (const obj of httpObj.response.result) {
        objectsPath.value[obj.uri] = $opensilex.getPathFromUriTypes(obj.rdf_types)
      }
    })

    $opensilex.loadOntologyLabelsWithType(
      targetUris,
      props.context,
      objectsLabels.value,
      ontologyService
    )
  }

  return http
}

function deleteEvent(uri: string) {
  eventService.deleteEvent(uri)
    .then(() => {
      refresh()
      const message =
        `${t('Event.name')} ${uri} ${t('component.common.success.delete-success-message')}`
      $opensilex.showSuccessToast(message)
      emit('onDelete', uri)
    })
    .catch($opensilex.errorHandler)
}

function isMove(event: any) {
  if (!event || !event.rdf_type) {
    return false
  }

  return $opensilex.Oeev.checkURIs(
    event.rdf_type,
    $opensilex.Oeev.MOVE_TYPE_URI
  )
}

function getEventPromise(event: EventGetDTO): Promise<HttpResponse<OpenSilexResponse>> {
  if (isMove(event)) {
    // return eventService.getEventDetails(event.uri)
    return eventService.getMoveEvent(event.uri)
  }
  return eventService.getEventDetails(event.uri)
}

async function showEventView(event: RowWithData<EventGetDTO>) {
  const http = await getEventPromise(event.item)
  await eventModalViewRef.value?.show?.(http)
}

function editEvent(item: any) {
  renderModalForm.value = true

  nextTick(() => {
    modalFormRef.value?.showEditForm?.(
      item.uri,
      item.rdf_type,
      item
    )
  })
}

function onImport() {
  refresh()
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

function getSelected() {
  return tableRef.value?.getSelected?.() ?? []
}

function createDocument() {
  documentFormRef.value?.showCreateForm?.(initForm())
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

function onActionsDropdownSelect(key: string) {
  switch (key) {
    case 'addDocument':
      createDocument()
      break
  }
}

defineExpose({
  refresh,
  reset,
  resetFilters,
  getSelected
})
</script>

<style scoped lang="scss">
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

.event-layout {
  background: transparent;
}

.event-content {
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

.createButton {
  margin-top: 10px;
}
</style>

<i18n>
en:
  EventList:
    add: Add an event
    end: End
    filter-label-placeholder: Enter a name
    description: Description of the event
    import: CSV Import
    move-csv-import-title: "Move CSV import"
    selected: Selected events
    start: Begin
    target-filter-placeholder: Enter part or all of an URI
    targets: Targets
    type-placeholder: Select an event type
fr:
  EventList:
    add: Ajouter un événement
    description: "Description de l'événement"
    end: Fin
    filter-label-placeholder: Saisir un nom
    import: Import CSV
    move-csv-import-title: "Import CSV des déplacements"
    selected: Évènements selectionnés
    start: Début
    target-filter-placeholder: Saisir une partie ou la totalité d'un URI
    targets: Concerne
    type-placeholder: Selectionner un type d'événement
</i18n>