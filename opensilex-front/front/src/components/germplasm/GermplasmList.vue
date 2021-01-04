<template>
  <div>

    <opensilex-SearchFilterField
      @search="updateFilters()"
      @clear="resetSearch()"
      label="GermplasmList.filter.description"
    >
      <template v-slot:filters>
        <div class="col col-xl-4 col-sm-6 col-12">
          <opensilex-TypeForm
            :type.sync="filter.type"
            :baseType="$opensilex.Oeso.GERMPLASM_TYPE_URI"
            placeholder="GermplasmList.filter.rdfType-placeholder"
          ></opensilex-TypeForm>
        </div>
        <div class="col col-xl-4 col-sm-6 col-12">
          <opensilex-SpeciesSelector
            ref="speciesSelector"
            label="GermplasmList.filter.species"
            placeholder="GermplasmList.filter.species-placeholder"
            :multiple="false"
            :species.sync="filter.fromSpecies"
          ></opensilex-SpeciesSelector>
        </div>
        <div class="col col-xl-4 col-sm-6 col-12">
          <b-form-group>
            <label>{{$t('GermplasmList.filter.year')}}</label>
            <b-input-group>
              <opensilex-StringFilter
                :filter.sync="filter.productionYear"
                placeholder="GermplasmList.filter.year-placeholder"
                type="number"
              ></opensilex-StringFilter>
            </b-input-group>
          </b-form-group>
        </div>
        <div class="col col-xl-4 col-sm-6 col-12">
          <b-form-group>
            <label>{{$t('GermplasmList.filter.institute')}}</label>
            <b-input-group>
              <opensilex-StringFilter
                :filter.sync="filter.institute"
                placeholder="GermplasmList.filter.institute-placeholder"
              ></opensilex-StringFilter>
            </b-input-group>
          </b-form-group>
        </div>
        <div class="col col-xl-4 col-sm-6 col-12">
          <b-form-group>
            <label>{{$t('GermplasmList.filter.label')}}</label>
            <b-input-group>
              <opensilex-StringFilter
                :filter.sync="filter.name"
                placeholder="GermplasmList.filter.label-placeholder"
              ></opensilex-StringFilter>
            </b-input-group>
          </b-form-group>
        </div>
        <div class="col col-xl-4 col-sm-6 col-12">
          <b-form-group>
            <b-input-group>
              <!-- Experiments -->
              <opensilex-ExperimentSelector
                label="GermplasmList.filter.experiment"
                :multiple="false"
                :experiments.sync="filter.experiment"
              ></opensilex-ExperimentSelector>
            </b-input-group>
          </b-form-group>
        </div>           
      </template>       
    </opensilex-SearchFilterField>

    <b-button class="mb-2 mr-2" @click="exportGermplasm()" >{{$t('GermplasmList.export')}}</b-button> 

    <opensilex-TableAsyncView
      ref="tableRef"
      :searchMethod="searchGermplasm"
      :fields="fields"
      :isSelectable="isSelectable"
      defaultSortBy="label"
      labelNumberOfSelectedRow="GermplasmList.selected"
      iconNumberOfSelectedRow="ik#ik-feather"
    >
      <template v-slot:cell(name)="{data}">
        <opensilex-UriLink
          :uri="data.item.uri"
          :value="data.item.name"
          :to="{path: '/germplasm/'+ encodeURIComponent(data.item.uri)}"
        ></opensilex-UriLink>
      </template>

      <template v-slot:row-details></template>

      <!-- <template v-slot:cell(uri)="{data}">
         <a  href="#" class="uri-info primary" @click="$emit('onDetails', data.item.uri)">{{ data.item.uri}}</a> 
        <opensilex-UriLink :uri="data.item.uri"></opensilex-UriLink>
      </template>-->

      <template v-slot:cell(actions)="{data}">
        <b-button-group size="sm">
          <opensilex-EditButton
            v-if="user.hasCredential(credentials.CREDENTIAL_GERMPLASM_MODIFICATION_ID) && !data.item.type.endsWith('Species')"
            @click="$emit('onEdit', data.item.uri)"
            label="GermplasmList.update"
            :small="true"
            
          ></opensilex-EditButton>
          <opensilex-DeleteButton
            v-if="user.hasCredential(credentials.CREDENTIAL_GERMPLASM_DELETE_ID)"
            @click="$emit('onDelete', data.item.uri)"
            label="GermplasmList.delete"
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
import VueRouter from "vue-router";
import {
  GermplasmService,
  GermplasmGetAllDTO,
  OntologyService,
  ResourceTreeDTO,
  ExperimentGetListDTO,
  ExperimentsService
} from "opensilex-core/index";

import Oeso from "../../ontologies/Oeso";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import { GermplasmSearchDTO } from "opensilex-core/index";

@Component
export default class GermplasmList extends Vue {
  $opensilex: any;
  $store: any;
  $route: any;
  $router: VueRouter;
  service: GermplasmService;

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

  get lang() {
    return this.$store.state.lang;
  }

  germplasmTypes = [];
  speciesList = [];
  experimentsList = [];

  filter: GermplasmSearchDTO = {
    type: null,
    name: null,
    fromSpecies: null,
    productionYear: null,
    institute: null,
    experiment: null
  };

  resetSearch() {
    this.resetFilters();
    this.updateFilters();
    this.refresh();
  }

  resetFilters() {
    this.filter = {
      type: null,
      name: null,
      fromSpecies: null,
      productionYear: null,
      institute: null,
      experiment: null
    };
    // Only if search and reset button are use in list
  }

  getSelected() {
    return this.tableRef.getSelected();
  }

  private langUnwatcher;
  mounted() {
    this.langUnwatcher = this.$store.watch(
      () => this.$store.getters.language,
      lang => {
        this.updateLang();
      }
    );
  }

  beforeDestroy() {
    this.langUnwatcher();
  }

  created() {
    this.service = this.$opensilex.getService("opensilex.GermplasmService");
    let query: any = this.$route.query;

    this.resetFilters();
    for (let [key, value] of Object.entries(this.filter)) {
      if (query[key]) {
        this.filter[key] = decodeURIComponent(query[key]);
      }
    }
  }

  updateFilters() {
    for (let [key, value] of Object.entries(this.filter)) {
      this.$opensilex.updateURLParameter(key, value, "");
    }
    this.refresh();
  }

  updateTypeFilter() {
    this.$opensilex.updateURLParameter("type", this.filter.type);
    this.refresh();
  }

  updateSpeciesFilter() {
    this.$opensilex.updateURLParameter("species", this.filter.fromSpecies);
    this.refresh();
  }

  updateYearFilter() {
    let year = null;
    if (this.filter.productionYear != null) {
      year = Number(this.filter.productionYear);
    }
    this.$opensilex.updateURLParameter("year", year);
  }

  updateInstituteFilter() {
    this.$opensilex.updateURLParameter("institute", this.filter.institute);
    this.refresh();
  }

  updateNameFilter() {
    this.$opensilex.updateURLParameter("name", this.filter.name);
    this.refresh();
  }

  updateExperimentFilter() {
    this.$opensilex.updateURLParameter("experiment", this.filter.experiment);
    this.refresh();
  }

  get fields() {
    let tableFields = [
      {
        key: "name",
        label: "GermplasmList.name",
        sortable: false
      },
      {
        key: "typeLabel",
        label: "GermplasmList.rdfType",
        sortable: true
      },
      {
        key: "speciesLabel",
        label: "GermplasmList.speciesLabel"
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

  @Ref("tableRef") readonly tableRef!: any;

  refresh() {
    this.tableRef.refresh();
  }

  searchGermplasm(options) {
    return this.service.searchGermplasm(
      options.orderBy,
      options.currentPage,
      options.pageSize,
      this.filter
    );
  }

  exportGermplasm() {
    let path = "/core/germplasm/exportCSV";
    let today = new Date();
    let filename = "export_germplasm_" + today.getFullYear() + String(today.getMonth() + 1).padStart(2, '0') + String(today.getDate()).padStart(2, '0');
    this.$opensilex
     .downloadFilefromPostService(path, filename, "csv", this.filter, this.lang);
  }


  loadExperiments() {
    let expService: ExperimentsService = this.$opensilex.getService(
      "opensilex.ExperimentsService"
    );

    this.experimentsList = [];
    expService
      .searchExperiments()
      .then(
        (
          http: HttpResponse<OpenSilexResponse<Array<ExperimentGetListDTO>>>
        ) => {
          //console.log(http.response.result)
          for (let i = 0; i < http.response.result.length; i++) {
            let expDTO = http.response.result[i];
            this.experimentsList.push({
              value: expDTO.uri,
              text: expDTO.label
            });
          }
        }
      )
      .catch(this.$opensilex.errorHandler);
  }

  updateLang() {
    this.loadExperiments();
    this.refresh();
  }

  loadGermplasmDetails(data) {
    data.toggleDetails();
  }
}
</script>

<style scoped lang="scss">
.clear-btn {
  color: rgb(229, 227, 227) !important;
  border-color: rgb(229, 227, 227) !important;
  border-left: none !important;
}
</style>

<i18n>

en:
  GermplasmList:
    uri: URI
    name: Name
    rdfType: Type
    fromSpecies: Species URI
    speciesLabel: Species
    update: Update Germplasm
    delete: Delete Germplasm
    selectLabel: Select Germplasm
    selected: Selected Germplasm
    export: Export Germplasm list

    filter:
      description: Germplasm Search
      species: Species
      species-placeholder: Select a species
      year: Production year
      year-placeholder: Enter a year
      institute: Institute code
      institute-placeholder: Enter an institute code
      label: Name
      label-placeholder: Enter germplasm name
      rdfType: Type
      rdfType-placeholder: Select a germplasm type
      experiment: Experiment
      experiment-placeholder: Select an experiment
      search: Search
      reset: Reset

fr:
  GermplasmList:
    uri: URI
    label: Nom
    rdfType: Type
    fromSpecies: URI de l'espèce
    speciesLabel: Espèce
    update: Editer le germplasm
    delete: Supprimer le germplasm
    selectLabel: Sélection de Matériel Génétiques
    selected: Matériel Génétique(s) Sélectionné(s)
    export: Exporter la liste

    filter:
      description: Recherche de Ressources Génétiques
      species: Espèce
      species-placeholder: Sélectionner une espèce
      year: Année de production
      year-placeholder: Entrer une année
      institute: Code Institut
      institute-placeholder: Entrer le code d'un institut
      label: Nom
      label-placeholder: Entrer un nom de germplasm
      rdfType: Type
      rdfType-placeholder: Sélectionner un type de germplasm
      experiment: Expérimentation
      experiment-placeholder: Sélectionner une expérimentation
      search: Rechercher
      reset: Réinitialiser      
  
</i18n>
