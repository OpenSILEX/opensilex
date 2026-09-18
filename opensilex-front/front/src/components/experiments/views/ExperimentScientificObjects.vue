<template>
  <div ref="page">
    <PageActions class="pageActionsBtns">
      <CreateButton
          v-if="user.hasCredential(credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID)"
          @click="soForm.createScientificObject()"
          label="component.scientificObjects.actions.add"
          class="createButton"
      ></CreateButton>&nbsp;

      <CreateButton
          v-if="user.hasCredential(credentials.CREDENTIAL_SCIENTIFIC_OBJECT_MODIFICATION_ID)"
          @click="importForm.show()"
          label="component.common.import-files.csv-import"
          class="createButton"
      ></CreateButton>

      <ScientificObjectCSVImporter
          ref="importForm"
          :experimentURI="uri"
          @csvImported="refresh()"
      ></ScientificObjectCSVImporter>
    </PageActions>

    <PageContent class="pagecontent">
      <n-layout has-sider class="so-layout">
        <SearchFiltersSidebar
            :activeFiltersCount="activeFiltersCount"
            v-model:filtersCollapsed="searchFiltersToggle"
            @refresh="unselectRefresh()"
            @reset="resetSearch()"
        >
          <!-- Name -->
          <n-form-item :label="t('component.common.name')" class="compact-form-item">
            <StringFilter
                id="name"
                v-model:filter="filters.name"
                placeholder="ExperimentScientificObjects.name-placeholder"
                class="searchFilter"
                @handlingEnterKey="unselectRefresh()"
            ></StringFilter>
          </n-form-item>

          <!-- Object type -->
          <n-form-item
              :label="t('ExperimentScientificObjects.objectType')"
              :show-feedback="false"
              class="compact-form-item"
          >
            <ScientificObjectTypeSelector
                id="type"
                v-model:selected="filters.types"
                :key="refreshKey"
                :multiple="true"
                :experimentURI="uri"
                class="searchFilter"
                @handlingEnterKey="unselectRefresh()"
            ></ScientificObjectTypeSelector>
          </n-form-item>

          <!-- Parent -->
          <n-form-item
              :label="t('ExperimentScientificObjects.parent-label')"
              :show-feedback="false"
              class="compact-form-item"
          >
            <FormSelector
                id="parentFilter"
                v-model:selected="filters.parent"
                :multiple="false"
                :required="false"
                :searchMethod="searchParents"
                :placeholder="t('ExperimentScientificObjects.parent-placeholder')"
                class="searchFilter"
                @handlingEnterKey="unselectRefresh()"
            ></FormSelector>
          </n-form-item>

          <!-- Germplasm -->
          <n-form-item :show-feedback="false" class="compact-form-item">
            <GermplasmSelector
                :germplasm="filters.germplasm"
                :multiple="false"
                :experiment="uri"
                class="searchFilter"
                @update:germplasm="filters.germplasm = $event"
                @handlingEnterKey="unselectRefresh()"
            ></GermplasmSelector>
          </n-form-item>

          <!-- Factor levels -->
          <n-form-item :show-feedback="false" class="compact-form-item">
            <FactorLevelSelector
                id="factorLevels"
                v-model:factorLevels="filters.factorLevels"
                :multiple="true"
                :required="false"
                :experimentURI="uri"
                class="searchFilter"
                @handlingEnterKey="unselectRefresh()"
            ></FactorLevelSelector>
          </n-form-item>

          <!-- Criteria search -->
          <n-form-item :show-feedback="false" class="compact-form-item">
            <CriteriaSearchModalCreator
                ref="criteriaSearchCreateModal"
                v-model:criteria_dto="filters.criteriaDto"
                :required="false"
                :requiredBlue="false"
                class="searchFilter"
            ></CriteriaSearchModalCreator>
          </n-form-item>
        </SearchFiltersSidebar>

        <n-layout-content class="so-content">
          <div class="so-panels" :class="{ 'so-panels--row': searchFiltersToggle }">
            <n-card class="treePanel">
              <div class="card-header">
                <h3 class="d-inline">
                  <Icon icon="bi#bi-bullseye" class="title-icon"></Icon>
                  {{ t("component.experiment.selected-scientific-object" +
                    "") }}
                </h3>&nbsp;
                <span class="badge badge-pill greenThemeColor">{{ selectedObjects.length }}</span>

                <n-dropdown
                    :options="dropdownOptions"
                    :disabled="selectedObjects.length === 0"
                    trigger="hover"
                    class="mb-2 mr-2"
                    @select="handleDropdownAction"
                >
                  <n-button
                      size="small"
                      :disabled="selectedObjects.length === 0"
                      :class="selectedObjects.length === 0 ? 'btn-disabled' : 'greenThemeColor'"
                  >
                    {{ t('component.common.actions') }}
                  </n-button>
                </n-dropdown>

                <n-button
                    size="small"
                    :disabled="soTree && soTree.nodeList.length === 0"
                    class="greenThemeColor mb-2 mr-2"
                    @click="exportCSV(true)"
                >
                  {{ t('component.menu.experimentalDesign.btn-exportAll') }}
                </n-button>
              </div>

              <div class="row align-items-center">
                <div class="col-auto">
                  <n-checkbox
                      v-model:checked="selectAll"
                      :label="t('component.common.import-files.select-all')"
                      class="selection-box"
                      @update:checked="onSelectAll()"
                  ></n-checkbox>
                </div>
              </div>

              <TreeViewAsync
                  ref="soTree"
                  v-model:selection="selectedObjects"
                  :searchMethod="searchMethod"
                  :searchMethodRootChildren="loadAllChildren"
                  :enableSelection="true"
                  @select="displayScientificObjectDetailsIfNew($event.data.uri)"
              >
                <template v-slot:node="{ node }">
                  <span>{{ node.title }}</span>
                </template>

                <template v-slot:buttons="{ node }">
                  <n-button-group size="small" class="btn-group btn-group-sm">
                    <EditButton
                        v-if="user.hasCredential(credentials.CREDENTIAL_SCIENTIFIC_OBJECT_MODIFICATION_ID)"
                        :small="true"
                        @click="soForm.editScientificObject(node.data.uri)"
                        label="ExperimentScientificObjects.edit-scientific-object"
                    ></EditButton>
                    <AddChildButton
                        v-if="user.hasCredential(credentials.CREDENTIAL_SCIENTIFIC_OBJECT_MODIFICATION_ID)"
                        :small="true"
                        @click="soForm.createScientificObject(node.data.uri)"
                        label="ExperimentScientificObjects.add-scientific-object-child"
                    ></AddChildButton>
                    <DeleteButton
                        v-if="user.hasCredential(credentials.CREDENTIAL_SCIENTIFIC_OBJECT_DELETE_ID)"
                        :small="true"
                        @click="deleteScientificObject(node)"
                        label="ExperimentScientificObjects.delete-scientific-object"
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

            <div v-if="selected" class="selectedCard">
              <h5>
                <Icon icon="bi#bi-bullseye" class="title-icon"></Icon>
                <slot name="name">&nbsp;{{ t(selected.name) }}</slot>
              </h5>
              <ScientificObjectDetail
                  :key="selected.name"
                  :selected="selected"
                  :selectedObject="uri"
                  :tabs="detailTabs"
                  :global-view="false"
                  :experiment="uri"
                  class="experimentDetails"
              ></ScientificObjectDetail>
            </div>
          </div>
        </n-layout-content>
      </n-layout>
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
import {computed, inject, onBeforeUnmount, onMounted, ref, useTemplateRef} from "vue";
import {useRoute} from "vue-router";
import {useStore} from "vuex";
import {useI18n} from "vue-i18n";
import {NButton, NButtonGroup, NCard, NCheckbox, NFormItem, NLayout, NLayoutContent} from "naive-ui";
import {ScientificObjectsService} from "opensilex-core/index";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import PageActions from "@/components/layout/PageActions.vue";
import PageContent from "@/components/layout/PageContent.vue";
import Icon from "@/components/common/views/Icon.vue";
import TreeViewAsync from "@/components/common/views/TreeViewAsync.vue";
import CreateButton from "@/components/common/buttons/CreateButton.vue";
import EditButton from "@/components/common/buttons/EditButton.vue";
import AddChildButton from "@/components/common/buttons/AddChildButton.vue";
import DeleteButton from "@/components/common/buttons/DeleteButton.vue";
import FormSelector from "@/components/common/forms/FormSelector.vue";
import StringFilter from "@/components/common/filters/StringFilter.vue";
import SearchFiltersSidebar from "@/components/common/filters/SearchFiltersSidebar.vue";
import ScientificObjectDetail, {Tab} from "@/components/scientificObjects/ScientificObjectDetail.vue";
import ScientificObjectForm from "@/components/scientificObjects/ScientificObjectForm.vue";
import ScientificObjectCSVImporter from "@/components/scientificObjects/ScientificObjectCSVImporter.vue";
import ScientificObjectTypeSelector from "@/components/scientificObjects/ScientificObjectTypeSelector.vue";
import CriteriaSearchModalCreator, {CriteriaDTO} from "@/components/scientificObjects/CriteriaSearchModalCreator.vue";
import FactorLevelSelector from "@/components/experiments/factors/FactorLevelSelector.vue";
import GermplasmSelector from "@/components/germplasm/GermplasmSelector.vue";
import DocumentForm, {DocumentFormModel} from "@/components/documents/DocumentForm.vue";
import EventCsvForm from "@/components/events/form/csv/EventCsvForm.vue";

//#region Plugins and services
const opensilex = inject<OpenSilexVuePlugin>('$opensilex')
const route = useRoute()
const store = useStore()
const {t} = useI18n()
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
interface ScientificObjectFilters {
  name: string,
  types: Array<string>,
  parent: string,
  germplasm: string,
  factorLevels: Array<string>,
  criteriaDto: CriteriaDTO
}

function defaultFilters(): ScientificObjectFilters {
  return {
    name: "",
    types: [],
    parent: undefined,
    germplasm: undefined,
    factorLevels: [],
    criteriaDto: {criteria_list: []}
  }
}

const uri = ref<string>('')
const searchFiltersToggle = ref<boolean>(false)
const refreshKey = ref<number>(0)
const filters = ref<ScientificObjectFilters>(defaultFilters())

const selected = ref(null)
const selectedObjects = ref<Array<string>>([])
const selectAll = ref<boolean>(false)
const selectAllLimit = ref<number>(10000)

const user = computed(() => {
  return store.state.user;
})

const credentials = computed(() => {
  return store.state.credentials;
})

const lang = computed(() => {
  return store.state.lang;
})

const activeFiltersCount = computed(() => {
  const staticFilters = [
    filters.value.name,
    filters.value.types,
    filters.value.parent,
    filters.value.germplasm,
    filters.value.factorLevels,
    filters.value.criteriaDto.criteria_list
  ]

  return staticFilters.filter(v => {
    if (Array.isArray(v)) return v.length > 0
    return v !== undefined && v !== null && String(v).trim() !== ''
  }).length
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
        refresh();

        if (selected.value) {
          displayScientificObjectDetails(selected.value.uri);
        }
      }
  )
})

onBeforeUnmount(() => {
  langUnwatcher?.()
})
//#endregion

//#region Methods
/**
 * Forces the type selector to reload its options, as the available types depend on the objects of the experiment.
 */
function refreshTypeSelectorComponent() {
  refreshKey.value += 1;
}

function refresh() {
  if (soTree.value) {
    soTree.value.refresh();
    selectAll.value = false;
    onSelectAll();
    selected.value = null;
  }
}

/**
 * Refreshes the tree and drops the current selection, which would otherwise keep objects filtered out of the results.
 */
function unselectRefresh() {
  selected.value = null;
  selectedObjects.value = [];
  refresh();
}

function resetSearch() {
  resetFilters();
  refresh();
}

function resetFilters() {
  filters.value = defaultFilters();
  criteriaSearchCreateModal.value.resetCriteriaListAndSave();
}

function refreshAfterCreateOrUpdate(result) {
  refresh();
  refreshTypeSelectorComponent();
  if (!result || !result.response.result) {
    return;
  }
  displayScientificObjectDetailsIfNew(result.response.result);
}

function loadAllChildren(nodeURI: string, page: number, pageSize: number) {
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

function searchMethod(nodeURI: string, page: number, pageSize: number) {
  const orderBy = ["name=asc"];
  const hasAnyCriterion = filters.value.criteriaDto.criteria_list.length > 0;
  const hasAnyFilter = filters.value.parent
      || filters.value.germplasm
      || filters.value.name.length !== 0
      || filters.value.types.length !== 0
      || filters.value.factorLevels.length !== 0
      || hasAnyCriterion;

  if (!hasAnyFilter) {
    return soService.getScientificObjectsChildren(
        nodeURI,
        uri.value,
        undefined,
        undefined,
        undefined,
        undefined,
        orderBy,
        page,
        pageSize
    );
  }

  return soService.searchScientificObjects(
      uri.value, // experiment?: string,
      filters.value.types, // rdfTypes?: Array<string>,
      filters.value.name, // pattern?: string,
      filters.value.parent ? filters.value.parent : nodeURI, // parentURI?: string,
      filters.value.germplasm ? [filters.value.germplasm] : [], // germplasm?: Array<string>,
      filters.value.factorLevels, // factorLevels?: Array<string>,
      undefined, // facility?: string,
      undefined,
      undefined,
      undefined,
      undefined,
      JSON.stringify(filters.value.criteriaDto),
      orderBy,
      page,
      pageSize
  );
}

function searchParents(query: string, page: number, pageSize: number) {
  return soService
      .searchScientificObjects(
          uri.value, // experiment?: string,
          undefined, // rdfTypes?: Array<string>,
          query, // pattern?: string,
          undefined, // parentURI?: string,
          [], // germplasm?: Array<string>,
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
        const nodeList = [];
        for (const so of http.response.result) {
          nodeList.push({
            id: so.uri,
            label: so.name + " (" + so.rdf_type_name + ")"
          });
        }
        http.response.result = nodeList;
        return http;
      });
}

function displayScientificObjectDetailsIfNew(nodeUri: string) {
  if (!selected.value || selected.value.uri != nodeUri) {
    displayScientificObjectDetails(nodeUri);
  }
}

function displayScientificObjectDetails(nodeUri: string) {
  opensilex.disableLoader();
  soService.getScientificObjectDetail(nodeUri, uri.value).then(http => {
    selected.value = http.response.result;
    opensilex.enableLoader();
  });
}

function deleteScientificObject(node) {
  soService.deleteScientificObject(node.data.uri, uri.value)
      .then(http => {
        if (selected.value.uri == http.response.result) {
          selected.value = null;
          soTree.value.refresh();
          refreshTypeSelectorComponent();
        }
      }).catch(opensilex.errorHandler);
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
  };
}

/**
 * Exports either every object matching the current filters, or only the selected ones.
 */
function exportCSV(exportAll: boolean) {
  const path = "/core/scientific_objects/export";
  const today = new Date();
  const filename =
      "export_scientific_objects_global_" +
      today.getFullYear() + ""
      + (today.getMonth()) + ""
      + today.getDate() + "_"
      + today.getHours() + ""
      + today.getMinutes()
      + "" + today.getSeconds();

  const exportDto = {
    experiment: uri.value,
    rdf_types: filters.value.types,
    name: filters.value.name,
    factor_levels: filters.value.factorLevels,
    parent: filters.value.parent
  };

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

function onSelectAll() {
  selectedObjects.value = [];

  if (!selectAll.value) {
    return;
  }

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
      selectAllLimit.value
  ).then(http => {
    const count = http.response.metadata.pagination.totalCount;
    if (count > selectAllLimit.value) {
      alert(t('ExperimentScientificObjects.alertSelectAllLimitSize') + selectAllLimit.value);
      selectAll.value = false;
      return;
    }
    selectedObjects.value = http.response.result.map(soDTO => soDTO.uri);
  });
}
//#endregion
</script>

<style scoped lang="scss">
.pageActionsBtns .createButton {
  margin-left: 15px;
}

.createButton {
  margin-top: 1px;
  margin-left: 0;
}

.pagecontent {
  margin-top: 10px;
  width: 100%;
}

.so-layout {
  height: 100%;
  background: transparent;
}

.so-content {
  height: 100%;
  overflow: auto;
  padding-left: 12px;
}

.card-header {
  padding-top: 0 !important;
  padding-left: 0 !important;
  padding-right: 0 !important;
}

.card-header .badge {
  margin: 5px;
}

.btn-disabled {
  background-color: #e0e0e0 !important;
  color: #2e2e2e !important;
  border: none !important;
  cursor: not-allowed;
}

.selection-box {
  margin-top: 1px;
  margin-left: 24px;
}

/* Detail placed under the tree when the filters sidebar is open,
   and on its right when the sidebar is collapsed */
.so-panels {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.so-panels--row {
  flex-direction: row;
  align-items: flex-start;
}

.so-panels--row > .treePanel,
.so-panels--row > .selectedCard {
  flex: 1 1 0;
  min-width: 0;
}

.selectedCard {
  background-color: #fff;
  padding: 15px 15px 0 15px;
}

/* "Criteria on data" filter: same template as the other sidebar inputs (naive-ui selectors, 34px),
   with the button stuck to the right edge of the input */
:deep(.summary-box) {
  min-height: 34px;
  height: 34px;
  padding: 0 0 0 12px;
  border-radius: 3px;
  overflow: hidden;
}

:deep(.summary-box .summary-content) {
  overflow: hidden;
}

:deep(.summary-box .summary-actions) {
  align-self: stretch;
}

:deep(.summary-box .summary-actions .createButton) {
  height: 100%;
  margin: 0;
  padding: 0 12px;
  border: none;
  border-radius: 0;
  display: flex;
  align-items: center;
}
</style>

<i18n>
en:
  ExperimentScientificObjects:
    create-scientific-object: Add scientific object
    edit-scientific-object: Edit scientific object
    delete-scientific-object: Delete scientific object
    add-scientific-object-child: Add scientific object child
    parent-label: Parent
    parent-placeholder: Select a parent
    objectType: Object type
    name-placeholder: Enter a name
    select-all: Select all
    alertSelectAllLimitSize: The selection has too many lines for this feature, refine your search, maximum=

fr:
  ExperimentScientificObjects:
    create-scientific-object: Ajouter un objet scientifique
    edit-scientific-object:  Mettre à jour l'objet scientifique
    delete-scientific-object: Supprimer l'objet scientifique
    add-scientific-object-child: Ajouter un objet scientifique enfant
    parent-label: Parent
    parent-placeholder: Sélectionner un parent
    objectType: Type d'objet
    name-placeholder: Saisir un nom
    select-all: Tout sélectionner
    alertSelectAllLimitSize: La selection comporte trop de lignes pour cette fonctionnalité, affinez votre recherche, maximum=
</i18n>
