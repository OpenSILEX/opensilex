<template>
  <div>
    <opensilex-SearchFilterField @clear="resetFilters()" @search="updateFilters()">
      <template v-slot:filters>


        <div class="col col-xl-3 col-sm-6 col-12">
          <label>{{$t('component.common.keyword')}}</label>
          <opensilex-StringFilter
            :filter.sync="termFilter"
            placeholder="component.project.filter-keywords-placeholder"
          ></opensilex-StringFilter>
        </div>
        
        <div class="col col-xl-3 col-sm-6 col-12">
          <label>{{$t('component.common.name')}}</label>
          <opensilex-StringFilter
            :filter.sync="nameFilter"
            placeholder="component.project.filter-label-placeholder"
          ></opensilex-StringFilter>
        </div>


        <div class="col col-xl-3 col-sm-6 col-12">
          <label>{{$t('component.common.year')}}</label>
          <opensilex-StringFilter
            :filter.sync="yearFilter"
            type="number"
            placeholder="component.project.filter-year-placeholder"
          ></opensilex-StringFilter>
        </div>

        <div class="col col-xl-3 col-sm-6 col-12">
          <label>{{$t('component.project.financialFunding')}}</label>
          <opensilex-StringFilter
            :filter.sync="financialFilter"
            placeholder="component.project.filter-financial-placeholder"
          ></opensilex-StringFilter>
        </div>
      </template>
    </opensilex-SearchFilterField>
    <opensilex-TableAsyncView
      ref="tableRef"
      :searchMethod="loadData"
      :fields="fields"
      defaultSortBy="startDate"
    >
      <template v-slot:cell(name)="{data}">
        <opensilex-UriLink
          :uri="data.item.uri"
          :value="data.item.name"
          :to="{path: '/project/details/'+ encodeURIComponent(data.item.uri),query: { name: data.item.name }}"
        ></opensilex-UriLink>
      </template>

      <template v-slot:cell(startDate)="{data}">
        <opensilex-DateView :value="data.item.startDate"></opensilex-DateView>
      </template>

      <template v-slot:cell(endDate)="{data}">
        <opensilex-DateView :value="data.item.endDate"></opensilex-DateView>
      </template>

      <template v-slot:cell(state)="{data}">
        <i
          v-if="!isEnded(data.item)"
          class="ik ik-activity badge-icon badge-info-opensilex"
          :title="$t('component.project.common.status.in-progress')"
        ></i>
        <i
          v-else
          class="ik ik-archive badge-icon badge-light"
          :title="$t('component.project.common.status.finished')"
        ></i>
      </template>

      <template v-slot:cell(actions)="{data}">
        <b-button-group size="sm">
          <opensilex-EditButton
            v-if="user.hasCredential(credentials.CREDENTIAL_PROJECT_MODIFICATION_ID)"
            @click="$emit('onEdit', data.item)"
            label="component.project.update"
            :small="true"
          ></opensilex-EditButton>

          <opensilex-DeleteButton
            v-if="user.hasCredential(credentials.CREDENTIAL_PROJECT_DELETE_ID)"
            @click="deleteUser(data.item.uri)"
            label="component.project.delete"
            :small="true"
          ></opensilex-DeleteButton>
        </b-button-group>
      </template>
    </opensilex-TableAsyncView>
  </div>
</template>

<script lang="ts">
import moment from "moment";
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
import VueRouter from "vue-router";
import { ProjectGetDTO, ProjectsService } from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "../../lib//HttpResponse";

@Component
export default class ProjectList extends Vue {
  $opensilex: any;
  service: ProjectsService;
  get user() {
    return this.$store.state.user;
  }
  get credentials() {
    return this.$store.state.credentials;
  }

  private yearFilter: any = "";
  updateYearFilter() {
    this.$opensilex.updateURLParameter("year", this.yearFilter, "");
  }
  private nameFilter: any = "";
  updateNameFilter() {
    this.$opensilex.updateURLParameter("name", this.nameFilter, "");
  }
  private termFilter: any = "";
  updateTermFilter() {
    this.$opensilex.updateURLParameter("term", this.termFilter, "");
  }
  private financialFilter: any = "";
  updateFinancialFilter() {
    this.$opensilex.updateURLParameter("financial", this.financialFilter, "");
  }

  updateFilters() {
    this.updateYearFilter();
    this.updateNameFilter();
    this.updateTermFilter();
    this.updateFinancialFilter();
    this.refresh();
  }
  resetFilters() {
    this.yearFilter = "";
    this.nameFilter = "";
    this.termFilter = "";
    this.financialFilter = "";
    this.updateFilters();
  }

  created() {
    this.service = this.$opensilex.getService("opensilex.ProjectsService");
    let query: any = this.$route.query;
    if (query.name) {
      this.nameFilter = decodeURI(query.name);
    }
    if (query.term) {
      this.termFilter = decodeURI(query.term);
    }
    if (query.year) {
      this.yearFilter = decodeURI(query.year);
    }
    if (query.financial) {
      this.financialFilter = decodeURI(query.financial);
    }
  }

  fields = [
    {
      key: "name",
      label: "component.common.name",
      sortable: true
    },
    {
      key: "shortname",
      label: "component.project.shortname",
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
      key: "financialFunding",
      label: "component.project.financialFunding",
      sortable: true
    },
    {
      key: "state",
      label: "component.common.state"
    },
    {
      label: "component.common.actions",
      key: "actions"
    }
  ];

  @Ref("tableRef") readonly tableRef!: any;
  refresh() {
    this.tableRef.refresh();
  }

  loadData(options) {
    let startDateFilter: string = undefined;
    let endDateFilter: string = undefined;
    if (this.yearFilter) {
      startDateFilter = this.yearFilter.toString() + "-01-01";
      endDateFilter = this.yearFilter.toString() + "-12-31";
    }

    return this.service.searchProjects(
      startDateFilter,
      endDateFilter,
      this.termFilter,
      this.nameFilter,
      this.financialFilter,
      options.orderBy,
      options.currentPage,
      options.pageSize
    );
  }
  isEnded(project) {
    if (project.endDate) {
      return moment(project.endDate, "YYYY-MM-DD").diff(moment()) < 0;
    }
    return false;
  }

  deleteUser(uri: string) {
    this.service
      .deleteProject(uri)
      .then(() => {
        this.refresh();
        this.$emit("onDelete", uri);
      })
      .catch(this.$opensilex.errorHandler);
  }
}
</script>

<style scoped lang="scss">
</style>
