<template>
  <b-form>
    <!-- URI -->
    <opensilex-UriForm
      :uri.sync="form.uri"
      label="component.infrastructure.infrastructure-uri"
      :editMode="editMode"
      :generated.sync="uriGenerated"
    ></opensilex-UriForm>

    <!-- Name -->
    <opensilex-InputForm
      :value.sync="form.name"
      label="component.common.name"
      type="text"
      :required="true"
      placeholder="component.infrastructure.form-name-placeholder"
    ></opensilex-InputForm>

    <!-- Type -->
    <opensilex-TypeForm
      :type.sync="form.type"
      :baseType="$opensilex.Oeso.INFRASTRUCTURE_TYPE_URI"
      :required="true"
      placeholder="component.infrastructure.form-type-placeholder"
    ></opensilex-TypeForm>

    <!-- Parent -->
    <opensilex-SelectForm
      :selected.sync="form.parent"
      :options="parentOptions"
      label="component.common.parent"
      placeholder="component.infrastructure.form-parent-placeholder"
    ></opensilex-SelectForm>
  </b-form>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import {
  OntologyService,
  InfrastructureGetDTO,
  InfrastructureCreationDTO,
  ResourceTreeDTO
} from "opensilex-core/index";

@Component
export default class InfrastructureForm extends Vue {
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
        parent: null
      };
    }
  })
  form;

  reset() {
    this.uriGenerated = true;
    if (this.parentInfrastructures == null) {
      this.$opensilex
        .getService("opensilex-core.InfrastructuresService")
        .searchInfrastructuresTree()
        .then(
          (http: HttpResponse<OpenSilexResponse<Array<ResourceTreeDTO>>>) => {
            this.setParentInfrastructures(http.response.result);
          }
        )
        .catch(this.$opensilex.errorHandler);
    }
  }

  getEmptyForm() {
    return {
      uri: null,
      type: null,
      name: "",
      parent: null
    };
  }

  parentInfrastructures = null;
  setParentInfrastructures(infrastructures) {
    this.parentInfrastructures = infrastructures;
  }

  get parentOptions() {
    if (this.editMode) {
      return this.$opensilex.buildTreeListOptions(this.parentInfrastructures, {
        disableSubTree: this.form.uri
      });
    } else {
      return this.$opensilex.buildTreeListOptions(this.parentInfrastructures);
    }
  }

  create(form) {
    return this.$opensilex
      .getService("opensilex.InfrastructuresService")
      .createInfrastructure(form)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        console.debug("Infrastructure facility created", uri);
      })
      .catch(error => {
        if (error.status == 409) {
          console.error("Infrastructure already exists", error);
          this.$opensilex.errorHandler(
            error,
            this.$t(
              "component.infrastructure.errors.infrastructure-already-exists"
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
      .updateInfrastructure(form)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        console.debug("Infrastructure updated", uri);
      })
      .catch(this.$opensilex.errorHandler);
  }
}
</script>

<style scoped lang="scss">
</style>

