<template>
  <div>
    <p v-if="!imports.length" class="text-body-secondary mb-0">{{ $t('GraphImport.stored.none') }}</p>
    <n-list v-else bordered hoverable>
      <n-list-item v-for="stored in imports" :key="stored.session_id">
        <div class="d-flex align-items-center justify-content-between gap-3">
          <div>
            <div class="fw-semibold">{{ stored.title }}</div>
            <div class="text-body-secondary small">
              {{ $t('GraphImport.stored.updated', { date: formatDate(stored.updated_at) }) }}
              · {{ $t('GraphImport.stored.expires', { date: formatDate(stored.expires_at) }) }}
            </div>
          </div>
          <n-space :size="8" :wrap="false">
            <n-button
              size="small"
              type="primary"
              :loading="resuming === stored.session_id"
              @click="emit('resume', stored.session_id)"
            >
              {{ $t('GraphImport.stored.resume') }}
            </n-button>
            <n-button size="small" tertiary type="error" @click="emit('remove', stored.session_id)">
              {{ $t('GraphImport.stored.delete') }}
            </n-button>
          </n-space>
        </div>
      </n-list-item>
    </n-list>
  </div>
</template>

<script setup lang="ts">
import type { StoredImportDTO } from '../lib'

//#region Public
const emit = defineEmits<{
  resume: [sessionId: string]
  remove: [sessionId: string]
}>()

defineProps<{
  imports: StoredImportDTO[]
  /** The import being read back, whose button shows it. */
  resuming?: string
}>()
//#endregion

//#region Private
//#region Methods
/**
 * The server writes dates as ISO text; the generated model types them as numbers, which `Date` reads
 * as well.
 */
function formatDate(value?: string | number): string {
  return value ? new Date(value).toLocaleString() : ''
}
//#endregion
//#endregion
</script>
