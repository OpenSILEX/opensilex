<template>
  <div>
    <b-input-group class="mt-3 mb-3" size="sm">
      <b-input-group>
        <b-form-input
          v-model="filterPattern"
          debounce="300"
          :placeholder="$t('component.group.filter-placeholder')"
        ></b-form-input>
        <template v-slot:append>
          <b-btn :disabled="!filterPattern" variant="primary" @click="filterPattern = ''">
            <font-awesome-icon icon="times" size="sm" />
          </b-btn>
        </template>
      </b-input-group>
    </b-input-group>
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
      <template v-slot:head(name)="data">{{$t(data.label)}}</template>
      <template v-slot:head(description)="data">{{$t(data.label)}}</template>
      <template v-slot:head(userProfiles)="data">{{$t(data.label)}}</template>
      <template v-slot:head(actions)="data">{{$t(data.label)}}</template>

      <template v-slot:cell(userProfiles)="data">
        <div>{{$tc("component.user.label", data.item.userProfiles.length, {count: data.item.userProfiles.length})}}</div>
      </template>

      <template v-slot:row-details="data">
        <strong class="capitalize-first-letter">{{$t("component.user.users")}}:</strong>
        <ul>
          <li
            v-for="userProfile in data.item.userProfiles"
            v-bind:key="userProfile.uri"
          >{{userProfile.userName}} ({{userProfile.profileName}})</li>
        </ul>
      </template>

      <template v-slot:cell(actions)="data">
        <b-button-group size="sm">
          <b-button size="sm" @click="data.toggleDetails" variant="outline-success">
            <font-awesome-icon v-if="!data.detailsShowing" icon="eye" size="sm" />
            <font-awesome-icon v-if="data.detailsShowing" icon="eye-slash" size="sm" />
          </b-button>
          <b-button
            size="sm"
            v-if="user.hasCredential(credentials.CREDENTIAL_GROUP_MODIFICATION_ID)"
            @click="$emit('onEdit', data.item)"
            variant="outline-primary"
          >
            <font-awesome-icon icon="edit" size="sm" />
          </b-button>
          <b-button
            size="sm"
            v-if="user.hasCredential(credentials.CREDENTIAL_GROUP_DELETE_ID)"
            @click="$emit('onDelete', data.item.uri)"
            variant="danger"
          >
            <font-awesome-icon icon="trash-alt" size="sm" />
          </b-button>
        </b-button-group>
      </template>
    </b-table>
    <b-pagination
      v-model="currentPage"
      :total-rows="totalRow"
      :per-page="pageSize"
      @change="refresh()"
    ></b-pagination>
  </div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
import VueRouter from "vue-router";
import HttpResponse, { OpenSilexResponse } from "opensilex-security/HttpResponse";
import { SecurityService, GroupDTO } from "opensilex-security/index";

@Component
export default class GroupList extends Vue {
  $opensilex: any;
  $store: any;
  $router: VueRouter;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  currentPage: number = 1;
  pageSize = 20;
  totalRow = 0;
  sortBy = "name";
  sortDesc = false;

  private filterPatternValue: any = "";
  set filterPattern(value: string) {
    this.filterPatternValue = value;
    this.refresh();
  }

  get filterPattern() {
    return this.filterPatternValue;
  }

  created() {
    let query: any = this.$route.query;
    if (query.filterPattern) {
      this.filterPatternValue = decodeURI(query.filterPattern);
    }
    if (query.pageSize) {
      this.pageSize = parseInt(query.pageSize);
    }
    if (query.currentPage) {
      this.currentPage = parseInt(query.currentPage);
    }
    if (query.sortBy) {
      this.sortBy = decodeURI(query.sortBy);
    }
    if (query.sortDesc) {
      this.sortDesc = query.sortDesc == "true";
    }
  }

  fields = [
    {
      key: "name",
      label: "component.common.name",
      sortable: true
    },
    {
      label: "component.common.description",
      key: "description",
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

  @Ref("tableRef") readonly tableRef!: any;

  refresh() {
    this.tableRef.refresh();
  }

  loadData() {
    let service: SecurityService = this.$opensilex.getService(
      "opensilex.SecurityService"
    );

    let orderBy = [];
    if (this.sortBy) {
      let orderByText = this.sortBy + "=";
      if (this.sortDesc) {
        orderBy.push(orderByText + "desc");
      } else {
        orderBy.push(orderByText + "asc");
      }
    }

    return service
      .searchGroups(
        this.filterPattern,
        orderBy,
        this.currentPage - 1,
        this.pageSize
      )
      .then((http: HttpResponse<OpenSilexResponse<Array<GroupDTO>>>) => {
        this.totalRow = http.response.metadata.pagination.totalCount;
        this.pageSize = http.response.metadata.pagination.pageSize;
        setTimeout(() => {
          this.currentPage = http.response.metadata.pagination.currentPage + 1;
        }, 0);

        this.$router
          .push({
            path: this.$route.fullPath,
            query: {
              filterPattern: encodeURI(this.filterPattern),
              sortBy: encodeURI(this.sortBy),
              sortDesc: "" + this.sortDesc,
              currentPage: "" + this.currentPage,
              pageSize: "" + this.pageSize
            }
          })
          .catch(function() {});

        return http.response.result;
      })
      .catch(this.$opensilex.errorHandler);
  }
}
</script>

<style scoped lang="scss">
.uri-info {
  text-overflow: ellipsis;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: inline-block;
  max-width: 300px;
}
</style>
