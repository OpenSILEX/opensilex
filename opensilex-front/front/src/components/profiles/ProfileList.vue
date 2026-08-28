<template>
  <div>
    <opensilex-StringFilter
      :filter.sync="filter"
      @update="updateFilter()"
      placeholder="component.profile.filter-placeholder"
      :debounce="300"
      :lazy="false"
    ></opensilex-StringFilter>

    <opensilex-TableAsyncView ref="tableRef" :searchMethod="searchProfiles" :fields="fields">
      <template v-slot:cell(credentials)="{data}">
        <div>{{$tc("component.profile.credential", data.item.credentials.length, {count: data.item.credentials.length})}}</div>
      </template>

      <template v-slot:row-details="{data}">
        <strong class="capitalize-first-letter">{{$t("component.profile.credentials")}}:</strong>
        <b-card-group columns>
          <b-card
            v-for="credentialGroup in filterCredentialGroups(data.item.credentials)"
            v-bind:key="credentialGroup.group_id"
          >
            <strong>{{$t(credentialGroup.group_key_name)}}</strong>
            <ul>
              <li
                v-for="credential in credentialGroup.credentials"
                v-bind:key="credential.value"
              >{{credential.text}}</li>
            </ul>
          </b-card>
        </b-card-group>
      </template>

      <template v-slot:cell(name)="{data}">
        <opensilex-UriLink
          :uri="data.item.uri"
          :value="data.item.name"
          :noExternalLink="true"
          :isClickable="isClickable"
          @click="data.toggleDetails()"
        ></opensilex-UriLink>
      </template>

      <template v-slot:cell(actions)="{data}">
        <b-button-group size="sm">
          <opensilex-DetailButton
            @click="data.toggleDetails()"
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
            @click="deleteProfile(data.item.uri)"
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

@Component
export default class ProfileList extends Vue {
  $opensilex: any;
  $store: any;
  $route: any;

  @Prop()
  isClickable;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  credentialsGroups: any;

  filterCredentialGroups(credentialsFiltered) {
    let credentialsDetails = [];
    for (let i in this.credentialsGroups) {
      let credentialsGroup = this.credentialsGroups[i];

      let credentialsDetailGroup = {
        group_id: credentialsGroup.group_id,
        group_key_name: credentialsGroup.group_key_name,
        credentials: []
      };

      for (let j in credentialsGroup.credentials) {
        let credential = credentialsGroup.credentials[j];
        if (credentialsFiltered.indexOf(credential.id) >= 0) {
          credentialsDetailGroup.credentials.push({
            text: this.$t(credential.name),
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

  searchProfiles(options) {
    return this.$opensilex
      .getService("opensilex.SecurityService")
      .searchProfiles(
        this.filter,
        options.orderBy,
        options.currentPage,
        options.pageSize
      );
  }

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
