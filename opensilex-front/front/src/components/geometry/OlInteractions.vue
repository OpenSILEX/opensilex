<template>
  <div class="ol-interactions"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import { DragBox } from 'ol/interaction'
import Select from 'ol/interaction/Select'
import Draw from 'ol/interaction/Draw'
import { ConditionFn } from 'ol/events/condition'
import type { Feature as OlFeature } from 'ol/Feature'

//#region Public
const props = withDefaults(
  defineProps<{
    mapInstance: unknown
    editingMode: boolean
  }>(),
  {}
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
  drawEnd: [feature: Feature]
}>()

//#endregion

//#region Private
//#region Template refs
//#endregion

//#region Data and computed
const dragBoxInteraction = ref<DragBox | null>(null)
const selectInteraction = ref<Select | null>(null)
const drawInteraction = ref<Draw | null>(null)
const selectedFeatures = ref<Feature[]>([])
//#endregion

//#region Hooks
onMounted(() => {
  initializeInteractions()
})

onBeforeUnmount(() => {
  removeInteractions()
})

watch(
  () => props.editingMode,
  (newMode: boolean) => {
    toggleDrawInteraction(newMode)
  }
)

//#endregion

//#region Event handlers
function onDragBoxEnd() {
  if (!props.mapInstance) {
    return
  }

  const mapInstanceTyped = props.mapInstance as { getLayers: () => import('ol/Collection').default<import('ol/layer/Layer').default> }
  const extent = dragBoxInteraction.value?.getGeometry()?.getExtent()
  if (!extent) {
    return
  }

  const layers = mapInstanceTyped.getLayers()
  layers.forEach((layer: import('ol/layer/Layer').default) => {
    if (layer.getLayerGroup?.() || layer.getType?.() === 'VECTOR') {
      const source = layer.getSource?.()
      if (source && 'forEachFeatureIntersectingExtent' in source) {
        const sourceTyped = source as { forEachFeatureIntersectingExtent: (extent: number[], callback: (feature: OlFeature) => void) => void }
        sourceTyped.forEachFeatureIntersectingExtent(extent, (feature: OlFeature) => {
          const featureData: Feature = {
            properties: {
              uri: feature.values_?.uri || feature.get('uri'),
              name: feature.values_?.name || feature.get('name'),
              type: feature.values_?.type || feature.get('type'),
              nature: feature.values_?.nature || feature.get('nature'),
            },
            geometry: feature.getGeometry() ? {
              type: feature.getGeometry()?.getType(),
              coordinates: feature.getGeometry()?.getCoordinates(),
            } : undefined,
          }
          selectedFeatures.value.push(featureData)
        })
      }
    }
  })

  emit('select', selectedFeatures.value[selectedFeatures.value.length - 1] || { properties: {}, geometry: {} })
}

function onMapClick(event: { pixel: [number, number] }) {
  if (!props.mapInstance) {
    return
  }

  const mapInstanceTyped = props.mapInstance as { getFeaturesAtPixel: (pixel: [number, number]) => OlFeature[]; forEachFeatureAtPixel: (pixel: [number, number], callback: (feature: OlFeature) => unknown) => unknown; getView: () => { fit: (extent: number[], options: Record<string, unknown>) => void } }
  const features = mapInstanceTyped.getFeaturesAtPixel(event.pixel)
  if (!features || features.length === 0) {
    if (!props.editingMode) {
      selectedFeatures.value = []
    }
    return
  }

  const feature = features[0]
  const clusterFeatures = feature.get('features') as OlFeature[]

  if (clusterFeatures && clusterFeatures.length > 0) {
    // Zoom to cluster
    const points: number[][] = []
    clusterFeatures.forEach((clusterFeature: OlFeature) => {
      const geom = clusterFeature.getGeometry()
      if (geom) {
        const type = geom.getType()
        if (type === 'Point') {
          points.push(geom.getCoordinates() as number[])
        } else if (type === 'Polygon') {
          points.push(geom.getInteriorPoint()?.getCoordinates() as number[])
        } else if (type === 'LineString') {
          points.push(geom.getCoordinateAt(0.5))
        }
      }
    })

    if (points.length > 0) {
      const extent = [
        Math.min(...points.map(p => p[0])),
        Math.min(...points.map(p => p[1])),
        Math.max(...points.map(p => p[0])),
        Math.max(...points.map(p => p[1])),
      ]
      mapInstanceTyped.getView().fit(extent, { maxZoom: 17 })
    }
  } else {
    if (!props.editingMode) {
      selectedFeatures.value = []
    }
  }
}

function onDragBoxStart() {
  selectedFeatures.value = []
}

//#endregion

//#region methods
/**
 * Initialize all interactions.
 */
function initializeInteractions() {
  if (!props.mapInstance) {
    return
  }

  const mapInstanceTyped = props.mapInstance as { addInteraction: (interaction: import('ol/interaction/Interaction').default) => void; on: (event: string, handler: (...args: unknown[]) => void) => void }

  dragBoxInteraction.value = new DragBox({
    condition: (event: Event) => {
      return (event as KeyboardEvent).shiftKey && (event as KeyboardEvent).altKey
    },
  })

  dragBoxInteraction.value.on('boxend', onDragBoxEnd)
  dragBoxInteraction.value.on('boxstart', onDragBoxStart)

  mapInstanceTyped.addInteraction(dragBoxInteraction.value)

  selectInteraction.value = new Select()
  mapInstanceTyped.addInteraction(selectInteraction.value)

  mapInstanceTyped.on('click', onMapClick)
}

/**
 * Toggle draw interaction based on editing mode.
 */
function toggleDrawInteraction(enabled: boolean) {
  if (!props.mapInstance) {
    return
  }

  const mapInstanceTyped = props.mapInstance as { addInteraction: (interaction: import('ol/interaction/Interaction').default) => void; removeInteraction: (interaction: import('ol/interaction/Interaction').default) => void }

  if (enabled) {
    drawInteraction.value = new Draw({
      type: 'Polygon',
      source: null,
    })

    drawInteraction.value.on('drawend', (event: { feature: OlFeature }) => {
      const feature: Feature = {
        properties: {
          uri: event.feature.values_?.uri || event.feature.get('uri'),
          name: event.feature.values_?.name || event.feature.get('name'),
          type: event.feature.values_?.type || event.feature.get('type'),
          nature: event.feature.values_?.nature || event.feature.get('nature'),
        },
        geometry: event.feature.getGeometry() ? {
          type: event.feature.getGeometry()?.getType(),
          coordinates: event.feature.getGeometry()?.getCoordinates(),
        } : undefined,
      }
      emit('drawEnd', feature)
    })

    mapInstanceTyped.addInteraction(drawInteraction.value)
  } else {
    if (drawInteraction.value) {
      mapInstanceTyped.removeInteraction(drawInteraction.value)
      drawInteraction.value = null
    }
  }
}

/**
 * Get selected features.
 */
function getSelectedFeatures() {
  return selectedFeatures.value
}

/**
 * Clear selected features.
 */
function clearSelection() {
  selectedFeatures.value = []
}

/**
 * Remove all interactions.
 */
function removeInteractions() {
  if (!props.mapInstance) {
    return
  }

  const mapInstanceTyped = props.mapInstance as { removeInteraction: (interaction: import('ol/interaction/Interaction').default) => void }

  if (dragBoxInteraction.value) {
    mapInstanceTyped.removeInteraction(dragBoxInteraction.value)
  }

  if (selectInteraction.value) {
    mapInstanceTyped.removeInteraction(selectInteraction.value)
  }

  if (drawInteraction.value) {
    mapInstanceTyped.removeInteraction(drawInteraction.value)
  }
}

//#endregion

defineExpose({
  toggleDrawInteraction,
  getSelectedFeatures,
  clearSelection,
  removeInteractions,
})
</script>

<style scoped lang="scss">
.ol-interactions {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}
</style>