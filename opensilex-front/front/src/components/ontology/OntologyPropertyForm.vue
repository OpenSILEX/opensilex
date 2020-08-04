<template>
  <b-form v-if="form.label">
    <opensilex-InputForm
      :value.sync="form.uri"
      label="component.common.uri"
      type="text"
      rules="url"
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
        :value="OWL.OBJECT_PROEPRTY_URI"
      >{{$t("OntologyPropertyForm.objectProperty")}}</b-form-radio>
    </b-form-group>

    <opensilex-SelectForm
      v-if="form.type == OWL.DATATYPE_PROPERTY_URI"
      label="OntologyPropertyForm.value-type"
      required="true"
      :selected.sync="form.range"
      :options="datatypes"
    ></opensilex-SelectForm>

    <opensilex-SelectForm
      v-if="form.type == OWL.OBJECT_PROEPRTY_URI"
      label="OntologyPropertyForm.value-type"
      required="true"
      :selected.sync="form.range"
      :options="objectTypes"
    ></opensilex-SelectForm>

    <opensilex-InputForm
      :value.sync="form.label.en"
      label="OntologyPropertyForm.labelEN"
      type="text"
      :required="true"
    ></opensilex-InputForm>

    <opensilex-TextAreaForm
      :value.sync="form.comment.en"
      label="OntologyPropertyForm.commentEN"
      :required="true"
    ></opensilex-TextAreaForm>

    <opensilex-InputForm
      :value.sync="form.label.fr"
      label="OntologyPropertyForm.labelFR"
      type="text"
      :required="true"
    ></opensilex-InputForm>

    <opensilex-TextAreaForm
      :value.sync="form.comment.fr"
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

  @Prop({
    default: () => {
      return {
        uri: null,
        type: OWL.DATATYPE_PROPERTY_URI,
        parent: null,
        label: {},
        comment: {},
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
      label: {},
      comment: {},
      domain: null,
      range: null
    };
  }

  get datatypes() {
    let datatypeOptions = [];
    for (let i in this.$opensilex.datatypes) {
      let label = this.$t(this.$opensilex.datatypes[i].labelKey);
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
    return [];
  }

  create(form) {
    console.error(form);
    return this.$opensilex
      .getService("opensilex.OntologyService")
      .createProperty(form)
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
    // return this.$opensilex
    //   .getService("opensilex.OntologyService")
    //   .updateGroup(form)
    //   .then((http: HttpResponse<OpenSilexResponse<any>>) => {
    //     let uri = http.response.result;
    //     console.debug("Property updated", uri);
    //   })
    //   .catch(this.$opensilex.errorHandler);
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
        value-type: Type of value
        labelEN: English name
        labelFR: French name
        commentEN: English comment
        commentFR: French comment
        property-already-exists: Property with same URI already exists

fr:
    OntologyPropertyForm:
        propertyType: Type de propriété
        dataProperty: Propriété litérale
        objectProperty: Relation vers un objet
        value-type: Type de valeur
        labelEN: Nom anglais
        labelFR: Nom français
        commentEN: Commentaire anglais
        commentFR: Commentaire français   
        property-already-exists: La propriété existe déjà avec la même URI
</i18n>