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
    <opensilex-InfrastructureSelector
        label="SiteForm.organizations"
        :infrastructures.sync="form.organizations"
        :multiple="true"
    ></opensilex-InfrastructureSelector>

    <!-- Facilities -->
    <opensilex-InfrastructureFacilitySelector
        label="SiteForm.facilites"
        :facilities.sync="form.facilities"
        :multiple="true"
    ></opensilex-InfrastructureFacilitySelector>

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
    >{{$t("OrganizationFacilityForm.toggleAddress")}}</b-form-checkbox>

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
import {SiteCreationDTO} from "opensilex-core/model/siteCreationDTO";
import HttpResponse, {OpenSilexResponse} from "../../../lib/HttpResponse";

@Component
export default class SiteForm extends Vue {
  @Ref("validatorRef") readonly validatorRef!: any;

  $opensilex: any;
  uriGenerated = true;

  @Prop({default: false})
  editMode: boolean;

  @Prop({
    default: SiteForm.getEmptyForm()
  })
  form: SiteCreationDTO;
  hasAddress: boolean;

  getEmptyForm() {
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

  create(form) {
    return this.$opensilex
        .getService("opensilex.OrganisationsService")
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

  update(form) {
    delete form.rdf_type_name;
    console.log(form);
    return this.$opensilex
        .getService("opensilex.OrganisationsService")
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
    facilities: Installations techniques
    groups: Groupes
    toggleAddress: "Adresse"
    siteAlreadyExists: Ce site existe déjà
</i18n>