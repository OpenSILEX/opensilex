<template>
  <div class="filter-panel">
    <div class="filter-controls">
      <NButton type="primary" size="small" @click="onCreateFilter">
        Create Filter
      </NButton>
    </div>

    <NTree
      v-if="filters.length > 0"
      :data="treeData"
      default-expand-all
      block-line
    />

    <div v-else class="no-filters">
      No filters created yet.
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { NTree, NButton } from 'naive-ui'

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
  filterAdded: [filter: Filter]
  filterUpdated: [filter: Filter]
  filterDeleted: [filter: { ref: string }]
}>()

//#endregion

//#region Private
const filters = ref<Filter[]>([])

const treeData = computed(() =>
  filters.value.map((filter) => ({
    key: filter.ref,
    label: filter.titleDisplay || 'Filter',
    isLeaf: true,
    data: filter,
  }))
)

function onCreateFilter() {
  const newFilter: Filter = {
    ref: `filter-${Date.now()}`,
    titleDisplay: `Filter ${filters.value.length + 1}`,
    vlStyleStrokeColor: '#00a38d',
    vlStyleFillColor: '#00a38d',
    geometry: null,
    properties: {},
  }

  filters.value.push(newFilter)
  emit('filterAdded', newFilter)
}
//#endregion
</script>

<style scoped lang="scss">
.filter-panel {
  padding: 10px 0;

  .filter-controls {
    margin-bottom: 10px;
  }

  .no-filters {
    text-align: center;
    color: #999;
    padding: 20px;
    font-size: 12px;
  }
}
</style>