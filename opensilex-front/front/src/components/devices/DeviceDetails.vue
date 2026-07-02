<template>
  <div class="container-fluid py-3">
    <opensilex-PageHeader
      icon="bi#bi-thermometer-half"
      :title="device.name || ''"
      :description="t('DeviceDetails.title')"
      class="detail-element-header"
      :hasIcon="true"
    />

    <opensilex-PageActions :returnButton="true">
      <nav class="tabs mb-3">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          :class="['tab', { active: currentTab === tab.key }]"
          @click="goToTab(tab.key)"
          type="button"
        >
          {{ tab.label }}

          <span
            v-if="tab.key === 'datafiles' && !datafilesCountIsLoading && datafiles > 0"
            class="tabBadge"
          >
            {{ formatCount(datafiles) }}
          </span>

          <span
            v-if="tab.key === 'events' && !eventsCountIsLoading && events > 0"
            class="tabBadge"
          >
            {{ formatCount(events) }}
          </span>

          <span
            v-if="tab.key === 'positions' && !positionsCountIsLoading && positions > 0"
            class="tabBadge"
          >
            {{ formatCount(positions) }}
          </span>

          <span
            v-if="tab.key === 'annotations' && !annotationsCountIsLoading && annotations > 0"
            class="tabBadge"
          >
            {{ formatCount(annotations) }}
          </span>

          <span
            v-if="tab.key === 'documents' && !documentsCountIsLoading && documents > 0"
            class="tabBadge"
          >
            {{ formatCount(documents) }}
          </span>
        </button>
      </nav>
    </opensilex-PageActions>

    <opensilex-PageContent>
      <component
        v-if="currentTabComponent"
        :is="currentTabComponent"
        :ref="currentTabRef"
        v-bind="currentTabProps"
        v-on="currentTabListeners"
      />
    </opensilex-PageContent>
  </div>
</template>

<script setup lang="ts">
import { computed, defineAsyncComponent, inject, onMounted, ref, watch, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { useStore } from 'vuex'

import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import type HttpResponse from '@/lib/HttpResponse'
import type { OpenSilexResponse } from '@/lib/HttpResponse'

import type { DeviceGetDTO, DevicesService } from 'opensilex-core/index'
import type { EventsService } from 'opensilex-core/api/events.service'
import type { AnnotationsService } from 'opensilex-core/api/annotations.service'
import type { DocumentsService } from 'opensilex-core/api/documents.service'
import type { PositionsService } from 'opensilex-core/api/positions.service'
import type { DataService } from 'opensilex-core/api/data.service'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const store = useStore()
const opensilex = inject<OpenSilexVuePlugin>('$opensilex')!

const credentials = computed(() => store.state.credentials)
const lang = computed(() => store.getters.language)

const uri = ref<string>('')

const device = ref<DeviceGetDTO>({
  uri: null as any,
  rdf_type: null as any,
  name: null as any,
  brand: null as any,
  constructor_model: null as any,
  serial_number: null as any,
  person_in_charge: null as any,
  start_up: null as any,
  removal: null as any,
  relations: null as any,
  description: null as any
})

const events = ref<number>(0)
const annotations = ref<number>(0)
const documents = ref<number>(0)
const positions = ref<number>(0)
const datafiles = ref<number>(0)

const eventsCountIsLoading = ref(true)
const annotationsCountIsLoading = ref(true)
const documentsCountIsLoading = ref(true)
const positionsCountIsLoading = ref(true)
const datafilesCountIsLoading = ref(true)

// services
const devicesService = opensilex.getService<DevicesService>('opensilex.DevicesService')
const eventsService = opensilex.getService<EventsService>('opensilex.EventsService')
const annotationsService = opensilex.getService<AnnotationsService>('opensilex.AnnotationsService')
const documentsService = opensilex.getService<DocumentsService>('opensilex.DocumentsService')
const positionsService = opensilex.getService<PositionsService>('opensilex.PositionsService')
const dataService = opensilex.getService<DataService>('opensilex.DataService')

// tabs
const tabDefinitions = [
  {
    key: 'details',
    labelKey: 'DeviceDetails.description',
    component: () => import('./DeviceDescription.vue'),
    refKey: 'deviceDescription'
  },
  {
    key: 'visualization',
    labelKey: 'DeviceDetails.visualization',
    // component: () => import('./DeviceVisualizationTab.vue'),
     component: () => import('./DeviceDescription.vue'),
    refKey: 'deviceVisualizationTab'
  },
  {
    key: 'datafiles',
    labelKey: 'DeviceDetails.datafiles',
    component: () => import('./DeviceDataFiles.vue'),
    refKey: 'deviceDataFiles'
  },
  {
    key: 'events',
    labelKey: 'DeviceDetails.events',
    component: () => import('../events/list/EventList.vue'),
    refKey: 'eventList'
  },
  {
    key: 'positions',
    labelKey: 'DeviceDetails.positions',
    component: () => import('../positions/list/PositionList.vue'),
    refKey: 'positionList'
  },
  {
    key: 'annotations',
    labelKey: 'DeviceDetails.annotations',
    component: () => import('../annotations/list/AnnotationList.vue'),
    refKey: 'annotationList'
  },
  {
    key: 'documents',
    labelKey: 'DeviceDetails.documents',
    component: () => import('../documents/DocumentTabList.vue'),
    refKey: 'documentTabList'
  }
] as const

type TabKey = typeof tabDefinitions[number]['key']

const tabs = computed(() =>
  tabDefinitions.map(({ key, labelKey }) => ({
    key,
    label: t(labelKey)
  }))
)

const tabComponents = Object.fromEntries(
  tabDefinitions.map(tab => [tab.key, defineAsyncComponent(tab.component)])
) as Record<TabKey, any>

const formRefs: Record<string, Ref<any>> = {}
tabDefinitions.forEach(tab => {
  formRefs[tab.refKey] = ref(null)
})

const currentTabComponent = computed(() => tabComponents[currentTab.value])

const currentTabRef = computed(() => {
  const def = tabDefinitions.find(tab => tab.key === currentTab.value)
  return def ? formRefs[def.refKey] : undefined
})

function getTabFromPath(): TabKey {
  const path = route.path

  if (path.startsWith('/device/visualization/')) return 'visualization'
  if (path.startsWith('/device/datafiles/')) return 'datafiles'
  if (path.startsWith('/device/events/')) return 'events'
  if (path.startsWith('/device/positions/')) return 'positions'
  if (path.startsWith('/device/annotations/')) return 'annotations'
  if (path.startsWith('/device/documents/')) return 'documents'
  return 'details'
}

const currentTab = ref<TabKey>(getTabFromPath())

watch(
  () => route.path,
  () => {
    currentTab.value = getTabFromPath()
  }
)

function goToTab(key: TabKey) {
  if (!uri.value) return

  const base =
    key === 'details'
      ? '/device/details/'
      : key === 'visualization'
        ? '/device/visualization/'
        : key === 'datafiles'
          ? '/device/datafiles/'
          : key === 'events'
            ? '/device/events/'
            : key === 'positions'
              ? '/device/positions/'
              : key === 'annotations'
                ? '/device/annotations/'
                : '/device/documents/'

  router.replace({ path: base + encodeURIComponent(uri.value) })
}

const currentTabProps = computed(() => {
  if (!uri.value) return {}

  if (currentTab.value === 'details') {
    return {
      key: lang.value
    }
  }

  if (currentTab.value === 'visualization') {
    return {
      device: uri.value,
      elementName: device.value.name
    }
  }

  if (currentTab.value === 'datafiles') {
    return {
      device: uri.value
    }
  }

  if (currentTab.value === 'documents') {
    return {
      uri: uri.value,
      modificationCredentialId: credentials.value.CREDENTIAL_DOCUMENT_MODIFICATION_ID
    }
  }

  if (currentTab.value === 'annotations') {
    return {
      target: uri.value,
      displayTargetColumn: false,
      enableActions: true,
      modificationCredentialId: credentials.value.CREDENTIAL_ANNOTATION_MODIFICATION_ID,
      deleteCredentialId: credentials.value.CREDENTIAL_ANNOTATION_DELETE_ID
    }
  }

  if (currentTab.value === 'events') {
    return {
      target: uri.value,
      columnsToDisplay: new Set(['type', 'start', 'end', 'description']),
      modificationCredentialId: credentials.value.CREDENTIAL_EVENT_MODIFICATION_ID,
      deleteCredentialId: credentials.value.CREDENTIAL_EVENT_DELETE_ID,
      displayTargetFilter: false
    }
  }

  if (currentTab.value === 'positions') {
    return {
      target: uri.value,
      modificationCredentialId: credentials.value.CREDENTIAL_EVENT_MODIFICATION_ID,
      deleteCredentialId: credentials.value.CREDENTIAL_EVENT_DELETE_ID
    }
  }

  return {}
})

const currentTabListeners = computed(() => {
  if (currentTab.value === 'annotations') {
    return { changed: searchAnnotations }
  }
  if (currentTab.value === 'documents') {
    return { changed: searchDocuments }
  }
  if (currentTab.value === 'events') {
    return { changed: searchEvents }
  }
  if (currentTab.value === 'positions') {
    return { changed: searchPositions }
  }
  if (currentTab.value === 'datafiles') {
    return { changed: searchDatafiles }
  }
  return {}
})

function syncUriFromRoute() {
  const raw = route.params.uri
  const param = Array.isArray(raw) ? raw[0] : (raw as string | undefined)
  uri.value = param ? decodeURIComponent(param) : ''
}

function formatCount(value: number) {
  const nf = (opensilex as any).$numberFormatter
  return nf?.formateResponse ? nf.formateResponse(value) : String(value)
}

async function loadDevice() {
  if (!uri.value) return

  try {
    const http: HttpResponse<OpenSilexResponse<DeviceGetDTO>> =
      await devicesService.getDevice(uri.value)

    device.value = http.response.result
  } catch (error) {
    opensilex.errorHandler(error)
  }
}

async function searchEvents() {
  if (!uri.value) return

  eventsCountIsLoading.value = true
  try {
    const http: HttpResponse<OpenSilexResponse<number>> =
      await eventsService.countEvents([uri.value], undefined, undefined)

    events.value = http.response.result ?? 0
  } catch (error) {
    opensilex.errorHandler(error)
  } finally {
    eventsCountIsLoading.value = false
  }
}

async function searchAnnotations() {
  if (!uri.value) return

  annotationsCountIsLoading.value = true
  try {
    const http: HttpResponse<OpenSilexResponse<number>> =
      await annotationsService.countAnnotations(uri.value, undefined, undefined)

    annotations.value = http.response.result ?? 0
  } catch (error) {
    opensilex.errorHandler(error)
  } finally {
    annotationsCountIsLoading.value = false
  }
}

async function searchDocuments() {
  if (!uri.value) return

  documentsCountIsLoading.value = true
  try {
    const http: HttpResponse<OpenSilexResponse<number>> =
      await documentsService.countDocuments(uri.value, undefined, undefined)

    documents.value = http.response.result ?? 0
  } catch (error) {
    opensilex.errorHandler(error)
  } finally {
    documentsCountIsLoading.value = false
  }
}

async function searchPositions() {
  if (!uri.value) return

  positionsCountIsLoading.value = true
  try {
    const http: HttpResponse<OpenSilexResponse<number>> =
      await positionsService.countMoves(uri.value, undefined, undefined)

    positions.value = http.response.result ?? 0
  } catch (error) {
    opensilex.errorHandler(error)
  } finally {
    positionsCountIsLoading.value = false
  }
}

async function searchDatafiles() {
  if (!uri.value) return

  datafilesCountIsLoading.value = true
  try {
    const http: HttpResponse<OpenSilexResponse<number>> =
      await dataService.countDatafiles(undefined, [uri.value])

    datafiles.value = http.response.result ?? 0
  } catch (error) {
    opensilex.errorHandler(error)
  } finally {
    datafilesCountIsLoading.value = false
  }
}

async function refreshAll() {
  await Promise.all([
    loadDevice(),
    searchEvents(),
    searchAnnotations(),
    searchDocuments(),
    searchPositions(),
    searchDatafiles()
  ])
}

onMounted(() => {
  syncUriFromRoute()
  const routeParts = route.path.split('/')
  localStorage.setItem('tabPath', routeParts[2] ?? 'details')
  localStorage.setItem('tabPage', '1')
  refreshAll()
})

watch(
  () => route.params.uri,
  () => {
    syncUriFromRoute()
    refreshAll()
  }
)

watch(
  () => route.path,
  (newPath) => {
    const routeParts = newPath.split('/')
    localStorage.setItem('tabPath', routeParts[2] ?? 'details')
  }
)
</script>

<style scoped lang="scss">

// autre disposition d'onglets possible :
// .tabs {
//   gap: 0.5rem;
// }

// .tab {
//   padding: 0.5rem 0.75rem;
// }

// .tab.active {
//   border-bottom-color: var(--primary-color, #00a38d);
// }

</style>

<i18n>
en:
  DeviceDetails:
    title: Device
    description: Description
    details: Details
    annotation: Annotation
    annotations: Annotations
    documents: Documents
    visualization: Visualization
    datafiles: Datafiles
    events: Events
    positions: Positions

fr:
  DeviceDetails:
    title: Appareil
    description: Description
    details: Détails
    annotation: Annotation
    annotations: Annotations
    documents: Documents
    visualization: Visualisation
    datafiles: Fichiers de données
    events: Événements
    positions: Positions
</i18n>