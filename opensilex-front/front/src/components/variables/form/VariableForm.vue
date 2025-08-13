<template>
  <div id="v-step-global">
    <opensilex-Tutorial
      ref="variableTutorial"
      :steps="tutorialSteps"
      @onSkip="continueFormEditing"
      @onFinish="continueFormEditing"
      :editMode="editMode"
    />

    <opensilex-UriForm
      v-model:uri="form.uri"
      :generated="uriGenerated"
      @update:generated="val => uriGenerated = val"
      :editMode="editMode"
      label="component.common.uri"
    />

    <div class="row">
      <!-- ENTITY -->
      <div class="col-lg-6 variableFormSelectors" id="v-step-entity">
        <opensilex-EntitySelector
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
          @loadMoreItems="loadMoreItems(entitySelector)"
        />
        <!-- <opensilex-AgroportalEntityForm ref="entityForm" @onCreate="setLoadedEntity" /> -->
      </div>

      <!-- INTEREST ENTITY -->
      <div class="col-lg-6 variableFormSelectors" id="v-step-interestEntity">
        <opensilex-InterestEntitySelector
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
          @loadMoreItems="loadMoreItems(interestEntitySelector)"
        />
        <!-- <opensilex-AgroportalEntityOfInterestForm ref="interestEntityForm" @onCreate="setLoadedInterestEntity" /> -->
      </div>

      <!-- CHARACTERISTIC -->
      <div class="col-lg-6 variableFormSelectors" id="v-step-characteristic">
        <opensilex-CharacteristicSelector
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
          @loadMoreItems="loadMoreItems(characteristicSelector)"
        />
        <!-- <opensilex-AgroportalCharacteristicForm ref="characteristicForm" @onCreate="setLoadedCharacteristic" /> -->
      </div>

      <!-- SPECIES -->
      <div class="col-lg-6 variableFormSelectors" id="v-step-species">
        <opensilex-SpeciesSelector
          v-if="!isGermplasmMenuExcluded"
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
          @loadMoreItems="loadMoreItems(methodSelector)"
        />
        <!-- <opensilex-AgroportalMethodForm ref="methodForm" @onCreate="setLoadedMethod" /> -->
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
          @loadMoreItems="loadMoreItems(unitSelector)"
        />
        <!-- <opensilex-AgroportalUnitForm ref="unitForm" @onCreate="setLoadedUnit" /> -->
      </div>
    </div>

    <hr />

    <div class="row">
      <div class="col-lg-6 variableFormSelectors" id="v-step-name">
        <opensilex-InputForm
          v-model:value="form.name"
          label="component.common.name"
          type="text"
          :required="true"
        />
      </div>

      <div class="col-lg-6 variableFormSelectors" id="v-step-alt">
        <opensilex-InputForm
          v-model:value="form.alternative_name"
          label="component.variable.altName"
          type="text"
        />
      </div>

      <div class="col-lg-6 variableFormSelectors" id="v-step-datatype">
        <opensilex-VariableDataTypeSelector
          label="component.variable.dataType.data-type"
          :placeholder="$t('component.variable.dataType.datatype-placeholder')"
          :required="true"
          v-model:selected="form.datatype"
          :helpMessage="$t('component.variable.dataType.datatype-help')"
          :itemLoadingMethod="loadDataType"
          :disabled="hasLinkedData"
          :options="datatypesNodes"
        />
      </div>

      <div class="col-lg-6 variableFormSelectors" id="v-step-time-interval">
        <opensilex-VariableTimeIntervalSelector
          label="component.variable.timeInterval.time-interval"
          v-model:timeinterval="form.time_interval"
          :placeholder="$t('component.variable.timeInterval.time-interval-placeholder')"
        />
      </div>

      <!-- <div class="col-lg-6"></div> -->

      <div class="col-lg-6 variableFormSelectors" id="v-step-sampling-interval">
        <opensilex-FormSelector
          label="component.variable.samplingInterval.sampling-interval"
          v-model:selected="form.sampling_interval"
          :multiple="false"
          :options="sampleList"
          :placeholder="$t('component.variable.samplingInterval.sampling-interval-placeholder')"
          :helpMessage="$t('component.variable.samplingInterval.sampling-interval-help')"
        />
      </div>

      <div class="col-xl-12" id="v-step-description">
        <opensilex-TextAreaForm
          v-model:value="form.description"
          label="component.common.description"
          @keydown.enter.stop
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, watch, inject } from 'vue';
import { useI18n } from 'vue-i18n';
import {
  CharacteristicCreationDTO,
  EntityCreationDTO,
  InterestEntityCreationDTO,
  MethodCreationDTO,
  NamedResourceDTO,
  UnitCreationDTO,
  VariableDatatypeDTO,
  VariablesService
} from 'opensilex-core';
import { DataService } from 'opensilex-core/api/data.service';
import { VariableCreationDTO } from 'opensilex-core/model/variableCreationDTO';
import type { ValidationObserverInstance } from '@vee-validate/components';
import type { HttpResponse, OpenSilexResponse } from 'opensilex-core/HttpResponse';
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin"

// Props
const props = defineProps({
  editMode: Boolean,
  uriGenerated: {
    type: Boolean,
    default: true
  },
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
});


const emit = defineEmits(["onCreate", "onUpdate"]);

// Services
const $opensilex = inject<OpenSilexVuePlugin>('opensilex');
const $store = inject<any>('$store')!;
const { t, locale } = useI18n();
const service = $opensilex.getService<VariablesService>('opensilex.VariablesService');
const dataService = $opensilex.getService<DataService>('opensilex-core.DataService');

// Form refs
const validatorRef = ref<InstanceType<typeof ValidationObserverInstance>>();
const variableTutorial = ref();

// UI Selectors
const entitySelector = ref();
const interestEntitySelector = ref();
const characteristicSelector = ref();
const methodSelector = ref();
const unitSelector = ref();
const traitForm = ref();

// External Forms
const entityForm = ref();
const interestEntityForm = ref();
const characteristicForm = ref();
const methodForm = ref();
const unitForm = ref();

// Tutorial State
const savedVariable = ref<VariableCreationDTO>();

const traitSteps = [{ component: 'opensilex-TraitForm' }];

// Datatypes and Options
const datatypes = ref<VariableDatatypeDTO[]>([]);
const datatypesNodes = ref<any[]>([]);

const periodList = ref([
  "millisecond", "second", "minute", "hour", "day", "week", "month", "year", "unique"
].map(p => ({
  id: t(`component.variable.dimensionValues.${p}`),
  label: t(`component.variable.dimensionValues.${p}`)
})));

const sampleList = ref([
  "mm", "cm", "m", "km", "field", "region"
].map(s => ({
  id: t(`component.variable.dimensionValues.${s}`),
  label: t(`component.variable.dimensionValues.${s}`)
})));

// Autogenerated name logic
const selectedEntityName = ref();
const selectedCharacteristicName = ref();
const selectedMethodName = ref();
const selectedUnitName = ref();

function updateEntity(val: any) {
  selectedEntityName.value = val.label;
  updateName();
}
function updateCharacteristic(val: any) {
  selectedCharacteristicName.value = val.label;
  updateName();
}
function updateMethod(val: any) {
  selectedMethodName.value = val.label;
  updateName();
}
function updateUnit(val: any) {
  selectedUnitName.value = val.label;
  updateName();
}

function updateName() {
  if (!props.editMode) {
    const form = props.form;
    const parts: string[] = [];

    if (selectedEntityName.value) parts.push(selectedEntityName.value.split(' ')[0]);
    if (selectedCharacteristicName.value) parts.push(selectedCharacteristicName.value);
    if (selectedMethodName.value) parts.push(selectedMethodName.value);
    if (selectedUnitName.value) parts.push(selectedUnitName.value);

    if (parts.length) {
      form.name = parts.join('_');
      form.alternative_name = parts.slice(0, 2).join('_');
    }
  }
}

// Wizard steps
function getEmptyTraitForm() {
  return {
    trait: props.form.trait,
    trait_name: props.form.trait_name
  };
}

function updateVariableTrait(form) {
  const bothFilled = !!form.trait && !!form.trait_name;
  if (bothFilled || (!form.trait && !form.trait_name)) {
    props.form.trait = form.trait;
    props.form.trait_name = form.trait_name;
  }
}

// DataType methods
function loadDatatypes() {
  console.log("loadDatatype")
  if (!datatypes.value.length) {
      console.log("loadDatatype if")
    service.getDatatypes().then((res: HttpResponse<OpenSilexResponse<VariableDatatypeDTO[]>>) => {
      console.log("res.response.result ", res.response.result)
      datatypes.value = res.response.result;
      updateDatatypeNodes();
    });
  } else {
    updateDatatypeNodes();
  }
}
function updateDatatypeNodes() {
  datatypesNodes.value = datatypes.value.map(dto => ({
    id: dto.uri,
    label: capitalize(t(dto.name))
  }));
}

function loadDataType(uris: Array<string | { uri: string }>) {
  const ids = uris.map(elementUri => (typeof elementUri === 'string' ? elementUri : elementUri.uri))
  // on réutilise le tableau 'datatypes' (DTOs) reçu de l'API
  return Promise.resolve(
    ids.map(id => {
      const dto = datatypes.value.find(datatype => datatype.uri === id)
      return { uri: id, name: dto?.name ?? id }  // format DTO
    })
  )
}


// Common helpers
function objectToSelectNode(dto: any) {
  return dto ? { id: dto.uri, label: dto.name } : null;
}
function capitalize(str: string) {
  return str.charAt(0).toUpperCase() + str.slice(1);
}

// Lang watcher
const langUnwatch = watch(() => locale.value, loadDatatypes);

onMounted(() => {
  loadDatatypes();
});
onBeforeUnmount(() => {
  langUnwatch();
});

function reset() {
  validatorRef.value?.reset();
  if (variableTutorial.value && !props.editMode) {
    variableTutorial.value.stop();
  }
}
function validate() {
  return validatorRef.value?.validate();
}

function tutorial() {
  savedVariable.value = JSON.parse(JSON.stringify(props.form));
  variableTutorial.value?.start();
}
function continueFormEditing() {
  if (savedVariable.value) {
    Object.assign(props.form, savedVariable.value);
  }
}

const isGermplasmMenuExcluded = computed(() => {
  return $opensilex.getConfig().menuExclusions.includes("germplasm");
});

const hasLinkedData = computed(() => {
  return props.form?.linked_data_nb > 0;
});



function isUriObject(item: any): item is { uri: string } {
  return typeof item === 'object' && item !== null && 'uri' in item;
}



// Search & Load Entity
function searchEntities(name: string, page: number, pageSize: number) {
  return service.searchEntities(name, ["name=asc"], page, pageSize);
}
function loadEntity(uris: Array<string | { uri: string }>) {
  if (!uris || uris.length !== 1) return undefined;

  const item = uris[0];

  if (typeof item === 'object' && 'uri' in item) {
    return [props.form.entity];
  }

  return service.getEntity(item).then(res => [res.response.result]);
}

function setLoadedEntity(created: EntityCreationDTO) {
  props.form.entity = created.uri;
  entitySelector.value.select({ id: created.uri, label: created.name });
}

// Search & Load InterestEntity
function searchInterestEntities(name: string, page: number, pageSize: number) {
  return service.searchInterestEntity(name, ["name=asc"], page, pageSize);
}
function loadInterestEntity(uris: Array<string | { uri: string }>) {
  if (!uris || uris.length !== 1) return undefined;

  const item = uris[0];

  if (typeof item === 'object' && 'uri' in item) {
    return [props.form.entity_of_interest];
  }

  return service.getInterestEntity(item).then(res => [res.response.result]);
}


function setLoadedInterestEntity(created: InterestEntityCreationDTO) {
  props.form.entity_of_interest = created.uri;
  interestEntitySelector.value.select({ id: created.uri, label: created.name });
}

// Search & Load Characteristic
function searchCharacteristics(name: string, page: number, pageSize: number) {
  return service.searchCharacteristics(name, ["name=asc"], page, pageSize)
    .then((res) => {
      return res;
    })
    .catch((err) => {
      throw err;
    });
}

function loadCharacteristic(uris: Array<string | { uri: string }>) {
  if (!uris || uris.length !== 1) return undefined;

  const item = uris[0];

  if (typeof item === 'object' && 'uri' in item) {
    return [props.form.characteristic];
  }

  return service.getCharacteristic(item).then(res => [res.response.result]);
}

function setLoadedCharacteristic(created: CharacteristicCreationDTO) {
  props.form.characteristic = created.uri;
  characteristicSelector.value.select({ id: created.uri, label: created.name });
}

// Search & Load Method
function searchMethods(name: string, page: number, pageSize: number) {
  return service.searchMethods(name, ["name=asc"], page, pageSize);
}
function loadMethod(uris: Array<string | { uri: string }>) {
  if (!uris || uris.length !== 1) return undefined;

  const item = uris[0];

  if (typeof item === 'object' && 'uri' in item) {
    return [props.form.method];
  }

  return service.getMethod(item).then(res => [res.response.result]);
}
function setLoadedMethod(created: MethodCreationDTO) {
  props.form.method = created.uri;
  methodSelector.value.select({ id: created.uri, label: created.name });
}

// Search & Load Unit
function searchUnits(name: string, page: number, pageSize: number) {
  return service.searchUnits(name, ["name=asc"], page, pageSize);
}
function loadUnit(uris: Array<string | { uri: string }>) {
  if (!uris || uris.length !== 1) return undefined;

  const item = uris[0];

  if (typeof item === 'object' && 'uri' in item) {
    return [props.form.unit];
  }

  return service.getUnit(item).then(res => [res.response.result]);
}
function setLoadedUnit(created: UnitCreationDTO) {
  props.form.unit = created.uri;
  unitSelector.value.select({ id: created.uri, label: created.name });
}

// Empty form factory
function getEmptyForm(): VariableCreationDTO {
  return {
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
  };
}

// Form creation modals
function showEntityCreateForm() {
  entityForm.value?.showCreateForm();
}
function showInterestEntityCreateForm() {
  interestEntityForm.value?.showCreateForm();
}
function showCharacteristicCreateForm() {
  characteristicForm.value?.showCreateForm();
}
function showMethodCreateForm() {
  methodForm.value?.showCreateForm();
}
function showUnitCreateForm() {
  unitForm.value?.showCreateForm();
}
function showTraitForm() {
  if (props.editMode) {
    traitForm.value?.showEditForm(getEmptyTraitForm());
  } else {
    traitForm.value?.showCreateForm();
  }
}
</script>

<style scoped>
#traitButton {
  padding-top: 23px;
}

.variableFormSelectors {
  margin-bottom: 15px;
}
</style>
