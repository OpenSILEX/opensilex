<template>
  <NModal
    v-model:show="internalShow"
    :show="internalShow"
    preset="card"
    title="Data Visualization"
    style="width: 800px; height: 600px"
    :bordered="true"
    :closable="true"
  >
    <div class="chart-modal-content">
      <p>Chart visualization will be displayed here.</p>
    </div>
  </NModal>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { NModal } from 'naive-ui'

//#region Public
const props = withDefaults(
  defineProps<{
    show: boolean
    selectedFeatures: Feature[]
    experimentUri: string
  }>(),
  {
    selectedFeatures: () => [],
  }
)

interface Feature {
  properties: {
    uri?: string
    name?: string
    type?: string
    nature?: string
    [key: string]: unknown
  }
  geometry?: {
    coordinates?: unknown
    type?: string
  }
}

const emit = defineEmits<{
  'update:show': [value: boolean]
}>()

//#endregion

//#region Private
const internalShow = ref<boolean>(props.show)

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
//#endregion
</script>

<style scoped lang="scss">
.chart-modal-content {
  padding: 10px 0;
  height: 500px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
}
</style>