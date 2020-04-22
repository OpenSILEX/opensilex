<template>
  <div class="container-fluid">
    <opensilex-PageHeader
      icon="ik-layers"
      title="component.menu.experiments"
      description="component.experiment.search.description"
    ></opensilex-PageHeader>

    <div class="card">
      <div class="card-header row clearfix">
        <div class="col col-sm-3">
          <div class="card-options d-inline-block">
            <b-button
              id="create-experiment"
              @click="goToExperimentCreateComponent()"
              variant="primary"
            >
              <i class="ik ik-plus"></i>
              {{ $t('component.experiment.search.buttons.create-experiment') }}
            </b-button>
            <!-- todo: add theming color -->
            <b-tooltip
              target="create-experiment"
            >{{ $t('component.experiment.search.buttons.create-experiment-help') }}</b-tooltip>
          </div>
        </div>
      </div>

      <b-table
        ref="tableRef"
        striped
        hover
        small
        :items="loadExperiments"
        :fields="fields"
        
        no-provider-paging
      >
        <template v-slot:head(uri)="data">{{$t(data.label)}}</template>
        <template v-slot:head(label)="data">{{$t(data.label)}}</template>
        <template v-slot:head(projects)="data">{{$t(data.label)}}</template>
        <template v-slot:head(species)="data">{{$t(data.label)}}</template>
        <template v-slot:head(startDate)="data">{{$t(data.label)}}</template>
        <template v-slot:head(endDate)="data">{{$t(data.label)}}</template> 
        <template v-slot:head(actions)="data">{{$t(data.label)}}</template>

        <template v-slot:cell(uri)="data">
          <a class="uri-info">
            <small>{{ data.item.uri }}</small>
          </a>
        </template>

        <template v-slot:cell(label)="data">{{data.item.label}}</template>

        <template v-slot:cell(projects)="data">
          <span :key="index" v-for="(uri, index) in data.item.projects">
              <span :title="uri">{{ getProjectName(uri) }}</span><span v-if="index + 1 < data.item.projects.length">, </span>
          </span>
        </template>

        <template v-slot:cell(species)="data">
          <span :key="index" v-for="(uri, index) in data.item.species">
              <span :title="uri">{{ getSpeciesName(uri) }}</span><span v-if="index + 1 < data.item.species.length">, </span>
          </span>
        </template>

        <template v-slot:cell(startDate)="data">{{ formatDate(data.item.startDate)}}</template>
        <template v-slot:cell(endDate)="data">{{ formatDate(data.item.endDate)}}</template>

       <template v-slot:cell(actions)="data">
        <b-button-group size="sm" >
          <b-button size="sm" @click="data.toggleDetails" variant="outline-success">
            <font-awesome-icon v-if="!data.detailsShowing" icon="eye" size="sm" />
            <font-awesome-icon v-if="data.detailsShowing" icon="eye-slash" size="sm" />
          </b-button>

          <b-button size="sm" @click="$emit('onEdit', data.item)" variant="outline-primary">
            <font-awesome-icon icon="edit" size="sm" />
          </b-button>    
        </b-button-group>
      </template>
      </b-table>




      <div v-if="totalRow > pageSize" class="card-footer">
        <nav class="float-right">
          <b-pagination
            v-model="currentPage"
            :total-rows="totalRow"
            :per-page="pageSize"
            @change="loadExperiments()"
            class="pagination mb-0"
          ></b-pagination>
        </nav>
      </div>
    </div>
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
import VueConstructor from "vue";
import VueRouter from "vue-router";
import moment from "moment";
import copy from "copy-to-clipboard";
import VueI18n from "vue-i18n";
import {
  ProjectGetDTO,
  SpeciesDTO,
  ExperimentGetDTO,
  ResourceTreeDTO,
  InfrastructureGetDTO,
  ExperimentsService,
  InfrastructuresService,
  ProjectsService,
  SpeciesService
} from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";

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
  private _projects: Array<ProjectGetDTO>;
  private _installations: Array<any>;
  private _infrastructures: Array<ResourceTreeDTO>;
  private _species: SpeciesDTO;
  private _state: ExperimentState;

  constructor(experimentList: ExperimentList) {
    this._experimentList = experimentList;
  }

  get alias() {
    return this._alias;
  }

  set alias(value: string) {
    this._alias = value;
    this._experimentList.loadExperiments();
  }

  set uri(value: string) {
    this._uri = value;
    this._experimentList.loadExperiments();
  }

  get uri() {
    return this._uri;
  }

  updateBeginDate() {
    let startDate = moment(this.startDate, "YYYY-MM-DD");
    let endDate = moment(this.endDate, "YYYY-MM-DD");
    this.beginDate =
      startDate.format("DD/MM/YYYY") + " - " + endDate.format("DD/MM/YYYY");
  }

  set beginDate(value: string) {
    this._beginDate = value;

    let dates = value.split(" - ");

    this._endDate = undefined;
    this._startDate = undefined;

    if (dates.length == 2 && dates[0].length == 10 && dates[1].length == 10) {
      let startDate = moment(dates[0], "DD/MM/YYYY");
      let endDate = moment(dates[1], "DD/MM/YYYY");

      if (startDate.isValid() && endDate.isValid()) {
        this._startDate = startDate.format("YYYY-MM-DD");
        this._endDate = endDate.format("YYYY-MM-DD");
      }
    }

    this._experimentList.loadExperiments();
  }

  get beginDate() {
    return this._beginDate;
  }
  set startDate(value) {
    this._startDate = value;
  }

  get startDate() {
    return this._startDate;
  }

  set endDate(value) {
    this._endDate = value;
  }

  get endDate() {
    return this._endDate;
  }

  set campaign(value: number) {
    this._campaign = value;
    this._experimentList.loadExperiments();
  }

  get campaign() {
    return this._campaign;
  }

  set projects(values: Array<ProjectGetDTO>) {
    this._projects = values;
    this._experimentList.loadExperiments();
  }

  get projects() {
    return this._projects;
  }

  set installations(values: Array<string>) {
    this._installations = values;
    this._experimentList.loadExperiments();
  }

  get installations() {
    return this._installations;
  }

  set infrastructures(values: Array<ResourceTreeDTO>) {
    this._infrastructures = values;
    this._experimentList.loadExperiments();
  }

  get infrastructures() {
    return this._infrastructures;
  }

  set species(value: SpeciesDTO) {
    this._species = value;
    this._experimentList.loadExperiments();
  }

  get species() {
    return this._species;
  }

  get state() {
    return this._state;
  }

  set state(value: ExperimentState) {
    this._state = value;
    this._experimentList.loadExperiments();
  }
}

@Component
export default class ExperimentList extends Vue {
  $opensilex: any;
  $store: any;
  $router: VueRouter;

  speciesList = [];
  speciesByUri: Map<String, SpeciesDTO> = new Map<String, SpeciesDTO>();

  infrastructuresList = [];
  infrastructuresByUri: Map<String, ResourceTreeDTO> = new Map<
    String,
    ResourceTreeDTO
  >();

  projectsList = [];
  projectsByUri: Map<String, ProjectGetDTO> = new Map<String, ProjectGetDTO>();

  installationsList = [];
  installationsByUri: Map<String, any> = new Map<String, any>();

  experimentStates: Array<ExperimentState> = new Array<ExperimentState>();
  campaigns: Array<Number> = new Array<Number>();

  experiments: Array<ExperimentGetDTO> = new Array<ExperimentGetDTO>();

  orderBy: Array<string>;
  currentPage: number = 1;
  pageSize = 10;
  totalRow = 0;

  filter: ExperimentFilter;

  constructor() {
    super();
    this.filter = new ExperimentFilter(this);
  }

  created() {
    this.loadDatas();
  }

  get user() {
    return this.$store.state.user;
  }

  loadDatas() {
    this.loadProjects();
    this.loadSpecies();
    // this.loadInfrastructures();
    this.loadExperiments();
    this.loadExperimentStates();
  }

  loadExperiments() {
    let service: ExperimentsService = this.$opensilex.getService(
      "opensilex.ExperimentsService"
    );

    let projects = undefined;
    if (this.filter.projects && this.filter.projects.length > 0) {
      projects = this.filter.projects.map(project => project.uri);
    }

    let isEnded = undefined;
    let isPublic = undefined;
    // if (this.filter.state) {
    //   if (this.filter.state.code === "finished") {
    //     isEnded = true;
    //   } else if (this.filter.state.code === "in-progress") {
    //     isEnded = false;
    //   } else if (this.filter.state.code === "public") {
    //     isPublic = true;
    //   }
    // }

    let startDate = undefined;
    let endDate = undefined;
    // if (this.filter.startDate && this.filter.startDate.length > 0) {
    //   startDate = this.filter.startDate;
    // }
    // if (this.filter.endDate && this.filter.endDate.length > 0) {
    //   endDate = this.filter.endDate;
    // }

    let species = undefined;
    // if (this.filter.species) {
    //   species = this.filter.species.uri;
    // }

    let uri = undefined;
    // if (this.filter.uri && this.filter.uri.length > 0) {
    //   uri = this.filter.uri;
    // }

    let alias = undefined;
    // if (this.filter.alias && this.filter.alias.length > 0) {
    //   alias = this.filter.alias;
    // }

    let campaign = undefined;
    // if (this.filter.campaign) {
    //   campaign = this.filter.campaign;
    // }

    if(this.sortBy){
      let orderByText = this.sortBy + "=";
      this.orderBy.push( orderByText.concat(this.sortDesc ?  "desc" : "asc"));
    }

    return service
      .searchExperiments(
        uri,
        startDate,
        endDate,
        campaign,
        alias,
        species,
        projects,
        isPublic,
        isEnded,
        this.orderBy,
        this.currentPage - 1,
        this.pageSize
      )
      .then((http: HttpResponse<OpenSilexResponse<Array<ExperimentGetDTO>>>) => {
          this.totalRow = http.response.metadata.pagination.totalCount;
          this.pageSize = http.response.metadata.pagination.pageSize;
          setTimeout(() => {
            this.currentPage = http.response.metadata.pagination.currentPage + 1;
          }, 0);

          this.experiments = http.response.result;

          // if (this.campaigns.length == 0) {
          //   let allCampaigns = this.experiments.map(
          //     experiment => experiment.campaign
          //   );
          //   this.campaigns = allCampaigns.filter((campaign, index) => {
          //     return allCampaigns.indexOf(campaign) === index;
          //   });
          // }
          return http.response.result;
      })
      .catch(error => {
        this.resetExperiments(error);
      });
  }

  resetExperiments(error) {
    this.totalRow = 0;
    this.experiments = new Array<ExperimentGetDTO>();
  }

  loadExperimentStates() {
    this.experimentStates = new Array<ExperimentState>();
    this.experimentStates.push(
      new ExperimentState(
        "in-progress",
        this.$i18n
          .t("component.experiment.common.status.in-progress")
          .toString()
      )
    );
    this.experimentStates.push(
      new ExperimentState(
        "finished",
        this.$i18n.t("component.experiment.common.status.finished").toString()
      )
    );
    this.experimentStates.push(
      new ExperimentState(
        "public",
        this.$i18n.t("component.experiment.common.status.public").toString()
      )
    );
  }

  loadProjects() {
    let service: ProjectsService = this.$opensilex.getService(
      "opensilex.ProjectsService"
    );

    service
      .searchProjects(
        undefined,
        undefined,
        undefined,
        undefined,
        undefined,
        undefined,
        undefined,
        0,
        1000
      )
      .then((http: HttpResponse<OpenSilexResponse<Array<ProjectGetDTO>>>) => {     
        for (let i = 0; i < http.response.result.length; i++) {
          this.projectsByUri.set(http.response.result[i].uri, http.response.result[i]);
          this.projectsList.push(http.response.result[i]);
        }
      })
      .catch(this.$opensilex.errorHandler);
  }

  loadSpecies() {
    let service: SpeciesService = this.$opensilex.getService(
      "opensilex.SpeciesService"
    );

    service
      .getAllSpecies()
      .then((http: HttpResponse<OpenSilexResponse<Array<SpeciesDTO>>>) => {
        for (let i = 0; i < http.response.result.length; i++) {
          this.speciesByUri.set(http.response.result[i].uri,http.response.result[i]);
          this.speciesList.push(http.response.result[i]);
        }
      })
      .catch(this.$opensilex.errorHandler);
  }

  formatDate(value: string): string {
    if(value != undefined && value != null){
        return moment(value, 'YYYY-MM-dd').format('DD/MM/YYYY');
    }
    return null;
  }

  getProjectName(uri: String): String {
    if (this.projectsByUri.has(uri)) {
      return this.projectsByUri.get(uri).label;
    }
    return null;
  }

  getSpeciesName(uri: String): String {
    if (this.speciesByUri.has(uri)) {
      return this.speciesByUri.get(uri).label;
    }
    return null;
  }

  goToExperimentCreateComponent() {
    this.$router.push({ path: "/experiments/create" });
  }

  goToExperimentUpdateComponent(experimentDto: ExperimentGetDTO) {
    this.$store.xpToUpdate = experimentDto;
    this.$store.editXp = true;
    this.$router.push({ path: "/experiments/create" });
  }

  copyUri(uri: string) {
    copy(uri);
    if (event) {
      event.preventDefault();
    }
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
      key: "projects",
      label: "component.experiment.projects",
      sortable: true
    },
    {
      key: "species",
      label: "component.experiment.species",
      // sortable: true
    },
    {
      key: "startDate",
      label: "component.experiment.startDate",
      // sortable: true
    },
    {
      key: "endDate",
      label: "component.experiment.endDate",
      // sortable: true
    },
    {
      label: "component.common.actions",
      key: "actions"
    }
  ];

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