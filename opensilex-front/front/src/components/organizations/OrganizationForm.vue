<template>
  <b-form>
    <!-- URI -->
    <opensilex-UriForm
      :uri.sync="form.uri"
      label="OrganizationForm.organization-uri"
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
      placeholder="OrganizationForm.form-name-placeholder"
    ></opensilex-InputForm>

    <!-- Type -->
    <opensilex-TypeForm
      :type.sync="form.rdf_type"
      :baseType="$opensilex.Foaf.ORGANIZATION_TYPE_URI"
      :ignoreRoot="false"
      :required="true"
      placeholder="OrganizationForm.form-type-placeholder"
    ></opensilex-TypeForm>

    <!-- Parents -->
    <opensilex-SelectForm
      :selected.sync="form.parents"
      :options="parentOptions"
      :multiple="true"
      label="component.common.parent"
      placeholder="OrganizationForm.form-parent-placeholder"
    ></opensilex-SelectForm>

    <!-- Groupes -->
    <opensilex-GroupSelector
        label="OrganizationForm.form-group-label"
        :groups.sync="form.groups"
        :multiple="true"
    ></opensilex-GroupSelector>

    <!-- Facilities -->
    <opensilex-FacilitySelector
        label="OrganizationForm.form-facilities-label"
        :facilities.sync="form.facilities"
        :multiple="true"
    >
    </opensilex-FacilitySelector>
  </b-form>
</template>

<script lang="ts">
import {Component, Prop} from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, {OpenSilexResponse} from "../../lib/HttpResponse";
import {ResourceDagDTO} from 'opensilex-core/index';
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {OrganizationsService} from "opensilex-core/api/organizations.service";
import {OrganizationCreationDTO} from "opensilex-core/model/organizationCreationDTO";
import {OrganizationUpdateDTO} from "opensilex-core/model/organizationUpdateDTO";

@Component
export default class OrganizationForm extends Vue {
  $opensilex: OpenSilexVuePlugin;

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
    if (this.parentOrganizations == null) {
      this.$opensilex
        .getService<OrganizationsService>("opensilex-core.OrganizationsService")
        .searchOrganizations()
        .then(
          (http: HttpResponse<OpenSilexResponse<Array<ResourceDagDTO>>>) => {
            this.setParentOrganizations(http.response.result);
          }
        )
        .catch(this.$opensilex.errorHandler);
    }
  }

  getEmptyForm(): OrganizationCreationDTO {
    return {
      uri: null,
      rdf_type: null,
      name: "",
      parents: [],
      groups: [],
      facilities: []
    };
  }

  parentOrganizations = null;
  setParentOrganizations(organizations) {
    this.parentOrganizations = organizations;
  }

  init() {
    if (this.parentOrganizations == null) {
      this.$opensilex
        .getService<OrganizationsService>("opensilex-core.OrganizationsService")
        .searchOrganizations()
        .then(
          (http: HttpResponse<OpenSilexResponse<Array<ResourceDagDTO>>>) => {
            this.setParentOrganizations(http.response.result);
          }
        )
        .catch(this.$opensilex.errorHandler);
    }
  }

  get parentOptions() {
    if (this.editMode) {
      return this.$opensilex.buildTreeFromDag(this.parentOrganizations, {
        disableSubTree: this.form.uri
      });
    } else {
      return this.$opensilex.buildTreeFromDag(this.parentOrganizations);
    }
  }

  cleanFormBeforeSend(form) {
    // I don't know why but sometimes this array contains null values so we filter them out
    form.parents = form.parents.filter(parent => parent);
  }

  create(form: OrganizationCreationDTO) {
    this.cleanFormBeforeSend(form);
    return this.$opensilex
      .getService<OrganizationsService>("opensilex.OrganizationsService")
      .createOrganization(form)
      .then((http: HttpResponse<OpenSilexResponse<string>>) => {
        let uri = http.response.result;
        console.debug("Organization facility created", uri);
        form.uri = uri;
        return form;
      })
      .catch((error) => {
        if (error.status == 409) {
          console.error("Organization already exists", error);
          this.$opensilex.errorHandler(
            error,
            this.$t("OrganizationForm.organization-already-exists")
          );
        } else {
          this.$opensilex.errorHandler(error);
        }
      });
  }

  update(form: OrganizationUpdateDTO) {
    this.cleanFormBeforeSend(form);
    delete form.rdf_type_name;
    return this.$opensilex
      .getService<OrganizationsService>("opensilex.OrganizationsService")
      .updateOrganization(form)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        console.debug("Organization updated", uri);
        let message = this.$i18n.t("OrganizationForm.name") + " " + form.name + " " + this.$i18n.t("component.common.success.update-success-message");
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
  OrganizationForm:
    name: The organization
    organization-uri: Organization URI
    form-name-placeholder: Enter organization name
    form-type-placeholder: Select organization type
    form-parent-placeholder: Select parent organization
    organization-already-exists: Organization already exists with this URI
    form-group-label: Groups
    form-facilities-label: Facilities
fr:
  OrganizationForm:
    name: L'organisation
    organization-uri: URI de l'organisation
    form-name-placeholder: Saisir le nom de l'organisation
    form-type-placeholder: Sélectionner le type d'organisation
    form-parent-placeholder: Sélectionner l'organisation parente
    organization-already-exists: Une organisation existe déjà avec cette URI
    form-group-label: Groupes
    form-facilities-label: Installations environnementales
</i18n>