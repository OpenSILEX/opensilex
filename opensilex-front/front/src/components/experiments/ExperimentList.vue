<template>
  <div class="container-fluid">
    <opensilex-PageHeader
      icon="ik#ik-layers"
      title="component.menu.experiments"
      description="component.experiment.search.description"
    ></opensilex-PageHeader>

    <opensilex-PageActions
      v-if="user.hasCredential(credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID)"
    >
      <template v-slot>
        <opensilex-CreateButton
          @click="experimentForm.showCreateForm()"
          label="component.experiment.search.buttons.create-experiment"
        ></opensilex-CreateButton>
      </template>
    </opensilex-PageActions>

    <opensilex-SearchForm
      labelTitle="component.experiment.search.label"
      :resetMethod="reset"
      :searchMethod="refresh"
    >
    
      <template v-slot:standardSearch>
        
        <!-- Label -->
        <div class="filter-group col col-xl-3 col-sm-6 col-12">
          <opensilex-InputForm
            :value.sync="filter.label"
            label="component.experiment.search.filter.alias"
            type="text"
            placeholder="component.experiment.search.placeholder.alias"
          ></opensilex-InputForm>
        </div>
        
        <!-- Species -->
        <div class="filter-group col col-xl-3 col-sm-6 col-12">
          <opensilex-SelectForm
            label="component.experiment.search.filter.species"
            placeholder="component.experiment.search.placeholder.species"
            :multiple="true"
            :selected.sync="filter.species"
            :optionsLoadingMethod="loadSpecies"
            :conversionMethod="speciesToSelectNode"
          ></opensilex-SelectForm>
        </div>

        <!-- Projects -->
        <div class="filter-group col col-xl-3 col-sm-6 col-12">
          <opensilex-SelectForm
            label="component.experiment.search.filter.projects"
            placeholder="component.experiment.search.placeholder.projects"
            :selected.sync="filter.projects"
            :conversionMethod="projectGetDTOToSelectNode"
            modalComponent="opensilex-ProjectModalList"
            :isModalSearch="true"
          ></opensilex-SelectForm>
        </div>

        <!-- Installations -->
        <div class="filter-group col col-xl-3 col-sm-6 col-12">
          <opensilex-InputForm
            :value.sync="filter.installations"
            label="component.experiment.search.filter.installations"
            type="text"
            placeholder="component.experiment.search.placeholder.installations"
          ></opensilex-InputForm>
        </div>

        <!-- Campaign -->
        <div class="filter-group col col-xl-3 col-sm-6 col-12">
          <opensilex-SelectForm
            label="component.experiment.search.filter.campaign"
            placeholder="component.experiment.search.placeholder.campaign"
            :multiple="false"
            :selected.sync="filter.campaign"
            :optionsLoadingMethod="loadCampaigns"
          ></opensilex-SelectForm>
        </div>

        <!-- Sites -->
        <div class="filter-group col col-xl-3 col-sm-6 col-12">
          <opensilex-InputForm
            :value.sync="filter.site"
            label="component.experiment.search.filter.sites"
            type="text"
            placeholder="component.experiment.search.placeholder.sites"
          ></opensilex-InputForm>
        </div>

        <!-- Start date -->
        <div class="filter-group col col-xl-3 col-sm-6 col-12">
          <opensilex-InputForm
            :value.sync="filter.startDate"
            label="component.experiment.search.filter.startDate"
            type="text"
            placeholder="component.experiment.search.placeholder.startDate"
          ></opensilex-InputForm>
        </div>

        <!-- State -->
        <div class="filter-group col col-xl-3 col-sm-6 col-12">
          <opensilex-SelectForm
            label="component.experiment.search.filter.state"
            placeholder="component.experiment.search.placeholder.state"
            :multiple="false"
            :selected.sync="filter.state"
            :optionsLoadingMethod="loadStates"
          ></opensilex-SelectForm>
        </div>

      </template>
    
    </opensilex-SearchForm>

    <opensilex-PageContent>

      <template v-slot>

        <opensilex-TableAsyncView
          ref="tableRef" 
          :searchMethod="searchExperiments" 
          :fields="fields"
          isSelectable="true"
          defaultSortBy="label"
          labelNumberOfSelectedRow="component.experiment.search.selectedLabel"
          iconNumberOfSelectedRow="ik#ik-layers">

          <template v-slot:actionsSelectableTable></template>
          
          <template v-slot:cell(uri)="{data}">
            <opensilex-UriLink
              :uri="data.item.uri"
              :to="{path: '/experiment/'+ encodeURIComponent(data.item.uri)}"
            ></opensilex-UriLink>
          </template>

          <template v-slot:cell(label)="{data}">{{data.item.label}}</template>

          <template v-slot:cell(species)="{data}">
            <span :key="index" v-for="(uri, index) in data.item.species">
              <span :title="uri">{{ getSpeciesName(uri) }}</span>
              <span v-if="index + 1 < data.item.species.length">,</span>
            </span>
          </template>

          <template v-slot:cell(projects)="{data}">
            <span :key="index" v-for="(uri, index) in data.item.projects">
              <span :title="uri">{{ getProjectName(uri) }}</span>
              <span v-if="index + 1 < data.item.projects.length">,</span>
            </span>
          </template>

          <template v-slot:cell(installations)>
            &nbsp;
          </template>

          <template v-slot:cell(sites)>
            &nbsp;
          </template>

          <template v-slot:cell(startDate)="{data}">
            <opensilex-DateView :value="data.item.startDate"></opensilex-DateView>
          </template>

          <template v-slot:cell(state)="{data}">
            <i
              v-if="!isEnded(data.item)"
              class="ik ik-zap badge-icon badge-info-phis"
              :title="$t('component.experiment.common.status.in-progress')"
            ></i>
            <i
              v-else
              class="ik ik-zap-off badge-icon badge-light"
              :title="$t('component.experiment.common.status.finished')"
            ></i>
            <i
              v-if="data.item.isPublic"
              class="ik ik-users badge-icon badge-info"
              :title="$t('component.experiment.common.status.public')"
            ></i>
          </template>

          <template v-slot:cell(actions)="{data}">
            <b-button-group size="sm">
              <opensilex-EditButton
                v-if="user.hasCredential(credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID)"
                @click="experimentForm.showEditForm(data.item)"
                label="component.experiment.update"
                :small="true"
              ></opensilex-EditButton>
              <opensilex-DeleteButton
                v-if="user.hasCredential(credentials.CREDENTIAL_EXPERIMENT_DELETE_ID)"
                @click="deleteExperiment(data.item.uri)"
                label="component.experiment.delete"
                :small="true"
              ></opensilex-DeleteButton>
            </b-button-group>
          </template>

        </opensilex-TableAsyncView>

      </template>

    </opensilex-PageContent>

    <opensilex-ExperimentForm
      v-if="user.hasCredential(credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID)"
      ref="experimentForm"
      @onCreate="refresh()"
      @onUpdate="refresh()"
    ></opensilex-ExperimentForm>

  </div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
import VueConstructor from "vue";
import copy from "copy-to-clipboard";
import VueI18n from "vue-i18n";
import moment from 'moment';
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
import { UserGetDTO } from "opensilex-security/index";

export class ExperimentState {
  id: string;
  labelKey: string;
  label: string;

  constructor(id: string, labelKey: string) {
    this.id = id;
    this.labelKey = labelKey;
  }
}

@Component
export default class ExperimentList extends Vue {
  $opensilex: any;

  filter:any = {
    label: undefined,
    uri: undefined,
    species: [],
    projects: [],
    installations: undefined,
    campaign: undefined,
    sites: undefined,
    dates: undefined,
    state: undefined
  };

  @Ref("experimentForm") readonly experimentForm!: any;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  private labelFilter: any = "";
  updateLabelFilter() {
    this.$opensilex.updateURLParameter("label", this.labelFilter, "");
    this.refresh();
  }

  private yearFilter: any = "";
  updateYearFilter() {
    this.$opensilex.updateURLParameter("year", this.yearFilter, "");
    this.refresh();
  }

  private speciesFilter: any = [];
  updateSpeciesFilter() {
    this.$opensilex.updateURLParameter("species", this.speciesFilter);
    this.refresh();
  }

  speciesByUri: Map<String, SpeciesDTO> = new Map<String, SpeciesDTO>();
  projectsByUri: Map<String, ProjectGetDTO> = new Map<String, ProjectGetDTO>();

  @Ref("tableRef") readonly tableRef!: any;

  refresh() {
    this.tableRef.refresh();
  }

  reset() {
    this.filter = {
      label: undefined,
      uri: undefined,
      species: [],
      projects: [],
      installations: undefined,
      campaign: undefined,
      sites: undefined,
      dates: undefined,
      state: undefined
    };

    this.refresh();
  }

  searchExperiments(options) {
    let isPublic = undefined;
    let isEnded = undefined;
    let startDate = undefined;
    let endDate = undefined;
    let species = undefined;
    let projects = undefined;
    let campaign = undefined;
    let label = undefined;
    let uri = undefined;

    if(this.filter.projects) {
      projects = this.filter.projects.map(value => value.id);
    }

    if(this.filter.species) {
      species = this.filter.species;
    }

    if(this.filter.state == 'in-progress') {
      isEnded =false;
    }

    if(this.filter.state == 'finished') {
      isEnded = true;
    }

    if(this.filter.state == 'public') {
      isPublic = true;
    }

    if(this.filter.campaign) {
      campaign = this.filter.campaign;
    }

    if(this.filter.label && this.filter.label.length > 0) {
      label = this.filter.label;
    }

    if(this.filter.uri && this.filter.uri.length > 0) {
      uri = this.filter.uri;
    }

    return this.$opensilex
      .getService("opensilex.ExperimentsService")
      .searchExperiments(
        startDate,
        endDate,
        label,
        species,
        projects,
        isPublic,
        isEnded,
        options.orderBy,
        options.currentPage,
        options.pageSize
      );
  }

  experimentStates: Array<ExperimentState> = [
    {
      id: "in-progress",
      labelKey: "component.experiment.common.status.in-progress",
      label: ""
    },
    {
      id: "finished",
      labelKey: "component.experiment.common.status.finished",
      label: ""
    },
    {
      id: "public",
      labelKey: "component.experiment.common.status.public",
      label: ""
    }
  ];

  created() {
    this.getAllSpecies();
    this.getAllProjects();
  }

  loadCampaigns() {
      return new Promise((resolve, reject) => {
          let campaigns = [];
          campaigns.push({id: 2010, label: 2010});
          campaigns.push({id: 2011, label: 2011});
          campaigns.push({id: 2012, label: 2012});
          campaigns.push({id: 2013, label: 2013});
          campaigns.push({id: 2014, label: 2014});
          campaigns.push({id: 2015, label: 2015});
          campaigns.push({id: 2016, label: 2016});
          campaigns.push({id: 2017, label: 2017});
          campaigns.push({id: 2018, label: 2018});
          campaigns.push({id: 2019, label: 2019});
          campaigns.push({id: 2020, label: 2020});
          resolve(campaigns);
      });
  }

  loadStates() {
    return new Promise((resolve, reject) => {

      for(let i=0; i<this.experimentStates.length; i++) {
        this.experimentStates[i].label = this.$i18n.t(this.experimentStates[i].labelKey).toString();
      }
      
      resolve(this.experimentStates);
    });
  }

  loadSpecies() {
    return this.$opensilex
      .getService("opensilex.SpeciesService")
      .getAllSpecies()
      .then((http: HttpResponse<OpenSilexResponse<Array<SpeciesDTO>>>) => http.response.result);
  }

  getAllSpecies() {
    this.$opensilex.getService("opensilex.SpeciesService").getAllSpecies().then((http: HttpResponse<OpenSilexResponse<Array<SpeciesDTO>>>) => {
      for (let i = 0; i < http.response.result.length; i++) {
        this.speciesByUri.set(
          http.response.result[i].uri,
          http.response.result[i]
        );
      }
      this.$forceUpdate();
    }).catch(this.$opensilex.errorHandler);
  }

  speciesToSelectNode(dto: SpeciesDTO) {
    return {
      id: dto.uri,
      label: dto.label
    };
  }

  getAllProjects() {
    this.$opensilex.getService("opensilex.ProjectsService").searchProjects(
      undefined,
      undefined,
      undefined,
      undefined,
      undefined,
      undefined, //["label"],
      0,
      1000
    ).then((http: HttpResponse<OpenSilexResponse<Array<ProjectGetDTO>>>) => {
      for (let i = 0; i < http.response.result.length; i++) {
        this.projectsByUri.set(
          http.response.result[i].uri,
          http.response.result[i]
        );
      }
      this.$forceUpdate();
    }).catch(this.$opensilex.errorHandler);
  }

  getSpeciesName(uri: String): String {
    if (this.speciesByUri.has(uri)) {
      return this.speciesByUri.get(uri).label;
    }
    return null;
  }

  getProjectName(uri: String): String {
    if (this.projectsByUri.has(uri)) {
      return this.projectsByUri.get(uri).label;
    }
    return null;
  }

  isEnded(experiment) {
    if (experiment.endDate && moment) {
      return moment(experiment.endDate, "YYYY-MM-DD").diff(moment()) < 0;
    } 
    return false;
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
      key: "species",
      label: "component.experiment.species"
    },
    {
      key: "projects",
      label: "component.experiment.search.column.projects",
    },
    {
      key: "installations",
      label: "component.experiment.search.column.installations",
    },
    {
      key: "campaign",
      label: "component.experiment.search.column.campaign",
      sortable: true
    },
    {
      key: "sites",
      label: "component.experiment.search.column.sites",
    },
    {
      key: "startDate",
      label: "component.experiment.startDate",
      sortable: true
    },
    {
      key: "state",
      label: "component.experiment.search.column.state"
    },
    {
      label: "component.common.actions",
      key: "actions"
    }
  ];

  deleteExperiment(uri: string) {
    this.$opensilex
      .getService("opensilex.ExperimentsService")
      .deleteExperiment(uri)
      .then(() => {
        this.refresh();
      })
      .catch(this.$opensilex.errorHandler);
  }

  projectGetDTOToSelectNode(dto: ProjectGetDTO) {
    return {
        id: dto.uri,
        label: dto.label
    };
  }

}
</script>


<style scoped lang="scss">

</style>
