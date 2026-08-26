<template>
  <div class="area-layer"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import Feature from 'ol/Feature'
import { Vector as VectorSource } from 'ol/source'
import { Vector as VectorLayer } from 'ol/layer'
import { Style, Fill, Stroke } from 'ol/style'
import GeoJSON from 'ol/format/GeoJSON'
import { fromLonLat } from 'ol/proj'

//#region Public
const props = withDefaults(
  defineProps<{
    experimentUri: string
    mapInstance: unknown
    features: Feature[]
  }>(),
  {
    features: () => [],
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
  layerReady: []
}>()

//#endregion

//#region Private
//#region Template refs
//#endregion

//#region Data and computed
const areaLayer = ref<VectorLayer | null>(null)
const vectorSource = ref<VectorSource | null>(null)
const areasByType = ref<Map<string, Feature[]>>(new Map())
//#endregion

//#region Hooks
onMounted(() => {
  // Defer initialization until mapInstance is available
})

watch(
  () => props.mapInstance,
  (newMapInstance) => {
    if (newMapInstance && !areaLayer.value) {
      initializeLayer()
      if (props.features.length > 0 && vectorSource.value) {
        addFeatures(props.features)
      }
    }
  },
  { immediate: true }
)

watch(
  () => props.features,
  (newFeatures) => {
    if (newFeatures.length > 0 && vectorSource.value) {
      addFeatures(newFeatures)
    }
  }
)
//#endregion

//#region methods
/**
 * Initialize the Area layer with different styles for structural and temporal areas.
 */
function initializeLayer() {
  if (!props.mapInstance) {
    return
  }

  const source = new VectorSource()
  vectorSource.value = source

  const layer = new VectorLayer({
    source: source,
    style: makeAreaStyleFunc(),
    zIndex: 0,
  })

  areaLayer.value = layer

  const mapInstanceTyped = props.mapInstance as { getLayers: () => import('ol/Collection').default<import('ol/layer/Layer').default> }
  const mapLayers = mapInstanceTyped.getLayers()
  mapLayers.push(layer)

  emit('layerReady')
}

/**
 * Create area style function based on type (structural=green, temporal=red).
 */
function makeAreaStyleFunc() {
  return (feature: import('ol/Feature').default) => {
    const type = feature.get('type') || 'unknown'
    const isStructural = type === 'structural'

    return new Style({
      fill: new Fill({
        color: isStructural ? 'rgba(76, 175, 80, 0.3)' : 'rgba(244, 67, 54, 0.3)',
      }),
      stroke: new Stroke({
        color: isStructural ? '#4CAF50' : '#F44336',
        width: 2,
      }),
    })
  }
}

/**
 * Add area features to the layer.
 */
function addFeatures(features: Feature[]) {
  if (!vectorSource.value) {
    return
  }

  const geoJSONFormat = new GeoJSON()

  features.forEach((feature) => {
    if (!feature.geometry?.coordinates) return

    const olGeometry = geoJSONFormat.readGeometry(feature.geometry, {
      dataProjection: 'EPSG:4326',
      featureProjection: 'EPSG:3857',
    })

    const olFeature = new Feature({
      geometry: olGeometry,
      properties: feature.properties,
    })

    vectorSource.value.addFeature(olFeature)

    const type = feature.properties?.type || 'unknown'
    if (!areasByType.value.has(type)) {
      areasByType.value.set(type, [])
    }
    areasByType.value.get(type)?.push(feature)
  })
}

/**
 * Clear all area features.
 */
function clearFeatures() {
  if (vectorSource.value) {
    vectorSource.value.clear()
  }
}

/**
 * Update visibility.
 */
function setVisibility(visible: boolean) {
  if (areaLayer.value) {
    areaLayer.value.setVisible(visible)
  }
}

/**
 * Get features by type.
 */
function getFeaturesByType(type: string) {
  return areasByType.value.get(type) || []
}

//#endregion

defineExpose({
  addFeatures,
  clearFeatures,
  setVisibility,
  getFeaturesByType,
  getLayer: () => areaLayer.value,
  getSource: () => vectorSource.value,
})
</script>

<style scoped lang="scss">
.area-layer {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}
</style>