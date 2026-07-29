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
      :editMode="modalFormLogic.editMode.value"
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
            :editMode="modalFormLogic.editMode.value"
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
                noResultsText="VariableForm.no-entity"
                v-model:selected="modalFormLogic.form.value.entity"
                :multiple="false"
                :required="true"
                :actionHandler="modalFormLogic.editMode.value ? undefined : showEntityCreateForm"
                :searchMethod="searchEntities"
                @select="updateEntity"
                :itemLoadingMethod="loadEntity"
                :conversionMethod="objectToSelectNode"
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
                :actionHandler="modalFormLogic.editMode.value ? undefined : showInterestEntityCreateForm"
                :helpMessage="$t('component.variable.entityOfInterest.interestEntity-help')"
                :searchMethod="searchInterestEntities"
                :itemLoadingMethod="loadInterestEntity"
                :conversionMethod="objectToSelectNode"
                noResultsText="VariableForm.no-interestEntity"
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
                @select="updateCharacteristic"
                :actionHandler="modalFormLogic.editMode.value ? undefined : showCharacteristicCreateForm"
                :helpMessage="$t('component.variable.characteristic.characteristic-help')"
                :searchMethod="searchCharacteristics"
                :itemLoadingMethod="loadCharacteristic"
                :conversionMethod="objectToSelectNode"
                noResultsText="VariableForm.no-characteristic"
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
                noResultsText="VariableForm.no-method"
                :actionHandler="modalFormLogic.editMode.value ? undefined : showMethodCreateForm"
                @select="updateMethod"
                :searchMethod="searchMethods"
                :itemLoadingMethod="loadMethod"
                :conversionMethod="objectToSelectNode"
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
            createTitle="VariableForm.trait-form-create-title"
            editTitle="VariableForm.trait-form-edit-title"
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
                @select="updateUnit"
                :helpMessage="$t('component.variable.unit.unit-help')"
                :actionHandler="modalFormLogic.editMode.value ? undefined : showUnitCreateForm"
                :searchMethod="searchUnits"
                :itemLoadingMethod="loadUnit"
                :conversionMethod="objectToSelectNode"
                noResultsText="VariableForm.no-unit"
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
                :disabled="hasLinkedData"
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
import useModalFormLogic from '@/composables/useModalFormLogic'

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
import {VariableCreationDTO} from "opensilex-core/model/variableCreationDTO";
import {VariableUpdateDTO} from "opensilex-core/model/variableUpdateDTO";

//#region Public
const emit = defineEmits<{
  (e: 'onUpdate', payload: HttpResponse<OpenSilexResponse>): void
  (e: 'onCreate', payload: HttpResponse<OpenSilexResponse>): void
  (e: 'onSuccess'): void
}>()

const props = defineProps<{
  createTitle: string,
  editTitle: string
}>()
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
const traitSteps = [{ component: 'opensilex-TraitForm' }]

const datatypes = ref<VariableDatatypeDTO[]>([])
const datatypesNodes = ref<any[]>([])

const sampleList = ref([
  'mm','cm','m','km','field','region'
].map(sample => ({ id: t(`component.variable.dimensionValues.${sample}`), label: t(`component.variable.dimensionValues.${sample}`) })))

const isGermplasmMenuExcluded = computed(() => opensilex.getConfig().menuExclusions.includes('germplasm'))
const hasLinkedData = computed(() => modalFormLogic.form.value?.linked_data_nb > 0)

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
    content: t('VariableForm.tutorial.global'),
    params: { placement: 'bottom' }
  },
  {
    target: '#v-step-entity',
    header: { title: t('component.variable.entity.entity') },
    content: t('VariableForm.tutorial.entity'),
    params: { placement: 'left' }
  },
  {
    target: '#v-step-entity',
    header: { title: t('component.variable.entity.entity') },
    content: t('VariableForm.tutorial.entity-check'),
    params: { placement: 'left' }
  },
  {
    target: '#v-step-interestEntity',
    header: { title: t('component.variable.entityOfInterest.entityOfInterest') },
    content: t('VariableForm.tutorial.entityOfInterest'),
    params: { placement: 'left' }
  },
  {
    target: '#v-step-interestEntity',
    header: { title: t('component.variable.entityOfInterest.entityOfInterest') },
    content: t('VariableForm.tutorial.entityOfInterest-check'),
    params: { placement: 'left' }
  },
  {
    target: '#v-step-characteristic',
    header: { title: t('component.variable.characteristic.characteristic') },
    content: t('VariableForm.tutorial.characteristic'),
    params: { placement: 'left' }
  },
  {
    target: '#v-step-characteristic',
    header: { title: t('component.variable.characteristic.characteristic') },
    content: t('VariableForm.tutorial.characteristic-check'),
    params: { placement: 'right' }
  },
  {
    target: '#v-step-method',
    header: { title: t('component.variable.method.method') },
    content: t('VariableForm.tutorial.method'),
    params: { placement: 'left' }
  },
  {
    target: '#v-step-method',
    header: { title: t('component.variable.method.method') },
    content: t('VariableForm.tutorial.method-check'),
    params: { placement: 'right' }
  },
  {
    target: '#v-step-unit',
    header: { title: t('component.variable.unit.unit') },
    content: t('VariableForm.tutorial.unit'),
    params: { placement: 'left' }
  },
  {
    target: '#v-step-species',
    header: { title: t('component.experiment.species') },
    content: t('VariableForm.tutorial.species'),
    params: { placement: 'left' }
  },
  {
    target: '#v-step-name',
    header: { title: t('component.common.name') },
    content: t('VariableForm.tutorial.name'),
    params: { placement: 'left' }
  },
  {
    target: '#v-step-alt',
    header: { title: t('VariableForm.altName') },
    content: t('VariableForm.tutorial.altName'),
    params: { placement: 'left' }
  },
  {
    target: '#v-step-datatype',
    header: { title: t('component.variable.dataType.data-type') },
    content: t('VariableForm.tutorial.datatype'),
    params: { placement: 'left' }
  },
  {
    target: '#v-step-time-interval',
    header: { title: t('VariableForm.time-interval') },
    content: t('VariableForm.tutorial.time-interval'),
    params: { placement: 'left' }
  },
  {
    target: '#v-step-sampling-interval',
    header: { title: t('VariableForm.sampling-interval') },
    content: t('VariableForm.tutorial.sampling-interval'),
    params: { placement: 'left' }
  },
  {
    target: '#v-step-description',
    header: { title: t('component.common.description') },
    content: t('VariableForm.tutorial.description'),
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
  addTitle: props.createTitle,
  editTitle: props.editTitle,
  onCreate: (res) => emit('onCreate', res),
  onUpdate: (res) => emit('onUpdate', res),
  onSuccess: () => emit('onSuccess'),
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
  if (variableTutorial.value && !modalFormLogic.editMode.value) {
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
  if (modalFormLogic.editMode.value) return

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
  modalFormLogic.editMode.value
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

//#region API calls (search / load)
function searchEntities(name: string, page: number, pageSize: number) {
  return service.searchEntities(name, ['name=asc'], page, pageSize)
}

function loadEntity(uris: Array<string | { uri: string }>) {
  if (!uris || uris.length !== 1) return undefined
  const item = uris[0]
  if (typeof item === 'object' && 'uri' in item) {
    return [modalFormLogic.form.value.entity]
  }
  return service.getEntity(item).then(res => [res.response.result])
}

function searchInterestEntities(name: string, page: number, pageSize: number) {
  return service.searchInterestEntity(name, ['name=asc'], page, pageSize)
}

function loadInterestEntity(uris: Array<string | { uri: string }>) {
  if (!uris || uris.length !== 1) return undefined
  const item = uris[0]
  if (typeof item === 'object' && 'uri' in item) {
    return [modalFormLogic.form.value.entity_of_interest]
  }
  return service.getInterestEntity(item).then(res => [res.response.result])
}

function searchCharacteristics(name: string, page: number, pageSize: number) {
  return service.searchCharacteristics(name, ['name=asc'], page, pageSize)
}

function loadCharacteristic(uris: Array<string | { uri: string }>) {
  if (!uris || uris.length !== 1) return undefined
  const item = uris[0]
  if (typeof item === 'object' && 'uri' in item) {
    return [modalFormLogic.form.value.characteristic]
  }
  return service.getCharacteristic(item).then(res => [res.response.result])
}

function searchMethods(name: string, page: number, pageSize: number) {
  return service.searchMethods(name, ['name=asc'], page, pageSize)
}

function loadMethod(uris: Array<string | { uri: string }>) {
  if (!uris || uris.length !== 1) return undefined
  const item = uris[0]
  if (typeof item === 'object' && 'uri' in item) {
    return [modalFormLogic.form.value.method]
  }
  return service.getMethod(item).then(res => [res.response.result])
}

function searchUnits(name: string, page: number, pageSize: number) {
  return service.searchUnits(name, ['name=asc'], page, pageSize)
}

function loadUnit(uris: Array<string | { uri: string }>) {
  if (!uris || uris.length !== 1) return undefined
  const item = uris[0]
  if (typeof item === 'object' && 'uri' in item) {
    return [modalFormLogic.form.value.unit]
  }
  return service.getUnit(item).then(res => [res.response.result])
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

defineExpose({
  showCreateForm: modalFormLogic.showCreateForm,
  showEditForm: modalFormLogic.showEditForm
})
</script>

<style scoped>
#traitButton { padding-top: 23px; }
</style>

<i18n>
en:
  VariableForm:
    altName: Alternative name
    time-interval: Time interval
    sampling-interval: Sample interval
    tutorial:
      global: "Create a variable : Before creating a new variable, make sur you check the existing ones in order to avoid duplicates. For example 'grain yield at harvest'."
      entity: "Select the entity that is the object of the observation/measurement. Here 'Grain'."
      entity-check: "If the entity is not already present in the list you can add it. Double check if there is no other spelling - seed, crop, etc."
      entityOfInterest: "Select the entity of interest that is the object of the observation/measurement."
      entityOfInterest-check: "If the entity of interest is not already present in the list you can add it. Double check if there is no other spelling."
      characteristic: "Select the measured characteristic. Here 'Yield' "
      characteristic-check: "If the characteristic is not in the list you can add it. Double check if it is not already present under another name."
      method: "Select the method that is associated with this variable. In our case this is a yield sensor onboard the harvester."
      method-check: "If the method is not present you can add it. Don't neglect the description as it is especially important for methods."
      unit: "Select the unit in which the variable is measured. What should I do if the unit is different from what I have measured ? I can select kg/ha, but my measurements are in t/ha.
        1 - I convert the measurements I have into the appropriate unit.
        2 - I declare a new Unit. This is highly advised to not create too many units and prefer convert into the existing units."
      name: "Precise the variable name. By default this field is auto filled according the entity and characteristic name, but it can be filled manually."
      altName: "Precise the alternative variable name if it exist. By default this field is auto filled according the entity, characteristic, method and unit names, but this field can be filled manually."
      time-interval: "Precise the time interval which associated with this variable. Here we obtained the grain yield each month."
      sampling-interval: "Precise the sample interval which is associated with this variable. Here we obtained the grain yield by harvesting experimental microplot (10m * 2.5m)."
      datatype: "Precise the data type. Here we are using decimal numbers."
      description: "Finalize the variable with some text description of it."
      species: "Select the species that is associated with this variable. Here rice."

fr:
  VariableForm:
    altName: Nom alternatif
    time-interval: Intervalle de temps
    sampling-interval: Échantillonnage
    tutorial:
      global: "Création de variable : Avant de créer une variable, soyez bien sûr d'avoir vérifié la liste existante pour ne pas introduire de doublon. Par exemple 'Rendement du grain à la récolte'."
      entity: "Sélectionner l'entité sur laquelle la variable est mesurée/observée. Ici le 'grain'."
      entity-check: "Si l'entité n'est pas dans la liste, vous pouvez la créer. Vérifier toutefois des orthographes alternatives - seed, crop, etc."
      entityOfInterest: "Sélectionner l'entité d'intérêt sur laquelle la variable est mesurée/observée."
      entityOfInterest-check: "Si l'entité d'intérêt n'est pas dans la liste, vous pouvez la créer. Vérifier toutefois des orthographes alternatives."
      characteristic: "Sélectionner la caractéristique mesurée. Ici 'rendement'."
      characteristic-check: "Si la caractéristique n'est pas dans la liste, vous pouvez l'ajouter. Vérifier encore une fois que la caractéristique n'est pas présente sous un autre nom."
      method: " Sélectionner la méthode qui vous a permis de réaliser cette variable. Dans notre cas, un capteur embarqué à bord de la moissoneuse-batteuse."
      method-check: "Si la méthode n'est pas présente, vous pouvez l'ajouter. Ne pas oublier de bien renseigner la description, c'est particulièrement important pour la méthode."
      unit: "Sélectionner l'unité dans laquelle est exprimée la variable. Que faire si l'unité proposée ne correspond pas à ma mesure ? On me propose kg/ha, mais j'ai des mesures en t/ha ?
        1 - Je convertie ma variable dans la bonne unité.
        2 - Je crée une nouvelle unité. Il vaut mieux limiter la création de multiples unités, privilégier la conversion."
      name: "Renseigner le nom de cette variable. Par défault ce champ est rempli automatiquement en fonction de l'entité et de la caractéristique, mais il peut être rempli manuellement."
      altName: "Renseigner le nom alternatif de cette variable si il existe. Par défault ce champ est rempli automatiquement en fonction de l'entité, de la caractéristique, de la méthode et de l'unité, mais il peut être rempli manuellement."
      time-interval: "Renseigner le pas-de-temps qui a permis d'obtenir cette variable. Ici le rendement est mesuré chaque mois."
      sampling-interval: "Renseigner l'échantillonnage qui a permis d'obtenir cette variable. Ici on a obtenu le rendement sur une microparcelle expérimentale de taille standard (2.5m*10m)."
      datatype: "Renseigner le type de données. Ici nous avons des nombre décimaux."
      description: "Finaliser la variable avec une description textuelle de la variable."
      species: "Sélectionner l'espèce associée à la variable variable. Ici le 'riz'."
</i18n>
