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
          @click="groupSelectorForm.showCreateForm()"
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
      ref="groupSelectorForm"
      component="opensilex-GroupSelectorForm"
      createTitle="component.group.add"
      editTitle="component.group.update"
      icon="ik#ik-users"
      modalSize="lg"
      @onCreate="$emit('onCreate', $event)"
      @onUpdate="$emit('onUpdate', $event)"
      :createAction="updateGroups"
      :initForm="setInfrastructure"
    ></opensilex-ModalForm>
  </b-card>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
// @ts-ignore
import { InfrastructureGetDTO, InfrastructureUpdateDTO } from "opensilex-core/index";

@Component
export default class InfrastructureGroupsView extends Vue {
  $opensilex: any;
  $store: any;

  @Ref("groupSelectorForm") readonly groupSelectorForm!: any;

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

  deleteGroup(uri) {
    let groupUris = this.selected.groups
        .filter(group => group.uri !== uri)
        .map(group => group.uri);
    return this.updateInfrastructureGroups(groupUris);
  }

  updateGroups(form) {
    return this.updateInfrastructureGroups(form.groups);
  }

  updateInfrastructureGroups(groupUris: Array<string>) {
    let infrastructure: InfrastructureUpdateDTO = {
      ...this.selected,
      groupUris: groupUris
    };
    return this.$opensilex
        .getService("opensilex.OrganisationsService")
        .updateInfrastructure(infrastructure)
        .then((http: HttpResponse<OpenSilexResponse<any>>) => {
          console.debug("Infrastructure updated", http.response.result);
          this.$emit('onUpdate');
        })
        .catch(error => {
          this.$opensilex.errorHandler(error);
        });
  }

  setInfrastructure(form) {
    form.groups = this.selected.groups.map((group) => group.uri);
  }
}
</script>

<style scoped lang="scss">
</style>

