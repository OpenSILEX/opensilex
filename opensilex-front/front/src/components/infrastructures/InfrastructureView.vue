<template>
  <div class="container-fluid">
    <opensilex-PageHeader
      icon="ik-globe"
      title="component.menu.infrastructures"
      description="component.infrastructure.description"
    ></opensilex-PageHeader>
    <div class="table-responsive">
      <div class="row">
        <div class="col-md-6">
          <opensilex-InfrastructureTree
            ref="infrastructureTree"
            v-if="user.hasCredential(credentials.CREDENTIAL_INFRASTRUCTURE_READ_ID)"
            @onSelect="updateSelected"
          ></opensilex-InfrastructureTree>
          <opensilex-InfrastructureDevicesView
            :selected="selected"
            @onUpdate="refresh"
            @onCreate="refresh"
            @onDelete="refresh"
          ></opensilex-InfrastructureDevicesView>
        </div>
        <div class="col-md-6">
          <opensilex-InfrastructureDetail :selected="selected"></opensilex-InfrastructureDetail>
          <opensilex-InfrastructureGroupsView
            :selected="selected"
            @onUpdate="refresh"
            @onCreate="refresh"
            @onDelete="refresh"
          ></opensilex-InfrastructureGroupsView>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import {
  InfrastructuresService,
  ResourceTreeDTO,
  InfrastructureUpdateDTO,
  InfrastructureGetDTO
} from "opensilex-core/index";

@Component
export default class InfrastructureView extends Vue {
  $opensilex: any;
  $store: any;
  service: InfrastructuresService;
  $i18n: any;

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

