<template>
  <div class="container-fluid">
    <opensilex-PageContent>
      <n-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-placement="top"
        :show-require-mark="true"
        size="small"
      >
        <!-- type -->
        <n-form-item path="rdf_type" ref="rdfTypeItem">
          <opensilex-TypeForm
            :key="form.rdf_type ?? 'no-type'"
            v-model:type="form.rdf_type"
            :baseType="Oeso.DATAFILE_TYPE_URI"
            :helpMessage="t('DataFileForm.type-help')"
            :required="true"
          />
        </n-form-item>
        <br>

        <template v-if="form.rdf_type">
            <!-- format -->
            <n-form-item label="Format">
            <n-radio-group v-model:value="selectedFormat">
                <n-space>
                <n-radio
                    v-for="option in formats"
                    :key="option.id"
                    :value="option.id"
                >
                    {{ option.label }}
                </n-radio>
                </n-space>
            </n-radio-group>
            </n-form-item>

            <!-- provenance -->
            <n-form-item path="provenance" ref="provenanceItem">
            <opensilex-ProvenanceSelector
                ref="provenanceSelector"
                v-model:provenances="form.provenance"
                :label="t('DataFileForm.provenance')"
                :multiple="false"
                @clear="reset"
                :actionHandler="
                user.hasCredential(credentials.CREDENTIAL_PROVENANCE_MODIFICATION_ID)
                    ? showProvenanceCreateForm
                    : undefined
                "
                :required="true"
            />
            </n-form-item>

            <!-- experiment -->
            <n-form-item v-if="!tabExperimentMode" path="experiments" ref="experimentsItem">
            <opensilex-ExperimentSelector
                :label="t('DataFileForm.experiment')"
                v-model:experiments="form.experiments"
                :required="true"
            />
            </n-form-item>
            <br>

            <!-- File source -->
            <n-form-item label="Source">
            <n-radio-group v-model:value="selectedSource">
                <n-space>
                <n-radio
                    v-for="option in sources"
                    :key="option.id"
                    :value="option.id"
                >
                    {{ option.label }}
                </n-radio>
                </n-space>
            </n-radio-group>
            </n-form-item>

            <!-- File -->
            <n-form-item v-if="!editMode && selectedSource === 'file'" path="file" ref="fileItem">
            <opensilex-FileInputForm
                v-model:file="form.file"
                :label="t('DataFileForm.fileDX')"
                type="file"
                :browse-text="t('DataFileForm.browse')"
                :required="true"
                rules="size:100000"
                :helpMessage="helpMessageFile"
            />
            </n-form-item>

            <!-- External source -->
            <n-form-item v-if="!editMode && selectedSource === 'external'" path="file" ref="externalSourceItem">
            <opensilex-InputForm
                :label="t('DataFileForm.external-source')"
                type="text"
                v-model:value="form.file"
                :required="true"
            />
            </n-form-item>

            <!-- date -->
            <n-form-item v-if="selectedFormat === 'all' || selectedSource === 'external'" path="date" ref="dateItem">
            <opensilex-DateForm
                v-model:value="form.date"
                :helpMessage="t('DataFileForm.date-help')"
                :label="t('DataFileForm.date')"
                :required="true"
            />
            </n-form-item>

            <!-- targets -->
            <n-form-item v-if="selectedFormat === 'all' || selectedSource === 'external'" path="target">
            <opensilex-InputForm
                v-model:value="form.target"
                :baseType="Oeso.targets"
                :label="t('DataFileForm.targets')"
                :helpMessage="t('DataFileForm.targets-help')"
                type="text"
            />
            </n-form-item>
        </template>
      </n-form>

      <opensilex-ModalForm
        ref="provenanceForm"
        component="opensilex-ProvenanceForm"
        :createTitle="t('DataFileForm.add-provenance')"
        :editTitle="t('DataFileForm.update-provenance')"
        icon="fa#seedling"
        modalSize="lg"
        @hide="validateProvenanceForm = true"
        :initForm="initForm"
        :successMessage="successMessage"
        :validationDisabled="validateProvenanceForm"
        @onCreate="afterCreateProvenance"
      />
    </opensilex-PageContent>
  </div>
</template>

<script setup lang="ts">
import { computed, inject, ref, nextTick, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useStore } from 'vuex'
import {
  NForm,
  NFormItem,
  NRadio,
  NRadioGroup,
  NSpace
} from 'naive-ui'
import type { FormInst, FormItemInst } from 'naive-ui'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import type HttpResponse from '@/lib/HttpResponse'
import type {
  OpenSilexResponse,
  ScientificObjectsService
} from 'opensilex-core/index'
import type { DataService } from 'opensilex-core/api/data.service'

const emit = defineEmits<{
  (e: 'onCreate', form: any): void
}>()

const props = withDefaults(defineProps<{
  data?: any
  editMode?: boolean
  form?: any
}>(), {
  data: undefined,
  editMode: false,
  form: () => ({
    rdf_type: null,
    provenance: null,
    experiments: [],
    file: null,
    date: null,
    target: null
  })
})

const { t } = useI18n()
const store = useStore()

const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const Oeso = $opensilex.Oeso

const serviceOS = $opensilex.getService<ScientificObjectsService>(
  'opensilex.ScientificObjectsService'
)

const selectedProvenance = ref<any>(null)
const selectedFormat = ref<'all' | 'DX' | 'CSV'>('DX')
const selectedSource = ref<'file' | 'external'>('file')
const validateProvenanceForm = ref(true)
const provenance = ref<any>(null)

const provenanceForm = ref<any>(null)
const provenanceSelector = ref<any>(null)

const form = props.form

const formRef = ref<FormInst | null>(null)
const rdfTypeItem = ref<FormItemInst | null>(null)
const provenanceItem = ref<FormItemInst | null>(null)
const experimentsItem = ref<FormItemInst | null>(null)
const fileItem = ref<FormItemInst | null>(null)
const externalSourceItem = ref<FormItemInst | null>(null)
const dateItem = ref<FormItemInst | null>(null)

const tabExperimentMode = computed(() => {
  return props.data?.tabExperimentMode === true
})

const user = computed(() => store.state.user)
const credentials = computed(() => store.state.credentials)

const formats = [
  { id: 'all', label: 'All file formats' },
  { id: 'DX', label: 'JCAMP-DX' },
  { id: 'CSV', label: 'CSV for Spectra' }
]

const sources = [
  { id: 'file', label: 'File source' },
  { id: 'external', label: 'External source' }
]

const helpMessageFile = computed(() => {
  if (selectedFormat.value === 'DX') {
    return 'DataFileForm.fileDX-help'
  }

  if (selectedFormat.value === 'CSV') {
    return 'DataFileForm.fileSpectraCSV-help'
  }

  return null
})

const rules = computed(() => ({
  rdf_type: {
    required: true,
    message: t('validations.required_if', {
      _field_: t('DataFileForm.type')
    }),
    trigger: ['change', 'blur']
  },

  provenance: {
    trigger: ['change', 'blur'],
    validator: (_rule: any, value: unknown) => {
      return isFilled(value)
        ? true
        : new Error(
            t('validations.required_if', {
              _field_: t('DataFileForm.provenance')
            }) as string
          )
    }
  },

  experiments: {
    trigger: ['change', 'blur'],
    validator: (_rule: any, value: unknown) => {
      if (tabExperimentMode.value) {
        return true
      }

      return isFilled(value)
        ? true
        : new Error(
            t('validations.required_if', {
              _field_: t('DataFileForm.experiment')
            }) as string
          )
    }
  },

  file: {
    trigger: ['change', 'blur'],
    validator: (_rule: any, value: unknown) => {
      if (selectedSource.value === 'file') {
        return isFilled(value)
          ? true
          : new Error(
              t('validations.required_if', {
                _field_: t('DataFileForm.fileDX')
              }) as string
            )
      }

      if (selectedSource.value === 'external') {
        return isFilled(value)
          ? true
          : new Error(
              t('validations.required_if', {
                _field_: t('DataFileForm.external-source')
              }) as string
            )
      }

      return true
    }
  },

  date: {
    trigger: ['change', 'blur'],
    validator: (_rule: any, value: unknown) => {
      if (selectedFormat.value === 'all' || selectedSource.value === 'external') {
        return isFilled(value)
          ? true
          : new Error(
              t('validations.required_if', {
                _field_: t('DataFileForm.date')
              }) as string
            )
      }

      return true
    }
  }
}))


const successMessage = computed(() => undefined)

function initForm() {
  return {
    uri: null,
    name: null,
    description: null,
    experiments: [],
    activity_type: null,
    activity_start_date: null,
    activity_end_date: null,
    activity_uri: null,
    agentTypes: [],
    agents: [
      {
        uris: [],
        rdf_type: null
      }
    ]
  }
}

function getEmptyForm() {
  return {
    rdf_type: null,
    provenance: null,
    experiments: [],
    file: null,
    date: null,
    target: null
  }
}

function reset() {
  selectedProvenance.value = null
}

function normalizeForm() {
  if (form.date === '') {
    form.date = undefined
  }

  if (form.target === '') {
    form.target = undefined
  }
}

function getExperimentArray() {
  return Array.isArray(form.experiments)
    ? form.experiments
    : form.experiments
      ? [form.experiments]
      : []
}

function getProvenanceUri() {
  if (!form.provenance) {
    return undefined
  }

  if (typeof form.provenance === 'string') {
    return form.provenance
  }

  return form.provenance.uri ?? form.provenance.id
}

async function create(submittedForm: any) {
  normalizeForm()

  const isValid = await validate()

  if (!isValid) {
    return false
  }

  const canCheckTarget =
    selectedSource.value === 'external' || selectedFormat.value === 'all'

  if (canCheckTarget && form.target) {
    const valid = await checkOSinExperiment()

    if (!valid) {
      return false
    }
  }

  if (selectedFormat.value === 'DX' && selectedSource.value === 'file') {
    return createDxFile(submittedForm)
  }

  if (selectedFormat.value === 'CSV' && selectedSource.value === 'file') {
    return createCsvSpectraFile(submittedForm)
  }

  if (selectedFormat.value === 'all' && selectedSource.value === 'file') {
    return createGenericFile(submittedForm)
  }

  if (selectedSource.value === 'external') {
    return createExternalSource(submittedForm)
  }

  return false
}

function createDxFile(submittedForm: any) {
  return $opensilex.uploadFileToService(
    '/core/datafiles/upload-dx',
    {
      rdf_type: form.rdf_type,
      provenance: form.provenance,
      experiments: [form.experiments],
      file: form.file
    },
    null,
    false
  )
    .then((uploadResponse: any) => {
      if (!uploadResponse || !uploadResponse.result) {
        throw new Error('File upload failed.')
      }

      return $opensilex
        .getService<DataService>('opensilex.DataService')
        .postDataFilePaths(uploadResponse.result)
    })
    .then((http: HttpResponse<OpenSilexResponse<any>>) => {
      const uri = http.response.result

      submittedForm.uri = uri

      console.debug('Datafile created', uri)
      emit('onCreate', submittedForm)

      $opensilex.showSuccessToast('File uploaded and processed successfully.')
    })
    .catch(handleCreateError)
}

function createCsvSpectraFile(submittedForm: any) {
  return $opensilex.uploadFileToService(
    '/core/datafiles/upload-spectra-csv',
    {
      rdf_type: form.rdf_type,
      provenance: form.provenance,
      experiments: [form.experiments],
      file: form.file
    },
    null,
    false
  )
    .then((uploadResponse: any) => {
      if (!uploadResponse || !uploadResponse.result) {
        throw new Error('File upload failed.')
      }

      return $opensilex
        .getService<DataService>('opensilex.DataService')
        .postDataFilePaths(uploadResponse.result)
    })
    .then((http: HttpResponse<OpenSilexResponse<any>>) => {
      const uri = http.response.result

      submittedForm.uri = uri

      console.debug('Datafile created', uri)
      emit('onCreate', submittedForm)

      $opensilex.showSuccessToast('File uploaded and processed successfully.')
    })
    .catch(handleCreateError)
}

function createGenericFile(submittedForm: any) {
  return $opensilex.uploadFileToService(
    '/core/datafiles',
    {
      description: {
        rdf_type: form.rdf_type,
        provenance: {
          uri: getProvenanceUri(),
          experiments: getExperimentArray()
        },
        date: form.date,
        target: form.target
      },
      file: form.file
    },
    null,
    false
  )
    .then((http: any) => {
      const uri = http.result

      submittedForm.uri = uri

      console.debug('Datafile created', uri)

      return submittedForm
    })
    .catch(handleCreateError)
}

function createExternalSource(submittedForm: any) {
  return $opensilex
    .getService<DataService>('opensilex.DataService')
    .postDataFilePaths([
      {
        rdf_type: form.rdf_type,
        provenance: {
          uri: getProvenanceUri(),
          experiments: getExperimentArray()
        },
        date: form.date,
        target: form.target,
        relative_path: form.file
      }
    ])
    .then((http: any) => {
      const uri = http.result ?? http.response?.result

      submittedForm.uri = uri

      console.debug('Datafile created', uri)

      return submittedForm
    })
    .catch(handleCreateError)
}

function handleCreateError(error: any) {
  if (error.status === 409) {
    console.error('DataFile already exists', error)

    $opensilex.errorHandler(
      error,
      t('DataFileForm.error.datafile-already-exists')
    )
  } else {
    $opensilex.errorHandler(error)
  }
}

function showProvenanceCreateForm() {
  validateProvenanceForm.value = false
  provenanceForm.value?.showCreateForm?.()
}

function afterCreateProvenance(data: any) {
  provenance.value = data
  form.provenance = data.uri

  nextTick(() => {
    provenanceSelector.value?.refresh?.()
  })
}

async function checkOSinExperiment() {
  const contextURI = form.experiments
  const target = form.target

  try {
    const http = await serviceOS.getScientificObjectsListByUris(
      contextURI,
      [target]
    )

    const foundObjects = http?.response?.result || []

    if (foundObjects.length === 0) {
      $opensilex.showErrorToast(
        t('DataFileForm.error.object-not-in-experiment')
      )

      return false
    }

    return true
  } catch (error) {
    console.error('Error checking the object:', error)
    $opensilex.errorHandler(error)

    return false
  }
}

function isFilled(value: unknown) {
  if (Array.isArray(value)) {
    return value.length > 0
  }

  if (value instanceof File) {
    return true
  }

  if (value === undefined || value === null) {
    return false
  }

  if (typeof value === 'string') {
    return value.trim() !== ''
  }

  return true
}

watch(
  () => form.rdf_type,
  () => {
    rdfTypeItem.value?.restoreValidation()
  },
  { flush: 'post' }
)

watch(
  () => form.provenance,
  () => {
    provenanceItem.value?.restoreValidation()
  },
  { flush: 'post' }
)

watch(
  () => form.experiments,
  () => {
    experimentsItem.value?.restoreValidation()
  },
  { deep: true, flush: 'post' }
)

watch(
  () => form.file,
  () => {
    fileItem.value?.restoreValidation()
    externalSourceItem.value?.restoreValidation()
  },
  { flush: 'post' }
)

watch(
  () => form.date,
  () => {
    dateItem.value?.restoreValidation()
  },
  { flush: 'post' }
)

watch(
  [selectedFormat, selectedSource],
  async () => {
    await nextTick()
    formRef.value?.restoreValidation()
  },
  { flush: 'post' }
)

async function validate() {
  try {
    await formRef.value?.validate()
    return true
  } catch {
    return false
  }
}

defineExpose({
  create,
  validate,
  getEmptyForm,
  reset
})
</script>

<style scoped lang="scss">
/* neutralisation des classes injectées par naive dans les <n-form-item> qui créent des espaces indésirés entre les champs */
:deep(.n-form-item-feedback-wrapper) {
  min-height: 0 !important;
  margin-top: 0 !important;
  padding-top: 0 !important;
}

:deep(.n-form-item-feedback) {
  margin-top: 2px !important;
  line-height: 1.2 !important;
}
</style>

<i18n>
en:
  DataFileForm:
    add-provenance: Add provenance
    update-provenance: Update provenance
    browse: Browse
    date: Date
    date-help: Creation Date
    error:
      datafile-already-exists: Datafile already exists
      file-name-too-long: File name is too long
      object-not-in-experiment: Scientific Object is not in this experiment
    experiment: Experiment
    external-source: External source
    fileDX: File
    fileDX-help: Insert a file in JCAMP-DX, JDX, DX format. A file can contain an infinite number of samples, provided you have created the OS in envibis beforehand.
    fileSpectraCSV-help: Insert a spectra file in CSV format with tabulation separator. A file can contain an infinite number of samples, provided you have created the OS in envibis beforehand.
    form-selectFormat-placeholder: Select datafile format to insert
    provenance: Provenance
    targets-help: List of resources's URI concerned by the document
    targets-error: Concerned item's URI expected
    type: Type
    type-help: Datafile type

fr:
  DataFileForm:
    add-provenance: Ajouter une provenance
    update-provenance: Modifier une provenance
    browse: Parcourir
    date: Date
    date-help: Date de création
    error:
      datafile-already-exists: Le fichier de données existe déjà
      file-name-too-long: Le nom du fichier est trop long
      object-not-in-experiment: L'objet scientifique n'est pas inclus dans cette expérience
    experiment: Expérimentation
    external-source: Source externe
    fileDX: Fichier
    fileDX-help: Insérer un fichier au format JCAMP-DX, JDX, DX. Un fichier peut comporter une infinité d'échantillons à condition d'avoir créé au préalable les OS dans envibis.
    fileSpectraCSV-help: Insérer un fichier de spectres au format CSV avec le séparateur tabulation. Un fichier peut comporter une infinité d'échantillons à condition d'avoir créé au préalable les OS dans envibis.
    form-selectFormat-placeholder: Sélectionner le format du fichier de données à insérer
    provenance: Provenance
    targets: Cible(s)
    targets-help: Liste d'URI des ressources concernées par le document
    type: Type
    type-help: Type de datafile
</i18n>