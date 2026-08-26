<template>
  <div class="filter-layer"></div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import Feature from 'ol/Feature'
import { Vector as VectorSource } from 'ol/source'
import { Vector as VectorLayer } from 'ol/layer'
import { Style, Fill, Stroke, Circle as CircleStyle } from 'ol/style'
import { fromLonLat } from 'ol/proj'

//#region Public
const props = withDefaults(
  defineProps<{
    experimentUri: string
    mapInstance: unknown
  }>(),
  {}
)

interface Filter {
  ref: string
  titleDisplay?: string
  vlStyleStrokeColor?: string
  vlStyleFillColor?: string
  geometry?: {
    coordinates?: unknown
    type?: string
  }
  properties?: Record<string, unknown>
}

interface Feature {
  properties: {
    uri?: string
    name?: string
    type?: string
    nature?: string
    ref?: string
    vlStyleStrokeColor?: string
    vlStyleFillColor?: string
    [key: string]: unknown
  }
  geometry?: {
    coordinates?: unknown
    type?: string
  }
}

const emit = defineEmits<{
  layerReady: []
  filterAdded: [filter: Filter]
  filterUpdated: [filter: Filter]
  filterDeleted: [filter: { ref: string }]
}>()

//#endregion

//#region Private
//#region Template refs
//#endregion

//#region Data and computed
const filterLayer = ref<VectorLayer | null>(null)
const vectorSource = ref<VectorSource | null>(null)
const filters = ref<Filter[]>([])
//#endregion

//#region Hooks
onMounted(() => {
  initializeLayer()
})
//#endregion

//#region methods
/**
 * Initialize the Filter layer.
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
          color: 'rgba(0, 163, 141, 0.5)',
        }),
        stroke: new Stroke({
          color: '#00a38d',
        }),
      }),
    }),
    zIndex: 2,
  })

  filterLayer.value = layer

  const mapInstanceTyped = props.mapInstance as { getLayers: () => import('ol/Collection').default<import('ol/layer/Layer').default> }
  const mapLayers = mapInstanceTyped.getLayers()
  mapLayers.push(layer)

  emit('layerReady')
}

/**
 * Add filter feature with custom colors.
 */
function addFilter(filter: Filter) {
  if (!vectorSource.value) {
    return
  }

  const feature: Feature = {
    geometry: filter.geometry,
    properties: {
      ...filter.properties,
      ref: filter.ref,
      vlStyleStrokeColor: filter.vlStyleStrokeColor || '#00a38d',
      vlStyleFillColor: filter.vlStyleFillColor || '#00a38d',
    },
  }

  if (feature.geometry?.coordinates) {
    const coords = feature.geometry.coordinates
    if (Array.isArray(coords[0])) {
      feature.geometry.coordinates = coords.map((ring: number[][]) =>
        ring.map((coord: number[]) => fromLonLat(coord))
      )
    } else if (Array.isArray(coords)) {
      feature.geometry.coordinates = fromLonLat(coords as [number, number])
    }
  }

  vectorSource.value?.addFeature(new Feature(feature))
  filters.value.push(filter)

  emit('filterAdded', filter)
}

/**
 * Update filter style.
 */
function updateFilter(filter: Filter) {
  if (!vectorSource.value) {
    return
  }

  const index = filters.value.findIndex((f) => f.ref === filter.ref)
  if (index !== -1) {
    filters.value[index] = filter
    const features = vectorSource.value.getFeatures()
    const feature = features.find((f: Feature) => f.properties.ref === filter.ref)
    if (feature) {
      const fillColor = filter.vlStyleFillColor ? hexToRgb(filter.vlStyleFillColor) : '0, 163, 141'
      const strokeColor = filter.vlStyleStrokeColor || '#00a38d'
      feature.properties.vlStyleFillColor = filter.vlStyleFillColor
      feature.properties.vlStyleStrokeColor = filter.vlStyleStrokeColor
    }
  }
}

/**
 * Delete filter.
 */
function deleteFilter(filterRef: string) {
  if (!vectorSource.value) {
    return
  }

  const features = vectorSource.value.getFeatures()
  const featureIndex = features.findIndex((f: Feature) => f.properties.ref === filterRef)
  if (featureIndex !== -1) {
    vectorSource.value.removeFeature(features[featureIndex])
  }

  const filterIndex = filters.value.findIndex((f) => f.ref === filterRef)
  if (filterIndex !== -1) {
    filters.value.splice(filterIndex, 1)
  }

  emit('filterDeleted', { ref: filterRef })
}

/**
 * Clear all filters.
 */
function clearFilters() {
  if (vectorSource.value) {
    vectorSource.value.clear()
  }
  filters.value = []
}

/**
 * Update visibility.
 */
function setVisibility(visible: boolean) {
  if (filterLayer.value) {
    filterLayer.value.setVisible(visible)
  }
}

/**
 * Convert hex color to RGB.
 */
function hexToRgb(hex: string): string {
  const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex)
  if (!result) {
    return '0, 0, 0'
  }
  return `${parseInt(result[1], 16)}, ${parseInt(result[2], 16)}, ${parseInt(result[3], 16)}`
}

//#endregion

defineExpose({
  addFilter,
  updateFilter,
  deleteFilter,
  clearFilters,
  setVisibility,
  getFilters: () => filters.value,
  getLayer: () => filterLayer.value,
  getSource: () => vectorSource.value,
})
</script>

<style scoped lang="scss">
.filter-layer {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}
</style>