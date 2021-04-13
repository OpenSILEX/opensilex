<template>
  <div class="container-fluid">
    <opensilex-PageHeader
      icon="ik#ik-globe"
      title="component.menu.infrastructures"
      description="InfrastructureView.description"
      :isExperimentalFeature="true"
    ></opensilex-PageHeader>
    <div class="row">
      <div class="col-md-6">
        <!-- Infrastructure tree -->
        <opensilex-InfrastructureTree
          ref="infrastructureTree"
          @onSelect="updateSelected"
        ></opensilex-InfrastructureTree>
        <!-- Infrastructure facilities -->
        <opensilex-InfrastructureFacilitiesView
          :selected="selected"
          @onUpdate="refresh"
          @onCreate="refresh"
          @onDelete="refresh"
        ></opensilex-InfrastructureFacilitiesView>
      </div>
      <div class="col-md-6">
        <!-- Infrastructure detail -->
        <opensilex-InfrastructureDetail :selected="selected"></opensilex-InfrastructureDetail>
        <!-- Infrastructure groups -->
        <opensilex-InfrastructureGroupsView
          :selected="selected"
          @onUpdate="refresh"
          @onCreate="refresh"
          @onDelete="refresh"
        ></opensilex-InfrastructureGroupsView>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import {
  OrganisationsService,
  ResourceTreeDTO,
  InfrastructureUpdateDTO,
  InfrastructureGetDTO
} from "opensilex-core/index";

@Component
export default class InfrastructureView extends Vue {
  $opensilex: any;
  $store: any;
  service: OrganisationsService;

  @Ref("infrastructureTree") readonly infrastructureTree!: any;

  selected: InfrastructureGetDTO = null;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  updateSelected(newSelection) {
    this.selected = newSelection;
  }

  refresh() {
    this.infrastructureTree.refresh(this.selected.uri);
  }
}
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  InfrastructureView:
    description: Manage and configure organizations
fr:
  InfrastructureView:
    description: Gérer et configurer les organisations
</i18n>