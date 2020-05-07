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
    <opensilex-TableAsyncView
      ref="tableRef"
      :searchMethod="loadData"
      :fields="fields"
      defaultSortBy="startDate"
    >
      <template v-slot:cell(uri)="{data}">
        <a class="uri-info">
          <small>{{ data.item.uri }}</small>
        </a>
      </template>

      <template v-slot:cell(startDate)="{data}">{{ format(data.item.startDate)}}</template>

      <template v-slot:cell(endDate)="{data}">{{ format(data.item.endDate)}}</template>

      <template v-slot:cell(actions)="{data}">
        <b-button-group size="sm">
          <opensilex-DetailButton
            label="component.user.details"
            :detailVisible="data.detailsShowing"
            :small="true"
          ></opensilex-DetailButton>

          <opensilex-EditButton
            @click="$emit('onEdit', data.item)"
            label="component.user.update"
            :small="true"
          ></opensilex-EditButton>

          <opensilex-DeleteButton
            @click="deleteUser(data.item.uri)"
            label="component.user.delete"
            :small="true"
          ></opensilex-DeleteButton>
        </b-button-group>
      </template>
    </opensilex-TableAsyncView>
  </div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
import VueRouter from "vue-router";
import { ProjectGetDTO, ProjectsService } from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "../../lib//HttpResponse";

@Component
export default class ProjectTable extends Vue {
  $opensilex: any;
  service: ProjectsService;
  get user() {
    return this.$store.state.user;
  }
  get credentials() {
    return this.$store.state.credentials;
  }

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
   
    this.service = this.$opensilex.getService("opensilex.ProjectsService");
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

  @Ref("tableRef") readonly tableRef!: any;
  refresh() {
    this.tableRef.refresh();
  }

  loadData(options) {
    let startDateFilter: string;
    let endDateFilter: string;
    if (
      this.yearFilterPatternValue !== "" &&
      this.yearFilterPattern > 1000 &&
      this.yearFilterPattern < 4000
    ) {
      startDateFilter = this.yearFilterPatternValue.toString() + "-01-01";
      endDateFilter = this.yearFilterPatternValue.toString() + "-12-31";
    } else {
      startDateFilter = undefined;
      endDateFilter = undefined;
    }

    return this.service
      .searchProjects(
        startDateFilter,
        endDateFilter,
        this.nameFilterPattern,
        this.nameFilterPattern,
        this.financialFilterPattern,
        options.orderBy,
        options.currentPage,
        options.pageSize
      )
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
</style>
