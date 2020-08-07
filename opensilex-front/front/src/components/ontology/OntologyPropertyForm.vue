<template>
  <b-form v-if="form.labelTranslations">
    <opensilex-InputForm
      :value.sync="form.uri"
      label="component.common.uri"
      type="text"
      rules="url"
      :disabled="editMode"
      :required="true"
    ></opensilex-InputForm>

    <b-form-group :label="$t('OntologyPropertyForm.propertyType')">
      <b-form-radio
        v-model="form.type"
        name="propertyType"
        id="datatypeRadio"
        :value="OWL.DATATYPE_PROPERTY_URI"
      >{{$t("OntologyPropertyForm.dataProperty")}}</b-form-radio>
      <b-form-radio
        v-model="form.type"
        name="propertyType"
        :value="OWL.OBJECT_PROPERTY_URI"
      >{{$t("OntologyPropertyForm.objectProperty")}}</b-form-radio>
      <b-form-radio
        v-model="form.type"
        name="inheritedType"
        :value="null"
      >{{$t("OntologyPropertyForm.inheritedType")}}</b-form-radio>
    </b-form-group>

    <opensilex-SelectForm
      v-if="form.type == OWL.DATATYPE_PROPERTY_URI"
      label="OntologyPropertyForm.data-type"
      :required="true"
      :selected.sync="form.range"
      :options="datatypes"
    ></opensilex-SelectForm>

    <opensilex-SelectForm
      v-if="form.type == OWL.OBJECT_PROPERTY_URI"
      label="OntologyPropertyForm.object-type"
      :required="true"
      :selected.sync="form.range"
      :options="objectTypes"
    ></opensilex-SelectForm>

    <opensilex-SelectForm
      v-if="form.type == null"
      label="component.common.parent"
      :required="true"
      :selected.sync="form.parent"
      :options="availableParents"
    ></opensilex-SelectForm>

    <opensilex-InputForm
      :value.sync="form.labelTranslations.en"
      label="OntologyPropertyForm.labelEN"
      type="text"
      :required="true"
    ></opensilex-InputForm>

    <opensilex-TextAreaForm
      :value.sync="form.commentTranslations.en"
      label="OntologyPropertyForm.commentEN"
      :required="true"
    ></opensilex-TextAreaForm>

    <opensilex-InputForm
      :value.sync="form.labelTranslations.fr"
      label="OntologyPropertyForm.labelFR"
      type="text"
      :required="true"
    ></opensilex-InputForm>

    <opensilex-TextAreaForm
      :value.sync="form.commentTranslations.fr"
      label="OntologyPropertyForm.commentFR"
      :required="true"
    ></opensilex-TextAreaForm>
  </b-form>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
import OWL from "../../ontologies/OWL";
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";

@Component
export default class OntologyPropertyForm extends Vue {
  $opensilex: any;
  OWL = OWL;

  @Prop()
  editMode;

  @Prop({
    default: () => {
      return {
        uri: null,
        type: OWL.DATATYPE_PROPERTY_URI,
        parent: null,
        label: null,
        labelTranslations: {},
        comment: null,
        commentTranslations: {},
        domain: null,
        range: null
      };
    }
  })
  form;

  getEmptyForm() {
    return {
      uri: null,
      type: OWL.DATATYPE_PROPERTY_URI,
      parent: null,
      label: null,
      labelTranslations: {},
      comment: null,
      commentTranslations: {},
      domain: null,
      range: null
    };
  }

  get datatypes() {
    let datatypeOptions = [];
    for (let i in this.$opensilex.datatypes) {
      let label: any = this.$t(this.$opensilex.datatypes[i].labelKey);
      datatypeOptions.push({
        id: this.$opensilex.datatypes[i].uri,
        label: label.charAt(0).toUpperCase() + label.slice(1)
      });
    }

    datatypeOptions.sort((a, b) => {
      let comparison = 0;
      if (a.label > b.label) {
        comparison = 1;
      } else if (a.label < b.label) {
        comparison = -1;
      }
      return comparison;
    });

    return datatypeOptions;
  }

  get objectTypes() {
    let objectTypeOptions = [];
    for (let i in this.$opensilex.objectTypes) {
      objectTypeOptions.push({
        id: this.$opensilex.objectTypes[i].uri,
        label: this.$opensilex.objectTypes[i].rdfClass.label
      });
    }
    objectTypeOptions.sort((a, b) => {
      let comparison = 0;
      if (a.label > b.label) {
        comparison = 1;
      } else if (a.label < b.label) {
        comparison = -1;
      }
      return comparison;
    });

    return objectTypeOptions;
  }

  availableParents = [];
  parentByURI = {};
  setParentPropertiesTree(nodes) {
    this.parentByURI = {};
    this.availableParents = this.loadNodesRecursivly(nodes);
  }

  private loadNodesRecursivly(nodes) {
    let parents = [];
    for (let i in nodes) {
      let node = nodes[i];
      let selectItem: any = {
        id: node.data.uri,
        label: node.title
      };
      this.parentByURI[node.data.uri] = node.data;
      if (node.children && node.children.length > 0) {
        selectItem.children = this.loadNodesRecursivly(node.children);
      }
      parents.push(selectItem);
    }

    return parents;
  }

  private domain = null;
  setDomain(domain) {
    this.domain = domain;
  }

  private computeFormToSend(form) {
    let sentForm = {
      uri: form.uri,
      type: form.type,
      parent: form.parent,
      label: form.label,
      labelTranslations: form.labelTranslations,
      comment: form.comment,
      commentTranslations: form.commentTranslations,
      domain: this.domain,
      range: form.range
    };

    if (sentForm.type == null) {
      let parentType = this.parentByURI[form.parent];
      sentForm.type = parentType.type;
    } else {
      sentForm.parent = null;
    }

    return sentForm;
  }

  create(form) {
    return this.$opensilex
      .getService("opensilex.OntologyService")
      .createProperty(this.computeFormToSend(form))
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        console.debug("Property created", uri);
      })
      .catch(error => {
        if (error.status == 409) {
          console.error("Property already exists", error);
          this.$opensilex.errorHandler(
            error,
            this.$t("OntologyPropertyForm.property-already-exists")
          );
        } else {
          this.$opensilex.errorHandler(error);
        }
      });
  }

  update(form) {
    return this.$opensilex
      .getService("opensilex.OntologyService")
      .updateProperty(this.computeFormToSend(form))
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        console.debug("Property updated", uri);
      })
      .catch(this.$opensilex.errorHandler);
  }
}
</script>

<style scoped lang="scss">
</style>


<i18n>
en:
    OntologyPropertyForm:
        propertyType: Property Type
        dataProperty: Data property
        objectProperty: Object property
        inheritedType: Type inherited from parent
        data-type: Data type
        object-type: Object class
        labelEN: English name
        labelFR: French name
        commentEN: English description
        commentFR: French description
        property-already-exists: Property with same URI already exists

fr:
    OntologyPropertyForm:
        propertyType: Type de propriété
        dataProperty: Propriété litérale
        objectProperty: Relation vers un objet
        inheritedType: Type hérité du parent
        data-type: Type de donnée
        object-type: Classe d'objet
        labelEN: Nom anglais
        labelFR: Nom français
        commentEN: Description anglaise
        commentFR: Description française
        property-already-exists: Une propriété existe déjà avec la même URI
</i18n>