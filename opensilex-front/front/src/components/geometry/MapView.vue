<template>
  <div class="map-view-container">
    <MapRenderer
      :experiment-uri="experimentUri"
      :scientific-objects="mockScientificObjects"
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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, inject } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'

import MapRenderer from './MapRenderer.vue'
import MapToolbar from './MapToolbar.vue'
import MapLegend from './MapLegend.vue'
import SelectedFeaturesTable from './SelectedFeaturesTable.vue'
import MapPanelDrawer from './MapPanelDrawer.vue'
import EventPanelDrawer from './EventPanelDrawer.vue'
import MapExportModal from './MapExportModal.vue'
import ChartModal from './ChartModal.vue'
import MapEditor from './MapEditor.vue'

// Inject OpenSilex service for experiment loading
const opensilex = inject<Record<string, unknown>>('$opensilex')

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
const selectedFeatures = ref<Feature[]>([])

// Mock data for testing - clearly labeled as MOCK DATA
// Scientific Objects (10+ items with various types and coordinates around Paris)
const mockScientificObjects: Feature[] = [
  {
    properties: { uri: 'urn:experiment:so1', name: 'Wheat Plot A1', type: 'Plot', nature: 'Structural' },
    geometry: { type: 'Point', coordinates: [2.3522, 48.8566] },
  },
  {
    properties: { uri: 'urn:experiment:so2', name: 'Wheat Plot A2', type: 'Plot', nature: 'Structural' },
    geometry: { type: 'Point', coordinates: [2.3532, 48.8576] },
  },
  {
    properties: { uri: 'urn:experiment:so3', name: 'Corn Plot B1', type: 'Plot', nature: 'Structural' },
    geometry: { type: 'Point', coordinates: [2.3542, 48.8586] },
  },
  {
    properties: { uri: 'urn:experiment:so4', name: 'Barley Plot B2', type: 'Plot', nature: 'Structural' },
    geometry: { type: 'Point', coordinates: [2.3552, 48.8596] },
  },
  {
    properties: { uri: 'urn:experiment:so5', name: 'Soybean Plot C1', type: 'Plot', nature: 'Structural' },
    geometry: { type: 'Point', coordinates: [2.3562, 48.8606] },
  },
  {
    properties: { uri: 'urn:experiment:so6', name: 'Rice Plot C2', type: 'Plot', nature: 'Structural' },
    geometry: { type: 'Point', coordinates: [2.3572, 48.8616] },
  },
  {
    properties: { uri: 'urn:experiment:so7', name: 'Sunflower Plot D1', type: 'Plot', nature: 'Structural' },
    geometry: { type: 'Point', coordinates: [2.3582, 48.8626] },
  },
  {
    properties: { uri: 'urn:experiment:so8', name: 'Rapeseed Plot D2', type: 'Plot', nature: 'Structural' },
    geometry: { type: 'Point', coordinates: [2.3592, 48.8636] },
  },
  {
    properties: { uri: 'urn:experiment:so9', name: 'Potato Plot E1', type: 'Plot', nature: 'Structural' },
    geometry: { type: 'Point', coordinates: [2.3602, 48.8646] },
  },
  {
    properties: { uri: 'urn:experiment:so10', name: 'Beet Plot E2', type: 'Plot', nature: 'Structural' },
    geometry: { type: 'Point', coordinates: [2.3612, 48.8656] },
  },
  {
    properties: { uri: 'urn:experiment:so11', name: 'Tomato Greenhouse', type: 'Greenhouse', nature: 'Structural' },
    geometry: { type: 'Point', coordinates: [2.3512, 48.8556] },
  },
  {
    properties: { uri: 'urn:experiment:so12', name: 'Lettuce Tray F1', type: 'Sample', nature: 'Temporal' },
    geometry: { type: 'Point', coordinates: [2.3527, 48.8571] },
  },
]

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
  // Show details
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
</style>

<i18n>
en:
  MapView:
    mapPanelTitle: "Map Panel"
    eventPanelTitle: "Events"
fr:
  MapView:
    mapPanelTitle: "Panneau Carte"
    eventPanelTitle: "Événements"
</i18n>