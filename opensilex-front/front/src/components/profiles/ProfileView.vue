<template>
  <div class="container-fluid">
    <opensilex-PageHeader
      icon="ik-settings"
      title="component.menu.security.profiles"
      description="component.profile.description"
    ></opensilex-PageHeader>
    <div class="card">
      <div class="card-header row clearfix">
        <div class="col col-sm-3">
          <div class="card-options d-inline-block">
            <b-button
              v-if="user.hasCredential(credentials.CREDENTIAL_PROFILE_MODIFICATION_ID)"
              @click="showCreateForm"
              variant="primary"
            >
              <i class="ik ik-plus"></i>
              {{$t('component.profile.add')}}
            </b-button>
          </div>
        </div>
      </div>
    </div>
    <div class="card-body p-0">
      <div class="table-responsive">
        <opensilex-UserList
          v-if="user.hasCredential(credentials.CREDENTIAL_USER_READ_ID)"
          ref="userList"
          @onEdit="editUser"
          @onDelete="deleteUser"
        ></opensilex-UserList>
      </div>
    </div>
    <opensilex-ProfileForm
      ref="profileForm"
      v-if="user.hasCredential(credentials.CREDENTIAL_PROFILE_MODIFICATION_ID)"
      v-bind:credentialsGroups="credentialsGroups"
      @onCreate="callCreateProfileService"
      @onUpdate="callUpdateProfileService"
    ></opensilex-ProfileForm>
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

  static credentialsGroups = [];
  static async asyncInit($opensilex) {
    console.debug("Loading credentials list...");
    let security: AuthenticationService = await $opensilex.loadService(
      "opensilex-security.AuthenticationService"
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
    this.service = this.$opensilex.getService("opensilex.SecurityService");
  }

  showCreateForm() {
    let profileForm: any = this.profileForm;
    profileForm.showCreateForm();
  }

  callCreateProfileService(form: ProfileCreationDTO, done) {
    done(
      this.service
        .createProfile(form)
        .then((http: HttpResponse<OpenSilexResponse<any>>) => {
          let uri = http.response.result;
          console.debug("Profile created", uri);
          let profileList: any = this.profileList;
          profileList.refresh();
        })
    );
  }

  callUpdateProfileService(form: ProfileUpdateDTO, done) {
    done(
      this.service
        .updateProfile(form)
        .then((http: HttpResponse<OpenSilexResponse<any>>) => {
          let uri = http.response.result;
          console.debug("Profile updated", uri);
          let profileList: any = this.profileList;
          profileList.refresh();
        })
    );
  }

  editProfile(form: ProfileGetDTO) {
    let profileForm: any = this.profileForm;
    profileForm.showEditForm(form);
  }

  deleteProfile(uri: string) {
    this.service
      .deleteProfile(uri)
      .then(() => {
        let profileList: any = this.profileList;
        profileList.refresh();
      })
      .catch(this.$opensilex.errorHandler);
  }
}
</script>

<style scoped lang="scss">
</style>
