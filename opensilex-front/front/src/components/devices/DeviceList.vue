<template>
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
        {{ t('DeviceList.display') }}
      </n-button>
    </n-dropdown>

    <!-- Dropdown Actions -->
    <n-dropdown
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
      <span> | </span>
      <span>
        {{ t('DeviceList.selected') }} :
        <span class="badge badge-pill greenThemeColor">{{ selectedCount }}</span>
      </span>
    </div>
  </n-space>

  <!-- Layout -->
  <n-layout has-sider class="device-layout">
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

    <!-- Sidebar / Filtres -->
    <n-layout-sider
      v-model:collapsed="filtersCollapsed"
      :collapsed-width="0"
      :width="360"
      collapse-mode="width"
      show-trigger
      bordered
      class="device-sider"
    >
      <n-space class="p-3" vertical>
        <n-form label-placement="top" size="small" @submit.prevent.stop="refresh">
          <!-- Name -->
          <n-form-item :label="t('DeviceList.filter.namePattern')"  class="compact-form-item">
            <opensilex-StringFilter
              v-model:filter="filter.name"
              :placeholder="t('DeviceList.filter.namePattern-placeholder')"
              class="searchFilter"
              @handlingEnterKey="refresh"
            />
          </n-form-item>

          <!-- Type -->
          <n-form-item  class="compact-form-item">
            <opensilex-TypeForm
              v-model:type="filter.rdf_type"
              :baseType="$opensilex.Oeso.DEVICE_TYPE_URI"
              :placeholder="t('DeviceList.filter.rdfTypes-placeholder')"
              class="searchFilter"
              @handlingEnterKey="refresh"
            />
          </n-form-item>

          <!-- Variables -->
          <n-form-item :label="t('DeviceList.filter.variable')">
            <opensilex-VariableSelectorWithFilter
              v-model:variables="filter.variable"
              :placeholder="t('DeviceList.filter.variable-placeholder')"
              maximumSelectedRows="1"
              class="searchFilter"
            />
          </n-form-item>

          <!-- Start up -->
          <n-form-item :label="t('DeviceList.filter.start_up')" class="compact-form-item">
            <opensilex-StringFilter
              v-model:filter="filter.start_up"
              :placeholder="t('DeviceList.filter.start_up-placeholder')"
              type="number"
              class="searchFilter"
              @handlingEnterKey="refresh"
            />
          </n-form-item>

          <!-- Facility -->
          <n-form-item  class="compact-form-item">
            <opensilex-FormSelector
              :label="t('DeviceList.filter.facility')"
              :placeholder="t('DeviceList.filter.facility-placeholder')"
              :multiple="false"
              v-model:selected="filter.facility"
              :options="facilities"
              class="searchFilter"
              @handlingEnterKey="refresh"
            />
          </n-form-item>

          <!-- Brand -->
          <n-form-item :label="t('DeviceList.filter.brand')"  class="compact-form-item">
            <opensilex-StringFilter
              v-model:filter="filter.brand"
              :placeholder="t('DeviceList.filter.brand-placeholder')"
              class="searchFilter"
              @handlingEnterKey="refresh"
            />
          </n-form-item>

          <!-- Model -->
          <n-form-item :label="t('DeviceList.filter.model')"  class="compact-form-item">
            <opensilex-StringFilter
              v-model:filter="filter.model"
              :placeholder="t('DeviceList.filter.model-placeholder')"
              class="searchFilter"
              @handlingEnterKey="refresh"
            />
          </n-form-item>

          <!-- Metadata key -->
          <n-form-item :label="t('DeviceList.filter.metadataKey')"  class="compact-form-item">
            <opensilex-StringFilter
              v-model:filter="filter.metadataKey"
              :placeholder="t('DeviceList.filter.metadataKey-placeholder')"
              class="searchFilter"
              @handlingEnterKey="refresh"
            />
          </n-form-item>

          <!-- Metadata value -->
          <n-form-item :label="t('DeviceList.filter.metadataValue')"  class="compact-form-item">
            <opensilex-StringFilter
              v-model:filter="filter.metadataValue"
              :placeholder="t('DeviceList.filter.metadataValue-placeholder')"
              class="searchFilter"
              @handlingEnterKey="refresh"
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

    <!-- Contenu Liste -->
    <n-layout-content class="device-content">
      <opensilex-TableAsyncView
        ref="tableRef"
        :searchMethod="searchDevices"
        :fields="fields"
        defaultSortBy="name"
        :isSelectable="true"
        :showHeaderCount="false"
        labelNumberOfSelectedRow="DeviceList.selected"
        iconNumberOfSelectedRow="ik#ik-thermometer"
        @refreshed="onRefreshed"
      >
        <template #cell(name)="{ data }">
          <opensilex-UriLink
            :uri="data.item.uri"
            :value="data.item.name"
            :to="{ path: '/device/details/' + encodeURIComponent(data.item.uri) }"
          />
        </template>

        <template #cell(actions)="{ data }">
          <n-button-group size="small">
            <opensilex-EditButton
              v-if="user.hasCredential(credentials.CREDENTIAL_DEVICE_MODIFICATION_ID)"
              @click="editDevice(data.item.uri)"
              label="DeviceList.update"
              :small="true"
            />
            <opensilex-DeleteButton
              v-if="user.hasCredential(credentials.CREDENTIAL_DEVICE_DELETE_ID)"
              label="DeviceList.delete"
              :small="true"
              @click="deleteDevice(data.item.uri)"
            />
          </n-button-group>
        </template>
      </opensilex-TableAsyncView>

      <opensilex-ModalForm
        v-if="user.hasCredential(credentials.CREDENTIAL_DOCUMENT_MODIFICATION_ID)"
        ref="documentFormRef"
        component="opensilex-DocumentForm"
        createTitle="component.common.addDocument"
        modalSize="lg"
        :initForm="initForm"
        icon="ik#ik-file-text"
      />

      <opensilex-DeviceModalForm
        ref="deviceFormRef"
        @onUpdate="updateSelectedDevice"
      />

      <opensilex-VariableModalList
        v-if="showVariableForm"
        ref="variableSelectionRef"
        label="label"
        :required="true"
        :multiple="true"
        @onValidate="editDeviceVar"
      />

      <opensilex-EventCsvForm
        v-if="showEventForm && user.hasCredential(credentials.CREDENTIAL_EVENT_MODIFICATION_ID)"
        ref="eventCsvFormRef"
        :targets="selectedUris"
      />

      <opensilex-EventCsvForm
        v-if="showMoveForm && user.hasCredential(credentials.CREDENTIAL_EVENT_MODIFICATION_ID)"
        ref="moveCsvFormRef"
        :targets="selectedUris"
        :isMove="true"
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
  NLayout,
  NLayoutSider,
  NLayoutContent,
  NForm,
  NFormItem,
  NButton,
  NDropdown,
  NSpace,
  NButtonGroup,
  NP
} from 'naive-ui'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import {
  DevicesService,
  type DeviceGetDetailsDTO,
  type FacilityGetDTO
} from 'opensilex-core/index'
import { OrganizationsService } from 'opensilex-core/api/organizations.service'

const emit = defineEmits<{
  (e: 'onDelete', uri: string): void
  (e: 'onUpdate', form: any): void
}>()

const { t, n } = useI18n()
const route = useRoute()
const store = useStore()

const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')
const deviceService = $opensilex.getService<DevicesService>('opensilex.DevicesService')
const organizationService = $opensilex.getService<OrganizationsService>('opensilex.OrganizationsService')

const tableRef = ref<any>(null)
const documentFormRef = ref<any>(null)
const deviceFormRef = ref<any>(null)
const variableSelectionRef = ref<any>(null)
const eventCsvFormRef = ref<any>(null)
const moveCsvFormRef = ref<any>(null)

const selectedUris = ref<string[]>([])
const filtersCollapsed = ref(true)

const showVariableForm = ref(false)
const showEventForm = ref(false)
const showMoveForm = ref(false)

const facilities = ref<Array<{ id: string; label: string }>>([])

const user = computed(() => store.state.user)
const credentials = computed(() => store.state.credentials)
const lang = computed(() => store.getters.language)

const filter = ref({
  name: undefined as string | undefined,
  rdf_type: undefined as string | undefined,
  variable: undefined as any,
  start_up: undefined as string | undefined,
  existence_date: undefined as string | undefined,
  facility: undefined as string | undefined,
  brand: undefined as string | undefined,
  model: undefined as string | undefined,
  metadataKey: undefined as string | undefined,
  metadataValue: undefined as string | undefined
})

const fields = [
  {
    key: 'name',
    label: t('DeviceList.name'),
    sortable: true
  },
  {
    key: 'rdf_type_name',
    label: t('DeviceList.rdfTypes'),
    sortable: true
  },
  {
    key: 'start_up',
    label: t('DeviceList.start_up'),
    sortable: true
  },
  {
    key: 'actions',
    label: 'component.common.actions'
  }
]

onMounted(() => {
  loadFacilities()
  $opensilex.updateFiltersFromURL(route.query, filter.value)
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

const activeFiltersCount = computed(() => {
  return [
    filter.value.name,
    filter.value.rdf_type,
    filter.value.variable,
    filter.value.start_up,
    filter.value.existence_date,
    filter.value.facility,
    filter.value.brand,
    filter.value.model,
    filter.value.metadataKey,
    filter.value.metadataValue
  ].filter(v => {
    if (Array.isArray(v)) return v.length > 0
    return v !== undefined && v !== null && String(v).trim() !== ''
  }).length
})

const onlySelected = computed(() => !!tableRef.value?.onlySelected)

const searchFiltersPanel = computed(() => t('searchfilter.label'))

const displayDropdownOptions = computed(() => [
  {
    label: onlySelected.value
      ? t('DeviceList.selected-all')
      : t('component.common.selected-only'),
    key: 'onlySelected'
  },
  { type: 'divider', key: 'd1' },
  { label: t('component.common.resetSelected'), key: 'resetSelected' }
])

const actionsDropdownOptions = computed(() => {
  const options: any[] = []

  if (user.value.hasCredential(credentials.value.CREDENTIAL_DOCUMENT_MODIFICATION_ID)) {
    options.push({
      label: t('component.common.addDocument'),
      key: 'addDocument'
    })
  }

  options.push({
    label: t('DeviceList.export'),
    key: 'exportDevices'
  })

  if (user.value.hasCredential(credentials.value.CREDENTIAL_DEVICE_MODIFICATION_ID)) {
    options.push({
      label: t('DeviceList.linkVariable'),
      key: 'linkVariable'
    })
  }

  if (options.length > 0) {
    options.push({ type: 'divider', key: 'd2' })
  }

  if (user.value.hasCredential(credentials.value.CREDENTIAL_EVENT_MODIFICATION_ID)) {
    options.push({
      label: t('DeviceList.add-multiple'),
      key: 'createEvents'
    })
    options.push({
      label: t('DeviceList.add-Move'),
      key: 'createMoves'
    })
  }

  return options
})

function resetFilters() {
  filter.value = {
    name: undefined,
    rdf_type: undefined,
    variable: undefined,
    start_up: undefined,
    existence_date: undefined,
    facility: undefined,
    brand: undefined,
    model: undefined,
    metadataKey: undefined,
    metadataValue: undefined
  }
}

function reset() {
  resetFilters()
  refresh()
}

function refresh() {
  updateSelectedDevice()
  tableRef.value?.setPage?.(1)
  nextTick(() => tableRef.value?.refresh?.())
  filtersCollapsed.value = true
}

function updateSelectedDevice() {
  $opensilex.updateURLParameters(filter.value)
  if (tableRef.value?.onlySelected) {
    tableRef.value.onlySelected = false
  }
}

function editDevice(uri: string) {
  deviceFormRef.value?.showEditForm?.(uri)
}

function addMetadataFilter() {
  if (
    filter.value.metadataKey &&
    filter.value.metadataKey !== '' &&
    filter.value.metadataValue &&
    filter.value.metadataValue !== ''
  ) {
    return JSON.stringify({
      [filter.value.metadataKey]: filter.value.metadataValue
    })
  }
  return undefined
}

async function searchDevices(options: any) {
  return await deviceService.searchDevices(
    filter.value.rdf_type,
    true,
    filter.value.name,
    filter.value.variable,
    filter.value.start_up,
    filter.value.existence_date,
    filter.value.facility,
    filter.value.brand,
    filter.value.model,
    undefined,
    addMetadataFilter(),
    options.orderBy,
    options.currentPage,
    options.pageSize
  )
}

function deleteDevice(uri: string) {
  deviceService
    .deleteDevice(uri)
    .then(() => {
      tableRef.value?.checkSelectedItems?.(uri)
      refresh()
      emit('onDelete', uri)

      const message =
        `${t('DeviceList.device')} ${uri} ${t('component.common.success.delete-success-message')}`

      $opensilex.showSuccessToast(message)
    })
    .catch((error: any) => {
      if (error?.response?.result?.title === 'LINKED_DEVICE_ERROR') {
        const message = `${t('DeviceList.associated-device-error')} ${error.response.result.message}`
        $opensilex.showErrorToast(message)
      } else {
        $opensilex.errorHandler(error)
      }
    })
}

function getSelected() {
  return tableRef.value?.getSelected?.() ?? []
}

function updateSelectedUris() {
  selectedUris.value = getSelected().map((select: any) => select.uri)
}

function initForm() {
  const targetURI = getSelected().map((select: any) => select.uri)

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

function createDocument() {
  documentFormRef.value?.showCreateForm?.()
}

function exportDevices() {
  const path = '/core/devices/export_by_uris'
  const today = new Date()
  const filename =
    'export_devices_' +
    today.getFullYear() +
    String(today.getMonth() + 1).padStart(2, '0') +
    String(today.getDate()).padStart(2, '0')

  const exportList = getSelected().map((select: any) => select.uri)

  $opensilex.downloadFilefromPostService(
    path,
    filename,
    'csv',
    { uris: exportList },
    lang.value
  )
}

function linkVariable() {
  const deniedType = [
    'vocabulary:RadiometricTarget',
    'vocabulary:Station',
    'vocabulary:ControlLaw'
  ]

  const invalid = getSelected().some((select: any) =>
    deniedType.includes(select.rdf_type)
  )

  if (invalid) {
    alert(t('DeviceList.alertBadDeviceType'))
    return
  }

  showVariableForm.value = true
  nextTick(() => {
    variableSelectionRef.value?.show?.()
  })
}

function editDeviceVar(variableSelected: any[]) {
  for (const select of getSelected()) {
    deviceService
      .getDevice(select.uri)
      .then((http: any) => {
        const varList = variableSelected.map((variable: any) => ({
          property: 'vocabulary:measures',
          value: variable.uri
        }))

        const device: DeviceGetDetailsDTO = http.response.result
        const form = JSON.parse(JSON.stringify(device))
        form.relations = form.relations.concat(varList)

        updateVariable(form)
      })
      .catch($opensilex.errorHandler)
  }
}

function updateVariable(form: any) {
  deviceService
    .updateDevice(form)
    .then((http: any) => {
      const uri = http.response.result
      console.debug('device updated', uri)
      emit('onUpdate', form)
    })
    .catch($opensilex.errorHandler)
}

function createEvents() {
  showEventForm.value = true
  nextTick(() => {
    updateSelectedUris()
    eventCsvFormRef.value?.show?.()
  })
}

function createMoves() {
  showMoveForm.value = true
  nextTick(() => {
    updateSelectedUris()
    moveCsvFormRef.value?.show?.()
  })
}

async function loadFacilities() {
  organizationService
    .getAllFacilities()
    .then((http: any) => {
      facilities.value = http.response.result.map((facility: FacilityGetDTO) => ({
        id: facility.uri!,
        label: facility.name!
      }))
    })
    .catch($opensilex.errorHandler)
}

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

function onActionsDropdownSelect(key: string) {
  switch (key) {
    case 'addDocument':
      createDocument()
      break
    case 'exportDevices':
      exportDevices()
      break
    case 'linkVariable':
      linkVariable()
      break
    case 'createEvents':
      createEvents()
      break
    case 'createMoves':
      createMoves()
      break
  }
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

defineExpose({
  refresh,
  reset,
  resetFilters,
  getSelected,
  updateSelectedDevice
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

.device-layout {
  background: transparent;
}

.device-content {
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
  DeviceList:
    uri: URI
    name: Name
    rdfTypes: Device Type
    variable: Variable
    start_up: Start up
    update: Update Device
    delete: Delete Device
    display: Display
    selected: Selected devices
    facility: Facility
    linkVariable: Link variables
    device: The device
    export: Export Device list
    alertSelectSize: The selection has too many lines, 1000 lines maximum
    addEvent: Add event
    add-multiple: Add events
    addAnnotation: Add annotation
    addMove: Move
    showMap: Show in a map
    alertBadDeviceType: The selected type doesn't match with add variable
    associated-device-error: Device is associated with
    selected-all: All Devices
    add-Move: Move

    filter:
      namePattern: Name
      namePattern-placeholder: Enter name
      rdfTypes: Type
      rdfTypes-placeholder: Select a device type
      variable: Variable
      variable-placeholder: Select a variable
      start_up: Start up
      start_up-placeholder: Enter year
      facility: Facility
      facility-placeholder: Select a facility
      brand: Brand
      brand-placeholder: Enter brand
      model: Constructor model
      model-placeholder: Enter constructor model
      metadataValue: Value
      metadataKey: Key
      metadataValue-placeholder: Enter value
      metadataKey-placeholder: Enter key

fr:
  DeviceList:
    uri: URI
    name: Nom
    rdfTypes: Type de l'appareil
    variable: Variable
    start_up: Date d'obtention
    update: Editer l'appareil
    delete: Supprimer l'appareil'
    display: Affichage
    selected: Appareils selectionnés
    facility: Installation technique
    linkVariable: Lier des variables
    device: L'appareil
    export: Exporter la liste
    alertSelectSize: La selection contient trop de ligne, 1000 lignes maximum
    addEvent: Ajouter un évènement
    addAnnotation: Ajouter une annotation
    addMove: Déplacement
    showMap: Afficher sur une carte
    alertBadDeviceType: La selection comporte un type incompatible avec l'ajout de variable
    associated-device-error: L'appareil est associé à
    selected-all: Tout les appareils
    add-multiple: Ajouter des événements
    add-Move: Déplacer

    filter:
      namePattern: Nom
      namePattern-placeholder: Entrer un nom
      rdfTypes: Type
      rdfTypes-placeholder: Sélectionner un type d'appareil
      variable: Variable
      variable-placeholder: Sélectionner une variable
      start_up: Date d'obtention
      start_up-placeholder: Entrer une année
      facility: Installation environnementale
      facility-placeholder: Sélectionner une installation
      brand: Marque
      brand-placeholder: Entrer une marque
      model: Modèle constructeur
      model-placeholder: Entrer le nom du modèle constructeur
      metadataValue: Valeur
      metadataKey: Attribut
      metadataValue-placeholder: Entrer une valeur
      metadataKey-placeholder: Entrer un attribut
</i18n>