<template>
  <div class="device-tree-panel">
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

interface DeviceFeature {
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
const devices = ref<DeviceFeature[]>([])
const checkedKeys = ref<string[]>([])

const treeData = computed(() => {
  if (devices.value.length === 0) {
    return []
  }

  const grouped: Map<string, DeviceFeature[]> = new Map()
  devices.value.forEach((device) => {
    const type = device.properties?.type || 'Unknown'
    if (!grouped.has(type)) {
      grouped.set(type, [])
    }
    grouped.get(type)?.push(device)
  })

  return [
    {
      key: 'devices',
      label: `Devices (${devices.value.length})`,
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
.device-tree-panel {
  max-height: 400px;
  overflow: auto;
}
</style>