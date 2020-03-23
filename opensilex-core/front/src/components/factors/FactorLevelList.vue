<template>
  <div>
    <b-input-group class="mt-3 mb-3" size="sm">
      <b-input-group>
        <b-form-input
          v-model="filterByAlias"
          debounce="300"
          :placeholder="$t('component.factorLevel.filter-placeholder')"
        ></b-form-input>
        <template v-slot:append>
          <b-btn :disabled="!filterByAlias" variant="primary" @click="filterByAlias = ''">
            <font-awesome-icon icon="times" size="sm" />
          </b-btn>
        </template>
      </b-input-group>
      <b-input-group>
          <b-form-select v-model="factors" :options="options" size="sm" class="mt-3">
            <template v-slot:first>
              <b-form-select-option value="" disabled>-- {{$t('component.factorLevel.factorLevel-factor-selector-placeholder')}} --</b-form-select-option>
            </template>
          </b-form-select>
          <div class="mt-3">Selected: <strong>{{ selected }}</strong></div>
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
      <template v-slot:head(alias)="data">{{$t(data.label)}}</template>
      <template v-slot:head(comment)="data">{{$t(data.label)}}</template>
      <template v-slot:head(uri)="data">{{$t(data.label)}}</template>
       <template v-slot:cell(actions)="data">
        <b-button-group>
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
import { FactorLevelsService } from "../../lib/api/factorLevels.service";
import { FactorLevelGetDTO } from "../../lib/model/factorLevelGetDTO";
import { FactorLevelSearchDTO } from "../../lib/model/factorLevelSearchDTO";


import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";

@Component
export default class FactorLevelList extends Vue {
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
  sortBy = "alias";
  sortDesc = false;

  private searchFrom: FactorLevelSearchDTO = {
    uri: "",
    alias: "",
    comment:"",
     // lang: "en-US"
  };
  
  set filterByAlias(value: string) {
    this.searchFrom.alias = value;
    this.refresh();
  }

  get filterByAlias() {
    return this.searchFrom.alias;
  }

  created() {
    let query: any = this.$route.query;
    if (query.filterByAlias) {
      this.searchFrom.alias = decodeURI(query.filterByAlias);
    }
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
      key: "alias",
      label: "component.factorLevel.alias",
      sortable: true
    },
    {
      key: "comment",
      label: "component.factorLevel.comment",
      sortable: false
    },
    {
      key: "factor",
      label: "component.factorLevel.factor",
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
    let service: FactorLevelsService = this.$opensilex.getService(
      "opensilex.FactorLevelsService"
    );
  
    let orderBy : string[] = [];
    if (this.sortBy) {
      let orderByText = this.sortBy + "=";
      if (this.sortDesc) {
        orderBy.push(orderByText + "desc");
      } else {
        orderBy.push(orderByText + "asc");
      }
    }

    return service
      .searchFactorLevels(
        this.user.getAuthorizationHeader(),
        orderBy,
        this.currentPage - 1,
        this.pageSize,
        this.searchFrom
      )
      .then((http: HttpResponse<OpenSilexResponse<Array<FactorLevelGetDTO>>>) => {
        this.totalRow = http.response.metadata.pagination.totalCount;
        this.pageSize = http.response.metadata.pagination.pageSize;
        setTimeout(() => {
          this.currentPage = http.response.metadata.pagination.currentPage + 1;
        }, 0);

        this.$router
          .push({
            path: this.$route.fullPath,
            query: {
              filterByAlias: encodeURI(this.searchFrom.alias),
              currentPage: "" + this.currentPage,
              pageSize: "" + this.pageSize
            }
          })
          .catch(function() {});

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
