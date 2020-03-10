<template>

  <div class="container-fluid">

    <div class="page-header">
        <div class="row align-items-end">
            <div class="col-lg-8">
                <div class="page-header-title">
                    <i class="ik ik-layers bg-phis-green"></i>
                    <div class="d-inline">
                        <h5>{{ $t('component.menu.experiments') }}</h5>
                        <span>{{ $t('component.experiment.search.description') }}</span>
                    </div>
                </div>
            </div>
            <div class="col-lg-4">
                <nav class="breadcrumb-container" aria-label="breadcrumb">
                    <ol class="breadcrumb">
                        <li class="breadcrumb-item">
                            <a href="index.html" title="Revenir au tableau de bord"><i class="ik ik-grid mr-1"></i>{{ $t('component.menu.dashboard') }}</a>
                        </li>
                        <li class="breadcrumb-item active"><i class="ik ik-layers mr-1"></i>{{ $t('component.menu.experiments') }}</li>
                    </ol>
                </nav>
            </div>
        </div>
    </div>  

    <div class="card">

      <!-- div class="card-header row clearfix">
          <div class="col col-sm-3">
              <div class="card-options d-inline-block">
                  <button type="button" class="btn btn-primary"><i class="ik ik-plus"></i>{{ $t('component.experiment.search.buttons.add-experiment') }}</button>
              </div>
          </div>                                
      </div -->

      <div class="card-body p-0">
        <div class="table-responsive">
          <table class="table table-striped table-hover">
            <thead>
                <tr>
                    <th>{{ $t('component.experiment.search.column.alias') }}</th>
                    <th>{{ $t('component.experiment.search.column.projects') }}</th>
                    <th>{{ $t('component.experiment.search.column.installations') }}</th>
                    <th>{{ $t('component.experiment.search.column.campaign') }}</th>
                    <th>{{ $t('component.experiment.search.column.places') }}</th>
                    <th>{{ $t('component.experiment.search.column.species') }}</th>
                    <th>{{ $t('component.experiment.search.column.startDate') }}</th>
                    <th>{{ $t('component.experiment.search.column.uri') }}</th>
                    <th>{{ $t('component.experiment.search.column.state') }}</th>
                </tr>
            </thead>
            <tbody>
                <tr class="cell-filters"></tr>

                <tr class="cell-filters">                                                
                  <td>
                    <b-form-input v-model="filter.alias" debounce="300" class="form-control" :placeholder="$t('component.experiment.search.filter.alias')"></b-form-input>
                  </td>
                  <td width="250">
                      <multiselect
                        :limit="1"
                        :multiple="true"
                        track-by="uri"
                        :placeholder="$t('component.experiment.search.filter.projects')"
                        :closeOnSelect="false"
                        v-model="filter.projects"
                        :options="projectsList" 
                        :custom-label="project => project.label"
                        selectLabel=""
                        selectedLabel="X"
                        deselectLabel="X"
                        :limitText="count => $t('component.common.multiselect.label.x-more', {count: count})"/>
                  </td>
                  <td width="250">
                    <multiselect
                        :limit="1"
                        :multiple="true"
                        :placeholder="$t('component.experiment.search.filter.installations')"
                        :closeOnSelect="false"
                        v-model="filter.installations"
                        :options="campains"
                        selectLabel=""
                        selectedLabel="X"
                        deselectLabel="X"
                        :limitText="count => $t('component.common.multiselect.label.x-more', {count: count})"/>
                  </td>
                  <td width="250">
                      <multiselect
                        :limit="1"
                        :placeholder="$t('component.experiment.search.filter.campains')"
                        :closeOnSelect="false"
                        v-model="filter.campains"
                        :options="campains"
                        selectLabel=""
                        selectedLabel="X"
                        deselectLabel="X"
                        :limitText="count => $t('component.common.multiselect.label.x-more', {count: count})" />
                  </td>
                  <td width="250">
                    <multiselect
                      :limit="1"
                      :multiple="true"
                      :placeholder="$t('component.experiment.search.filter.places')"
                      :closeOnSelect="false"
                      v-model="filter.places"
                      :options="campains"
                      selectLabel=""
                      selectedLabel="X"
                      deselectLabel="X"
                      :limitText="count => $t('component.common.multiselect.label.x-more', {count: count})" />
                  </td>
                  <td width="250">
                    <multiselect
                      :limit="1"
                      :multiple="true"
                      :placeholder="$t('component.experiment.search.filter.species')"
                      :closeOnSelect="false"
                      v-model="filter.species"
                      :options="campains"
                      selectLabel=""
                      selectedLabel="X"
                      deselectLabel="X"
                      :limitText="count => $t('component.common.multiselect.label.x-more', {count: count})"/>
                  </td>
                  <td>
                    <div class="datepicker-trigger">
                      <input type="text" id="datepicker1" class="form-control" name="daterange" debounce="300" v-model="filter.beginDate" :placeholder="$t('component.experiment.search.filter.startDate')"  />
                      <AirbnbStyleDatepicker 
                        :trigger-element-id="'datepicker1'"
                        :date-one="filter.startDate"
                        :date-two="filter.endDate"
                        v-on:date-one-selected="function(value) { filter.startDate = value }"
                        v-on:date-two-selected="function(value) { filter.endDate = value }"
                        @apply="filter.updateBeginDate()"
                      />
                    </div>
                  </td>
                  <td>
                    <b-form-input v-model="filter.uri" debounce="300" class="form-control" :placeholder="$t('component.experiment.search.filter.uri')"></b-form-input>
                  </td>
                  <td width="200">
                      <multiselect
                        track-by="code"
                        :custom-label="state => state.label"
                        :limit="1"
                        :placeholder="$t('component.experiment.search.filter.state')"
                        :closeOnSelect="false"
                        v-model="filter.state"
                        :options="experimentStates"
                        selectLabel=""
                        selectedLabel="X"
                        deselectLabel="X"
                        :limitText="count => $t('component.common.multiselect.label.x-more', {count: count})" />
                  </td>
                </tr>

                <tr v-for="experiment in experiments" v-bind:key="experiment.id">
                  <td>{{ experiment.label }}</td>
                  <td>
                    <span :key="index" v-for="(uri, index) in experiment.projects">
                      <span :title="uri">{{ getProjectName(uri) }}</span><span v-if="index + 1 < experiment.projects.length">, </span>
                    </span>
                  </td>
                  <td></td>
                  <td>{{ experiment.campaign }}</td>
                  <td></td>
                  <td>
                    <span :key="index" v-for="(uri, index) in experiment.species">
                      <span :title="uri">{{ getSpeciesName(uri) }}</span><span v-if="index + 1 < experiment.species.length">, </span>
                    </span>
                  </td>
                  <td>{{ formatDate(experiment.startDate) }}</td>
                  <td><span class="uri">{{ experiment.uri }}<a href="#" class="uri-copy" title="Copier dans le presse-papier"><i class="ik ik-copy"></i></a></span></td>
                  <td>
                    <i v-if="!experiment.isEnded" class="ik ik-sun badge-icon badge-info-phis" :title="$t('component.experiment.common.status.in-progress')"></i>
                    <i v-else class="ik ik-moon badge-icon badge-light" :title="$t('component.experiment.common.status.finished')"></i>
                    <!-- i class="ik ik-users badge-icon badge-info" :title="$t('component.experiment.common.status.public')"></i -->
                  </td>
                </tr>                                      
            </tbody>
          </table>
        </div>                                
      </div> 

      <div v-if="totalRow > pageSize" class="card-footer"> 
        <nav class="float-right">
          <b-pagination
            v-model="currentPage"
            :total-rows="totalRow"
            :per-page="pageSize"
            @change="loadExperiments()"
            class="pagination mb-0"
          >
          </b-pagination>
        </nav>
      </div>
                    
    </div>                        
   
  </div>
</template>

<script lang="ts">
import { Component } from "vue-property-decorator";
import Vue from "vue";
import VueConstructor from "vue";
import { ExperimentsService } from "../../lib/api/experiments.service";
import { ProjectsService } from "../../lib/api/projects.service";
import HttpResponse, { OpenSilexResponse } from "../../lib//HttpResponse";
import { ExperimentGetDTO } from "../../lib//model/experimentGetDTO";
import { ProjectCreationDTO } from "../../lib//model/projectCreationDTO";
import VueRouter from "vue-router";
import VueI18n from 'vue-i18n';
import moment from "moment";

export class ExperimentState {

  code: String;
  label: String;

  constructor(code: String, label: String) {
    this.code = code;
    this.label = label;
  }

}

export class ExperimentFilter {

  private _experimentList: ExperimentList;

  private _alias: string;
  private _uri: string;
  private _beginDate: string;
  private _startDate: string;
  private _endDate: string;
  private _campaign: number;
  private _projects: Array<ProjectCreationDTO>;
  private _installations: Array<string>;
  private _places: Array<string>;
  private _species: Array<string>;
  private _state: ExperimentState;

  constructor(experimentList: ExperimentList) {
    this._experimentList = experimentList;
  }

  get alias() {
    return this._alias;
  }

  set alias(value: string) {
    console.log("new alias = " + value);
    this._alias = value;
    this._experimentList.loadExperiments();
  }

  set uri(value: string) {
    console.log("new uri = " + value);
    this._uri = value;
    this._experimentList.loadExperiments();
  }

  get uri() {
    return this._uri;
  }

  updateBeginDate() {
    let startDate = moment(this.startDate, 'YYYY-MM-DD');
    let endDate = moment(this.endDate, 'YYYY-MM-DD');
    this.beginDate = startDate.format("DD/MM/YYYY") + " - " + endDate.format("DD/MM/YYYY");
  }

  set beginDate(value: string) {
    console.log("new beginDate = " + value);
    this._beginDate = value;

    let dates = value.split(" - ");
    
    if(dates.length == 2 && dates[0].length == 10 && dates[1].length == 10) {
      let startDate = moment(dates[0], 'DD/MM/YYYY');
      let endDate = moment(dates[1], 'DD/MM/YYYY');

      console.log("startDate = " + startDate);
      console.log("endDate = " + endDate);

      if(startDate.isValid() && endDate.isValid()) {
        console.log("Date valid");
        this._startDate = startDate.format('YYYY-MM-DD');
        this._endDate = endDate.format('YYYY-MM-DD');
      } 
    }

    this._experimentList.loadExperiments();
  }

  get beginDate() {
    return this._beginDate;
  }

  set startDate(value) {
    console.log("new startDate = " + value);
    this._startDate = value;
  }

  get startDate() {
    return this._startDate;
  }

  set endDate(value) {
    console.log("new endDate = " + value);
    this._endDate = value;
  }

  get endDate() {
    return this._endDate;
  }

  set campaign(value: number) {
    console.log("new campaign = " + value);
    this._campaign = value;
    this._experimentList.loadExperiments();
  }

  get campaign() {
    return this._campaign;
  }

  set projects(values: Array<ProjectCreationDTO>) {
    console.log("new projects = " + values);
    console.log("projects size = " + values.length);
    this._projects = values;
    this._experimentList.loadExperiments();
  }

  get projects() {
    return this._projects;
  }

  set installations(values: Array<string>) {
    console.log("new installations = " + values);
    console.log("installations size = " + values.length);
    this._installations = values;
    this._experimentList.loadExperiments();
  }

  get installations() {
    return this._installations;
  }

   set places(values: Array<string>) {
    console.log("new places = " + values);
    console.log("places size = " + values.length);
    this._places = values;
    this._experimentList.loadExperiments();
  }

  get places() {
    return this._places;
  }

  set species(values: Array<string>) {
    console.log("new species = " + values);
    console.log("species size = " + values.length);
    this._species = values;
    this._experimentList.loadExperiments();
  }

  get species() {
    return this._species;
  }

  get state() {
    return this._state;
  }

  set state(value: ExperimentState) {
    console.log("new state = " + value);
    this._state = value;
    this._experimentList.loadExperiments();
  }
  
}

@Component
export default class ExperimentList extends Vue {
  $opensilex: any;
  $store: any;
  $router: VueRouter;

  projectsList = [];
  projects: Map<String, ProjectCreationDTO> = new Map<String, ProjectCreationDTO>();
  experiments: Array<ExperimentGetDTO> = new Array<ExperimentGetDTO>();
  species: Array<String> = new Array<String>();
  experimentStates: Array<ExperimentState> = new Array<ExperimentState>();
  campains: Array<Number> = new Array<Number>();

  filter: ExperimentFilter;

  orderBy: Array<string>;
  currentPage: number = 1;
  pageSize = 10;
  totalRow = 0;

  constructor() {
    super();
    this.filter = new ExperimentFilter(this);
  }

  created () {
    console.log("created()");
    this.loadDatas();
  }

  get user() {
    return this.$store.state.user;
  }

  loadDatas() {
    this.loadProjects();
    this.loadExperiments();
    this.loadExperimentStates();
  }

  loadExperiments() {
    console.log("loadExperiments()");
    console.log("currentPage = " + this.currentPage);

    let service: ExperimentsService = this.$opensilex.getService(
      "opensilex.ExperimentsService"
    );

    let projects = null;
    if(this.filter.projects && this.filter.projects.length > 0) {
      projects = this.filter.projects.map(project => project.uri);
    }

    let isArchived;
    if(this.filter.state) {
      isArchived = this.filter.state.code === "finished";
    }

    let startDate;
    let endDate;

    if(this.filter.beginDate) {
      let dates = this.filter.beginDate.split(" - ");
      if(dates.length == 2 && dates[0].length == 10 && dates[1].length == 10) {
        startDate = moment(dates[0], 'DD/MM/YYYY').format('YYYY-MM-DD');
        endDate = moment(dates[1], 'DD/MM/YYYY').format('YYYY-MM-DD');
      }
    }
/*
    service.searchExperiments(
      this.user.getAuthorizationHeader(),
      this.filter.uri,
      startDate,
      endDate,
      this.filter.campaign,
      this.filter.alias,
      null,
      projects,
      this.filter.species,
      isArchived,
      this.orderBy,
      this.currentPage - 1,
      this.pageSize
    )
    .then((http: HttpResponse<OpenSilexResponse<Array<ExperimentGetDTO>>>) => {
      console.log(http.response);

      this.totalRow = http.response.metadata.pagination.totalCount;
      this.experiments = http.response.result;

      if(this.campains.length == 0) {
        let allCampaigns = this.experiments.map(experiment => experiment.campaign);
        this.campains = allCampaigns.filter((campaign, index) => {
          return allCampaigns.indexOf(campaign) === index;          
        });
      }
    })
    .catch(this.resetExperiments);
*/
  }

  resetExperiments(error) {
    console.log(error);
    this.totalRow = 0;
    this.experiments = new Array<ExperimentGetDTO>();
  }

  loadExperimentStates() {
    this.experimentStates = new Array<ExperimentState>();
    this.experimentStates.push(new ExperimentState("in-progress", this.$i18n.t("component.experiment.common.status.in-progress").toString()));
    this.experimentStates.push(new ExperimentState("finished", this.$i18n.t("component.experiment.common.status.finished").toString()));
  }

  loadProjects() {
    console.log("loadProjects()");

    let service: ProjectsService = this.$opensilex.getService(
      "opensilex.ProjectsService"
    );
/*
    service.search(
      this.user.getAuthorizationHeader(),
      null,
      0,
      1000
    )
    .then((http: HttpResponse<OpenSilexResponse<Array<ProjectCreationDTO>>>) => {
      console.log(http.response);

      let results: Map<String, ProjectCreationDTO> = new Map<String, ProjectCreationDTO>();
      let resultsList = [];
      for(let i=0; i<http.response.result.length; i++) {
        results.set(http.response.result[i].uri, http.response.result[i]);
        resultsList.push(http.response.result[i]);
      }
      this.projectsList = resultsList;
      this.projects = results;
    }).catch(this.$opensilex.errorHandler);
*/
  }

  formatDate(value: any): String {
    return moment().year(value.year).month(value.month).date(value.day).format('DD/MM/YYYY');
  }

  getProjectName(uri: String): String {
    if(this.projects.has(uri)) {
      let project = this.projects.get(uri);
      return project.label;
    }
    return null;
  }

  getSpeciesName(uri: String): String {
    return uri;
  }

}

</script>

<style scoped lang="scss">

</style>
