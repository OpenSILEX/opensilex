<template>
  <Modal ref="modalRef">
    <template #header>
      <FormHeader :title="modalFormLogic.formTitle.value" icon="ik#ik-thermometer" />
    </template>

    <n-form
      ref="formRef"
      :model="modalFormLogic.form.value"
      :rules="rules"
      label-placement="top"
      :show-require-mark="true"
    >
      <!-- URI -->
      <n-form-item>
        <UriForm
          v-model:uri="modalFormLogic.form.value.uri"
          :label="t('DeviceForm.uri')"
          :editMode="modalFormLogic.editMode.value"
          v-model:generated="uriGenerated"
          :helpMessage="t('DeviceForm.uri-help')"
        />
      </n-form-item>

      <!-- Type -->
      <n-form-item path="rdf_type">
        <TypeForm
          v-model:type="modalFormLogic.form.value.rdf_type"
          :baseType="baseType"
          :helpMessage="t('DeviceForm.type-help')"
          :required="true"
          :multiple="false"
          :disabled="modalFormLogic.editMode.value"
          :ignoreRoot="false"
          @select="typeSwitch($event.id, false)"
        />
      </n-form-item>

      <!-- name -->
      <n-form-item :label="t('component.common.name')" path="name">
        <InputForm
          v-model:value="modalFormLogic.form.value.name"
          type="text"
          :helpMessage="t('DeviceForm.name-help')"
          :required="true"
        />
      </n-form-item>

      <!-- description -->
      <n-form-item>
        <TextAreaForm
          v-model:value="modalFormLogic.form.value.description"
          :label="t('DeviceForm.description')"
          type="text"
          :helpMessage="t('DeviceForm.description-help')"
          @keydown.enter.stop
        />
      </n-form-item>

      <!-- brand -->
      <n-form-item>
        <InputForm
          v-model:value="modalFormLogic.form.value.brand"
          :label="t('DeviceForm.brand')"
          type="text"
          :helpMessage="t('DeviceForm.brand-help')"
        />
      </n-form-item>

      <!-- constructor_model -->
      <n-form-item>
        <InputForm
          v-model:value="modalFormLogic.form.value.constructor_model"
          :label="t('DeviceForm.constructor_model')"
          type="text"
          :helpMessage="t('DeviceForm.constructor_model-help')"
        />
      </n-form-item>

      <!-- serial_number -->
      <n-form-item>
        <InputForm
          v-model:value="modalFormLogic.form.value.serial_number"
          :label="t('DeviceForm.serial_number')"
          type="text"
          :helpMessage="t('DeviceForm.serial_number-help')"
        />
      </n-form-item>

      <!-- person_in_charge -->
        <PersonSelector
          v-model:persons="modalFormLogic.form.value.person_in_charge"
          :label="t('DeviceForm.person_in_charge')"
          :helpMessage="t('DeviceForm.person_in_charge-help')"
        />

      <!-- Period -->
        <DateRangePickerForm
          v-model:start="modalFormLogic.form.value.start_up"
          v-model:end="modalFormLogic.form.value.removal"
          :labelStart="t('DeviceForm.start_up')"
          :labelEnd="t('DeviceForm.removal')"
          :helpMessageStart="t('DeviceForm.start_up-help')"
          :helpMessageEnd="t('DeviceForm.removal-help')"
        />

        <OntologyRelationsForm
          ref="ontologyRelationsForm"
          :rdfType="modalFormLogic.form.value.rdf_type"
          :relations="modalFormLogic.form.value.relations"
          :excludedProperties="excludedProperties"
          :baseType="baseType"
          :editMode="modalFormLogic.editMode.value"
        />

      <!-- metadata -->
        <AttributesTable
          ref="attributeTable"
          :attributesArray="attributesArray"
        />

    </n-form>

    <template #footer>
      <FormFooter @cancel="modalFormLogic.hide" @submit="modalFormLogic.submit" />
    </template>
  </Modal>
</template>

<script setup lang="ts">
import { computed, inject, ref, useTemplateRef } from 'vue'
import { useStore } from 'vuex'
import { NForm, NFormItem } from 'naive-ui'
import { useI18n } from 'vue-i18n'

import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import OntologyRelationsForm from '../../ontology/OntologyRelationsForm.vue'
import AttributesTable, { readAttributes } from '../../common/forms/AttributesTable.vue'
import { requiredTrimmed } from '../../../models/FormFieldsFormatter'

import type { DevicesService } from 'opensilex-core/api/devices.service'
import type { DeviceCreationDTO } from 'opensilex-core/index'

import FormHeader from '@/components/common/forms/FormHeader.vue'
import FormFooter from '@/components/common/forms/FormFooter.vue'
import useModalFormLogic from '@/composables/useModalFormLogic'
import Modal from '@/components/common/views/Modal.vue'
import UriForm from "@/components/common/forms/UriForm.vue";
import TypeForm from "@/components/common/forms/TypeForm.vue";
import InputForm from "@/components/common/forms/InputForm.vue";
import TextAreaForm from "@/components/common/forms/TextAreaForm.vue";
import PersonSelector from "@/components/persons/PersonSelector.vue";
import DateRangePickerForm from "@/components/common/forms/DateRangePickerForm.vue";

//#region Public
const emit = defineEmits(['hide','onCreate','onUpdate','onSuccess'])

const props = defineProps<{
  createTitle: string,
  editTitle: string
}>();
//#endregion

//#region Private

const store = useStore()
const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const service = $opensilex.getService<DevicesService>('opensilex.DevicesService')
const { t } = useI18n()

const modalRef = useTemplateRef<InstanceType<typeof Modal>>('modalRef')
const nFormRef = useTemplateRef<InstanceType<typeof NForm>>('formRef')

const attributeTable = ref<InstanceType<typeof AttributesTable> | any>(null)
const ontologyRelationsForm = ref<InstanceType<typeof OntologyRelationsForm> | any>(null)

const user = computed(() => store.state.user)

const uriGenerated = ref(true)
const baseType = $opensilex.Oeso.DEVICE_TYPE_URI

const excludedProperties = new Set<string>([
  $opensilex.Rdfs.LABEL,
  $opensilex.Rdfs.COMMENT,
  $opensilex.Oeso.HAS_MODEL,
  $opensilex.Oeso.HAS_BRAND,
  $opensilex.Oeso.HAS_SERIAL_NUMBER,
  $opensilex.Oeso.PERSON_IN_CHARGE,
  $opensilex.Oeso.START_UP,
  $opensilex.Oeso.REMOVAL
])

const attributesArray = ref<any[]>([])

//#endregion

//#region Computed / rules
const rules = computed(() => ({
  rdf_type: {
    required: true,
    message: t('validations.required_if', { _field_: t('DeviceForm.type') }),
    trigger: ['change', 'blur']
  },
  name: requiredTrimmed('component.common.name')
}))
//#endregion

//#region modalFormLogic composable
const modalFormLogic = useModalFormLogic<DeviceCreationDTO>({
  modalRef,
  nFormRef,
  getEmptyForm,
  create,
  update,
  reset,
  addTitle: props.createTitle,
  editTitle: props.editTitle,
  onCreate: (res) => emit('onCreate', res),
  onUpdate: (res) => emit('onUpdate', res),
  onSuccess: () => emit('onSuccess'),
  onHide: () => emit('hide')
})
//#endregion

//#region Methods
function getEmptyForm(): DeviceCreationDTO {
  return {
    uri: undefined,
    name: undefined,
    rdf_type: undefined,
    brand: undefined,
    constructor_model: undefined,
    serial_number: undefined,
    person_in_charge: undefined,
    start_up: undefined,
    removal: undefined,
    description: undefined,
    metadata: undefined,
    relations: []
  }
}

function reset() {
  uriGenerated.value = true
  attributeTable.value?.resetTable?.()
}

async function create(form: DeviceCreationDTO) {
  form.metadata = attributeTable.value?.pushAttributes?.()
  return await service.createDevice(false, form)
}

async function update(form: DeviceCreationDTO) {
  form.metadata = attributeTable.value?.pushAttributes?.()
  return await service.updateDevice(form)
}

function loadAttributes(metadata: Record<string, string>) {
  attributesArray.value = readAttributes(metadata)
}

function typeSwitch(type: string, initialLoad: boolean) {
  ontologyRelationsForm.value?.typeSwitch?.(type, initialLoad)
}
//#endregion

defineExpose({
  showCreateForm: modalFormLogic.showCreateForm,
  showEditForm: modalFormLogic.showEditForm,
  hide: modalFormLogic.hide,
  user,
  reset,
  create,
  update,
  readAttributes: loadAttributes,
  getEmptyForm
})
</script>
<style scoped lang="scss">
</style>

<i18n>
en:
  DeviceForm:
    uri: URI
    uri-help: Unique device identifier autogenerated
    type: Type
    type-help: Device Type
    name: The device
    name-help: A name given to the device
    brand: Brand
    brand-help: A brand of the device
    constructor_model: Constructor model
    constructor_model-help: A constructor model of the device
    serial_number: Serial number
    serial_number-help: A serial number of the device
    person_in_charge: Person in charge
    person_in_charge-help: Person in charge of the device
    start_up: Start up
    start_up-help: Date of start up
    removal: Removal
    removal-help: Date of removal
    description: Description
    description-help: Description associated of the device
    variable: Variable
    variable-help: Insert one or several URI's variables

fr:
  DeviceForm:
    uri: URI
    uri-help: Identifiant unique de l'appareil généré automatiquement
    type: Type
    type-help: Type d'appareil
    name: L'appareil
    name-help: Nom de l'apppareil
    brand: Marque de l'appareil
    brand-help: Marque de l'appareil
    constructor_model: Modèle constructeur
    constructor_model-help: Modèle constructeur de l'appareil
    serial_number: Numéro de série
    serial_number-help: Numéro de série de l'appareil
    person_in_charge: Personne responsable
    person_in_charge-help: Personne responsable de l'appareil
    start_up: Date d'obtention
    start_up-help: Date d'obtention de l'appareil
    removal: Date de mise hors service
    removal-help: Date de mise hors service de l'appareil
    description: Description
    description-help: Description associée à l'appareil
    variable: Variable
    variable-help: Insérer une ou plusieurs URI de variables
</i18n>