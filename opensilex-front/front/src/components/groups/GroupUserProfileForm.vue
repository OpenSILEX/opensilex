<template>
  <div>
    <opensilex-UserSelector
      label="component.group.user-profiles"
      :users.sync="searchedUser"
      :multiple="false"
      @select="selectUser"
    ></opensilex-UserSelector>
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
          value-field="uri"
          text-field="name"
          :options="profilesList"
          v-model="data.item.profile_uri"
          :required="true"
          class="profile-selector"
        ></b-form-select>
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
import {
  UserCreationDTO,
  GroupUpdateDTO,
  SecurityService,
  UserGetDTO,
  ProfileGetDTO,
  GroupUserProfileDTO
} from "opensilex-security/index";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";

@Component
export default class GroupUserProfileForm extends Vue {
  $opensilex: any;

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

