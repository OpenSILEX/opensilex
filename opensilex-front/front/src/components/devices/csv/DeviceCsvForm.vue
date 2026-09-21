<template>
  <div>
    <opensilex-OntologyCsvImporter
      ref="importForm"
      :baseType="$opensilex.Oeso.DEVICE_TYPE_URI"
      :validateCSV="validateCSV"
      :uploadCSV="uploadCSV"
      :title="t('component.device.import.title')"
      :successImportMsg="t('component.device.import.success-message')"
      @csvImported="onCsvImported"
    >
      <template #icon>
        <opensilex-Icon icon="bi#bi-bullseye" class="icon-title" />
      </template>

      <template #help>
        <opensilex-DeviceImportHelp />
      </template>

      <template #generator>
        <div class="col-12 col-md-2">
          <opensilex-Button
            class="me-2 greenThemeColor"
            :small="false"
            icon
            :label="t('component.common.import-files.generate-template')"
            @click="showTemplateGenerator"
          />

          <opensilex-OntologyCsvTemplateGenerator
            ref="templateGenerator"
            :baseType="$opensilex.Oeso.DEVICE_TYPE_URI"
            templatePrefix="device"
            :typePlaceholder="t('component.device.form-placeholders')"
            :uriHelp="t('component.device.help-messages.uri-help')"
            :uriExample="DEVICE_URI_EXAMPLE"
            :typeHelp="t('component.device.help-messages.type-help')"
            :typeExample="DEVICE_TYPE_EXAMPLE"
          />
        </div>
      </template>
    </opensilex-OntologyCsvImporter>
  </div>
</template>

<script setup lang="ts">
import { inject, ref } from 'vue'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import { useI18n } from 'vue-i18n'

//#region Emits
const emit = defineEmits<{
  (e: 'csvImported', response: any): void
}>();
//#endregion

//#region Constant values
const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!;
const { t } = useI18n();

const DEVICE_URI_EXAMPLE: string = "http://opensilex.org/id/device/rasperry_pi_4B";
const DEVICE_TYPE_EXAMPLE: string = "vocabulary:SensingDevice";
//#endregion

const templateGenerator = ref<any>(null)
const importForm = ref<any>(null)

function show() {
  importForm.value?.show?.()
}

function showTemplateGenerator() {
  templateGenerator.value?.show?.()
}

function validateCSV(csvFile: File) {
  return $opensilex.uploadFileToService(
    '/core/devices/import_validation',
    {
      description: {},
      file: csvFile
    },
    null,
    false
  )
}

function uploadCSV(validationToken: string, csvFile: File) {
  return $opensilex.uploadFileToService(
    '/core/devices/import',
    {
      description: {
        validationToken
      },
      file: csvFile
    },
    null,
    false
  )
}

function onCsvImported(response: any) {
  emit('csvImported', response)
}

defineExpose({
  show
})
</script>

<style scoped>
</style>
