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
          label="component.experiment.add"
        ></opensilex-CreateButton>
      </template>
    </opensilex-PageActions>

    <opensilex-SearchFilterField @clear="resetFilters()" @search="updateFilters()">
      <template v-slot:filters>
          <div class="col col-xl-4 col-sm-6 col-12">
            <label>{{$t('ExperimentList.filter-label')}}:</label>
            <opensilex-StringFilter
              :filter.sync="labelFilter"
              @update="updateLabelFilter()"
              placeholder="ExperimentList.label-filter-placeholder"
            ></opensilex-StringFilter>
          </div>

          <div class="col col-xl-4 col-sm-6 col-12">
            <label>{{$t('ExperimentList.filter-year')}}:</label>
            <opensilex-StringFilter
              :filter.sync="yearFilter"
              type="number"
              min="1000"
              max="9999"
              @update="updateYearFilter()"
              placeholder="ExperimentList.year-filter-placeholder"
            ></opensilex-StringFilter>
          </div>

          <div class="col col-xl-4 col-sm-6 col-12">
            <opensilex-SpeciesSelector
              label="ExperimentList.filter-species"
              :multiple="true"
              :species.sync="speciesFilter"
              @clear="updateSpeciesFilter()"
              @select="updateSpeciesFilter()"
              @deselect="updateSpeciesFilter()"
            ></opensilex-SpeciesSelector>
          </div>
          
           <!-- Factor -->
          <div class="col col-xl-4 col-sm-6 col-12">
            <opensilex-FactorSelector 
              :multiple="true" 
              :factors.sync="factorsFilter"  
              @clear="updateFactorsFilter()"
              @select="updateFactorsFilter()"
              @deselect="updateFactorsFilter()">
            </opensilex-FactorSelector>
          </div>
      </template>
    </opensilex-SearchFilterField>

    <opensilex-PageContent>
      <template v-slot>
        <opensilex-TableAsyncView ref="tableRef" :searchMethod="searchExperiments" :fields="fields">
          <template v-slot:cell(label)="{data}">
            <opensilex-UriLink
              :uri="data.item.uri"
              :value="data.item.label"
              :to="{path: '/experiment/details/'+ encodeURIComponent(data.item.uri),query: { name: data.item.label }}"
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
                @click="showEditForm(data.item.uri)"
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

export class ExperimentState {
  code: String;
  label: String;

  constructor(code: String, label: String) {
    this.code = code;
    this.label = label;
  }
}

@Component
export default class ExperimentList extends Vue {
  $opensilex: any;

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
  }

  private yearFilter: any = "";
  updateYearFilter() {
    this.$opensilex.updateURLParameter("year", this.yearFilter, "");
  }

  private speciesFilter: any = [];
  updateSpeciesFilter() {
    this.$opensilex.updateURLParameter("species", this.speciesFilter);
  }

  updateFilters() {
    this.updateYearFilter();
    this.updateLabelFilter();
    this.updateSpeciesFilter();
    this.updateFactorsFilter();
    this.refresh();
  }
  resetFilters() {
    this.yearFilter = "";
    this.labelFilter = "";
    this.speciesFilter = [];
    this.factorsFilter = [];
    this.updateFilters();
  }

  private factorsFilter: any = [];
  updateFactorsFilter() {
    this.$opensilex.updateURLParameter("factors", this.factorsFilter);
   }

  speciesByUri: Map<String, SpeciesDTO> = new Map<String, SpeciesDTO>();

  @Ref("tableRef") readonly tableRef!: any;

  refresh() {
    this.tableRef.refresh();
  }

  searchExperiments(options) {
    let startDateFilter: string = undefined;
    let endDateFilter: string = undefined;
    if (this.yearFilter) {
      startDateFilter = this.yearFilter.toString() + "-01-01";
      endDateFilter = this.yearFilter.toString() + "-12-31";
    }

    return this.$opensilex
      .getService("opensilex.ExperimentsService")
      .searchExperiments(
        startDateFilter, // startDate
        endDateFilter, // endDate
        this.labelFilter, // label
        this.speciesFilter, // species
        this.factorsFilter, // factors
        undefined, // projects
        undefined, // isPublic
        undefined, // isEnded
        options.orderBy,
        options.currentPage,
        options.pageSize
      );
  }

  experimentStates: Array<ExperimentState> = [
    {
      code: "in-progress",
      label: "component.experiment.common.status.in-progress"
    },
    {
      code: "finished",
      label: "component.experiment.common.status.finished"
    },
    {
      code: "public",
      label: "component.experiment.common.status.public"
    }
  ];

  private langUnwatcher;
  mounted() {
    this.langUnwatcher = this.$store.watch(
      () => this.$store.getters.language,
      lang => {
        this.loadSpecies();
      }
    );
  }

  beforeDestroy() {
    this.langUnwatcher();
  }

  loadSpecies() {
    let service: SpeciesService = this.$opensilex.getService(
      "opensilex.SpeciesService"
    );

    this.$opensilex.disableLoader();
    service
      .getAllSpecies()
      .then((http: HttpResponse<OpenSilexResponse<Array<SpeciesDTO>>>) => {
        for (let i = 0; i < http.response.result.length; i++) {
          this.speciesByUri.set(
            http.response.result[i].uri,
            http.response.result[i]
          );
        }
        this.$opensilex.enableLoader();
        this.refresh();
      })
      .catch(this.$opensilex.errorHandler);
  }

  created() {
    let query: any = this.$route.query;
    if (query.label) {
      this.labelFilter = decodeURI(query.label);
    }
    if (query.year) {
      this.yearFilter = decodeURI(query.year);
    }
    if (query.species && Array.isArray(query.species)) {
      for (let i in query.species) {
        this.speciesFilter.push(decodeURI(query.species[i]));
      }
    }
    this.loadSpecies();
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

  fields = [
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
    },
    {
      label: "component.common.actions",
      key: "actions"
    }
  ];

  showEditForm(uri: string) {
    this.$opensilex
      .getService("opensilex.ExperimentsService")
      .getExperiment(uri)
      .then(http => {
        this.experimentForm.showEditForm(http.response.result);
      });
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
}
</script>


<style scoped lang="scss">
</style>

<i18n>
en:
  ExperimentList:
    filter-label: Name
    label-filter-placeholder: Enter a name
    filter-year: Year
    year-filter-placeholder: Enter a year
    filter-species: Species

fr:
  ExperimentList:
    filter-label: Nom
    label-filter-placeholder: Saisir un nom
    filter-year: Année
    year-filter-placeholder: Saisir une année
    filter-species: Espèces
</i18n>