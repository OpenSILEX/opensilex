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
      <template v-slot:cell(last_name)="{data}">
        <opensilex-UriLink
          :uri="data.item.uri"
          :value="data.item.last_name"
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

      <template v-slot:row-details="{data}">
        <strong class="capitalize-first-letter">{{$t("component.user.user-groups")}}:</strong>
        <ul>
          <li
            v-for="groupDetail in  data.item.groupDetails"
            v-bind:key="groupDetail.uri"
          >{{groupDetail.name}}</li>
        </ul>
      </template>

      <template v-slot:cell(actions)="{data}">
        <b-button-group size="sm">
          <opensilex-DetailButton
            @click="data.toggleDetails()"
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
// @ts-ignore
import { SecurityService } from "opensilex-security/index";

@Component
export default class UserList extends Vue {
  $opensilex: any;
  service: SecurityService;
  $store: any;
  $route: any;
  fields = [
    {
      key: "last_name",
      label: "component.user.last-name",
      sortable: true
    },
    {
      key: "first_name",
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
  get user() {
    return this.$store.state.user;
  }
  get credentials() {
    return this.$store.state.credentials;
  }
  private filter: any = "";
  @Ref("tableRef") readonly tableRef!: any;
  currentURI = null;
  groupDetails = [];  
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

  refresh() {
    this.tableRef.refresh();
  }

  searchUsers(options) {
    let toReturn;
    return this.service
      .searchUsers(
        this.filter,
        options.orderBy,
        options.currentPage,
        options.pageSize
      )
      .then((http: any) => {
        let promises = [],
          promise;
        toReturn = http;
        toReturn.response.result.forEach((element, index) => {
          promise = this.service
            .getUserGroups(element.uri)
            .then((http2: any) => {
              element.groupDetails = http2.response.result;
            });
          promises.push(promise);
        });
        return Promise.all(promises).then(values => {
          return toReturn;
        });
      });
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
