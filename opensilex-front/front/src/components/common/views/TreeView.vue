<template>
  <!-- <span>titre du premier node {{ nodes[0].title }}</span> -->
  <!-- <p>nodes: {{ nodes }}</p> -->

<n-tree
  ref="treeRef"
  :data="nodeList"
  :show-irrelevant-nodes="false"
  key-field="key"
  :selectable="true"
  block-line
  @update:selected-keys="onSelectItem"
  @update:expanded-keys="onToggle"
  :render-label="renderLabel"
  :default-expand-all="defaultExpandAll"
>
</n-tree>

</template>

<script setup generic="T extends TreeViewOption" lang="ts">
import {ref, useSlots, watch, onMounted, h, VNodeChild} from 'vue'
import {NTree, TreeOption} from 'naive-ui'

export interface TreeViewOption extends TreeOption {
  title?: string
}

const props = withDefaults(defineProps<{
  nodes: T[]
  noButtons?: boolean
  defaultExpandAll: boolean
}>(), {
  defaultExpandAll: false
})

const emit = defineEmits<{
  select: [Array<T>],
  toggle: [Array<T>]
}>()
const slots = defineSlots<{
  buttons: (props: { node: T, selected: boolean }) => VNodeChild
  node: (props: { node: T, selected: boolean }) => VNodeChild
}>()

const treeRef = ref<InstanceType<typeof NTree> | null>(null)
const nodeList = ref(props.nodes || [])
const selectedKeys = ref<string[]>([])

onMounted(() => {
  console.debug("[TreeView] nodeList au mount :", nodeList.value)
})

// Dans le watch de props.nodes
watch(
  () => props.nodes,
  (newVal) => {
     console.debug("[TreeView] Mise à jour des nodes", newVal)
    nodeList.value = newVal
  },
  { immediate: true }
)


function onSelectItem(keys: string[], options: Array<T>) {
  selectedKeys.value = keys
  emit('select', options)
}

function onToggle(keys: string[], options: Array<T>) {
  emit('toggle', options)
}

function getSelectedNode() {
  const selected = selectedKeys.value
  if (!selected || selected.length === 0) return undefined

  const findNode = (nodes: any[]): any => {
    for (const node of nodes) {
      if (node.key === selected[0]) return node
      if (node.children) {
        const found = findNode(node.children)
        if (found) return found
      }
    }
    return undefined
  }

  return findNode(nodeList.value)
}


function renderLabel(info: { option: T, selected: boolean }): VNodeChild {
  const node = info.option; // c'est ici qu'on retrouve le vrai node avec uri, title, etc.
  const selected = info.selected;

  return h(
      'div',
      {
        class: 'd-flex align-items-center',
      },
      [
        h(
            'span',
            {style: {"flex-grow": "1"}},
            [slots.node ? slots.node({node, selected}) : h('span', {}, node.title)],
        ),
        slots.buttons
            ? h(
                'span',
                {},
                [slots.buttons({node, selected})]
            )
            : null
      ]
  )
}

defineExpose({
  getSelectedNode
})
</script>

<style scoped>
.toggle-icon {
  padding-left: 5px;
  padding-right: 5px;
}
</style>
