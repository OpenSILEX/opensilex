<template>
  <div>
    <opensilex-SearchFilterField
      @search="updateFilter()"
      @clear="clearFilters()"
      label="component.factor.filter.label"
    >
      <template v-slot:filters>
        <b-row class="ml-2">
          <opensilex-FilterField>
            <template v-slot>
              <opensilex-StringFilter
                label="component.factor.filter.name"
                :filter.sync="filter.name"
                placeholder="component.factor.filter.name-placeholder"
              ></opensilex-StringFilter>
            </template>
          </opensilex-FilterField>
          <opensilex-FilterField>
            <template v-slot>
              <opensilex-StringFilter
                :disabled="true"
                label="component.factor.filter.experiment"
                :filter.sync="filter.experiment"
                placeholder="component.factor.filter.experiment-placeholder"
              ></opensilex-StringFilter>
            </template>
          </opensilex-FilterField>
        </b-row>
      </template>
    </opensilex-SearchFilterField>

    <opensilex-TableAsyncView
      ref="tableRef"
      :searchMethod="searchFactors"
      :fields="fields"
      defaultSortBy="name"
    >
      <template v-slot:head(name)="{data}">{{$t(data.label)}}</template>
      <template v-slot:head(comment)="{data}">{{$t(data.label)}}</template>
      <template v-slot:head(uri)="{data}">{{$t(data.label)}}</template>
      <template v-slot:head(actions)="{data}">{{$t(data.label)}}</template>
      <template v-slot:cell(uri)="{data}">
        <opensilex-UriLink
          :uri="data.item.uri"
          :to="{path: '/factor/'+ encodeURIComponent(data.item.uri)}"
        ></opensilex-UriLink>
      </template>
      <template v-slot:cell(actions)="{data}">
        <b-button-group size="sm">
          <opensilex-EditButton
            v-if="user.hasCredential(credentials.CREDENTIAL_FACTOR_MODIFICATION_ID)"
            :small="true"
            label="component.factor.update"
            @click="$emit('onEdit', data.item.uri)"
          ></opensilex-EditButton>
          <opensilex-InteroperabilityButton
            v-if="user.hasCredential(credentials.CREDENTIAL_FACTOR_MODIFICATION_ID)"
            :small="true"
            label="component.factor.interoperrability"
            @click="$emit('onInteroperability', data.item.uri)"
          ></opensilex-InteroperabilityButton>
          <opensilex-DeleteButton
            v-if="user.hasCredential(credentials.CREDENTIAL_FACTOR_DELETE_ID)"
            :small="true"
            label="component.factor.delete"
            @click="$emit('onDelete', data.item.uri)"
          ></opensilex-DeleteButton>
        </b-button-group>
      </template>
    </opensilex-TableAsyncView>
  </div>
</template>

<script lang="ts">
import { Component, Ref, Prop } from "vue-property-decorator";
import Vue from "vue";
import VueRouter from "vue-router";
import {
  FactorsService,
  FactorGetDTO,
  FactorSearchDTO
} from "opensilex-core/index";
import HttpResponse, {
  OpenSilexResponse
} from "opensilex-security/HttpResponse";

@Component
export default class FactorList extends Vue {
  $opensilex: any;
  service: FactorsService;
  $store: any;
  $route: any;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  filter: FactorSearchDTO = {
    uri: "",
    name: "",
    comment: "",
    experiment: ""
  };

  clearFilters() {
    this.filter = {
      uri: "",
      name: "",
      comment: ""
      // experiment: ""
    };
  }

  onDetails(uri) {
    this.$emit("onDetails", uri);
  }

  created() {
    this.service = this.$opensilex.getService("opensilex.FactorsService");
    let query: any = this.$route.query;

    for (let [key, value] of Object.entries(this.filter)) {
      if (query[key]) {
        this.filter[key] = decodeURI(query[key]);
      }
    }
  }

  updateFilter(value: string) {
    for (let [key, value] of Object.entries(this.filter)) {
      this.$opensilex.updateURLParameter(key, value, "");
    }
    this.refresh();
  }

  fields = [
    {
      key: "uri",
      label: "component.factor.uri",
      sortable: true
    },
    {
      key: "name",
      label: "component.factor.name",
      sortable: true
    },
    {
      key: "comment",
      label: "component.factor.comment",
      sortable: false
    },
    {
      key: "actions",
      label: "component.common.actions"
    }
  ];
  @Ref("tableRef") readonly tableRef!: any;

  refresh() {
    this.tableRef.refresh();
  }

  searchFactors(options) {
    return this.service.searchFactors(
      options.orderBy,
      options.currentPage,
      options.pageSize,
      this.filter
    );
  }
}
</script>

<style scoped lang="scss">
a {
  color: #007bff;
}
.uri-info {
  text-overflow: ellipsis;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: inline-block;
  max-width: 300px;
}
</style>
