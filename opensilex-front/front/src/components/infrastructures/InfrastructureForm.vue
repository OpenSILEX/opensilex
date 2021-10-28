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
      :baseType="$opensilex.Oeso.ORGANIZATION_TYPE_URI"
      :required="true"
      placeholder="InfrastructureForm.form-type-placeholder"
    ></opensilex-TypeForm>

    <!-- Parents -->
    <opensilex-SelectForm
      :selected.sync="form.parents"
      :options="parentOptions"
      :multiple="true"
      label="component.common.parent"
      placeholder="InfrastructureForm.form-parent-placeholder"
    ></opensilex-SelectForm>

    <!-- Groupes -->
    <opensilex-GroupSelector
        label="InfrastructureForm.form-group-label"
        :groups.sync="form.groups"
        :multiple="true"
    ></opensilex-GroupSelector>

    <!-- Facilities -->
    <opensilex-InfrastructureFacilitySelector
        label="InfrastructureForm.form-facilities-label"
        :facilities.sync="form.facilities"
        :multiple="true"
    >
    </opensilex-InfrastructureFacilitySelector>
  </b-form>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
// @ts-ignore
import { ResourceTreeDTO } from "opensilex-core/index";
import {InfrastructureGetDTO} from "opensilex-core/model/infrastructureGetDTO";

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
        parents: [],
        groups: [],
        facilities: []
      };
    },
  })
  form;

  reset() {
    this.uriGenerated = true;
    if (this.parentInfrastructures == null) {
      this.$opensilex
        .getService("opensilex-core.OrganisationsService")
        .searchInfrastructures()
        .then(
          (http: HttpResponse<OpenSilexResponse<Array<InfrastructureGetDTO>>>) => {
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
      parents: [],
      groups: [],
      facilities: []
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
        .searchInfrastructures()
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
      return this.$opensilex.buildTreeFromDag(this.parentInfrastructures, {
        disableSubTree: this.form.uri
      });
    } else {
      return this.$opensilex.buildTreeFromDag(this.parentInfrastructures);
    }
  }

  cleanFormBeforeSend(form) {
    // I don't know why but sometimes this array contains null values so we filter them out
    form.parents = form.parents.filter(parent => parent);
  }

  create(form) {
    this.cleanFormBeforeSend(form);
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
    this.cleanFormBeforeSend(form);
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
    form-group-label: Groups
    form-facilities-label: Facilities
fr:
  InfrastructureForm:
    infrastructure-uri: URI de l'organisation
    form-name-placeholder: Saisir le nom de l'organisation
    form-type-placeholder: Sélectionner le type d'organisation
    form-parent-placeholder: Sélectionner l'organisation parente
    infrastructure-already-exists: Une organisation existe déjà avec cette URI
    form-group-label: Groupes
    form-facilities-labe: Installations techniques
</i18n>