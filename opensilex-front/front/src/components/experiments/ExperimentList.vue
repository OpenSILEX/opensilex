<template>
  <div>
    <opensilex-SearchFilterField
      @search="refresh()"
      @clear="reset()"
      label="component.experiment.search.label"
      :showAdvancedSearch="true"
    >
      <template v-slot:filters>
        <!-- Label -->
        <opensilex-FilterField>
          <opensilex-InputForm
            :value.sync="filter.label"
            label="ExperimentList.filter-label"
            type="text"
            placeholder="ExperimentList.filter-label-placeholder"
          ></opensilex-InputForm>
        </opensilex-FilterField>

        <!-- Species -->
        <opensilex-FilterField>
          <opensilex-SelectForm
            label="ExperimentList.filter-species"
            placeholder="ExperimentList.filter-species-placeholder"
            :multiple="true"
            :selected.sync="filter.species"
            :options="species"
          ></opensilex-SelectForm>
        </opensilex-FilterField>

        <!-- Factors -->
        <opensilex-FilterField>
          <opensilex-FactorSelector :multiple="true" :factors.sync="filter.factors"></opensilex-FactorSelector>
        </opensilex-FilterField>

        <opensilex-FilterField>
          <label>{{$t('ExperimentList.filter-year')}}</label>
          <opensilex-StringFilter
            placeholder="ExperimentList.filter-year-placeholder"
            :filter.sync="filter.yearFilter"
            type="number"
            min="1000"
            max="9999"
          ></opensilex-StringFilter>
        </opensilex-FilterField>
      </template>

      <template v-slot:advancedSearch>
        <!-- Projects -->
        <opensilex-FilterField>
          <opensilex-SelectForm
            label="ExperimentList.filter-project"
            placeholder="ExperimentList.filter-project-placeholder"
            :selected.sync="filter.projects"
            :conversionMethod="projectGetDTOToSelectNode"
            modalComponent="opensilex-ProjectModalList"
            :isModalSearch="true"
          ></opensilex-SelectForm>
        </opensilex-FilterField>

        <!-- State -->
        <opensilex-FilterField>
          <opensilex-SelectForm
            label="ExperimentList.filter-state"
            placeholder="ExperimentList.filter-state-placeholder"
            :multiple="false"
            :selected.sync="filter.state"
            :options="experimentStates"
          ></opensilex-SelectForm>
        </opensilex-FilterField>
      </template>
    </opensilex-SearchFilterField>

    <opensilex-TableAsyncView
      ref="tableRef"
      :searchMethod="searchExperiments"
      :fields="fields"
      :isSelectable="isSelectable"
    >
      <template v-slot:cell(label)="{data}">
        <opensilex-UriLink
          :uri="data.item.uri"
          :value="data.item.label"
          :to="{path: '/experiment/details/'+ encodeURIComponent(data.item.uri)}"
        ></opensilex-UriLink>
      </template>

      <template v-slot:cell(species)="{data}">
        <span :key="index" v-for="(uri, index) in data.item.species">
          <span :title="uri">{{ getSpeciesName(uri) }}</span>
          <span v-if="index + 1 < data.item.species.length">,</span>
        </span>
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
          :title="$t('component.experiment.common.status.in-progress')"
        ></i>
        <i
          v-else
          class="ik ik-archive badge-icon badge-light"
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
            @click="$emit('onEdit', data.item.uri)"
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
  </div>
</template>

<script lang="ts">
import { Component, Ref, Prop } from "vue-property-decorator";
import Vue from "vue";
import VueConstructor from "vue";
import copy from "copy-to-clipboard";
import VueI18n from "vue-i18n";
import moment from "moment";
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

@Component
export default class ExperimentList extends Vue {
  $opensilex: any;
  $i18n: any;
  $store: any;

  @Prop({
    default: false
  })
  isSelectable;

  @Prop({
    default: false
  })
  noActions;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  species = [];
  speciesByUri: Map<String, SpeciesDTO> = new Map<String, SpeciesDTO>();

  @Ref("tableRef") readonly tableRef!: any;

  refresh() {
    this.tableRef.refresh();
  }

  filter = {
    label: "",
    species: [],
    factors: [],
    projects: [],
    yearFilter: undefined,
    state: ""
  };

  reset() {
    this.filter = {
      label: "",
      species: [],
      factors: [],
      projects: [],
      yearFilter: undefined,
      state: ""
    };
    this.refresh();
  }

  searchExperiments(options) {
    let isPublic = undefined;
    let isEnded = undefined;
    if (this.filter.state) {
      if (this.filter.state == "public") {
        isPublic = true;
      }
      if (this.filter.state == "finished") {
        isEnded = true;
      } else if (this.filter.state == "in-progress") {
        isEnded = false;
      }
    }

    let projects = [];
    if (this.filter.projects) {
      for (let i in this.filter.projects) {
        projects.push(this.filter.projects[i].id);
      }
    }

    return this.$opensilex
      .getService("opensilex.ExperimentsService")
      .searchExperiments(
       this.filter.yearFilter, // year
        this.filter.label, // label
        this.filter.species, // species
        this.filter.factors, // factors
        projects, // projects
        isPublic, // isPublic
        isEnded, // isEnded
        options.orderBy,
        options.currentPage,
        options.pageSize
      );
  }

  experimentStates = [];

  created() {
    this.loadSpecies();
    this.refreshStateLabel();
  }

  private langUnwatcher;
  mounted() {
    this.langUnwatcher = this.$store.watch(
      () => this.$store.getters.language,
      lang => {
        this.refreshStateLabel();
        this.loadSpecies();
      }
    );
  }

  beforeDestroy() {
    this.langUnwatcher();
  }

  refreshStateLabel() {
    this.experimentStates = [
      {
        id: "in-progress",
        label: this.$i18n.t("component.experiment.common.status.in-progress")
      },
      {
        id: "finished",
        label: this.$i18n.t("component.experiment.common.status.finished")
      },
      {
        id: "public",
        label: this.$i18n.t("component.experiment.common.status.public")
      }
    ];
  }

  loadSpecies() {
    let service: SpeciesService = this.$opensilex.getService(
      "opensilex.SpeciesService"
    );
    service
      .getAllSpecies()
      .then((http: HttpResponse<OpenSilexResponse<Array<SpeciesDTO>>>) => {
        this.species = [];
        for (let i = 0; i < http.response.result.length; i++) {
          this.speciesByUri.set(
            http.response.result[i].uri,
            http.response.result[i]
          );
          this.species.push({
            id: http.response.result[i].uri,
            label: http.response.result[i].label
          });
        }
      })
      .catch(this.$opensilex.errorHandler);
  }

  getSpeciesName(uri: String): String {
    if (this.speciesByUri.has(uri)) {
      return this.speciesByUri.get(uri).label;
    }
    return null;
  }

  isEnded(experiment) {
    if (experiment.endDate) {
      return moment(experiment.endDate, "YYYY-MM-DD").diff(moment()) < 0;
    }
    return false;
  }

  get fields() {
    let tableFields = [
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
        key: "state",
        label: "component.experiment.search.column.state"
      }
    ];
    if (!this.noActions) {
      tableFields.push({
        key: "actions",
        label: "component.common.actions"
      });
    }
    return tableFields;
  }

  deleteExperiment(uri: string) {
    this.$opensilex
      .getService("opensilex.ExperimentsService")
      .deleteExperiment(uri)
      .then(() => {
        this.refresh();
      })
      .catch(this.$opensilex.errorHandler);
  }

  projectGetDTOToSelectNode(dto) {
    if (dto) {
      return {
        id: dto.uri,
        label: dto.shortname ? dto.shortname : dto.label
      };
    }
    return null;
  }
}
</script>


<style scoped lang="scss">
</style>

<i18n>
en:
  ExperimentList:
    filter-label: Name
    filter-label-placeholder: Enter a name
    filter-year: Year
    filter-year-placeholder: Enter a year
    filter-species: Species
    filter-species-placeholder: Select one or more species
    filter-project: Project
    filter-project-placeholder: Select a project
    filter-state: State
    filter-state-placeholder: Select an experiment state

fr:
  ExperimentList:
    filter-label: Nom
    filter-label-placeholder: Saisir un nom
    filter-year: Année
    filter-year-placeholder: Saisir une année
    filter-species: Espèces
    filter-species-placeholder: Sélectionner une ou plusieurs espèces
    filter-project: Projet
    filter-project-placeholder: Sélectionner un projet
    filter-state: Etat
    filter-state-placeholder: Sélectionner un état

</i18n>