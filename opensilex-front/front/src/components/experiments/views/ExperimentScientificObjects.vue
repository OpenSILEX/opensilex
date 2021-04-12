<template>
  <div ref="page">
    <opensilex-PageActions>
      <opensilex-CreateButton
        v-if="
          user.hasCredential(credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID)
        "
        @click="soForm.createScientificObject()"
        label="ExperimentScientificObjects.create-scientific-object"
      ></opensilex-CreateButton>&nbsp;
      <opensilex-CreateButton
        v-if="
          user.hasCredential(credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID)
        "
        @click="importForm.show()"
        label="OntologyCsvImporter.import"
      ></opensilex-CreateButton>
      <opensilex-ScientificObjectCSVImporter
        ref="importForm"
        :experimentURI="uri"
        @csvImported="refresh()"
      ></opensilex-ScientificObjectCSVImporter>
    </opensilex-PageActions>

    <div class="row">
      <div class="col-md-12">
        <opensilex-SearchFilterField @clear="resetSearch()" @search="unselectRefresh()">
          <template v-slot:filters>
            <opensilex-FilterField>
              <b-form-group>
                <label for="name">{{ $t("component.common.name") }}</label>
                <opensilex-StringFilter
                  id="name"
                  :filter.sync="filters.name"
                  placeholder="ExperimentScientificObjects.name-placeholder"
                ></opensilex-StringFilter>
              </b-form-group>
            </opensilex-FilterField>

            <opensilex-FilterField>
              <b-form-group>
                <label for="type">{{ $t("ExperimentScientificObjects.objectType") }}</label>
                <opensilex-ScientificObjectTypeSelector
                  id="type"
                  :types.sync="filters.types"
                  :multiple="true"
                  :experimentURI="uri"
                ></opensilex-ScientificObjectTypeSelector>
              </b-form-group>
            </opensilex-FilterField>

            <opensilex-FilterField>
              <b-form-group>
                <label for="parentFilter">{{ $t("ExperimentScientificObjects.parent-label") }}</label>
                <opensilex-SelectForm
                  id="parentFilter"
                  :selected.sync="filters.parent"
                  :multiple="false"
                  :required="false"
                  :searchMethod="searchParents"
                ></opensilex-SelectForm>
              </b-form-group>
            </opensilex-FilterField>

            <opensilex-FilterField>
              <b-form-group>
                <label for="factorLevels">{{ $t("FactorLevelSelector.label") }}</label>
                <opensilex-FactorLevelSelector
                  id="factorLevels"
                  :factorLevels.sync="filters.factorLevels"
                  :multiple="true"
                  :required="false"
                  :experimentURI="uri"
                ></opensilex-FactorLevelSelector>
              </b-form-group>
            </opensilex-FilterField>
          </template>
        </opensilex-SearchFilterField>
      </div>
    </div>
    <div class="row">
      <div class="col-md-6">
        <b-card>
          <div class="card-header">
            <h3 class="d-inline">
              <opensilex-Icon icon="ik#ik-target" class="title-icon" />
              {{ $t("ScientificObjectList.selected") }}
            </h3>&nbsp;
            <span class="badge badge-pill badge-info">
              {{
              selectedObjects.length
              }}
            </span>
            <b-dropdown
              dropright
              class="mb-2 mr-2"
              :small="true"
              :disabled="selectedObjects.length == 0"
              text=actions>
                <b-dropdown-item-button    
                  @click="createDocument()"
                >{{$t('component.common.addDocument')}}</b-dropdown-item-button>
                <b-dropdown-item-button
                  @click="exportCSV"
                >Export CSV</b-dropdown-item-button>
                <b-dropdown-divider></b-dropdown-divider>
                <b-dropdown-item-button 
                  @click="visualize"
                >{{$t('ExperimentScientificObjects.visualize')}}</b-dropdown-item-button>
            </b-dropdown>
          </div>
          <opensilex-TreeViewAsync
            ref="soTree"
            :searchMethod="searchMethod"
            :enableSelection="true"
            :selection.sync="selectedObjects"
            @select="displayScientificObjectDetailsIfNew($event.data.uri)"
          >
            <template v-slot:node="{ node }">
              <span class="item-icon">
                <opensilex-Icon :icon="$opensilex.getRDFIcon(node.data.rdf_type)" />
              </span>&nbsp;
              <span>{{ node.title }}</span>
            </template>

            <template v-slot:buttons="{ node }">
              <opensilex-EditButton
                v-if="
                  user.hasCredential(
                    credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID
                  )
                "
                @click="soForm.editScientificObject(node.data.uri)"
                label="ExperimentScientificObjects.edit-scientific-object"
                :small="true"
              ></opensilex-EditButton>
              <opensilex-AddChildButton
                v-if="
                  user.hasCredential(
                    credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID
                  )
                "
                @click="soForm.createScientificObject(node.data.uri)"
                label="ExperimentScientificObjects.add-scientific-object-child"
                :small="true"
              ></opensilex-AddChildButton>
              <opensilex-DeleteButton
                v-if="
                  user.hasCredential(
                    credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID
                  )
                "
                @click="deleteScientificObject(node)"
                label="ExperimentScientificObjects.delete-scientific-object"
                :small="true"
              ></opensilex-DeleteButton>
            </template>
          </opensilex-TreeViewAsync>
          <opensilex-ScientificObjectForm
            v-if="
              user.hasCredential(
                credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID
              )
            "
            ref="soForm"
            :context="{ experimentURI: this.uri }"
            @refresh="refresh"
          ></opensilex-ScientificObjectForm>
        </b-card>
      </div>
      <div class="col-md-6">
        <opensilex-ScientificObjectDetail v-if="selected" :selected="selected" :tabs="detailTabs" :experiment="uri"/>
      </div>
    </div>

    <opensilex-ExperimentDataVisuView
      v-if="showDataVisuView"
      :selectedScientificObjects="selectedNamedObjects"
      @graphicCreated="onGraphicCreated"
    ></opensilex-ExperimentDataVisuView>

    <opensilex-ModalForm
      v-if="user.hasCredential(credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID)"
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
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
import { ScientificObjectsService } from "opensilex-core/index";
import HttpResponse from "opensilex-core/HttpResponse";
import ScientificObjectDetail from "../../scientificObjects/ScientificObjectDetail.vue";
@Component
export default class ExperimentScientificObjects extends Vue {
  $opensilex: any;
  $route: any;
  $store: any;
  $t: any;
  soService: ScientificObjectsService;
  uri: string;
  showDataVisuView = false;
  numberOfSelectedRows = 0;

  
  @Ref("soForm") readonly soForm!: any;
  @Ref("soTree") readonly soTree!: any;
  @Ref("importForm") readonly importForm!: any;
  @Ref("documentForm") readonly documentForm!: any;

  get customColumns() {
    return [
      {
        id: "geometry",
        name: this.$t("ExperimentScientificObjects.geometry-label"),
        type: "WKT",
        comment: this.$t("ExperimentScientificObjects.geometry-comment"),
        is_required: false,
        is_list: false
      }
    ];
  }

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  get detailTabs() {
    return [
      ScientificObjectDetail.DOCUMENTS_TAB,
      ScientificObjectDetail.ANNOTATIONS_TAB,
      ScientificObjectDetail.EVENTS_TAB,
      ScientificObjectDetail.POSITIONS_TAB
    ]
  }

  public nodes = [];

  public filters = {
    name: "",
    types: [],
    parent: undefined,
    factorLevels: []
  };

  public selected = null;

  selectedObjects = [];
  namedObjectsArray = [];
  selectedNamedObjects = [];
  @Ref("facilitySelector") readonly facilitySelector!: any;
  @Ref("page") readonly page!: any;
  created() {
    this.uri = decodeURIComponent(this.$route.params.uri);

    this.soService = this.$opensilex.getService(
      "opensilex.ScientificObjectsService"
    );

    this.refresh();
  }

  visualize() {
    //build selectedNamedObject
    this.selectedNamedObjects = [];
    this.selectedObjects.forEach(value => {
      this.selectedNamedObjects.push({
        uri: value,
        name: this.namedObjectsArray[value]
      });
    });
    this.showDataVisuView = true;
    let that =this
    this.$nextTick(() =>
      setTimeout(function() {
        that.page.scrollIntoView({
          behavior: "smooth",
          block: "end",
          inline: "nearest"
        });
      }, 100)
    );
  }

  onGraphicCreated() {
    let that = this;
    setTimeout(function() {
      that.page.scrollIntoView({
        behavior: "smooth",
        block: "end",
        inline: "nearest"
      });
    }, 500);
  }

  private langUnwatcher;
  mounted() {
    this.langUnwatcher = this.$store.watch(
      () => this.$store.getters.language,
      lang => {
        this.refresh();
        if (this.selected) {
          this.displayScientificObjectDetails(this.selected.uri);
        }
      }
    );
  }

  beforeDestroy() {
    this.langUnwatcher();
  }

  resetSearch() {
    this.resetFilters();
    this.refresh();
  }

  unselectRefresh() {
    this.selected = null;
    this.selectedObjects = []; // fix bug filtre/selection
    this.showDataVisuView = false; 
    this.refresh();
  }

  get lang() {
    return this.$store.state.lang;
  }

  resetFilters() {
    this.filters = {
      name: "",
      types: [],
      parent: undefined,
      factorLevels: []
    };
    // Only if search and reset button are use in list
  }

  refresh() {
    if (this.soTree) {
      this.soTree.refresh();
      this.selected = null;
    }
  }

  loadAllChildren(node) {
    let nodeURI = node.data.uri;

    let root = this.nodes[node.path[0]];
    for (let i = 1; i < node.path.length; i++) {
      root = root.children[node.path[i]];
    }

    this.soService
      .getScientificObjectsChildren(nodeURI, this.uri)
      .then(http => {
        let childrenNodes = [];
        for (let i in http.response.result) {
          let soDTO = http.response.result[i];

          let soNode = {
            title: soDTO.name,
            data: soDTO,
            isLeaf: [],
            children: [],
            isExpanded: true,
            isSelected: false,
            isDraggable: false,
            isSelectable: true
          };
          childrenNodes.push(soNode);
        }

        root.children = childrenNodes;
      });
  }

  searchMethod(nodeURI, page, pageSize) {
    if (
      this.filters.name == "" &&
      this.filters.types.length == 0 &&
      this.filters.parent == undefined &&
      this.filters.factorLevels.length == 0
    ) {
      return this.soService.getScientificObjectsChildren(
        nodeURI,
        this.uri,
        undefined,
        page,
        pageSize
      );
    } else {
      return this.soService.searchScientificObjects(
        this.uri,
        this.filters.types,
        this.filters.name,
        this.filters.parent,
        undefined,
        this.filters.factorLevels,
        undefined,
        undefined,
        undefined,
        [],
        page,
        pageSize
      );
    }
  }

  searchParents(query, page, pageSize) {
    return this.soService
      .searchScientificObjects(
        this.uri, // experiment uri?: string,
        undefined, // rdfTypes?: Array<string>,
        query, // pattern?: string,
        undefined, // parentURI?: string,
        undefined, // Germplasm
        undefined, // factorLevels?: Array<string>,
        undefined, // facility?: string,
        undefined,
        undefined,
        [], // orderBy?: ,
        page, // page?: number,
        pageSize // pageSize?: number
      )
      .then(http => {
        let nodeList = [];
        for (let so of http.response.result) {
          nodeList.push({
            id: so.uri,
            label: so.name + " (" + so.rdf_type_name + ")"
          });
        }
        http.response.result = nodeList;
        return http;
      });
  }

  public displayScientificObjectDetailsIfNew(nodeUri: any) {
    this.showDataVisuView = false;
    if (!this.selected || this.selected.uri != nodeUri) {
      this.displayScientificObjectDetails(nodeUri);
    }
  }

  public displayScientificObjectDetails(nodeUri: any) {
    this.$opensilex.disableLoader();
    this.soService.getScientificObjectDetail(nodeUri, this.uri).then(http => {
      this.selected = http.response.result;
      this.$opensilex.enableLoader();
      this.addNamedObject(this.selected);
    });
  }

  public addNamedObject(selected) {
    let sokey = selected.uri;
    if (!this.namedObjectsArray[sokey]) {
      this.namedObjectsArray[sokey] = selected.name;
    }
  }

  public deleteScientificObject(node: any) {
    this.soService
      .deleteScientificObject(node.data.uri, this.uri)
      .then(http => {
        if (this.selected.uri == http.response.result) {
          this.selected = null;
          this.soTree.refresh();
        }
      });
  }

  exportCSV() {
    let path = "/core/scientific_objects/export";
    let today = new Date();
    let filename =
      "export_scientific_objects_" +
      today.getFullYear() +
      String(today.getMonth() + 1).padStart(2, "0") +
      String(today.getDate()).padStart(2, "0");

    this.$opensilex.downloadFilefromPostService(
      path,
      filename,
      "csv",
      {
        experiment: this.uri,
        uris: this.selectedObjects
      },
      this.lang
    );
  }

  createDocument() {
    this.documentForm.showCreateForm();
  }

  initForm() {
    return {
      description: {
        uri: undefined,
        identifier: undefined,
        rdf_type: undefined,
        title: undefined,
        date: undefined,
        description: undefined,
        targets: this.selectedObjects,
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
.async-tree-action {
  font-style: italic;
}

.async-tree-action a:hover {
  text-decoration: underline;
  cursor: pointer;
}

.card-header {
  padding-top: 0 !important;
  padding-left: 0 !important;
  padding-right: 0 !important;
}

.card-header .badge {
  margin-left: 5px;
}
</style>

<i18n>
en:
  ExperimentScientificObjects:
    import-scientific-objects: Import scientific objets
    add: Add scientific object
    update: Update scientific object
    create-scientific-object: Create scientific object
    edit-scientific-object: Edit scientific object
    delete-scientific-object: Delete scientific object
    add-scientific-object-child: Add scientific object child
    parent-label: Parent
    load-more: Load more...
    export-csv: Export CSV
    geometry-label: Geometry
    geometry-comment: Geospatial coordinates
    objectType: Object type
    name-placeholder: Enter a name
    visualize: Visualize

fr:
  ExperimentScientificObjects:
    import-scientific-objects:  Importer des objets scientifiques
    add: Ajouter un objet scientifique
    update: Mettre à jour un objet scientifiques
    create-scientific-object: Créer un objet scientifique
    edit-scientific-object:  Mettre à jour l'objet scientifique
    delete-scientific-object: Supprimer l'objet scientifique
    add-scientific-object-child: Ajouter un objet scientifique enfant
    parent-label: Parent
    load-more: Charger plus...
    export-csv: Exporter en CSV
    geometry-label: Géometrie
    geometry-comment: Coordonnées géospatialisées
    objectType: Type d'objet
    name-placeholder: Saisir un nom
    visualize: Visualiser
</i18n>
