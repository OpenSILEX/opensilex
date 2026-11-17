<template>
  <!--
    Everything the user says of a file nobody has a template for: which files, which experiment,
    what each sheet and column is. The page owns the conversation; this panel owns the description
    and tells the page when the report must be read again.
  -->
  <div class="d-flex flex-column gap-3">
    <opensilex-ai-import-SessionFilesPanel
      :session-id="sessionId"
      :files="files"
      @changed="onFilesChanged"
    />


    <n-space v-if="description?.sheets?.length" align="center">
      <n-checkbox v-model:checked="remember">{{ t('AiImport.templates.remember') }}</n-checkbox>
      <n-button size="small" secondary @click="onDownloadClick">{{ t('AiImport.templates.downloadDescription') }}</n-button>
    </n-space>

    <opensilex-ai-import-SheetDescriptionPanel
      v-if="description"
      :sheets="description.sheets ?? []"
      :kinds="description.available_kinds ?? []"
      :roles="description.available_roles ?? []"
      :properties-of="propertiesOf"
      :busy="busy"
      @describe="onDescribe"
      @need-properties="loadProperties"
    />

    <!--
      Under the sheets, and only while no sheet is validated as a sheet of experiments: the file then
      says which experiment it is, row by row. Otherwise the same experiment serves the whole file,
      chosen here or created with the platform's form.
    -->
    <opensilex-Card v-if="showExperiment" label="AiImport.advanced.experiment.title" :no-footer="true">
      <template #body>
        <p class="text-body-secondary small">{{ t('AiImport.advanced.experiment.hint') }}</p>
        <n-space align="end">
          <opensilex-ExperimentSelector
            class="ai-import-experiment"
            :experiments="experiment"
            :label="t('AiImport.advanced.experiment.label')"
            :required="false"
            @update:experiments="onExperimentChange"
          />
          <n-button text type="primary" @click="emit('createExperiment')">
            {{ t('AiImport.advanced.experiment.create') }}
          </n-button>
          <n-button v-if="experiment" size="small" secondary @click="onDeclareFactors">
            {{ t('AiImport.advanced.experiment.declareFactors') }}
          </n-button>
        </n-space>
      </template>
    </opensilex-Card>
  </div>
</template>

<script setup lang="ts">
import { computed, inject, onMounted, ref, watch } from 'vue'
import type { SessionFileDTO, SheetDescriptionUpdateDTO } from '../lib'
import type { OpenSilexPlugin } from '../opensilexPlugin'
import { useFileDescription } from '../useFileDescription'

//#region Public
const emit = defineEmits<{
  /** The description or the files changed: the report, the mapping and the files are to be read again. */
  changed: []
  filesChanged: [files: SessionFileDTO[]]
  /** The user wants a new experiment for the files: the page opens the platform's form. */
  createExperiment: []
  /** The user wants the factors of the experiment declared with the platform's factor form. */
  declareFactors: [experiment: string]
}>()

const props = defineProps<{
  sessionId: string
  files: SessionFileDTO[]
  /** Changes whenever the report is recomputed, which a creation can do to the sheets. */
  reportStamp?: string
}>()
//#endregion

//#region Private

//#region Plugins & services
const $opensilex = inject<OpenSilexPlugin>('$opensilex')!
const { description, busy, load, apply, loadProperties, propertiesOf } = useFileDescription(
  $opensilex,
  () => props.sessionId
)
//#endregion

//#region Data & Computed
const experiment = computed(() => description.value?.experiment ?? undefined)

/**
 * The experiment card is shown while no sheet is validated as a sheet of experiments, when a sheet
 * needs an experiment or one is already chosen, so that it can be changed.
 */
const showExperiment = computed(() => {
  const sheets = description.value?.sheets ?? []
  const describedAsExperiments = sheets.some((sheet) => sheet.kind === 'EXPERIMENTS' && sheet.kind_chosen)
  return !describedAsExperiments && (!!experiment.value || sheets.some((sheet) => sheet.needs_experiment))
})

/**
 * Whether what the user says of the sheets is kept as a template for the next files of the same
 * shape: yes unless they say otherwise.
 */
const remember = ref(true)
//#endregion

//#region Hooks & watchers
onMounted(load)
watch(() => props.reportStamp, load)
//#endregion

//#region Event handlers
async function onDescribe(change: SheetDescriptionUpdateDTO) {
  if (await apply({ sheets: [change], remember: remember.value })) {
    emit('changed')
  }
}

async function onExperimentChange(uri: string | string[] | undefined) {
  const chosen = Array.isArray(uri) ? uri[0] : uri
  if (chosen && chosen !== description.value?.experiment && (await apply({ experiment: chosen, sheets: [] }))) {
    emit('changed')
  }
}

/**
 * Downloads the description as it stands, as CSVW: whether or not it is kept as a template.
 */
async function onDownloadClick() {
  try {
    await $opensilex.downloadFilefromService(
      '/ai-import/sessions/' + encodeURIComponent(props.sessionId) + '/sheet_descriptions/csvw',
      'description',
      'csvm.json'
    )
  } catch (error) {
    $opensilex.errorHandler(error)
  }
}

function onDeclareFactors() {
  if (experiment.value && !warnRefusedFactorSheets()) {
    emit('declareFactors', experiment.value)
  }
}

async function onFilesChanged(files: SessionFileDTO[]) {
  emit('filesChanged', files)
  await load()
  emit('changed')
}
//#endregion

//#region Methods
function t(key: string): string {
  return String($opensilex.$i18n.t(key))
}

/**
 * The sheets of levels that are not read, most often for want of a factor name: declaring the factors
 * without them would open an empty form, so the user is told which sheet to complete instead.
 *
 * @return whether such a sheet was found and the user told
 */
function warnRefusedFactorSheets(): boolean {
  const refused = (description.value?.sheets ?? []).filter((sheet) => sheet.kind === 'FACTORS' && !sheet.readable)
  refused.forEach((sheet) =>
    $opensilex.showErrorToast(String($opensilex.$i18n.t('AiImport.advanced.sheets.factorSheetRefused', { sheet: sheet.sheet })))
  )
  return refused.length > 0
}

/**
 * Makes an experiment the one of the files, such as one just created with the platform's form.
 */
async function chooseExperiment(uri: string) {
  await onExperimentChange(uri)
}
//#endregion

//#endregion

defineExpose({ chooseExperiment, warnRefusedFactorSheets })
</script>

<style scoped lang="scss">
.ai-import-experiment {
  min-width: 24rem;
}
</style>
