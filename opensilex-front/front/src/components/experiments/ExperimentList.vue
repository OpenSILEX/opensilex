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

        <div class="card">
            
            <div class="card-header row clearfix">
                <div class="col col-sm-3">
                    <div class="card-options d-inline-block">
                        <b-button id="create-experiment" @click="goToExperimentCreateComponent()" variant="primary"><i class="ik ik-plus"></i>{{ $t('component.experiment.search.buttons.create-experiment') }}</b-button>
                        <!-- todo: add theming color -->
                        <b-tooltip target="create-experiment" >{{ $t('component.experiment.search.buttons.create-experiment-help') }}</b-tooltip>
                    </div>
                </div>
            </div>
    

            <div class="card-body p-0">
                <div class="table-responsive">
                    <table class="table table-striped table-hover">
                        <thead>
                        <tr>
                            <th>Actions</th>
                            <th>{{ $t('component.experiment.search.column.alias') }}</th>
                            <th>{{ $t('component.experiment.search.column.projects') }}</th>
                            <!-- <th>{{ $t('component.experiment.search.column.installations') }}</th> -->
                            <!-- <th>{{ $t('component.experiment.search.column.campaign') }}</th> -->
                            <!-- <th>{{ $t('component.experiment.search.column.places') }}</th> -->
                            <th>{{ $t('component.experiment.search.column.species') }}</th>
                            <th>{{ $t('component.experiment.search.column.startDate') }}</th>
                            <th>{{ $t('component.experiment.search.column.endDate') }}</th>
                            <th>{{ $t('component.experiment.search.column.uri') }}</th>
                            <th>{{ $t('component.experiment.search.column.state') }}</th>
                        </tr>
                        </thead>
                        <tbody>
                        <tr class="cell-filters"></tr>

                        <tr class="cell-filters">
                            <td>
                                Edit
                            </td>
                            <td>
                                <b-form-input v-model="filter.alias" debounce="300" class="form-control" 
                                :placeholder="$t('component.experiment.search.filter.alias')"></b-form-input>
                            </td>
                            <td width="190">
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
                            <td width="200">
                                <multiselect
                                        :limit="1"
                                        track-by="uri"
                                        multiple="true"
                                        :placeholder="$t('component.experiment.search.filter.species')"
                                        :closeOnSelect="false"
                                        v-model="filter.species"
                                        :options="speciesList"
                                        :custom-label="species => species.label"
                                        selectLabel=""
                                        selectedLabel="X"
                                        deselectLabel="X"
                                        :limitText="count => $t('component.common.multiselect.label.x-more', {count: count})"/>
                            </td>
                            <td>
                                <div class="datepicker-trigger">
                                    <input type="text" id="datepicker1" class="form-control" name="daterange" debounce="300" v-model="filter.beginDate" :placeholder="$t('component.experiment.search.filter.date')"  />
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
                            <td></td>
                            <td>
                                <b-form-input v-model="filter.uri" debounce="300" class="form-control" :placeholder="$t('component.experiment.search.filter.uri')"></b-form-input>
                            </td>
                            <td width="170">
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
                            <td>
                                    <b-button-group size="sm">
                                        <b-button size="sm" @click="goToExperimentUpdateComponent(experiment)" variant="outline-primary">
                                            <font-awesome-icon icon="edit" size="sm" />
                                        </b-button>
                                    </b-button-group>
                            </td>
                            <td>
                                <router-link :to="{path: '/experiment/' + encodeURIComponent(experiment.uri)}">
                                    {{ experiment.label }}
                                </router-link>
                            </td>
                            <td>
                                <span :key="index" v-for="(uri, index) in experiment.projects">
                                    <span :title="uri">{{ getProjectName(uri) }}</span><span v-if="index + 1 < experiment.projects.length">, </span>
                                </span>
                            </td>
                            <!-- <td></td> -->
                            <!-- <td>{{ experiment.campaign }}</td>
                            <td>
                                <span :key="index" v-for="(uri, index) in experiment.infrastructures">
                                    <span :title="uri">{{ getInfrastructureName(uri) }}</span><span v-if="index + 1 < experiment.infrastructures.length">, </span>
                                </span>
                            </td> -->
                             <td>
                                <span :key="index" v-for="(uri, index) in experiment.species">
                                    <span :title="uri">{{ getSpeciesName(uri) }}</span><span v-if="index + 1 < experiment.species.length">, </span>
                                </span>
                            </td>

                            <!-- <td><span :title="experiment.species">{{ getSpeciesName(experiment.species) }}</span></td> -->
                            <td>{{ formatDate(experiment.startDate) }}</td>
                            <td>{{ formatDate(experiment.endDate) }}</td>
                            <td>
                                <span class="uri">
                                    {{ experiment.uri }}
                                    <a href="#" v-on:click="copyUri(experiment.uri, $event)" class="uri-copy" :title="$t('component.copyToClipboard.copyUri')"><i class="ik ik-copy"></i></a>
                                </span>
                            </td>
                            <td>
                                <i v-if="!experiment.isEnded" class="ik ik-activity badge-icon badge-info-phis" :title="$t('component.experiment.common.status.in-progress')"></i>
                                <i v-else class="ik ik-archive badge-icon badge-light" :title="$t('component.experiment.common.status.finished')"></i>
                                <i v-if="experiment.isPublic" class="ik ik-users badge-icon badge-info" :title="$t('component.experiment.common.status.public')"></i>
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
                      <i class="ik ik-copy"></i>
                    </a>
                  </span>
                </td>
                <td>
                  <i
                    v-if="!experiment.isEnded"
                    class="ik ik-activity badge-icon badge-info-phis"
                    :title="$t('component.experiment.common.status.in-progress')"
                  ></i>
                  <i
                    v-else
                    class="ik ik-archive badge-icon badge-light"
                    :title="$t('component.experiment.common.status.finished')"
                  ></i>
                  <i
                    v-if="experiment.isPublic"
                    class="ik ik-users badge-icon badge-info"
                    :title="$t('component.experiment.common.status.public')"
                  ></i>
                </td>
                 <td>
                  <b-button-group size="sm">
                    <b-button
                      size="sm"
                      @click="goToExperimentUpdateComponent(experiment)"
                      variant="outline-primary"
                    >
                      <font-awesome-icon icon="edit" size="sm" />
                    </b-button>
                  </b-button-group>
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
    console.log(value);
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

    export class ExperimentFilter {

        private _experimentList: ExperimentList;

        private _alias: string;
        private _uri: string;
        private _beginDate: string;
        private _startDate: string;
        private _endDate: string;
        private _campaign: number;
        private _projects: Array<ProjectCreationDTO>;
        private _installations: Array<any>;
        private _infrastructures: Array<ResourceTreeDTO>;
        private _species: Array<SpeciesDTO>;
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
            let startDate = moment(this.startDate, 'YYYY-MM-DD');
            let endDate = moment(this.endDate, 'YYYY-MM-DD');
            this.beginDate = startDate.format("DD/MM/YYYY") + " - " + endDate.format("DD/MM/YYYY");
        }

        set beginDate(value: string) {
            console.log(value);
            this._beginDate = value;

            let dates = value.split(" - ");

            this._endDate = undefined;
            this._startDate = undefined;
            
            if(dates.length == 2 && dates[0].length == 10 && dates[1].length == 10) {
                let startDate = moment(dates[0], 'DD/MM/YYYY');
                let endDate = moment(dates[1], 'DD/MM/YYYY');

                if(startDate.isValid() && endDate.isValid()) {
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
            console.log(value);
            this._campaign = value;
            this._experimentList.loadExperiments();
        }

        get campaign() {
            return this._campaign;
        }

        set projects(values: Array<ProjectCreationDTO>) {
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

        set species(values: Array<SpeciesDTO>) {
            this._species = values
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
          this.experiments = http.response.result;

          if (this.campaigns.length == 0) {
            let allCampaigns = this.experiments.map(
              experiment => experiment.campaign
            );

            let projects = undefined;
            if(this.filter.projects && this.filter.projects.length > 0) {
                projects = this.filter.projects.map(project => project.uri);
            }

            let isEnded = undefined;
            let isPublic = undefined;
            if(this.filter.state) {
                if(this.filter.state.code === "finished") {
                    isEnded = true;
                } else if(this.filter.state.code === "in-progress") {
                    isEnded = false;
                } else if(this.filter.state.code === "public") {
                    isPublic = true;
                }
            }

            let startDate = undefined;
            let endDate = undefined;
            if(this.filter.startDate && this.filter.startDate.length > 0) {
                startDate = this.filter.startDate;
            }
            if(this.filter.endDate && this.filter.endDate.length > 0) {
                endDate = this.filter.endDate;
            }

            let species = undefined;
            if(this.filter.species && this.filter.species.length > 0 ) {
                species = this.filter.species.map(_species => _species.uri);
            }

            let uri = undefined;
            if(this.filter.uri && this.filter.uri.length > 0) {
                uri = this.filter.uri;
            }

            let alias = undefined;
            if(this.filter.alias && this.filter.alias.length > 0) {
                alias = this.filter.alias;
            }

            let campaign = undefined;
            if(this.filter.campaign) {
                campaign = this.filter.campaign;
            }
                
            service.searchExperiments(
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
                this.experiments = http.response.result;

                if(this.campaigns.length == 0) {
                    let allCampaigns = this.experiments.map(experiment => experiment.campaign);
                    this.campaigns = allCampaigns.filter((campaign, index) => {
                        return allCampaigns.indexOf(campaign) === index;
                    });
                }

                this.$store.state.search.results = this.experiments.map(experiment => experiment.uri);
            }).catch(error => {
                this.resetExperiments(error);
            });
        }

        resetExperiments(error) {
            this.totalRow = 0;
            this.experiments = new Array<ExperimentGetDTO>();
        }

        loadExperimentStates() {
            this.experimentStates = new Array<ExperimentState>();
            this.experimentStates.push(new ExperimentState("in-progress", this.$i18n.t("component.experiment.common.status.in-progress").toString()));
            this.experimentStates.push(new ExperimentState("finished", this.$i18n.t("component.experiment.common.status.finished").toString()));
            this.experimentStates.push(new ExperimentState("public", this.$i18n.t("component.experiment.common.status.public").toString()));
        }

        loadProjects() {
            let service: ProjectsService = this.$opensilex.getService(
                "opensilex.ProjectsService"
            );

            service.searchProjects(
                null,
                0,
                1000
            )
            .then((http: HttpResponse<OpenSilexResponse<Array<ProjectCreationDTO>>>) => {
                let results: Map<String, ProjectCreationDTO> = new Map<String, ProjectCreationDTO>();
                let resultsList = [];
                for(let i=0; i<http.response.result.length; i++) {
                    results.set(http.response.result[i].uri, http.response.result[i]);
                    resultsList.push(http.response.result[i]);
                }
                this.projectsList = resultsList;
                this.projectsByUri = results;
            }).catch(this.$opensilex.errorHandler);
        }

        loadInfrastructures() {
            let service: InfrastructuresService = this.$opensilex.getService(
                "opensilex.InfrastructuresService"
            );

            service.searchInfrastructuresTree(
                this.user.getAuthorizationHeader(),
                undefined
            ).then((http: HttpResponse<OpenSilexResponse<Array<ResourceTreeDTO>>>) => {
                let results: Map<String, ResourceTreeDTO> = new Map<String, ResourceTreeDTO>();
                let resultsList = [];
                for(let i=0; i<http.response.result.length; i++) {
                    results.set(http.response.result[i].uri, http.response.result[i]);
                    resultsList.push(http.response.result[i]);
                }
                this.infrastructuresList = resultsList;
                this.infrastructuresByUri = results;
            }).catch(this.$opensilex.errorHandler);
        }

        loadSpecies() {
            let service: SpeciesService = this.$opensilex.getService(
                "opensilex.SpeciesService"
            );

            service.getAllSpecies().then((http: HttpResponse<OpenSilexResponse<Array<SpeciesDTO>>>) => {
                for(let i=0; i<http.response.result.length; i++) {
                    this.speciesByUri.set(http.response.result[i].uri, http.response.result[i]);
                    this.speciesList.push(http.response.result[i]);
                }
            }).catch(this.$opensilex.errorHandler);
        }

        formatDate(value: string): string {
            if(value != undefined && value != null){
                return moment(value, 'YYYY-MM-dd').format('DD/MM/YYYY');
            }
            return null;
        }

        getProjectName(uri: String): String {
            if(this.projectsByUri.has(uri)) {
                return this.projectsByUri.get(uri).label;
            }
            return null;
        }

        getSpeciesName(uri: String): String {
            if(this.speciesByUri.has(uri)){
                return this.speciesByUri.get(uri).label;
            }
            return null
        }

        getInfrastructureName(uri: String): String {
            if(this.infrastructuresByUri.has(uri)){
                return this.infrastructuresByUri.get(uri).name;
            }
            return null
        }

        goToExperimentCreateComponent(){
            this.$router.push({ path: '/experiments/create' });
        }
        
        goToExperimentUpdateComponent(experimentDto: ExperimentGetDTO){
            this.$store.xpToUpdate = experimentDto;
            this.$store.editXp = true;
            this.$router.push({  path: '/experiments/create' });
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
      sortable: true,
    },
    {
      key: "label",
      label: "component.common.name",
      sortable: true
    },
    {
      key: "projects",
      label: "component.experiment.projects",
    },
    {
      key: "species",
      label: "component.experiment.species",
    },
    {
      key: "startDate",
      label: "component.experiment.startDate",
      sortable: true
    },
    {
      key: "endDate",
      label: "component.experiment.endDate",
      sortable: true
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