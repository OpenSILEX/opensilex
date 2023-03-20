<template>
  <div class="container-fluid">
    <div class="row">
      <div class="col-md-6">
        <!-- Infrastructure tree -->
        <opensilex-InfrastructureTree
          ref="infrastructureTree"
          @onSelect="onSelectedOrganizationOrSite"
        ></opensilex-InfrastructureTree>
      </div>
      <div class="col-md-6">
        <!-- Infrastructure detail -->
        <opensilex-InfrastructureDetail
          :selected="selectedOrganization"
          @onUpdate="refreshTree"
          class="infrastructureDetailComponent"
        ></opensilex-InfrastructureDetail>
        <!-- Site detail -->
        <opensilex-SiteDetail
          :selected="selectedSite"
          :withActions="true"
          @onUpdate="refreshTree"
        ></opensilex-SiteDetail>
        <!-- Facilities -->
        <opensilex-FacilitiesView
          v-if="selectedFacilities"
          :withActions="true"
          @onUpdate="refreshTree"
          @onCreate="refreshTree"
          @onDelete="refreshTree"
          :facilities="selectedFacilities"
          :organization="selectedOrganization"
          :site="selectedSite"
          :isSelectable="false"
          :debounce="300"
          :lazy="false"
        ></opensilex-FacilitiesView>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import {Component, Ref} from "vue-property-decorator";
import Vue from "vue";
import {OrganizationGetDTO, NamedResourceDTOFacilityModel, OrganizationsService, SiteGetDTO} from "opensilex-core/index";
import Org from "../../ontologies/Org";
import InfrastructureTree from "./InfrastructureTree.vue";

@Component
export default class InfrastructureView extends Vue {
  $opensilex: any;
  $store: any;
  $route: any;
  service: OrganizationsService;

  @Ref("infrastructureTree") readonly infrastructureTree!: InfrastructureTree;
  @Ref("organizationFacilitiesView") readonly organizationFacilitiesView!: any;
  @Ref("facilitiesView") readonly facilitiesView!: any;

  selectedOrganization: OrganizationGetDTO = null;
  selectedSite: SiteGetDTO = null;

  created() {
    this.service = this.$opensilex.getService(
        "opensilex-core.OrganizationsService"
    );
  }

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  get selectedFacilities(): Array<NamedResourceDTOFacilityModel> {
    if (this.selectedOrganization) {
      return this.selectedOrganization.facilities;
    }
    if (this.selectedSite) {
      return this.selectedSite.facilities;
    }
    return undefined;
  }

  onSelectedOrganizationOrSite(selection: OrganizationGetDTO | SiteGetDTO) {
    if (!selection) {
      this.clearSelection();
      return;
    }

    if (Org.checkURIS(Org.SITE_TYPE_URI, selection.rdf_type)) {
      this.updateSelectedSite(selection);
    } else { // Organization
      this.updateSelectedOrganization(selection);
    }
  }

  clearSelection() {
    this.selectedSite = undefined;
    this.selectedOrganization = undefined;
  }

  updateSelectedOrganization(newSelection) {
    this.selectedSite = undefined;
    this.selectedOrganization = newSelection;
  }

  updateSelectedSite(newSite) {
    this.selectedOrganization = undefined;
    this.selectedSite = newSite;
  }

  refreshTree() {
    let uri = undefined;
    if (this.selectedOrganization) {
      uri = this.selectedOrganization.uri;
    } else if (this.selectedSite) {
      uri = this.selectedSite.uri;
    }

    this.infrastructureTree.refresh(uri);
  }
}
</script>


<style scoped lang="scss">
@media only screen and (min-width: 768px) {
  .infrastructureDetailComponent {
    margin-top: 30px;
  }
}
</style>

<i18n>
en:
  InfrastructureView:
    description: Manage and configure organizations
    organizations: Organizations and sites
    facilities: Facilities
fr:
  InfrastructureView:
    description: Gérer et configurer les organisations
    organizations: Organisations et sites
    facilities: Installations techniques
</i18n>
