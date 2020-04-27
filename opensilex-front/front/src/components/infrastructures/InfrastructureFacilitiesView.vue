<template>
  <b-card>
    <template v-slot:header>
      <h3>
        <i class="ik ik-map"></i>
        {{$t("component.infrastructure.facilities")}}
      </h3>
      <div class="card-header-right">
        <opensilex-CreateButton
          v-if="user.hasCredential(credentials.CREDENTIAL_INFRASTRUCTURE_MODIFICATION_ID)"
          @click="showCreateForm"
          label="component.infrastructure.facility.add"
        ></opensilex-CreateButton>
      </div>
    </template>
    <b-table
      v-if="selected"
      striped
      hover
      small
      responsive
      sort-by="typeLabel"
      :items="selected.facilities"
      :fields="fields"
    >
      <template v-slot:head(name)="data">{{$t(data.label)}}</template>
      <template v-slot:head(typeLabel)="data">{{$t(data.label)}}</template>
      <template v-slot:head(actions)="data">{{$t(data.label)}}</template>

      <template v-slot:cell(name)="data">
        <font-awesome-icon :icon="$opensilex.getRDFIcon(data.item.type)" size="sm" />&nbsp;
        <span class="capitalize-first-letter">{{data.item.name}}</span>
      </template>

      <template v-slot:cell(typeLabel)="data">
        <span class="capitalize-first-letter">{{data.item.typeLabel}}</span>
      </template>

      <template v-slot:cell(actions)="data">
        <b-button-group class="tree-button-group" size="sm">
          <opensilex-EditButton
            v-if="user.hasCredential(credentials.CREDENTIAL_INFRASTRUCTURE_MODIFICATION_ID)"
            @click="infrastructureFacilityForm.showEditForm(data.item)"
            label="component.infrastructure.facility.update"
            :small="true"
          ></opensilex-EditButton>
          <opensilex-DeleteButton
            v-if="user.hasCredential(credentials.CREDENTIAL_INFRASTRUCTURE_DELETE_ID)"
            @click="deleteFacility(data.item.uri)"
            label="component.infrastructure.facility.delete"
            :small="true"
          ></opensilex-DeleteButton>
        </b-button-group>
      </template>
    </b-table>
    <opensilex-InfrastructureFacilityForm
      ref="infrastructureFacilityForm"
      :parentURI="selected ? selected.uri : null"
      v-if="user.hasCredential(credentials.CREDENTIAL_INFRASTRUCTURE_MODIFICATION_ID)"
      @onUpdate="$emit('onUpdate', selected)"
      @onCreate="$emit('onCreate', selected)"
    ></opensilex-InfrastructureFacilityForm>
  </b-card>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import {
  InfrastructuresService,
  ResourceTreeDTO,
  InfrastructureGetDTO,
  InfrastructureFacilityGetDTO,
  InfrastructureTeamDTO
} from "opensilex-core/index";
import { GroupCreationDTO, GroupUpdateDTO } from "opensilex-security/index";

@Component
export default class InfrastructureFacilitiesView extends Vue {
  $opensilex: any;
  $store: any;
  service: InfrastructuresService;

  @Ref("infrastructureFacilityForm") readonly infrastructureFacilityForm!: any;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  created() {
    this.service = this.$opensilex.getService(
      "opensilex-core.InfrastructuresService"
    );
  }

  @Prop()
  private selected: InfrastructureGetDTO;

  fields = [
    {
      key: "name",
      label: "component.common.name",
      sortable: true
    },
    {
      key: "typeLabel",
      label: "component.common.type",
      sortable: true
    },
    {
      label: "component.common.actions",
      key: "actions"
    }
  ];

  public deleteFacility(uri) {
    this.service.deleteInfrastructureFacility(uri).then(() => {
      this.$emit("onDelete", uri);
    });
  }

  public showCreateForm() {
    this.infrastructureFacilityForm.showCreateForm();
  }
}
</script>

<style scoped lang="scss">
.table {
  border: 1px solid #dee2e6;
  border-radius: 3px;
  border-collapse: separate;
}
.table th {
  text-align: left;
}

.sl-vue-tree-root {
  min-height: 400px;
  max-height: 600px;
  overflow-y: auto;
}

.user-list {
  padding-left: 10px;
}

.leaf-spacer {
  display: inline-block;
  width: 23px;
}

.button-cell {
  padding-top: 1px;
  padding-bottom: 1px;
}

@media (max-width: 768px) {
  .sl-vue-tree-root {
    min-height: auto;
  }

  .table {
    margin-top: 10px;
  }
}
</style>

