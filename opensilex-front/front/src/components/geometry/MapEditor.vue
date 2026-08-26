<template>
  <div class="map-editor">
    <div v-if="editingMode" class="editing-indicator">
      <span>Drawing mode active. Click to finish polygon.</span>
      <NButton size="small" type="error" @click="cancelDrawing">
        Cancel
      </NButton>
    </div>
  </div>
</template>

<script setup lang="ts">
import { NButton } from 'naive-ui'

//#region Public
const props = withDefaults(
  defineProps<{
    experimentUri: string
    editingMode: boolean
  }>(),
  {}
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
  'update:editingMode': [value: boolean]
  areaCreated: [area: Feature]
  areaUpdated: [area: Feature]
}>()

//#endregion

//#region Private
function cancelDrawing() {
  emit('update:editingMode', false)
}
//#endregion
</script>

<style scoped lang="scss">
.map-editor {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 50;
}

.editing-indicator {
  position: absolute;
  top: 10px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(255, 255, 255, 0.9);
  padding: 10px 20px;
  border-radius: 5px;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.2);
  display: flex;
  align-items: center;
  gap: 10px;
  pointer-events: auto;
  font-size: 12px;
}
</style>