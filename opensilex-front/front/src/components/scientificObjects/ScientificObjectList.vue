<template>
  <div class="container-fluid">
    <opensilex-PageHeader
      icon="ik#ik-target"
      title="component.menu.scientificObjects"
      description="ScientificObjectsList.description"
    ></opensilex-PageHeader>

    <opensilex-SearchFilterField
      @search="refresh()"
      @clear="reset()"
      label="ScientificObjectsList.filter.label"
      advancedSearchLabel="ScientificObjectsList.advancedSearch"
      :showAdvancedSearch="true"
    >
      <template v-slot:filters>
        <!-- Experiments -->
        <opensilex-FilterField>
          <opensilex-SelectForm
            label="ScientificObjectsList.filter.experiments"
            placeholder="ScientificObjectsList.placeholder.experiments"
            :selected.sync="filter.experiments"
            :conversionMethod="experimentGetListDTOToSelectNode"
            modalComponent="opensilex-ExperimentModalList"
            :isModalSearch="true"
          ></opensilex-SelectForm>
        </opensilex-FilterField>

        <!-- URI -->
        <opensilex-FilterField>
          <opensilex-InputForm
            :value.sync="filter.label"
            label="ScientificObjectsList.filter.uri"
            type="text"
            placeholder="ScientificObjectsList.placeholder.uri"
          ></opensilex-InputForm>
        </opensilex-FilterField>
      </template>

      <template v-slot:advancedSearch>
        <div class="row">
          <!-- Germplasm -->
          <opensilex-FilterField>
            <opensilex-SelectForm
              label="ScientificObjectsList.filter.germplasm"
              placeholder="ScientificObjectsList.placeholder.germplasm"
              :selected.sync="filter.germplasm"
              :conversionMethod="germplasmGetDTOToSelectNode"
              modalComponent="opensilex-GermplasmModalList"
              :isModalSearch="true"
            ></opensilex-SelectForm>
          </opensilex-FilterField>

          <!-- Position -->
          <opensilex-FilterField>
            <opensilex-SelectForm
              label="ScientificObjectsList.filter.position"
              placeholder="ScientificObjectsList.placeholder.position"
              :selected.sync="filter.positions"
              :conversionMethod="featureToSelectNode"
              modalComponent="opensilex-GeometrySelector"
              :isModalSearch="true"
            ></opensilex-SelectForm>
          </opensilex-FilterField>

          <!-- Dates -->
          <opensilex-FilterField>
            <opensilex-DateRangePickerForm
              :value.sync="filter.dates"
              label="ScientificObjectsList.filter.dates"
            ></opensilex-DateRangePickerForm>
          </opensilex-FilterField>

          <!-- Factors -->
          <opensilex-FilterField>
            <opensilex-SelectForm
              label="ScientificObjectsList.filter.factors"
              placeholder="ScientificObjectsList.placeholder.factors"
              :selected.sync="filter.factors"
              :conversionMethod="factorGetDTOToSelectNode"
              modalComponent="opensilex-FactorModalList"
              :isModalSearch="true"
            ></opensilex-SelectForm>
          </opensilex-FilterField>

          <!-- isPartOf -->
          <div class="filter-group col col-xl-3 col-sm-6 col-12">
            <opensilex-InputForm
              :value.sync="filter.isPartOf"
              label="ScientificObjectsList.filter.isPartOf"
              type="text"
              placeholder="ScientificObjectsList.placeholder.isPartOf"
            ></opensilex-InputForm>
          </div>
        </div>
      </template>
    </opensilex-SearchFilterField>

    <opensilex-PageContent>
      <template v-slot>
        <opensilex-TableAsyncView
          ref="tableRef"
          :searchMethod="searchScientificObject"
          :fields="fields"
          defaultSortBy="label"
          :isSelectable="true"
          labelNumberOfSelectedRow="ScientificObjectsList.selected"
          iconNumberOfSelectedRow="ik#ik-target"
        >
          <template v-slot:firstActionsSelectableTable>
            <div
              v-if="tableRef.getSelected().length > 0"
              class="card-options d-inline-block"
            >
              <a
                href="data.html"
                class="btn btn-icon btn-outline-primary"
                title="Voir les données"
                ><i class="ik ik ik-bar-chart-line-"></i
              ></a>
            </div>
          </template>

          <template v-slot:cell(uri)="{ data }">
            <opensilex-UriLink
              :uri="data.item.uri"
              :to="{ path: '/experiment/' + encodeURIComponent(data.item.uri) }"
            ></opensilex-UriLink>
          </template>

          <template v-slot:cell(actions)>
            <a
              href="data.html"
              class="btn btn-icon btn-row-action btn-outline-primary"
              title="Visualiser les données de cet objet scientifique"
              ><i class="ik ik-bar-chart-line-"></i
            ></a>
          </template>
        </opensilex-TableAsyncView>
      </template>
    </opensilex-PageContent>
    <!-- End results table -->
  </div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
import VueConstructor from "vue";
import VueRouter from "vue-router";
import moment from "moment";
import copy from "copy-to-clipboard";
import VueI18n from "vue-i18n";
import { BDropdown } from "bootstrap-vue";
import {
  GermplasmGetAllDTO,
  ProjectCreationDTO,
  FactorGetDTO,
  SpeciesDTO,
  ExperimentGetDTO,
  ResourceTreeDTO,
  ExperimentGetListDTO,
  OntologyService,
} from "opensilex-core/index";
import Oeso from "../../ontologies/Oeso";
import HttpResponse, {
  OpenSilexResponse,
  MetadataDTO,
  PaginationDTO,
} from "../../lib/HttpResponse";

class ScientificObjectFilter {
  experiments = [];
  germplasm = [];
  positions = [];
  uri = undefined;
  isPartOf = undefined;
  hasVariety = [];
  campaigns = [];
  factors = [];
  dates = undefined;

  constructor() {
    this.reset();
  }

  reset() {
    this.experiments = [];
    this.germplasm = [];
    this.positions = [];
    this.uri = undefined;
    this.isPartOf = undefined;
    this.hasVariety = [];
    this.campaigns = [];
    this.factors = [];
    this.dates = undefined;
  }
}

@Component
export default class ScientificObjectList extends Vue {
  $opensilex: any;
  $store: any;
  $router: VueRouter;

  @Ref("tableRef") readonly tableRef!: any;

  fields = [
    {
      key: "uri",
      label: "ScientificObjectsList.column.uri",
      sortable: true,
    },
    {
      key: "label",
      label: "ScientificObjectsList.column.alias",
      sortable: true,
    },
    {
      key: "type",
      label: "ScientificObjectsList.column.type",
      sortable: true,
    },
    {
      key: "parents",
      label: "ScientificObjectsList.column.parents",
    },
    {
      key: "experiments",
      label: "ScientificObjectsList.column.experiments",
    },
    {
      key: "actions",
      label: "ScientificObjectsList.column.actions",
    },
  ];

  experimentsByScientificObject: Map<String, Array<ExperimentGetDTO>> = new Map<
    String,
    Array<ExperimentGetDTO>
  >();

  filter = new ScientificObjectFilter();

  static async asyncInit($opensilex) {
    // await $opensilex.loadModule("opensilex-phis");
  }

  get user() {
    return this.$store.state.user;
  }

  reset() {
    this.filter.reset();
    this.refresh();
  }

  refresh() {
    this.tableRef.refresh();
  }

  searchScientificObject(options) {
    let experiment = undefined;
    let uri = undefined;
    let germplasm_URI = undefined;

    if (this.filter.experiments && this.filter.experiments.length > 0) {
      experiment = this.filter.experiments[0].id;
    }

    if (this.filter.uri && this.filter.uri.length > 0) {
      uri = this.filter.uri;
    }

    if (this.filter.germplasm && this.filter.germplasm.length > 0) {
      germplasm_URI = this.filter.germplasm[0].id;
    }

    return Promise.resolve([]);
    // let scientificObjectsService: ScientificObjectsService = this.$opensilex.getService("opensilex-phis.ScientificObjectsService");
    // return scientificObjectsService.getScientificObjectsBySearch(
    //     options.pageSize,
    //     options.currentPage,
    //     uri,
    //     experiment,
    //     undefined,
    //     germplasm_URI,
    //     true
    // ).then(http => {
    //     let result = {
    //         response: {
    //             metadata: null,
    //             result: null
    //         }
    //     };
    //     result.response.metadata = http.response.metadata;
    //     let data:any = http.response.result;
    //     result.response.result = data.data;

    //     return result;
    // });
  }

  experimentGetListDTOToSelectNode(dto: ExperimentGetListDTO) {
    if (dto) {
      return {
        id: dto.uri,
        label: dto.name,
      };
    }
    return null;
  }

  factorGetDTOToSelectNode(dto: FactorGetDTO) {
    if (dto) {
      return {
        id: dto.uri,
        label: dto.name,
      };
    }
    return null;
  }

  germplasmGetDTOToSelectNode(dto) {
    if (dto) {
      return {
        id: dto.uri,
        label: dto.label,
      };
    }
    return null;
  }

  loadSpecies() {
    return this.$opensilex
      .getService("opensilex.SpeciesService")
      .getAllSpecies()
      .then(
        (http: HttpResponse<OpenSilexResponse<Array<SpeciesDTO>>>) =>
          http.response.result
      );
  }

  speciesToSelectNode(dto: SpeciesDTO) {
    return {
      id: dto.uri,
      label: dto.label,
    };
  }

  featureToSelectNode(feature) {
    return {
      id: feature.id,
      label: feature.geometry.type,
      //label: feature.geometry.coordinates
    };
  }
}
</script>

<style scoped lang="scss">
</style>

<i18n>

en:
  ScientificObjectsList:
    selected: Selected Scientific Objects
    description: Manage and configure scientific objects
    advancedSearch: Advanced search
    propetiesConfiguration: Properties to display
    column:
      alias: Nom
      experiments: Experiment(s)
      type: Type
      uri: URI
      parents: Parent(s)
      actions: Actions
    placeholder:
      experiments: All Experiments
      germplasm: All Germplasm
      types: All Types
      uri: All URI
      isPartOf: All Alias / URI
      position: All Spacial Positions
      factors: All Factors
      dates: All Dates
    filter:
      label: Search for Scientific Objects
      experiments: Filter by Experiments
      germplasm: Filter by Germplasm
      types: Filter by Type(s)
      uri: Filter by  URI
      isPartOf: isPartOf (Alias ou URI)
      position: Filter by Spatial Position
      factors: Filter by Factors
      dates: Filter by Dates
fr:
  ScientificObjectsList:
    selected: Objets Scientifiques Sélectionnés
    description: Gérer et configurer les objets scientifiques
    advancedSearch: Recherche avancée
    propetiesConfiguration: Propriétés à afficher
    column:
      alias: Name
      experiments: Expérimentation(s)
      type: Type
      uri: URI
      parents: Parent(s)
      actions: Actions
    placeholder:
      experiments: Toutes les Expérimentations
      germplasm: Tous les Matériels Génétiques
      types: Tous les Types
      uris: Toutes les URI
      isPartOf: Tous les Alias / URI
      position: Toutes les Positions Spaciales
      factors: Tous les Facteurs
      dates: Toutes les Dates
    filter:
      label: Rechercher des Objets Scientifiques
      experiments: Filtrer par Expérimentation(s)
      germplasm: Filtrer par Matériel Génétiques
      types: Filtrer par Type(s)
      uri: Filtrer par URI
      isPartOf: isPartOf (Alias ou URI)
      position: Filtrer par Position Spaciale
      factors: Filtrer par Facteurs
      dates: Filtrer par Dates

</i18n>
