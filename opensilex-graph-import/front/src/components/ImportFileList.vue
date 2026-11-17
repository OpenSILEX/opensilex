<template>
  <div>
    <n-list bordered>
      <n-list-item v-for="file in files" :key="file.id">
        <div class="d-flex align-items-center justify-content-between gap-3">
          <div>
            <code>{{ file.name }}</code>
            <n-space :size="4" class="mt-1">
              <n-tag v-for="sheet in file.sheets" :key="sheet" size="small" :bordered="false">{{ sheet }}</n-tag>
            </n-space>
          </div>
          <n-button
            v-if="files.length > 1"
            size="small"
            tertiary
            type="error"
            :disabled="busy"
            @click="emit('remove', file.id)"
          >
            {{ $t('GraphImport.files.remove') }}
          </n-button>
        </div>
      </n-list-item>
    </n-list>
    <FileDrop
      class="mt-2"
      compact
      label="GraphImport.files.add"
      :busy="busy"
      @chosen="(file: File) => emit('add', file)"
    />
  </div>
</template>

<script setup lang="ts">
import type { GraphImportFileDTO } from '../lib'
import FileDrop from './FileDrop.vue'

//#region Public
const emit = defineEmits<{
  add: [file: File]
  remove: [fileId: string]
}>()

defineProps<{
  files: GraphImportFileDTO[]
  busy?: boolean
}>()
//#endregion
</script>
