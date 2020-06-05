<template>

  <b-modal ref="modalRef" size="xl" :static="true">
    
    <template v-slot:modal-title>
      <i class="ik ik-search mr-1"></i> {{ $t('GermplasmList.selectLabel') }}
    </template>

    <template v-slot:modal-footer>
      <button type="button" class="btn btn-secondary" v-on:click="hide(false)">{{ $t('component.common.close') }}</button>
      <button type="button" class="btn btn-primary" v-on:click="hide(true)">{{ $t('component.common.validateSelection') }}</button>
    </template>

    <div class="card">

      <opensilex-SearchForm
        labelTitle="component.experiment.search.label"
        :resetMethod="resetFilters"
        :searchMethod="refresh"
        :showTitle="false"
      >
            
        <template v-slot:standardSearch>
            
          <!-- Type -->
          <div class="filter-group col col-xl-3 col-sm-6 col-12">
            <opensilex-SelectForm
              label="GermplasmList.filter.rdfType"
              placeholder="component.germplasm.filter.allTypes"
              :selected.sync="filterByRdfType"
              :optionsLoadingMethod="loadGermplasmTypes"
              :conversionMethod="ontologyToSelectNode"
            ></opensilex-SelectForm>
          </div>

          <!-- Species -->
          <div class="filter-group col col-xl-3 col-sm-6 col-12">
            <opensilex-SelectForm
              label="GermplasmList.filter.species"
              :selected.sync="filterBySpecies"
              :optionsLoadingMethod="loadSpecies"
              :conversionMethod="germplasmToSelectNode"
            ></opensilex-SelectForm>
          </div>

          <!-- Institute -->
          <div class="filter-group col col-xl-3 col-sm-6 col-12">
            <opensilex-InputForm
              :value.sync="filterByInstitute"
              label="GermplasmList.filter.institute"
              type="text"
            ></opensilex-InputForm>
          </div>
            
          <!-- Label -->
          <div class="filter-group col col-xl-3 col-sm-6 col-12">
            <opensilex-InputForm
              :value.sync="filterByLabel"
              label="GermplasmList.filter.label"
              type="text"
            ></opensilex-InputForm>
          </div>

        </template>
            
      </opensilex-SearchForm>

      <opensilex-TableAsyncView
        ref="tableRef"
        :searchMethod="searchGermplasm"
        :fields="fields"
        defaultSortBy="name"
        isSelectable="true"
        defaultPageSize="10"
        labelNumberOfSelectedRow="GermplasmList.selected"
        iconNumberOfSelectedRow="ik#ik-feather"
      >

        <template v-slot:cell(uri)="{data}">
            <opensilex-UriLink
            :uri="data.item.uri"
            ></opensilex-UriLink>
        </template>

      </opensilex-TableAsyncView>

    </div>

    </b-modal>

</template>

<script lang="ts">
import { Component } from "vue-property-decorator";
import Vue from "vue";
import VueRouter from "vue-router";
import { 
  GermplasmService,
  GermplasmGetAllDTO,
  GermplasmSearchDTO,
  ResourceTreeDTO
} 
from "opensilex-core/index";

import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import GermplasmList from './GermplasmList.vue';
import Oeso from "../../ontologies/Oeso";

@Component
export default class GermplasmModalList extends GermplasmList {

  fields = [
    {
      key: "uri",
      label: "GermplasmList.uri",
      sortable: true
    },
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
    {
      key: "speciesLabel",
      label: "GermplasmList.speciesLabel",
      sortable: true
    }
  ];

  show() {
      let modalRef: any = this.$refs.modalRef;
      modalRef.show();
  }

  hide(validate: boolean) {
      let modalRef: any = this.$refs.modalRef;
      modalRef.hide();

      if(validate) {
          this.$emit("onValidate", this.tableRef.getSelected());
      }
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
    return this.service
      .searchGermplasm(undefined, undefined, 100, germplasmPostDTO)
      .then((http: HttpResponse<OpenSilexResponse<Array<GermplasmGetAllDTO>>>) => http.response.result);
  }

  germplasmToSelectNode(dto: GermplasmGetAllDTO) {
    return {
      id: dto.uri,
      label: dto.label
    };
  }

  loadGermplasmTypes(){
    return this.$opensilex.getService("opensilex.OntologyService")
      .getSubClassesOf(Oeso.GERMPLASM_TYPE_URI,true)
      .then((http: HttpResponse<OpenSilexResponse<Array<ResourceTreeDTO>>>) => http.response.result);
  }

  ontologyToSelectNode(dto: ResourceTreeDTO) {
      return {
          id: dto.uri,
          label: dto.name
      };
  }
  
}
</script>

<style scoped lang="scss">
</style>
