<template>
  <div>
    <opensilex-StringFilter
      :filter.sync="filterPattern"
      placeholder="component.profile.filter-placeholder"
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
      <template v-slot:head(credentials)="data">{{$t(data.label)}}</template>
      <template v-slot:head(actions)="data">{{$t(data.label)}}</template>

      <template v-slot:cell(credentials)="data">
        <div>{{$tc("component.profile.credential", data.item.credentials.length, {count: data.item.credentials.length})}}</div>
      </template>

      <template v-slot:row-details="{data}">
        <strong class="capitalize-first-letter">{{$t("component.profile.credentials")}}:</strong>
        <b-card-group columns>
          <b-card
            v-for="credentialGroup in filterCredentialGroups(data.item.credentials)"
            v-bind:key="credentialGroup.groupId"
          >
            <strong>{{$t(credentialGroup.groupKeyLabel)}}</strong>
            <ul>
              <li
                v-for="credential in credentialGroup.credentials"
                v-bind:key="credential.value"
              >{{credential.text}}</li>
            </ul>
          </b-card>
        </b-card-group>
      </template>

      <template v-slot:cell(uri)="data">
        <opensilex-UriLink :uri="data.item.uri"></opensilex-UriLink>
      </template>

      <template v-slot:cell(actions)="{data}">
        <b-button-group size="sm">
          <opensilex-DetailButton
            @click="data.toggleDetails"
            label="component.profile.details"
            :detailVisible="data.detailsShowing"
            :small="true"
          ></opensilex-DetailButton>
          <opensilex-EditButton
            v-if="user.hasCredential(credentials.CREDENTIAL_PROFILE_MODIFICATION_ID)"
            @click="$emit('onEdit', data.item)"
            label="component.profile.update"
            :small="true"
          ></opensilex-EditButton>
          <opensilex-DeleteButton
            v-if="user.hasCredential(credentials.CREDENTIAL_PROFILE_DELETE_ID)"
            @click="$emit('onDelete', data.item.uri)"
            label="component.profile.delete"
            :small="true"
          ></opensilex-DeleteButton>
        </b-button-group>
      </template>
    </opensilex-TableAsyncView>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
import VueRouter from "vue-router";
import { SecurityService, ProfileGetDTO } from "opensilex-security/index";
import HttpResponse, {
  OpenSilexResponse
} from "opensilex-security/HttpResponse";

@Component
export default class ProfileList extends Vue {
  $opensilex: any;

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

  @Prop()
  credentialsGroups: any;

  filterCredentialGroups(credentialsFiltered) {
    let credentialsDetails = [];
    for (let i in this.credentialsGroups) {
      let credentialsGroup = this.credentialsGroups[i];

      let credentialsDetailGroup = {
        groupId: credentialsGroup.groupId,
        groupKeyLabel: credentialsGroup.groupKeyLabel,
        credentials: []
      };

      for (let j in credentialsGroup.credentials) {
        let credential = credentialsGroup.credentials[j];
        if (credentialsFiltered.indexOf(credential.id) >= 0) {
          credentialsDetailGroup.credentials.push({
            text: this.$t(credential.label),
            value: credential.id
          });
        }
      }

      if (credentialsDetailGroup.credentials.length > 0) {
        credentialsDetails.push(credentialsDetailGroup);
      }
    }
    return credentialsDetails;
  }

  private filter: any = "";

  created() {
    this.$opensilex.getCredentials().then(credentials => {
      this.credentialsGroups = credentials;
    });

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
      label: "component.profile.credentials",
      key: "credentials"
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
      .searchProfiles(
        this.filterPattern,
        orderBy,
        this.currentPage - 1,
        this.pageSize
      )
      .then((http: HttpResponse<OpenSilexResponse<Array<ProfileGetDTO>>>) => {
        this.totalRow = http.response.metadata.pagination.totalCount;
        this.pageSize = http.response.metadata.pagination.pageSize;
        setTimeout(() => {
          this.currentPage = http.response.metadata.pagination.currentPage + 1;
        }, 0);

        this.isSearching = false;
        this.$opensilex.enableLoader();

  deleteProfile(uri: string) {
    this.$opensilex
      .getService("opensilex.SecurityService")
      .deleteProfile(uri)
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
