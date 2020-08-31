<template>
  <b-form>
    <!-- Parent -->
    <opensilex-SelectForm
      :selected.sync="form.property"
      :options="propertiesOptions"
      :required="true"
      label="component.common.parent"
    ></opensilex-SelectForm>

    <!-- is abstract -->
    <opensilex-CheckboxForm :value.sync="form.required" title="OntologyClassDetail.required"></opensilex-CheckboxForm>

    <!-- is abstract -->
    <opensilex-CheckboxForm :value.sync="form.list" title="OntologyClassDetail.list"></opensilex-CheckboxForm>
  </b-form>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
import OWL from "../../ontologies/OWL";
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";

@Component
export default class OntologyClassPropertyForm extends Vue {
  $opensilex: any;
  OWL = OWL;

  @Prop()
  editMode;

  @Prop({
    default: () => {
      return {
        property: null,
        required: false,
        list: false
      };
    }
  })
  form;

  getEmptyForm() {
    return {
      property: null,
      required: false,
      list: false
    };
  }

  availableProperties = null;
  excludedProperties = [];
  setProperties(properties, excludedProperties) {
    this.availableProperties = properties;
    this.excludedProperties = excludedProperties;
  }

  get propertiesOptions() {
    return this.$opensilex.buildTreeListOptions(this.availableProperties);
  }

  create(form) {
    // return this.$opensilex
    //   .getService("opensilex.VueJsOntologyExtensionService")
    //   .createClass(form)
    //   .then((http: HttpResponse<OpenSilexResponse<any>>) => {
    //     let uri = http.response.result;
    //     console.debug("Object type created", uri);
    //   })
    //   .catch(error => {
    //     if (error.status == 409) {
    //       console.error("Object type already exists", error);
    //       this.$opensilex.errorHandler(
    //         error,
    //         this.$t("OntologyClassForm.object-type-already-exists")
    //       );
    //     } else {
    //       this.$opensilex.errorHandler(error);
    //     }
    //   });
  }

  update(form) {
    // return this.$opensilex
    //   .getService("opensilex.VueJsOntologyExtensionService")
    //   .updateClass(form)
    //   .then((http: HttpResponse<OpenSilexResponse<any>>) => {
    //     let uri = http.response.result;
    //     console.debug("Object type updated", uri);
    //   })
    //   .catch(this.$opensilex.errorHandler);
  }
}
</script>

<style scoped lang="scss">
</style>


<i18n>
en:
    OntologyClassForm:
        abstract-type: Abstract type
        labelEN: English name
        labelFR: French name
        commentEN: English description
        commentFR: French description
        object-type-already-exists: Object type with same URI already exists
        icon: Icon identifier

fr:
    OntologyClassForm:
        abstract-type: Type abstrait
        labelEN: Nom anglais
        labelFR: Nom français
        commentEN: Description anglaise
        commentFR: Description française
        object-type-already-exists: Un type d'objet existe déjà avec la même URI
        icon: Identifiant de l'icône
</i18n>