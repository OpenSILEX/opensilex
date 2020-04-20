<template>
  <div class="card">
    <div class="card-header">
      <h3>
        <i class="ik ik-map"></i>
        {{$t("component.infrastructure.devices")}}
      </h3>
      <div class="card-header-right">
        <b-button
          @click="showCreateForm"
          variant="primary"
          v-if="user.hasCredential(credentials.CREDENTIAL_INFRASTRUCTURE_MODIFICATION_ID)"
        >
          <i class="ik ik-plus"></i>
          {{$t('component.infrastructure.device.add')}}
        </b-button>
      </div>
    </div>
    <div class="card-body">
      <b-table
        v-if="selected"
        striped
        hover
        small
        sort-by="typeLabel"
        :items="selected.devices"
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
            <b-button
              size="sm"
              v-if="user.hasCredential(credentials.CREDENTIAL_INFRASTRUCTURE_MODIFICATION_ID)"
              @click.prevent="showEditForm(data.item)"
              variant="outline-primary"
            >
              <font-awesome-icon icon="edit" size="sm" />
            </b-button>
            <b-button
              size="sm"
              v-if="user.hasCredential(credentials.CREDENTIAL_INFRASTRUCTURE_DELETE_ID)"
              @click.prevent="deleteDevice(data.item.uri)"
              variant="danger"
            >
              <font-awesome-icon icon="trash-alt" size="sm" />
            </b-button>
          </b-button-group>
        </template>
      </b-table>
      <opensilex-InfrastructureDeviceForm
        ref="infrastructureDeviceForm"
        :parentURI="selected ? selected.uri : null"
        v-if="user.hasCredential(credentials.CREDENTIAL_INFRASTRUCTURE_MODIFICATION_ID)"
        @onUpdate="$emit('onUpdate', selected)"
        @onCreate="$emit('onCreate', selected)"
      ></opensilex-InfrastructureDeviceForm>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import {
  InfrastructuresService,
  ResourceTreeDTO,
  InfrastructureGetDTO,
  InfrastructureDeviceGetDTO,
  InfrastructureTeamDTO
} from "opensilex-core/index";
import { GroupCreationDTO, GroupUpdateDTO } from "opensilex-security/index";

@Component
export default class InfrastructureDevicesView extends Vue {
  $opensilex: any;
  $store: any;
  service: InfrastructuresService;

  @Ref("infrastructureDeviceForm") readonly infrastructureDeviceForm!: any;

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

  public showCreateForm() {
    this.infrastructureDeviceForm.showCreateForm();
  }

  public showEditForm(device) {
    this.infrastructureDeviceForm.showEditForm(device);
  }

  public deleteDevice(uri) {
    this.service.deleteInfrastructureDevice(uri).then(() => {
      this.$emit("onDelete", uri);
    });
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

.device-split-cell {
  width: 50%;
  display: inline-block;
}

.add-device-button-container {
  text-align: right;
}

.table-device {
  margin-top: 10px;
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

