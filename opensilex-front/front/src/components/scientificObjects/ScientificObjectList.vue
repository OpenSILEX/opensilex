<template>
  <div class="container-fluid">
    <opensilex-PageHeader
      icon="ik#ik-target"
      title="component.menu.scientificObjects"
      description="ScientificObjectList.description"
    ></opensilex-PageHeader>

    <opensilex-SearchFilterField
      @search="refresh()"
      @clear="reset()"
      label="ScientificObjectList.filter.label"
      :showAdvancedSearch="true"
    >
      <template v-slot:filters>
        <!-- Name -->
        <opensilex-FilterField>
          <label for="name">{{$t('component.common.name')}}</label>
          <opensilex-StringFilter
            id="name"
            :filter.sync="filter.name"
            placeholder="ScientificObjectList.name-placeholder"
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

        <opensilex-FilterField>
          <label for="type">{{ $t("component.common.type") }}</label>
          <opensilex-ScientificObjectTypeSelector
            id="type"
            :types.sync="filter.types"
            :multiple="true"
          ></opensilex-ScientificObjectTypeSelector>
        </opensilex-FilterField>
      </template>

      <template v-slot:advancedSearch>
        <!-- Germplasm -->
        <opensilex-FilterField>
          <opensilex-GermplasmSelector :multiple="false" :germplasm.sync="filter.germplasm"></opensilex-GermplasmSelector>
        </opensilex-FilterField>

        <!-- Factors -->
        <opensilex-FilterField>
          <opensilex-FactorSelector :multiple="true" :factors.sync="filter.factors"></opensilex-FactorSelector>
        </opensilex-FilterField>
      </template>
    </opensilex-SearchFilterField>

    <opensilex-PageContent>
      <template v-slot>
        <opensilex-TableAsyncView
          ref="tableRef"
          :searchMethod="searchScientificObject"
          :fields="fields"
          defaultSortBy="label"
          :isSelectable="false"
          labelNumberOfSelectedRow="ScientificObjectList.selected"
          iconNumberOfSelectedRow="ik#ik-target"
        >
          <template v-slot:cell(uri)="{ data }">
            <opensilex-UriLink
              :uri="data.item.uri"
              :value="data.item.name"
              :to="{ path: '/scientific-objects/details/' + encodeURIComponent(data.item.uri) }"
            ></opensilex-UriLink>
          </template>

          <template v-slot:cell(actions)>
            <a
              href="data.html"
              class="btn btn-icon btn-row-action btn-outline-primary"
              title="Visualiser les données de cet objet scientifique"
            >
              <i class="ik ik-bar-chart-line-"></i>
            </a>
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
  ScientificObjectsService
} from "opensilex-core/index";
import Oeso from "../../ontologies/Oeso";
import HttpResponse, {
  OpenSilexResponse,
  MetadataDTO,
  PaginationDTO
} from "../../lib/HttpResponse";

@Component
export default class ScientificObjectList extends Vue {
  $opensilex: any;
  $store: any;
  $router: VueRouter;

  @Ref("tableRef") readonly tableRef!: any;

  fields = [
    {
      key: "uri",
      label: "component.common.name",
      sortable: true
    },
    {
      key: "typeLabel",
      label: "component.common.type",
      sortable: true
    }
  ];

  private langUnwatcher;
  mounted() {
    this.langUnwatcher = this.$store.watch(
      () => this.$store.getters.language,
      lang => {
        this.tableRef.update();
      }
    );
  }

  beforeDestroy() {
    this.langUnwatcher();
  }

  experimentsByScientificObject: Map<String, Array<ExperimentGetDTO>> = new Map<
    String,
    Array<ExperimentGetDTO>
  >();

  filter = {
    name: "",
    experiment: undefined,
    germplasm: undefined,
    factors: [],
    types: []
  };

  get user() {
    return this.$store.state.user;
  }

  reset() {
    this.filter = {
      name: "",
      experiment: undefined,
      germplasm: undefined,
      factors: [],
      types: []
    };
    this.refresh();
  }

  refresh() {
    this.tableRef.refresh();
  }

  searchScientificObject(options) {
    let scientificObjectsService: ScientificObjectsService = this.$opensilex.getService(
      "opensilex.ScientificObjectsService"
    );
    return scientificObjectsService.searchScientificObjects(
      this.filter.experiment ? this.filter.experiment : undefined, // contextURI?: string,
      this.filter.name, // pattern?: string,
      this.filter.types, // rdfTypes?: Array<string>,
      undefined, // parentURI?: string,
      this.filter.germplasm ? this.filter.germplasm : undefined,
      this.filter.factors, // factors?: Array<string>,
      undefined, // factorLevels?: Array<string>,
      options.currentPage, // page?: number,
      options.pageSize // pageSize?: number
    );
  }
}
</script>

<style scoped lang="scss">
</style>

<i18n>

en:
  ScientificObjectList:
    name-placeholder: Enter name
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
  ScientificObjectList:
    name-placeholder: Saisir un nom
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
