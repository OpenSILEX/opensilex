<template>
  <div>
    <div class="card-vertical-group">
      <div class="card">
        <div class="card-header">
          <h3 class="mr-3">
            <i class="ik ik-search"></i>
            {{$t('component.project.filter-description')}}
          </h3>
        </div>
        <div class="card-body row">
          <div class="filter-group col col-xl-4 col-sm-6 col-12">
            <label>{{$t('component.project.filter-year')}}:</label>
            <div class="input-group input-group-button">
              <b-input-group>
                <b-form-input
                  id="input-live"
                  v-model="yearFilterPattern"
                  :placeholder="$t('component.project.filter-year-placeholder')"
                  trim
                ></b-form-input>
                <template v-slot:append>
                  <b-btn :disabled="!yearState" variant="primary" @click="yearFilterPattern = ''">
                    <font-awesome-icon icon="times" size="sm" />
                  </b-btn>
                </template>
              </b-input-group>
            </div>
          </div>

          <div class="filter-group col col-xl-4 col-sm-6 col-12">
            <label>{{$t('component.project.filter-label')}}:</label>
            <b-input-group>
              <b-form-input
                v-model="nameFilterPattern"
                debounce="1000"
                :placeholder="$t('component.project.filter-label-placeholder')"
              ></b-form-input>
              <template v-slot:append>
                <b-btn
                  :disabled="!nameFilterPattern"
                  variant="primary"
                  @click="nameFilterPattern = ''"
                >
                  <font-awesome-icon icon="times" size="sm" />
                </b-btn>
              </template>
            </b-input-group>
          </div>

          <div class="filter-group col col-xl-4 col-sm-6 col-12">
            <label>{{$t('component.project.filter-financial')}}</label>
            <b-input-group>
              <b-form-input
                v-model="financialFilterPattern"
                debounce="300"
                :placeholder="$t('component.project.filter-financial-placeholder')"
              ></b-form-input>
              <template v-slot:append>
                <b-btn
                  :disabled="!financialFilterPattern"
                  variant="primary"
                  @click="financialFilterPattern = ''"
                >
                  <font-awesome-icon icon="times" size="sm" />
                </b-btn>
              </template>
            </b-input-group>
          </div>
         
        </div>
      </div>
    </div>

    <div class="card-body p-0">
      <div class="table-responsive">
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
          <template v-slot:head(uri)="data">{{$t(data.label)}}</template>
          <template v-slot:head(shortname)="data">{{$t(data.label)}}</template>
          <template v-slot:head(label)="data">{{$t(data.label)}}</template>
          <template v-slot:head(startDate)="data">{{$t(data.label)}}</template>
          <template v-slot:head(endDate)="data">{{$t(data.label)}}</template>
          <template v-slot:head(hasFinancialFunding)="data">{{$t(data.label)}}</template>
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
              DESCRIPTION:
              <br />
              <div class="capitalize-first-letter">{{ data.item.description }}</div>
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
    </div>
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
  $i18n: any;

  get user() {
    return this.$store.state.user;
  }
  get credentials() {
    return this.$store.state.credentials;
  }

  pageSize = 20;
  currentPage: number = 1;
  totalRow = 0;
  sortBy = "uri";
  sortDesc = false;

  private yearFilterPatternValue: any = "";
  set yearFilterPattern(value: number) {
    this.yearFilterPatternValue = value;
    if (this.yearFilterPattern > 1000 && this.yearFilterPattern < 4000) {
      //the user enter a valid year
      this.refresh();
    }
    if (!this.yearFilterPattern) {
      this.refresh();
    }
  }
  get yearFilterPattern() {
    return this.yearFilterPatternValue;
  }
  get yearState() {
    return this.yearFilterPattern > 1000 && this.yearFilterPattern < 4000
      ? true
      : false;
  }

  private nameFilterPatternValue: any = "";
  set nameFilterPattern(value: string) {
    this.nameFilterPatternValue = value;
    this.refresh();
  }
  get nameFilterPattern() {
    return this.nameFilterPatternValue;
  }

  private financialFilterPatternValue: any = "";
  set financialFilterPattern(value: string) {
    this.financialFilterPatternValue = value;
    this.refresh();
  }
  get financialFilterPattern() {
    return this.financialFilterPatternValue;
  }

  created() {
    let query: any = this.$route.query;
    if (query.nameFilterPattern) {
      this.nameFilterPatternValue = decodeURI(query.nameFilterPattern);
    }
    if (query.yearFilterPattern) {
      this.yearFilterPatternValue = decodeURI(query.yearFilterPattern);
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
      label: "component.common.uri",
      sortable: true
    },
    {
      key: "shortname",
      label: "component.common.name",
      sortable: true
    },
    {
      key: "label",
      label: "component.project.longname",
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
      key: "hasFinancialFunding",
      label: "component.project.financialFunding",
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

    let startDateFilter: string;
    let endDateFilter: string;
    if (this.yearFilterPatternValue !== ""&&this.yearFilterPattern > 1000 && this.yearFilterPattern < 4000) {
      startDateFilter = this.yearFilterPatternValue.toString() + "-01-01";
      endDateFilter = this.yearFilterPatternValue.toString() + "-12-31";
    } else {
      startDateFilter = undefined;
      endDateFilter = undefined;
    }

    return service
      .searchProjects(
        startDateFilter,
        endDateFilter,
        this.nameFilterPattern,
        this.nameFilterPattern,
        this.financialFilterPattern,
        orderBy,
        this.currentPage - 1,
        this.pageSize
      )
      .then((http: HttpResponse<OpenSilexResponse<Array<ProjectGetDTO>>>) => {
        console.log(http);
        this.totalRow = http.response.metadata.pagination.totalCount;
        this.pageSize = http.response.metadata.pagination.pageSize;

        this.$router
          .push({
            path: this.$route.fullPath,
            query: {
              nameFilterPattern: this.nameFilterPattern
                ? encodeURI(this.nameFilterPattern)
                : null,
              yearFilterPattern: this.yearFilterPattern
                ? encodeURI(this.yearFilterPattern.toString())
                : null,
              sortBy: encodeURI(this.sortBy),
              sortDesc: "" + this.sortDesc
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
