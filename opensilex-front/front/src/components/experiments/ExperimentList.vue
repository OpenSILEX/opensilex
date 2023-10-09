<template>
  <div>
    <opensilex-PageContent
      class="pagecontent"
    >
      <!-- Toggle Sidebar--> 
      <div class="searchMenuContainer"
      v-on:click="SearchFiltersToggle = !SearchFiltersToggle"
      :title="searchFiltersPannel()">
        <div class="searchMenuIcon">
          <i class="icon ik ik-search"></i>
        </div>
      </div>
      <!-- FILTERS -->
      <Transition>
        <div v-show="SearchFiltersToggle">
          <opensilex-SearchFilterField
            ref="searchFilterField"
            @search="refresh()"
            @clear="reset()"
            label="component.experiment.search.label"
            :showAdvancedSearch="true"
            class="searchFilterField"
          >
            <template v-slot:filters>
              <!-- Name -->
              <div>
                <opensilex-FilterField>
                  <b-form-group>
                    <label for="name">{{ $t("ExperimentList.filter-label") }}</label>
                    <opensilex-StringFilter
                      id="name"
                      :filter.sync="filter.name"
                      placeholder="ExperimentList.filter-label-placeholder"
                      class="searchFilter"
                      @handlingEnterKey="refresh()"
                    ></opensilex-StringFilter>
                  </b-form-group>
                </opensilex-FilterField>
              </div>

              <!-- Species -->
              <div>
                <opensilex-FilterField>
                  <opensilex-SelectForm
                    v-if="!isGermplasmMenuExcluded"
                    label="ExperimentList.filter-species"
                    placeholder="ExperimentList.filter-species-placeholder"
                    :multiple="true"
                    :selected.sync="filter.species"
                    :options="species"
                    class="searchFilter"
                  ></opensilex-SelectForm>
                </opensilex-FilterField>
              </div>

              <!-- factorCategories -->
              <div>
                <opensilex-FilterField>
                  <opensilex-FactorCategorySelector
                  ref="factorCategorySelector"
                  label="ExperimentList.filter-factors-categories"
                  placeholder="ExperimentList.filter-factors-categories-placeholder"
                  helpMessage="component.factor.name-help"
                  :multiple="true" 
                  :category.sync="filter.factorCategories"
                  class="searchFilter"
                ></opensilex-FactorCategorySelector> 
                </opensilex-FilterField>
              </div>

              <!-- Facilities -->
              <div>
                <opensilex-FilterField>
                  <opensilex-SelectForm
                      label="ExperimentList.filter-facilities"
                      placeholder="ExperimentList.filter-facilities-placeholder"
                      :multiple="true"
                      :selected.sync="filter.facilities"
                      :options="facilities"
                      class="searchFilter"
                  ></opensilex-SelectForm>
                </opensilex-FilterField>
              </div>

              <!-- Year -->
              <div>
                <opensilex-FilterField>
                  <label>{{ $t("ExperimentList.filter-year") }}</label>
                  <opensilex-StringFilter
                    placeholder="ExperimentList.filter-year-placeholder"
                    :filter.sync="filter.yearFilter"
                    type="number"
                    class="searchFilter"
                    @handlingEnterKey="refresh()"
                  ></opensilex-StringFilter>
                </opensilex-FilterField><br>
              </div>
            </template>

            <template v-slot:advancedSearch>
              <!-- Projects -->
              <div>
                <opensilex-FilterField>
                  <opensilex-SelectForm
                    ref="projectSelector"
                    label="ExperimentList.filter-project"
                    placeholder="ExperimentList.filter-project-placeholder"
                    :selected.sync="filter.projects"
                    modalComponent="opensilex-ProjectModalList"
                    :isModalSearch="true"
                    :clearable="true"
                    :multiple="true"
                    @clear="refreshProjectSelector"
                    :limit="1"
                    class="searchFilter"
                    @handlingEnterKey="refresh()"
                  ></opensilex-SelectForm>
                </opensilex-FilterField>
              </div>

              <!-- State -->
              <div>
                <opensilex-FilterField>
                  <opensilex-SelectForm
                    label="ExperimentList.filter-state"
                    placeholder="ExperimentList.filter-state-placeholder"
                    :multiple="false"
                    :selected.sync="filter.state"
                    :options="experimentStates"
                    class="searchFilter"
                    @handlingEnterKey="refresh()"
                  ></opensilex-SelectForm>
                </opensilex-FilterField>
              </div>
            </template>
          </opensilex-SearchFilterField>
        </div>
      </Transition>

    <opensilex-TableAsyncView
      ref="tableRef"
      :searchMethod="searchExperiments"
      :fields="fields"
      :isSelectable="true"
      @refreshed="onRefreshed"
      labelNumberOfSelectedRow="ExperimentList.selected"
      iconNumberOfSelectedRow="ik#ik-layers"
    >

      <template v-slot:selectableTableButtons="{ numberOfSelectedRows }">

        <b-dropdown
          dropright
          class="mb-2 mr-2"
          :small="true"
          :text="$t('VariableList.display')">

          <b-dropdown-item-button @click="clickOnlySelected()">{{ onlySelected ? $t('ExperimentList.selected-all') : $t("component.common.selected-only")}}</b-dropdown-item-button>
          <b-dropdown-item-button @click="resetSelected()">{{$t("component.common.resetSelected")}}</b-dropdown-item-button>
        </b-dropdown>

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

      <template v-if="!isGermplasmMenuExcluded" v-slot:cell(species)="{ data }">
        <span class="species-list" v-if="data.item.species.length > 0">
          <span :key="index" v-for="(uri, index) in data.item.species">
            <span :title="uri">{{ getSpeciesName(uri) }}</span>
            <span v-if="index + 1 < data.item.species.length">, </span>
          </span>
        </span>
        <span v-else></span>
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
      </opensilex-PageContent>
  </div>
</template>

<script lang="ts">
import {Component, Prop, Ref} from "vue-property-decorator";
import Vue from "vue";
import {SpeciesDTO, SpeciesService} from "opensilex-core/index";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import {User} from "../../models/User";
import {OrganizationsService} from "opensilex-core/api/organizations.service";
import {FacilityGetDTO} from "opensilex-core/index";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {ExperimentsService} from "opensilex-core/api/experiments.service";

@Component
export default class ExperimentList extends Vue {
  $opensilex: OpenSilexVuePlugin;
  $i18n: any;
  $store: any;
  SearchFiltersToggle: boolean = false;
  
  @Ref("documentForm") readonly documentForm!: any;

  @Prop({
    default: false,
  })
  isSelectable;

  @Prop({
    default: false,
  })
  noActions;


  get onlySelected() {
    return this.tableRef.onlySelected;
  }

  get user(): User {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  facilities = [];
  species = [];

  /**
   * The key is the URI in extended form
   */
  speciesByUri: Map<String, SpeciesDTO> = new Map<String, SpeciesDTO>();

  @Ref("tableRef") readonly tableRef!: any;
  @Ref("projectSelector") readonly projectSelector!: any;

  onItemUnselected(row) {
    this.tableRef.onItemUnselected(row);
  }
  onItemSelected(row) {
    this.tableRef.onItemSelected(row);
  }

  refresh() {
    this.updateSelectedExperiment();
    this.tableRef.changeCurrentPage(1);
    
  }

  filter = {
    name: "",
    species: [],
    factorCategories: [],
    projects: [],
    yearFilter: undefined,
    state: "",
    facilities: [],
  };


  reset() {
    this.filter = {
      name: "",
      species: [],
      factorCategories: [],
      projects: [],
      yearFilter: undefined,
      state: "",
      facilities: [],
    };   
    this.refresh();
  }

  clickOnlySelected() {
    this.tableRef.clickOnlySelected();
  }

  resetSelected() {
    this.tableRef.resetSelected();
  }

  refreshProjectSelector() {
   
    this.projectSelector.refreshModalSearch();
  }

  updateSelectedExperiment(){
    this.$opensilex.updateURLParameters(this.filter);
    if(this.tableRef.onlySelected) {
      this.tableRef.onlySelected = false;
    }
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
      .getService<ExperimentsService>("opensilex.ExperimentsService")
      .searchExperiments(
        this.filter.name, // label
        this.filter.yearFilter, // year
        isEnded, // isEnded
        this.filter.species, // species
        this.filter.factorCategories, // factorCategories
        this.filter.projects, // projects
        isPublic, // isPublic
        this.filter.facilities,
        options.orderBy,
        options.currentPage,
        options.pageSize
      );
  }

  experimentStates = [];

  created() {
    this.loadSpecies();
    this.loadFacilities();
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
        this.loadFacilities();
        this.$opensilex.loadFactorCategories();
        this.refresh();
      }
    );
  }

  onRefreshed() {
      let that = this;
      setTimeout(function() {
        if(that.tableRef.selectAll === true && that.tableRef.selectedItems.length !== that.tableRef.totalRow) {                    
          that.tableRef.selectAll = false;
        } 
      }, 1);
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
            this.$opensilex.getLongUri(http.response.result[i].uri),
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

  loadFacilities() {
    let service: OrganizationsService = this.$opensilex.getService(
        "opensilex.OrganizationsService"
    );
    service
        .getAllFacilities()
        .then((http: HttpResponse<OpenSilexResponse<Array<FacilityGetDTO>>>) => {
          this.facilities = [];
          for (let i = 0; i < http.response.result.length; i++) {
            this.facilities.push({
              id: http.response.result[i].uri,
              label: http.response.result[i].name,
            });
          }
        })
        .catch(this.$opensilex.errorHandler);
  }

  getSpeciesName(uri: string): String {
    return this.speciesByUri.get(this.$opensilex.getLongUri(uri))?.name;
  }

  isEnded(experiment) {
    if (experiment.end_date) {
      return new Date(experiment.end_date).getTime() < new Date().getTime();
    }
    return false;
  }

  get isGermplasmMenuExcluded() {
        return this.$opensilex.getConfig().menuExclusions.includes("germplasm");
  }

  get fields() {
    let tableFields = [
      {
        key: "name",
        label: "component.common.name",
        sortable: true,
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
    if (!this.isGermplasmMenuExcluded) {
      tableFields.push({
        key: "species",
        label: "component.experiment.species",
      });
    }
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
      .getService<ExperimentsService>("opensilex.ExperimentsService")
      .deleteExperiment(uri)
      .then(() => {
        this.refresh();
        let message = this.$i18n.t("ExperimentList.name") + " " + uri + " " + this.$i18n.t("component.common.success.delete-success-message");
        this.$opensilex.showSuccessToast(message);
      })
      .catch(this.$opensilex.errorHandler);
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

  searchFiltersPannel() {
    return  this.$t("searchfilter.label")
  }
}
</script>


<style scoped lang="scss">
.species-list {
  text-overflow: ellipsis;
  overflow: hidden;
  white-space: nowrap;
  display: inline-block;
  max-width: 40vw;
}
</style>

<i18n>
en:
  ExperimentList:
    name: The experiment
    filter-label: Name
    filter-label-placeholder: Enter a name
    filter-year: Year
    filter-year-placeholder: Enter a year
    filter-species: Species
    filter-species-placeholder: Select one or more species
    filter-facilities: Facilities
    filter-facilities-placeholder: Select one or more facilities
    filter-project: Project
    filter-project-placeholder: Select a project
    filter-state: State
    filter-state-placeholder: Select an experiment state
    filter-factors-categories: Factors categories
    filter-factors-categories-placeholder: Select one or more categories
    selected: Selected experiments
    selected-all: All experiments

fr:
  ExperimentList:
    name: L'expérimentation
    filter-label: Nom
    filter-label-placeholder: Saisir un nom
    filter-year: Année
    filter-year-placeholder: Saisir une année
    filter-species: Espèces
    filter-species-placeholder: Sélectionner une ou plusieurs espèces
    filter-facilities: Installations environnementales
    filter-facilities-placeholder: Sélectionner une ou plusieurs installations
    filter-project: Projet
    filter-project-placeholder: Sélectionner un projet
    filter-state: Etat
    filter-state-placeholder: Sélectionner un état
    filter-factors-categories: Categories de facteurs
    filter-factors-categories-placeholder: Sélectionner une ou plusieurs categories
    selected: Expérimentations selectionnées
    selected-all: Toutes les expérimentations

</i18n>