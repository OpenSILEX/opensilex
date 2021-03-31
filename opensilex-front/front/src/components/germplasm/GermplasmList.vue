<template>
  <div>

    <opensilex-SearchFilterField
      @search="refresh()"
      @clear="resetSearch()"
      label="GermplasmList.filter.description"
      :showAdvancedSearch="true"
    >
      <template v-slot:filters>
        <!-- Type -->
        <opensilex-FilterField>
          <opensilex-TypeForm
            :type.sync="filter.rdf_type"
            :baseType="$opensilex.Oeso.GERMPLASM_TYPE_URI"
            placeholder="GermplasmList.filter.rdfType-placeholder"
          ></opensilex-TypeForm>
        </opensilex-FilterField>
        
        <!-- Species -->
        <opensilex-FilterField>
          <opensilex-SelectForm
            label="GermplasmList.filter.species"
            placeholder="GermplasmList.filter.species-placeholder"
            :multiple="false"
            :selected.sync="filter.species"
            :options="species"
          ></opensilex-SelectForm>
        </opensilex-FilterField>

        <!-- Year -->
        <opensilex-FilterField>
          <label>{{$t('GermplasmList.filter.year')}}</label>
          <opensilex-StringFilter
            :filter.sync="filter.production_year"
            placeholder="GermplasmList.filter.year-placeholder"
            type="number"
          ></opensilex-StringFilter>
        </opensilex-FilterField>

        <!-- Institute -->
        <opensilex-FilterField>
          <label>{{$t('GermplasmList.filter.institute')}}</label>
          <opensilex-StringFilter
            :filter.sync="filter.institute"
            placeholder="GermplasmList.filter.institute-placeholder"
          ></opensilex-StringFilter>
        </opensilex-FilterField>

        <!-- Name -->
        <opensilex-FilterField>
          <label>{{$t('GermplasmList.filter.label')}}</label>
          <opensilex-StringFilter
            :filter.sync="filter.name"
            placeholder="GermplasmList.filter.label-placeholder"
          ></opensilex-StringFilter>
        </opensilex-FilterField>

        <!-- Experiments -->
        <opensilex-FilterField>
          <opensilex-ExperimentSelector
            label="GermplasmList.filter.experiment"
            :multiple="false"
            :experiments.sync="filter.experiment"
          ></opensilex-ExperimentSelector>
        </opensilex-FilterField> 

        <!-- URI -->
        <opensilex-FilterField>
          <label>{{$t('GermplasmList.filter.uri')}}</label>
          <opensilex-StringFilter
            :filter.sync="filter.uri"
            placeholder="GermplasmList.filter.uri-placeholder"
          ></opensilex-StringFilter>
        </opensilex-FilterField>        
      </template>  

      <template v-slot:advancedSearch>
        <opensilex-FilterField>
          <opensilex-StringFilter
            :filter.sync="filter.metadataKey"
            label="GermplasmList.filter.metadataKey"
          ></opensilex-StringFilter>
        </opensilex-FilterField>
        <opensilex-FilterField>
          <opensilex-StringFilter
            :filter.sync="filter.metadataValue"
            label="GermplasmList.filter.metadataValue"
          ></opensilex-StringFilter>
        </opensilex-FilterField>
      </template>     
      
    </opensilex-SearchFilterField>

    <opensilex-TableAsyncView
      ref="tableRef"
      :searchMethod="searchGermplasm"
      :fields="fields"
      :isSelectable="true"
      defaultSortBy="name"
      labelNumberOfSelectedRow="GermplasmList.selected"
      iconNumberOfSelectedRow="ik#ik-feather"
    >
      <template v-slot:selectableTableButtons="{ numberOfSelectedRows }">
        <b-dropdown
          dropright
          class="mb-2 mr-2"
          :small="true"
          :disabled="numberOfSelectedRows == 0"
          text=actions>
            <b-dropdown-item-button    
              @click="createDocument()"
            >{{$t('component.common.addDocument')}}</b-dropdown-item-button>
            <b-dropdown-item-button
              @click="exportGermplasm()"
          >{{$t('GermplasmList.export')}}</b-dropdown-item-button>
        </b-dropdown>
      </template>
      <template v-slot:cell(name)="{data}">
        <opensilex-UriLink
          :uri="data.item.uri"
          :value="data.item.name"
          :to="{path: '/germplasm/details/'+ encodeURIComponent(data.item.uri)}"
        ></opensilex-UriLink>
      </template>

      <template v-slot:cell(actions)="{data}">
        <b-button-group size="sm">
          <opensilex-EditButton
            v-if="user.hasCredential(credentials.CREDENTIAL_GERMPLASM_MODIFICATION_ID) && !data.item.rdf_type.endsWith('Species')"
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
    <opensilex-ModalForm
      v-if="user.hasCredential(credentials.CREDENTIAL_GERMPLASM_MODIFICATION_ID)"
      ref="documentForm"
      component="opensilex-DocumentForm"
      createTitle="component.common.addDocument"
      modalSize="lg"
      :initForm="initForm"
      icon="ik#ik-settings"
    ></opensilex-ModalForm>
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
  ExperimentsService,
  SpeciesService,
  SpeciesDTO
} from "opensilex-core/index";

import Oeso from "../../ontologies/Oeso";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";

@Component
export default class GermplasmList extends Vue {
  $opensilex: any;
  $store: any;
  $route: any;
  $router: VueRouter;
  service: GermplasmService;

  @Ref("documentForm") readonly documentForm!: any;
  @Ref("tableRef") readonly tableRef!: any;

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
  species = [];
  speciesByUri: Map<String, SpeciesDTO> = new Map<String, SpeciesDTO>();
  experimentsList = [];

  exportPath = "/core/germplasm/export";

  filter = {
    rdf_type: undefined,
    name: undefined,
    species: undefined,
    production_year: undefined,
    institute: undefined,
    experiment: undefined,
    uri: undefined,
    metadataKey: undefined,
    metadataValue: undefined
  };

  // exportFilter = {
  //   rdf_type: undefined,
  //   name: undefined,
  //   species: undefined,
  //   production_year: undefined,
  //   institute: undefined,
  //   experiment: undefined,
  //   uri: undefined,
  //   metadata: undefined
  // };

  resetSearch() {
    this.resetFilters();
    //this.updateFilters();
    this.refresh()
  }

  resetFilters() {
    this.filter = {
      rdf_type: undefined,
      name: undefined,
      species: undefined,
      production_year: undefined,
      institute: undefined,
      experiment: undefined,
      uri: undefined,
      metadataKey: undefined,
      metadataValue: undefined
    };
    // this.exportFilter = {
    //   rdf_type: undefined,
    //   name: undefined,
    //   species: undefined,
    //   production_year: undefined,
    //   institute: undefined,
    //   experiment: undefined,
    //   uri: undefined,
    //   metadata: undefined
    // };
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
    this.loadSpecies();

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
    
  }

  get fields() {
    let tableFields = [
      {
        key: "name",
        label: "GermplasmList.name",
        sortable: false
      },
      {
        key: "rdf_type_name",
        label: "GermplasmList.rdfType",
        sortable: true
      },
      {
        key: "species_name",
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

  @Ref("speciesSelector") readonly speciesSelector!: any;

  refresh() {
    this.tableRef.selectAll = false;
    this.tableRef.onSelectAll();
    this.updateFilters();
    this.tableRef.refresh();
  }

  searchGermplasm(options) {
    // this.updateExportFilters();
    return this.service.searchGermplasm(
      this.filter.uri,
      this.filter.rdf_type,
      this.filter.name,
      undefined,
      this.filter.production_year,
      this.filter.species,
      undefined,
      undefined,
      this.filter.institute,
      this.filter.experiment,
      this.addMetadataFilter(),
      options.orderBy,
      options.currentPage,
      options.pageSize
    );
  }

  exportGermplasm() {
    let path = "/core/germplasm/export_by_uris";
    let today = new Date();
    let filename = "export_germplasm_" + today.getFullYear() + String(today.getMonth() + 1).padStart(2, '0') + String(today.getDate()).padStart(2, '0');
    var exportList = []
    for (let select of this.tableRef.getSelected()) {
      exportList.push(select.uri);
    }
    this.$opensilex
     .downloadFilefromPostService(path, filename, "csv", {uris: exportList}, this.lang);
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
              text: expDTO.name
            });
          }
        }
      )
      .catch(this.$opensilex.errorHandler);
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
            label: http.response.result[i].name
          });
        }
      })
      .catch(this.$opensilex.errorHandler);
  }

  updateLang() {
    this.loadExperiments();
    this.loadSpecies();
    this.refresh();
  }

  loadGermplasmDetails(data) {
    data.toggleDetails();
  }

  addMetadataFilter() {
    let metadata = undefined;
    if (this.filter.metadataKey != undefined && this.filter.metadataKey != ""
    && this.filter.metadataValue != undefined && this.filter.metadataValue != "") {
      metadata = '{"' + this.filter.metadataKey + '":"' + this.filter.metadataValue + '"}'
      return metadata;
    }
  }

  // INFO: function used for web service exporting from search filters
  // updateExportFilters() {
  //   this.exportFilter.rdf_type = this.filter.rdf_type;
  //   this.exportFilter.name = this.filter.name;
  //   this.exportFilter.species = this.filter.species;
  //   this.exportFilter.production_year = this.filter.production_year;
  //   this.exportFilter.institute = this.filter.institute;
  //   this.exportFilter.experiment = this.filter.experiment;
  //   this.exportFilter.uri = this.filter.uri;
  //   this.exportFilter.metadata = this.addMetadataFilter();
  // }
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
      uri: URI
      uri-placeholder: Enter a part of an uri
      search: Search
      reset: Reset
      metadataKey: Attribute name
      metadataValue: Attribute value

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
      uri: URI
      uri-placeholder: Entrer une partie d'une uri
      search: Rechercher
      reset: Réinitialiser
      metadataKey: Nom de l'attribut
      metadataValue: Valeur de l'attribut
  
</i18n>
