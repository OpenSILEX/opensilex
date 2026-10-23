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
        <i class="ik ik-search mr-1"></i>
        {{ $t('GermplasmList.selectLabel') }}
      </div>
    </template>

    <div class="card">
      <GermplasmList
          ref="germplasmSelection"
          :isSelectable="true"
          :noActions="true"
          :pageSize="5"
          :noUpdateURL="true"
          :experimentUri="experiment"
          @select="$emit('select', $event)"
          @unselect="$emit('unselect', $event)"
          @selectall="$emit('selectall', $event)"
      ></GermplasmList>
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
import GermplasmList from "@/components/germplasm/GermplasmList.vue";

defineProps<{
  /**
   * Set an experiment uri, in this case we don't show experiment filter and show only germplasms of this experiment
   */
  experiment?: string
}>()

const germplasmSelection = ref<any>(null)
const visible = ref(false)

const emit = defineEmits<{
  onValidate: [value: any[]]
  onClose: []
  shown: []
  hide: []
  select: [value: any]
  unselect: [value: any]
  selectall: [value: any]
}>()

function selectItem(row) {
  germplasmSelection.value?.onItemSelected?.(row)
}

function unSelect(row) {
  germplasmSelection.value?.onItemUnselected?.(row)
}

function setInitiallySelectedItems(initiallySelectedItems: Array<any>) {
  germplasmSelection.value?.setInitiallySelectedItems?.(initiallySelectedItems)
}

async function show() {
  visible.value = true
  await nextTick()
  emit('shown')
}

function refresh() {
  germplasmSelection.value?.refresh?.()
}

function hide(validate: boolean) {
  visible.value = false
  if (validate) {
    emit('onValidate', germplasmSelection.value?.getSelected?.() ?? [])
  } else {
    emit('onClose')
  }
}

defineExpose({
  show,
  hide,
  selectItem,
  unSelect,
  setInitiallySelectedItems,
  refresh
})
</script>

<style scoped lang="scss">
.modal-title {
  display: flex;
  align-items: center;
  gap: .5rem;
}
</style>
