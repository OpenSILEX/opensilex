<template>
  <div id="v-step-global">
    <opensilex-Tutorial
      ref="variableTutorial"
      :steps="tutorialSteps"
      @onSkip="continueFormEditing"
      @onFinish="continueFormEditing"
      :editMode="editMode"
      class="variableFormTutorial"
    />

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
        :generated="uriGenerated"
        @update:generated="val => uriGenerated = val"
        :editMode="editMode"
        label="component.common.uri"
        class="v-step-uri"
      />

      <div class="row">
        <!-- ENTITY -->
        <div class="col-lg-6 variableFormSelectors" id="v-step-entity">
          <opensilex-EntitySelector
            path="entity"                            
            ref="entitySelector"
            label="component.variable.entity.entity"
            :placeholder="$t('component.variable.entity.entity-placeholder')"
            :helpMessage="$t('component.variable.entity.entity-help')"
            noResultsText="VariableForm.no-entity"
            v-model:selected="form.entity"
            :multiple="false"
            :required="true"                             
            :actionHandler="editMode ? undefined : showEntityCreateForm"
            :searchMethod="searchEntities"
            @select="updateEntity"
            :itemLoadingMethod="loadEntity"
            :conversionMethod="objectToSelectNode"
            :disabled="false"
          />
            <opensilex-AgroportalEntityForm
              ref="entityForm"
              ontologies="entities"
              @onCreate="onEntityCreated"
              @onUpdate="onEntityCreated"
            />
        </div>

        <!-- INTEREST ENTITY -->
        <div class="col-lg-6 variableFormSelectors" id="v-step-interestEntity">
          <opensilex-InterestEntitySelector
            path="entity_of_interest"                 
            ref="interestEntitySelector"
            label="component.variable.entityOfInterest.entityOfInterest"
            :placeholder="$t('component.variable.entityOfInterest.entityOfInterest-placeholder')"
            v-model:selected="form.entity_of_interest"
            :multiple="false"
            :required="false"
            :actionHandler="editMode ? undefined : showInterestEntityCreateForm"
            :helpMessage="$t('component.variable.entityOfInterest.interestEntity-help')"
            :searchMethod="searchInterestEntities"
            :itemLoadingMethod="loadInterestEntity"
            :conversionMethod="objectToSelectNode"
            noResultsText="VariableForm.no-interestEntity"
            :disabled="false"
          />
            <opensilex-AgroportalEntityOfInterestForm
              ref="interestEntityForm"
              ontologies="entities"
              @onCreate="onEntityOfInterestCreated"
              @onUpdate="onEntityOfInterestCreated"
            />
        </div>

        <!-- CHARACTERISTIC -->
        <div class="col-lg-6 variableFormSelectors" id="v-step-characteristic">
          <opensilex-CharacteristicSelector
            path="characteristic"                      
            ref="characteristicSelector"
            label="component.variable.characteristic.characteristic"
            :placeholder="$t('component.variable.characteristic.characteristic-placeholder')"
            v-model:selected="form.characteristic"
            :multiple="false"
            :required="true"                             
            @select="updateCharacteristic"
            :actionHandler="editMode ? undefined : showCharacteristicCreateForm"
            :helpMessage="$t('component.variable.characteristic.characteristic-help')"
            :searchMethod="searchCharacteristics"
            :itemLoadingMethod="loadCharacteristic"
            :conversionMethod="objectToSelectNode"
            noResultsText="VariableForm.no-characteristic"
            :disabled="false"
          />
            <opensilex-AgroportalCharacteristicForm
              ref="characteristicForm"
              ontologies="entities"
              @onCreate="onCharacteristicCreated"
              @onUpdate="onCharacteristicCreated"
            />
        </div>

        <!-- SPECIES (facultatif) -->
        <div class="col-lg-6 variableFormSelectors" id="v-step-species">
          <opensilex-SpeciesSelector
            v-if="!isGermplasmMenuExcluded"
            path="species"                                
            label="component.variable.species.species"
            :placeholder="$t('component.variable.species.select-multiple-placeholder')"
            :multiple="true"
            :checkable="true"
            v-model:selected="form.species"
          />
        </div>

        <!-- METHOD -->
        <div class="col-lg-6 variableFormSelectors" id="v-step-method">
          <opensilex-MethodSelector
            path="method"                                
            ref="methodSelector"
            label="component.variable.method.method"
            :placeholder="$t('component.variable.method.method-placeholder')"
            :multiple="false"
            :required="true"                             
            v-model:selected="form.method"
            :helpMessage="$t('component.variable.method.method-help')"
            noResultsText="VariableForm.no-method"
            :actionHandler="editMode ? undefined : showMethodCreateForm"
            @select="updateMethod"
            :searchMethod="searchMethods"
            :itemLoadingMethod="loadMethod"
            :conversionMethod="objectToSelectNode"
            :disabled="false"
          />
            <opensilex-AgroportalMethodForm
              ref="methodForm"
              ontologies="entities"
              @onCreate="onMethodCreated"
              @onUpdate="onMethodCreated"
            />
        </div>

        <!-- TRAIT BUTTON -->
        <div class="col-lg-6 variableFormSelectors" id="traitButton">
          <opensilex-Button
            label="component.variable.trait-button"
            helpMessage="component.variable.trait-button-help"
            @click="showTraitForm"
            :small="false"
            icon="fa#globe-americas"
            class="greenThemeColor"
          />
        </div>

        <opensilex-WizardForm
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
        <div class="col-lg-6 variableFormSelectors" id="v-step-unit">
          <opensilex-UnitSelector
            path="unit"                                 
            ref="unitSelector"
            label="component.variable.unit.unit"
            :placeholder="$t('component.variable.unit.unit-placeholder')"
            :multiple="false"
            :required="true"                             
            v-model:selected="form.unit"
            @select="updateUnit"
            :helpMessage="$t('component.variable.unit.unit-help')"
            :actionHandler="editMode ? undefined : showUnitCreateForm"
            :searchMethod="searchUnits"
            :itemLoadingMethod="loadUnit"
            :conversionMethod="objectToSelectNode"
            noResultsText="VariableForm.no-unit"
            :disabled="false"
          />
            <opensilex-AgroportalUnitForm
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
        <div class="col-lg-6 variableFormSelectors" id="v-step-name">
          <n-form-item :label="$t('component.common.name')" path="name">
            <opensilex-InputForm
              v-model:value="form.name"
              type="text"
              :required="true"
            />
          </n-form-item>
        </div>

        <!-- ALT NAME -->
        <div class="col-lg-6 variableFormSelectors" id="v-step-alt">
          <opensilex-InputForm
            v-model:value="form.alternative_name"
            label="component.variable.altName"
            type="text"
          />
        </div>

        <!-- DATATYPE -->
        <div class="col-lg-6 variableFormSelectors" id="v-step-datatype">
          <!-- Si VariableDataTypeSelector utilise FormSelector en interne,
               donner la prop `path="datatype"`.
               Sinon on l’enveloppe comme ci-dessous. -->
          <!-- <n-form-item :label="$t('component.variable.dataType.data-type')" path="datatype"> -->
            <opensilex-VariableDataTypeSelector
              path="datatype"
              v-model:selected="form.datatype"
              :label="'component.variable.dataType.data-type'"
              :placeholder="$t('component.variable.dataType.datatype-placeholder')"
              :required="true"
              :helpMessage="$t('component.variable.dataType.datatype-help')"
              :itemLoadingMethod="loadDataType"
              :conversionMethod="objectToSelectNode"
              :disabled="hasLinkedData"
              :options="datatypesNodes"
            />

          <!-- </n-form-item> -->
        </div>

        <!-- TIME INTERVAL (pas requis) -->
        <div class="col-lg-6 variableFormSelectors" id="v-step-time-interval">
          <opensilex-VariableTimeIntervalSelector
            label="component.variable.timeInterval.time-interval"
            v-model:selected="form.time_interval"
            :placeholder="$t('component.variable.timeInterval.time-interval-placeholder')"
          />
        </div>

        <!-- SAMPLING INTERVAL -->
        <div class="col-lg-6 variableFormSelectors" id="v-step-sampling-interval">
          <opensilex-FormSelector
            path="sampling_interval"                      
            label="component.variable.samplingInterval.sampling-interval"
            v-model:selected="form.sampling_interval"
            :multiple="false"
            :options="sampleList"
            :placeholder="$t('component.variable.samplingInterval.sampling-interval-placeholder')"
            :helpMessage="$t('component.variable.samplingInterval.sampling-interval-help')"
          />
        </div>

        <!-- DESCRIPTION -->
        <div class="col-xl-12" id="v-step-description">
          <opensilex-TextAreaForm
            v-model:value="form.description"
            label="component.common.description"
            @keydown.enter.stop
          />
        </div>
      </div>
    </n-form>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, watch, inject } from 'vue'
import { useI18n } from 'vue-i18n'
import { NForm, NFormItem } from 'naive-ui'
import {
  CharacteristicCreationDTO,
  EntityCreationDTO,
  InterestEntityCreationDTO,
  MethodCreationDTO,
  NamedResourceDTO,
  UnitCreationDTO,
  VariableDatatypeDTO,
  VariablesService
} from 'opensilex-core'
import { DataService } from 'opensilex-core/api/data.service'
import { VariableCreationDTO } from 'opensilex-core/model/variableCreationDTO'
import type { ValidationObserverInstance } from '@vee-validate/components'
import type { HttpResponse, OpenSilexResponse } from 'opensilex-core/HttpResponse'
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin"
import { requiredTrimmed } from  "../../../models/FormFieldsFormatter"
import AgroportalEntityForm from './../agroportal/AgroportalEntityForm.vue'
import AgroportalEntityOfInterestForm from './../agroportal/AgroportalEntityOfInterestForm.vue'
import AgroportalCharacteristicForm from './../agroportal/AgroportalCharacteristicForm.vue'
import AgroportalMethodForm from './../agroportal/AgroportalMethodForm.vue'
import AgroportalUnitForm from './../agroportal/AgroportalUnitForm.vue'

const props = defineProps({
  editMode: Boolean,
  uriGenerated: { type: Boolean, default: true },
  form: {
    type: Object as () => VariableCreationDTO,
    default: () => ({
      uri: undefined,
      alternative_name: undefined,
      name: undefined,
      entity: undefined,
      entity_of_interest: undefined,
      characteristic: undefined,
      description: undefined,
      time_interval: undefined,
      sampling_interval: undefined,
      datatype: undefined,
      trait: undefined,
      trait_name: undefined,
      method: undefined,
      unit: undefined,
      exact_match: [],
      close_match: [],
      broad_match: [],
      narrow_match: [],
      species: undefined,
      linked_data_nb: 0
    })
  }
})

const emit = defineEmits(['onCreate','onUpdate'])

/* Services */
const $opensilex = inject<OpenSilexVuePlugin>('opensilex')
const $store = inject<any>('$store')!
const { t, locale } = useI18n()
const service = $opensilex.getService<VariablesService>('opensilex.VariablesService')
const dataService = $opensilex.getService<DataService>('opensilex-core.DataService')

/* Refs */
const validatorRef = ref<InstanceType<typeof ValidationObserverInstance>>()
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

/* Datatypes */
const datatypes = ref<VariableDatatypeDTO[]>([])
const datatypesNodes = ref<any[]>([])

/* Lists */
const periodList = ref([
  'millisecond','second','minute','hour','day','week','month','year','unique'
].map(period => ({ id: t(`component.variable.dimensionValues.${period}`), label: t(`component.variable.dimensionValues.${period}`) })))

const sampleList = ref([
  'mm','cm','m','km','field','region'
].map(sample => ({ id: t(`component.variable.dimensionValues.${sample}`), label: t(`component.variable.dimensionValues.${sample}`) })))

/* Autogenerated name */
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
  if (props.editMode) return

  const parts: string[] = []
  if (selectedEntityName.value) parts.push(selectedEntityName.value.split(' ')[0])
  if (selectedCharacteristicName.value) parts.push(selectedCharacteristicName.value)
  if (selectedMethodName.value) parts.push(selectedMethodName.value)
  if (selectedUnitName.value) parts.push(selectedUnitName.value)

  if (parts.length) {
    props.form.name = parts.join('_')
    props.form.alternative_name = parts.slice(0, 2).join('_')
  } else {
    props.form.name = undefined
    props.form.alternative_name = undefined
  }
}

/* Trait */
function getEmptyTraitForm(){ return { trait: props.form.trait, trait_name: props.form.trait_name } }
function updateVariableTrait(form:any){
  const bothFilled = !!form.trait && !!form.trait_name
  if(bothFilled || (!form.trait && !form.trait_name)){
    props.form.trait = form.trait; props.form.trait_name = form.trait_name
  }
}

/* Datatypes load */
function loadDatatypes () {
  if (!datatypes.value.length) {
    service.getDatatypes().then((res: HttpResponse<OpenSilexResponse<VariableDatatypeDTO[]>>) => {
      datatypes.value = res.response.result               // <-- DTOs
      updateDatatypeNodes()
    })
  } else {
    updateDatatypeNodes()
  }
}

function updateDatatypeNodes () {
  datatypesNodes.value = datatypes.value.map(dto => ({
    id: dto.uri,
    label: capitalize(t(dto.name))
  }))
}

async function loadDataType (uris: string[]) {
  if (!datatypes.value.length) {
    await service.getDatatypes().then((res: HttpResponse<OpenSilexResponse<VariableDatatypeDTO[]>>) => {
      datatypes.value = res.response.result
      updateDatatypeNodes()
    })
  }
  const set = new Set(uris)
  return datatypes.value.filter(dto => set.has(dto.uri))   // <-- DTOs back
}

/* Helpers */
function objectToSelectNode(dto:any){ return dto ? { id: dto.uri, label: dto.name } : null }
function capitalize(str:string){ return str.charAt(0).toUpperCase() + str.slice(1) }

const langUnwatch = watch(() => locale.value, loadDatatypes)

onMounted(() => {
  loadDatatypes()
})
onBeforeUnmount(() => {
  langUnwatch()
})


/* Tutorial helpers */
function reset(){ 
  validatorRef.value?.reset(); 
  if(variableTutorial.value && !props.editMode){ 
    variableTutorial.value.stop() 
  } 
}

function validate(){ 
  return validatorRef.value?.validate() 
}

function tutorial(){ 
  savedVariable.value = JSON.parse(JSON.stringify(props.form)); 
  variableTutorial.value?.start() 
}

function continueFormEditing(){ 
  if(savedVariable.value){ 
    Object.assign(props.form, savedVariable.value) 
  } 
}

/* Computeds */
const isGermplasmMenuExcluded = computed(() => $opensilex.getConfig().menuExclusions.includes('germplasm'))
const hasLinkedData = computed(() => props.form?.linked_data_nb > 0)

/* API calls (search/load) */
function searchEntities(name:string,page:number,pageSize:number){ 
  return service.searchEntities(name, ['name=asc'], page, pageSize) 
}

function loadEntity(uris:Array<string|{uri:string}>){
  if(!uris || uris.length !== 1) return undefined
  const item = uris[0]
  if(typeof item === 'object' && 'uri' in item){ 
    return [props.form.entity] 
  }
  return service.getEntity(item).then(
    res => [res.response.result]
  )
}
function searchInterestEntities(name:string,page:number,pageSize:number){ 
  return service.searchInterestEntity(name, ['name=asc'], page, pageSize) 
}

function loadInterestEntity(uris:Array<string|{uri:string}>){
  if(!uris || uris.length !== 1) return undefined
  const item = uris[0]
  if(typeof item === 'object' && 'uri' in item){ 
    return [props.form.entity_of_interest] 
  }
  return service.getInterestEntity(item).then(res => [res.response.result])
}

function searchCharacteristics(name:string,page:number,pageSize:number){
  return service.searchCharacteristics(name, ['name=asc'], page, pageSize)
}

function loadCharacteristic(uris:Array<string|{uri:string}>){
  if(!uris || uris.length !== 1) return undefined
  const item = uris[0]
  if(typeof item === 'object' && 'uri' in item){
    return [props.form.characteristic] 
  }
  return service.getCharacteristic(item).then(res => [res.response.result])
}

function searchMethods(name:string,page:number,pageSize:number){ 
  return service.searchMethods(name, ['name=asc'], page, pageSize) 
}

function loadMethod(uris:Array<string|{uri:string}>){
  if(!uris || uris.length !== 1) return undefined
  const item = uris[0]
  if(typeof item === 'object' && 'uri' in item){ 
    return [props.form.method] 
  }
  return service.getMethod(item).then(res => [res.response.result])
}

function searchUnits(name:string,page:number,pageSize:number){ 
  return service.searchUnits(name, ['name=asc'], page, pageSize) 
}

function loadUnit(uris:Array<string|{uri:string}>){
  if(!uris || uris.length !== 1) return undefined
  const item = uris[0]
  if(typeof item === 'object' && 'uri' in item){ 
    return [props.form.unit] 
  }
  return service.getUnit(item).then(res => [res.response.result])
}

function onEntityCreated(newEntityForm: any) {
  // newEntityForm vient d'AgroportalCreateForm > contient au minimum `uri` et `name`
  if (!newEntityForm?.uri) return
  // 1. on met l’URI dans le formulaire de la variable
  props.form.entity = newEntityForm.uri

  // 2. on met à jour le nom auto-généré de la variable :
  selectedEntityName.value = newEntityForm.name
  updateName()
}

function onEntityOfInterestCreated(newInterestEntityForm: any) {
  if (!newInterestEntityForm?.uri) return
  props.form.entity_of_interest = newInterestEntityForm.uri
}

function onCharacteristicCreated(newCharacteristicForm: any) {
  if (!newCharacteristicForm?.uri) return
  props.form.characteristic = newCharacteristicForm.uri
  selectedCharacteristicName.value = newCharacteristicForm.name
  updateName()
}

function onMethodCreated(newMethodForm: any) {
  if (!newMethodForm?.uri) return
  props.form.method = newMethodForm.uri
  selectedMethodName.value = newMethodForm.name
  updateName()
}

function onUnitCreated(newUnitForm: any) {
  if (!newUnitForm?.uri) return
  props.form.unit = newUnitForm.uri
  selectedUnitName.value = newUnitForm.name
  updateName()
}

/* Create modals */
function showEntityCreateForm(){ entityForm.value?.showCreateForm() }
function showInterestEntityCreateForm(){ interestEntityForm.value?.showCreateForm() }
function showCharacteristicCreateForm(){ characteristicForm.value?.showCreateForm() }
function showMethodCreateForm(){ methodForm.value?.showCreateForm() }
function showUnitCreateForm(){ unitForm.value?.showCreateForm() }
function showTraitForm(){ props.editMode ? traitForm.value?.showEditForm(getEmptyTraitForm()) : traitForm.value?.showCreateForm() }

/* NForm rules */
const formRef = ref()
const rules = computed(() => ({
  entity:        { required: true, message: t('validations.required_if', { _field_: t('component.variable.entity.entity') }), trigger: ['change','blur'] },
  characteristic:{ required: true, message: t('validations.required_if', { _field_: t('component.variable.characteristic.characteristic') }), trigger: ['change','blur'] },
  method:        { required: true, message: t('validations.required_if', { _field_: t('component.variable.method.method') }), trigger: ['change','blur'] },
  unit:          { required: true, message: t('validations.required_if', { _field_: t('component.variable.unit.unit') }), trigger: ['change','blur'] },
  name:          requiredTrimmed('component.common.name'),
  datatype:      { required: true, message: t('validations.required_if', { _field_: t('component.variable.dataType.data-type') }), trigger: ['change','blur'] },
}))

async function validateForm() {
  try { 
    await formRef.value?.validate();
    return true 
  } catch { 
    return false 
  }
}

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

defineExpose({ 
  validate: validateForm,
  tutorial
})

</script>

<style scoped>
#traitButton { padding-top: 23px; }
.variableFormSelectors { margin-bottom: 15px; }
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
