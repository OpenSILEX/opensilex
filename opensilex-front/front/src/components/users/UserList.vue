<template>
  <div>
    <opensilex-StringFilter
      :filter.sync="filter"
      @update="updateFilter()"
      placeholder="component.user.filter-placeholder"
    ></opensilex-StringFilter>

    <opensilex-TableAsyncView
      ref="tableRef"
      :searchMethod="searchUsers"
      :fields="fields"
      defaultSortBy="email"
    >
      <template v-slot:cell(lastName)="{data}">
        <opensilex-UriLink
          :uri="data.item.uri"
          :value="data.item.lastName"
          :noExternalLink="true"
          @click="data.toggleDetails()"
        ></opensilex-UriLink>
      </template>

      <template v-slot:cell(email)="{data}">
        <a :href="'mailto:' + data.item.email">{{ data.item.email }}</a>
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
            @click="deleteUser(data.item.uri)"
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
  $store: any;
  $route: any;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  private filter: any = "";

  created() {
    let query: any = this.$route.query;
    if (query.filter) {
      this.filter = decodeURIComponent(query.filter);
    }

    this.service = this.$opensilex.getService("opensilex.SecurityService");
  }

  updateFilter() {
    this.$opensilex.updateURLParameter("filter", this.filter, "");
    this.refresh();
  }

  fields = [
    {
      key: "lastName",
      label: "component.user.last-name",
      sortable: true
    },
    {
      key: "firstName",
      label: "component.user.first-name",
      sortable: true
    },
    {
      key: "email",
      label: "component.user.email",
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

  searchUsers(options) {
    return this.service.searchUsers(
      this.filter,
      options.orderBy,
      options.currentPage,
      options.pageSize
    );
  }

  currentURI = null;
  groupDetails = [];
  loadUserDetail(data) {
    if (!data.detailsShowing) {
      this.groupDetails = [];
      this.currentURI = data.item.uri;
      this.$opensilex.disableLoader();
      this.service
        .getUserGroups(data.item.uri)
        .then(
          (http: HttpResponse<OpenSilexResponse<Array<NamedResourceDTO>>>) => {
            this.groupDetails = http.response.result;
            data.toggleDetails();
            this.$opensilex.enableLoader();
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
