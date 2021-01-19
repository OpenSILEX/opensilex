<template>
  <div>
    <opensilex-SearchFilterField @clear="reset()" @search="refresh()">
      <template v-slot:filters>
        <opensilex-FilterField>
          <opensilex-InputForm
            :value.sync="termFilter"
            label="component.common.keyword"
            type="text"
            placeholder="component.project.filter-keywords-placeholder"
          ></opensilex-InputForm>
        </opensilex-FilterField>

        <opensilex-FilterField>
          <opensilex-InputForm
            :value.sync="nameFilter"
            label="component.common.name"
            type="text"
            placeholder="component.project.filter-label-placeholder"
          ></opensilex-InputForm>
        </opensilex-FilterField>

        <opensilex-FilterField>
          <opensilex-InputForm
            :value.sync="yearFilter"
            label="component.common.year"
            type="number"
            placeholder="component.project.filter-year-placeholder"
          ></opensilex-InputForm>
        </opensilex-FilterField>

        <opensilex-FilterField>
          <opensilex-InputForm
            :value.sync="financialFilter"
            label="component.project.financialFunding"
            type="text"
            placeholder="component.project.filter-financial-placeholder"
          ></opensilex-InputForm>
        </opensilex-FilterField>
      </template>
    </opensilex-SearchFilterField>
    <opensilex-TableAsyncView
      ref="tableRef"
      :searchMethod="loadData"
      :fields="fields"
      defaultSortBy="startDate"
      :isSelectable="isSelectable"
      labelNumberOfSelectedRow="component.project.selectedLabel"
    >
      <template v-slot:cell(name)="{ data }">
        <opensilex-UriLink
          :uri="data.item.uri"
          :value="data.item.name"
          :to="{
            path: '/project/details/' + encodeURIComponent(data.item.uri),
          }"
        ></opensilex-UriLink>
      </template>

      <template v-slot:cell(start_date)="{ data }">
        <opensilex-DateView :value="data.item.start_date"></opensilex-DateView>
      </template>

      <template v-slot:cell(end_date)="{ data }">
        <opensilex-DateView :value="data.item.end_date"></opensilex-DateView>
      </template>

      <template v-slot:cell(state)="{ data }">
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

      <template v-slot:cell(actions)="{ data }">
        <b-button-group size="sm">
          <opensilex-EditButton
            v-if="
              user.hasCredential(credentials.CREDENTIAL_PROJECT_MODIFICATION_ID)
            "
            @click="$emit('onEdit', data.item)"
            label="component.project.update"
            :small="true"
          ></opensilex-EditButton>

          <opensilex-DeleteButton
            v-if="user.hasCredential(credentials.CREDENTIAL_PROJECT_DELETE_ID)"
            @click="deleteProject(data.item.uri)"
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
import { Component, Ref, Prop } from "vue-property-decorator";
import Vue from "vue";
import VueRouter from "vue-router";
import { ProjectGetDTO, ProjectsService } from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "../../lib//HttpResponse";

@Component
export default class ProjectList extends Vue {
  $opensilex: any;
  $store: any;

  service: ProjectsService;
  get user() {
    return this.$store.state.user;
  }
  get credentials() {
    return this.$store.state.credentials;
  }

  @Prop({
    default: false,
  })
  isSelectable;

  @Prop({
    default: false,
  })
  noActions;

  private yearFilter: any = "";
  private nameFilter: any = "";
  private termFilter: any = "";
  private financialFilter: any = "";

  reset() {
    this.yearFilter = "";
    this.nameFilter = "";
    this.termFilter = "";
    this.financialFilter = "";
    this.refresh();
  }

  created() {
    this.service = this.$opensilex.getService("opensilex.ProjectsService");
  }

  get fields() {
    let tableFields: any = [
      {
        key: "name",
        label: "component.common.name",
        sortable: true,
      },
      {
        key: "shortname",
        label: "component.project.shortname",
        sortable: true,
      },
      {
        key: "start_date",
        label: "component.common.startDate",
        sortable: true,
      },
      {
        key: "end_date",
        label: "component.common.endDate",
        sortable: true,
      },
      {
        key: "financial_funding",
        label: "component.project.financialFunding",
        sortable: true,
      },
      {
        key: "state",
        label: "component.common.state",
      },
    ];
    if (!this.noActions) {
      tableFields.push({
        key: "actions",
        label: "component.common.actions",
      });
    }
    return tableFields;
  }

  getSelected() {
    return this.tableRef.getSelected();
  }

  @Ref("tableRef") readonly tableRef!: any;
  refresh() {
    this.tableRef.refresh();
  }

  loadData(options) {
    return this.service.searchProjects(
      this.yearFilter,
      this.termFilter,
      this.nameFilter,
      this.financialFilter,
      options.orderBy,
      options.currentPage,
      options.pageSize
    );
  }

  isEnded(project) {
    if (project.end_date) {
      return moment(project.end_date, "YYYY-MM-DD").diff(moment()) < 0;
    }
    return false;
  }

  deleteProject(uri: string) {
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
