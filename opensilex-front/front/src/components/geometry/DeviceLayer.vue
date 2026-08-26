<template>
  <div class="device-layer"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import Feature from 'ol/Feature'
import { Vector as VectorSource } from 'ol/source'
import { Vector as VectorLayer } from 'ol/layer'
import { Style, Circle as CircleStyle, Fill, Stroke } from 'ol/style'
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
const deviceLayer = ref<VectorLayer | null>(null)
const vectorSource = ref<VectorSource | null>(null)
const devicesByType = ref<Map<string, Feature[]>>(new Map())
//#endregion

//#region Hooks
onMounted(() => {
  // Defer initialization until mapInstance is available
})

watch(
  () => props.mapInstance,
  (newMapInstance) => {
    if (newMapInstance && !deviceLayer.value) {
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
 * Initialize the Device layer with point markers.
 */
function initializeLayer() {
  if (!props.mapInstance) {
    return
  }

  const source = new VectorSource()
  vectorSource.value = source

  const layer = new VectorLayer({
    source: source,
    style: new Style({
      image: new CircleStyle({
        radius: 5,
        fill: new Fill({
          color: 'orange',
        }),
        stroke: new Stroke({
          color: 'yellow',
        }),
      }),
    }),
    zIndex: 0,
  })

  deviceLayer.value = layer

  const mapInstanceTyped = props.mapInstance as { getLayers: () => import('ol/Collection').default<import('ol/layer/Layer').default> }
  const mapLayers = mapInstanceTyped.getLayers()
  mapLayers.push(layer)

  emit('layerReady')
}

/**
 * Add device features to the layer.
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
    if (!devicesByType.value.has(type)) {
      devicesByType.value.set(type, [])
    }
    devicesByType.value.get(type)?.push(feature)
  })
}

/**
 * Clear all device features.
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
  if (deviceLayer.value) {
    deviceLayer.value.setVisible(visible)
  }
}

/**
 * Get features by type.
 */
function getFeaturesByType(type: string) {
  return devicesByType.value.get(type) || []
}

//#endregion

defineExpose({
  addFeatures,
  clearFeatures,
  setVisibility,
  getFeaturesByType,
  getLayer: () => deviceLayer.value,
  getSource: () => vectorSource.value,
})
</script>

<style scoped lang="scss">
.device-layer {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}
</style>