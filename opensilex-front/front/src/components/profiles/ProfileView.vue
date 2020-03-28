<template>
  <div>
    <b-button
      v-if="user.hasCredential(credentials.CREDENTIAL_PROFILE_MODIFICATION_ID)"
      @click="showCreateForm"
      variant="success"
    >{{$t('component.profile.add')}}</b-button>
    <opensilex-ProfileForm
      ref="profileForm"
      v-if="user.hasCredential(credentials.CREDENTIAL_PROFILE_MODIFICATION_ID)"
      v-bind:credentialsGroups="credentialsGroups"
      @onCreate="callCreateProfileService"
      @onUpdate="callUpdateProfileService"
    ></opensilex-ProfileForm>
    <opensilex-ProfileList
      ref="profileList"
      v-if="user.hasCredential(credentials.CREDENTIAL_PROFILE_READ_ID)"
      v-bind:credentialsMapping="credentialsMapping"
      @onEdit="editProfile"
      @onDelete="deleteProfile"
    ></opensilex-ProfileList>
  </div>
</template>

<script lang="ts">
import { Component } from "vue-property-decorator";
import Vue from "vue";
import {
  UsersGroupsProfilesService,
  ProfileCreationDTO,
  ProfileUpdateDTO,
  ProfileGetDTO,
  SecurityService,
  CredentialsGroupDTO
} from "opensilex-rest/index";
import HttpResponse, { OpenSilexResponse } from "opensilex-rest/HttpResponse";

@Component
export default class ProfileView extends Vue {
  $opensilex: any;
  service: UsersGroupsProfilesService;
  $store: any;
  credentialsGroups: Array<CredentialsGroupDTO> = [];
  credentialsMapping: any = {};

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  static credentialsGroups = [];
  static async asyncInit($opensilex) {
    console.debug("Loading credentials list...");
    let security: SecurityService = await $opensilex.loadService(
      "opensilex-rest.SecurityService"
    );
    let http: HttpResponse<OpenSilexResponse<
      Array<CredentialsGroupDTO>
    >> = await security.getCredentialsGroups();
    ProfileView.credentialsGroups = http.response.result;
    console.debug("Credentials list loaded !", ProfileView.credentialsGroups);
  }

  created() {
    this.credentialsGroups = ProfileView.credentialsGroups;
    for (let i in this.credentialsGroups) {
      for (let j in this.credentialsGroups[i].credentials) {
        let credential = this.credentialsGroups[i].credentials[j];
        this.credentialsMapping[credential.id] = credential.label;
      }
    }
    this.service = this.$opensilex.getService("opensilex.UsersGroupsProfilesService");
  }

  showCreateForm() {
    let profileForm: any = this.$refs.profileForm;
    profileForm.showCreateForm();
  }

  callCreateProfileService(form: ProfileCreationDTO, done) {
    done(
      this.service
        .createProfile(this.user.getAuthorizationHeader(), form)
        .then((http: HttpResponse<OpenSilexResponse<any>>) => {
          let uri = http.response.result;
          console.debug("Profile created", uri);
          let profileList: any = this.$refs.profileList;
          profileList.refresh();
        })
    );
  }

  callUpdateProfileService(form: ProfileUpdateDTO, done) {
    done(
      this.service
        .updateProfile(this.user.getAuthorizationHeader(), form)
        .then((http: HttpResponse<OpenSilexResponse<any>>) => {
          let uri = http.response.result;
          console.debug("Profile updated", uri);
          let profileList: any = this.$refs.profileList;
          profileList.refresh();
        })
    );
  }

  editProfile(form: ProfileGetDTO) {
    let profileForm: any = this.$refs.profileForm;
    profileForm.showEditForm(form);
  }

  deleteProfile(uri: string) {
    this.service
      .deleteProfile(this.user.getAuthorizationHeader(), uri)
      .then(() => {
        let profileList: any = this.$refs.profileList;
        profileList.refresh();
      })
      .catch(this.$opensilex.errorHandler);
  }
}
</script>

<style scoped lang="scss">
</style>
