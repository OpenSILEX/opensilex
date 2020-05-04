<template>
  <b-form>
    <!-- URI -->
    <opensilex-UriForm
      :uri.sync="form.uri"
      label="component.infrastructure.facility.facility-uri"
      :editMode="editMode"
      :generated.sync="uriGenerated"
    ></opensilex-UriForm>

    <!-- Name -->
    <opensilex-InputForm
      :value.sync="form.name"
      label="component.common.name"
      type="text"
      :required="true"
      placeholder="component.infrastructure.facility.form-name-placeholder"
    ></opensilex-InputForm>

    <!-- Type -->
    <opensilex-TypeForm
      :type.sync="form.type"
      :baseType="$opensilex.Oeso.INFRASTRUCTURE_FACILITY_TYPE_URI"
      :required="true"
      placeholder="component.infrastructure.facility.form-type-placeholder"
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
        type: null,
        name: "",
        infrastructure: null
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
      type: null,
      name: "",
      infrastructure: null
    };
  }

  create(form) {
    return this.$opensilex
      .getService("opensilex.InfrastructuresService")
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
            this.$i18n.t(
              "component.infrastructure.errors.infrastructure-facility-already-exists"
            )
          );
        } else {
          this.$opensilex.errorHandler(error);
        }
      });
  }

  update(form) {
    delete form.typeLabel;
    return this.$opensilex
      .getService("opensilex.InfrastructuresService")
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

