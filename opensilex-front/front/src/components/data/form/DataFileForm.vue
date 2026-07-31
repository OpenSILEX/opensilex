<template>
  <Modal ref="modalRef">
    <template #header>
      <FormHeader :title="modalFormLogic.formTitle.value" icon="bi#bi-file-earmark-text" />
    </template>

    <n-form
      ref="formRef"
      :model="modalFormLogic.form.value"
      :rules="rules"
      label-placement="top"
      :show-require-mark="true"
      size="small"
    >
      <!-- type -->
      <n-form-item path="rdf_type" ref="rdfTypeItem">
        <TypeForm
          :key="modalFormLogic.form.value.rdf_type ?? 'no-type'"
          v-model:type="modalFormLogic.form.value.rdf_type"
          :baseType="Oeso.DATAFILE_TYPE_URI"
          :helpMessage="t('DataFileForm.type-help')"
          :required="true"
        />
      </n-form-item>

      <template v-if="modalFormLogic.form.value.rdf_type">
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
          <ProvenanceSelector
            path="provenance"
            ref="provenanceSelector"
            v-model:provenances="modalFormLogic.form.value.provenance"
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

        <!-- experiment -->
          <ExperimentSelector
            path="experiments"
            :label="t('DataFileForm.experiment')"
            v-model:experiments="modalFormLogic.form.value.experiments"
            :required="true"
          />

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
        <n-form-item v-if="!modalFormLogic.isEditMode.value && selectedSource === 'file'" path="file" ref="fileItem">
          <FileInputForm
            v-model:file="modalFormLogic.form.value.file"
            :label="t('DataFileForm.fileDX')"
            type="file"
            :browse-text="t('DataFileForm.browse')"
            :required="true"
            rules="size:100000"
            :helpMessage="helpMessageFile"
          />
        </n-form-item>

        <!-- External source -->
        <n-form-item v-if="!modalFormLogic.isEditMode.value && selectedSource === 'external'" path="file" ref="externalSourceItem">
          <InputForm
            :label="t('DataFileForm.external-source')"
            type="text"
            v-model:value="modalFormLogic.form.value.file"
            :required="true"
          />
        </n-form-item>

        <!-- date -->
        <n-form-item v-if="selectedFormat === 'all' || selectedSource === 'external'" path="date" ref="dateItem">
          <DateForm
            v-model:value="modalFormLogic.form.value.date"
            :helpMessage="t('DataFileForm.date-help')"
            :label="t('DataFileForm.date')"
            :required="true"
          />
        </n-form-item>

        <!-- targets -->
        <n-form-item v-if="selectedFormat === 'all' || selectedSource === 'external'" path="target">
          <InputForm
            v-model:value="modalFormLogic.form.value.target"
            :baseType="Oeso.targets"
            :label="t('DataFileForm.targets')"
            :helpMessage="t('DataFileForm.targets-help')"
            type="text"
          />
        </n-form-item>
      </template>
    </n-form>

    <template #footer>
      <FormFooter @cancel="modalFormLogic.hide" @submit="modalFormLogic.submit" />
    </template>
  </Modal>
</template>

<script setup lang="ts">
import { computed, inject, ref, nextTick, watch, useTemplateRef } from 'vue'
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
import TypeForm from '@/components/common/forms/TypeForm.vue'
import ProvenanceSelector from '@/components/data/ProvenanceSelector.vue'
import ExperimentSelector from '@/components/experiments/ExperimentSelector.vue'
import FileInputForm from '@/components/common/forms/FileInputForm.vue'
import InputForm from '@/components/common/forms/InputForm.vue'
import DateForm from '@/components/common/forms/DateForm.vue'
import Modal from '@/components/common/views/Modal.vue'
import FormHeader from '@/components/common/forms/FormHeader.vue'
import FormFooter from '@/components/common/forms/FormFooter.vue'
import useModalFormLogic, {ModalFormEmits, ModalFormProps} from '@/composables/useModalFormLogic'

//#region Public
type DataFileFormModel = {
  rdf_type: string | null
  provenance: any
  experiments: any[]
  file: File | string | null
  date: string | null
  target: string | null
}

const emit = defineEmits<ModalFormEmits>();
const props = defineProps<ModalFormProps>();
//#endregion

//#region Private

//#region Plugin and services
const opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const Oeso = opensilex.Oeso
const { t } = useI18n()
const store = useStore()
const dataService = opensilex.getService<DataService>('opensilex.DataService')
const serviceOS = opensilex.getService<ScientificObjectsService>(
  'opensilex.ScientificObjectsService'
)
//#endregion

const modalRef = useTemplateRef<InstanceType<typeof Modal>>('modalRef')
const formRef = useTemplateRef<InstanceType<typeof NForm>>('formRef')
const provenanceSelector = ref<any>(null)

//#region Datas & computed
const selectedFormat = ref<'all' | 'DX' | 'CSV'>('DX')
const selectedSource = ref<'file' | 'external'>('file')

const tabExperimentMode = computed(() => props.tabExperimentMode === true)
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
//#endregion

//#region modalFormLogic composable
const modalFormLogic = useModalFormLogic<DataFileFormModel>({
  modalRef,
  nFormRef: formRef,
  getEmptyForm,
  create,
  reset,
  props,
  emit
})
//#endregion

//#region Methods
function getEmptyForm(): DataFileFormModel {
  return {
    rdf_type: null,
    provenance: null,
    experiments: [],
    file: null,
    date: null,
    target: null
  }
}

async function reset(): Promise<void> {
  selectedFormat.value = 'DX'
  selectedSource.value = 'file'
}

function normalizeForm(form: DataFileFormModel) {
  if (form.date === '') {
    form.date = null
  }

  if (form.target === '') {
    form.target = null
  }
}

function getExperimentArray(experiments: any) {
  return Array.isArray(experiments)
    ? experiments
    : experiments
      ? [experiments]
      : []
}

function getProvenanceUri(provenance: any) {
  if (!provenance) {
    return undefined
  }

  if (typeof provenance === 'string') {
    return provenance
  }

  return provenance.uri ?? provenance.id
}

function showProvenanceCreateForm() {
  // This would be handled by a separate ProvenanceForm component if needed
  // For now, kept as placeholder for potential integration
}

async function checkOSinExperiment(form: DataFileFormModel) {
  const contextURI = form.experiments
  const target = form.target

  const http = await serviceOS.getScientificObjectsListByUris(
    contextURI,
    [target]
  )

  const foundObjects = http?.response?.result || []

  if (foundObjects.length === 0) {
    opensilex.showErrorToast(
      t('DataFileForm.error.object-not-in-experiment')
    )
    return false
  }

  return true
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

async function create(submittedForm: DataFileFormModel) {
  normalizeForm(submittedForm)

  const canCheckTarget =
    selectedSource.value === 'external' || selectedFormat.value === 'all'

  if (canCheckTarget && submittedForm.target) {
    const valid = await checkOSinExperiment(submittedForm)
    if (!valid) {
      return Promise.reject(new Error('Target validation failed'))
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

  return Promise.reject(new Error('Invalid file format or source'))
}

function createDxFile(submittedForm: DataFileFormModel) {
  return opensilex.uploadFileToService(
    '/core/datafiles/upload-dx',
    {
      rdf_type: submittedForm.rdf_type,
      provenance: submittedForm.provenance,
      experiments: [submittedForm.experiments],
      file: submittedForm.file
    },
    null,
    false
  )
    .then((uploadResponse: any) => {
      if (!uploadResponse || !uploadResponse.result) {
        throw new Error('File upload failed.')
      }

      return dataService.postDataFilePaths(uploadResponse.result)
    })
    .then((http: HttpResponse<OpenSilexResponse<any>>) => {
      const uri = http.response.result
      submittedForm.uri = uri
      console.debug('Datafile created', uri)
      opensilex.showSuccessToast('File uploaded and processed successfully.')
      return submittedForm
    })
}

function createCsvSpectraFile(submittedForm: DataFileFormModel) {
  return opensilex.uploadFileToService(
    '/core/datafiles/upload-spectra-csv',
    {
      rdf_type: submittedForm.rdf_type,
      provenance: submittedForm.provenance,
      experiments: [submittedForm.experiments],
      file: submittedForm.file
    },
    null,
    false
  )
    .then((uploadResponse: any) => {
      if (!uploadResponse || !uploadResponse.result) {
        throw new Error('File upload failed.')
      }

      return dataService.postDataFilePaths(uploadResponse.result)
    })
    .then((http: HttpResponse<OpenSilexResponse<any>>) => {
      const uri = http.response.result
      submittedForm.uri = uri
      console.debug('Datafile created', uri)
      opensilex.showSuccessToast('File uploaded and processed successfully.')
      return submittedForm
    })
}

function createGenericFile(submittedForm: DataFileFormModel) {
  return opensilex.uploadFileToService(
    '/core/datafiles',
    {
      description: {
        rdf_type: submittedForm.rdf_type,
        provenance: {
          uri: getProvenanceUri(submittedForm.provenance),
          experiments: getExperimentArray(submittedForm.experiments)
        },
        date: submittedForm.date,
        target: submittedForm.target
      },
      file: submittedForm.file
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
}

function createExternalSource(submittedForm: DataFileFormModel) {
  return dataService.postDataFilePaths([
    {
      rdf_type: submittedForm.rdf_type,
      provenance: {
        uri: getProvenanceUri(submittedForm.provenance),
        experiments: getExperimentArray(submittedForm.experiments)
      },
      date: submittedForm.date,
      target: submittedForm.target,
      relative_path: submittedForm.file
    }
  ])
    .then((http: any) => {
      const uri = http.result ?? http.response?.result
      submittedForm.uri = uri
      console.debug('Datafile created', uri)
      return submittedForm
    })
}
//#endregion

//#endregion
defineExpose({
  showCreateForm: modalFormLogic.showCreateForm,
  showEditForm: modalFormLogic.showEditForm
})
</script>

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