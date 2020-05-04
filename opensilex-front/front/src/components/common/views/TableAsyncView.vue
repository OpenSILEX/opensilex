<template>
  <opensilex-Overlay :show="isSearching">
    <b-table
      ref="tableRef"
      striped
      hover
      small
      responsive
      primary-key="uri"
      :busy="isSearching"
      :sort-by.sync="sortBy"
      :sort-desc.sync="sortDesc"
      :items="loadData"
      :fields="fields"
    >
      <template v-for="(field, index) in fields" v-slot:[getHeadTemplateName(field.key)]="data">
        <span :key="index">{{$t(data.label)}}</span>
      </template>

      <template v-for="(field, index) in fields" v-slot:[getCellTemplateName(field.key)]="data">
        <span :key="index">
          <slot :name="getCellTemplateName(field.key)" v-bind:data="data">{{data.item[field.key]}}</slot>
        </span>
      </template>

      <template v-slot:row-details="data">
        <slot name="row-details" v-bind:data="data"></slot>
      </template>
    </b-table>
    <b-pagination
      v-model="currentPage"
      :total-rows="totalRow"
      :per-page="pageSize"
      @change="tableRef.refresh()"
    ></b-pagination>
  </opensilex-Overlay>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../../lib/HttpResponse";

@Component
export default class TableAsyncView extends Vue {
  $opensilex: any;

  @Ref("tableRef") readonly tableRef!: any;

  @Prop()
  fields;

  @Prop()
  searchMethod;

  @Prop({
    default: true
  })
  useQueryParams;

  @Prop({
    default: "name"
  })
  defaultSortBy;

  @Prop({
    default: false
  })
  defaultSortDesc;

  @Prop({
    default: 20
  })
  defaultPageSize;

  currentPage: number = 1;
  pageSize: number;
  totalRow = 0;
  sortBy;
  sortDesc = false;
  isSearching = false;

  created() {
    if (this.useQueryParams) {
      let query: any = this.$route.query;

      if (query.pageSize) {
        this.pageSize = parseInt(query.pageSize) || this.defaultPageSize;
      } else {
        this.pageSize = this.defaultPageSize;
      }

      if (query.sortBy) {
        this.sortBy = decodeURI(query.sortBy) || this.defaultSortBy;
      } else {
        this.sortBy = this.defaultSortBy;
      }

      if (query.sortDesc) {
        this.sortDesc = query.sortDesc == "true";
      } else {
        this.sortDesc = this.defaultSortDesc;
      }
    }
  }

  getHeadTemplateName(key) {
    return "head(" + key + ")";
  }

  getCellTemplateName(key) {
    return "cell(" + key + ")";
  }

  refresh() {
    this.currentPage = 1;
    this.tableRef.refresh();
  }

  loadData() {
    let orderBy = [];
    if (this.sortBy) {
      let orderByText = this.sortBy + "=";
      if (this.sortDesc) {
        orderBy.push(orderByText + "desc");
      } else {
        orderBy.push(orderByText + "asc");
      }
    }

    if (this.useQueryParams) {
      this.$opensilex.updateURLParameter(
        "sortBy",
        this.sortBy,
        this.defaultSortBy
      );
      this.$opensilex.updateURLParameter(
        "sortDesc",
        this.sortDesc,
        this.defaultSortDesc
      );
      this.$opensilex.updateURLParameter(
        "pageSize",
        this.pageSize,
        this.pageSize
      );
    }

    this.$opensilex.disableLoader();
    this.isSearching = true;

    return this.searchMethod({
      orderBy: orderBy,
      currentPage: this.currentPage - 1,
      pageSize: this.pageSize
    })
      .then((http: HttpResponse<OpenSilexResponse<Array<any>>>) => {
        this.totalRow = http.response.metadata.pagination.totalCount;
        this.pageSize = http.response.metadata.pagination.pageSize;

        this.isSearching = false;
        this.$opensilex.enableLoader();

        return http.response.result;
      })
      .catch(error => {
        this.isSearching = false;
        this.$opensilex.errorHandler(error);
      });
  }
}
</script>

<style scoped lang="scss">
</style>

