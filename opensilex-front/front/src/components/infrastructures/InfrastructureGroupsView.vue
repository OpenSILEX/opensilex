<template>
  <div class="card">
    <div class="card-header">
      <h3>
        <i class="ik ik-users"></i>
        {{$t("component.experiment.groups")}}
      </h3>
      <div class="card-header-right">
        <b-button
          @click="showCreateForm"
          variant="primary"
          v-if="user.hasCredential(credentials.CREDENTIAL_INFRASTRUCTURE_MODIFICATION_ID)"
        >
          <i class="ik ik-plus"></i>
          {{$t('component.group.add')}}
        </b-button>
      </div>
    </div>
    <div class="card-body">
      <b-table
        v-if="selected"
        striped
        hover
        small
        sort-by="name"
        :items="selected.groups"
        :fields="fields"
      >
        <template v-slot:head(name)="data">{{$t(data.label)}}</template>
        <template v-slot:head(userProfiles)="data">{{$t(data.label)}}</template>
        <template v-slot:head(actions)="data">{{$t(data.label)}}</template>

        <template v-slot:cell(name)="data">
          <span class="capitalize-first-letter">{{data.item.name}}</span>
        </template>

        <template v-slot:cell(userProfiles)="data">
          <div>{{$tc("component.user.label", data.item.userProfiles.length, {count: data.item.userProfiles.length})}}</div>
        </template>

        <template v-slot:row-details="data">
          <div class="static-field">
            <span class="static-field-key">{{$t("component.common.description")}}:</span>
            <span class="static-field-line">{{data.item.description}}</span>
          </div>
          <strong class="capitalize-first-letter">{{$t("component.user.users")}}:</strong>
          <ul>
            <li
              v-for="userProfile in data.item.userProfiles"
              v-bind:key="userProfile.uri"
            >{{userProfile.userName}} ({{userProfile.profileName}})</li>
          </ul>
        </template>

        <template v-slot:cell(actions)="data">
          <b-button-group class="tree-button-group" size="sm">
            <b-button size="sm" @click="data.toggleDetails" variant="outline-success">
              <font-awesome-icon v-if="!data.detailsShowing" icon="eye" size="sm" />
              <font-awesome-icon v-if="data.detailsShowing" icon="eye-slash" size="sm" />
            </b-button>
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
              @click.prevent="deleteGroup(data.item.uri)"
              variant="danger"
            >
              <font-awesome-icon icon="trash-alt" size="sm" />
            </b-button>
          </b-button-group>
        </template>
      </b-table>

      <opensilex-GroupForm
        ref="groupForm"
        v-if="user.hasCredential(credentials.CREDENTIAL_GROUP_MODIFICATION_ID)"
        @onCreate="callCreateGroupService"
        @onUpdate="callUpdateGroupService"
      ></opensilex-GroupForm>
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
export default class InfrastructureGroupsView extends Vue {
  $opensilex: any;
  $store: any;
  service: InfrastructuresService;

  @Ref("groupForm") readonly groupForm!: any;

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

  fields = [
    {
      key: "name",
      label: "component.common.name",
      sortable: true
    },
    {
      label: "component.user.users",
      key: "userProfiles"
    },
    {
      label: "component.common.actions",
      key: "actions"
    }
  ];

  @Prop()
  private selected: InfrastructureGetDTO;

  public showCreateForm() {
    this.groupForm.showCreateForm();
  }

  public showEditForm(device) {
    this.groupForm.showEditForm(device);
  }

  public deleteGroup(uri) {
    this.service.deleteInfrastructureTeam(uri).then(() => {
      this.$emit("onDelete", uri);
    });
  }

  callCreateGroupService(form: InfrastructureTeamDTO, done) {
    form.infrastructure = this.selected.uri;
    done(
      this.service
        .createInfrastructureTeam(form)
        .then((http: HttpResponse<OpenSilexResponse<any>>) => {
          this.$emit("onCreate", form);
        })
    );
  }

  callUpdateGroupService(form: InfrastructureTeamDTO, done) {
    form.infrastructure = this.selected.uri;
    done(
      this.service
        .updateInfrastructureTeam(form)
        .then((http: HttpResponse<OpenSilexResponse<any>>) => {
          this.$emit("onUpdate", form);
        })
    );
  }
}
</script>

<style scoped lang="scss">
</style>

