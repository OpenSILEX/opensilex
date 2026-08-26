<template>
  <div class="ol-layers-container">
    <ScientificObjectLayer
      v-if="layersReady"
      :experiment-uri="experimentUri"
      :map-instance="mapInstance"
      :features="scientificObjects"
      ref="scientificObjectLayerRef"
      @layer-ready="onScientificObjectLayerReady"
    />

    <AreaLayer
      v-if="layersReady"
      :experiment-uri="experimentUri"
      :map-instance="mapInstance"
      :features="areas"
      ref="areaLayerRef"
      @layer-ready="onAreaLayerReady"
    />

    <DeviceLayer
      v-if="layersReady"
      :experiment-uri="experimentUri"
      :map-instance="mapInstance"
      :features="devices"
      ref="deviceLayerRef"
      @layer-ready="onDeviceLayerReady"
    />

    <FilterLayer
      v-if="layersReady"
      :experiment-uri="experimentUri"
      :map-instance="mapInstance"
      @layer-ready="onFilterLayerReady"
      @filter-added="onFilterAdded"
      @filter-updated="onFilterUpdated"
      @filter-deleted="onFilterDeleted"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, nextTick } from 'vue'
import ScientificObjectLayer from './ScientificObjectLayer.vue'
import AreaLayer from './AreaLayer.vue'
import DeviceLayer from './DeviceLayer.vue'
import FilterLayer from './FilterLayer.vue'

//#region Public
const props = withDefaults(
  defineProps<{
    experimentUri: string
    mapInstance: unknown
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

const emit = defineEmits<{
  layerReady: []
  filterAdded: [filter: Filter]
  filterUpdated: [filter: Filter]
  filterDeleted: [filter: { ref: string }]
}>()

//#endregion

//#region Private
//#region Template refs
const scientificObjectLayerRef = ref<InstanceType<typeof ScientificObjectLayer> | null>(null)
const areaLayerRef = ref<InstanceType<typeof AreaLayer> | null>(null)
const deviceLayerRef = ref<InstanceType<typeof DeviceLayer> | null>(null)
//#endregion

//#region Data and computed
const layersReady = ref<boolean>(false)
const scientificObjectLayerReady = ref<boolean>(false)
const areaLayerReady = ref<boolean>(false)
const deviceLayerReady = ref<boolean>(false)
const filterLayerReady = ref<boolean>(false)
//#endregion

//#region Hooks
onMounted(() => {
  layersReady.value = true
})

watch(
  [() => props.scientificObjects, () => props.mapInstance],
  ([newObjects, newMap]) => {
    if (newObjects.length > 0 && newMap && scientificObjectLayerReady.value) {
      nextTick(() => {
        scientificObjectLayerRef.value?.addFeatures(newObjects)
      })
    }
  }
)

watch(
  [() => props.areas, () => props.mapInstance],
  ([newAreas, newMap]) => {
    if (newAreas.length > 0 && newMap && areaLayerReady.value) {
      nextTick(() => {
        areaLayerRef.value?.addFeatures(newAreas)
      })
    }
  }
)

watch(
  [() => props.devices, () => props.mapInstance],
  ([newDevices, newMap]) => {
    if (newDevices.length > 0 && newMap && deviceLayerReady.value) {
      nextTick(() => {
        deviceLayerRef.value?.addFeatures(newDevices)
      })
    }
  }
)

watch(
  [() => scientificObjectLayerReady.value, () => props.mapInstance],
  ([layerReady, newMap]) => {
    if (layerReady && newMap && props.scientificObjects.length > 0) {
      nextTick(() => {
        scientificObjectLayerRef.value?.addFeatures(props.scientificObjects)
      })
    }
  }
)

watch(
  [() => areaLayerReady.value, () => props.mapInstance],
  ([layerReady, newMap]) => {
    if (layerReady && newMap && props.areas.length > 0) {
      nextTick(() => {
        areaLayerRef.value?.addFeatures(props.areas)
      })
    }
  }
)

watch(
  [() => deviceLayerReady.value, () => props.mapInstance],
  ([layerReady, newMap]) => {
    if (layerReady && newMap && props.devices.length > 0) {
      nextTick(() => {
        deviceLayerRef.value?.addFeatures(props.devices)
      })
    }
  }
)
//#endregion

//#region Event handlers
function onScientificObjectLayerReady() {
  scientificObjectLayerReady.value = true
  checkAllLayersReady()
}

function onAreaLayerReady() {
  areaLayerReady.value = true
  checkAllLayersReady()
}

function onDeviceLayerReady() {
  deviceLayerReady.value = true
  checkAllLayersReady()
}

function onFilterLayerReady() {
  filterLayerReady.value = true
  checkAllLayersReady()
}

function checkAllLayersReady() {
  if (
    scientificObjectLayerReady.value &&
    areaLayerReady.value &&
    deviceLayerReady.value &&
    filterLayerReady.value
  ) {
    emit('layerReady')
  }
}

function onFilterAdded(filter: Filter) {
  emit('filterAdded', filter)
}

function onFilterUpdated(filter: Filter) {
  emit('filterUpdated', filter)
}

function onFilterDeleted(filter: { ref: string }) {
  emit('filterDeleted', filter)
}
//#endregion

//#endregion
</script>

<style scoped lang="scss">
.ol-layers-container {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}
</style>