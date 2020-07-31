<template>
  <div>
    <opensilex-SearchFilterField
      @search="updateFilters()"
      @clear="clearFilters()"
      label="GermplasmList.filter.description"
    >
      <template v-slot:filters>
        <b-row class="ml-2">
          <opensilex-FilterField>
            <b-form-group>
              <label>{{$t('GermplasmList.filter.rdfType')}}</label>   
              <b-input-group>      
                <b-select
                  v-model="filterByRdfType"
                  :options="germplasmTypes"
                >
                  <template v-slot:first>
                    <b-form-select-option :value="null">{{$t('GermplasmList.filter.allTypes')}}</b-form-select-option>
                  </template>
                </b-select>
                <template v-slot:append>
                  <b-btn variant="primary" @click="filterByRdfType = null">
                    <font-awesome-icon icon="times" size="sm" />
                  </b-btn>
                </template>
              </b-input-group>
            </b-form-group>
          </opensilex-FilterField>

          <opensilex-FilterField>
            <b-form-group>
              <label>{{$t('GermplasmList.filter.species')}}</label>   
              <b-input-group>
                <b-select
                  v-model="filterBySpecies"
                  :options="speciesList"
                >
                  <template v-slot:first>
                    <b-form-select-option :value="null"></b-form-select-option>
                  </template>
                </b-select>
                <template v-slot:append>
                  <b-btn variant="primary" @click="filterBySpecies = null">
                    <font-awesome-icon icon="times" size="sm" />
                  </b-btn>
                </template>            
              </b-input-group>
            </b-form-group>
          </opensilex-FilterField>

          <opensilex-FilterField>
            <b-form-group>
              <label>{{$t('GermplasmList.filter.year')}}</label>   
              <b-input-group>
                <b-form-input
                  v-model="filterByYear"
                  type="number"
                  step=1
                  debounce="1000"
                  default=null
                ></b-form-input>
                <template v-slot:append>
                  <b-btn variant="primary" @click="filterByYear= null">
                    <font-awesome-icon icon="times" size="sm" />
                  </b-btn>
                </template>
              </b-input-group>
            </b-form-group>
          </opensilex-FilterField>

          <opensilex-FilterField>
            <b-form-group>
              <label>{{$t('GermplasmList.filter.institute')}}</label>   
              <b-input-group>
                <b-form-input
                  v-model="filterByInstitute"
                  type="text"
                ></b-form-input>
                <template v-slot:append>
                  <b-btn variant="primary" @click="filterByInstitute=null">
                    <font-awesome-icon icon="times" size="sm" />
                  </b-btn>
                </template>
              </b-input-group>
            </b-form-group>
          </opensilex-FilterField>
            
          <opensilex-FilterField>
            <b-form-group>
              <label>{{$t('GermplasmList.filter.label')}}</label>   
              <b-input-group>  
                <b-form-input
                  v-model="filterByLabel"
                  debounce="300"
                ></b-form-input>
                <template v-slot:append>
                  <b-btn variant="primary" @click="filterByLabel = null">
                    <font-awesome-icon icon="times" size="sm" />
                  </b-btn>
                </template>
              </b-input-group>
            </b-form-group>
          </opensilex-FilterField>

          <opensilex-FilterField>
            <b-form-group>
              <label>{{$t('GermplasmList.filter.experiment')}}</label>   
              <b-input-group>
                <b-select
                  v-model="filterByExperiment"
                  :options="experimentsList"
                >
                  <template v-slot:first>
                    <b-form-select-option :value="null"></b-form-select-option>
                  </template>
                </b-select>
                <template v-slot:append>
                  <b-btn variant="primary" @click="filterByExperiment = null">
                    <font-awesome-icon icon="times" size="sm" />
                  </b-btn>
                </template>            
              </b-input-group>
            </b-form-group>
          </opensilex-FilterField>
        </b-row>
      </template>

    </opensilex-SearchFilterField>
    <opensilex-TableAsyncView
      ref="tableRef"
      :searchMethod="searchGermplasm"
      :fields="fields"
      defaultSortBy="label"
    >
      <template v-slot:cell(label)="{data}">
        <opensilex-UriLink
          :uri="data.item.uri"
          :value="data.item.label"
          :to="{path: '/germplasm/'+ encodeURIComponent(data.item.uri)}"
        ></opensilex-UriLink>
      </template>

      <template v-slot:row-details>
      </template>

      <!-- <template v-slot:cell(uri)="{data}">
         <a  href="#" class="uri-info primary" @click="$emit('onDetails', data.item.uri)">{{ data.item.uri}}</a> 
        <opensilex-UriLink :uri="data.item.uri"></opensilex-UriLink>
      </template>       -->

      <template v-slot:cell(actions)="{data}">
        <b-button-group size="sm">
          <opensilex-EditButton
            v-if="user.hasCredential(credentials.CREDENTIAL_GERMPLASM_MODIFICATION_ID)"
            @click="$emit('onEdit', data.item.uri)"
            label="GermplasmList.update"
            :small="true"
          ></opensilex-EditButton>
          <opensilex-DeleteButton
            v-if="user.hasCredential(credentials.CREDENTIAL_GERMPLASM_DELETE_ID)"
            @click="deleteGermplasm(data.item.uri)"
            label="GermplasmList.delete"
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
import { 
  GermplasmService,
  GermplasmGetAllDTO,
  OntologyService,
  ResourceTreeDTO,
  ExperimentGetListDTO,
  ExperimentsService
} 
from "opensilex-core/index";

import Oeso from "../../ontologies/Oeso";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import {GermplasmSearchDTO} from "opensilex-core/index"; 

@Component
export default class GermplasmList extends Vue {
  $opensilex: any;
  $store: any;
  $router: VueRouter;
  service: GermplasmService;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  germplasmTypes = [];
  speciesList = [];
  years = []
  experimentsList = [];
  selected = null;

  private searchForm: GermplasmSearchDTO = {
    "rdfType": null,
    "label": null,
    "fromSpecies": null,
    "productionYear": null,
    "institute": null
  };

  mounted() {
    this.$store.watch(
      () => this.$store.getters.language,
      lang => {
        this.updateLang();
      }
    );
  }

  private filter: any = "";

  created() {
    this.service = this.$opensilex.getService("opensilex.GermplasmService");
    this.$opensilex.disableLoader();
    this.loadGermplasmTypes();
    this.loadSpecies();
    this.loadExperiments();
    this.loadYears();
    this.$opensilex.enableLoader();
    
  }

  updateFilters() {
    this.$opensilex.updateURLParameter("filter", this.filter, "");
    this.refresh();
  }

  set filterByLabel(value: string) {
    this.searchForm.label = value;
  }

  get filterByLabel() {
    return this.searchForm.label;
  }

  set filterByRdfType(value: string) {
      this.searchForm.rdfType = value;
  }

  get filterByRdfType() {
    return this.searchForm.rdfType;
  }

  set filterBySpecies(value: string) {
    this.searchForm.fromSpecies = value;
  }

  get filterBySpecies() {
    return this.searchForm.fromSpecies;
  }

  set filterByYear(value: number) {
    if ((value>1980 && value<=new Date().getFullYear()) || value == null) {
      this.searchForm.productionYear = value;
    }    
  }

  get filterByYear() {
    if (this.searchForm.productionYear != null) {
      return Number(this.searchForm.productionYear);
    } else {
      return null;
    }
  }

  set filterByInstitute(value: string) {
    this.searchForm.institute = value;
  }

  get filterByInstitute() {
    return this.searchForm.institute;
  }

  set filterByExperiment(value: string) {
    this.searchForm.experiment = value;
  }

  get filterByExperiment() {
    return this.searchForm.experiment;
  }
  
  fields = [
    // {
    //   key: "uri",
    //   label: "GermplasmList.uri",
    //   sortable: true
    // },
    {
      key: "label",
      label: "GermplasmList.label",
      sortable: true
    },
    {
      key: "typeLabel",
      label: "GermplasmList.rdfType",
      sortable: true
    },
    
    // {
    //   key: "fromSpecies",
    //   label: "component.germplasm.list.fromSpecies",
    //   sortable: true
    // },
    {
      key: "speciesLabel",
      label: "GermplasmList.speciesLabel",
      //sortable: true
    },
    {
      key: "actions",
      label: "component.common.actions"
    }
  ];

  @Ref("tableRef") readonly tableRef!: any;

  refresh() {
    this.tableRef.refresh();
  }

  searchGermplasm(options) {
    return this.service.searchGermplasm(
      options.orderBy,
      options.currentPage,
      options.pageSize,
      this.searchForm
    );
  }  

  loadGermplasmTypes(){
    this.germplasmTypes = [];
    let ontoService: OntologyService = this.$opensilex.getService("opensilex.OntologyService");
    ontoService.getSubClassesOf(Oeso.GERMPLASM_TYPE_URI,true)
    .then((http: HttpResponse<OpenSilexResponse<Array<ResourceTreeDTO>>>) => {
      for(let i=0; i<http.response.result.length; i++) {
        let resourceDTO = http.response.result[i];
        if (resourceDTO.uri.endsWith("PlantMaterialLot")) {
          let children = resourceDTO.children;
          for (let j=0; j<children.length; j++) {
            this.germplasmTypes.push({
              value: children[j].uri,
              text: children[j].name
            });
          }
        } else {
          this.germplasmTypes.push({
              value: resourceDTO.uri,
              text: resourceDTO.name
          });
        }
      }
      //this.$opensilex.enableLoader();
    }).catch(this.$opensilex.errorHandler);
  
  }

  loadSpecies(){

    let germplasmPostDTO: GermplasmSearchDTO = {
      uri: null,
      rdfType: "vocabulary:Species",
      label: null,
      fromSpecies: null,
      fromVariety: null,
      fromAccession: null
    }

    this.speciesList = [];
    this.service
    .searchGermplasm(undefined, undefined,100, germplasmPostDTO)
    .then((http: HttpResponse<OpenSilexResponse<Array<GermplasmGetAllDTO>>>) => {
      //console.log(http.response.result)
        for(let i=0; i<http.response.result.length; i++) {
          let germplasmDTO = http.response.result[i];          
          this.speciesList.push({
              value: germplasmDTO.uri,
              text: germplasmDTO.label
          });
        }     
    }).catch(this.$opensilex.errorHandler);
  }

  loadYears () {
    const year = new Date().getFullYear()
    this.years = Array.from({length: 20}, (value, index) => (year - index).toString())
  }

  loadExperiments(){
    let expService: ExperimentsService = this.$opensilex.getService("opensilex.ExperimentsService");

    this.experimentsList = [];
    expService
    .searchExperiments()
    .then((http: HttpResponse<OpenSilexResponse<Array<ExperimentGetListDTO>>>) => {
      //console.log(http.response.result)
        for(let i=0; i<http.response.result.length; i++) {
          let expDTO = http.response.result[i];          
          this.experimentsList.push({
              value: expDTO.uri,
              text: expDTO.label
          });
        }     
    }).catch(this.$opensilex.errorHandler);
  }


  updateLang() {
    this.loadGermplasmTypes();
    this.loadSpecies();
    this.loadExperiments();
    this.refresh();
  }

  clearFilters() {
    this.filterByLabel = null;
    this.filterByRdfType = null;
    this.filterBySpecies = null;
    this.filterByYear = null;
    this.filterByInstitute = null;
    this.filterByExperiment = null;
    this.refresh();
  }

  loadGermplasmDetails(data) {
    data.toggleDetails();
  }


  deleteGermplasm(uri: string) {
    this.service
      .deleteGermplasm(uri)
      .then(() => {
        this.refresh();
        this.$emit("onDelete", uri);
      })
      .catch(this.$opensilex.errorHandler);
  }

}
</script>

<style scoped lang="scss">
</style>

<i18n>

en:
  GermplasmList:
    uri: URI
    label: Name
    rdfType: Type
    fromSpecies: Species URI
    speciesLabel: Species
    update: Update Germplasm
    delete: Delete Germplasm

    filter:
      description: Germplasm Search
      allTypes: All types
      species: Filter by species
      year: Filter by production year
      institute: Filter by institute
      label: Filter by name
      label-placeholder: Enter germplasm name
      rdfType: Filter by type
      experiment: Filter by experiment
      search: Search
      reset: Reset

fr:
  GermplasmList:
    uri: URI
    label: Nom
    rdfType: Type
    fromSpecies: URI de l'espèce
    speciesLabel: Espèce
    update: éditer germplasm
    delete: supprimer germplasm

    filter:
      description: Recherche de Ressources Génétiques
      allTypes: Tous
      species: Filtrer par espèce
      year: Filtrer par année de production
      institute: Filtrer par institut
      label: Filtrer par nom
      label-placeholder: Entrez un nom de germplasm
      rdfType: Filtrer par type
      experiment: Filtrer par expérimentation
      search: Rechercher
      reset: Réinitialiser
  
</i18n>
