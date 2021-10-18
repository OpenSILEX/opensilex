<template>
  <b-form v-if="form.name_translations">
    <opensilex-InputForm
      :value.sync="form.uri"
      label="component.common.uri"
      type="text"
      rules="url"
      :disabled="editMode"
      :required="true"
    ></opensilex-InputForm>

    <!-- Parent -->
    <opensilex-SelectForm
      :selected.sync="form.parent"
      :options="parentOptions"
      :required="true"
      label="component.common.parent"
    ></opensilex-SelectForm>

    <opensilex-InputForm
      :value.sync="form.name_translations.en"
      label="OntologyClassForm.labelEN"
      type="text"
      :required="true"
    ></opensilex-InputForm>

    <opensilex-TextAreaForm
      :value.sync="form.comment_translations.en"
      label="OntologyClassForm.commentEN"
      :required="true"
    ></opensilex-TextAreaForm>

    <opensilex-InputForm
      :value.sync="form.name_translations.fr"
      label="OntologyClassForm.labelFR"
      type="text"
      :required="true"
    ></opensilex-InputForm>

    <opensilex-TextAreaForm
      :value.sync="form.comment_translations.fr"
      label="OntologyClassForm.commentFR"
      :required="true"
    ></opensilex-TextAreaForm>

    <!-- is abstract -->
    <!-- <opensilex-CheckboxForm
      :value.sync="form.is_abstract"
      title="OntologyClassForm.abstract-type"
    ></opensilex-CheckboxForm> -->
    
    <opensilex-IconForm :value.sync="form.icon" label="OntologyClassForm.icon"></opensilex-IconForm>
  </b-form>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
import OWL from "../../ontologies/OWL";

import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";

@Component
export default class OntologyClassForm extends Vue {
  $opensilex: any;
  OWL = OWL;

  @Prop()
  editMode;

  @Prop({
    default: () => {
      return {
        uri: null,
        parent: null,
        name: null,
        name_translations: {},
        comment: null,
        comment_translations: {},
        icon: null,
        is_abstract: false
      };
    }
  })
  form;

  getEmptyForm() {
    return {
      uri: null,
      parent: null,
      name: null,
      name_translations: {},
      comment: null,
      comment_translations: {},
      icon: null,
      is_abstract: false
    };
  }

  availableParents = null;
  setParentTypes(parents) {
    this.availableParents = parents;
  }

  get parentOptions() {
    if (this.editMode) {
      return this.$opensilex.buildTreeListOptions(this.availableParents, {
        disableSubTree: this.form.uri
      });
    } else {
      return this.$opensilex.buildTreeListOptions(this.availableParents);
    }
  }

  create(form) {
    return this.$opensilex
      .getService("opensilex.VueJsOntologyExtensionService")
      .createRDFType(form)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        let message = this.$i18n.t("OntologyClassView.the-type") + " " +uri + this.$i18n.t("component.common.success.creation-success-message");
        this.$opensilex.showSuccessToast(message);
      })
      .catch(error => {
        if (error.status == 409) {
          console.error("Object type already exists", error);
          this.$opensilex.errorHandler(
            error,
            this.$t("OntologyClassForm.object-type-already-exists")
          );
        } else {
          this.$opensilex.errorHandler(error);
        }
      });
  }

  update(form) {
    return this.$opensilex
      .getService("opensilex.VueJsOntologyExtensionService")
      .updateRDFType(form)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        let message = this.$i18n.t("OntologyClassView.the-type") + " " +uri + this.$i18n.t("component.common.success.update-success-message");
        this.$opensilex.showSuccessToast(message);
      })
      .catch(this.$opensilex.errorHandler);
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
        icon: Icon

fr:
    OntologyClassForm:
        abstract-type: Type abstrait
        labelEN: Nom anglais
        labelFR: Nom français
        commentEN: Description anglaise
        commentFR: Description française
        object-type-already-exists: Un type d'objet existe déjà avec la même URI
        icon: Icône
</i18n>