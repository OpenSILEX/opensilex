<template>
  <n-form
    ref="formRef"
    :model="form"
    :rules="rules"
    label-placement="top"
    :show-require-mark="true"
  >
    <!-- URI -->
    <opensilex-UriForm
      v-model:uri="form.uri"
      :label="t('DeviceForm.uri')"
      :editMode="editMode"
      v-model:generated="uriGenerated"
      :helpMessage="t('DeviceForm.uri-help')"
    />

    <!-- Type -->
    <n-form-item path="rdf_type">
      <opensilex-TypeForm
        v-model:type="form.rdf_type"
        :baseType="baseType"
        :helpMessage="t('DeviceForm.type-help')"
        :required="true"
        :multiple="false"
        :disabled="editMode"
        :ignoreRoot="false"
        @select="typeSwitch($event.id, false)"
      />
    </n-form-item>

    <!-- name -->
    <n-form-item :label="t('component.common.name')" path="name">
      <opensilex-InputForm
        v-model:value="form.name"
        type="text"
        :helpMessage="t('DeviceForm.name-help')"
        :required="true"
      />
    </n-form-item>

    <!-- description -->
    <opensilex-TextAreaForm
      v-model:value="form.description"
      :label="t('DeviceForm.description')"
      type="text"
      :helpMessage="t('DeviceForm.description-help')"
      @keydown.enter.stop
    />

    <!-- brand -->
    <opensilex-InputForm
      v-model:value="form.brand"
      :label="t('DeviceForm.brand')"
      type="text"
      :helpMessage="t('DeviceForm.brand-help')"
    />

    <!-- constructor_model -->
    <opensilex-InputForm
      v-model:value="form.constructor_model"
      :label="t('DeviceForm.constructor_model')"
      type="text"
      :helpMessage="t('DeviceForm.constructor_model-help')"
    />

    <!-- serial_number -->
    <opensilex-InputForm
      v-model:value="form.serial_number"
      :label="t('DeviceForm.serial_number')"
      type="text"
      :helpMessage="t('DeviceForm.serial_number-help')"
    />

    <!-- person_in_charge -->
    <opensilex-PersonSelector
      v-model:persons="form.person_in_charge"
      :label="t('DeviceForm.person_in_charge')"
      :helpMessage="t('DeviceForm.person_in_charge-help')"
    />

    <!-- Period -->
    <opensilex-DateRangePickerForm
      v-model:start="form.start_up"
      v-model:end="form.removal"
      :labelStart="t('DeviceForm.start_up')"
      :labelEnd="t('DeviceForm.removal')"
      :helpMessageStart="t('DeviceForm.start_up-help')"
      :helpMessageEnd="t('DeviceForm.removal-help')"
    />

    <opensilex-OntologyRelationsForm
      ref="ontologyRelationsForm"
      :rdfType="form.rdf_type"
      :relations="form.relations"
      :excludedProperties="excludedProperties"
      :baseType="baseType"
      :editMode="editMode"
    />

    <!-- metadata -->
    <opensilex-AttributesTable
      ref="attributeTable"
      :attributesArray="attributesArray"
    />
  </n-form>
</template>

<script setup lang="ts">
import { computed, inject, ref } from 'vue'
import { useStore } from 'vuex'
import { NForm, NFormItem } from 'naive-ui'
import { useI18n } from 'vue-i18n'

import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import OntologyRelationsForm from '../../ontology/OntologyRelationsForm.vue'
import AttributesTable, { readAttributes } from '../../common/forms/AttributesTable.vue'
import { requiredTrimmed } from '../../../models/FormFieldsFormatter'

import type { DevicesService } from 'opensilex-core/api/devices.service'
import type { DeviceCreationDTO } from 'opensilex-core/index'

const props = defineProps<{
  editMode?: boolean
  form?: DeviceCreationDTO
}>()

const store = useStore()
const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const service = $opensilex.getService<DevicesService>('opensilex.DevicesService')
const { t } = useI18n()

const formRef = ref<any>(null)
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

const form = computed<DeviceCreationDTO>({
  get() {
    return props.form ?? getEmptyForm()
  },
  set() {
  }
})

const attributesArray = ref<any[]>([])

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

async function create(formArg: DeviceCreationDTO) {
  try {
    formArg.metadata = attributeTable.value?.pushAttributes?.()
    const http = await service.createDevice(false, formArg)
    formArg.uri = (http as any).response.result
    return formArg
  } catch (error) {
    $opensilex.errorHandler(error)
    return false
  }
}

async function update(formArg: DeviceCreationDTO) {
  try {
    formArg.metadata = attributeTable.value?.pushAttributes?.()
    await service.updateDevice(formArg)
    return formArg
  } catch (error) {
    $opensilex.errorHandler(error)
    return false
  }
}

const rules = computed(() => ({
  rdf_type: {
    required: true,
    message: t('validations.required_if', { _field_: t('DeviceForm.type') }),
    trigger: ['change', 'blur']
  },
  name: requiredTrimmed('component.common.name')
}))

function loadAttributes(metadata: Record<string, string>) {
  attributesArray.value = readAttributes(metadata)
}

function typeSwitch(type: string, initialLoad: boolean) {
  ontologyRelationsForm.value?.typeSwitch?.(type, initialLoad)
}

async function validate() {
  try {
    await formRef.value?.validate()
    return true
  } catch {
    return false
  }
}

defineExpose({
  user,
  reset,
  create,
  update,
  readAttributes: loadAttributes,
  typeSwitch,
  validate,
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