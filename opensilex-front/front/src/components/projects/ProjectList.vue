<template>
  <div>
    <opensilex-SearchFilterField :withButton="false">
      <template v-slot:filters>
        <div class="col col-xl-4 col-sm-6 col-12">
          <label>{{$t('component.project.filter-label')}}:</label>
          <opensilex-StringFilter
            :filter.sync="nameFilter"
            @update="refresh()"
            placeholder="component.project.filter-label-placeholder"
          ></opensilex-StringFilter>
        </div>

        <div class="col col-xl-4 col-sm-6 col-12">
          <label>{{$t('component.project.filter-year')}}:</label>
          <opensilex-StringFilter
            :filter.sync="yearFilter"
            @update="refresh()"
            type="number"
            placeholder="component.project.filter-year-placeholder"
          ></opensilex-StringFilter>
        </div>

        <div class="col col-xl-4 col-sm-6 col-12">
          <label>{{$t('component.project.filter-financial')}}:</label>
          <opensilex-StringFilter
            :filter.sync="financialFilter"
            @update="refresh()"
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
      <template v-slot:cell(uri)="{data}">
        <opensilex-UriLink :uri="data.item.uri"></opensilex-UriLink>
      </template>

      <template v-slot:cell(startDate)="{data}">
        <opensilex-DateView :value="data.item.startDate"></opensilex-DateView>
      </template>

      <template v-slot:cell(endDate)="{data}">
        <opensilex-DateView :value="data.item.endDate"></opensilex-DateView>
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

  private nameFilter: any = "";

  private financialFilter: any = "";

  created() {
    this.service = this.$opensilex.getService("opensilex.ProjectsService");
  }

  fields = [
    {
      key: "uri",
      label: "component.common.uri",
      sortable: true
    },
    {
      key: "label",
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
      key: "hasFinancialFunding",
      label: "component.project.financialFunding",
      sortable: true
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
      this.nameFilter,
      this.financialFilter,
      options.orderBy,
      options.currentPage,
      options.pageSize
    );
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
