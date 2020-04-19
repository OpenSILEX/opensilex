<template>
  <div>
    <b-input-group class="mt-3 mb-3" size="sm">
      <b-input-group>
        <b-form-input
          v-model="filterPattern"
          debounce="300"
          :placeholder="$t('component.project.filter-placeholder')"
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
      <template v-slot:head(shortname)="data">{{$t(data.label)}}</template>
      <template v-slot:head(label)="data">{{$t(data.label)}}</template>
      <template v-slot:head(objective)="data">{{$t(data.label)}}</template>
      <template v-slot:head(hasFinancialFunding)="data">{{$t(data.label)}}</template>
      <template v-slot:head(startDate)="data">{{$t(data.label)}}</template>
      <template v-slot:head(endDate)="data">{{$t(data.label)}}</template>
      <template v-slot:head(homePage)="data">{{$t(data.label)}}</template>
      <template v-slot:head(uri)="data">{{$t(data.label)}}</template>
      <template v-slot:head(actions)="data">{{$t(data.label)}}</template>

      <template v-slot:cell(uri)="data">
        <a class="uri-info">
          <small>{{ data.item.uri }}</small>
        </a>
      </template>

      <template v-slot:cell(startDate)="data">{{ format(data.item.startDate)}}</template>

      <template v-slot:cell(endDate)="data">{{ format(data.item.endDate)}}</template>

      <template v-slot:row-details="data">
        <div v-if="data.item.description">
          DESCRIPTION: <strong class="capitalize-first-letter">{{ data.item.description }}</strong>
        </div>
        
       <div v-if="data.item.coordinators">
          Coordinators :
          <b-badge
            v-for="(item, index) in data.item.coordinators"
            :key="index"
            pill
            variant="info"
          >{{item}}</b-badge>
        </div>
        <div v-if="data.item.scientificContacts">
          Scientific contact :
          <b-badge
            v-for="(item, index) in data.item.scientificContacts"
            :key="index"
            pill
            variant="info"
          >{{item}}</b-badge>
        </div>
        <div v-if="data.item.administrativeContacts">
          Administrative contact :
          <b-badge
            v-for="(item, index) in data.item.administrativeContacts"
            :key="index"
            pill
            variant="info"
          >{{item}}</b-badge>
        </div>
      </template>

      <template v-slot:cell(actions)="data">
        <b-button-group>
          <b-button size="sm" @click="data.toggleDetails" variant="outline-success">
            <font-awesome-icon v-if="!data.detailsShowing" icon="eye" size="sm" />
            <font-awesome-icon v-if="data.detailsShowing" icon="eye-slash" size="sm" />
          </b-button>

          <b-button size="sm" @click="$emit('onEdit', data.item)" variant="outline-primary">
            <font-awesome-icon icon="edit" size="sm" />
          </b-button>

          <b-button size="sm" @click="$emit('onDelete', data.item.uri)" variant="danger">
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
import { ProjectGetDTO, ProjectsService } from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "../../lib//HttpResponse";

@Component
export default class ProjectTable extends Vue {
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
  sortBy = "uri";
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
      key: "shortname",
      label: "component.common.acronym",
      sortable: true
    },
    {
      key: "label",
      label: "component.common.name",
      sortable: true
    }, 
    {
      key: "objective",
      label: "component.project.objective",
      sortable: true
    },
    {
      key: "hasFinancialFunding",
      label: "component.project.financialFunding",
      sortable: true
    },
    {
      key: "startDate",
      label: "component.common.startDate",
      sortable: true
    },
    {
      key: "endDate",
      label: "component.common.endDate",
      sortable: true
    },
    {
      key: "uri",
      label: "component.common.uri",
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
    let service: ProjectsService = this.$opensilex.getService(
      "opensilex.ProjectsService"
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
      .searchProjects(
        undefined,
        undefined,
        undefined,
        this.filterPattern,
        undefined,
        undefined,
        undefined,
        this.currentPage - 1,
        this.pageSize
      )
      .then((http: HttpResponse<OpenSilexResponse<Array<ProjectGetDTO>>>) => {
        console.log(http);
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

  format(date) {
    if (date) {
      var d = new Date(date),
        month = "" + (d.getMonth() + 1),
        day = "" + d.getDate(),
        year = d.getFullYear();

      if (month.length < 2) month = "0" + month;
      if (day.length < 2) day = "0" + day;

      return [day, month, year].join("-");
    } else {
      return this.$i18n.t("component.project.inProgress");
    }
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
