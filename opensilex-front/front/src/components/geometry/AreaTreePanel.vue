<template>
  <div class="area-tree-panel">
    <NTree
      :data="treeData"
      :checked-keys="checkedKeys"
      checkable
      default-expand-all
      block-line
      @update:checked-keys="onCheck"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { NTree } from 'naive-ui'

//#region Public
const props = withDefaults(
  defineProps<{
    experimentUri: string
  }>(),
  {}
)

interface AreaFeature {
  properties: {
    type?: string
    name?: string
    uri?: string
  }
}

const emit = defineEmits<{
  visibilityChange: [layer: string, visible: boolean]
}>()

//#endregion

//#region Private
const areas = ref<AreaFeature[]>([])
const checkedKeys = ref<string[]>([])

const treeData = computed(() => {
  if (areas.value.length === 0) {
    return []
  }

  const grouped: Map<string, AreaFeature[]> = new Map()
  areas.value.forEach((area) => {
    const type = area.properties?.type || 'Unknown'
    if (!grouped.has(type)) {
      grouped.set(type, [])
    }
    grouped.get(type)?.push(area)
  })

  return [
    {
      key: 'areas',
      label: `Areas (${areas.value.length})`,
      isLeaf: false,
      children: Array.from(grouped.entries()).map(([type, items]) => ({
        key: type,
        label: `${type} (${items.length})`,
        isLeaf: true,
        visible: true,
      })),
    },
  ]
})

function onCheck(keys: string[]) {
  checkedKeys.value = keys
  keys.forEach((key) => {
    emit('visibilityChange', key, true)
  })
}
//#endregion
</script>

<style scoped lang="scss">
.area-tree-panel {
  max-height: 400px;
  overflow: auto;
}
</style>