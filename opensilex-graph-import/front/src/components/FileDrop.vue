<template>
  <div
    class="graph-import-drop"
    :class="{ 'graph-import-drop--over': dragging, 'graph-import-drop--compact': compact }"
    @dragover.prevent="dragging = true"
    @dragleave.prevent="dragging = false"
    @drop.prevent="onDrop"
  >
    <input ref="fileInput" type="file" class="d-none" :accept="acceptAttribute" @change="onFileInputChange" />
    <n-button :secondary="!compact" :size="compact ? 'small' : 'medium'" :loading="busy" @click="fileInput?.click()">
      {{ $t(label) }}
    </n-button>
    <span class="text-body-secondary small">{{ $t('GraphImport.upload.drop') }}</span>
  </div>
</template>

<script setup lang="ts">
import { ref, useTemplateRef } from 'vue'
import { ACCEPTED_EXTENSIONS } from '../opensilexPlugin'

//#region Public
const emit = defineEmits<{
  chosen: [file: File]
}>()

withDefaults(
  defineProps<{
    /** Translation key of the button. */
    label?: string
    busy?: boolean
    /** A smaller zone, for adding a file to an open import. */
    compact?: boolean
  }>(),
  { label: 'GraphImport.upload.choose', busy: false, compact: false }
)
//#endregion

//#region Private
//#region Template refs
const fileInput = useTemplateRef<HTMLInputElement>('fileInput')
//#endregion

//#region Data & Computed
const dragging = ref(false)
const acceptAttribute = ACCEPTED_EXTENSIONS.map((extension) => '.' + extension).join(',')
//#endregion

//#region Event handlers
function onDrop(event: DragEvent) {
  dragging.value = false
  const dropped = event.dataTransfer?.files?.[0]
  if (dropped) {
    emit('chosen', dropped)
  }
}

function onFileInputChange(event: Event) {
  const input = event.target as HTMLInputElement
  const chosen = input.files?.[0]
  if (chosen) {
    emit('chosen', chosen)
  }
  // The same file can be chosen again after a refusal.
  input.value = ''
}
//#endregion
//#endregion
</script>

<style scoped lang="scss">
.graph-import-drop {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 1.5rem;
  border: 2px dashed var(--bs-border-color, #ced4da);
  border-radius: 0.5rem;
  transition: background-color 0.15s;
}

.graph-import-drop--compact {
  padding: 0.5rem 0.75rem;
  border-width: 1px;
}

.graph-import-drop--over {
  background-color: var(--bs-tertiary-bg, #f1f3f5);
}
</style>
