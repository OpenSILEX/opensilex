<template>
  <div>
    <opensilex-StringFilter
        :filter.sync="filter"
        @update="updateFilter()"
        placeholder="component.person.filter-placeholder"
    ></opensilex-StringFilter>

    <opensilex-TableAsyncView
        ref="tableRef"
        :searchMethod="searchPersons"
        :fields="fields"
        defaultSortBy="email"
    >

      <template v-slot:cell(email)="{data}">
        <a :href="'mailto:' + data.item.email">{{ data.item.email }}</a>
      </template>

      <template v-slot:cell(actions)="{data}">
        <b-button-group size="sm">
          <opensilex-EditButton
              v-if="person.hasCredential(credentials.CREDENTIAL_PERSON_MODIFICATION_ID)"
              @click="$emit('onEdit', data.item)"
              label="component.person.update"
              :small="true"
          ></opensilex-EditButton>
          <opensilex-DeleteButton
              v-if="person.hasCredential(credentials.CREDENTIAL_PERSON_DELETE_ID)"
              @click="deletePerson(data.item.uri)"
              label="component.person.delete"
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
import { SecurityService } from "opensilex-security/index";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {OpenSilexStore} from "../../models/Store";
import {Route} from "vue-router";

@Component
export default class PersonList extends Vue {
  $opensilex: OpenSilexVuePlugin;
  service: SecurityService;
  $store: OpenSilexStore;
  $route: Route;
  fields = [
    {
      key: "last_name",
      label: "component.person.last-name",
      sortable: true
    },
    {
      key: "first_name",
      label: "component.person.first-name",
      sortable: true
    },
    {
      key: "email",
      label: "component.person.email",
      sortable: true
    },
    {
      label: "component.common.actions",
      key: "actions",
      class: "table-actions"
    }
  ];
  get person() {
    return this.$store.state.user;
  }
  get credentials() {
    return this.$store.state.credentials;
  }
  private filter: any = "";
  @Ref("tableRef") readonly tableRef!: any;
  currentURI = null;
  created() {
    let query: any = this.$route.query;
    if (query.filter) {
      this.filter = decodeURIComponent(query.filter);
    }
    this.service = this.$opensilex.getService("opensilex.SecurityService");
  }

  refresh() {
    this.tableRef.refresh();
  }

  searchPersons(options) {
    return this.service
        .searchPersons(
            this.filter,
            false,
            options.orderBy,
            options.currentPage,
            options.pageSize
        )
  }

  updateFilter() {
    this.$opensilex.updateURLParameter("filter", this.filter, "");
    this.refresh();
  }

  deletePerson(uri: string) {
    this.service
        .deletePerson(uri)
        .then(() => {
          this.refresh();
          this.$opensilex.showSuccessToast(this.$t('component.person.successDelete').toString())
        })
        .catch(this.$opensilex.errorHandler);
  }
}
</script>

<style scoped lang="scss">
</style>