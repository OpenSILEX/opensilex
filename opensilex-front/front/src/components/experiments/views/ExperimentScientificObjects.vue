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
          <n-form-item
              :label="t('component.common.name')" class="compact-form-item">
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
                <slot name="name">&nbsp;{{ selected.name }}</slot>
              </h5>

              <nav class="tabs mb-3">
                <button
                    v-for="tab in detailTabs"
                    :key="tab.key"
                    type="button"
                    :class="['tab', { active: currentDetailTab === tab.key }]"
                    @click="currentDetailTab = tab.key"
                >
                  {{ tab.label }}
                  <span v-if="tab.count > 0" class="tabBadge">
                    {{ opensilex.$numberFormatter.formateResponse(tab.count) }}
                  </span>
                </button>
              </nav>

              <component
                  :is="currentDetailTabComponent"
                  :key="selected.uri"
                  v-bind="currentDetailTabProps"
                  v-on="currentDetailTabListeners"
              ></component>
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
import {computed, defineAsyncComponent, inject, onBeforeUnmount, onMounted, ref, useTemplateRef} from "vue";
import {useRoute} from "vue-router";
import {useStore} from "vuex";
import {useI18n} from "vue-i18n";
import {NButton, NButtonGroup, NCard, NCheckbox, NFormItem, NLayout, NLayoutContent} from "naive-ui";
import {ScientificObjectsService} from "opensilex-core/index";
import {EventsService} from "opensilex-core/api/events.service";
import {AnnotationsService} from "opensilex-core/api/annotations.service";
import {DocumentsService} from "opensilex-core/api/documents.service";
import {PositionsService} from "opensilex-core/api/positions.service";
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
import ScientificObjectForm from "@/components/scientificObjects/ScientificObjectForm.vue";
import ScientificObjectCSVImporter from "@/components/scientificObjects/ScientificObjectCSVImporter.vue";
import ScientificObjectTypeSelector from "@/components/scientificObjects/ScientificObjectTypeSelector.vue";
import CriteriaSearchModalCreator, {CriteriaDTO} from "@/components/scientificObjects/CriteriaSearchModalCreator.vue";
import FactorLevelSelector from "@/components/experiments/factors/FactorLevelSelector.vue";
import GermplasmSelector from "@/components/germplasm/GermplasmSelector.vue";
import DocumentForm, {DocumentFormModel} from "@/components/documents/DocumentForm.vue";
import EventCsvForm from "@/components/events/form/csv/EventCsvForm.vue";
import ScientificObjectDetailProperties from "@/components/scientificObjects/scientificObjectDetailTabs/ScientificObjectDetailProperties.vue";

//#region Plugins and services
const opensilex = inject<OpenSilexVuePlugin>('$opensilex')
const route = useRoute()
const store = useStore()
const {t} = useI18n()
const soService = opensilex.getService<ScientificObjectsService>('opensilex.ScientificObjectsService')
const eventsService = opensilex.getService<EventsService>('opensilex.EventsService')
const annotationsService = opensilex.getService<AnnotationsService>('opensilex.AnnotationsService')
const documentsService = opensilex.getService<DocumentsService>('opensilex.DocumentsService')
const positionsService = opensilex.getService<PositionsService>('opensilex.PositionsService')
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
const searchFiltersToggle = ref<boolean>(true)
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

/**
 * Tabs of the inline detail panel, mirroring the Vue 2 behaviour: they are purely local, so selecting a tab never
 * leaves the experiment page and the tree stays visible. The routed tabs of ScientificObjectDetail cannot be reused
 * here, as they would navigate to the global scientific object detail page.
 */
const detailTabComponents = {
  details: ScientificObjectDetailProperties,
  events: defineAsyncComponent(() => import("@/components/events/list/EventList.vue")),
  positions: defineAsyncComponent(() => import("@/components/positions/list/PositionList.vue")),
  annotations: defineAsyncComponent(() => import("@/components/annotations/list/AnnotationList.vue")),
  documents: defineAsyncComponent(() => import("@/components/documents/DocumentTabList.vue"))
}

type DetailTabKey = keyof typeof detailTabComponents

const detailTabDefinitions: Array<{ key: DetailTabKey, labelKey: string }> = [
  {key: 'details', labelKey: 'component.common.details-label'},
  {key: 'events', labelKey: 'component.menu.events'},
  {key: 'positions', labelKey: 'component.common.geometry.positions'},
  {key: 'annotations', labelKey: 'component.annotation.list-title'},
  {key: 'documents', labelKey: 'component.common.details.document'}
]

const currentDetailTab = ref<DetailTabKey>('details')

const eventQuantity = ref<number>(0)
const positionQuantity = ref<number>(0)
const annotationQuantity = ref<number>(0)
const documentQuantity = ref<number>(0)

const detailTabs = computed(() => {
  const counts: Record<DetailTabKey, number> = {
    details: 0,
    events: eventQuantity.value,
    positions: positionQuantity.value,
    annotations: annotationQuantity.value,
    documents: documentQuantity.value
  }

  return detailTabDefinitions.map(({key, labelKey}) => ({
    key,
    label: t(labelKey),
    count: counts[key]
  }))
})

const currentDetailTabComponent = computed(() => detailTabComponents[currentDetailTab.value])

const currentDetailTabProps = computed(() => {
  const objectUri = selected.value?.uri;
  if (!objectUri) {
    return {};
  }

  switch (currentDetailTab.value) {
    case 'details':
      return {
        selected: selected.value,
        experiment: uri.value,
        globalView: false
      };
    case 'events':
      return {
        target: objectUri,
        context: uri.value,
        columnsToDisplay: new Set(['type', 'start', 'end', 'description']),
        displayTargetFilter: false,
        maximizeFilterSize: true,
        enableActions: true,
        modificationCredentialId: credentials.value.CREDENTIAL_EVENT_MODIFICATION_ID,
        deleteCredentialId: credentials.value.CREDENTIAL_EVENT_DELETE_ID
      };
    case 'positions':
      return {
        target: objectUri,
        columnsToDisplay: new Set(['end']),
        enableActions: true,
        modificationCredentialId: credentials.value.CREDENTIAL_EVENT_MODIFICATION_ID,
        deleteCredentialId: credentials.value.CREDENTIAL_EVENT_DELETE_ID
      };
    case 'annotations':
      return {
        target: objectUri,
        displayTargetColumn: false,
        enableActions: true,
        modificationCredentialId: credentials.value.CREDENTIAL_ANNOTATION_MODIFICATION_ID,
        deleteCredentialId: credentials.value.CREDENTIAL_ANNOTATION_DELETE_ID
      };
    case 'documents':
      return {
        uri: objectUri,
        modificationCredentialId: credentials.value.CREDENTIAL_DOCUMENT_MODIFICATION_ID
      };
    default:
      return {};
  }
})

const currentDetailTabListeners = computed(() => {
  switch (currentDetailTab.value) {
    case 'events':
      // EventList only emits onDelete for now, so the badge is refreshed on deletion.
      return {onDelete: countEvents};
    case 'positions':
      return {changed: countPositions, onDelete: countPositions};
    case 'annotations':
      return {changed: countAnnotations, onDelete: countAnnotations};
    case 'documents':
      return {changed: countDocuments};
    default:
      return {};
  }
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
  // Switching to another object restarts the detail panel on its first tab, as the Vue 2 version did by remounting it.
  if (selected.value?.uri !== nodeUri) {
    currentDetailTab.value = 'details';
  }

  opensilex.disableLoader();
  soService.getScientificObjectDetail(nodeUri, uri.value)
      .then(http => {
        selected.value = http.response.result;
        refreshDetailCounts();
      })
      .catch(opensilex.errorHandler)
      .finally(() => opensilex.enableLoader());
}

/**
 * Reloads the badge counts of the detail panel tabs for the currently selected object.
 */
function refreshDetailCounts() {
  eventQuantity.value = 0;
  positionQuantity.value = 0;
  annotationQuantity.value = 0;
  documentQuantity.value = 0;

  countEvents();
  countPositions();
  countAnnotations();
  countDocuments();
}

function countEvents() {
  const objectUri = selected.value?.uri;
  if (!objectUri) {
    return;
  }
  return eventsService.countEvents([objectUri], undefined, undefined)
      .then(http => {
        eventQuantity.value = http.response.result as number;
      })
      .catch(opensilex.errorHandler);
}

function countPositions() {
  const objectUri = selected.value?.uri;
  if (!objectUri) {
    return;
  }
  return positionsService.countMoves(objectUri, undefined, undefined)
      .then(http => {
        positionQuantity.value = http.response.result as number;
      })
      .catch(opensilex.errorHandler);
}

function countAnnotations() {
  const objectUri = selected.value?.uri;
  if (!objectUri) {
    return;
  }
  return annotationsService.countAnnotations(objectUri, undefined, undefined)
      .then(http => {
        annotationQuantity.value = http.response.result as number;
      })
      .catch(opensilex.errorHandler);
}

function countDocuments() {
  const objectUri = selected.value?.uri;
  if (!objectUri) {
    return;
  }
  return documentsService.countDocuments(objectUri, undefined, undefined)
      .then(http => {
        documentQuantity.value = http.response.result as number;
      })
      .catch(opensilex.errorHandler);
}

function deleteScientificObject(node) {
  const deletedUri = node.data.uri;

  soService.deleteScientificObject(deletedUri, uri.value)
      .then(() => {
        // The detail panel only closes when it was showing the deleted object.
        if (selected.value?.uri === deletedUri) {
          selected.value = null;
        }
        selectedObjects.value = selectedObjects.value.filter(soUri => soUri !== deletedUri);
        soTree.value?.refresh();
        refreshTypeSelectorComponent();
      })
      .catch(opensilex.errorHandler);
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
