<template>
  <div class="container-fluid">
    <!-- <opensilex-PageHeader
      icon="ik#ik-globe"
      title="component.menu.infrastructures"
      description="InfrastructureView.description"
    ></opensilex-PageHeader> -->
    <opensilex-PageActions>
      <div>
        <b-tabs content-class="mt-3" :value=currentTabIndex @input="updateType">
          <b-tab :title="$t('InfrastructureView.organizations')"></b-tab>
          <b-tab :title="$t('InfrastructureView.facilities')"></b-tab>
        </b-tabs>
      </div>
    </opensilex-PageActions>
    <div class="row" v-if="organizationTab">
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
            @onUpdate="refresh"
        ></opensilex-InfrastructureDetail>
        <!-- Site detail -->
        <opensilex-SiteDetail
            :selected="selectedSite"
            :withActions="true"
            @onUpdate="refresh"
        ></opensilex-SiteDetail>
      </div>
    </div>
    <div class="row" v-if="facilityTab">
      <div class="col-md-6">
        <!-- Facilities -->
        <!-- <opensilex-InfrastructureFacilitiesView
            v-if="selectedFacilities"

            :withActions="facilitiesActions"
            @onUpdate="refresh"
            @onCreate="refresh"
            @onDelete="refresh"
            :facilities="selectedFacilities"
            :organization="selectedOrganization"
            :site="selectedSite"
            :isSelectable="false"
            ref="organizationFacilitiesView"
            @facilitySelected="updateSelectedFacility"
        ></opensilex-InfrastructureFacilitiesView> -->
        <opensilex-InfrastructureFacilitiesView
            :withActions="true"
            @onUpdate="refresh"
            @onCreate="refresh"
            @onDelete="refresh"
            :isSelectable="true"
            ref="facilitiesView"
            @facilitySelected="updateSelectedFacility"
        ></opensilex-InfrastructureFacilitiesView>
      </div>
      <div class="col-md-6">
        <!-- Facility detail -->
        <opensilex-OrganizationFacilityDetail
            :selected="selectedFacility"
        >
        </opensilex-OrganizationFacilityDetail>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import {Component, Ref} from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import { OrganizationsService, InfrastructureGetDTO } from "opensilex-core/index";
import {InfrastructureFacilityGetDTO} from "opensilex-core/model/infrastructureFacilityGetDTO";
import {SiteGetDTO} from "opensilex-core/model/siteGetDTO";
import Org from "../../ontologies/Org";
import {NamedResourceDTOInfrastructureFacilityModel} from "opensilex-core/model/namedResourceDTOInfrastructureFacilityModel";
import HttpResponse, {OpenSilexResponse} from "../../lib/HttpResponse";

@Component
export default class InfrastructureView extends Vue {
  $opensilex: any;
  $store: any;
  $route: any;
  service: OrganizationsService;

  // Gestion des tabs : inspiré de VariablesView.vue
  static ORGANIZATION_TAB = "Organization";
  static FACILITY_TAB = "Facility";
  static TABS = [
      InfrastructureView.ORGANIZATION_TAB,
      InfrastructureView.FACILITY_TAB
  ];

  currentTabIndex = 0;
  currentTabName = InfrastructureView.TABS[this.currentTabIndex];

  @Ref("infrastructureTree") readonly infrastructureTree!: any;
  @Ref("organizationFacilitiesView") readonly organizationFacilitiesView!: any;
  @Ref("facilitiesView") readonly facilitiesView!: any;

  selectedOrganization: InfrastructureGetDTO = null;
  selectedSite: SiteGetDTO = null;
  selectedFacility: InfrastructureFacilityGetDTO = null;

  created() {
    this.service = this.$opensilex.getService(
        "opensilex-core.OrganizationsService"
    );

    let query = this.$route.query;
    if (query && query.tab) {
      let requestedTab = decodeURIComponent(query.tab).toLowerCase();
      let index = InfrastructureView.TABS.findIndex(tab => tab.toLowerCase() === requestedTab);
      if (index >= 0) {
        this.updateType(index);
      } else {
        this.updateType(0);
      }
    } else {
      this.updateType(0);
    }
  }

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  get facilitiesActions(): boolean {
    return !!this.selectedOrganization;
  }

  get selectedFacilities(): Array<NamedResourceDTOInfrastructureFacilityModel> {
    if (this.selectedOrganization) {
      return this.selectedOrganization.facilities;
    }
    if (this.selectedSite) {
      return this.selectedSite.facilities;
    }
    return undefined;
  }

  get organizationTab() {
    return this.currentTabName == InfrastructureView.ORGANIZATION_TAB;
  }

  get facilityTab() {
    return this.currentTabName == InfrastructureView.FACILITY_TAB;
  }

  updateType(tabIndex) {
    if (tabIndex < 0 || tabIndex >= InfrastructureView.TABS.length) {
      return;
    }
    if (tabIndex !== this.currentTabIndex) {
      this.onTabChange(this.currentTabIndex, tabIndex);
      this.currentTabIndex = tabIndex;
      this.currentTabName = InfrastructureView.TABS[this.currentTabIndex];
    }
    this.$opensilex.updateURLParameter("tab",this.currentTabName);
  }

  onTabChange(oldIndex, newIndex) {
    if (InfrastructureView.TABS[oldIndex] === InfrastructureView.FACILITY_TAB) {
      this.selectedFacility = undefined;
    }
  }

  onSelectedOrganizationOrSite(selection: InfrastructureGetDTO | SiteGetDTO) {
    if (Org.checkURIS(Org.SITE_TYPE_URI, selection.rdf_type)) {
      this.updateSeletedSite(selection);
    } else { // Organization
      this.updateSelectedOrganization(selection);
    }
  }

  updateSelectedOrganization(newSelection) {
    this.selectedSite = undefined;
    this.selectedOrganization = newSelection;
  }

  updateSeletedSite(newSite) {
    this.selectedOrganization = undefined;
    this.selectedSite = newSite;
  }

  updateSelectedFacility(facility: InfrastructureFacilityGetDTO) {
    this.service
        .getInfrastructureFacility(facility.uri)
        .then((http: HttpResponse<OpenSilexResponse<InfrastructureFacilityGetDTO>>) => {
          let detailDTO: InfrastructureFacilityGetDTO = http.response.result;
          this.selectedFacility = detailDTO;
        });
  }

  refresh() {
    if (this.infrastructureTree) {
      this.infrastructureTree.refresh(this.selectedOrganization ? this.selectedOrganization.uri : undefined);
    }
    if (this.facilitiesView) {
      this.facilitiesView.refresh();
    }
  }
}
</script>

<style scoped lang="scss">
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