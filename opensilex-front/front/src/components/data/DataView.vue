<template>
  <div class="container-fluid">
    <opensilex-PageHeader
      icon="ik#ik-bar-chart-line"
      title="component.menu.data.label"
      description="DataView.description"
    ></opensilex-PageHeader>

    <opensilex-SearchFilterField
      @search="refresh()"
      @clear="reset()"
      label="DataView.filter.label"
      :showTitle="true"
    >

      <template v-slot:filters>

        <!-- Experiments -->
        <opensilex-FilterField>
            <opensilex-SelectForm
              label="DataView.filter.experiments"
              :selected.sync="filter.experiments"
              :conversionMethod="dtoToSelectNode"
              modalComponent="opensilex-ExperimentModalList"
              :isModalSearch="true"
              :required="true"
            ></opensilex-SelectForm>
        </opensilex-FilterField>

        <!-- traits -->
        <opensilex-FilterField>
          <opensilex-SelectForm
            label="DataView.filter.traits"
            placeholder="DataView.placeholder.traits"
            :multiple="true"
            :selected.sync="filter.traits"
            :optionsLoadingMethod="loadTraits"
            :conversionMethod="dtoToSelectNode"
          ></opensilex-SelectForm>
        </opensilex-FilterField>

        <!-- methods -->
        <opensilex-FilterField>
          <opensilex-SelectForm
            label="DataView.filter.methods"
            placeholder="DataView.placeholder.methods"
            :multiple="true"
            :selected.sync="filter.methods"
            :optionsLoadingMethod="loadMethods"
            :conversionMethod="dtoToSelectNode"
          ></opensilex-SelectForm>
        </opensilex-FilterField>

        <!-- units -->
        <opensilex-FilterField>
          <opensilex-SelectForm
            label="DataView.filter.units"
            placeholder="DataView.placeholder.units"
            :multiple="true"
            :selected.sync="filter.units"
            :optionsLoadingMethod="loadUnits"
            :conversionMethod="dtoToSelectNode"
          ></opensilex-SelectForm>
        </opensilex-FilterField>

        <!-- Scientific objects -->
        <opensilex-FilterField :fullWidth="true">
            <opensilex-SelectForm
              label="DataView.filter.scientificObjects"
              :selected.sync="filter.scientificObjects"
              :conversionMethod="dtoToSelectNode"
              modalComponent="opensilex-ExperimentModalList"
              :isModalSearch="true"
              :required="true"
              :disabled="disabled"
            ></opensilex-SelectForm>
        </opensilex-FilterField>

      </template>
    </opensilex-SearchFilterField>

    <opensilex-PageContent>

      <template v-slot></template>

    </opensilex-PageContent>

  </div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
import VueConstructor from "vue";
import VueI18n from "vue-i18n";
import moment from 'moment';
import {
  ExperimentGetListDTO
} from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";
import { UserGetDTO } from "opensilex-security/index";

class DataFilter {
  experiments = [];
  units = [];
  traits = [];
  methods = [];
  scientificObjects = [];

  constructor() {
    this.reset();
  }

  reset() {
    this.experiments = [];
    this.units = [];
    this.traits = [];
    this.methods = [];
    this.scientificObjects = [];
  }
}

@Component
export default class DataView extends Vue {
  $opensilex: any;
  $store: any;

  filter = new DataFilter();

  get user() {
    return this.$store.state.user;
  }

  refresh() {
    
  }

  reset() {
    this.filter.reset();
    this.refresh();
  }

  created() {
  }

  convertHttp(http) {
    let result = {
        response: {
            metadata: null,
            result: null
        }
    };
    result.response.metadata = http.response.metadata;
    let data:any = http.response.result;
    result.response.result = data.data;
    
    return result;
  }

  loadUnits() {
    let service = this.$opensilex.getService("opensilex-phis.UnitsService");
    return service.getUnitsBySearch(
      100,
      0,
      undefined,
      undefined
    ).then(http => this.convertHttp(http));
  }

  loadTraits() {
    let service = this.$opensilex.getService("opensilex-phis.TraitsService");
    return service.getTraitsBySearch(
      100,
      0,
      undefined,
      undefined
    ).then(http => this.convertHttp(http));
  }

  loadMethods() {
    let service = this.$opensilex.getService("opensilex-phis.MethodsService");
    return service.getMethodsBySearch(
      100,
      0,
      undefined,
      undefined
    ).then(http => this.convertHttp(http));
  }

  dtoToSelectNode(dto) {
    return {
        id: dto.uri,
        label: dto.label
    };
  }

}
</script>


<style scoped lang="scss">

</style>

<i18n>

en:
  DataView:
    description: View and export data
    filter:
      label: Search data
      experiments: Filter by Experiment(s)
      traits: Filter par Trait(s)
      methods: Filter by Method(s)
      units: Filter by Unit(s)
      scientificObjects: Select a Scientific Object
    placeholder:
      traits: All Traits
      methods: All Methods
      units: All Units

fr:
  DataView:
    description: Visualiser et exporter des données
    filter:
      label: Rechercher des données
      experiments: Filtrer par Expérimentation(s)
      traits: Filtrer par Trait(s)
      methods: Filtrer par Méthode(s)
      units: Filtrer par Unité(s)
      scientificObjects: Sélectionner un Objet Scientifique
    placeholder:
      traits: Tous les Traits
      methods: Toutes les Méthodes
      units: Toutes les Unités
  
</i18n>
