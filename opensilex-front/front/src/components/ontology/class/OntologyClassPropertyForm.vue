<template>
  <n-form>

    <opensilex-InputForm
        :value.sync="rdf_type"
        label="component.common.type"
        type="text"
        :disabled="true"
    ></opensilex-InputForm>

    <!-- Parent -->
    <opensilex-FormSelector
        :selected.sync="form.property"
        :options="propertiesOptions"
        :required="true"
        label="OntologyClassPropertyForm.property"
        helpMessage="OntologyClassPropertyForm.property-help"
        @update:selected="updateIsListProperty"
    ></opensilex-FormSelector>


    <!-- is_required -->
    <opensilex-FormField
        :required="true"
        label="OntologyClassDetail.required"
        helpMessage="OntologyClassPropertyForm.required-help"
    >
      <template v-slot:field="field">
        <b-form-checkbox
            v-model="form.is_required" switch
        ></b-form-checkbox>
      </template>
    </opensilex-FormField>

    <!-- is_list -->
    <opensilex-FormField
        :required="true"
        label="OntologyClassDetail.list"
        helpMessage="OntologyClassPropertyForm.is-list-help"
    >
      <template v-slot:field="field">
        <b-form-checkbox
            :disabled="dataTypeProperties.indexOf(form.property) >= 0"
            v-model="form.is_list" switch
        ></b-form-checkbox>
      </template>
    </opensilex-FormField>

  </n-form>
</template>

<script setup lang="ts">
import {computed, inject, ref} from "vue";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import {ResourceTreeDTO} from "opensilex-core/model/resourceTreeDTO";
import HttpResponse, {OpenSilexResponse} from "@/lib/HttpResponse";
import {OntologyService} from "opensilex-core/api/ontology.service";
import {useI18n} from "vue-i18n";

const opensilex = inject<OpenSilexVuePlugin>("$opensilex");
const {t} = useI18n();

const availableProperties = ref();
const dataTypeProperties = ref([]);
const rdf_type = ref();
const domain = ref();

const propertiesOptions = computed(() => {
  return buildTreeListOptions(
      availableProperties.value,
      []
  );
})

const props = withDefaults(defineProps<{
  editMode: boolean,
  form: any
}>(), {
  form: {
    property: null,
    is_required: false,
    is_list: false
  }
});

defineExpose({
  getEmptyForm,
  setClassURI,
  setDomain,
  create,
  update
})

function disableIsListCheckBox(): boolean {
  let dataPropIdx = this.dataTypeProperties.indexOf(this.form.property);
  return dataPropIdx >= 0;
}

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

function setClassURI(rdf_type) {
  this.rdf_type = rdf_type;
}

function setDomain(domain) {
  this.domain = domain;
}

function updateIsListProperty() {
  if (!props.form.property || !dataTypeProperties) {
    return;
  }

  // if the property is a data property then set is_list to false, since we don't actually handle generics list component for data-property
  if (isDataProperty(props.form.property)) {
    props.form.is_list = false;
  }
}

function create(form) {
  let propertyForm = {
    rdf_type: rdf_type.value,
    property: form.property,
    required: form.is_required,
    list: form.is_list,
    domain: domain.value
  };

  return opensilex
      .getService<OntologyService>("opensilex.OntologyService")
      .addClassPropertyRestriction(propertyForm)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let msg = t("OntologyClassPropertyForm.link-success-msg", [form.property, form.rdf_type]).toString();
        opensilex.showSuccessToast(msg);
      })
      .catch(opensilex.errorHandler);
}

function update(form) {
  let propertyForm = {
    rdf_type: rdf_type.value,
    property: form.property,
    required: form.is_required,
    list: form.is_list,
    domain: domain.value
  };

  return opensilex
      .getService<OntologyService>("opensilex.OntologyService")
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
      t("OntologyPropertyForm.dataProperty") :
      t("OntologyPropertyForm.objectProperty");

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
fr:
  OntologyClassPropertyForm:
    property: Propriété
    link-success-msg: 'La propriété {0} a été ajoutée au type {1}'
    property-help: 'Selectionner la propriété à associer au type. Seul les propriétés qui ne sont pas déjà associées, sont sélectionnables.'
    required-help: Cocher cette case pour rendre cette propriété obligatoire pour le type selectionné
    is-list-help: 'Cocher cette case pour pouvoir utiliser une liste de valeurs. Seul les propriétés "objets" sont supportés.'


</i18n>