<template>
  <div ref="page">
    <PageActions class="pageActionsBtns">
      <CreateButton
          v-if="user.hasCredential(credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID)"
          label="ExperimentScientificObjects.create-scientific-object"
          @click="soForm.createScientificObject()"
          class="createButton">
      </CreateButton>&nbsp;

      <CreateButton
          v-if="
          user.hasCredential(credentials.CREDENTIAL_SCIENTIFIC_OBJECT_MODIFICATION_ID)
        "
          @click="importForm.show()"
          label="OntologyCsvImporter.import"
          class="createButton">
      </CreateButton>

      <ScientificObjectCSVImporter
          ref="importForm"
          :experimentURI="uri"
          @csvImported="refresh()"
      ></ScientificObjectCSVImporter>
    </PageActions>

    <PageContent class="pagecontent">
        <!-- Toggle Sidebar-->
        <div class="searchMenuContainer"
             v-on:click="searchFiltersToggle = !searchFiltersToggle"
             :title="t('searchfilter.label')">
          <div class="searchMenuIcon">
            <i class="icon ik ik-search"></i>
          </div>
        </div>

        <!-- FILTERS -->
        <Transition>
          <div v-show="searchFiltersToggle" class="card-vertical-group">
            <div class="card searchFilterField">
              <div class="card-header">
                <h3 class="mr-3">
                  <Icon class="search-icon" icon="ik#ik-search" />
                  {{ t('searchfilter.label') }}
                </h3>
              </div>

              <div class="card-body">
                <div class="container-full">
                  <div class="row">

                    <!-- Name -->
                    <div class="col col-12 col-xl-3 col-sm-6">
                      <div class="form-group">
                        <label for="name">{{ t("component.common.name") }}</label>
                        <StringFilter
                            id="name"
                            v-model:filter="filters.name"
                            placeholder="ExperimentScientificObjects.name-placeholder"
                            class="searchFilter"
                            @handlingEnterKey="refresh()"
                        ></StringFilter>
                      </div>
                    </div>

                    <!-- Object Type -->
                    <div class="col col-12 col-xl-3 col-sm-6">
                      <div class="form-group">
                        <label for="type">{{ t("ExperimentScientificObjects.objectType") }}</label>
                        <ScientificObjectTypeSelector
                            id="type"
                            v-model:selected="filters.types"
                            :multiple="true"
                            :experimentURI="uri"
                            :key="refreshKey"
                            class="searchFilter"
                            @handlingEnterKey="refresh()"
                        ></ScientificObjectTypeSelector>
                      </div>
                    </div>

                    <!-- Parent -->
                    <div class="col col-12 col-xl-3 col-sm-6">
                      <div class="form-group">
                        <label for="parentFilter">{{ t("ExperimentScientificObjects.parent-label") }}</label>
                        <FormSelector
                            id="parentFilter"
                            v-model:selected="filters.parent"
                            :multiple="false"
                            :required="false"
                            :searchMethod="searchParents"
                            :placeholder="t('ExperimentScientificObjects.parent-placeholder')"
                            class="searchFilter"
                            @handlingEnterKey="refresh()"
                        ></FormSelector>
                      </div>
                    </div>

                    <!-- Germplasm -->
                    <div class="col col-12 col-xl-3 col-sm-3">
                      <GermplasmSelector
                          :multiple="false"
                          :germplasm="filters.germplasm"
                          :experiment="uri"
                          class="searchFilter"
                          @update:germplasm="filters.germplasm = $event"
                          @handlingEnterKey="refresh()"
                      ></GermplasmSelector>
                    </div>

                    <!-- Factor Level -->
                    <div class="col col-12 col-xl-3 col-sm-6">
                      <div class="form-group">
                        <label for="factorLevels">{{ t("FactorLevelSelector.label") }}</label>
                        <FactorLevelSelector
                            id="factorLevels"
                            v-model:factorLevels="filters.factorLevels"
                            :multiple="true"
                            :required="false"
                            :experimentURI="uri"
                            class="searchFilter"
                            @handlingEnterKey="refresh()"
                        ></FactorLevelSelector>
                      </div>
                    </div>

                    <!-- Criteria search -->
                    <div class="col col-12 col-xl-3 col-sm-3">
                      <CriteriaSearchModalCreator
                          class="searchFilter"
                          ref="criteriaSearchCreateModal"
                          v-model:criteria_dto="filters.criteriaDto"
                          :required="false"
                          :requiredBlue="false"
                      ></CriteriaSearchModalCreator>
                    </div>

                  </div>
                </div>
              </div>
            </div>

            <div class="container-fluid button-group">
              <div class="row">
                <div class="col-md-12 text-right">
                  <Button
                      label="component.common.search.clear-button"
                      icon="bi-x-lg"
                      @click="resetSearch()"
                      variant="light"
                      class="mr-3"
                      :small="false"
                  ></Button>
                  <Button
                      label="component.common.search.search-button"
                      @click="unselectRefresh()"
                      icon="bi-search"
                      class="greenThemeColor createButton"
                      :small="false"
                  ></Button>
                </div>
              </div>
            </div>
          </div>
        </Transition>

        <div class="experimentScientificObjectsList"
             v-bind:style='{
      "display":(!searchFiltersToggle?"flex":"block"),
      }'>
          <div
              v-bind:style='{
          "width":(!searchFiltersToggle?"100%":"100%"),
      }'>
            <n-card>
              <div class="card-header">
                <h3 class="d-inline">
                  <Icon icon="ik#ik-target" class="title-icon" />
                  {{ t("ScientificObjectList.selected") }}
                </h3>&nbsp;
                <span class="badge badge-pill greenThemeColor" style="margin: 5px">
              {{
                    selectedObjects.length
                  }}
            </span>
                <n-dropdown
                    :options="dropdownOptions"
                    trigger="hover"
                    :disabled="selectedObjects.length == 0"
                    @select="handleDropdownAction"
                    class="mb-2 mr-2">
                  <n-button
                      size="small"
                      :disabled="selectedObjects.length == 0"
                      :class="selectedObjects.length == 0 ? 'btn-disabled' : 'greenThemeColor'"
                  >
                    actions
                  </n-button>
                </n-dropdown>
                <n-button
                    size="small"
                    class="greenThemeColor mb-2 mr-2"
                    :disabled="soTree && soTree.nodeList.length === 0"
                    @click="exportCSV(true)"
                >
                  {{ t('ScientificObjectList.export-all') }}
                </n-button>
              </div>

              <div>
                <div class="row align-items-center">
                  <div class="col-auto">
                    <n-checkbox
                        class="selection-box"
                        v-model:checked="selectAll"
                        @update:checked="onSelectAll()"
                        label="test selection à traduire"
                    >
                    </n-checkbox>
                  </div>
                </div>
              </div>

              <TreeViewAsync
                  ref="soTree"
                  :searchMethod="searchMethod"
                  :searchMethodRootChildren="loadAllChildren"
                  :enableSelection="true"
                  v-model:selection="selectedObjects"
                  @select="displayScientificObjectDetailsIfNew($event.data.uri)"
              >
                <template v-slot:node="{ node }">
              <span class="item-icon">
                <Icon :icon="opensilex.getRDFIcon(node.data.rdf_type)" />
              </span>&nbsp;
                  <span>{{ node.title }}</span>
                </template>

                <template v-slot:buttons="{ node }">
                  <n-button-group size="small" class="btn-group btn-group-sm">
                  <EditButton
                      v-if="
                  user.hasCredential(
                    credentials.CREDENTIAL_SCIENTIFIC_OBJECT_MODIFICATION_ID
                  )
                "
                      @click="soForm.editScientificObject(node.data.uri)"
                      label="ExperimentScientificObjects.edit-scientific-object"
                      :small="true"
                  ></EditButton>
                  <AddChildButton
                      v-if="
                  user.hasCredential(
                    credentials.CREDENTIAL_SCIENTIFIC_OBJECT_MODIFICATION_ID
                  )
                "
                      @click="soForm.createScientificObject(node.data.uri)"
                      label="ExperimentScientificObjects.add-scientific-object-child"
                      :small="true"
                  ></AddChildButton>
                  <DeleteButton
                      v-if="
                  user.hasCredential(
                    credentials.CREDENTIAL_SCIENTIFIC_OBJECT_DELETE_ID
                  )
                "
                      @click="deleteScientificObject(node)"
                      label="ExperimentScientificObjects.delete-scientific-object"
                      :small="true"
                  ></DeleteButton>
                  </n-button-group>
                </template>
              </TreeViewAsync>
              <ScientificObjectForm
                  v-if="user.hasCredential(credentials.CREDENTIAL_SCIENTIFIC_OBJECT_MODIFICATION_ID)"
                  ref="soForm"
                  :context="uri"
                  @onUpdate="refreshAfterCreateOrUpdate"
                  @onCreate="refreshAfterCreateOrUpdate"
              ></ScientificObjectForm>
            </n-card>
          </div>

          <div v-if="selected" class="selectedCard"
               v-bind:style='{
          "padding":(!searchFiltersToggle?"15px 15px 0 15px":"15px"),
          "margin-left":(!searchFiltersToggle?"15px":"0"),
          "width":(!searchFiltersToggle? "100%" : "100%")
      }'>
            <h5>
              <Icon icon="ik#ik-target" class="title-icon" />
              <slot name="name">&nbsp;{{ t(selected.name) }}</slot>
            </h5>
            <ScientificObjectDetail
                :key="selected.name"
                :selected="selected"
                :selectedObject="uri"
                :tabs="detailTabs"
                :global-view="false"
                :experiment="uri"
                class="experimentDetails"/>
          </div>
        </div>

      </PageContent>

      <DocumentForm
          v-if="user.hasCredential(credentials.CREDENTIAL_DOCUMENT_MODIFICATION_ID)"
          ref="documentForm"
          createTitle="component.common.addDocument"
          editTitle=""
      ></DocumentForm>

      <EventCsvForm
          v-if="user.hasCredential(credentials.CREDENTIAL_EVENT_MODIFICATION_ID)"
          ref="eventCsvForm"
          :targets="selectedObjects"
      ></EventCsvForm>

      <EventCsvForm
          v-if="user.hasCredential(credentials.CREDENTIAL_EVENT_MODIFICATION_ID)"
          ref="moveCsvForm"
          :targets="selectedObjects"
          :isMove="true"
      ></EventCsvForm>
  </div>
</template>

<script setup lang="ts">
import { ScientificObjectsService } from "opensilex-core/index";
import ScientificObjectDetail, {Tab} from "../../scientificObjects/ScientificObjectDetail.vue";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import PageActions from "@/components/layout/PageActions.vue";
import CreateButton from "@/components/common/buttons/CreateButton.vue";
import ScientificObjectCSVImporter from "@/components/scientificObjects/ScientificObjectCSVImporter.vue";
import PageContent from "@/components/layout/PageContent.vue";
import StringFilter from "@/components/common/filters/StringFilter.vue";
import ScientificObjectTypeSelector from "@/components/scientificObjects/ScientificObjectTypeSelector.vue";
import FormSelector from "@/components/common/forms/FormSelector.vue";
import FactorLevelSelector from "@/components/experiments/factors/FactorLevelSelector.vue";
import CriteriaSearchModalCreator from "@/components/scientificObjects/CriteriaSearchModalCreator.vue";
import Icon from "@/components/common/views/Icon.vue";
import TreeViewAsync from "@/components/common/views/TreeViewAsync.vue";
import EditButton from "@/components/common/buttons/EditButton.vue";
import AddChildButton from "@/components/common/buttons/AddChildButton.vue";
import DeleteButton from "@/components/common/buttons/DeleteButton.vue";
import ScientificObjectForm from "@/components/scientificObjects/ScientificObjectForm.vue";
import EventCsvForm from "@/components/events/form/csv/EventCsvForm.vue";
import DocumentForm, {DocumentFormModel} from "@/components/documents/DocumentForm.vue";
import GermplasmSelector from "@/components/germplasm/GermplasmSelector.vue";
import Button from "@/components/common/buttons/Button.vue";
import {useRoute} from "vue-router";
import {useStore} from "vuex";
import {useI18n} from "vue-i18n";
import {computed, inject, onBeforeUnmount, onMounted, ref, useTemplateRef} from "vue";
import {NButtonGroup, NCard, NCheckbox, NSpace} from "naive-ui";

//#region Plugins and services
const opensilex = inject<OpenSilexVuePlugin>('$opensilex')
const route = useRoute()
const store = useStore()
const { t } = useI18n()
const soService = opensilex.getService<ScientificObjectsService>('opensilex.ScientificObjectsService')
//#endregion

//#region Template refs
const soForm = useTemplateRef<InstanceType<typeof ScientificObjectForm>>('soForm')
const soTree = useTemplateRef<InstanceType<typeof TreeViewAsync>>('soTree')
const importForm = useTemplateRef<InstanceType<typeof ScientificObjectCSVImporter>>('importForm')
const documentForm = useTemplateRef<InstanceType<typeof DocumentForm>>('documentForm')
const eventCsvForm = useTemplateRef<InstanceType<typeof EventCsvForm>>('eventCsvForm')
const moveCsvForm = useTemplateRef<InstanceType<typeof EventCsvForm>>('moveCsvForm')
const criteriaSearchCreateModal = useTemplateRef<InstanceType<typeof CriteriaSearchModalCreator>>('criteriaSearchCreateModal')
//#endregion

//#region Data and computed
const uri = ref<string>('');
const searchFiltersToggle = ref<boolean>(false)
const refreshKey = ref(0)

const filters = ref({
  name: "",
  types: [],
  parent: undefined,
  germplasm: undefined,
  factorLevels: [],
  criteriaDto: {criteria_list: []}
});

const selected = ref(null);
const selectedObjects = ref([]);

const selectAll = ref<boolean>(false);
const selectAllLimit = ref(10000);

const user = computed(() => {
  return store.state.user;
})

const credentials = computed(() => {
  return store.state.credentials;
})

const lang = computed(() => {
  return store.state.lang;
})

const dropdownOptions = computed(() => {
  const options: Array<{ label: string, key: string }> = [];
  if (user.value.hasCredential(credentials.value.CREDENTIAL_DOCUMENT_MODIFICATION_ID)) {
    options.push({label: t('component.common.addDocument'), key: 'createDocument'});
  }
  options.push({label: 'Export CSV', key: 'exportCSVSelected'});
  if (user.value.hasCredential(credentials.value.CREDENTIAL_EVENT_MODIFICATION_ID)) {
    options.push({label: t('Event.add-multiple'), key: 'createEvents'});
    options.push({label: t('Move.add'), key: 'createMoves'});
  }
  return options;
})
//#endregion

//#region Hooks
let langUnwatcher: (() => void) | undefined

onMounted(() => {
  uri.value = decodeURIComponent(route.params.uri as string);
  refresh();

  langUnwatcher = store.watch(
      () => store.getters.language,
      () => {
        refresh()

        if (selected.value) {
          displayScientificObjectDetails(selected.value.uri)
        }
      }
  )
})

onBeforeUnmount(() => {
  langUnwatcher?.()
})
//#endregion

//#region Methods
function refreshTypeSelectorComponent() {
  refreshKey.value += 1
}

function resetSearch() {
  resetFilters();
  refresh();
}

function unselectRefresh() {
  selected.value = null;
  selectedObjects.value = []; // fix bug filtre/selection
  refresh();
}

function resetFilters() {
  filters.value = {
    name: "",
    types: [],
    parent: undefined,
    germplasm: undefined,
    factorLevels: [],
    criteriaDto: {criteria_list: []}
  };
  criteriaSearchCreateModal.value.resetCriteriaListAndSave();
  // Only if search and reset button are use in list
}

function refreshAfterCreateOrUpdate(result) {
  refresh();
  refreshTypeSelectorComponent();
  if (!result || !result.response.result) {
    return;
  }
  displayScientificObjectDetailsIfNew(result.response.result);
}

function refresh() {
  if (soTree.value) {
    soTree.value.refresh();
    selectAll.value = false;
    onSelectAll();
    selected.value = null;
  }
}

function loadAllChildren(nodeURI, page, pageSize) {
  return soService.getScientificObjectsChildren(
      nodeURI,
      uri.value,
      undefined,
      undefined,
      undefined,
      undefined,
      undefined,
      page,
      pageSize
  );
}

function searchMethod(nodeURI, page, pageSize) {

  let orderBy = ["name=asc"];
  const hasAnyCriterion = filters.value.criteriaDto.criteria_list.length > 0;
  if (filters.value.parent || filters.value.types.length !== 0 || filters.value.factorLevels.length !== 0 ||
      filters.value.name.length !== 0 || filters.value.germplasm || hasAnyCriterion) {
    return soService.searchScientificObjects(
        uri.value, // experiment uri?: string,
        filters.value.types,
        filters.value.name,
        filters.value.parent ? filters.value.parent : nodeURI,
        filters.value.germplasm ? [filters.value.germplasm] : [], // Germplasm
        filters.value.factorLevels,
        undefined, // facility?: string,
        undefined,
        undefined,
        undefined,
        undefined,
        JSON.stringify(filters.value.criteriaDto),
        orderBy,
        page,
        pageSize);

  } else {

    return soService.getScientificObjectsChildren(
        nodeURI,
        uri.value,
        undefined,
        undefined,
        undefined,
        undefined,
        orderBy,
        page,
        pageSize);
  }
}

function searchParents(query, page, pageSize) {
  return soService
      .searchScientificObjects(
          uri.value, // experiment uri?: string,
          undefined, // rdfTypes?: Array<string>,
          query, // pattern?: string,
          undefined, // parentURI?: string,
          [], // Germplasm
          undefined, // factorLevels?: Array<string>,
          undefined, // facility?: string,
          undefined,
          undefined,
          undefined,
          undefined,
          undefined,
          [],
          page,
          pageSize
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

function displayScientificObjectDetailsIfNew(nodeUri: any) {
  if (!selected.value || selected.value.uri != nodeUri) {
    displayScientificObjectDetails(nodeUri);
  }
}

function displayScientificObjectDetails(nodeUri: any) {
  opensilex.disableLoader();
  soService.getScientificObjectDetail(nodeUri, uri.value).then(http => {
    selected.value = http.response.result;
    opensilex.enableLoader();
  });
}

function deleteScientificObject(node: any) {
  soService.deleteScientificObject(node.data.uri, uri.value)
      .then(http => {
        if (selected.value.uri == http.response.result) {
          selected.value = null;
          soTree.value.refresh();
          refreshTypeSelectorComponent();
        }
      }).catch(opensilex.errorHandler);
}

function exportCSV(exportAll: boolean) {
  let path = "/core/scientific_objects/export";
  let today = new Date();
  let filename =
      "export_scientific_objects_global_" +
      today.getFullYear() + ""
      + (today.getMonth()) + ""
      + today.getDate() + "_"
      + today.getHours() + ""
      + today.getMinutes()
      + "" + today.getSeconds();

  // export all OS corresponding to filter
  let exportDto = {
    experiment: uri.value,
    rdf_types: filters.value.types,
    name: filters.value.name,
    factor_levels: filters.value.factorLevels,
    parent: filters.value.parent
  };

  // export only selected URIS
  if (!exportAll) {
    Object.assign(exportDto, {
      uris: selectedObjects.value,
    });
  }

  opensilex.downloadFilefromPostService(
      path,
      filename,
      "csv",
      exportDto,
      lang.value
  );
}

function handleDropdownAction(key: string) {
  switch (key) {
    case 'createDocument':
      createDocument();
      break;
    case 'exportCSVSelected':
      exportCSV(false);
      break;
    case 'createEvents':
      createEvents();
      break;
    case 'createMoves':
      createMoves();
      break;
  }
}

function detailTabs(objectUri: string, experimentUri?: string): Tab[] {
  return [
    {
      key: 'documents',
      label: t('component.common.details.document'),
      to: {
        name: 'ScientificObjectDocuments',
        params: {uri: objectUri, experiment: experimentUri}
      }
    },
    {
      key: 'annotations',
      label: t('component.annotation.list-title'),
      to: {
        name: 'ScientificObjectAnnotations',
        params: {uri: objectUri, experiment: experimentUri}
      }
    },
    {
      key: 'events',
      label: t('component.menu.events'),
      to: {
        name: 'ScientificObjectEvents',
        params: {uri: objectUri, experiment: experimentUri}
      }
    },
    {
      key: 'positions',
      label: t('component.common.geometry.positions'),
      to: {
        name: 'ScientificObjectPositions',
        params: {uri: objectUri, experiment: experimentUri}
      }
    },
  ];
}

function createDocument() {
  documentForm.value.showCreateForm(initForm());
}

function createEvents() {
  eventCsvForm.value.show();
}

function createMoves() {
  moveCsvForm.value.show();
}

function initForm(): DocumentFormModel {
  return {
    description: {
      uri: undefined,
      identifier: undefined,
      rdf_type: undefined,
      title: undefined,
      date: undefined,
      description: undefined,
      targets: selectedObjects.value,
      authors: undefined,
      language: undefined,
      deprecated: undefined,
      keywords: undefined
    },
    file: undefined
  }
}

function onSelectAll() {
  if (selectAll.value) {
    selectedObjects.value = [];

    soService.searchScientificObjects(
        uri.value,
        filters.value.types,
        filters.value.name,
        filters.value.parent,
        [],
        filters.value.factorLevels,
        undefined,
        undefined,
        undefined,
        undefined,
        undefined,
        JSON.stringify(filters.value.criteriaDto),
        undefined,
        0,
        selectAllLimit.value)
        .then((http) => {
          let count = http.response.metadata.pagination.totalCount;
          if (count > selectAllLimit.value) {
            alert(t('ExperimentScientificObjects.alertSelectAllLimitSize') + selectAllLimit.value);
            selectAll.value = false;
          } else {
            for (let i in http.response.result) {
              let soDTO = http.response.result[i];
              selectedObjects.value.push(soDTO.uri);
            }
          }
        })
  } else {
    selectedObjects.value = [];
  }
}

//#endregion
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

.btn-disabled {
  background-color: #e0e0e0 !important;
  color: #2e2e2e !important;
  border: none !important;
  cursor: not-allowed;
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

.listActionButtons {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 12px;
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
    parent-placeholder: Select a parent
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
    parent-placeholder: Sélectionner un parent
    export-csv: Exporter en CSV
    geometry-label: Géometrie
    geometry-comment: Coordonnées géospatialisées
    objectType: Type d'objet
    name-placeholder: Saisir un nom
    alertSelectAllLimitSize: La selection comporte trop de lignes pour cette fonctionnalité, affinez votre recherche, maximum=
</i18n>