<template>
  <div class="map-view-container">
    <MapRenderer
      :experiment-uri="experimentUri"
      :scientific-objects="scientificObjects"
      :areas="mockAreas"
      :devices="mockDevices"
      @select="onFeatureSelected"
      @layerVisibilityChange="onLayerVisibilityChange"
    />

    <MapToolbar
      @toggle-map-panel="onToggleMapPanel"
      @toggle-event-panel="onToggleEventPanel"
      @center-map="onCenterMap"
      @print-map="onPrintMap"
      @export-map="onExportMap"
      @show-chart="onShowChart"
      @toggle-date-range="onToggleDateRange"
    />

    <MapLegend />

    <SelectedFeaturesTable
      v-if="selectedFeatures.length > 0"
      :features="selectedFeatures"
      :experiment-uri="experimentUri"
      @delete="onDeleteFeature"
      @edit="onEditFeature"
      @details="onShowDetails"
    />

    <NDrawer
      v-model:show="mapPanelVisible"
      placement="left"
      :width="320"
      class="map-panel-drawer"
    >
      <NDrawerContent :title="t('MapView.mapPanelTitle')" closable>
        <MapPanelDrawer
          :experiment-uri="experimentUri"
          @visibility-change="onLayerVisibilityChange"
        />
      </NDrawerContent>
    </NDrawer>

    <NDrawer
      v-model:show="eventPanelVisible"
      placement="right"
      :width="400"
      class="event-panel-drawer"
    >
      <NDrawerContent :title="t('MapView.eventPanelTitle')" closable>
        <EventPanelDrawer
          :experiment-uri="experimentUri"
          @select-feature="onSelectFeatureFromTimeline"
        />
      </NDrawerContent>
    </NDrawer>

    <MapExportModal
      v-model:show="exportModalVisible"
      :experiment-uri="experimentUri"
      @export="onExport"
    />

    <ChartModal
      v-model:show="chartModalVisible"
      :selected-features="selectedFeatures"
      :experiment-uri="experimentUri"
    />

    <MapEditor
      v-model:editing-mode="editingMode"
      :experiment-uri="experimentUri"
      @area-created="onAreaCreated"
      @area-updated="onAreaUpdated"
    />

    <NDrawer
      v-model:show="detailsDrawerVisible"
      placement="right"
      :width="360"
      class="details-drawer"
    >
      <NDrawerContent :title="t('MapView.featureDetailsTitle')" closable>
        <FeatureDetailsDrawer
          :feature="selectedFeatureForDetails"
          @close="onCloseDetails"
        />
      </NDrawerContent>
    </NDrawer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, inject } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ScientificObjectsService } from 'opensilex-core'
import type { ScientificObjectNodeDTO } from 'opensilex-core'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'

import MapRenderer from './MapRenderer.vue'
import MapToolbar from './MapToolbar.vue'
import MapLegend from './MapLegend.vue'
import SelectedFeaturesTable from './SelectedFeaturesTable.vue'
import MapPanelDrawer from './MapPanelDrawer.vue'
import EventPanelDrawer from './EventPanelDrawer.vue'
import MapExportModal from './MapExportModal.vue'
import ChartModal from './ChartModal.vue'
import MapEditor from './MapEditor.vue'
import FeatureDetailsDrawer from './FeatureDetailsDrawer.vue'

// Inject OpenSilex service for experiment loading
const opensilex = inject<OpenSilexVuePlugin>('$opensilex')!

//#region Public
const route = useRoute()
const { t } = useI18n()

interface FeatureProperties {
  uri?: string
  name?: string
  type?: string
  nature?: string
  [key: string]: unknown
}

interface Feature {
  properties: FeatureProperties
  geometry?: {
    coordinates?: unknown
    type?: string
  }
}

const emit = defineEmits<{
  featureSelected: [feature: Feature]
  layerVisibilityChange: [layer: string, visible: boolean]
}>()

//#endregion

//#region Private
//#region Plugins and services
const experimentUri = ref<string>('')
//#endregion

//#region Template refs
//#endregion

//#region Data and computed
const mapPanelVisible = ref<boolean>(false)
const eventPanelVisible = ref<boolean>(false)
const exportModalVisible = ref<boolean>(false)
const chartModalVisible = ref<boolean>(false)
const editingMode = ref<boolean>(false)
const detailsDrawerVisible = ref<boolean>(false)
const selectedFeatures = ref<Feature[]>([])
const selectedFeatureForDetails = ref<Feature | null>(null)
const scientificObjects = ref<Feature[]>([])

function transformScientificObjectToFeature(so: ScientificObjectNodeDTO): Feature | null {
  const feature = so.location?.geojson ?? so.geometry
  const geometry = feature?.geometry
  if (!geometry) {
    return null
  }

  return {
    properties: {
      uri: so.uri,
      name: so.name,
      type: so.rdf_type,
      nature: so.location?.geojson ? 'Structural' : 'Temporal',
      creation_date: so.creation_date,
      destruction_date: so.destruction_date,
      rdf_type_name: so.rdf_type_name,
    },
    geometry: {
      type: geometry.type,
      coordinates: geometry.coordinates,
    },
  }
}

async function loadScientificObjects() {
  try {
    const service = opensilex!.getService<ScientificObjectsService>('opensilex.ScientificObjectsService')
    const response = await service.searchScientificObjectsWithGeometryListByUris(experimentUri.value)
    const data = response?.response?.result ?? []

    scientificObjects.value = data
      .map(transformScientificObjectToFeature)
      .filter((feature): feature is Feature => feature !== null)
  }
  catch (error) {
    opensilex!.errorHandler(error)
  }
}

// Mock Areas (3+ items: structural and temporal)
const mockAreas: Feature[] = [
  {
    properties: { uri: 'urn:experiment:area1', name: 'Field North', type: 'structural', nature: 'Structural' },
    geometry: {
      type: 'Polygon',
      coordinates: [[
        [2.3500, 48.8550], [2.3550, 48.8550], [2.3550, 48.8600], [2.3500, 48.8600], [2.3500, 48.8550]
      ]],
    },
  },
  {
    properties: { uri: 'urn:experiment:area2', name: 'Field South', type: 'structural', nature: 'Structural' },
    geometry: {
      type: 'Polygon',
      coordinates: [[
        [2.3550, 48.8550], [2.3600, 48.8550], [2.3600, 48.8600], [2.3550, 48.8600], [2.3550, 48.8550]
      ]],
    },
  },
  {
    properties: { uri: 'urn:experiment:area3', name: 'Experimental Zone', type: 'temporal', nature: 'Temporal' },
    geometry: {
      type: 'Polygon',
      coordinates: [[
        [2.3520, 48.8570], [2.3580, 48.8570], [2.3580, 48.8630], [2.3520, 48.8630], [2.3520, 48.8570]
      ]],
    },
  },
  {
    properties: { uri: 'urn:experiment:area4', name: 'Buffer Zone', type: 'structural', nature: 'Structural' },
    geometry: {
      type: 'Polygon',
      coordinates: [[
        [2.3480, 48.8530], [2.3620, 48.8530], [2.3620, 48.8650], [2.3480, 48.8650], [2.3480, 48.8530]
      ]],
    },
  },
]

// Mock Devices (5+ items)
const mockDevices: Feature[] = [
  {
    properties: { uri: 'urn:experiment:dev1', name: 'Weather Station WS1', type: 'WeatherStation', nature: 'Device' },
    geometry: { type: 'Point', coordinates: [2.3530, 48.8570] },
  },
  {
    properties: { uri: 'urn:experiment:dev2', name: 'Soil Sensor SS1', type: 'SoilSensor', nature: 'Device' },
    geometry: { type: 'Point', coordinates: [2.3540, 48.8580] },
  },
  {
    properties: { uri: 'urn:experiment:dev3', name: 'Drone Base DB1', type: 'DroneBase', nature: 'Device' },
    geometry: { type: 'Point', coordinates: [2.3560, 48.8590] },
  },
  {
    properties: { uri: 'urn:experiment:dev4', name: 'Irrigation Control IC1', type: 'Irrigation', nature: 'Device' },
    geometry: { type: 'Point', coordinates: [2.3570, 48.8600] },
  },
  {
    properties: { uri: 'urn:experiment:dev5', name: 'Camera CAM1', type: 'Camera', nature: 'Device' },
    geometry: { type: 'Point', coordinates: [2.3580, 48.8610] },
  },
  {
    properties: { uri: 'urn:experiment:dev6', name: 'Spectrometer SP1', type: 'Spectrometer', nature: 'Device' },
    geometry: { type: 'Point', coordinates: [2.3590, 48.8620] },
  },
  {
    properties: { uri: 'urn:experiment:dev7', name: 'Rain Gauge RG1', type: 'RainGauge', nature: 'Device' },
    geometry: { type: 'Point', coordinates: [2.3600, 48.8630] },
  },
]
//#endregion

//#region Hooks
onMounted(() => {
  experimentUri.value = decodeURIComponent(route.params.uri as string)
  loadScientificObjects()
})
//#endregion

//#region Event handlers
function onFeatureSelected(feature: Feature) {
  selectedFeatures.value.push(feature)
  emit('featureSelected', feature)
}

function onLayerVisibilityChange(layer: string, visible: boolean) {
  emit('layerVisibilityChange', layer, visible)
}

function onToggleMapPanel() {
  mapPanelVisible.value = !mapPanelVisible.value
}

function onToggleEventPanel() {
  eventPanelVisible.value = !eventPanelVisible.value
}

function onCenterMap() {
  // Center map on features
}

function onPrintMap() {
  exportModalVisible.value = true
}

function onExportMap() {
  // Export all visible features
}

function onShowChart() {
  chartModalVisible.value = true
}

function onToggleDateRange() {
  // Toggle date range picker
}

function onDeleteFeature(feature: Feature) {
  const index = selectedFeatures.value.findIndex(f => f.properties?.uri === feature.properties?.uri)
  if (index !== -1) {
    selectedFeatures.value.splice(index, 1)
  }
}

function onEditFeature(feature: Feature) {
  // Open edit form
}

function onShowDetails(feature: Feature) {
  selectedFeatureForDetails.value = feature
  detailsDrawerVisible.value = true
}

function onCloseDetails() {
  detailsDrawerVisible.value = false
  selectedFeatureForDetails.value = null
}

function onSelectFeatureFromTimeline(uri: string) {
  // Select feature from timeline
}

function onAreaCreated(area: Feature) {
  // Handle area creation
}

function onAreaUpdated(area: Feature) {
  // Handle area update
}

function onExport(format: string) {
  // Handle export
}
//#endregion

//#endregion
</script>

<style scoped lang="scss">
.map-view-container {
  position: relative;
  width: 100%;
  height: 100%;
}

.map-panel-drawer {
  :deep(.n-drawer-header) {
    background-color: #00a38d;
    color: white;
  }
}

.event-panel-drawer {
  :deep(.n-drawer-header) {
    background-color: #00a38d;
    color: white;
  }
}

.details-drawer {
  :deep(.n-drawer-header) {
    background-color: #00a38d;
    color: white;
  }
}
</style>

<i18n>
en:
  MapView:
    mapPanelTitle: "Map Panel"
    eventPanelTitle: "Events"
    featureDetailsTitle: "Feature Details"
fr:
  MapView:
    mapPanelTitle: "Panneau Carte"
    eventPanelTitle: "Événements"
    featureDetailsTitle: "Détails de la Fonctionnalité"
</i18n>