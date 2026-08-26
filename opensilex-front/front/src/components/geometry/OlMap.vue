<template>
  <div ref="mapContainerRef" class="map-container"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useTemplateRef } from 'vue'
import Map from 'ol/Map'
import View from 'ol/View'
import TileLayer from 'ol/layer/Tile'
import OSM from 'ol/source/OSM'
import { fromLonLat, toLonLat } from 'ol/proj'
import { defaults as defaultControls, ScaleLine } from 'ol/control'

//#region Public
const props = withDefaults(
  defineProps<{
    experimentUri: string
  }>(),
  {}
)

interface MapInstance {
  instance: Map
  view: View
  getLayers: () => import('ol/Collection').default<import('ol/layer/Layer').default>
  forEachFeatureAtPixel: (pixel: [number, number], callback: (feature: import('ol/Feature').default) => unknown) => unknown
  getFeaturesAtPixel: (pixel: [number, number]) => import('ol/Feature').default[]
  addInteraction: (interaction: import('ol/interaction/Interaction').default) => void
  on: (event: string, handler: (...args: unknown[]) => void) => void
  getSize: () => [number, number] | false
  getSizePrint: () => [number, number] | false
  setSize: (size: [number, number]) => void
  getView: () => View
  calculateExtent: (size?: [number, number]) => import('ol/extent').Extent
  once: (event: string, handler: (...args: unknown[]) => void) => void
}

const emit = defineEmits<{
  mapCreated: [instance: MapInstance]
}>()

//#endregion

//#region Private
//#region Template refs
const mapContainerRef = useTemplateRef<HTMLDivElement>('mapContainerRef')
//#endregion

//#region Data and computed
const mapInstance = ref<Map | null>(null)
const viewInstance = ref<View | null>(null)
//#endregion

//#region Hooks
onMounted(() => {
  initializeMap()
})

onBeforeUnmount(() => {
  if (mapInstance.value) {
    mapInstance.value.setTarget(null)
    mapInstance.value = null
  }
})
//#endregion

//#region methods
/**
 * Initialize the OpenLayers map with OSM base layer and default controls.
 */
function initializeMap() {
  if (!mapContainerRef.value) {
    return
  }

  const view = new View({
    center: fromLonLat([2.3522, 48.8566]),
    zoom: 12,
    minZoom: 2,
    maxZoom: 19,
  })

  viewInstance.value = view

  const map = new Map({
    target: mapContainerRef.value,
    layers: [
      new TileLayer({
        source: new OSM(),
      }),
    ],
    view: view,
    controls: defaultControls().extend([new ScaleLine()]),
    loadTilesWhileAnimating: true,
    loadTilesWhileInteracting: true,
  })

  mapInstance.value = map

  const mapInstanceObj: MapInstance = {
    instance: map,
    view: view,
    getLayers: () => map.getLayers(),
    forEachFeatureAtPixel: (pixel: [number, number], callback: (feature: import('ol/Feature').default) => unknown) => {
      return map.forEachFeatureAtPixel(pixel, callback)
    },
    getFeaturesAtPixel: (pixel: [number, number]) => {
      return map.getFeaturesAtPixel(pixel)
    },
    addInteraction: (interaction: import('ol/interaction/Interaction').default) => {
      map.addInteraction(interaction)
    },
    on: (event: string, handler: (...args: unknown[]) => void) => {
      map.on(event, handler)
    },
    getSize: () => map.getSize(),
    getSizePrint: () => map.getSize(),
    setSize: (size: [number, number]) => {
      map.setSize(size)
    },
    getView: () => map.getView(),
    calculateExtent: (size?: [number, number]) => {
      return map.calculateExtent(size)
    },
    once: (event: string, handler: (...args: unknown[]) => void) => {
      map.once(event, handler)
    },
  }

  emit('mapCreated', mapInstanceObj)
}

/**
 * Center the map on the given extent.
 */
function fitExtent(extent: import('ol/extent').Extent, options: Record<string, unknown> = {}) {
  if (viewInstance.value && extent) {
    viewInstance.value.fit(extent, options)
  }
}

/**
 * Get the current map extent in EPSG:4326.
 */
function getCurrentExtent() {
  if (!viewInstance.value || !mapContainerRef.value) {
    return null
  }

  const size = mapContainerRef.value.getBoundingClientRect()
  const extent = viewInstance.value.calculateExtent([size.width, size.height])

  const topLeft = toLonLat([extent[0], extent[3]])
  const bottomRight = toLonLat([extent[2], extent[1]])

  return {
    west: topLeft[0],
    south: topLeft[1],
    east: bottomRight[0],
    north: bottomRight[1],
  }
}

//#endregion

defineExpose({
  fitExtent,
  getCurrentExtent,
  getMapInstance: () => mapInstance.value,
  getViewInstance: () => viewInstance.value,
})
</script>

<style scoped lang="scss">
.map-container {
  width: 100%;
  height: 100%;
  position: relative;
}
</style>