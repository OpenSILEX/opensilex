<template>
  <b-form>
    <!-- URI -->
    <opensilex-UriForm
        :uri.sync="form.uri"
        label="OntologyObjectForm.uri-label"
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
        placeholder="OntologyObjectForm.form-name-placeholder"
    ></opensilex-InputForm>

    <!-- Organizations -->
    <opensilex-OrganizationSelector
        ref="organizationSelector"
        label="SiteForm.organizations"
        :organizations.sync="form.organizations"
        :multiple="true"
        :required="true"
    ></opensilex-OrganizationSelector>

    <!-- Facilities -->
    <opensilex-FacilitySelector
        label="SiteForm.facilities"
        :facilities.sync="form.facilities"
        :multiple="true"
    ></opensilex-FacilitySelector>

    <!-- Groups -->
    <opensilex-GroupSelector
        label="SiteForm.groups"
        :groups.sync="form.groups"
        :multiple="true"
    ></opensilex-GroupSelector>

    <!-- Address toggle -->
    <b-form-checkbox
        v-model="hasAddress"
        :value="true"
        :unchecked-value="false"
        @change="onAddressToggled"
        switches
    >{{$t("FacilityForm.toggleAddress")}}</b-form-checkbox>

    <!-- Address -->
    <opensilex-AddressForm
        :address.sync="form.address"
    >
    </opensilex-AddressForm>
  </b-form>
</template>

<script lang="ts">
import {Component, Prop, Ref, Watch} from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, {OpenSilexResponse} from "../../../lib/HttpResponse";
import OrganizationSelector from "../OrganizationSelector.vue";
import {SiteCreationDTO} from 'opensilex-core/index';
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import {OrganizationsService} from "opensilex-core/api/organizations.service";
import {SiteUpdateDTO} from "opensilex-core/model/siteUpdateDTO";

@Component
export default class SiteForm extends Vue {
  @Ref("validatorRef") readonly validatorRef!: any;

  $opensilex: OpenSilexVuePlugin;
  uriGenerated = true;

  @Prop({default: false})
  editMode: boolean;

  @Prop({
    default: SiteForm.getEmptyForm()
  })
  form: SiteCreationDTO;
  hasAddress: boolean;

  @Ref("organizationSelector")
  organizationSelector: OrganizationSelector;

  getEmptyForm(): SiteCreationDTO {
    return SiteForm.getEmptyForm();
  }

  static getEmptyForm(): SiteCreationDTO {
    return {
      uri: undefined,
      rdf_type: undefined,
      name: undefined,
      address: undefined,
      organizations: [],
      groups: []
    };
  }

  reset() {
    this.organizationSelector.reset();
  }

  created() {
    this.hasAddress = !!this.form.address;
  }

  @Watch("form")
  onSiteChanged() {
    // Update hasAddress checkbox
    this.hasAddress = !!this.form.address;
  }

  onAddressToggled() {
    this.form.address = this.hasAddress
        ? {}
        : undefined;
  }

  create(form: SiteCreationDTO) {
    return this.$opensilex
        .getService<OrganizationsService>("opensilex.OrganizationsService")
        .createSite(form)
        .then((http: HttpResponse<OpenSilexResponse<string>>) => {
          let uri = http.response.result;
          console.debug("Site facility created", uri);
          form.uri = uri;
          return form;
        })
        .catch((error) => {
          if (error.status == 409) {
            console.error("Site already exists", error);
            this.$opensilex.errorHandler(
                error,
                this.$t("SiteForm.siteAlreadyExists")
            );
          } else {
            this.$opensilex.errorHandler(error);
          }
        });
  }

  update(form: SiteUpdateDTO) {
    delete form.rdf_type_name;
    console.log(form);
    return this.$opensilex
        .getService<OrganizationsService>("opensilex.OrganizationsService")
        .updateSite(form)
        .then((http: HttpResponse<OpenSilexResponse<string>>) => {
          let uri = http.response.result;
          console.debug("Site updated", uri);
        })
        .catch(this.$opensilex.errorHandler);
  }
}
</script>

<style scoped>

</style>

<i18n>
en:
  SiteForm:
    organizations: Organizations
    facilities: Facilities
    groups: Groups
    toggleAddress: "Address"
    siteAlreadyExists: Site already exists
fr:
  SiteForm:
    organizations: Organisations
    facilities: Installations environnementales
    groups: Groupes
    toggleAddress: "Adresse"
    siteAlreadyExists: Ce site existe déjà
</i18n>