<template>
  <opensilex-PageContent>
    <template v-slot>
      <opensilex-TableAsyncView
        ref="tableRef"
        :searchMethod="searchScientificObject"
        :fields="fields"
        defaultSortBy="name"
        :isSelectable="true"
        labelNumberOfSelectedRow="ScientificObjectList.selected"
        iconNumberOfSelectedRow="ik#ik-target"
        :defaultPageSize="pageSize"
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
            <b-dropdown-item-button
                v-if="user.hasCredential(credentials.CREDENTIAL_DOCUMENT_MODIFICATION_ID)"
                @click="$emit('createDocument')">
              {{ $t("component.common.addDocument") }}
            </b-dropdown-item-button>
            <b-dropdown-item-button @click="exportCSV(false)">
              Export CSV
            </b-dropdown-item-button>

            <b-dropdown-item-button
                v-if="user.hasCredential(credentials.CREDENTIAL_EVENT_MODIFICATION_ID)"
                @click="$emit('createEvents')">
              {{ $t("Event.add-multiple") }}
            </b-dropdown-item-button>
            <b-dropdown-item-button
                v-if="user.hasCredential(credentials.CREDENTIAL_EVENT_MODIFICATION_ID)"
                @click="$emit('createMoves')">
              {{ $t("Move.add") }}
            </b-dropdown-item-button>
          </b-dropdown>

          <opensilex-CreateButton
              v-if="!noActions" 
              class="mb-2 mr-2"
              @click="exportCSV(true)"
              :disabled="tableRef.totalRow === 0"
              label="ScientificObjectList.export-all"
          ></opensilex-CreateButton>

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
              @click="$emit('update',data.item.uri)"
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
</template>

<script lang="ts">
import { Component, Ref, Prop, PropSync } from "vue-property-decorator";
import Vue from "vue";
import VueRouter from "vue-router";
import { ExperimentGetDTO, ScientificObjectsService } from "opensilex-core/index";

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

  // If true, the filters won't be stored in the page url
  @Prop({
    default: false
  })
  noUpdateURL;

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
  @Ref("soForm") readonly soForm!: any;

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
    if (!this.noUpdateURL) {
      this.$opensilex.updateFiltersFromURL(this.$route.query, this.filter);
    }
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

  refresh() {
    this.tableRef.selectAll = false;
    this.tableRef.onSelectAll();
    this.tableRef.refresh();
    this.$nextTick(() => {
      if (!this.noUpdateURL) {
        this.$opensilex.updateURLParameters(this.filter);
      }      
    });
  }

  refreshWithKeepingSelection() {
    if (!this.noUpdateURL) {
        this.$opensilex.updateURLParameters(this.filter);
    }
    this.tableRef.refresh();
  }

  searchScientificObject(options) {

    let scientificObjectsService: ScientificObjectsService = this.$opensilex.getService(
      "opensilex.ScientificObjectsService"
    );
    return scientificObjectsService.searchScientificObjects(
      this.filter.experiment,
      this.filter.types,
      this.filter.name,
      undefined,
      this.filter.germplasm,
      this.filter.factorLevels,
      undefined,
      this.filter.existenceDate,
      this.filter.creationDate,
      options.orderBy,
      options.currentPage,
      options.pageSize
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

  exportCSV(exportAll: boolean) {
    let path = "/core/scientific_objects/export";
    let today = new Date();
    let filename =
      "export_scientific_objects_" +
      today.getFullYear() +
      String(today.getMonth() + 1).padStart(2, "0") +
      String(today.getDate()).padStart(2, "0");

    // export all OS corresponding to filter
    let exportDto  = {
      experiment: this.filter.experiment,
      rdf_types: this.filter.types,
      name: this.filter.name,
      facility: this.filter.facility,
      germplasm: this.filter.germplasm,
      factor_levels: this.filter.factorLevels,
      existence_date: this.filter.existenceDate,
      creation_date: this.filter.creationDate,
      excluded_uris: undefined,
      order_by: this.tableRef.getOrderBy()
    };

    // export only selected URIS (+matching with filter)
    if(! exportAll){
      let objectURIs = [];
      for (let select of this.tableRef.getSelected()) {
        objectURIs.push(select.uri);
      }
      Object.assign(exportDto, {
        uris: objectURIs
      });
    }

    this.$opensilex.downloadFilefromPostService(
        path,
        filename,
        "csv",
        exportDto,
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

  getSelected() {
    return this.tableRef.getSelected();
  }

  onItemUnselected(row) {
    this.tableRef.onItemUnselected(row);
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
    export-all: Export all
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
    export-all: Tout exporter
</i18n>
