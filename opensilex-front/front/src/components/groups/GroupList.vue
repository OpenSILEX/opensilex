<template>
  <div>
    <opensilex-StringFilter
      :filter.sync="filterPattern"
      placeholder="component.group.filter-placeholder"
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
    >
      <template v-slot:head(uri)="data">{{$t(data.label)}}</template>
      <template v-slot:head(name)="data">{{$t(data.label)}}</template>
      <template v-slot:head(description)="data">{{$t(data.label)}}</template>
      <template v-slot:head(userProfiles)="data">{{$t(data.label)}}</template>
      <template v-slot:head(actions)="data">{{$t(data.label)}}</template>

      <template v-slot:cell(uri)="data">
        <opensilex-UriLink :uri="data.item.uri"></opensilex-UriLink>
      </template>

      <template v-slot:cell(userProfiles)="data">
        <div>{{$tc("component.user.label", data.item.userProfiles.length, {count: data.item.userProfiles.length})}}</div>
      </template>

      <template v-slot:row-details="{data}">
        <strong class="capitalize-first-letter">{{$t("component.user.users")}}:</strong>
        <ul>
          <li
            v-for="userProfile in data.item.userProfiles"
            v-bind:key="userProfile.uri"
          >{{userProfile.userName}} ({{userProfile.profileName}})</li>
        </ul>
      </template>

      <template v-slot:cell(actions)="{data}">
        <b-button-group size="sm">
          <opensilex-DetailButton
            @click="data.toggleDetails"
            label="component.group.details"
            :detailVisible="data.detailsShowing"
            :small="true"
          ></opensilex-DetailButton>
          <opensilex-EditButton
            v-if="user.hasCredential(credentials.CREDENTIAL_GROUP_MODIFICATION_ID)"
            @click="$emit('onEdit', data.item)"
            label="component.group.update"
            :small="true"
          ></opensilex-EditButton>
          <opensilex-DeleteButton
            v-if="user.hasCredential(credentials.CREDENTIAL_GROUP_DELETE_ID)"
            @click="$emit('onDelete', data.item.uri)"
            label="component.group.delete"
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
import VueRouter from "vue-router";
import HttpResponse, {
  OpenSilexResponse
} from "opensilex-security/HttpResponse";
import { SecurityService, GroupDTO } from "opensilex-security/index";

@Component
export default class GroupList extends Vue {
  $opensilex: any;
  $store: any;

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
      key: "actions",
      class: "table-actions"
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

    this.$opensilex.updateURLParameters({
      filterPattern: encodeURI(this.filterPattern),
      sortBy: encodeURI(this.sortBy),
      sortDesc: "" + this.sortDesc,
      currentPage: "" + this.currentPage,
      pageSize: "" + this.pageSize
    });

    this.$opensilex.disableLoader();
    this.isSearching = true;
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

        this.isSearching = false;
        this.$opensilex.enableLoader();

        return http.response.result;
      })
      .catch(this.$opensilex.errorHandler);
  }
}
</script>

<style scoped lang="scss">
</style>
