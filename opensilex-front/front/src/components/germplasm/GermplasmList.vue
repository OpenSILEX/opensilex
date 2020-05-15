<template>
  <div>
    <div class="card-vertical-group">
      <div class="card">
        <div class="card-header">
          <h3 class="mr-3">
            <i class="ik ik-search"></i>{{$t('component.germplasm.filter.description')}}
          </h3>
        </div>
        <div class="card-body row"> 
          <div class="filter-group col col-xl-3 col-sm-6 col-12">                                        
            <b-form-group>
              <label>{{$t('component.germplasm.filter.rdfType')}}</label>   
              <b-input-group>      
                <b-select
                  v-model="filterByRdfType"
                  :options="germplasmTypes"
                >
                  <template v-slot:first>
                    <b-form-select-option :value="null">{{$t('component.germplasm.filter.allTypes')}}</b-form-select-option>
                  </template>
                </b-select>
                <template v-slot:append>
                  <b-btn variant="primary" @click="filterByRdfType = null">
                    <font-awesome-icon icon="times" size="sm" />
                  </b-btn>
                </template>
              </b-input-group>
            </b-form-group>
          </div>
          <div class="filter-group col col-xl-3 col-sm-6 col-12">
            <b-form-group>
              <label>{{$t('component.germplasm.filter.species')}}</label>   
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
          </div>
          <div class="filter-group col col-xl-3 col-sm-6 col-12">
            <b-form-group>
              <label>{{$t('component.germplasm.filter.year')}}</label>   
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
          </div>
          <div class="filter-group col col-xl-3 col-sm-6 col-12">
            <b-form-group>
              <label>{{$t('component.germplasm.filter.institute')}}</label>   
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
          </div>
          <div class="filter-group col col-xl-3 col-sm-6 col-12">
            <b-form-group>
              <label>{{$t('component.germplasm.filter.label')}}</label>   
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
          </div>
          <div class="filter-group col col-xl-3 col-sm-6 col-12">
            <b-form-group>
              <label>{{$t('component.germplasm.filter.experiment')}}</label>   
              <b-input-group>  
                <b-form-input disabled
                ></b-form-input>
                <template v-slot:append>
                  <b-btn variant="primary" disabled>
                    <font-awesome-icon icon="times" size="sm" />
                  </b-btn>
                </template>
              </b-input-group>
            </b-form-group>
          </div>    
        </div>
        <div>
          <button type="button" class="btn btn-primary float-right mb-2 mr-2" @click="refresh">
            <i class="ik ik-search"></i>{{$t('component.germplasm.filter.search')}}
          </button>
          <button type="button" class="btn btn-light float-right mb-2 mr-2" @click="resetFilters">
            <i class="ik ik-x"></i>{{$t('component.germplasm.filter.reset')}}
          </button>          
        </div> 
      </div>
    </div>
    <opensilex-TableAsyncView
      ref="tableRef"
      :searchMethod="searchGermplasm"
      :fields="fields"
      defaultSortBy="label"
    >
      <template v-slot:cell(uri)="{data}">
        <opensilex-UriLink :uri="data.item.uri"></opensilex-UriLink>
      </template>

      <template v-slot:row-details>
      </template>

      <!-- <template v-slot:cell(uri)="{data}">
         <a  href="#" class="uri-info primary" @click="$emit('onDetails', data.item.uri)">{{ data.item.uri}}</a> 
        <opensilex-UriLink :uri="data.item.uri"></opensilex-UriLink>
      </template>       -->

      <template v-slot:cell(actions)="{data}">
        <b-button-group size="sm">
          <opensilex-Button size="sm" icon="fa#eye" @click="$emit('onDetails', data.item.uri)" variant="outline-success">
          </opensilex-Button> 
          <!-- <opensilex-DetailButton
            @click="loadGermplasmDetails(data)"
            label="component.germplasm.details"
            :detailVisible="data.detailsShowing"
            :small="true"
          ></opensilex-DetailButton> -->
          <opensilex-EditButton
            v-if="user.hasCredential(credentials.CREDENTIAL_GERMPLASM_MODIFICATION_ID)"
            @click="$emit('onEdit', data.item)"
            label="component.germplasm.update"
            :small="true"
          ></opensilex-EditButton>
          <opensilex-DeleteButton
            v-if="user.hasCredential(credentials.CREDENTIAL_GERMPLASM_DELETE_ID)"
            @click="deleteGermplasm(data.item.uri)"
            label="component.germplasm.delete"
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
  GermplasmGetDTO,
  OntologyService,
  ResourceTreeDTO
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
    this.loadYears();
    this.$opensilex.enableLoader();
    
  }

  updateFilter() {
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
    //this.refresh();
  }

  get filterByInstitute() {
    return this.searchForm.institute;
  }

  
  // updateFilter() {
  //   this.$opensilex.updateURLParameter("filter", this.filter, "");
  //   this.refresh();
  // }

  fields = [
    {
      key: "uri",
      label: "component.germplasm.list.uri",
      sortable: true
    },
    {
      key: "typeLabel",
      label: "component.germplasm.list.rdfType",
      sortable: true
    },
    {
      key: "label",
      label: "component.germplasm.list.label",
      sortable: true
    },
    // {
    //   key: "fromSpecies",
    //   label: "component.germplasm.list.fromSpecies",
    //   sortable: true
    // },
    {
      key: "speciesLabel",
      label: "component.germplasm.list.speciesLabel",
      sortable: true
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
    .then((http: HttpResponse<OpenSilexResponse<Array<GermplasmGetDTO>>>) => {
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

  updateLang() {
    this.loadGermplasmTypes();
    this.loadSpecies();
    this.refresh();
  }

  resetFilters() {
    this.filterByLabel = null;
    this.filterByRdfType = null;
    this.filterBySpecies = null;
    this.filterByYear = null;
    this.filterByInstitute = null;
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
