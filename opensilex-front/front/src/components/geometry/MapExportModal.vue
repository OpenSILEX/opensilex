<template>
  <NModal
    v-model:show="internalShow"
    :show="internalShow"
    preset="card"
    title="Export Map"
    style="width: 400px"
    :bordered="true"
  >
    <div class="export-modal-content">
      <p>{{ t('MapView.save-confirmation') }}</p>

      <NSpace vertical :size="10">
        <NButton type="primary" block @click="onExport('pdf')">
          PDF
        </NButton>
        <NButton type="info" block @click="onExport('png')">
          PNG
        </NButton>
      </NSpace>
    </div>
  </NModal>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { NModal, NButton, NSpace } from 'naive-ui'
import { useI18n } from 'vue-i18n'

//#region Public
const props = withDefaults(
  defineProps<{
    show: boolean
    experimentUri: string
  }>(),
  {}
)

const emit = defineEmits<{
  'update:show': [value: boolean]
  export: [format: string]
}>()

//#endregion

//#region Private
const internalShow = ref<boolean>(props.show)
const { t } = useI18n()

watch(
  () => props.show,
  (newVal: boolean) => {
    internalShow.value = newVal
  }
)

watch(
  internalShow,
  (newVal: boolean) => {
    emit('update:show', newVal)
  }
)

function onExport(format: string) {
  emit('export', format)
  internalShow.value = false
}
//#endregion
</script>

<style scoped lang="scss">
.export-modal-content {
  padding: 10px 0;
}
</style>