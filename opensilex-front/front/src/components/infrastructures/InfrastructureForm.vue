<template>
  <b-form>
    <!-- URI -->
    <opensilex-UriForm
      :uri.sync="form.uri"
      label="InfrastructureForm.infrastructure-uri"
      helpMessage="component.common.uri-help-message"
      :editMode="editMode"
      :generated.sync="uriGenerated"
    ></opensilex-UriForm>

    <!-- Name -->
    <opensilex-InputForm
      :value.sync="form.name"
      label="component.common.name"
      type="text"
      :required="true"
      placeholder="InfrastructureForm.form-name-placeholder"
    ></opensilex-InputForm>

    <!-- Type -->
    <opensilex-TypeForm
      :type.sync="form.rdf_type"
      :baseType="$opensilex.Oeso.INFRASTRUCTURE_TYPE_URI"
      :required="true"
      placeholder="InfrastructureForm.form-type-placeholder"
    ></opensilex-TypeForm>

    <!-- Parent -->
    <opensilex-SelectForm
      :selected.sync="form.parent"
      :options="parentOptions"
      label="component.common.parent"
      placeholder="InfrastructureForm.form-parent-placeholder"
    ></opensilex-SelectForm>
  </b-form>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
// @ts-ignore
import { ResourceTreeDTO } from "opensilex-core/index";

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
        rdf_type: null,
        name: "",
        parent: null,
      };
    },
  })
  form;

  reset() {
    this.uriGenerated = true;
    if (this.parentInfrastructures == null) {
      this.$opensilex
        .getService("opensilex-core.OrganisationsService")
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
      rdf_type: null,
      name: "",
      parent: null,
    };
  }

  parentInfrastructures = null;
  setParentInfrastructures(infrastructures) {
    this.parentInfrastructures = infrastructures;
  }

  init() {
    if (this.parentInfrastructures == null) {
      this.$opensilex
        .getService("opensilex-core.OrganisationsService")
        .searchInfrastructuresTree()
        .then(
          (http: HttpResponse<OpenSilexResponse<Array<ResourceTreeDTO>>>) => {
            this.setParentInfrastructures(http.response.result);
          }
        )
        .catch(this.$opensilex.errorHandler);
    }
  }

  get parentOptions() {
    if (this.editMode) {
      return this.$opensilex.buildTreeListOptions(this.parentInfrastructures, {
        disableSubTree: this.form.uri,
      });
    } else {
      return this.$opensilex.buildTreeListOptions(this.parentInfrastructures);
    }
  }

  create(form) {
    return this.$opensilex
      .getService("opensilex.OrganisationsService")
      .createInfrastructure(form)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        console.debug("Infrastructure facility created", uri);
        form.uri = uri;
        return form;
      })
      .catch((error) => {
        if (error.status == 409) {
          console.error("Infrastructure already exists", error);
          this.$opensilex.errorHandler(
            error,
            this.$t("InfrastructureForm.infrastructure-already-exists")
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

<i18n>
en:
  InfrastructureForm:
    infrastructure-uri: Organization URI
    form-name-placeholder: Enter organization name
    form-type-placeholder: Select organization type
    form-parent-placeholder: Select parent organization
    infrastructure-already-exists: Organization already exists with this URI
fr:
  InfrastructureForm:
    infrastructure-uri: URI de l'organisation
    form-name-placeholder: Saisir le nom de l'organisation
    form-type-placeholder: Sélectionner le type d'organisation
    form-parent-placeholder: Sélectionner l'organisation parente
    infrastructure-already-exists: Une organisation existe déjà avec cette URI
</i18n>