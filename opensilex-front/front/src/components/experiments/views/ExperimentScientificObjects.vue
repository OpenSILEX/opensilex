<template>
  <div ref="page">
    <opensilex-PageActions class="pageActionsBtns">
      <opensilex-CreateButton
        v-if="user.hasCredential(credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID)"
        label="ExperimentScientificObjects.create-scientific-object"
        @click="soForm.createScientificObject()"
        class="createButton">
      </opensilex-CreateButton>&nbsp;

      <opensilex-CreateButton
        v-if="
          user.hasCredential(credentials.CREDENTIAL_SCIENTIFIC_OBJECT_MODIFICATION_ID)
        "
        @click="importForm.show()"
        label="OntologyCsvImporter.import"
        class="createButton">
      </opensilex-CreateButton>

      <opensilex-ScientificObjectCSVImporter
        ref="importForm"
        :experimentURI="uri"
        @csvImported="refresh()"
      ></opensilex-ScientificObjectCSVImporter>
    </opensilex-PageActions>


  <template>
    <opensilex-PageContent class="pagecontent">
      <!-- Toggle Sidebar--> 
      <div class="searchMenuContainer"
      v-on:click="SearchFiltersToggle = !SearchFiltersToggle"
      :title="searchFiltersPannel()">
        <div class="searchMenuIcon">
          <i class="icon ik ik-search"></i>
        </div>
      </div>


      <!-- FILTERS -->
      <Transition>
        <div v-show="SearchFiltersToggle">

        <opensilex-SearchFilterField @clear="resetSearch()" @search="unselectRefresh()" class="searchFilterField">
          <template v-slot:filters>

            <!-- Name -->
            <div>
              <opensilex-FilterField>
                <b-form-group>
                  <label for="name">{{ $t("component.common.name") }}</label>
                  <opensilex-StringFilter
                    id="name"
                    :filter.sync="filters.name"
                    placeholder="ExperimentScientificObjects.name-placeholder"
                    class="searchFilter"
                    @handlingEnterKey="refresh()"
                  ></opensilex-StringFilter>
                </b-form-group>
              </opensilex-FilterField>
            </div>

            <!-- Object Type -->
            <div>
              <opensilex-FilterField>
                <b-form-group>
                  <label for="type">{{ $t("ExperimentScientificObjects.objectType") }}</label>
                  <opensilex-ScientificObjectTypeSelector
                    id="type"
                    :types.sync="filters.types"
                    :multiple="true"
                    :experimentURI="uri"
                    :key="refreshKey"
                    class="searchFilter"
                    @handlingEnterKey="refresh()"
                  ></opensilex-ScientificObjectTypeSelector>
                </b-form-group>
              </opensilex-FilterField>
            </div>

            <!-- Parent -->
            <div>
              <opensilex-FilterField>
                <b-form-group>
                  <label for="parentFilter">{{ $t("ExperimentScientificObjects.parent-label") }}</label>
                  <opensilex-SelectForm
                    id="parentFilter"
                    :selected.sync="filters.parent"
                    :multiple="false"
                    :required="false"
                    :searchMethod="searchParents"
                    class="searchFilter"
                    @handlingEnterKey="refresh()"
                  ></opensilex-SelectForm>
                </b-form-group>
              </opensilex-FilterField>
            </div>
            
            <!-- Germplasm -->
            <div>
              <opensilex-FilterField>
                <b-form-group>
                  <opensilex-GermplasmSelector
                    :multiple="false"
                    :germplasm.sync="filters.germplasm"
                    :experiment="uri"
                    class="searchFilter"
                    @handlingEnterKey="refresh()"
                  ></opensilex-GermplasmSelector>
                </b-form-group>
              </opensilex-FilterField>
            </div>

            <!-- Factor Level -->
            <div>
              <opensilex-FilterField>
                <b-form-group>
                  <label for="factorLevels">{{ $t("FactorLevelSelector.label") }}</label>
                  <opensilex-FactorLevelSelector
                    id="factorLevels"
                    :factorLevels.sync="filters.factorLevels"
                    :multiple="true"
                    :required="false"
                    :experimentURI="uri"
                    class="searchFilter"
                    @handlingEnterKey="refresh()"
                  ></opensilex-FactorLevelSelector>
                </b-form-group>
              </opensilex-FilterField>
            </div>
            <!-- Criteria search -->
            <div>
              <opensilex-FilterField quarterWidth="false">
                <opensilex-CriteriaSearchModalCreator
                    class="searchFilter"
                    ref="criteriaSearchCreateModal"
                    :criteria_dto.sync="filters.criteriaDto"
                    :required="false"
                    :requiredBlue="false"
                ></opensilex-CriteriaSearchModalCreator>
              </opensilex-FilterField>
            </div>
          </template>
        </opensilex-SearchFilterField>
        </div>
      </Transition>

  <div class="experimentScientificObjectsList"
    v-bind:style='{
      "display":(!SearchFiltersToggle?"flex":"block"),
      }'>
      <div
        v-bind:style='{
          "width":(!SearchFiltersToggle?"100%":"100%"),
      }'>
        <b-card>
          <div class="card-header">
            <h3 class="d-inline">
              <opensilex-Icon icon="ik#ik-target" class="title-icon" />
              {{ $t("ScientificObjectList.selected") }}
            </h3>&nbsp;
            <span class="badge badge-pill greenThemeColor">
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
                    v-if="user.hasCredential(credentials.CREDENTIAL_DOCUMENT_MODIFICATION_ID)"
                    @click="createDocument()" >
                  {{$t('component.common.addDocument')}}
                </b-dropdown-item-button>
                <b-dropdown-item-button @click="exportCSV(false)">
                  Export CSV
                </b-dropdown-item-button>

                <b-dropdown-item-button
                    v-if="user.hasCredential(credentials.CREDENTIAL_EVENT_MODIFICATION_ID)"
                    @click="createEvents()">
                  {{$t('Event.add-multiple')}}
                </b-dropdown-item-button>

                <b-dropdown-item-button
                    v-if="user.hasCredential(credentials.CREDENTIAL_EVENT_MODIFICATION_ID)"
                    @click="createMoves()">
                  {{$t('Move.add')}}
                </b-dropdown-item-button>

            </b-dropdown>
            <opensilex-CreateButton
                class="mb-2 mr-2"
                @click="exportCSV(true)"
                :disabled="soTree && soTree.nodeList.length === 0"
                label="ScientificObjectList.export-all"
            ></opensilex-CreateButton>
          </div>

          <div>
            <b-row>
              <b-col cols="0">
                <b-form-checkbox
                    class="selection-box custom-control custom-checkbox"
                    v-model="selectAll"
                    @change="onSelectAll()"
                    switches
                  >
                </b-form-checkbox>
              </b-col>
              <span class="ml-1 mt-1 selectLabel"> {{!selectAll ? $t('component.common.select-all') : $t('component.common.unselect-all')}}</span>
            </b-row>
          </div>


          <opensilex-TreeViewAsync
            ref="soTree"
            :searchMethod="searchMethod"
            :searchMethodRootChildren="loadAllChildren"
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
                    credentials.CREDENTIAL_SCIENTIFIC_OBJECT_MODIFICATION_ID
                  )
                "
                @click="soForm.editScientificObject(node.data.uri)"
                label="ExperimentScientificObjects.edit-scientific-object"
                :small="true"
              ></opensilex-EditButton>
              <opensilex-AddChildButton
                v-if="
                  user.hasCredential(
                    credentials.CREDENTIAL_SCIENTIFIC_OBJECT_MODIFICATION_ID
                  )
                "
                @click="soForm.createScientificObject(node.data.uri)"
                label="ExperimentScientificObjects.add-scientific-object-child"
                :small="true"
              ></opensilex-AddChildButton>
              <opensilex-DeleteButton
                v-if="
                  user.hasCredential(
                    credentials.CREDENTIAL_SCIENTIFIC_OBJECT_DELETE_ID
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
                credentials.CREDENTIAL_SCIENTIFIC_OBJECT_MODIFICATION_ID
              )
            "
            ref="soForm"
            :context="{ experimentURI: this.uri }"
            @onUpdate="refreshAfterCreateOrUpdate"
            @onCreate="refreshAfterCreateOrUpdate"
          ></opensilex-ScientificObjectForm>
        </b-card>
      </div>
     
        <div v-if="selected" class="selectedCard"
         v-bind:style='{
          "padding":(!SearchFiltersToggle?"15px 15px 0 15px":"15px"),
          "margin-left":(!SearchFiltersToggle?"15px":"0"),
          "width":(!SearchFiltersToggle? "100%" : "100%")
      }'>
            <h5>
              <opensilex-Icon icon="ik#ik-target" class="title-icon" />
              <slot name="name">&nbsp;{{ $t(selected.name) }}</slot>
            </h5>
          <opensilex-ScientificObjectDetail
          :key="selected.name"
          :selected="selected"
          :tabs="detailTabs"
          :selectedObject="uri"

          class="experimentDetails"/>
        </div>
      
    </div>
    </opensilex-PageContent>

    <opensilex-ModalForm
      v-if="user.hasCredential(credentials.CREDENTIAL_DOCUMENT_MODIFICATION_ID)"
      ref="documentForm"
      component="opensilex-DocumentForm"
      createTitle="component.common.addDocument"
      modalSize="lg"
      :initForm="initForm"
      icon="ik#ik-settings"
    ></opensilex-ModalForm>

    <opensilex-EventCsvForm
        v-if="user.hasCredential(credentials.CREDENTIAL_EVENT_MODIFICATION_ID)"
        ref="eventCsvForm"
        :targets="selectedObjects"
    ></opensilex-EventCsvForm>

    <opensilex-EventCsvForm
        v-if="user.hasCredential(credentials.CREDENTIAL_EVENT_MODIFICATION_ID)"
        ref="moveCsvForm"
        :targets="selectedObjects"
        :isMove="true"
    ></opensilex-EventCsvForm>

  </template>
  </div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
import { ScientificObjectsService } from "opensilex-core/index";
import ScientificObjectDetail from "../../scientificObjects/ScientificObjectDetail.vue";
import EventCsvForm from "../../events/form/csv/EventCsvForm.vue";
import TreeViewAsync from "../../common/views/TreeViewAsync.vue";
import {User} from "../../../models/User";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import ScientificObjectForm from "../../scientificObjects/ScientificObjectForm.vue";
@Component
export default class ExperimentScientificObjects extends Vue {
  $opensilex: OpenSilexVuePlugin;
  $route: any;
  $store: any;
  $t: any;
  soService: ScientificObjectsService;
  uri: string;
  numberOfSelectedRows = 0;
  SearchFiltersToggle : boolean = false;

  refreshKey = 0;

  refreshTypeSelectorComponent(){
    this.refreshKey += 1
  }


  
  @Ref("soForm") readonly soForm!: ScientificObjectForm;
  @Ref("soTree") readonly soTree!: TreeViewAsync;
  @Ref("importForm") readonly importForm!: any;
  @Ref("documentForm") readonly documentForm!: any;
  @Ref("eventCsvForm") readonly eventCsvForm!: EventCsvForm;
  @Ref("moveCsvForm") readonly moveCsvForm!: EventCsvForm;

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

  get user(): User {
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
    germplasm: undefined,
    factorLevels: [],
    criteriaDto: undefined
  };

  public selected = null;
  public selectedObject = null

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
      germplasm: undefined,
      factorLevels: [],
      criteriaDto: undefined

    };
    // Only if search and reset button are use in list
  }

  refreshAfterCreateOrUpdate(result){
      this.refresh();
      this.refreshTypeSelectorComponent();
      if(! result || ! result.response.result) {
        return;
      }
      this.displayScientificObjectDetailsIfNew(result.response.result);
  }

  refresh() {
    if (this.soTree) {
      this.soTree.refresh();
      this.selectAll = false;
      this.onSelectAll();
      this.selected = null;
    }
    
  }

  loadAllChildren(nodeURI,page,pageSize) {
    return this.soService.getScientificObjectsChildren(
        nodeURI,
        this.uri,
        undefined,
        undefined,
        undefined,
        undefined,
        undefined,
        page,
        pageSize
    );
  }

  searchMethod(nodeURI, page, pageSize) {

    let orderBy = ["name=asc"];
    if(this.filters.parent || this.filters.types.length !== 0 || this.filters.factorLevels.length !== 0 || this.filters.name.length !== 0 || this.filters.germplasm || this.filters.criteriaDto) {
       return this.soService.searchScientificObjects(
        this.uri, // experiment uri?: string,
        this.filters.types, 
        this.filters.name, 
        this.filters.parent ? this.filters.parent : nodeURI, 
        this.filters.germplasm, // Germplasm
        this.filters.factorLevels, 
        undefined, // facility?: string,
        undefined,
        undefined,
         JSON.stringify(this.filters.criteriaDto),
        orderBy,
        page,
        pageSize );

    } else {

        return this.soService.getScientificObjectsChildren(
        nodeURI,
        this.uri,
        undefined,
        undefined,
        undefined,
        undefined,
        orderBy,
        page,
        pageSize );
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
          undefined,
          [],// orderBy?: ,
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
    if (!this.selected || this.selected.uri != nodeUri) {
      this.displayScientificObjectDetails(nodeUri);
    }
  }

  public displayScientificObjectDetails(nodeUri: any) {
    this.$opensilex.disableLoader();
    this.soService.getScientificObjectDetail(nodeUri, this.uri).then(http => {
      this.selected = http.response.result;
      this.selectedObject = this.selected.uri
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
    this.soService.deleteScientificObject(node.data.uri, this.uri)
      .then(http => {
        if (this.selected.uri == http.response.result) {
          this.selected = null;
          this.soTree.refresh();
          this.refreshTypeSelectorComponent();
        }
      }).catch(this.$opensilex.errorHandler);
  }

  exportCSV(exportAll: boolean) {
    let path = "/core/scientific_objects/export";
    let today = new Date();
    let filename =
      "export_scientific_objects_global_" +
        today.getFullYear() + ""
        + (today.getMonth()) + "" 
        + today.getDate() + "_"
        +  today.getHours() + ""
        + today.getMinutes()
        + "" + today.getSeconds();

    // export all OS corresponding to filter
    let exportDto = {
      experiment: this.uri,
      rdf_types: this.filters.types,
      name: this.filters.name,
      factor_levels: this.filters.factorLevels,
      parent: this.filters.parent
    };

    // export only selected URIS
    if (!exportAll) {
      Object.assign(exportDto, {
        uris: this.selectedObjects,
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

  createDocument() {
    this.documentForm.showCreateForm();
  }

  createEvents(){
    this.eventCsvForm.show();
  }

  createMoves(){
    this.moveCsvForm.show();
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

  selectAll = false;
  selectAllLimit = 10000;
  
  onSelectAll(){
    if (this.selectAll) {
      this.selectedObjects = [];

      this.soService.searchScientificObjects(
        this.uri, 
        this.filters.types,
        this.filters.name,
        this.filters.parent,
        undefined, 
        this.filters.factorLevels,
        undefined, 
        undefined, 
        undefined,
          JSON.stringify(this.filters.criteriaDto),
          undefined,
        0,
        this.selectAllLimit)
      .then((http) => {
        let count = http.response.metadata.pagination.totalCount;
          if(count > this.selectAllLimit) {
            alert(this.$t('ExperimentScientificObjects.alertSelectAllLimitSize') + this.selectAllLimit);
            this.selectAll=false;
          }
          else {
            for (let i in http.response.result) {
              let soDTO = http.response.result[i];
              this.selectedObjects.push(soDTO.uri);
            }  
              this.numberOfSelectedRows = this.selectedObjects.length;
              return this.selectedObjects;
            }
        })
      }
    else {
      this.selectedObjects = [];
      this.numberOfSelectedRows = this.selectedObjects.length;
    }
  }

  searchFiltersPannel() {
    return  this.$t("searchfilter.label")
  }
}
</script>

<style scoped lang="scss">
.selection-box {
  margin-top: 1px;
  margin-left: 24px;
}

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

.createButton, .helpButton{
  margin-top: 1px;
  margin-left: 0;
}

.pageActionsBtns .createButton{
  margin-left: 15px;
}

.selectLabel {
  font-weight: bold;
}

.pagecontent{
  margin-top : 10px;
  width: 100%

}

.selectedCard {
  background-color: #fff;
  padding: 15px 15px 0 15px;
}

</style>

<i18n>
en:
  ExperimentScientificObjects:
    import-scientific-objects: Import scientific objets
    add: Add scientific object
    update: Update scientific object
    create-scientific-object: Add scientific object
    edit-scientific-object: Edit scientific object
    delete-scientific-object: Delete scientific object
    add-scientific-object-child: Add scientific object child
    parent-label: Parent
    export-csv: Export CSV
    geometry-label: Geometry
    geometry-comment: Geospatial coordinates
    objectType: Object type
    name-placeholder: Enter a name
    alertSelectAllLimitSize: The selection has too many lines for this feature, refine your search, maximum= 

fr:
  ExperimentScientificObjects:
    import-scientific-objects:  Importer des objets scientifiques
    add: Ajouter un objet scientifique
    update: Mettre à jour un objet scientifiques
    create-scientific-object: Ajouter un objet scientifique
    edit-scientific-object:  Mettre à jour l'objet scientifique
    delete-scientific-object: Supprimer l'objet scientifique
    add-scientific-object-child: Ajouter un objet scientifique enfant
    parent-label: Parent
    export-csv: Exporter en CSV
    geometry-label: Géometrie
    geometry-comment: Coordonnées géospatialisées
    objectType: Type d'objet
    name-placeholder: Saisir un nom
    alertSelectAllLimitSize: La selection comporte trop de lignes pour cette fonctionnalité, affinez votre recherche, maximum= 
</i18n>