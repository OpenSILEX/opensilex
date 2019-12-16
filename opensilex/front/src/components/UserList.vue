<template>
  <div>
    <b-input-group class="mt-3 mb-3" size="sm">
      <b-form-input v-model="filterPattern" debounce="300" placeholder="Filter users"></b-form-input>
      <b-input-group-text slot="append">
        <b-btn
          class="p-0"
          :disabled="!filterPattern"
          variant="link"
          size="sm"
          @click="filterPattern = ''"
        >
          <i class="fa fa-remove"></i>
        </b-btn>
      </b-input-group-text>
    </b-input-group>
    <b-table ref="table" striped hover :items="dataProvider" :fields="fields"></b-table>
    <b-pagination
      v-model="currentPage"
      :total-rows="totalRow"
      :per-page="pageSize"
      :sort-by="sortBy"
      :sort-desc="sortDesc"
      @change="loadData"
    ></b-pagination>
    <b-button to="/users/create">Create</b-button>
  </div>
</template>

<script lang="ts">
import { Component } from "vue-property-decorator";
import Vue from "vue";
import { UsersService } from "../lib";
import HttpResponse from "../lib/HttpResponse";
import { UserGetDTO } from "../lib/model/userGetDTO";
import VueRouter from "vue-router";
import { OpenSilexResponse } from "opensilex/HttpResponse";

@Component
export default class UserList extends Vue {
  $opensilex: any;
  $store: any;
  $router: VueRouter;

  get user() {
    return this.$store.state.user;
  }

  currentPage: number = 1;
  pageSize = 20;
  totalRow = 0;
  sortBy = "firstName";
  sortDesc = false;

  private filterPatternValue: any = "";
  set filterPattern(value: string) {
    this.filterPatternValue = value;
    let tableRef: any = this.$refs.table;
    tableRef.refresh();
  }

  get filterPattern() {
    return this.filterPatternValue;
  }

  created() {
    let tableRef: any = this.$refs.table;
    let query: any = this.$route.query;
    if (query.filterPattern) {
      this.filterPatternValue = decodeURI(query.filterPattern);
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
      key: "firstName",
      sortable: true
    },
    {
      key: "lastName",
      sortable: true
    },
    {
      key: "email",
      sortable: true
    }
  ];

  dataProvider(ctx) {
    return this.loadData();
  }

  loadData() {
    let service: UsersService = this.$opensilex.getService(
      "opensilex.UsersService"
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

    return service
      .search(
        this.user.getToken(),
        this.filterPattern,
        orderBy,
        this.currentPage - 1,
        this.pageSize
      )
      .then((sucess: any) => {
        this.totalRow = sucess.response.metadata.totalCount;
        this.$router
          .push({
            path: this.$route.fullPath,
            query: {
              filterPattern: encodeURI(this.filterPattern),
              sortBy: encodeURI(this.sortBy),
              sortDesc: "" + this.sortDesc,
              currentPage: "" + this.currentPage,
              pageSize: "" + this.pageSize
            }
          })
          .catch(function() {});
        return sucess.response.result;
      })
      .catch(error => {
        console.error("Error while loading users", error);
        return [];
      });
  }
}
</script>

<style scoped lang="scss">
</style>
