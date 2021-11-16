<template>
  <div class="container-fluid">
    <opensilex-PageHeader
      icon="ik#ik-globe"
      title="component.menu.infrastructures"
      description="InfrastructureView.description"
      :isExperimentalFeature="true"
    ></opensilex-PageHeader>
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
          @onSelect="updateSelectedOrganization"
        ></opensilex-InfrastructureTree>
      </div>
      <div class="col-md-6">
        <!-- Infrastructure detail -->
        <opensilex-InfrastructureDetail
            :selected="selectedOrganization"
            @onUpdate="refresh"
        ></opensilex-InfrastructureDetail>
        <!-- Facilities -->
        <opensilex-InfrastructureFacilitiesView
            v-if="selectedOrganization"

            :withActions="true"
            @onUpdate="refresh"
            @onCreate="refresh"
            @onDelete="refresh"
            :selected="selectedOrganization"
            :isSelectable="false"
            ref="organizationFacilitiesView"
            @facilitySelected="updateSelectedFacility"
        ></opensilex-InfrastructureFacilitiesView>
      </div>
    </div>
    <div class="row" v-if="facilityTab">
      <div class="col-md-6">
        <!-- Facilities -->
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
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import { OrganisationsService, InfrastructureGetDTO } from "opensilex-core/index";
import {InfrastructureFacilityGetDTO} from "opensilex-core/model/infrastructureFacilityGetDTO";

@Component
export default class InfrastructureView extends Vue {
  $opensilex: any;
  $store: any;
  $route: any;
  service: OrganisationsService;

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
  selectedFacility: InfrastructureFacilityGetDTO = null;

  created() {
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

  updateSelectedOrganization(newSelection) {
    this.selectedOrganization = newSelection;
  }

  updateSelectedFacility(facility: InfrastructureFacilityGetDTO) {
    this.selectedFacility = facility;
  }

  refresh() {
    if (this.infrastructureTree) {
      this.infrastructureTree.refresh(this.selectedOrganization.uri);
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
    organizations: Organizations
    facilities: Facilities
fr:
  InfrastructureView:
    description: Gérer et configurer les organisations
    organizations: Organisations
    facilities: Installations techniques
</i18n>