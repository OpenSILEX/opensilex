<template>
  <div>
    <opensilex-AccountSelector
      label="component.group.user-profiles"
      :users.sync="searchedUser"
      :multiple="false"
      @select="selectUser"
    ></opensilex-AccountSelector>
    <b-table
      id="user-selection-table"
      striped
      hover
      small
      responsive
      :items="userProfiles"
      :fields="selectedFields"
      :per-page="pageSize"
      :current-page="currentSelectedPage"
    >
      <template v-slot:head(firstName)="data">{{ $t(data.label) }}</template>
      <template v-slot:head(admin)="data">{{ $t(data.label) }}</template>

      <template v-slot:cell(selected)="data">
        <b-btn
          variant="outline-danger"
          class="btn-no-border"
          size="xs"
          @click="unselectUserProfile(data.item)"
        >
          <font-awesome-icon icon="trash-alt" size="xs" />
        </b-btn>
      </template>

      <template v-slot:cell(firstName)="data">{{data.item.user_name}}</template>

      <template v-slot:cell(uri)="data">
        <a class="uri-info">{{ data.item.uri }}</a>
      </template>

      <template v-slot:cell(admin)="data">
        <b-form-select
          size="sm"
          :options="profileOptionsWithFallback"
          :value="normalizeUri(data.item.profile_uri)"
          @input="onProfileChange(data.item, $event)"
          :required="true"
          class="profile-selector"
        />
      </template>
    </b-table>
    <b-pagination
      class="bottom-pagination"
      v-model="currentSelectedPage"
      :total-rows="userProfiles.length"
      :per-page="pageSize"
      aria-controls="user-selection-table"
    ></b-pagination>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Emit, Ref, PropSync } from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import { SecurityService, ProfileGetDTO, GroupUserProfileDTO } from "opensilex-security/index";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";

@Component
export default class GroupUserProfileForm extends Vue {
  $opensilex: OpenSilexVuePlugin;

  searchedUser = null;

  profilesList: Array<ProfileGetDTO> = [];

  @PropSync("profiles")
  userProfiles;

  service: SecurityService;

  mounted() {
    this.clearForm();
  }

  clearForm() {
    this.currentSelectedPage = 1;
    this.pageSize = 5;
    this.sortBy = "first_name";
    this.sortDesc = false;
    this.searchedUser = null;
  }

  currentSelectedPage: number = 1;
  pageSize = 5;
  sortBy = "first_name";
  sortDesc = false;

  async created() {
    this.service = this.$opensilex.getService("opensilex.SecurityService");

      let http: HttpResponse<OpenSilexResponse<
          Array<ProfileGetDTO>
      >> = await this.service.getAllProfiles();
      this.profilesList = http.response.result;
  }

  selectedFields = [
    {
      key: "selected",
      label: ""
    },
    {
      key: "firstName",
      label: "component.common.name",
      sortable: true
    },
    {
      key: "admin",
      label: "component.profile.label"
    }
  ];

  // Builds the list of profile options from the profiles returned by the API
  get profileOptions() {
    return (this.profilesList || []).map(profil => ({
      value: profil.uri,
      text: profil.name
    }));
  }

  // Adds to the options the profiles already used in the group but absent from the API : getAllProfiles()
  get profileOptionsWithFallback() {
    const existingOptions = this.profileOptions;

    const knownProfileUris = new Set(
      existingOptions.map(option => option.value)
    );

    const missingOptions = (this.userProfiles || [])
      .map(userProfile => ({
        value: userProfile.profile_uri,
        text: userProfile.profile_name || userProfile.profile_uri
      }))
      .filter(option =>
        option.value && !knownProfileUris.has(option.value)
      );

    return [...missingOptions, ...existingOptions];
  }

  // Updates the selected profile for a user and synchronizes the URI and label
  onProfileChange(item, newUri) {
    item.profile_uri = newUri;

    // If the profile is known, the name is also synchronized.
    const profilOptions = this.profileOptionsWithFallback.find(option => option.value === newUri);
    if (profilOptions) {
      item.profile_name = profilOptions.text;
    } else {
      // Fallback 
      item.profile_name = item.profile_name || newUri;
    }
  }

  normalizeUri(u: any) {
    return String(u ?? "").trim().replace(/\/+$/, "");
  }

  selectUser(user) {
    const index = this.userProfiles.findIndex(up => up.user_uri == user.id);
    if (index > -1) {
      return;
    }

    let user_profile: GroupUserProfileDTO = {
      user_uri: user.id,
      user_name: user.label
    };

    if (this.profilesList.length > 0) {
      user_profile.profile_uri = this.profilesList[0].uri;
    }
    this.userProfiles.push(user_profile);
  }

  unselectUserProfile(userProfile) {
    const index = this.userProfiles.findIndex(
      up => up.user_uri == userProfile.user_uri
    );
    if (index > -1) {
      this.userProfiles.splice(index, 1);
    }
  }
}
</script>

<style scoped lang="scss">
.profile-selector {
  height: 20px;
  line-height: 15px;
  padding-top: 0;
  padding-bottom: 0;
}

.btn-xs {
  width: 25px;
  height: 25px;
  padding: 0;
}
</style>