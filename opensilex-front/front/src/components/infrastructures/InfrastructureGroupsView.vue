<template>
  <b-card v-if="selected">
    <template v-slot:header>
      <h3>
        <opensilex-Icon icon="ik#ik-users" />
        {{$t("component.experiment.groups")}}
      </h3>
      <div class="card-header-right">
        <opensilex-CreateButton
          v-if="user.hasCredential(credentials.CREDENTIAL_GROUP_MODIFICATION_ID)"
          @click="groupForm.showCreateForm()"
          label="component.group.add"
        ></opensilex-CreateButton>
      </div>
    </template>

    <b-table
      striped
      hover
      small
      responsive
      sort-icon-left
      sort-by="name"
      :items="selected.groups"
      :fields="fields"
    >
      <template v-slot:head(name)="data">{{$t(data.label)}}</template>
      <template v-slot:head(user_profiles)="data">{{$t(data.label)}}</template>
      <template v-slot:head(actions)="data">{{$t(data.label)}}</template>

      <template v-slot:cell(name)="data">
        <span class="capitalize-first-letter">{{data.item.name}}</span>
      </template>

      <template v-slot:cell(user_profiles)="data">
        <div>{{$tc("component.user.label", data.item.user_profiles.length, {count: data.item.user_profiles.length})}}</div>
      </template>

      <template v-slot:row-details="data">
        <div class="static-field">
          <span class="static-field-key">{{$t("component.common.uri")}}:</span>
          <span class="static-field-line">{{data.item.uri}}</span>
        </div>
        <div class="static-field">
          <span class="static-field-key">{{$t("component.common.description")}}:</span>
          <span class="static-field-line">{{data.item.description}}</span>
        </div>
        <strong class="capitalize-first-letter">{{$t("component.user.users")}}:</strong>
        <ul>
          <li
            v-for="userProfile in data.item.user_profiles"
            v-bind:key="userProfile.uri"
          >{{userProfile.user_name}} ({{userProfile.profile_name}})</li>
        </ul>
      </template>

      <template v-slot:cell(actions)="data">
        <b-button-group size="sm">
          <opensilex-DetailButton
            @click="data.toggleDetails"
            label="component.group.details"
            :detailVisible="data.detailsShowing"
            :small="true"
          ></opensilex-DetailButton>
          <opensilex-EditButton
            v-if="user.hasCredential(credentials.CREDENTIAL_GROUP_MODIFICATION_ID)"
            @click="groupForm.showEditForm(data.item)"
            label="component.group.update"
            :small="true"
          ></opensilex-EditButton>
          <opensilex-DeleteButton
            v-if="user.hasCredential(credentials.CREDENTIAL_GROUP_DELETE_ID)"
            @click="deleteGroup(data.item.uri)"
            label="component.group.delete"
            :small="true"
          ></opensilex-DeleteButton>
        </b-button-group>
      </template>
    </b-table>

    <opensilex-ModalForm
      v-if="user.hasCredential(credentials.CREDENTIAL_GROUP_MODIFICATION_ID)"
      ref="groupForm"
      component="opensilex-GroupForm"
      createTitle="component.group.add"
      editTitle="component.group.update"
      icon="ik#ik-users"
      modalSize="lg"
      @onCreate="$emit('onCreate', $event)"
      @onUpdate="$emit('onUpdate', $event)"
      :updateAction="update"
      :createAction="create"
      :initForm="setInfrastructure"
    ></opensilex-ModalForm>
  </b-card>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
// @ts-ignore
import { InfrastructureGetDTO } from "opensilex-core/index";

@Component
export default class InfrastructureGroupsView extends Vue {
  $opensilex: any;
  $store: any;

  @Ref("groupForm") readonly groupForm!: any;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  fields = [
    {
      key: "name",
      label: "component.common.name",
      sortable: true
    },
    {
      label: "component.user.users",
      key: "user_profiles"
    },
    {
      label: "component.common.actions",
      key: "actions"
    }
  ];

  @Prop()
  private selected: InfrastructureGetDTO;

  public deleteGroup(uri) {
    this.$opensilex
      .getService("opensilex.OrganisationsService")
      .deleteInfrastructureTeam(uri)
      .then(() => {
        this.$emit("onDelete", uri);
      });
  }

  create(form) {
    return this.$opensilex
      .getService("opensilex.OrganisationsService")
      .createInfrastructureTeam(form)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        console.debug("Team created", uri);
      })
      .catch(error => {
        if (error.status == 409) {
          console.error("Team already exists", error);
          this.$opensilex.errorHandler(
            error,
            this.$t("component.group.errors.group-already-exists")
          );
        } else {
          this.$opensilex.errorHandler(error);
        }
      });
  }

  update(form) {
    return this.$opensilex
      .getService("opensilex.OrganisationsService")
      .updateInfrastructureTeam(form)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        console.debug("Team updated", uri);
      })
      .catch(this.$opensilex.errorHandler);
  }

  setInfrastructure(form) {
    form.organisation = this.selected.uri;
  }
}
</script>

<style scoped lang="scss">
</style>

