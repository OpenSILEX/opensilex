<template>
  <n-modal
    v-model:show="visible"
    :mask-closable="false"
    :preset="'dialog'"
    :show-icon="false"
    :style="{ width: '1140px', maxWidth: '95vw' }"
    @after-leave="$emit('hide')"
  >
    <template #header>
      <div class="modal-title">
        <i class="bi bi-search"></i>
        {{ $t('component.project.filter-description') }}
      </div>
    </template>

    <div class="modal-body">
      <opensilex-VariableList
        ref="variableSelection"
        :noActions="true"
        :pageSize="5"
        :maximumSelectedRows="maximumSelectedRows"
        :withAssociatedData="withAssociatedData"
        :experiment="experiment"
        :objects="objects"
        :devices="devices"
        @select="$emit('select', $event)"
        @unselect="$emit('unselect', $event)"
        @selectall="$emit('selectall', $event)"
      />
    </div>

    <template #action>
      <n-space justify="end">
        <n-button tertiary @click="hide(false)">
          {{ $t('component.common.close') }}
        </n-button>
        <n-button type="primary" class="greenThemeColor" @click="hide(true)">
          {{ $t('component.common.validateSelection') }}
        </n-button>
      </n-space>
    </template>
  </n-modal>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { NModal, NButton, NSpace } from 'naive-ui'

const variableSelection = ref<any>(null)
const visible = ref(false)

const props = defineProps<{
  maximumSelectedRows?: number
  withAssociatedData?: boolean
  experiment?: any
  objects?: any
  devices?: any
}>()

const emit = defineEmits<{
  (e: 'onValidate', value: any[]): void
  (e: 'onClose'): void
  (e: 'shown'): void
  (e: 'hide'): void
  (e: 'select', value: any): void
  (e: 'unselect', value: any): void
  (e: 'selectall', value: any): void
}>()

function show() {
  visible.value = true
  emit('shown')
}

function hide(validate: boolean) {
  // on ferme toujours la modale
  visible.value = false
  // puis on remonte le contenu si validation
  if (validate) {
    const sel = variableSelection.value?.getSelected?.()
      ?? variableSelection.value?.getSelectedUris?.()
      ?? []
    emit('onValidate', sel)
  } else {
    emit('onClose')
  }
}

function selectItem(row: any) {
  variableSelection.value?.onItemSelected?.(row)
}
function unSelect(row: any) {
  variableSelection.value?.onItemUnselected?.(row)
}
function setInitiallySelectedItems(items: Array<any>) {
  variableSelection.value?.setInitiallySelectedItems?.(items)
}
function refresh() {
  variableSelection.value?.refresh?.()
}
function refreshWithKeepingSelection() {
  variableSelection.value?.refreshWithKeepingSelection?.()
}

function applySelectionToPage() {
  variableSelection.value?.applySelectionToPage?.()
}

defineExpose({
  show,
  hide,
  selectItem,
  unSelect,
  setInitiallySelectedItems,
  refresh,
  refreshWithKeepingSelection,
  applySelectionToPage
})
</script>

<style scoped>
.modal-title {
  display: flex;
  align-items: center;
  gap: .5rem;
}
.modal-body {
  padding: 8px 0;
}
</style>
