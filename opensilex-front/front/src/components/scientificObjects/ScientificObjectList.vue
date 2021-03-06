<template>
  <div>
    <opensilex-SearchFilterField
      @search="refresh()"
      @clear="reset()"
      label="ScientificObjectList.filter.label"
      :showAdvancedSearch="true"
    >
      <template v-slot:filters>
        <!-- Name -->
        <opensilex-FilterField>
          <label for="name">{{ $t("component.common.name") }}</label>
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
          <opensilex-GermplasmSelector
            :multiple="false"
            :germplasm.sync="filter.germplasm"
          ></opensilex-GermplasmSelector>
        </opensilex-FilterField>
        <!-- Factors levels -->
        <opensilex-FilterField>
          <b-form-group>
            <label for="factorLevels">
              {{ $t("FactorLevelSelector.label") }}
            </label>
            <opensilex-FactorLevelSelector
              id="factorLevels"
              :factorLevels.sync="filter.factorLevels"
              :multiple="true"
              :required="false"
            ></opensilex-FactorLevelSelector>
          </b-form-group>
        </opensilex-FilterField>
        <!-- Exists -->
        <opensilex-FilterField>
          <opensilex-DateForm
            :value.sync="filter.existenceDate"
            label="ScientificObjectList.existenceDate"
          ></opensilex-DateForm>
        </opensilex-FilterField>
        <!-- Created -->
        <opensilex-FilterField>
          <opensilex-DateForm
            :value.sync="filter.creationDate"
            label="ScientificObjectList.creationDate"
          ></opensilex-DateForm>
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
          :isSelectable="true"
          labelNumberOfSelectedRow="ScientificObjectList.selected"
          iconNumberOfSelectedRow="ik#ik-target"
        >
          <template v-slot:selectableTableButtons="{ numberOfSelectedRows }">
            <b-dropdown
              v-if="!noActions" 
              dropright
              class="mb-2 mr-2"
              :small="true"
              :disabled="numberOfSelectedRows == 0"
              text="actions"
            >
              <b-dropdown-item-button @click="createDocument()">
                {{ $t("component.common.addDocument") }}
              </b-dropdown-item-button>
              <b-dropdown-item-button @click="exportCSV">
                Export CSV
              </b-dropdown-item-button>

              <b-dropdown-item-button @click="createEvents()">
                {{ $t("Event.add-multiple") }}
              </b-dropdown-item-button>
              <b-dropdown-item-button @click="createMoves()">
                {{ $t("Move.add") }}
              </b-dropdown-item-button>
            </b-dropdown>
          </template>

          <template v-slot:cell(name)="{ data }">
            <opensilex-UriLink
              :uri="data.item.uri"
              :value="data.item.name"
              :to="{
                path:
                  '/scientific-objects/details/' +
                  encodeURIComponent(data.item.uri),
              }"
            ></opensilex-UriLink>
          </template>
          <template v-slot:row-details="{ data }">
            <div
              v-if="
                objectDetails[data.item.uri] &&
                objectDetails[data.item.uri].length > 0
              "
            >
              <div>{{ $t("ScientificObjectList.experiments") }}:</div>
              <ul>
                <li
                  v-for="(xp, index) in objectDetails[data.item.uri]"
                  :key="index"
                >
                  <opensilex-UriLink
                    :uri="xp.uri"
                    :value="xp.name"
                    :to="{
                      path: '/experiment/details/' + encodeURIComponent(xp.uri),
                    }"
                  ></opensilex-UriLink>
                </li>
              </ul>
            </div>
            <div v-else>
              {{ $t("ScientificObjectList.not-used-in-experiments") }}
            </div>
          </template>
          <template v-slot:cell(actions)="{ data }">
            <b-button-group size="sm">
              <opensilex-DetailButton
                @click="loadObjectDetail(data)"
                label="component.common.details-label"
                :detailVisible="data.detailsShowing"
                :small="true"
              ></opensilex-DetailButton>
              <opensilex-EditButton
                v-if="
                  user.hasCredential(
                    credentials.CREDENTIAL_SCIENTIFIC_OBJECT_MODIFICATION_ID
                  )
                "
                @click="soForm.editScientificObject(data.item.uri)"
                label="ExperimentScientificObjects.edit-scientific-object"
                :small="true"
              ></opensilex-EditButton>
              <opensilex-DeleteButton
                v-if="
                  user.hasCredential(
                    credentials.CREDENTIAL_SCIENTIFIC_OBJECT_DELETE_ID
                  )
                "
                label="ExperimentScientificObjects.delete-scientific-object"
                @click="deleteScientificObject(data.item.uri)"
                :small="true"
              ></opensilex-DeleteButton>
            </b-button-group>
          </template>
        </opensilex-TableAsyncView>
      </template>
    </opensilex-PageContent>
    <!-- End results table -->
    <opensilex-ModalForm
      ref="documentForm"
      component="opensilex-DocumentForm"
      createTitle="component.common.addDocument"
      modalSize="lg"
      :initForm="initForm"
      icon="ik#ik-file-text"
    ></opensilex-ModalForm>

    <opensilex-EventCsvForm
      ref="eventCsvForm"
      :targets="selectedUris"
    ></opensilex-EventCsvForm>

    <opensilex-EventCsvForm
      ref="moveCsvForm"
      :targets="selectedUris"
      :isMove="true"
    ></opensilex-EventCsvForm>

    <opensilex-ScientificObjectForm
      v-if="!noValidation"
      ref="soForm"
      @onUpdate="redirectToDetail"
    ></opensilex-ScientificObjectForm>
  </div>
</template>

<script lang="ts">
import { Component, Ref, Prop, PropSync } from "vue-property-decorator";
import Vue from "vue";
import VueRouter from "vue-router";
// @ts-ignore
import { ExperimentGetDTO, ScientificObjectsService } from "opensilex-core/index";
import EventCsvForm from "../events/form/csv/EventCsvForm.vue";

@Component
export default class ScientificObjectList extends Vue {
  $opensilex: any;
  $store: any;
  $router: VueRouter;

  @Prop({
    default: false
  })
  isSelectable;

  @Prop({
    default: false
  })
  noActions;

  @Prop({
    default: 20
  })
  pageSize: number;

  @Prop({
    default: false
  })
  noUpdateURL;

  @Prop({
    default: false
  })
  noValidation;

  @PropSync("searchFilter", {
    default: () => {
      return {
        name: "",
        experiment: undefined,
        germplasm: undefined,
        factorLevels: [],
        types: [],
        existenceDate: undefined,
        creationDate: undefined,
      };
    },
  })
  filter;

  @Ref("tableRef") readonly tableRef!: any;
  @Ref("documentForm") readonly documentForm!: any;
  @Ref("eventCsvForm") readonly eventCsvForm!: EventCsvForm;
  @Ref("moveCsvForm") readonly moveCsvForm!: EventCsvForm;
  @Ref("soForm") readonly soForm!: any;

  selectedUris: Array<string> = [];

  get fields() {
    let fields: any = [
      {
        key: "name",
        label: "component.common.name",
        sortable: true,
      },
      {
        key: "rdf_type_name",
        label: "component.common.type",
        sortable: true,
      },
      {
        key: "creation_date",
        label: "ScientificObjectList.creationDate",
        sortable: true,
      },
      {
        key: "destruction_date",
        label: "ScientificObjectList.destructionDate",
        sortable: true,
      }
    ];

    if (!this.noActions) {
      fields.push({
        key: "actions",
        label: "component.common.actions"
      });
    }

    return fields;
  }

  created() {
    this.updateFiltersFromURL();
  }

  private langUnwatcher;
  mounted() {
    this.langUnwatcher = this.$store.watch(
      () => this.$store.getters.language,
      (lang) => {
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

  get user() {
    return this.$store.state.user;
  }

  get lang() {
    return this.$store.state.lang;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  reset() {
    this.filter = {
      name: "",
      experiment: undefined,
      germplasm: undefined,
      factorLevels: [],
      types: [],
      existenceDate: undefined,
      creationDate: undefined,
    };
    this.refresh();
  }

  updateFiltersFromURL() {
    if (!this.noUpdateURL) {
      let query: any = this.$route.query;
      for (let [key, value] of Object.entries(this.filter)) {
        if (query[key]) {
          if (Array.isArray(this.filter[key])){
            this.filter[key] = decodeURIComponent(query[key]).split(",");
          } else {
            this.filter[key] = decodeURIComponent(query[key]);
          }        
        }
      }
    }
    
  }

  updateURLFilters() {
    if (!this.noUpdateURL) {
      for (let [key, value] of Object.entries(this.filter)) {
        this.$opensilex.updateURLParameter(key, value, "");       
      }
    }    
  }

  refresh() {
    this.tableRef.selectAll = false;
    this.tableRef.onSelectAll();
    this.updateURLFilters();
    this.tableRef.refresh();
  }

  refreshWithKeepingSelection() {
    this.updateURLFilters();
    this.tableRef.refresh();
  }

  searchScientificObject(options) {
    if (this.pageSize != null) {
      options.pageSize = this.pageSize;
    }

    let scientificObjectsService: ScientificObjectsService = this.$opensilex.getService(
      "opensilex.ScientificObjectsService"
    );
    return scientificObjectsService.searchScientificObjects(
      this.filter.experiment ? this.filter.experiment : undefined, // experiment uri?: string,
      this.filter.types, // rdfTypes?: Array<string>,
      this.filter.name, // pattern?: string,
      undefined, // parentURI?: string,
      this.filter.germplasm ? this.filter.germplasm : undefined,
      this.filter.factorLevels, // factorLevels?: Array<string>,
      undefined, // facility?: string,
      this.filter.existenceDate ? this.filter.existenceDate : undefined,
      this.filter.creationDate ? this.filter.creationDate : undefined,
      options.orderBy,
      options.currentPage, // page?: number,
      options.pageSize // pageSize?: number
    );
  }

  private objectDetails = {};

  loadObjectDetail(data) {
    if (!data.detailsShowing) {
      this.$opensilex.disableLoader();
      let scientificObjectsService: ScientificObjectsService = this.$opensilex.getService(
        "opensilex.ScientificObjectsService"
      );
      scientificObjectsService
        .getScientificObjectDetailByExperiments(data.item.uri)
        .then((http: any) => {
          let objectExperiments = [];
          for (let objectDetail of http.response.result) {
            if (objectDetail.experiment != null) {
              objectExperiments.push({
                uri: objectDetail.experiment,
                name: objectDetail.experiment_name,
              });
            }
          }
          this.objectDetails[data.item.uri] = objectExperiments;
          data.toggleDetails();
          this.$opensilex.enableLoader();
        })
        .catch(this.$opensilex.errorHandler);
    } else {
      data.toggleDetails();
    }
  }

  exportCSV() {
    let path = "/core/scientific_objects/export";
    let today = new Date();
    let filename =
      "export_scientific_objects_" +
      today.getFullYear() +
      String(today.getMonth() + 1).padStart(2, "0") +
      String(today.getDate()).padStart(2, "0");

    let objectURIs = [];
    for (let select of this.tableRef.getSelected()) {
      objectURIs.push(select.uri);
    } 
    
    this.$opensilex.downloadFilefromPostService(
      path,
      filename,
      "csv",
      {        
        uris: objectURIs,
      },
      this.lang
    );
  }

  deleteScientificObject(uri) {
    let scientificObjectsService: ScientificObjectsService = this.$opensilex.getService(
      "opensilex.ScientificObjectsService"
    );
    scientificObjectsService
      .deleteScientificObject(uri)
      .then(() => {
        this.refresh();
      })
      .catch(this.$opensilex.errorHandler);
  }

  createDocument() {
    this.documentForm.showCreateForm();
  }

  createEvents() {
    this.updateSelectedUris();
    this.eventCsvForm.show();
  }

  createMoves() {
    this.updateSelectedUris();
    this.moveCsvForm.show();
  }

  updateSelectedUris() {
    this.selectedUris = [];
    for (let select of this.tableRef.getSelected()) {
      this.selectedUris.push(select.uri);
    }
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
        keywords: undefined,
      },
      file: undefined,
    };
  }

  getSelected() {
    return this.tableRef.getSelected();
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
    not-used-in-experiments: Scientific object not used in any experiments
    experiments: Scientific object used in experiment(s)
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
    creationDate:  Creation date
    destructionDate: Destruction date
    existenceDate: Object exists
    visualize: Visualize
fr:
  ScientificObjectList:
    name-placeholder: Saisir un nom
    selected: Objets Scientifiques Sélectionnés
    description: Gérer et configurer les objets scientifiques
    advancedSearch: Recherche avancée
    propetiesConfiguration: Propriétés à afficher
    not-used-in-experiments: L'objet scientifique n'est utilisé dans aucune expérimentation
    experiments: L'objet scientifique est utilisé dans le/les expérimentation(s)
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
    creationDate:  Date de création
    destructionDate: Date de destruction
    existenceDate: Date d'existence
    visualize: Visualiser
</i18n>
