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
        <b-tabs content-class="mt-3" :value=elementIndex @input="updateType">
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
      </div>
    </div>
    <div class="row" v-if="facilityTab">
      <div class="col-md-6">
        <!-- Facilities -->
        <opensilex-InfrastructureFacilitiesView
            :selected="selectedFacility"
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
  service: OrganisationsService;

  // Gestion des tabs : inspiré de VariablesView.vue
  static ORGANIZATION_TYPE = "Organization";
  static FACILITY_TYPE = "Facility";
  static ELEMENT_TYPES = [
      InfrastructureView.ORGANIZATION_TYPE,
      InfrastructureView.FACILITY_TYPE
  ];

  elementIndex = 0;
  elementType = InfrastructureView.ELEMENT_TYPES[this.elementIndex];

  @Ref("infrastructureTree") readonly infrastructureTree!: any;
  @Ref("facilitiesView") readonly facilitiesView!: any;

  selectedOrganization: InfrastructureGetDTO = null;
  selectedFacility: InfrastructureFacilityGetDTO = null;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  get organizationTab() {
    return this.elementType == InfrastructureView.ORGANIZATION_TYPE;
  }

  get facilityTab() {
    return this.elementType == InfrastructureView.FACILITY_TYPE;
  }

  updateType(tabIndex) {
    if (tabIndex < 0 || tabIndex >= InfrastructureView.ELEMENT_TYPES.length) {
      return;
    }
    if (tabIndex !== this.elementIndex) {
      this.onTabChange(this.elementIndex, tabIndex);
      this.elementIndex = tabIndex;
      this.elementType = InfrastructureView.ELEMENT_TYPES[this.elementIndex];
    }
  }

  onTabChange(oldIndex, newIndex) {
    if (InfrastructureView.ELEMENT_TYPES[oldIndex] === InfrastructureView.FACILITY_TYPE) {
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