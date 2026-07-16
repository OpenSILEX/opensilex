<template>
  <n-space class="container-fluid">

    <PageActions
      v-if="user.hasCredential(credentials.CREDENTIAL_SCIENTIFIC_OBJECT_MODIFICATION_ID)"
    >

      <CreateButton
        @click="soForm.createScientificObject()"
        label="ExperimentScientificObjects.create-scientific-object"
        class="createButton greenThemeColor"
      ></CreateButton>
      <ScientificObjectForm
        ref="soForm"
        @onUpdate="redirectToDetail"
        @onCreate="redirectToDetail"
      ></ScientificObjectForm>

      <CreateButton
        @click="importForm.show()"
        label="OntologyCsvImporter.import"
        class="createButton greenThemeColor"
      ></CreateButton>


<!--      <ScientificObjectCSVImporter
        ref="importForm"
        @csvImported="soList.refresh()"
      ></ScientificObjectCSVImporter>-->
    </PageActions>


    <PageContent
      class="pagecontent"
    >
      <n-layout has-sider class="so-layout">
        <n-space class="mb-2 me-1" align="top">
          <n-button
            quaternary
            circle
            @click="searchFiltersToggle = !searchFiltersToggle"
            :title="searchFiltersPannelTitle()"
            :class="{ greenThemeColor: searchFiltersToggle }"
            class="globalFiltersSearchButton"
          >
            <i class="bi bi-search filtersGlobalSearchIcon"></i>

            <div
              v-show="searchFiltersToggle && activeFiltersCount > 0"
              class="filters-count-badge"
            >
              ( {{ activeFiltersCount }} )
            </div>
          </n-button>
        </n-space>

        <!-- FILTERS -->
        <n-layout-sider
          v-model:collapsed="searchFiltersToggle"
          :collapsed-width="0"
          :width="360"
          collapse-mode="width"
          show-trigger
          bordered
          class="device-sider"
        >
          <n-space class="p-3" vertical>

            <n-form size="small" @submit.prevent.stop="soList.refresh()">

                <!-- Name -->
              <n-form-item :label="t('component.common.name')"  class="compact-form-item">
                <StringFilter
                  id="name"
                  v-model:filter="filter.name"
                  :placeholder="t('ScientificObjectList.name-placeholder')"
                  @handlingEnterKey="soList.refresh()"
                  class="searchFilter"
                />
              </n-form-item>

                <!-- Experiments -->
              <n-form-item class="compact-form-item">
                <opensilex-ExperimentSelector
                  :label="t('component.experiment.view.title')"
                  v-model:experiments="filter.experiment"
                  :multiple="true"
                  class="searchFilter"
                  @handlingEnterKey="soList.refresh()"
                  :key="resetExperimentSelectorKey"
                />
              </n-form-item>

                <!-- Types -->
              <n-form-item
                :label="$t('component.common.type')"
                :show-feedback="false"
                class="compact-form-item"
              >
                <opensilex-ScientificObjectTypeSelector
                  id="type"
                  v-model:types="filter.types"
                  :multiple="true"
                  class="searchFilter"
                />
              </n-form-item>

              <!-- ADVANCED SEARCH STARTS HERE -->

              <n-collapse
                v-model:expanded-names="expandedNCollapseNames"
                :accordion="false"
                @update:expanded-names="onCollapseUpdate"
                class="advancedFiltersSearch"
              >
                <n-collapse-item :title="t('component.common.advanced-search-title')" name="adv">
                  <!-- Germplasm -->
                  <n-form-item :show-feedback="false" class="compact-form-item">
                    <GermplasmSelector
                      :multiple="false"
                      :germplasm="filter.germplasm"
                      :experiment="filter.experiment"
                      class="searchFilter"
                      @update:germplasm="filter.germplasm = $event"
                      @handlingEnterKey="soList.refresh()"
                    />
                  </n-form-item>

                  <!-- Factor levels -->
                  <n-form-item :show-feedback="false" class="compact-form-item">
                    <FactorLevelSelector
                      :factorLevels="filter.factorLevels"
                      :multiple="true"
                      :required="false"
                      class="searchFilter"
                      @update:factorLevels="filter.factorLevels = $event"
                      @handlingEnterKey="soList.refresh()"
                    />
                  </n-form-item>

                  <!-- Existence date -->
                  <n-form-item
                    :label="t('ScientificObjectModalList.filter.existenceDate')"
                    :show-feedback="false"
                  >
                    <DateForm
                      :value="filter.existenceDate"
                      class="searchFilter"
                      @update:value="filter.existenceDate = $event"
                    />
                  </n-form-item>

                  <br />

                  <!-- Creation date -->
                  <n-form-item
                    :label="t('ScientificObjectModalList.filter.creationDate')"
                    :show-feedback="false"
                  >
                    <DateForm
                      :value="filter.creationDate"
                      class="searchFilter"
                      @update:value="filter.creationDate = $event"
                    />
                  </n-form-item>

                  <!-- Criteria search -->
                  <n-form-item :show-feedback="false">
                    <CriteriaSearchModalCreator
                      ref="criteriaSearchCreateModal"
                      class="searchFilter"
                      :criteria_dto="filter.criteriaDto"
                      :required="false"
                      :requiredBlue="false"
                      @update:criteria_dto="filter.criteriaDto = $event"
                    />
                  </n-form-item>
                </n-collapse-item>
              </n-collapse>

            </n-form>
          </n-space>
        </n-layout-sider>

        <n-layout-content class="so-content">
          <ScientificObjectList
            ref="soList"
            :searchFilter="filter"
            :isSelectable="true"
            @update="soForm.editScientificObject($event)"
            @createDocument="createDocument"
            @createEvents="createEvents"
            @createMoves="createMoves"
            class="scientificObjectList"
          ></ScientificObjectList>
        </n-layout-content>
      </n-layout>
    </PageContent>

    <ModalForm
      ref="documentForm"
      component="opensilex-DocumentForm"
      createTitle="component.common.addDocument"
      modalSize="lg"
      :initForm="initForm"
      icon="bi#bi-file-earmark-text"
    ></ModalForm>

    <EventCsvForm
      ref="eventCsvForm"
      :targets="selectedUris"
    ></EventCsvForm>

    <EventCsvForm
      ref="moveCsvForm"
      :targets="selectedUris"
      :isMove="true"
    ></EventCsvForm>

    <ScientificObjectForm
      ref="soForm"
      @onUpdate="redirectToDetail"
    ></ScientificObjectForm>
  </n-space>
</template>

<script setup lang="ts">

import {computed, inject, ref} from "vue";
import EventCsvForm from "../events/form/csv/EventCsvForm.vue";
import CriteriaSearchModalCreator from "./CriteriaSearchModalCreator.vue";
import {useStore} from "vuex";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import ModalForm from "@/components/common/forms/ModalForm.vue";
import ScientificObjectList, {ScientificObjectFilter} from "@/components/scientificObjects/ScientificObjectList.vue";
import {useI18n} from "vue-i18n";
import {useRouter} from "vue-router";
import {DocForm} from "@/components/documents/DocumentForm.vue";
import CreateButton from "@/components/common/buttons/CreateButton.vue";
import PageActions from "@/components/layout/PageActions.vue";
import PageContent from "@/components/layout/PageContent.vue";
import StringFilter from "@/components/common/filters/StringFilter.vue";
import ExperimentSelector from "@/components/experiments/ExperimentSelector.vue";
import ScientificObjectTypeSelector from "@/components/scientificObjects/ScientificObjectTypeSelector.vue";
import FactorLevelSelector from "@/components/experiments/factors/FactorLevelSelector.vue";
import DateForm from "@/components/common/forms/DateForm.vue";
import ScientificObjectForm from "@/components/scientificObjects/ScientificObjectForm.vue";
import ScientificObjectCSVImporter from "@/components/scientificObjects/ScientificObjectCSVImporter.vue";
import {
  NButton,
  NCollapse,
  NCollapseItem,
  NForm,
  NFormItem,
  NLayout,
  NLayoutContent,
  NLayoutSider,
  NSpace
} from "naive-ui";
import GermplasmSelector from "@/components/germplasm/GermplasmSelector.vue";

//#region Constant values
const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!;
const $store = useStore();
const { t } = useI18n();
const $router = useRouter();
//#endregion
//#region Reactive Data
const selectedUris = ref<string[]>([]);
const filter = ref<ScientificObjectFilter>({
  name: "",
  experiment: undefined,
  germplasm: [],
  factorLevels: [],
  types: [],
  existenceDate: undefined,
  creationDate: undefined,
  criteriaDto: {criteria_list:[]}
});
const resetExperimentSelectorKey = ref(0);
const resetFactorLevelSelectorKey = ref(0);
const searchFiltersToggle = ref(false);
const loadAdvancedSearchFilters = ref(false);
const expandedNCollapseNames = ref<string[]>([])
//#endregion
//#region Template refs
const soForm = ref<InstanceType<typeof ScientificObjectForm> | null>(null);
const importForm = ref<InstanceType<typeof ScientificObjectCSVImporter> | null>(null);
const documentForm = ref<InstanceType<typeof ModalForm> | null>(null);
const eventCsvForm = ref<InstanceType<typeof EventCsvForm> | null>(null);
const moveCsvForm = ref<InstanceType<typeof EventCsvForm> | null>(null);
const soList = ref<InstanceType<typeof ScientificObjectList> | null>(null);
const criteriaSearchCreateModal = ref<InstanceType<typeof CriteriaSearchModalCreator> | null>(null);
//#endregion
//#region Computed
const user = computed(() => $store.state.user);
const credentials = computed(() => $store.state.credentials);

const activeFiltersCount = computed(() => {

  const staticFilters = [
    filter.value.name,
    filter.value.experiment,
    filter.value.germplasm,
    filter.value.factorLevels,
    filter.value.types,
    filter.value.existenceDate,
    filter.value.creationDate,
    filter.value.criteriaDto.criteria_list
  ]

  return staticFilters.filter(v => {
    if (Array.isArray(v)) return v.length > 0
    return v !== undefined && v !== null && String(v).trim() !== ''
  }).length
})
//#endregion
//#region Functions
function searchFiltersPannelTitle(): string {
  return t("searchfilter.label")
}

function redirectToDetail(http): void {
  $router.push({
    path:
      "/scientific-objects/details/" +
      encodeURIComponent(http.response.result),
  });
}

/**
 * Shows the modal form to create a new document on an OS.
 */
function createDocument(): void {
  documentForm.value.showCreateForm();
}

function createEvents(): void {
  updateSelectedUris();
  eventCsvForm.value.show();
}

function createMoves(): void{
  updateSelectedUris();
  moveCsvForm.value.show();
}

function updateSelectedUris(): void{
  selectedUris.value = [];
  for (let select of soList.value.getSelected()) {
    selectedUris.value.push(select.uri);
  }
}

function initForm(): DocForm{
  let targetURI = [];
  for (let select of soList.value.getSelected()) {
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

function reset(): void {
  filter.value = {
    name: "",
    experiment: undefined,
    germplasm: [],
    factorLevels: [],
    types: [],
    existenceDate: undefined,
    creationDate: undefined,
    criteriaDto: {criteria_list:[]}
  };
  criteriaSearchCreateModal.value.resetCriteriaListAndSave();
  soList.value.refresh();
  resetExperimentSelectorKey.value++;
  resetFactorLevelSelectorKey.value++;
}

function onCollapseUpdate(names: string[]) {
  expandedNCollapseNames.value = names
  if (names.includes('adv')) {
    loadAdvancedSearchFilters.value = true
  }
}
//#endregion

</script>

<style scoped lang="scss">

.so-layout {
  height: 100%;
  background: transparent;
}

.so-sider,
.so-content {
  height: 100%;
}

.so-sider {
  background: #fff;
  overflow: auto;
}

.so-content {
  overflow: auto;
  padding-left: 12px;
}

.filtersGlobalSearchIcon {
  font-size: 1.2em;
}

.globalFiltersSearchButton {
  width: 40px;
  height: 55px;
}

.globalFiltersSearchButton span {
  display: block !important;
}

.advancedFiltersSearch {
  margin-top: 10px;
}

</style>