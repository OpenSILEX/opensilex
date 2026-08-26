<template>
  <div class="so-tree-panel">
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

interface ScientificObject {
  type?: string
  name?: string
  uri?: string
}

const emit = defineEmits<{
  visibilityChange: [layer: string, visible: boolean]
}>()

//#endregion

//#region Private
const scientificObjects = ref<ScientificObject[]>([])
const checkedKeys = ref<string[]>([])

const treeData = computed(() => {
  if (scientificObjects.value.length === 0) {
    return []
  }

  const grouped: Map<string, ScientificObject[]> = new Map()
  scientificObjects.value.forEach((so) => {
    const type = so.type || 'Unknown'
    if (!grouped.has(type)) {
      grouped.set(type, [])
    }
    grouped.get(type)?.push(so)
  })

  return [
    {
      key: 'scientific-objects',
      label: `Scientific Objects (${scientificObjects.value.length})`,
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

function loadScientificObjects() {
  scientificObjects.value = []
}

function onCheck(keys: string[]) {
  checkedKeys.value = keys
  keys.forEach((key) => {
    emit('visibilityChange', key, true)
  })
}
//#endregion
</script>

<style scoped lang="scss">
.so-tree-panel {
  max-height: 400px;
  overflow: auto;
}
</style>