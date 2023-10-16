<template>
  <div>
    <opensilex-StringFilter
      :filter.sync="filter"
      @update="updateFilter()"
      placeholder="component.group.filter-placeholder"
      :debounce="300"
      :lazy="false"
    ></opensilex-StringFilter>

    <opensilex-TableAsyncView ref="tableRef" :searchMethod="searchGroups" :fields="fields">
      <template v-slot:cell(name)="{data}">
        <opensilex-UriLink
          :uri="data.item.uri"
          :value="data.item.name"
          :noExternalLink="true"
          :isClickable="false"
        ></opensilex-UriLink>
      </template>

      <template v-slot:cell(user_profiles)="{data}">
        <div>{{$tc("component.account.label", data.item.user_profiles.length, {count: data.item.user_profiles.length})}}</div>
      </template>

      <template v-slot:row-details="{data}">
        <strong class="capitalize-first-letter">{{$t("component.account.accounts")}}:</strong>
        <ul>
          <li
            v-for="userProfile in data.item.user_profiles"
            v-bind:key="userProfile.uri"
          >{{userProfile.user_name}} ({{userProfile.profile_name}})</li>
        </ul>
      </template>

      <template v-slot:cell(actions)="{data}">
        <b-button-group size="sm">
          <opensilex-DetailButton
            @click="data.toggleDetails()"
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
            @click="deleteGroup(data.item.uri)"
            label="component.group.delete"
            :small="true"
          ></opensilex-DeleteButton>
        </b-button-group>
      </template>
    </opensilex-TableAsyncView>
  </div>
</template>

<script lang="ts">
import { Component, Ref, Prop } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class GroupList extends Vue {
  $opensilex: any;
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
  }

  updateFilter() {
    this.$opensilex.updateURLParameter("filter", this.filter, "");
    this.refresh();
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
      label: "component.account.accounts",
      key: "user_profiles"
    },
    {
      label: "component.common.actions",
      key: "actions",
      class: "table-actions"
    }
  ];

  @Ref("tableRef") readonly tableRef!: any;

  refresh() {
    this.$opensilex.updateURLParameters(this.filter);
    this.tableRef.changeCurrentPage(1);
  }

  updateSelectedGroup(){
    this.$opensilex.updateURLParameters(this.filter);
  }

  searchGroups(options) {
    return this.$opensilex
      .getService("opensilex.SecurityService")
      .searchGroups(
        this.filter,
        options.orderBy,
        options.currentPage,
        options.pageSize
      );
  }

  deleteGroup(uri: string) {
    this.$opensilex
      .getService("opensilex.SecurityService")
      .deleteGroup(uri)
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
