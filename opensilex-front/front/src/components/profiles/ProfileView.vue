<template>
  <div class="container-fluid">
    <opensilex-PageHeader
      icon="ik#ik-settings"
      title="component.menu.security.profiles"
      description="component.profile.description"
    ></opensilex-PageHeader>

    <opensilex-PageActions>
      <template v-slot>
        <opensilex-CreateButton
          v-if="user.hasCredential(credentials.CREDENTIAL_PROFILE_MODIFICATION_ID)"
          @click="profileForm.showCreateForm()"
          label="component.profile.add"
        ></opensilex-CreateButton>
      </template>
    </opensilex-PageActions>

    <opensilex-PageContent>
      <template v-slot>
        <opensilex-ProfileList
          v-if="user.hasCredential(credentials.CREDENTIAL_PROFILE_READ_ID)"
          ref="profileList"
          v-bind:credentialsGroups="credentialsGroups"
          @onEdit="profileForm.showEditForm($event)"
        ></opensilex-ProfileList>
      </template>
    </opensilex-PageContent>

    <opensilex-ModalForm
      v-if="user.hasCredential(credentials.CREDENTIAL_PROFILE_MODIFICATION_ID)"
      ref="profileForm"
      component="opensilex-ProfileForm"
      createTitle="component.profile.add"
      editTitle="component.profile.update"
      modalSize="lg"
      icon="ik#ik-settings"
      @onCreate="profileList.refresh()"
      @onUpdate="profileList.refresh()"
    ></opensilex-ModalForm>
  </div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
import {
  ProfileCreationDTO,
  ProfileUpdateDTO,
  ProfileGetDTO,
  SecurityService,
  CredentialsGroupDTO,
  AuthenticationService
} from "opensilex-security/index";
import HttpResponse, {
  OpenSilexResponse
} from "opensilex-security/HttpResponse";

@Component
export default class ProfileView extends Vue {
  $opensilex: any;
  service: SecurityService;
  $store: any;
  credentialsGroups: Array<CredentialsGroupDTO> = [];
  credentialsMapping: any = {};

  @Ref("profileForm") readonly profileForm!: any;
  @Ref("profileList") readonly profileList!: any;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }
}
</script>

<style scoped lang="scss">
</style>
