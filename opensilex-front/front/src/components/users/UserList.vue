<template>
  <div>
    <opensilex-StringFilter
      :filter.sync="filterPattern"
      placeholder="component.user.filter-placeholder"
    ></opensilex-StringFilter>
    <b-table
      ref="tableRef"
      striped
      hover
      small
      responsive
      primary-key="uri"
      :busy="isSearching"
      :items="loadData"
      :fields="fields"
      defaultSortBy="email"
    >
     <template v-slot:head(uri)="data">{{$t(data.label)}}</template>
      <template v-slot:head(firstName)="data">{{$t(data.label)}}</template>
      <template v-slot:head(lastName)="data">{{$t(data.label)}}</template>
      <template v-slot:head(email)="data">{{$t(data.label)}}</template>
      <template v-slot:head(admin)="data">{{$t(data.label)}}</template>
      <template v-slot:head(actions)="data">{{$t(data.label)}}</template>

      <template v-slot:cell(email)="data">
        <a :href="'mailto:' + data.item.email">{{ data.item.email }}</a>
      </template>

      <template v-slot:cell(uri)="data">
        <opensilex-UriLink :uri="data.item.uri"></opensilex-UriLink>
      </template>

      <template v-slot:cell(admin)="{data}">
        <span class="capitalize-first-letter" v-if="data.item.admin">{{$t("component.common.yes")}}</span>
        <span class="capitalize-first-letter" v-if="!data.item.admin">{{$t("component.common.no")}}</span>
      </template>

      <template v-slot:row-details>
        <strong class="capitalize-first-letter">{{$t("component.user.user-groups")}}:</strong>
        <ul>
          <li v-for="groupDetail in groupDetails" v-bind:key="groupDetail.uri">{{groupDetail.name}}</li>
        </ul>
      </template>

      <template v-slot:cell(actions)="{data}">
        <b-button-group size="sm">
          <opensilex-DetailButton
            @click="loadUserDetail(data)"
            label="component.user.details"
            :detailVisible="data.detailsShowing"
            :small="true"
          ></opensilex-DetailButton>
          <opensilex-EditButton
            v-if="user.hasCredential(credentials.CREDENTIAL_USER_MODIFICATION_ID)"
            @click="$emit('onEdit', data.item)"
            label="component.user.update"
            :small="true"
          ></opensilex-EditButton>
          <opensilex-DeleteButton
            v-if="user.hasCredential(credentials.CREDENTIAL_USER_DELETE_ID) && user.email != data.item.email"
            @click="$emit('onDelete', data.item.uri)"
            label="component.user.delete"
            :small="true"
          ></opensilex-DeleteButton>
        </b-button-group>
      </template>
    </opensilex-TableAsyncView>
  </div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
import {
  SecurityService,
  UserGetDTO,
  GroupDTO,
  NamedResourceDTO
} from "opensilex-security/index";
import HttpResponse, {
  OpenSilexResponse
} from "opensilex-security/HttpResponse";

@Component
export default class UserList extends Vue {
  $opensilex: any;
  service: SecurityService;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  currentPage: number = 1;
  pageSize = 20;
  totalRow = 0;
  sortBy = "firstName";
  sortDesc = false;
  isSearching = false;

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
    if (query.filter) {
      this.filter = decodeURI(query.filter);
    }

    this.service = this.$opensilex.getService("opensilex.SecurityService");
  }

  updateFilter() {
    this.$opensilex.updateURLParameter("filter", this.filter, "");
    this.refresh();
  }

  fields = [
    {
      key: "uri",
      label: "component.common.uri",
      sortable: true
    },
    {
      key: "email",
      label: "component.user.email",
      sortable: true
    },
    {
      key: "firstName",
      label: "component.user.first-name",
      sortable: true
    },
    {
      key: "lastName",
      label: "component.user.last-name",
      sortable: true
    },

    {
      key: "admin",
      label: "component.user.admin",
      sortable: true
    },
    {
      label: "component.common.actions",
      key: "actions",
      class: "table-actions"
    }
  ];

  @Ref("tableRef") readonly tableRef!: any;

  refresh() {
    this.tableRef.refresh();
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

    this.$opensilex.updateURLParameters({
      filterPattern: encodeURI(this.filterPattern),
      sortBy: encodeURI(this.sortBy),
      sortDesc: "" + this.sortDesc,
      currentPage: "" + this.currentPage,
      pageSize: "" + this.pageSize
    });

    this.$opensilex.disableLoader();
    this.isSearching = true;
    return this.service
      .searchUsers(
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

        this.isSearching = false;
        this.$opensilex.enableLoader();

        return http.response.result;
      })
      .catch(this.$opensilex.errorHandler);
  }

  groupDetails = [];
  loadUserDetail(data) {
    if (!data.detailsShowing) {
      this.groupDetails = [];
      this.service
        .getUserGroups(data.item.uri)
        .then(
          (http: HttpResponse<OpenSilexResponse<Array<NamedResourceDTO>>>) => {
            this.groupDetails = http.response.result;
            data.toggleDetails();
          }
        )
        .catch(this.$opensilex.errorHandler);
    } else {
      data.toggleDetails();
    }
  }

  deleteUser(uri: string) {
    this.service
      .deleteUser(uri)
      .then(() => {
        this.refresh();
        this.$emit("onDelete", uri);
      })
      .catch(this.$opensilex.errorHandler);
  }
}
</script>

<style scoped lang="scss">
</style>
