<template>
  <div class="map-panel-drawer-content">
    <NTabs type="line" animated :default-value="activeTab" @update:value="onTabChange">
      <NTabPane name="so" tab="Scientific Objects">
        <SOTreePanel
          :experiment-uri="experimentUri"
          @visibility-change="onVisibilityChange"
        />
      </NTabPane>

      <NTabPane name="areas" tab="Areas">
        <AreaTreePanel
          :experiment-uri="experimentUri"
          @visibility-change="onVisibilityChange"
        />
      </NTabPane>

      <NTabPane name="devices" tab="Devices">
        <DeviceTreePanel
          :experiment-uri="experimentUri"
          @visibility-change="onVisibilityChange"
        />
      </NTabPane>

      <NTabPane name="filters" tab="Filters">
        <FilterPanel
          :experiment-uri="experimentUri"
          @filter-added="onFilterAdded"
          @filter-updated="onFilterUpdated"
          @filter-deleted="onFilterDeleted"
        />
      </NTabPane>
    </NTabs>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { NTabs, NTabPane } from 'naive-ui'
import SOTreePanel from './SOTreePanel.vue'
import AreaTreePanel from './AreaTreePanel.vue'
import DeviceTreePanel from './DeviceTreePanel.vue'
import FilterPanel from './FilterPanel.vue'

//#region Public
const props = withDefaults(
  defineProps<{
    experimentUri: string
  }>(),
  {}
)

interface Filter {
  ref: string
  titleDisplay?: string
  vlStyleStrokeColor?: string
  vlStyleFillColor?: string
  geometry?: unknown
  properties?: Record<string, unknown>
}

const emit = defineEmits<{
  visibilityChange: [layer: string, visible: boolean]
  filterAdded: [filter: Filter]
  filterUpdated: [filter: Filter]
  filterDeleted: [filter: { ref: string }]
}>()

//#endregion

//#region Private
const activeTab = ref<string>('so')

function onTabChange(tab: string) {
  activeTab.value = tab
}

function onVisibilityChange(layer: string, visible: boolean) {
  emit('visibilityChange', layer, visible)
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
</script>

<style scoped lang="scss">
.map-panel-drawer-content {
  padding: 10px;
}
</style>