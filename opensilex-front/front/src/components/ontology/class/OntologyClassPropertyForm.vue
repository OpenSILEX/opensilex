<template>
  <Modal ref="modal">
    <template #header>
      <FormHeader :title="formTitle" icon="bi#bi-geo-alt"/>
    </template>

    <n-form
        ref="nForm"
        :rules="rules"
        :model="form"
    >
      <n-form-item>
        <InputForm
            :value="classUri"
            label="component.common.type"
            type="text"
            :disabled="true"
        ></InputForm>
      </n-form-item>

      <!-- Parent -->
      <FormSelector
          path="property"
          v-model:selected="form.property"
          :options="propertiesOptions"
          :required="true"
          :label="t('component.ontology.property.property')"
          :helpMessage="t('component.ontology.property.property-help')"
          @update:selected="updateIsListProperty"
      ></FormSelector>

      <!-- is_required -->
      <n-form-item path="is_required">
        <FormField
            :label="t('component.ontology.property.required')"
            :helpMessage="t('component.ontology.property.required-help')"
        >
          <template #field>
            <n-switch
                v-model:value="form.is_required"
                size="small"
            ></n-switch>
          </template>
        </FormField>
      </n-form-item>

      <!-- is_list -->
      <n-form-item path="is_list">
        <FormField
            :label="t('component.ontology.property.list')"
            :helpMessage="t('component.ontology.property.is-list-help')"
        >
          <template #field>
            <n-switch
                :disabled="dataTypeProperties.indexOf(form.property) >= 0"
                v-model:value="form.is_list"
                size="small"
            ></n-switch>
          </template>
        </FormField>
      </n-form-item>
    </n-form>

    <template #footer>
      <FormFooter @cancel="hide" @submit="submit"/>
    </template>
  </Modal>
</template>

<script setup lang="ts">
import {computed, inject, ref, useTemplateRef, watchEffect} from "vue";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import {ResourceTreeDTO} from "opensilex-core/model/resourceTreeDTO";
import {OntologyService} from "opensilex-core/api/ontology.service";
import {useI18n} from "vue-i18n";
import {NForm, NSwitch, NFormItem, FormRules} from "naive-ui";
import FormSelector from "@/components/common/forms/FormSelector.vue";
import InputForm from "@/components/common/forms/InputForm.vue";
import FormField from "@/components/common/forms/FormField.vue";
import Modal from "@/components/common/views/Modal.vue";
import FormHeader from "@/components/common/forms/FormHeader.vue";
import FormFooter from "@/components/common/forms/FormFooter.vue";
import useModalFormLogic, {ModalFormEmits, ModalFormProps} from "@/composables/useModalFormLogic";
import {required} from "@/models/FormFieldsFormatter";

//#region Public
const props = withDefaults(defineProps<ModalFormProps & {
  domain: string,
  classUri: string
}>(), {});

const emit = defineEmits<ModalFormEmits>();

export interface OntologyClassPropertyFormType {
  property: string,
  is_required: boolean,
  is_list: boolean
}

//#endregion

//#region Private
const opensilex = inject<OpenSilexVuePlugin>("$opensilex");
const ontologyService = opensilex.getService<OntologyService>("opensilex.OntologyService");
const {t} = useI18n();

const availableProperties = ref();
const dataTypeProperties = ref([]);
const rules: FormRules = {
  property: required(t('component.ontology.property.property'))
}

const propertiesOptions = computed(() => {
  return buildTreeListOptions(
      availableProperties.value,
      []
  );
})

watchEffect(() => {
  ontologyService.getLinkableProperties(props.classUri, props.domain).then((http) => {
    setProperties(http.response.result);
  });
});

const {
  form,
  formTitle,
  exposed,
  submit,
  hide
} = useModalFormLogic<OntologyClassPropertyFormType>({
  modalRef: useTemplateRef<InstanceType<typeof Modal>>('modal'),
  nFormRef: useTemplateRef<InstanceType<typeof NForm>>('nForm'),
  getEmptyForm: () => ({
    property: undefined,
    is_required: false,
    is_list: false,
  }),
  create,
  update,
  props,
  emit
});

function setProperties(properties: ResourceTreeDTO[]) {
  availableProperties.value = properties;

  dataTypeProperties.value = [];
  availableProperties.value.forEach((prop) => {
    if (prop.rdf_type == "owl:DatatypeProperty") {
      dataTypeProperties.value.push(prop.uri);
    }
  });
}

function updateIsListProperty() {
  if (!form.value.property || !dataTypeProperties) {
    return;
  }

  // if the property is a data property then set is_list to false, since we don't actually handle generics list component for data-property
  if (isDataProperty(form.value.property)) {
    form.value.is_list = false;
  }
}

function create(form: OntologyClassPropertyFormType) {
  let propertyForm = {
    rdf_type: props.classUri,
    property: form.property,
    required: form.is_required,
    list: form.is_list,
    domain: props.domain
  };

  return ontologyService.addClassPropertyRestriction(propertyForm);
}

function update(form: OntologyClassPropertyFormType) {
  let propertyForm = {
    rdf_type: props.classUri,
    property: form.property,
    required: form.is_required,
    list: form.is_list,
    domain: props.domain
  };

  return ontologyService.updateClassPropertyRestriction(propertyForm);
}

function buildTreeListOptions(resourceTrees: Array<any>, excludeProperties) {
  let options = [];

  if (resourceTrees != null) {
    resourceTrees.forEach((resourceTree: any) => {
      let subOption = buildTreeOptions(resourceTree, excludeProperties);
      options.push(subOption);
    });
  }

  return options;
}

function isDataProperty(property: string): boolean {
  return dataTypeProperties.value.indexOf(property) >= 0;
}

function buildTreeOptions(resourceTree: any, excludeProperties: Array<string>) {

  let dataProperty = isDataProperty(resourceTree.uri);
  let propertyType = dataProperty ?
      t("component.ontology.property.dataProperty") :
      t("component.ontology.property.objectProperty");

  let option = {
    id: resourceTree.uri,
    label: resourceTree.name + " (" + propertyType + ")",
    isDefaultExpanded: true,
    isDisabled: excludeProperties.indexOf(resourceTree.uri) >= 0,
    children: [],
  };

  resourceTree.children.forEach((child) => {
    let subOption = buildTreeOptions(child, excludeProperties);
    option.children.push(subOption);
  });

  if (resourceTree.disabled) {
    option.isDisabled = true;
  }

  if (option.children.length == 0) {
    delete option.children;
  }

  return option;
}

//#endregion

defineExpose(exposed)
</script>

<style scoped lang="scss">
</style>
