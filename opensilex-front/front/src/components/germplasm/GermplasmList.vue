<template>
  <div>
    <b-input-group class="mt-3 mb-3" size="sm">
      <b-input-group>
        <b-form-input
          v-model="filterByAlias"
          debounce="300"
          :placeholder="$t('component.germplasm.filter-placeholder')"
        ></b-form-input>
        <template v-slot:append>
          <b-btn :disabled="!filterByAlias" variant="primary" @click="filterByAlias = ''">
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
      <template v-slot:head(rdfType)="data">{{$t(data.label)}}</template>
      <template v-slot:head(label)="data">{{$t(data.label)}}</template>
      <template v-slot:head(fromSpecies)="data">{{$t(data.label)}}</template>
      <template v-slot:head(fromVariety)="data">{{$t(data.label)}}</template>
      <template v-slot:head(fromAccession)="data">{{$t(data.label)}}</template>
      <template v-slot:head(uri)="data">{{$t(data.label)}}</template>
       <template v-slot:cell(actions)="data">
        <b-button-group size="sm">
          <b-button
            size="sm"
            @click="$emit('onEdit', data.item)"
            variant="outline-primary"
          >
            <font-awesome-icon icon="edit" size="sm" />
          </b-button>
          <b-button
            size="sm"
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
import { Component } from "vue-property-decorator";
import Vue from "vue";
import VueRouter from "vue-router";
import { 
  GermplasmService,
  GermplasmGetDTO
} 
from "opensilex-core/index";

import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";

@Component
export default class GermplasmList extends Vue {
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
  sortBy = "label";
  sortDesc = false;

  private searchFrom: GermplasmGetDTO = {
  };


  created() {
    let query: any = this.$route.query;

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
      key: "rdfType",
      label: "component.germplasm.rdfType",
      sortable: true
    },
    {
      key: "label",
      label: "component.germplasm.label",
      sortable: true
    },
    {
      key: "fromSpecies",
      label: "component.germplasm.fromSpecies",
      sortable: false
    },
    {
      key: "actions",
      label: "component.common.actions"
    }
  ];

  refresh() {
    let tableRef: any = this.$refs.tableRef;
    tableRef.refresh();
  }

  loadData() {
    let service: GermplasmService = this.$opensilex.getService(
      "opensilex.GermplasmService"
    );

    let GermplasmSearchDTO : GermplasmGetDTO;

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
      .searchGermplasm(
        orderBy,
        this.currentPage - 1,
        this.pageSize,
        this.searchFrom
        
      )
      .then((http: HttpResponse<OpenSilexResponse<Array<GermplasmGetDTO>>>) => {
        this.totalRow = http.response.metadata.pagination.totalCount;
        this.pageSize = http.response.metadata.pagination.pageSize;
        setTimeout(() => {
          this.currentPage = http.response.metadata.pagination.currentPage + 1;
        }, 0);

        this.$router
          .push({
            path: this.$route.fullPath,
            query: {
              currentPage: "" + this.currentPage,
              pageSize: "" + this.pageSize
            }
          })
          .catch(err => {})

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
