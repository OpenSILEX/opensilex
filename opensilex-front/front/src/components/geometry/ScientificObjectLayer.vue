<template>
  <div class="scientific-object-layer"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import Feature from 'ol/Feature'
import { Cluster, Vector as VectorSource } from 'ol/source'
import { Vector as VectorLayer } from 'ol/layer'
import { Style, Circle as CircleStyle, Fill, Stroke, Text } from 'ol/style'
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
const scientificObjectLayer = ref<VectorLayer | null>(null)
const clusterSource = ref<Cluster | null>(null)
const vectorSource = ref<VectorSource | null>(null)
const featuresByType = ref<Map<string, Feature[]>>(new Map())
//#endregion

//#region Hooks
onMounted(() => {
  // Defer initialization until mapInstance is available
})

watch(
  () => props.mapInstance,
  (newMapInstance) => {
    if (newMapInstance && !scientificObjectLayer.value) {
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
 * Initialize the Scientific Object layer with clustering support.
 */
function initializeLayer() {
  if (!props.mapInstance) {
    return
  }

  const source = new VectorSource()
  vectorSource.value = source

  const clusterSourceInstance = new Cluster({
    source: source,
    distance: 25,
  })
  clusterSource.value = clusterSourceInstance

  const layer = new VectorLayer({
    source: clusterSourceInstance,
    style: makeClusterStyleFunc(),
    zIndex: 1,
  })

  scientificObjectLayer.value = layer

  const mapInstanceTyped = props.mapInstance as { getLayers: () => import('ol/Collection').default<import('ol/layer/Layer').default> }
  const mapLayers = mapInstanceTyped.getLayers()
  mapLayers.push(layer)

  emit('layerReady')
}

/**
 * Create cluster style function with caching.
 */
function makeClusterStyleFunc() {
  const styleCache: Map<number, Style> = new Map()

  return (feature: import('ol/Feature').default) => {
    const features = feature.get('features') as import('ol/Feature').default[]
    const size = features?.length
    if (!size) {
      return null
    }

    let style = styleCache.get(size)
    if (!style) {
      style = new Style({
        image: new CircleStyle({
          radius: 10,
          stroke: new Stroke({
            color: '#fff',
          }),
          fill: new Fill({
            color: '#00a38d',
          }),
        }),
        text: new Text({
          text: size.toString(),
          fill: new Fill({
            color: '#fff',
          }),
        }),
      })
      styleCache.set(size, style)
    }

    return style
  }
}

/**
 * Add features to the layer, grouped by type.
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
  })
}

/**
 * Clear all features from the layer.
 */
function clearFeatures() {
  if (vectorSource.value) {
    vectorSource.value.clear()
  }
}

/**
 * Update visibility of the layer.
 */
function setVisibility(visible: boolean) {
  if (scientificObjectLayer.value) {
    scientificObjectLayer.value.setVisible(visible)
  }
}

//#endregion

defineExpose({
  addFeatures,
  clearFeatures,
  setVisibility,
  getLayer: () => scientificObjectLayer.value,
  getSource: () => vectorSource.value,
  getClusterSource: () => clusterSource.value,
})
</script>

<style scoped lang="scss">
.scientific-object-layer {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}
</style>