<template>
  <div>
    <b-input-group class="mt-3 mb-3" size="sm">
      <b-input-group>
        <b-form-input
          v-model="filterPattern"
          debounce="300"
          :placeholder="$t('component.user.filter-placeholder')"
        ></b-form-input>
        <template v-slot:append>
          <b-btn :disabled="!filterPattern" variant="primary" @click="filterPattern = ''">
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
      <template v-slot:head(firstName)="data">{{$t(data.label)}}</template>
      <template v-slot:head(lastName)="data">{{$t(data.label)}}</template>
      <template v-slot:head(email)="data">{{$t(data.label)}}</template>
      <template v-slot:head(uri)="data">{{$t(data.label)}}</template>
      <template v-slot:head(admin)="data">{{$t(data.label)}}</template>
      <template v-slot:head(actions)="data">{{$t(data.label)}}</template>

      <template v-slot:cell(email)="data">
        <a :href="'mailto:' + data.item.email">{{ data.item.email }}</a>
      </template>

      <template v-slot:cell(uri)="data">
        <a class="uri-info">
          <small>{{ data.item.uri }}</small>
        </a>
      </template>

      <template v-slot:cell(admin)="data">
        <span class="capitalize-first-letter" v-if="data.item.admin">{{$t("component.common.yes")}}</span>
        <span class="capitalize-first-letter" v-if="!data.item.admin">{{$t("component.common.no")}}</span>
      </template>

      <template v-slot:cell(actions)="data">
        <b-button-group>
          <b-button
            size="sm"
            v-if="user.hasCredential(credentials.CREDENTIAL_USER_MODIFICATION_ID)"
            @click="$emit('onEdit', data.item)"
            variant="outline-primary"
          >
            <font-awesome-icon icon="edit" size="sm" />
          </b-button>
          <b-button
            size="sm"
            v-if="user.hasCredential(credentials.CREDENTIAL_USER_DELETE_ID) && user.email != data.item.email"
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
import { UsersService, UserGetDTO } from "opensilex-rest/index";
import HttpResponse, { OpenSilexResponse } from "opensilex-rest/HttpResponse";

@Component
export default class UserList extends Vue {
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
  sortBy = "firstName";
  sortDesc = false;

  private filterPatternValue: any = "";
  set filterPattern(value: string) {
    this.filterPatternValue = value;
    this.refresh();
  }

  get filterPattern() {
    return this.filterPatternValue;
  }

  created() {
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
      label: "component.user.first-name",
      sortable: true
    },
    {
      key: "lastName",
      label: "component.user.last-name",
      sortable: true
    },
    {
      key: "email",
      label: "component.user.email",
      sortable: true
    },
    {
      key: "uri",
      label: "component.common.uri",
      sortable: true
    },
    {
      key: "admin",
      label: "component.user.admin",
      sortable: true
    },
    {
      label: "component.common.actions",
      key: "actions"
    }
  ];

  refresh() {
    let tableRef: any = this.$refs.tableRef;
    tableRef.refresh();
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
      .searchUsers(
        this.user.getAuthorizationHeader(),
        this.filterPattern,
        orderBy,
        this.currentPage - 1,
        this.pageSize
      )
      .then((http: HttpResponse<OpenSilexResponse<Array<UserGetDTO>>>) => {
        this.totalRow = http.response.metadata.pagination.totalCount;
        this.pageSize = http.response.metadata.pagination.pageSize;
        setTimeout(() => {
          this.currentPage = http.response.metadata.pagination.currentPage + 1;
        }, 0);

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
