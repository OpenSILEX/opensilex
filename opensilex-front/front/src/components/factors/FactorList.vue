<template>
  <div>
    <b-input-group class="mt-3 mb-3" size="sm">
      <b-input-group>
        <b-form-input
          v-model="filterByName"
          debounce="300"
          :placeholder="$t('component.factor.filter-placeholder')"
        ></b-form-input>
        <template v-slot:append>
          <b-btn :disabled="!filterByName" variant="primary" @click="filterByName = ''">
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
      <template v-slot:head(name)="data">{{$t(data.label)}}</template>
      <template v-slot:head(comment)="data">{{$t(data.label)}}</template>
      <template v-slot:head(uri)="data">{{$t(data.label)}}</template>
      <template v-slot:head(actions)="data">{{$t(data.label)}}</template>

      <template
        v-slot:cell(name)="data"
      >{{(data.item.name == null ? $t('component.common.not-specified').toString() : data.item.name) }}</template>
      <template v-slot:cell(uri)="data">
        <a href="#" class="uri-info" @click="$emit('onDetails', data.item.uri)">
          <font-awesome-icon icon="eye" size="sm" />
          {{ data.item.uri}}
        </a>
      </template>

      <!-- <span @click="$emit('onDetails', data.item.uri)"> -->
      <template v-slot:cell(actions)="data">
        <b-button-group size="sm">
          <!-- <b-button size="sm" @click="$emit('onDetails', data.item.uri)" variant="outline-success">
            
          </b-button>-->
          <b-button
           v-if="user.hasCredential(credentials.CREDENTIAL_FACTOR_MODIFICATION_ID)"
           size="sm" 
           @click="$emit('onEdit', data.item.uri)" 
           variant="outline-primary">
          <font-awesome-icon icon="edit" size="sm" />
          </b-button>
          <b-button
            v-if="user.hasCredential(credentials.CREDENTIAL_FACTOR_MODIFICATION_ID)"
            size="sm"
            @click="$emit('onInteroperability', data.item.uri)"
            variant="outline-info"
          >
          <font-awesome-icon icon="globe-americas" size="sm" />
          </b-button>
          <b-button
          v-if="user.hasCredential(credentials.CREDENTIAL_FACTOR_DELETE_ID)"
          size="sm" 
          @click="$emit('onDelete', data.item.uri)" 
          variant="danger">
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
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
import VueRouter from "vue-router";
import {
  FactorsService,
  FactorGetDTO,
  FactorSearchDTO
} from "opensilex-core/index";

import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";

@Component
export default class FactorList extends Vue {
  $opensilex: any;
  $store: any;
  $router: VueRouter;
  service: FactorsService;

  @Ref("tableRef") readonly tableRef!: any;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  mounted() {
    this.$store.watch(
      () => this.$store.getters.language,
      lang => {
        this.refresh();
      }
    );
  }
  currentPage: number = 1;
  pageSize = 20;
  totalRow = 0;
  sortBy = "name";
  sortDesc = false;

  private searchFrom: FactorSearchDTO = {
    uri: "",
    name: "",
    comment: ""
    // lang: "en-US"
  };

  set filterByName(value: string) {
    this.searchFrom.name = value;
    this.refresh();
  }

  get filterByName() {
    return this.searchFrom.name;
  }

  created() {
    this.service = this.$opensilex.getService("opensilex.FactorsService");
    let query: any = this.$route.query;
    if (query.filterByName) {
      this.searchFrom.name = decodeURI(query.filterByName);
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

  refresh() {
    if (this.tableRef != null || this.tableRef != undefined) {
      this.tableRef.refresh();
    }
  }

  loadData() {
    let orderBy: string[] = [];
    if (this.sortBy) {
      let orderByText = this.sortBy + "=";
      if (this.sortDesc) {
        orderBy.push(orderByText + "desc");
      } else {
        orderBy.push(orderByText + "asc");
      }
    }
    return this.service
      .searchFactors(
        orderBy,
        this.currentPage - 1,
        this.pageSize,
        this.searchFrom
      )
      .then((http: HttpResponse<OpenSilexResponse<Array<FactorGetDTO>>>) => {
        this.totalRow = http.response.metadata.pagination.totalCount;
        this.pageSize = http.response.metadata.pagination.pageSize;
        setTimeout(() => {
          this.currentPage = http.response.metadata.pagination.currentPage + 1;
        }, 0);

        this.$router
          .push({
            path: this.$route.fullPath,
            query: {
              filterByName: encodeURI(this.searchFrom.name),
              currentPage: "" + this.currentPage,
              pageSize: "" + this.pageSize
            }
          })
          .catch(err => {});
        return http.response.result;
      })
      .catch(this.$opensilex.errorHandler);
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
