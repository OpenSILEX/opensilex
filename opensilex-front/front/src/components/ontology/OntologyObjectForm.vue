<template>
  <n-form
    v-if="form"
    :model="form"
    :rules="rules"
    label-placement="top"
    :show-require-mark="true"
  >
    <!-- URI -->
    <UriForm
      v-model:uri="form.uri"
      :generated="uriGenerated"
      @update:generated="val => (uriGenerated = val)"
      :editMode="editMode"
      label="component.common.forms-generic-placeholders.uri-label"
      helpMessage="component.common.uri-help-message"
    ></UriForm>

    <!-- Name -->
    <InputForm
      v-model:value="form.name"
      label="component.common.name"
      type="text"
      :required="true"
      placeholder="component.common.forms-generic-placeholders.form-name-placeholder"
    ></InputForm>

    <!-- Type -->
    <n-form-item path="rdf_type" ref="rdfTypeItem">
      <TypeForm
        v-if="baseType"
        v-model:type="form.rdf_type"
        :baseType="baseType"
        :required="true"
        :disabled="editMode"
        :ignoreRoot="false"
        placeholder="component.common.forms-generic-placeholders.form-type-placeholder"
        @select="typeSwitch($event.id,false)"
      ></TypeForm>
    </n-form-item>

    <!-- Custom properties -->
    <OntologyRelationsForm
      v-if="baseType && loadCustomProperties"
      ref="ontologyRelationsForm"
      :rdfType="form.rdf_type"
      :typeToLoad="currentType"
      :relations="form.relations"
      :excludedProperties="excludedProperties"
      :customComponentProps="customComponentProps"
      :baseType="baseType"
      :editMode="editMode"
      :context="context ? { experimentURI: context} : undefined"
      :initHandler="initHandler"
    ></OntologyRelationsForm>

    <slot v-if="form.rdf_type" v-bind:form="form"></slot>
  </n-form>
</template>

<script setup lang="ts">
import {computed, inject, onMounted, onUnmounted, ref, watch} from "vue";
import OntologyRelationsForm from "./OntologyRelationsForm.vue";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {MultiValuedRDFObjectRelation} from "./models/MultiValuedRDFObjectRelation";
import Rdfs from "../../ontologies/Rdfs";
import DC from "../../ontologies/DC";
import type {RDFObjectRelationDTO} from "opensilex-core/model/rDFObjectRelationDTO";
import UriForm from "@/components/common/forms/UriForm.vue";
import InputForm from "@/components/common/forms/InputForm.vue";
import TypeForm from "@/components/common/forms/TypeForm.vue";
import {
  NForm,
  NFormItem
} from 'naive-ui'
import {FormItemInst} from "naive-ui";
import {useI18n} from "vue-i18n";

/*
 * Component used for handling URI, type, name and custom properties for a given type
 */

//#region Constant values
const { t } = useI18n();

export interface OntologyObjectFormModel{
  uri?: string,
  rdf_type?: string,
  name: string,
  relations: Array<RDFObjectRelationDTO>
}
//#endregion

//#region Reactive data
const uriGenerated = ref(true);

const excludedProperties = ref<Set<string>>(new Set<string>([
  Rdfs.getShortURI(Rdfs.LABEL),
  DC.MODIFIED,
  DC.ISSUED,
  DC.PUBLISHER
]));

const customComponentProps = ref<Map<string, Map<string, any>>>(new Map<string, Map<string, any>>());

const initHandler = ref<(relation: MultiValuedRDFObjectRelation) => void>(
  (relation: MultiValuedRDFObjectRelation) => {}
);

const loadCustomProperties = ref<boolean>(true);
//#endregion

//#region Template refs
const ontologyRelationsForm = ref<InstanceType<typeof OntologyRelationsForm>>(null);
const rdfTypeItem = ref<FormItemInst | null>(null)
//#endregion

//#region Props
interface Props {
  editMode: boolean,
  form: OntologyObjectFormModel,
  context?: string,
  currentType: string,
  baseType: string
}

const props = withDefaults(
  defineProps<Props>(),
  {
    form: () => ( {
      uri: null,
      rdf_type: null,
      name: "",
      relations: []
    })
  }
);
//#endregion

//#region functions

function setInitHandler(handler) {
  initHandler.value = handler;
}

function setExcludedProperties(excludedPropertiess: Set<string>) {
  excludedProperties.value = excludedPropertiess;
}

function setCustomComponentProps(customComponentPropss: Map<string, Map<string, any>>){
  customComponentProps.value = customComponentPropss;
}

function setLoadCustomProperties(loadCustomPropertiess: boolean){
  loadCustomProperties.value = loadCustomPropertiess;
}

function updateRelations() {
  ontologyRelationsForm.value.updateRelation(null, null);
}

async function typeSwitch(type: string, initialLoad: boolean) {
  if(ontologyRelationsForm.value){
    await ontologyRelationsForm.value.typeSwitch(type, initialLoad);
  }
}
//#endregion

//#region Computed
const rules = computed(() => ({
  'name': {
    required: true,
    message: t('validations.required_if', {_field_: t('component.common.name')}),
    trigger: ['blur', 'change']
  },
  'rdf_type': {
    required: true,
    message: t('validations.required_if', {_field_: t('component.common.type')}),
    trigger: ['change', 'blur']
  }
}))
//#endregion

//#region Watch towers
//A watcher to remove error state when a new type is filled
watch(
  () => props.form.rdf_type,
  () => rdfTypeItem.value?.restoreValidation(),
  { flush: 'post' }
);
//#endregion

//#region Exposed
defineExpose({
  setInitHandler,
  updateRelations,
  setExcludedProperties,
  setCustomComponentProps,
  setLoadCustomProperties
})
//#endregion

</script>

<style scoped lang="scss">
</style>



