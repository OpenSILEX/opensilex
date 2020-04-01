<template>
  <div>
    <b-input-group class="mt-3 mb-3" size="sm">
      <b-input-group>
        <b-form-input
          v-model="filterPattern"
          debounce="300"
          :placeholder="$t('component.user.filter-placeholder')"
        ></b-form-input>
        <template v-slot:append>
          <b-btn :disabled="!filterPattern" variant="primary" @click="filterPattern = ''">
            <font-awesome-icon icon="times" size="sm" />
          </b-btn>
        </template>
      </b-input-group>
    </b-input-group>
    <div class="tables">
      <div class="table-left">
        <div>
          <div class="table-title">{{$t('component.group.form-all-users-title')}}</div>
          <b-table
            ref="tableRef"
            striped
            hover
            small
            :items="loadData"
            :fields="fields"
            :sort-by.sync="sortBy"
            :sort-desc.sync="sortDesc"
            no-provider-paging
          >
            <template v-slot:head(firstName)="data">{{ $t(data.label) }}</template>

            <template v-slot:cell(selected)="data">
              <b-form-checkbox
                v-model="userProfilesByUserIDs[data.item.uri]"
                @change="toggleUserSelection(data.item)"
              ></b-form-checkbox>
            </template>

            <template v-slot:cell(firstName)="data">
              {{data.item.firstName}} {{data.item.lastName}}
              <a
                :href="'mailto:' + data.item.email"
              >({{ data.item.email }})</a>
            </template>
          </b-table>
        </div>
        <b-pagination
          class="bottom-pagination"
          v-model="currentPage"
          :total-rows="totalRow"
          :per-page="pageSize"
          @change="refresh()"
        ></b-pagination>
      </div>
      <div class="table-right">
        <div>
          <div
            class="table-title"
          >{{$t('component.group.form-selected-users-title')}} ({{userProfiles.length}})</div>
          <b-table
            id="user-selection-table"
            striped
            hover
            small
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

            <template v-slot:cell(firstName)="data">{{data.item.userName}}</template>

            <template v-slot:cell(uri)="data">
              <a class="uri-info">{{ data.item.uri }}</a>
            </template>

            <template v-slot:cell(admin)="data">
              <b-form-select
                size="sm"
                value-field="uri"
                text-field="name"
                :options="profiles"
                v-model="data.item.profileURI"
                :required="true"
                class="profile-selector"
              ></b-form-select>
            </template>
          </b-table>
        </div>
        <b-pagination
          class="bottom-pagination"
          v-model="currentSelectedPage"
          :total-rows="userProfiles.length"
          :per-page="pageSize"
          aria-controls="user-selection-table"
        ></b-pagination>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Emit, Ref } from "vue-property-decorator";
import Vue from "vue";
import VueRouter from "vue-router";
import {
  UserCreationDTO,
  GroupUpdateDTO,
  UsersGroupsProfilesService,
  UserGetDTO,
  ProfileGetDTO,
  GroupUserProfileModificationDTO,
  GroupUserProfileDTO
} from "opensilex-rest/index";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";

@Component
export default class GroupUserProfileForm extends Vue {
  $opensilex: any;
  $store: any;
  $router: VueRouter;

  profiles: Array<ProfileGetDTO> = [];

  userProfilesByUserIDs = {};

  @Prop()
  userProfiles;

  service: UsersGroupsProfilesService;

  get user() {
    return this.$store.state.user;
  }

  mounted() {
    this.clearForm();
  }

  clearForm() {
    this.currentSelectedPage = 1;
    this.currentPage = 1;
    this.pageSize = 5;
    this.totalRow = 0;
    this.sortBy = "firstName";
    this.sortDesc = false;
    this.filterPatternValue = "";
    this.userProfilesByUserIDs = {};
  }

  initFormProfiles(userProfiles) {
    this.userProfilesByUserIDs = {};
    userProfiles.forEach(userProfile => {
      this.userProfilesByUserIDs[userProfile.userURI] = true;
    });
  }
  @Ref("tableRef") readonly tableRef!: any;

  refresh() {
    this.tableRef.refresh();
  }

  currentPage: number = 1;
  currentSelectedPage: number = 1;
  pageSize = 5;
  totalRow = 0;
  sortBy = "firstName";
  sortDesc = false;

  private filterPatternValue: any = "";
  set filterPattern(value: string) {
    this.filterPatternValue = value;
    this.refresh();
  }

  get filterPattern() {
    return this.filterPatternValue;
  }

  async created() {
    this.service = this.$opensilex.getService(
      "opensilex.UsersGroupsProfilesService"
    );

    let http: HttpResponse<OpenSilexResponse<
      Array<ProfileGetDTO>
    >> = await this.service.getAllProfiles(
      this.$opensilex.getUser().getAuthorizationHeader()
    );
    this.profiles = http.response.result;
    console.debug("Profiles list loaded !", this.profiles);
  }

  loadData() {
    let orderBy = [];
    if (this.sortBy) {
      let orderByText = this.sortBy + "=";
      if (this.sortDesc) {
        orderBy.push(orderByText + "desc");
      } else {
        orderBy.push(orderByText + "asc");
      }
    }

    return this.service
      .searchUsers(
        this.user.getAuthorizationHeader(),
        this.filterPattern,
        orderBy,
        this.currentPage - 1,
        this.pageSize
      )
      .then((http: HttpResponse<OpenSilexResponse<Array<UserGetDTO>>>) => {
        this.totalRow = http.response.metadata.pagination.totalCount;
        this.pageSize = http.response.metadata.pagination.pageSize;
        setTimeout(() => {
          this.currentPage = http.response.metadata.pagination.currentPage + 1;
        }, 0);

        return http.response.result;
      })
      .catch(this.$opensilex.errorHandler);
  }

  fields = [
    {
      key: "selected",
      label: ""
    },
    {
      key: "firstName",
      label: "component.common.name",
      sortable: true
    }
  ];

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

  toggleUserSelection(user) {
    let userProfile: GroupUserProfileDTO = {
      userURI: user.uri,
      userName: user.firstName + " " + user.lastName
    };

    if (!this.userProfilesByUserIDs[user.uri]) {
      if (this.profiles.length > 0) {
        userProfile.profileURI = this.profiles[0].uri;
      }

      this.userProfilesByUserIDs[user.uri] = true;
      this.userProfiles.push(userProfile);
    } else {
      this.unselectUserProfile(userProfile);
    }
  }

  unselectUserProfile(userProfile) {
    const index = this.userProfiles.findIndex(
      up => up.userURI == userProfile.userURI
    );
    if (index > -1) {
      this.userProfiles.splice(index, 1);
    }
    this.userProfilesByUserIDs[userProfile.userURI] = false;
  }
}
</script>

<style scoped lang="scss">
.tables {
  display: flex;
}

.table-left,
.table-right {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.table-left > div:first-child,
.table-right > div:first-child {
  flex: 1;
  flex-direction: column;
  flex-grow: 1;
}

.table-left {
  margin-right: 5px;
}

.table-right {
  margin-left: 5px;
}

.profile-selector {
  height: 20px;
  line-height: 15px;
  padding-top: 0;
  padding-bottom: 0;
}

.table-title {
  font-weight: bold;
  text-align: center;
}

.btn-xs {
  width: 25px;
  height: 25px;
  padding: 0;
}
</style>

