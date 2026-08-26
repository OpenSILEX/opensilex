<template>
  <div class="map-renderer-container">
    <OlMap
      ref="olMapRef"
      :experiment-uri="experimentUri"
      @map-created="onMapCreated"
    />

    <OlLayers
      :experiment-uri="experimentUri"
      :map-instance="mapInstance"
      :scientific-objects="props.scientificObjects"
      :areas="props.areas"
      :devices="props.devices"
      @layer-ready="onLayersReady"
    />

    <OlOverlays
      :map-instance="mapInstance"
      :selected-features="selectedFeatures"
      @feature-hover="onFeatureHover"
      @feature-click="onFeatureClick"
    />

    <OlInteractions
      ref="olInteractionsRef"
      :map-instance="mapInstance"
      :editing-mode="editingMode"
      @select="onFeatureSelect"
      @draw-end="onDrawEnd"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useTemplateRef } from 'vue'
import OlMap from './OlMap.vue'
import OlLayers from './OlLayers.vue'
import OlOverlays from './OlOverlays.vue'
import OlInteractions from './OlInteractions.vue'

//#region Public
const props = withDefaults(
  defineProps<{
    experimentUri: string
    scientificObjects: Feature[]
    areas: Feature[]
    devices: Feature[]
  }>(),
  {
    scientificObjects: () => [],
    areas: () => [],
    devices: () => [],
  }
)

interface Feature {
  properties: {
    uri?: string
    name?: string
    type?: string
    nature?: string
    [key: string]: unknown
  }
  geometry?: {
    coordinates?: unknown
    type?: string
  }
}

const emit = defineEmits<{
  select: [feature: Feature]
  layerVisibilityChange: [layer: string, visible: boolean]
}>()

//#endregion

//#region Private
//#region Template refs
const olMapRef = useTemplateRef<InstanceType<typeof OlMap>>('olMap')
const olInteractionsRef = useTemplateRef<InstanceType<typeof OlInteractions>>('olInteractions')
//#endregion

//#region Data and computed
const mapInstance = ref<unknown>(null)
const selectedFeatures = ref<Feature[]>([])
const editingMode = ref<boolean>(false)
const layersReady = ref<boolean>(false)
//#endregion

//#region Hooks
function onMapCreated(instance: unknown) {
  mapInstance.value = instance
}

function onLayersReady() {
  layersReady.value = true
}

function onFeatureHover(feature: Feature) {
  // Handle feature hover
}

function onFeatureClick(feature: Feature) {
  // Handle feature click
}

function onFeatureSelect(feature: Feature) {
  emit('select', feature)
}

function onDrawEnd(feature: Feature) {
  // Handle drawing end
}
//#endregion

//#endregion

defineExpose({
  mapInstance,
  selectedFeatures,
  editingMode,
})
</script>

<style scoped lang="scss">
.map-renderer-container {
  position: relative;
  width: 100%;
  height: 700px;
}
</style>