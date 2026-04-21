<template>
  <div>
    <opensilex-OntologyCsvImporter
      ref="importForm"
      :baseType="baseType"
      :validateCSV="validateCSV"
      :uploadCSV="uploadCSV"
      :successImportMsg="t('EventCsvForm.multiple-insert')"
      :title="title"
      @csvImported="onCsvImported"
    >
      <template #icon>
        <opensilex-Icon icon="bi#bi-bullseye" class="icon-title" />
      </template>

      <template #help>
        <opensilex-EventHelpTableView :isMove="isMove" />
      </template>

      <template #generator>
        <div class="col-12 col-md-2">
          <opensilex-Button
            class="mr-2 greenThemeColor"
            :small="false"
            @click="showTemplateGenerator"
            icon
            :label="t('EventCsvForm.generate-template')"
          />
          <opensilex-GenerateEventTemplate
            ref="templateGenerator"
            :targets="targets"
            :isMove="isMove"
          />
        </div>
      </template>
    </opensilex-OntologyCsvImporter>
  </div>
</template>

<script setup lang="ts">
import { computed, inject, ref } from 'vue'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import { useI18n } from 'vue-i18n'

const props = withDefaults(defineProps<{
  targets?: string[]
  isMove?: boolean
}>(), {
  targets: () => [],
  isMove: false
})

const emit = defineEmits<{
  (e: 'csvImported', response: any): void
}>()

const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')
const { t } = useI18n()

const templateGenerator = ref<any>(null)
const importForm = ref<any>(null)
const nbLinesImported = ref(0)

const baseType = computed(() =>
  props.isMove
    ? $opensilex.Oeev.MOVE_TYPE_URI
    : $opensilex.Oeev.EVENT_TYPE_URI
)

const title = computed(() =>
  props.isMove
    ? t('EventCsvForm.move-csv-import-title')
    : t('EventCsvForm.csv-import-title')
)

function show() {
  importForm.value?.show?.()
}

function showTemplateGenerator() {
  templateGenerator.value?.show?.()
}

function validateCSV(csvFile: File) {
  const path = props.isMove
    ? '/core/events/moves/import_validation'
    : '/core/events/import_validation'

  return $opensilex.uploadFileToService(
    path,
    { description: {}, file: csvFile },
    null,
    false
  )
}

function uploadCSV(validationToken: string, csvFile: File) {
  const path = props.isMove
    ? '/core/events/moves/import'
    : '/core/events/import'

  return $opensilex.uploadFileToService(
    path,
    { description: { validationToken }, file: csvFile },
    null,
    false
  )
}

function onCsvImported(response: any) {
  nbLinesImported.value = response?.result?.nb_lines_imported ?? 0
  emit('csvImported', response)
}

defineExpose({
  show
})
</script>

<style scoped>
</style>

<i18n>
en:
  EventCsvForm:
    csv-import-title: "Event(s) CSV import"
    invalidDate: Start date cannot be later than End date
    move-csv-import-title: "Move CSV import"
    generate-template: Generate template
    multiple-insert: event(s) registered.
fr:
  EventCsvForm:
    csv-import-title: "Import CSV des événements"
    invalidDate: La date de début ne peut être ultérieure à celle de fin
    move-csv-import-title: "Import CSV des déplacements"
    generate-template: Générer un gabarit
    multiple-insert: événement(s) enregistré(s)
</i18n>