<template>
  <div class="ol-overlays">
    <div
      v-if="showTooltip"
      class="tooltip-overlay"
      :style="{ left: tooltipPosition.x + 'px', top: tooltipPosition.y + 'px' }"
    >
      <div class="panel-content">
        {{ tooltipContent }}
      </div>
    </div>

    <div
      v-if="showDetailPopup"
      class="detail-popup-overlay"
      :style="{ left: detailPopupPosition.x + 'px', top: detailPopupPosition.y + 'px' }"
    >
      <div class="panel-content detail-panel">
        <h4>{{ selectedFeature?.properties?.name }}</h4>
        <p>{{ selectedFeature?.properties?.type }}</p>
        <p v-if="selectedFeature?.properties?.uri">
          URI: {{ selectedFeature.properties.uri }}
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

//#region Public
const props = withDefaults(
  defineProps<{
    mapInstance: unknown
    selectedFeatures: Feature[]
  }>(),
  {
    selectedFeatures: () => [],
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
  featureHover: [feature: Feature]
  featureClick: [feature: Feature]
}>()

//#endregion

//#region Private
//#region Data and computed
const showTooltip = ref<boolean>(false)
const showDetailPopup = ref<boolean>(false)
const tooltipPosition = ref({ x: 0, y: 0 })
const detailPopupPosition = ref({ x: 0, y: 0 })
const selectedFeature = ref<Feature | null>(null)
const tooltipContent = ref<string>('')
//#endregion

//#region Hooks
//#endregion

//#region Event handlers
function onMapPointerMove(pixel: [number, number]) {
  if (!props.mapInstance || !pixel) {
    return
  }

  const mapInstanceTyped = props.mapInstance as { forEachFeatureAtPixel: (pixel: [number, number], callback: (feature: import('ol/Feature').default) => unknown) => unknown }
  const hitFeature = mapInstanceTyped.forEachFeatureAtPixel(
    pixel,
    (feature: import('ol/Feature').default) => feature
  )

  if (hitFeature) {
    const name = hitFeature.values_?.name || hitFeature.get('name') || ''
    const type = hitFeature.values_?.type || hitFeature.get('type') || ''

    if (name && type) {
      tooltipContent.value = `${name} (${type})`
      showTooltip.value = true
      tooltipPosition.value = {
        x: pixel[0] + 10,
        y: pixel[1] - 10,
      }
    }

    const feature: Feature = {
      properties: {
        uri: hitFeature.values_?.uri || hitFeature.get('uri'),
        name: hitFeature.values_?.name || hitFeature.get('name'),
        type: hitFeature.values_?.type || hitFeature.get('type'),
        nature: hitFeature.values_?.nature || hitFeature.get('nature'),
      },
      geometry: hitFeature.getGeometry() ? {
        type: hitFeature.getGeometry()?.getType(),
        coordinates: hitFeature.getGeometry()?.getCoordinates(),
      } : undefined,
    }

    emit('featureHover', feature)
  } else {
    showTooltip.value = false
  }
}

function onMapClick(pixel: [number, number]) {
  if (!props.mapInstance || !pixel) {
    return
  }

  const mapInstanceTyped = props.mapInstance as { getFeaturesAtPixel: (pixel: [number, number]) => import('ol/Feature').default[] }
  const features = mapInstanceTyped.getFeaturesAtPixel(pixel)
  if (features && features.length > 0) {
    const feature = features[0]
    selectedFeature.value = {
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
    showDetailPopup.value = true
    detailPopupPosition.value = {
      x: pixel[0] + 10,
      y: pixel[1] + 10,
    }

    emit('featureClick', selectedFeature.value)
  } else {
    showDetailPopup.value = false
  }
}

//#endregion

//#region methods
function setMapInstance(mapInstance: unknown) {
  // Event handlers will be attached by parent
}

function hideOverlays() {
  showTooltip.value = false
  showDetailPopup.value = false
}

//#endregion

defineExpose({
  onMapPointerMove,
  onMapClick,
  hideOverlays,
  setMapInstance,
})
</script>

<style scoped lang="scss">
.tooltip-overlay,
.detail-popup-overlay {
  position: absolute;
  z-index: 1000;
  pointer-events: none;
  transform: translate(-50%, -100%);
}

.panel-content {
  background: white;
  box-shadow: 0 0.25em 0.5em rgba(0, 0, 0, 0.2);
  border-radius: 5px;
  padding: 5px 10px;
  font-size: 12px;
  white-space: nowrap;
}

.detail-panel {
  min-width: 200px;
  max-width: 300px;
  padding: 10px;

  h4 {
    margin: 0 0 5px 0;
    font-size: 14px;
    font-weight: bold;
  }

  p {
    margin: 2px 0;
    font-size: 12px;
  }
}
</style>