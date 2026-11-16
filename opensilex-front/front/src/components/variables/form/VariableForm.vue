<template>
  <Modal ref="modalRef">
    <template #header>
      <FormHeader :title="modalFormLogic.formTitle.value" icon="fa#vials" />
    </template>

  <div id="v-step-global">
    <Tutorial
      ref="variableTutorial"
      :steps="tutorialSteps"
      @onSkip="continueFormEditing"
      @onFinish="continueFormEditing"
      :editMode="modalFormLogic.isEditMode.value"
      class="variableFormTutorial"
    />

      <n-form
        ref="formRef"
        :model="modalFormLogic.form.value"
        :rules="rules"
        label-placement="top"
        :show-require-mark="true"
        size="large"
      >
        <!-- URI -->
        <n-form-item>
          <UriForm
            v-model:uri="modalFormLogic.form.value.uri"
            :generated="uriGenerated"
            @update:generated="val => uriGenerated = val"
            :editMode="modalFormLogic.isEditMode.value"
            label="component.common.uri"
            class="v-step-uri"
          />
        </n-form-item>

        <div class="row">
          <!-- ENTITY -->
          <div class="col-lg-6" id="v-step-entity">
              <EntitySelector
                path="entity"
                ref="entitySelector"
                label="component.variable.entity.entity"
                :placeholder="$t('component.variable.entity.entity-placeholder')"
                :helpMessage="$t('component.variable.entity.entity-help')"
                v-model:selected="modalFormLogic.form.value.entity"
                :multiple="false"
                :required="true"
                :actionHandler="modalFormLogic.isEditMode.value ? undefined : showEntityCreateForm"
                @selectionChange="updateEntity"
                :disabled="false"
              />
            <AgroportalEntityForm
              ref="entityForm"
              ontologies="entities"
              @onCreate="onEntityCreated"
              @onUpdate="onEntityCreated"
            />
          </div>

          <!-- INTEREST ENTITY -->
          <div class="col-lg-6" id="v-step-interestEntity">
              <InterestEntitySelector
                ref="interestEntitySelector"
                label="component.variable.entityOfInterest.entityOfInterest"
                :placeholder="$t('component.variable.entityOfInterest.entityOfInterest-placeholder')"
                v-model:selected="modalFormLogic.form.value.entity_of_interest"
                :actionHandler="modalFormLogic.isEditMode.value ? undefined : showInterestEntityCreateForm"
                :helpMessage="$t('component.variable.entityOfInterest.interestEntity-help')"
              />
            <AgroportalEntityOfInterestForm
              ref="interestEntityForm"
              ontologies="entities"
              @onCreate="onEntityOfInterestCreated"
              @onUpdate="onEntityOfInterestCreated"
            />
          </div>

          <!-- CHARACTERISTIC -->
          <div class="col-lg-6" id="v-step-characteristic">
              <CharacteristicSelector
                path="characteristic"
                ref="characteristicSelector"
                label="component.variable.characteristic.characteristic"
                :placeholder="$t('component.variable.characteristic.characteristic-placeholder')"
                v-model:selected="modalFormLogic.form.value.characteristic"
                :multiple="false"
                :required="true"
                @selectionChange="updateCharacteristic"
                :actionHandler="modalFormLogic.isEditMode.value ? undefined : showCharacteristicCreateForm"
                :helpMessage="$t('component.variable.characteristic.characteristic-help')"
              />
            <AgroportalCharacteristicForm
              ref="characteristicForm"
              ontologies="entities"
              @onCreate="onCharacteristicCreated"
              @onUpdate="onCharacteristicCreated"
            />
          </div>

          <!-- SPECIES -->
          <div class="col-lg-6" id="v-step-species">
              <SpeciesSelector
                v-if="!isGermplasmMenuExcluded"
                label="component.variable.species.species"
                :placeholder="$t('component.variable.species.select-multiple-placeholder')"
                :multiple="true"
                :checkable="true"
                v-model:selected="modalFormLogic.form.value.species"
              />
          </div>

          <!-- METHOD -->
          <div class="col-lg-6" id="v-step-method">
              <MethodSelector
                path="method"
                ref="methodSelector"
                label="component.variable.method.method"
                :placeholder="$t('component.variable.method.method-placeholder')"
                :multiple="false"
                :required="true"
                v-model:selected="modalFormLogic.form.value.method"
                :helpMessage="$t('component.variable.method.method-help')"
                :actionHandler="modalFormLogic.isEditMode.value ? undefined : showMethodCreateForm"
                @selectionChange="updateMethod"
              />
            <AgroportalMethodForm
              ref="methodForm"
              ontologies="entities"
              @onCreate="onMethodCreated"
              @onUpdate="onMethodCreated"
            />
          </div>

          <!-- TRAIT BUTTON -->
          <div class="col-lg-6" id="traitButton">
            <n-form-item>
              <Button
                label="component.variable.trait-button"
                helpMessage="component.variable.trait-button-help"
                @click="showTraitForm"
                :small="false"
                icon="fa#globe-americas"
                class="greenThemeColor"
              />
            </n-form-item>
          </div>

          <WizardForm
            ref="traitForm"
            :steps="traitSteps"
            createTitle="component.variable.trait-form-create-title"
            editTitle="component.variable.trait-form-edit-title"
            modalSize="full"
            :static="false"
            :initForm="getEmptyTraitForm"
            :createAction="updateVariableTrait"
            :updateAction="updateVariableTrait"
          />

          <!-- UNIT -->
          <div class="col-lg-6" id="v-step-unit">
              <UnitSelector
                path="unit"
                ref="unitSelector"
                label="component.variable.unit.unit"
                :placeholder="$t('component.variable.unit.unit-placeholder')"
                :multiple="false"
                :required="true"
                v-model:selected="modalFormLogic.form.value.unit"
                @selectionChange="updateUnit"
                :helpMessage="$t('component.variable.unit.unit-help')"
                :actionHandler="modalFormLogic.isEditMode.value ? undefined : showUnitCreateForm"
              />
            <AgroportalUnitForm
              ref="unitForm"
              ontologies="unit"
              @onCreate="onUnitCreated"
              @onUpdate="onUnitCreated"
            />
          </div>
        </div>

        <hr />

        <div class="row">
          <!-- NAME -->
          <div class="col-lg-6" id="v-step-name">
            <n-form-item path="name">
              <InputForm
                v-model:value="modalFormLogic.form.value.name"
                label="component.common.name"
                type="text"
                :required="true"
              />
            </n-form-item>
          </div>

          <!-- ALT NAME -->
          <div class="col-lg-6" id="v-step-alt">
            <n-form-item>
              <InputForm
                v-model:value="modalFormLogic.form.value.alternative_name"
                label="component.variable.altName"
                type="text"
              />
            </n-form-item>
          </div>

          <!-- DATATYPE -->
          <div class="col-lg-6" id="v-step-datatype">
              <VariableDataTypeSelector
                path="datatype"
                v-model:selected="modalFormLogic.form.value.datatype"
                :label="'component.variable.dataType.data-type'"
                :placeholder="$t('component.variable.dataType.datatype-placeholder')"
                :required="true"
                :helpMessage="$t('component.variable.dataType.datatype-help')"
                :itemLoadingMethod="loadDataType"
                :conversionMethod="objectToSelectNode"
                :options="datatypesNodes"
              />
          </div>

          <!-- TIME INTERVAL -->
          <div class="col-lg-6" id="v-step-time-interval">
              <VariableTimeIntervalSelector
                label="component.variable.timeInterval.time-interval"
                v-model:selected="modalFormLogic.form.value.time_interval"
                :placeholder="$t('component.variable.timeInterval.time-interval-placeholder')"
              />
          </div>

          <!-- SAMPLING INTERVAL -->
          <div class="col-lg-6" id="v-step-sampling-interval">
              <FormSelector
                label="component.variable.samplingInterval.sampling-interval"
                v-model:selected="modalFormLogic.form.value.sampling_interval"
                :multiple="false"
                :options="sampleList"
                :placeholder="$t('component.variable.samplingInterval.sampling-interval-placeholder')"
                :helpMessage="$t('component.variable.samplingInterval.sampling-interval-help')"
              />
          </div>

          <!-- DESCRIPTION -->
          <div class="col-xl-12" id="v-step-description">
            <n-form-item>
              <TextAreaForm
                v-model:value="modalFormLogic.form.value.description"
                label="component.common.description"
                @keydown.enter.stop
              />
            </n-form-item>
          </div>
        </div>
      </n-form>
    </div>

    <template #footer>
      <FormFooter @cancel="modalFormLogic.hide" @submit="modalFormLogic.submit" />
    </template>
  </Modal>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, watch, inject, useTemplateRef } from 'vue'
import { useI18n } from 'vue-i18n'
import { NForm, NFormItem } from 'naive-ui'
import {
  VariableDatatypeDTO,
  VariablesService
} from 'opensilex-core'
import HttpResponse, {OpenSilexResponse} from "@/lib/HttpResponse";
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import { requiredTrimmed } from '@/models/FormFieldsFormatter'

import Tutorial from '@/components/common/views/Tutorial.vue'
import UriForm from '@/components/common/forms/UriForm.vue'
import InputForm from '@/components/common/forms/InputForm.vue'
import TextAreaForm from '@/components/common/forms/TextAreaForm.vue'
import FormSelector from '@/components/common/forms/FormSelector.vue'
import FormHeader from '@/components/common/forms/FormHeader.vue'
import FormFooter from '@/components/common/forms/FormFooter.vue'
import Button from '@/components/common/buttons/Button.vue'
import WizardForm from '@/components/common/forms/WizardForm.vue'
import Modal from '@/components/common/views/Modal.vue'
import useModalFormLogic, {ModalFormEmits, ModalFormProps} from '@/composables/useModalFormLogic'

import EntitySelector from './EntitySelector.vue'
import InterestEntitySelector from './InterestEntitySelector.vue'
import CharacteristicSelector from './CharacteristicSelector.vue'
import MethodSelector from './MethodSelector.vue'
import UnitSelector from './UnitSelector.vue'
import VariableDataTypeSelector from './VariableDataTypeSelector.vue'
import VariableTimeIntervalSelector from './VariableTimeIntervalSelector.vue'

import SpeciesSelector from '@/components/species/SpeciesSelector.vue'
import AgroportalEntityForm from '@/components/variables/agroportal/AgroportalEntityForm.vue'
import AgroportalEntityOfInterestForm from '@/components/variables/agroportal/AgroportalEntityOfInterestForm.vue'
import AgroportalCharacteristicForm from '@/components/variables/agroportal/AgroportalCharacteristicForm.vue'
import AgroportalMethodForm from '@/components/variables/agroportal/AgroportalMethodForm.vue'
import AgroportalUnitForm from '@/components/variables/agroportal/AgroportalUnitForm.vue'
import TraitForm from '@/components/variables/form/TraitForm.vue'
import {VariableCreationDTO} from "opensilex-core/model/variableCreationDTO";
import {VariableUpdateDTO} from "opensilex-core/model/variableUpdateDTO";

//#region Public
const emit = defineEmits<ModalFormEmits>();
const props = defineProps<ModalFormProps>();
//#endregion

//#region Private

//#region Plugin and services
const opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const service = opensilex.getService<VariablesService>('opensilex.VariablesService')
const { t, locale } = useI18n()
//#endregion

const modalRef = useTemplateRef<InstanceType<typeof Modal>>('modalRef')
const formRef = useTemplateRef<InstanceType<typeof NForm>>('formRef')

//#region Datas & computed
let uriGenerated = ref<boolean>(true)
const variableTutorial = ref()
const entitySelector = ref()
const interestEntitySelector = ref()
const characteristicSelector = ref()
const methodSelector = ref()
const unitSelector = ref()
const traitForm = ref()

const entityForm = ref()
const interestEntityForm = ref()
const characteristicForm = ref()
const methodForm = ref()
const unitForm = ref()

const savedVariable = ref<VariableCreationDTO>()
const traitSteps = [{ component: TraitForm }]

const datatypes = ref<VariableDatatypeDTO[]>([])
const datatypesNodes = ref<any[]>([])

const sampleList = ref([
  'mm','cm','m','km','field','region'
].map(sample => ({ id: t(`component.variable.dimensionValues.${sample}`), label: t(`component.variable.dimensionValues.${sample}`) })))

const isGermplasmMenuExcluded = computed(() => opensilex.getConfig().menuExclusions.includes('germplasm'))

const rules = computed(() => ({
  entity:        { required: true, message: t('validations.required_if', { _field_: t('component.variable.entity.entity') }), trigger: ['change','blur'] },
  characteristic:{ required: true, message: t('validations.required_if', { _field_: t('component.variable.characteristic.characteristic') }), trigger: ['change','blur'] },
  method:        { required: true, message: t('validations.required_if', { _field_: t('component.variable.method.method') }), trigger: ['change','blur'] },
  unit:          { required: true, message: t('validations.required_if', { _field_: t('component.variable.unit.unit') }), trigger: ['change','blur'] },
  name:          requiredTrimmed('component.common.name'),
  datatype:      { required: true, message: t('validations.required_if', { _field_: t('component.variable.dataType.data-type') }), trigger: ['change','blur'] },
}))
//#region tutorial steps
const tutorialSteps = [
  {
    target: '#v-step-global .v-step-uri' ,
    header: { title: t('component.variable.title') },
    content: t('component.variable.tutorial.global'),
    params: { placement: 'bottom' }
  },
  {
    target: '#v-step-entity',
    header: { title: t('component.variable.entity.entity') },
    content: t('component.variable.tutorial.entity'),
    params: { placement: 'left' }
  },
  {
    target: '#v-step-entity',
    header: { title: t('component.variable.entity.entity') },
    content: t('component.variable.tutorial.entity-check'),
    params: { placement: 'left' }
  },
  {
    target: '#v-step-interestEntity',
    header: { title: t('component.variable.entityOfInterest.entityOfInterest') },
    content: t('component.variable.tutorial.entityOfInterest'),
    params: { placement: 'left' }
  },
  {
    target: '#v-step-interestEntity',
    header: { title: t('component.variable.entityOfInterest.entityOfInterest') },
    content: t('component.variable.tutorial.entityOfInterest-check'),
    params: { placement: 'left' }
  },
  {
    target: '#v-step-characteristic',
    header: { title: t('component.variable.characteristic.characteristic') },
    content: t('component.variable.tutorial.characteristic'),
    params: { placement: 'left' }
  },
  {
    target: '#v-step-characteristic',
    header: { title: t('component.variable.characteristic.characteristic') },
    content: t('component.variable.tutorial.characteristic-check'),
    params: { placement: 'right' }
  },
  {
    target: '#v-step-method',
    header: { title: t('component.variable.method.method') },
    content: t('component.variable.tutorial.method'),
    params: { placement: 'left' }
  },
  {
    target: '#v-step-method',
    header: { title: t('component.variable.method.method') },
    content: t('component.variable.tutorial.method-check'),
    params: { placement: 'right' }
  },
  {
    target: '#v-step-unit',
    header: { title: t('component.variable.unit.unit') },
    content: t('component.variable.tutorial.unit'),
    params: { placement: 'left' }
  },
  {
    target: '#v-step-species',
    header: { title: t('component.experiment.species') },
    content: t('component.variable.tutorial.species'),
    params: { placement: 'left' }
  },
  {
    target: '#v-step-name',
    header: { title: t('component.common.name') },
    content: t('component.variable.tutorial.name'),
    params: { placement: 'left' }
  },
  {
    target: '#v-step-alt',
    header: { title: t('component.variable.altName') },
    content: t('component.variable.tutorial.altName'),
    params: { placement: 'left' }
  },
  {
    target: '#v-step-datatype',
    header: { title: t('component.variable.dataType.data-type') },
    content: t('component.variable.tutorial.datatype'),
    params: { placement: 'left' }
  },
  {
    target: '#v-step-time-interval',
    header: { title: t('component.variable.timeInterval.time-interval') },
    content: t('component.variable.tutorial.time-interval'),
    params: { placement: 'left' }
  },
  {
    target: '#v-step-sampling-interval',
    header: { title: t('component.variable.samplingInterval.sampling-interval') },
    content: t('component.variable.tutorial.sampling-interval'),
    params: { placement: 'left' }
  },
  {
    target: '#v-step-description',
    header: { title: t('component.common.description') },
    content: t('component.variable.tutorial.description'),
    params: { placement: 'top' }
  }
]
//#endregion

//#endregion

//#region modalFormLogic composable
const modalFormLogic = useModalFormLogic<VariableCreationDTO>({
  modalRef,
  nFormRef: formRef,
  getEmptyForm,
  create,
  update,
  reset,
  props,
  emit
})
//#endregion

//#region Methods

//#region Form lifecycle
function getEmptyForm(): VariableCreationDTO {
  return {
    uri: null,
    alternative_name: null,
    name: null,
    entity: null,
    entity_of_interest: null,
    characteristic: null,
    description: null,
    time_interval: null,
    sampling_interval: null,
    datatype: null,
    trait: null,
    trait_name: null,
    method: null,
    unit: null,
    exact_match: [],
    close_match: [],
    broad_match: [],
    narrow_match: [],
    species: null,
    linked_data_nb: 0
  }
}

async function reset(): Promise<void> {
  uriGenerated.value = true
  if (variableTutorial.value && !modalFormLogic.isEditMode.value) {
    variableTutorial.value.stop()
  }
}

async function create(formData: VariableCreationDTO) {
  return await service.createVariable(formData)
}

async function update(formData: VariableUpdateDTO) {
  return await service.updateVariable(formData)
}
//#endregion

//#region Autogenerated name
const selectedEntityName = ref<string>()
const selectedCharacteristicName = ref<string>()
const selectedMethodName = ref<string>()
const selectedUnitName = ref<string>()

function updateEntity(val: any) {
  selectedEntityName.value = val?.label
  updateName()
}

function updateCharacteristic(val: any) {
  selectedCharacteristicName.value = val?.label
  updateName()
}

function updateMethod(val: any) {
  selectedMethodName.value = val?.label
  updateName()
}

function updateUnit(val: any) {
  selectedUnitName.value = val?.label
  updateName()
}

function updateName() {
  if (modalFormLogic.isEditMode.value) return

  const parts: string[] = []
  if (selectedEntityName.value) parts.push(selectedEntityName.value.split(' ')[0])
  if (selectedCharacteristicName.value) parts.push(selectedCharacteristicName.value)
  if (selectedMethodName.value) parts.push(selectedMethodName.value)
  if (selectedUnitName.value) parts.push(selectedUnitName.value)

  if (parts.length) {
    modalFormLogic.form.value.name = parts.join('_')
    modalFormLogic.form.value.alternative_name = parts.slice(0, 2).join('_')
  } else {
    modalFormLogic.form.value.name = null
    modalFormLogic.form.value.alternative_name = null
  }
}
//#endregion

//#region Trait
function getEmptyTraitForm() {
  return { trait: modalFormLogic.form.value.trait, trait_name: modalFormLogic.form.value.trait_name }
}

function updateVariableTrait(form: any) {
  const bothFilled = !!form.trait && !!form.trait_name
  if (bothFilled || (!form.trait && !form.trait_name)) {
    modalFormLogic.form.value.trait = form.trait
    modalFormLogic.form.value.trait_name = form.trait_name
  }
}

function showTraitForm() {
  modalFormLogic.isEditMode.value
    ? traitForm.value?.showEditForm(getEmptyTraitForm())
    : traitForm.value?.showCreateForm()
}
//#endregion

//#region Datatypes
function loadDatatypes() {
  if (!datatypes.value.length) {
    service.getDatatypes().then((res: HttpResponse<OpenSilexResponse<VariableDatatypeDTO[]>>) => {
      datatypes.value = res.response.result
      updateDatatypeNodes()
    })
  } else {
    updateDatatypeNodes()
  }
}

function updateDatatypeNodes() {
  datatypesNodes.value = datatypes.value.map(dto => ({
    id: dto.uri,
    label: capitalize(t(dto.name))
  }))
}

async function loadDataType(uris: string[]) {
  if (!datatypes.value.length) {
    await service.getDatatypes().then((res: HttpResponse<OpenSilexResponse<VariableDatatypeDTO[]>>) => {
      datatypes.value = res.response.result
      updateDatatypeNodes()
    })
  }
  const set = new Set(uris)
  return datatypes.value.filter(dto => set.has(dto.uri))
}
//#endregion

//#region Helpers
function objectToSelectNode(dto: any) { return dto ? { id: dto.uri, label: dto.name } : null }
function capitalize(str: string) { return str.charAt(0).toUpperCase() + str.slice(1) }
//#endregion

//#region Tutorial
function continueFormEditing() {
  if (savedVariable.value) {
    Object.assign(modalFormLogic.form.value, savedVariable.value)
  }
}
//#endregion

//#region Agroportal entity creation handlers
function onEntityCreated(newEntityForm: any) {
  if (!newEntityForm?.uri) return
  modalFormLogic.form.value.entity = newEntityForm.uri
  selectedEntityName.value = newEntityForm.name
  updateName()
}

function onEntityOfInterestCreated(newInterestEntityForm: any) {
  if (!newInterestEntityForm?.uri) return
  modalFormLogic.form.value.entity_of_interest = newInterestEntityForm.uri
}

function onCharacteristicCreated(newCharacteristicForm: any) {
  if (!newCharacteristicForm?.uri) return
  modalFormLogic.form.value.characteristic = newCharacteristicForm.uri
  selectedCharacteristicName.value = newCharacteristicForm.name
  updateName()
}

function onMethodCreated(newMethodForm: any) {
  if (!newMethodForm?.uri) return
  modalFormLogic.form.value.method = newMethodForm.uri
  selectedMethodName.value = newMethodForm.name
  updateName()
}

function onUnitCreated(newUnitForm: any) {
  if (!newUnitForm?.uri) return
  modalFormLogic.form.value.unit = newUnitForm.uri
  selectedUnitName.value = newUnitForm.name
  updateName()
}
//#endregion

//#region Create modals
function showEntityCreateForm() { entityForm.value?.showCreateForm() }
function showInterestEntityCreateForm() { interestEntityForm.value?.showCreateForm() }
function showCharacteristicCreateForm() { characteristicForm.value?.showCreateForm() }
function showMethodCreateForm() { methodForm.value?.showCreateForm() }
function showUnitCreateForm() { unitForm.value?.showCreateForm() }
//#endregion

//#endregion

//#region Watchers & lifecycle
const langUnwatch = watch(() => locale.value, loadDatatypes)

onMounted(() => {
  loadDatatypes()
})

onBeforeUnmount(() => {
  langUnwatch()
})
//#endregion

//#endregion

defineExpose(modalFormLogic.exposed)

</script>

<style scoped>
#traitButton { padding-top: 23px; }
</style>
