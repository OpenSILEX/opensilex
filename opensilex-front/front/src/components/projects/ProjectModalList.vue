<template>
  <n-modal
    v-model:show="visible"
    :mask-closable="false"
    :preset="'dialog'"
    :show-icon="false"
    :style="{ width: '1400px', maxWidth: '95vw' }"
    @after-leave="$emit('hide')"
  >
    <template #header>
      <div class="modal-title">
        <i class="bi bi-search"></i>
        {{ $t('component.project.filter-description') }}
      </div>
    </template>

    <div class="modal-body">
      <ProjectList
        ref="projectSelection"
        :isSelectable="true"
        :noActions="true"
        :pageSize="5"
        :noUpdateURL="true"
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
import { ref, nextTick } from 'vue'
import { NModal, NButton, NSpace } from 'naive-ui'
import ProjectList from "@/components/projects/ProjectList.vue";

const projectSelection = ref<any>(null)
const visible = ref(false)

const emit = defineEmits<{
  (e: 'onValidate', value: any[]): void
  (e: 'onClose'): void
  (e: 'shown'): void
  (e: 'hide'): void
  (e: 'select', value: any): void
  (e: 'unselect', value: any): void
  (e: 'selectall', value: any): void
}>()

async function show() {
  visible.value = true
  await nextTick()
  emit('shown')
}

function hide(validate: boolean) {
  visible.value = false
  if (validate) {
    emit('onValidate', projectSelection.value?.getSelected?.() ?? [])
  } else {
    emit('onClose')
  }
}

function selectItem(row: any) {
  projectSelection.value?.onItemSelected?.(row)
}

function unSelect(row: any) {
  projectSelection.value?.onItemUnselected?.(row)
}

function refresh() {
  projectSelection.value?.refresh?.()
}

defineExpose({
  show,
  hide,
  selectItem,
  unSelect,
  refresh
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
