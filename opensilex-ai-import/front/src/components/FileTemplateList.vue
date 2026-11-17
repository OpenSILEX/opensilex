<template>
  <!--
    The templates the user may use: what they said of a file, kept to be proposed for the next one
    of the same shape. Their own can be renamed, shared and deleted; those shared with them can only
    be used. A template leaves as CSVW, which this page reads back, or as RML, for tools that run it.
  -->
  <opensilex-Card label="AiImport.templates.title" :no-footer="true">
    <template #rightHeader>
      <input ref="fileInput" type="file" class="d-none" accept=".json,.csvm.json" @change="onFileChosen" />
      <n-button size="small" secondary @click="fileInput?.click()">
        {{ t('AiImport.templates.import') }}
      </n-button>
    </template>
    <template #body>
      <p class="text-body-secondary small">{{ t('AiImport.templates.hint') }}</p>
      <p v-if="!templates.length" class="text-body-secondary mb-0">{{ t('AiImport.templates.empty') }}</p>
      <n-list v-else>
        <n-list-item v-for="template in templates" :key="template.id">
          <n-space align="center" justify="space-between">
            <n-space align="center">
              <n-input
                v-if="template.is_mine"
                size="small"
                :value="template.name"
                :aria-label="t('AiImport.templates.name')"
                @change="(name: string) => onRename(template, name)"
              />
              <strong v-else>{{ template.name }}</strong>
              <span class="text-body-secondary small">
                {{ (template.sheets ?? []).join(', ') }} · {{ template.author_name }}
              </span>
              <n-tag v-if="template.shared" size="small" type="info">{{ t('AiImport.templates.shared') }}</n-tag>
            </n-space>
            <n-space align="center">
              <n-switch
                v-if="template.is_mine"
                size="small"
                :value="template.shared"
                :title="t('AiImport.templates.share')"
                @update:value="(shared: boolean) => update(template.id ?? '', { shared })"
              />
              <n-button size="tiny" secondary @click="download(template, 'csvw')">CSVW</n-button>
              <n-button size="tiny" secondary @click="download(template, 'rml')">RML</n-button>
              <n-button v-if="template.is_mine" size="tiny" quaternary type="error"
                        @click="remove(template.id ?? '')">
                {{ t('AiImport.templates.delete') }}
              </n-button>
            </n-space>
          </n-space>
        </n-list-item>
      </n-list>
    </template>
  </opensilex-Card>
</template>

<script setup lang="ts">
import { inject, onMounted, useTemplateRef } from 'vue'
import type { FileTemplateDTO } from '../lib'
import type { OpenSilexPlugin } from '../opensilexPlugin'
import { useFileTemplates } from '../useFileTemplates'

//#region Private

//#region Plugins & services
const $opensilex = inject<OpenSilexPlugin>('$opensilex')!
const { templates, load, update, remove, importCsvw, download } = useFileTemplates($opensilex)
//#endregion

//#region Template refs
const fileInput = useTemplateRef<HTMLInputElement>('fileInput')
//#endregion

//#region Hooks & watchers
onMounted(load)
//#endregion

//#region Event handlers
async function onFileChosen(event: Event) {
  const input = event.target as HTMLInputElement
  const chosen = input.files && input.files.length ? input.files[0] : null
  input.value = ''
  if (chosen) {
    await importCsvw(chosen)
  }
}

function onRename(template: FileTemplateDTO, name: string) {
  if (name.trim() && name !== template.name) {
    update(template.id ?? '', { name })
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
