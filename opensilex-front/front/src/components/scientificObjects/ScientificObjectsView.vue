<template>
  <div class="container-fluid">

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


      <ScientificObjectCSVImporter
        ref="importForm"
        @csvImported="soList.refresh()"
      ></ScientificObjectCSVImporter>
    </PageActions>


    <PageContent
      class="pagecontent"
    >

      <!-- Toggle Sidebar-->
      <div
        class="searchMenuContainer"
        v-on:click="searchFiltersToggle = !searchFiltersToggle"
        :title="searchFiltersPannelTitle()"
      >
        <div class="searchMenuIcon">
          <i class="icon ik ik-search"></i>
        </div>
      </div>

      <!-- FILTERS -->
      <Transition>
        <div v-show="searchFiltersToggle">

          <SearchFilterField
            @search="soList.refresh()"
            @clear="reset()"
            searchButtonLabel="component.common.search.search-button"
            :showAdvancedSearch="true"
            class="searchFilterField"
          >
            <template v-slot:filters>

              <!-- Name -->
              <div>
                <FilterField>
                  <label for="name">{{ $t("component.common.name") }}</label>
                  <StringFilter
                    id="name"
                    :filter.sync="filter.name"
                    placeholder="ScientificObjectList.name-placeholder"
                    class="searchFilter"
                    @handlingEnterKey="soList.refresh()"
                  ></StringFilter>
                  <br>
                </FilterField>
              </div>

              <!-- Experiments -->
              <div>
                <FilterField>
                  <ExperimentSelector
                    label="GermplasmList.filter.experiment"
                    :multiple="false"
                    :experiments.sync="filter.experiment"
                    class="searchFilter"
                    @handlingEnterKey="soList.refresh()"
                    :key="resetExperimentSelectorKey"
                  ></ExperimentSelector>
                </FilterField>
              </div>

              <!-- Types -->
              <div>
                <FilterField>
                  <label for="type">{{ $t("component.common.type") }}</label>
                  <ScientificObjectTypeSelector
                    id="type"
                    :types.sync="filter.types"
                    :multiple="true"
                    class="searchFilter"
                  ></ScientificObjectTypeSelector>
                </FilterField>
              </div>
            </template>

            <template v-slot:advancedSearch>

              <!-- Germplasm -->
              <div>
                <FilterField quarterWidth="false">
                  <GermplasmSelectorWithFilter
                    :germplasmsUris.sync="filter.germplasm"
                  ></GermplasmSelectorWithFilter>
                </FilterField>
              </div>

              <!-- Factors levels -->
              <div>
                <FilterField>
                  <b-form-group>
                    <label for="factorLevels">
                      {{ $t("FactorLevelSelector.label") }}
                    </label>
                    <FactorLevelSelector
                      id="factorLevels"
                      :factorLevels.sync="filter.factorLevels"
                      :multiple="true"
                      :required="false"
                      :key="resetFactorLevelSelectorKey"
                      class="searchFilter"
                    ></FactorLevelSelector>
                  </b-form-group>
                </FilterField>
              </div>

              <!-- Exists -->
              <div>
                <FilterField>
                  <DateForm
                    :value.sync="filter.existenceDate"
                    label="ScientificObjectList.existenceDate"
                    class="searchFilter"
                  ></DateForm>
                </FilterField>
              </div>

              <!-- Created -->
              <div>
                <FilterField>
                  <DateForm
                    :value.sync="filter.creationDate"
                    label="ScientificObjectList.creationDate"
                    class="searchFilter"
                  ></DateForm>
                </FilterField>
              </div>

              <!-- Criteria search -->
              <div>
                <FilterField quarterWidth="false">
                  <CriteriaSearchModalCreator
                    class="searchFilter"
                    ref="criteriaSearchCreateModal"
                    :criteria_dto.sync="filter.criteriaDto"
                    :required="false"
                    :requiredBlue="false"
                  ></CriteriaSearchModalCreator>
                </FilterField>
              </div>
            </template>
          </SearchFilterField>
        </div>
      </Transition>

      <ScientificObjectList
        ref="soList"
        :searchFilter="filter"
        @update="soForm.editScientificObject($event)"
        @createDocument="createDocument"
        @createEvents="createEvents"
        @createMoves="createMoves"
        class="scientificObjectList"
      ></ScientificObjectList>

    </PageContent>

    <ModalForm
      ref="documentForm"
      component="opensilex-DocumentForm"
      createTitle="component.common.addDocument"
      modalSize="lg"
      :initForm="initForm"
      icon="ik#ik-file-text"
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
  </div>
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
//#endregion

</script>

<style scoped lang="scss">

.createButton{
  margin-bottom: 10px;
  margin-top: -15px;
}
</style>