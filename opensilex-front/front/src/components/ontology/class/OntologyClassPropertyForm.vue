<template>
  <n-form
      ref="formRef"
      :model="form"
      :rules="rules"
  >
    <n-form-item>
      <InputForm
          v-model:value="data.classUri"
          label="component.common.type"
          type="text"
          :disabled="true"
      ></InputForm>
    </n-form-item>

    <!-- Parent -->
    <n-form-item path="property">
      <FormSelector
          v-model:selected="form.property"
          :options="propertiesOptions"
          :required="true"
          :label="t('OntologyClassPropertyForm.property')"
          :helpMessage="t('OntologyClassPropertyForm.property-help')"
          @update:selected="updateIsListProperty"
      ></FormSelector>
    </n-form-item>


    <!-- is_required -->
    <n-form-item path="is_required">
      <FormField
          :label="t('OntologyClassPropertyForm.required')"
          :helpMessage="t('OntologyClassPropertyForm.required-help')"
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
          :label="t('OntologyClassPropertyForm.list')"
          :helpMessage="t('OntologyClassPropertyForm.is-list-help')"
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
</template>

<script setup lang="ts">
import {computed, inject, onMounted, ref, useTemplateRef, watch, watchEffect} from "vue";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import {ResourceTreeDTO} from "opensilex-core/model/resourceTreeDTO";
import HttpResponse, {OpenSilexResponse} from "@/lib/HttpResponse";
import {OntologyService} from "opensilex-core/api/ontology.service";
import {useI18n} from "vue-i18n";
import {NSwitch, NForm, NFormItem, FormRules} from "naive-ui";
import FormSelector from "@/components/common/forms/FormSelector.vue";
import InputForm from "@/components/common/forms/InputForm.vue";
import FormField from "@/components/common/forms/FormField.vue";
import {required} from "@/models/FormFieldsFormatter";

//#region Public
const props = withDefaults(defineProps<{
  editMode: boolean,
  data: {
    domain: string,
    classUri: string
  }
}>(), {});

defineExpose({
  getEmptyForm,
  create,
  update
});

const emit = defineEmits<{
  validationChanged: [boolean]
}>();
//#endregion

//#region Private
const opensilex = inject<OpenSilexVuePlugin>("$opensilex");
const ontologyService = opensilex.getService<OntologyService>("opensilex.OntologyService");
const {t} = useI18n();

const formRef = useTemplateRef<InstanceType<typeof NForm>>("formRef")

const availableProperties = ref();
const dataTypeProperties = ref([]);
const form = ref({
  property: undefined,
  is_required: false,
  is_list: false
})

const propertiesOptions = computed(() => {
  return buildTreeListOptions(
      availableProperties.value,
      []
  );
})

const rules = ref<FormRules>({
  property: required('OntologyClassPropertyForm.property')
});

onMounted(() => {
  emit('validationChanged', false);
});

watch(form.value, async () => {
  const isValid = await validate();
  console.log(`Form changed : valid = ${isValid}`);
  emit('validationChanged', isValid);
});

watchEffect(() => {
  ontologyService.getLinkableProperties(props.data.classUri, props.data.domain).then((http) => {
    setProperties(http.response.result);
  });
});


function getEmptyForm() {
  return {
    property: null,
    is_required: false,
    is_list: false,
  };
}

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

function create(form) {
  let propertyForm = {
    rdf_type: props.data.classUri,
    property: form.property,
    required: form.is_required,
    list: form.is_list,
    domain: props.data.domain
  };

  return ontologyService
      .addClassPropertyRestriction(propertyForm)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let msg = t("OntologyClassPropertyForm.link-success-msg", [form.property, form.rdf_type]).toString();
        opensilex.showSuccessToast(msg);
      })
      .catch(opensilex.errorHandler);
}

function update(form) {
  let propertyForm = {
    rdf_type: props.data.classUri,
    property: form.property,
    required: form.is_required,
    list: form.is_list,
    domain: props.data.domain
  };

  return ontologyService
      .updateClassPropertyRestriction(propertyForm)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let msg = t("OntologyClassPropertyForm.link-success-msg", form.property, form.rdf_type).toString();
        opensilex.showSuccessToast(msg);
      })
      .catch(opensilex.errorHandler);
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
      t("OntologyClassPropertyForm.dataProperty") :
      t("OntologyClassPropertyForm.objectProperty");

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

async function validate(): Promise<boolean> {
  try {
    const errors = await formRef.value.validate();
    return !errors.warnings;
  } catch (error) {
    return false;
  }
}
//#endregion
</script>

<style scoped lang="scss">
</style>


<i18n>
en:
  OntologyClassPropertyForm:
    property: Property
    link-success-msg: 'The property {0} has been added to {0} type'
    property-help: Select the property to associate to the type. Only properties which are not already associated, are selectable.
    required-help: Check this checkbox to make this property required for the selected type.
    is-list-help: Check this checkbox in order to use multiple values. Currently only object-properties are supported.
    required: Required
    list: List of values
    dataProperty: Data property
    objectProperty: Object property
fr:
  OntologyClassPropertyForm:
    property: Propriété
    link-success-msg: 'La propriété {0} a été ajoutée au type {1}'
    property-help: 'Selectionner la propriété à associer au type. Seul les propriétés qui ne sont pas déjà associées, sont sélectionnables.'
    required-help: Cocher cette case pour rendre cette propriété obligatoire pour le type selectionné
    is-list-help: 'Cocher cette case pour pouvoir utiliser une liste de valeurs. Seul les propriétés "objets" sont supportés.'
    required: Obligatoire
    list: Liste de valeurs
    dataProperty: Propriété litérale
    objectProperty: Relation vers un objet
</i18n>