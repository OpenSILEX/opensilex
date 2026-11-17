<template>
  <!--
    The files of the conversation, in the order they were added. An import is often several files —
    the plan in one, the measurements in another — and each brings its sheets.
  -->
  <opensilex-Card label="AiImport.advanced.files.title" :no-footer="true">
    <template #rightHeader>
      <input ref="fileInput" type="file" class="d-none" :accept="accept" @change="onFileChosen" />
      <n-button size="small" secondary :loading="adding" @click="fileInput?.click()">
        {{ t('AiImport.advanced.files.add') }}
      </n-button>
    </template>
    <template #body>
      <n-list>
        <n-list-item v-for="file in files" :key="file.id">
          <n-space align="center">
            <code>{{ file.name }}</code>
            <span class="text-body-secondary small">
              {{ t('AiImport.advanced.files.sheets') }} {{ (file.sheets ?? []).join(', ') }}
            </span>
            <n-tag v-if="file.charset" size="small" type="warning" :title="t('AiImport.advanced.files.charsetTitle')">
              {{ file.charset }}
            </n-tag>
            <n-button v-if="files.length > 1" size="tiny" quaternary type="error" :disabled="adding"
                      @click="onRemoveClick(file)">
              {{ t('AiImport.advanced.files.remove') }}
            </n-button>
          </n-space>
        </n-list-item>
      </n-list>
    </template>
  </opensilex-Card>
</template>

<script setup lang="ts">
import { inject, ref, useTemplateRef } from 'vue'
import type { AiImportService, SessionFileDTO } from '../lib'
import { ACCEPTED_EXTENSIONS, AI_IMPORT_SERVICE, type OpenSilexPlugin } from '../opensilexPlugin'

//#region Public
const emit = defineEmits<{
  /** The files changed, so the sheets, the description and the report did too. */
  changed: [files: SessionFileDTO[]]
}>()

const props = defineProps<{
  sessionId: string
  files: SessionFileDTO[]
}>()
//#endregion

//#region Private

//#region Plugins & services
const $opensilex = inject<OpenSilexPlugin>('$opensilex')!
//#endregion

//#region Template refs
const fileInput = useTemplateRef<HTMLInputElement>('fileInput')
//#endregion

//#region Data & Computed
const accept = ACCEPTED_EXTENSIONS.map((extension) => '.' + extension).join(',')
const adding = ref(false)
//#endregion

//#region Event handlers
async function onFileChosen(event: Event) {
  const input = event.target as HTMLInputElement
  const chosen = input.files && input.files.length ? input.files[0] : null
  input.value = ''
  if (!chosen) {
    return
  }
  const extension = chosen.name.split('.').pop()?.toLowerCase() ?? ''
  if (!ACCEPTED_EXTENSIONS.includes(extension)) {
    $opensilex.errorHandler(new Error($opensilex.$i18n.t('AiImport.upload.wrongType')))
    return
  }
  adding.value = true
  try {
    // Multipart, so it goes through the upload helper rather than the generated client.
    const response = await $opensilex.uploadFileToService(
      '/ai-import/sessions/' + encodeURIComponent(props.sessionId) + '/files',
      { file: chosen }
    )
    emit('changed', (response.result ?? response) as SessionFileDTO[])
  } catch (error) {
    $opensilex.errorHandler(error)
  } finally {
    adding.value = false
  }
}

async function onRemoveClick(file: SessionFileDTO) {
  try {
    const http = await $opensilex
      .getService<AiImportService>(AI_IMPORT_SERVICE)
      .removeFile(props.sessionId, file.id ?? '')
    emit('changed', http.response.result)
  } catch (error) {
    $opensilex.errorHandler(error)
  }
}
//#endregion

//#region Methods
function t(key: string): string {
  return String($opensilex.$i18n.t(key))
}
//#endregion

//#endregion
</script>
