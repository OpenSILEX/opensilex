<template>
  <b-form>
    <!-- URI -->
    <opensilex-UriForm
      :uri.sync="form.uri"
      label="ScientificObjectForm.uri"
      helpMessage="ScientificObjectForm.uri-help"
      :editMode="editMode"
      :generated.sync="uriGenerated"
    ></opensilex-UriForm>

    <!-- Label -->
    <opensilex-InputForm
      :value.sync="form.name"
      label="ScientificObjectForm.label"
      type="text"
      :required="true"
      placeholder="scientificObject.label-placeholder"
    ></opensilex-InputForm>

    <!-- Type -->
    <opensilex-TypeForm
      :type.sync="form.type"
      :baseType="$opensilex.Oeso.SCIENTIFIC_OBJECT_TYPE_URI"
      :required="true"
      @input="getAdditionalFields"
      placeholder="scientificObject.form-type-placeholder"
    ></opensilex-TypeForm>

    <!-- <div v-for="(value, index) in propertyComponents" :key="index">{{value}}</div> -->

    <component
      v-for="(value, index) in propertyComponents"
      :key="index"
      v-bind:is="value.component"
      :property="value.property"
      :value.sync="v"
    ></component>
  </b-form>
</template>

<script lang="ts">
import { Component, Prop, PropSync, Ref } from "vue-property-decorator";
import Vue from "vue";
import VueRouter from "vue-router";

import {
  ExperimentCreationDTO,
  SpeciesService,
  SpeciesDTO
} from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";

@Component
export default class ScientificObjectForm extends Vue {
  $opensilex: any;

  v = "";
  @Prop()
  editMode;

  @Prop({ default: true })
  uriGenerated;

  @Ref("validatorRef") readonly validatorRef!: any;

  get user() {
    return this.$store.state.user;
  }

  @Prop({
    default: () => {
      return {
        uri: undefined,
        type: undefined,
        name: "",
        factors: [],
        germplasms: [],
        relations: []
      };
    }
  })
  form;

  propertyComponents = [];

  getAdditionalFields() {
    let ontologyService = this.$opensilex.getService(
      "opensilex.OntologyService"
    );

    return ontologyService
      .getClasses([
        this.form.type,
        this.$opensilex.Oeso.SCIENTIFIC_OBJECT_TYPE_URI
      ])
      .then(http => {
        let classModel = http.response.result[0];
        let baseClassModel = http.response.result[1];

        let managedURIs = [];
        for (let i in baseClassModel.properties) {
          let managedURI = baseClassModel.properties[i].uri;
          managedURIs.push(managedURI);
        }

        this.propertyComponents = [];
        for (let i in classModel.properties) {
          let property = classModel.properties[i];
          let typeComponent = this.$opensilex.getTypeComponent(
            property.typeRestriction
          );
          if (managedURIs.indexOf(property.uri) < 0 && typeComponent) {
            property.required = true;
            this.propertyComponents.push({
              property: property,
              component: typeComponent
            });
          }
        }

        // TODO display components
      });
  }

  reset() {
    // TODO
  }

  create(form) {
    // TODO
  }

  update(form) {
    // TODO
  }
}
</script>
<style scoped lang="scss">
</style>
