<template>
  <div>
    <opensilex-SearchFilterField
      ref="searchFilterField"
      @search="refresh()"
      @clear="reset()"
      label="component.experiment.search.label"
      :showAdvancedSearch="true"
    >
      <template v-slot:filters>
        <!-- Name -->
        <opensilex-FilterField>
          <b-form-group>
            <label for="name">{{ $t("ExperimentList.filter-label") }}</label>
            <opensilex-StringFilter
              id="name"
              :filter.sync="filter.name"
              placeholder="ExperimentList.filter-label-placeholder"
            ></opensilex-StringFilter>
          </b-form-group>
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

        <!-- factorCategories -->
        <opensilex-FilterField>
           <opensilex-FactorCategorySelector
          ref="factorCategorySelector"
          label="ExperimentList.filter-factors-categories"
          placeholder="ExperimentList.filter-factors-categories-placeholder"
          helpMessage="component.factor.name-help"
          :multiple="true" 
          :category.sync="filter.factorCategories"
        ></opensilex-FactorCategorySelector> 
        </opensilex-FilterField>

        <!-- Year -->
        <opensilex-FilterField>
          <label>{{ $t("ExperimentList.filter-year") }}</label>
          <opensilex-StringFilter
            placeholder="ExperimentList.filter-year-placeholder"
            :filter.sync="filter.yearFilter"
            type="number"
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
            :itemLoadingMethod="loadProjects"
            :isModalSearch="true"
            :clearable="true"
            :multiple="true"
            @clear="filter.projects=null"
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
      :isSelectable="true"
      labelNumberOfSelectedRow="ExperimentList.selected"
      iconNumberOfSelectedRow="ik#ik-layers"
    >

      <template v-slot:selectableTableButtons="{ numberOfSelectedRows }">
        <b-dropdown
          dropright
          class="mb-2 mr-2"
          :small="true"
          :disabled="numberOfSelectedRows == 0"
          text=actions
          v-if="user.hasCredential(credentials.CREDENTIAL_DOCUMENT_MODIFICATION_ID)"
        >
            <b-dropdown-item-button
                v-if="user.hasCredential(credentials.CREDENTIAL_DOCUMENT_MODIFICATION_ID)"
              @click="createDocument()"
            >{{$t('component.common.addDocument')}}</b-dropdown-item-button>
        </b-dropdown>
      </template>
      <template v-slot:cell(name)="{ data }">
        <opensilex-UriLink
          :uri="data.item.uri"
          :value="data.item.name"
          :to="{
            path: '/experiment/details/' + encodeURIComponent(data.item.uri),
          }"
        ></opensilex-UriLink>
      </template>

      <template v-slot:cell(species)="{ data }">
        <span :key="index" v-for="(uri, index) in data.item.species">
          <span :title="uri">{{ getSpeciesName(uri) }}</span>
          <span v-if="index + 1 < data.item.species.length">,</span>
        </span>
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
          :title="$t('component.experiment.common.status.in-progress')"
        ></i>
        <i
          v-else
          class="ik ik-archive badge-icon badge-light"
          :title="$t('component.experiment.common.status.finished')"
        ></i>
        <i
          v-if="data.item.is_public"
          class="ik ik-users badge-icon badge-info"
          :title="$t('component.experiment.common.status.public')"
        ></i>
      </template>

      <template v-slot:cell(actions)="{ data }">
        <b-button-group size="sm">
          <opensilex-EditButton
            v-if="
              user.hasCredential(
                credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID
              )
            "
            @click="$emit('onEdit', data.item.uri)"
            label="component.experiment.update"
            :small="true"
          ></opensilex-EditButton>
          <opensilex-DeleteButton
            v-if="
              user.hasCredential(credentials.CREDENTIAL_EXPERIMENT_DELETE_ID)
            "
            @click="deleteExperiment(data.item.uri)"
            label="component.experiment.delete"
            :small="true"
          ></opensilex-DeleteButton>
        </b-button-group>
      </template>
    </opensilex-TableAsyncView>
    <opensilex-ModalForm
      v-if="user.hasCredential(credentials.CREDENTIAL_DOCUMENT_MODIFICATION_ID)"
      ref="documentForm"
      component="opensilex-DocumentForm"
      createTitle="component.common.addDocument"
      modalSize="lg"
      :initForm="initForm"
      icon="ik#ik-file-text"
    ></opensilex-ModalForm>
  </div>
</template>

<script lang="ts">
import { Component, Ref, Prop } from "vue-property-decorator";
import Vue from "vue";
import moment from "moment";
// @ts-ignore
import { SpeciesDTO, SpeciesService, ProjectGetDTO } from "opensilex-core/index";
// @ts-ignore
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";
import {User} from "../../models/User";

@Component
export default class ExperimentList extends Vue {
  $opensilex: any;
  $i18n: any;
  $store: any;
  
  @Ref("documentForm") readonly documentForm!: any;

  @Prop({
    default: false,
  })
  isSelectable;

  @Prop({
    default: false,
  })
  noActions;

  get user(): User {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  species = [];
  speciesByUri: Map<String, SpeciesDTO> = new Map<String, SpeciesDTO>();

  @Ref("tableRef") readonly tableRef!: any;

  refresh() {
    this.tableRef.selectAll = false;
    this.tableRef.onSelectAll();
    this.$opensilex.updateURLParameters(this.filter);
    this.tableRef.refresh();
  }

  filter = {
    name: "",
    species: [],
    factorCategories: [],
    projects: [],
    yearFilter: undefined,
    state: "",
  };

  reset() {
    this.filter = {
      name: "",
      species: [],
      factorCategories: [],
      projects: [],
      yearFilter: undefined,
      state: "",
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

    return this.$opensilex
      .getService("opensilex.ExperimentsService")
      .searchExperiments(
        this.filter.name, // label
        this.filter.yearFilter, // year
        isEnded, // isEnded
        this.filter.species, // species
        this.filter.factorCategories, // factorCategories
        this.filter.projects, // projects
        isPublic, // isPublic
        options.orderBy,
        options.currentPage,
        options.pageSize
      );
  }

  experimentStates = [];

  created() {
    this.loadSpecies();
    this.refreshStateLabel();
    this.$opensilex.updateFiltersFromURL(this.$route.query, this.filter);
  }

  private langUnwatcher;
  mounted() {
    this.langUnwatcher = this.$store.watch(
      () => this.$store.getters.language,
      (lang) => {
        this.refreshStateLabel();
        this.loadSpecies();
        this.$opensilex.loadFactorCategories();
        this.refresh();
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
        label: this.$i18n.t("component.experiment.common.status.in-progress"),
      },
      {
        id: "finished",
        label: this.$i18n.t("component.experiment.common.status.finished"),
      },
      {
        id: "public",
        label: this.$i18n.t("component.experiment.common.status.public"),
      },
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
            label: http.response.result[i].name,
          });
        }
      })
      .catch(this.$opensilex.errorHandler);
  }

  getSpeciesName(uri: String): String {
    if (this.speciesByUri.has(uri)) {
      return this.speciesByUri.get(uri).name;
    }
    return null;
  }

  isEnded(experiment) {
    if (experiment.end_date) {
      return moment(experiment.end_date, "YYYY-MM-DD").diff(moment()) < 0;
    }
    return false;
  }

  get fields() {
    let tableFields = [
      {
        key: "name",
        label: "component.common.name",
        sortable: true,
      },
      {
        key: "species",
        label: "component.experiment.species",
      },
      {
        key: "start_date",
        label: "component.experiment.startDate",
        sortable: true,
      },
      {
        key: "end_date",
        label: "component.experiment.endDate",
        sortable: true,
      },
      {
        key: "state",
        label: "component.experiment.search.column.state",
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
        name: dto.shortname ? dto.shortname : dto.name,
      };
    }
    return null;
  } 
    createDocument() {
    this.documentForm.showCreateForm();
  }

  initForm() {
    let targetURI = [];
    for (let select of this.tableRef.getSelected()) {
      targetURI.push(select.uri);
    }

    return {
      description: {
        uri: undefined,
        identifier: undefined,
        rdf_type: undefined,
        title: undefined,
        date: undefined,
        description: undefined,
        targets: targetURI,
        authors: undefined,
        language: undefined,
        deprecated: undefined,
        keywords: undefined
      },
      file: undefined
    }
  }

  soGetDTOToSelectNode(dto) {
    if (dto) {
      return {
        id: dto.uri,
        label: dto.name
      };
    }
    return null;
  }

  loadProjects(projectsURIs) {
      return this.$opensilex.getService("opensilex.ProjectsService")
        .getProjectsByURI(projectsURIs)
        .then((http: HttpResponse<OpenSilexResponse<Array<ProjectGetDTO>>>) => {
            return (http && http.response) ? http.response.result : undefined
    }).catch(this.$opensilex.errorHandler);
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
    filter-factors-categories: Factors categories
    filter-factors-categories-placeholder: Select one or more categories
    selected: Selected experiments

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
    filter-factors-categories: Categories de facteurs
    filter-factors-categories-placeholder: Sélectionner une ou plusieurs categories
    selected: Expérimentations selectionnées

</i18n>