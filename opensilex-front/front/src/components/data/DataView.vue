<template>
  <div class="container-fluid">
    <opensilex-PageHeader
      icon="ik#ik-bar-chart-line"
      title="component.menu.data.label"
      description="DataView.description"
    ></opensilex-PageHeader>

    <opensilex-PageActions>
      <template v-slot>
        <opensilex-Button
          variant="outline-primary"
          @click="templateForm.show()"
          :small="false"
          icon
          label="DataView.buttons.generate-template"
        ></opensilex-Button>
        <opensilex-CreateButton
          @click="dataForm.showCreateForm()"
          label="DataView.buttons.create-data"
        ></opensilex-CreateButton>
      </template>
    </opensilex-PageActions>

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
    <opensilex-DataForm
      ref="dataForm"
      @onCreate="refresh()"
      @onUpdate="refresh()"
    ></opensilex-DataForm>

    <opensilex-GenerateDataTemplateFrom
      ref="templateForm"
    ></opensilex-GenerateDataTemplateFrom>
    <opensilex-TableAsyncView
      ref="tableRef"
      :searchMethod="searchDataList"
      :fields="fields"
      defaultSortBy="name"
      :isSelectable="isSelectable"
      labelNumberOfSelectedRow="FactorList.selected"
      iconNumberOfSelectedRow="ik#ik-feather"
    >
      <template v-slot:head(variable)="{ data }">{{ $t(data.label) }}</template>
      <template v-slot:head(value)="{ data }">{{ $t(data.label) }}</template>
      <template v-slot:head(actions)="{ data }">{{ $t(data.label) }}</template>
      <template v-slot:cell(variable)="{ data }">
        <opensilex-UriLink
          :uri="data.item.variable"
          :value="data.item.variable"
          :to="{
            path: '/variable/' + encodeURIComponent(data.item.variable),
          }"
        ></opensilex-UriLink>
      </template>
      <template v-slot:cell(object)="{ data }">
        {{ data.item.object[0].uri }}
      </template>
    </opensilex-TableAsyncView>
  </div>
</template>

<script lang="ts">
import { Prop, Component, Ref } from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";

@Component
export default class DataView extends Vue {
  $opensilex: any;
  $store: any;
  service: any;
  disabled = false;

  @Ref("templateForm") readonly templateForm!: any;

  get credentials() {
    return this.$store.state.credentials;
  }

  filter = {
    experiments: [],
    units: [],
    traits: [],
    methods: [],
    scientificObjects: [],
    disabled: false,
  };

  resetFilter() {
    this.filter.experiments = [];
    this.filter.units = [];
    this.filter.traits = [];
    this.filter.methods = [];
    this.filter.scientificObjects = [];
  }
  @Prop({
    default: false,
  })
  isSelectable;

  @Prop({
    default: false,
  })
  noActions;

  @Ref("dataForm") readonly dataForm!: any;

  get user() {
    return this.$store.state.user;
  }

  get fields() {
    let tableFields: any = [
      {
        key: "date",
        label: "DataView.list.date",
        sortable: true,
      },
      {
        key: "variable",
        label: "DataView.list.variable",
        sortable: true,
      },
      {
        key: "object",
        label: "DataView.list.object",
        sortable: true,
      },
      {
        key: "value",
        label: "DataView.list.value",
        sortable: false,
      },
    ];
    if (!this.noActions) {
      tableFields.push({
        key: "actions",
        label: "component.common.actions",
      });
    }
    return tableFields;
  }

  @Ref("tableRef") readonly tableRef!: any;

  refresh() {
    this.tableRef.refresh();
  }

  reset() {
    this.resetFilter();
    this.refresh();
  }

  updateFilters(value: string) {
    for (let [key, value] of Object.entries(this.filter)) {
      this.$opensilex.updateURLParameter(key, value, "");
    }
    this.refresh();
  }

  created() {
    this.service = this.$opensilex.getService("opensilex.DataService");
    let query: any = this.$route.query;

    this.reset();
    for (let [key, value] of Object.entries(this.filter)) {
      if (query[key]) {
        this.filter[key] = decodeURIComponent(query[key]);
      }
    }
  }

  searchDataList(options) {
    return this.service.searchDataList(
      undefined, // start_date
      undefined, // end_date
      undefined, // timezone
      undefined, // scientific_object
      undefined, // variable
      undefined, // min_confidence
      undefined, // max_confidence
      undefined, // provenance
      undefined, // metadata
      undefined, // order_by
      options.currentPage,
      options.pageSize
    );
  }

  convertHttp(http) {
    let result = {
      response: {
        metadata: null,
        result: null,
      },
    };
    result.response.metadata = http.response.metadata;
    let data: any = http.response.result;
    result.response.result = data.data;

    return result;
  }

  loadUnits() {
    let service = this.$opensilex.getService("opensilex.VariablesService");
    return service
      .searchUnits(undefined, undefined, 100, 0)
      .then(
        (http: HttpResponse<OpenSilexResponse<Array<any>>>) =>
          http.response.result
      );
  }

  loadTraits() {
    let service = this.$opensilex.getService("opensilex.VariablesService");
    return service
      .searchEntities(undefined, undefined, 100, 0)
      .then(
        (http: HttpResponse<OpenSilexResponse<Array<any>>>) =>
          http.response.result
      );
  }

  loadMethods() {
    let service = this.$opensilex.getService("opensilex.VariablesService");
    return service
      .searchMethods(undefined, undefined, 100, 0)
      .then(
        (http: HttpResponse<OpenSilexResponse<Array<any>>>) =>
          http.response.result
      );
  }

  dtoToSelectNode(dto) {
    return {
      id: dto.uri,
      name: dto.name,
    };
  }
}
</script>


<style scoped lang="scss">
</style>

<i18n>

en:
  DataView:
    buttons:
      create-data : Add data
      generate-template : Generate template
    description: View and export data
    list:
      date: Date
      variable: Variable
      value: Value
      object: Object
    filter:
      label: Search data
      experiments:  Experiment(s)
      traits: Filter par Trait(s)
      methods:  Method(s)
      units:  Unit(s)
      scientificObjects: Select a Scientific Object
    placeholder:
      traits: All Traits
      methods: All Methods
      units: All Units

fr:
  DataView:
    buttons:
      create-data : Ajouter un jeu de données
      generate-template : Générer un gabarit
    description: Visualiser et exporter des données
    list:
      date: Date
      variable: Variable
      value: Valeur
      object: objet
    filter:
      label: Rechercher des données
      experiments:  Expérimentation(s)
      traits:  Trait(s)
      methods: Méthode(s)
      units:  Unité(s)
      scientificObjects: Sélectionner un Objet Scientifique
    placeholder:
      traits: Tous les Traits
      methods: Toutes les Méthodes
      units: Toutes les Unités

      
  
</i18n>
