<template>
  <!-- <span>titre du premier node {{ nodes[0].title }}</span> -->
  <!-- <p>nodes: {{ nodes }}</p> -->

<n-tree
  ref="treeRef"
  v-model:expanded-keys="expandedKeys"
  :data="nodeList"
  :show-irrelevant-nodes="false"
  key-field="key"
  :selectable="true"
  block-line
  @update:selected-keys="onSelectItem"
  @update:expanded-keys="onToggle"
  :render-label="renderLabel"
>
  <!-- label-field="title" -->
  <!-- Slot label : rendu du node -->
  <template #label="{ option }">
    <div class="d-flex align-items-center justify-content-between">
      <span>
        <span v-if="option.selected"><strong>{{ option.title }}{{ option.variables?.length ? ' ' + $tc('VariableStructureList.variable', option.variables.length, { count: option.variables.length }) : '' }}</strong></span>
        <span v-else>{{ option.title }}{{ option.variables?.length ? ' ' + $tc('VariableStructureList.variable', option.variables.length, { count: option.variables.length }) : '' }}</span>
      </span>
      <!-- slot buttons s’il existe -->
      <span v-if="!noButtons">
        <slot name="buttons" :node="option" />
      </span>
    </div>
  </template>
</n-tree>

</template>

<script setup lang="ts">
import { ref, defineProps, defineEmits, useSlots, defineExpose, watch, onMounted, h } from 'vue'
import {NTree, TreeOption} from 'naive-ui'

const props = defineProps<{
  nodes: TreeOption[]
  noButtons?: boolean
}>()

const emit = defineEmits<{
  select: [Array<TreeOption>],
  toggle: [Array<TreeOption>]
}>()
const slots = useSlots()

const treeRef = ref<InstanceType<typeof NTree> | null>(null)
const nodeList = ref(props.nodes || [])
const expandedKeys = ref<string[]>([])
const selectedKeys = ref<string[]>([])

onMounted(() => {
  console.log("TreeView mounted")
  console.log("nodeList au mount :", nodeList.value)
})

// Dans le watch de props.nodes
watch(
  () => props.nodes,
  (newVal) => {
     console.log("[TreeView] Mise à jour des nodes", newVal)
    nodeList.value = newVal
  },
  { immediate: true }
)


function onSelectItem(keys: string[], options: Array<TreeOption>) {
  selectedKeys.value = keys
  emit('select', options)
}

function onToggle(keys: string[], options: Array<TreeOption>) {
  expandedKeys.value = keys
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


function renderLabel(option: any) {
  const rawNode = option.option; // c'est ici qu'on retrouve le vrai node avec uri, title, etc.

  return h(
    'div',
    {
      class: 'd-flex align-items-center justify-content-between w-100',
      style: 'width: 100%'
    },
    [
      h('span', {}, rawNode.title),
      slots.buttons
        ? h(
            'span',
            {
              onMousedown: (e: Event) => e.stopPropagation(),
              onMouseup: (e: Event) => e.stopPropagation()
            },
            slots.buttons({ node: rawNode }) 
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
