<template>
  <b-form>
    <opensilex-InputForm
      :value.sync="form.uri"
      label="component.common.uri"
      type="text"
      rules="url"
      :required="true"
    ></opensilex-InputForm>

    <b-form-group :label="$t('OntologyPropertyForm.propertyType')">
      <b-form-radio
        v-model="form.propertyType"
        name="propertyType"
        value="dataProperty"
      >{{$t("OntologyPropertyForm.dataProperty")}}</b-form-radio>
      <b-form-radio
        v-model="form.propertyType"
        name="propertyType"
        value="objectProperty"
      >{{$t("OntologyPropertyForm.objectProperty")}}</b-form-radio>
    </b-form-group>

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

@Component
export default class OntologyPropertyForm extends Vue {
  $opensilex: any;

  @Prop({
    default: () => {
      return {
        uri: null,
        propertyType: "dataProperty",
        parent: null,
        label: {},
        comment: {},
        domain: null,
        range: null
      };
    }
  })
  form;
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
        labelEN: English name
        labelFR: French name
        commentEN: English name
        commentFR: French comment

fr:
    OntologyPropertyForm:
        propertyType: Type de propriété
        dataProperty: Propriété litérale
        objectProperty: Relation vers un objet
        labelEN: English name
        labelFR: French name
        commentEN: English name
        commentFR: French comment        
</i18n>
