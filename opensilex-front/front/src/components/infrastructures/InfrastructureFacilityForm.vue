<template>
  <b-form>
    <!-- URI -->
    <opensilex-UriForm
      :uri.sync="form.uri"
      label="InfrastructureFacilityForm.facility-uri"
      :editMode="editMode"
      :generated.sync="uriGenerated"
    ></opensilex-UriForm>

    <!-- Name -->
    <opensilex-InputForm
      :value.sync="form.name"
      label="component.common.name"
      type="text"
      :required="true"
      placeholder="InfrastructureFacilityForm.form-name-placeholder"
    ></opensilex-InputForm>

    <!-- Type -->
    <opensilex-TypeForm
      :type.sync="form.rdf_type"
      :baseType="$opensilex.Oeso.INFRASTRUCTURE_FACILITY_TYPE_URI"
      :required="true"
      placeholder="InfrastructureFacilityForm.form-type-placeholder"
    ></opensilex-TypeForm>
  </b-form>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import Oeso from "../../ontologies/Oeso";
import {
  InfrastructureGetDTO,
  InfrastructureCreationDTO,
  ResourceTreeDTO,
  InfrastructureFacilityCreationDTO
} from "opensilex-core/index";

@Component
export default class InfrastructureFacilityForm extends Vue {
  $opensilex: any;

  uriGenerated = true;

  @Prop()
  editMode;

  @Prop({
    default: () => {
      return {
        uri: null,
        rdf_type: null,
        name: "",
        organisation: null
      };
    }
  })
  form;

  reset() {
    this.uriGenerated = true;
  }

  getEmptyForm() {
    return {
      uri: null,
      rdf_type: null,
      name: "",
      organisation: null
    };
  }

  create(form) {
    return this.$opensilex
      .getService("opensilex.OrganisationsService")
      .createInfrastructureFacility(form)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        console.debug("Infrastructure facility created", uri);
      })
      .catch(error => {
        if (error.status == 409) {
          console.error("Infrastructure facility already exists", error);
          this.$opensilex.errorHandler(
            error,
            this.$t(
              "InfrastructureFacilityForm.infrastructure-facility-already-exists"
            )
          );
        } else {
          this.$opensilex.errorHandler(error);
        }
      });
  }

  update(form) {
    delete form.rdf_type_name;
    return this.$opensilex
      .getService("opensilex.OrganisationsService")
      .updateInfrastructureFacility(form)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        console.debug("Infrastructure facility updated", uri);
      })
      .catch(this.$opensilex.errorHandler);
  }
}
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  InfrastructureFacilityForm:
    facility-uri: Infrastructure facility URI
    form-name-placeholder: Enter infrastructure facility name
    form-type-placeholder: Select infrastructure facility type
    infrastructure-facility-already-exists: Infrastructure facility already exists with this URI
fr:
  InfrastructureFacilityForm:
    facility-uri: URI de l'installation technique
    form-name-placeholder: Saisir le nom de l'installation technique
    form-type-placeholder: Sélectionner le type de l'installation technique
    infrastructure-facility-already-exists: Une installation technique existe déjà avec cette URI
</i18n>