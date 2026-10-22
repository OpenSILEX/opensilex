<template>
  <div class="container-fluid">
    <PageActions>
      <!-- Create Button -->
      <CreateButton
          v-if="user.hasCredential(
              credentials.CREDENTIAL_DATA_MODIFICATION_ID)"
          @click="showImportForm()"
          label="component.common.import-files.csv-import"
          class="createButton greenThemeColor"
      >
      </CreateButton>
      <!-- Export button-->
      <Button
          @click="exportModal.show()"
          class="createButton greenThemeColor"
          icon="bi-download"
          :small="false"
          label="component.data.view.buttons.export"
      ></Button>
      <!-- Delete by batch button -->
      <Button
          @click="deleteByBatchModal.show()"
          class="createButton greenThemeColor"
          icon="fa#trash-alt"
          :small="false"
          label="component.data.view.buttons.delete-by-batch"
          :disabled="false"
      ></Button>
    </PageActions>


    <DataExportModal
        ref="exportModal"
        :filter="filter"
    ></DataExportModal>

    <DeleteByBatchModal
        ref="deleteByBatchModal"
        @deleted="refresh"
        experiment-uri="">
    </DeleteByBatchModal>

    <DataImportForm
        v-if="renderImportForm"
        ref="modalDataForm"
        @onCreate="afterCreateData"
    ></DataImportForm>

    <PageContent
        class="pagecontent">
      <n-layout has-sider class="data-layout">
        <!-- FILTERS -->
        <SearchFiltersSidebar
            v-model:filtersCollapsed="toggleSearchFilters"
            :activeFiltersCount="activeFiltersCount"
            searchButtonLabelTranslationKey="component.data.view.filter.label"
            @refresh="refresh()"
            @reset="reset()"
        >
          <!-- Variables -->
          <n-form-item class="compact-form-item">
            <VariableSelectorWithFilter
                label="component.data.view.filter.variables"
                placeholder="component.variable.placeholder-multiple"
                v-model:variables="filter.variables"
                :withAssociatedData="true"
                class="searchFilter"
            ></VariableSelectorWithFilter>
          </n-form-item>

          <!-- Experiments -->
            <ExperimentSelector
                :label="t('component.data.view.filter.experiments')"
                v-model:experiments="filter.experiments"
                :multiple="true"
                @select="updateSOFilter"
                @clear="updateSOFilter"
                class="searchFilter"
                :key="resetExperimentSelectorKey"
            ></ExperimentSelector>

          <!-- Scientific objects -->
          <n-form-item class="compact-form-item">
            <ModalFormSelector
                ref="soSelector"
                :label="t('component.data.view.filter.scientificObjects')"
                :placeholder="t('component.data.view.filter.scientificObjects-placeholder')"
                v-model:selected="filter.scientificObjects"
                modalComponent="opensilex-ScientificObjectModalList"
                class="searchFilter"
                v-model:filter="soFilter"
                :clearable="true"
                :multiple="true"
                @clear="refreshSoSelector"
                @onValidate="refreshComponent"
                @onClose="refreshComponent"
                @select="refreshComponent"
                :limit="1"
            ></ModalFormSelector>
          </n-form-item>

          <!-- Germplasm Group -->
          <GermplasmGroupSelector
              label="component.data.view.filter.germplasm-group"
              :multiple="false"
              v-model:germplasmGroup="filter.germplasm_group"
              class="searchFilter"
              @handlingEnterKey="refresh()"
          ></GermplasmGroupSelector>

          <!-- germplasm -->
          <n-form-item class="compact-form-item">
            <GermplasmSelectorWithFilter
                v-model:germplasmUris="filter.germplasm"
                edit-mode
                experiment-uri="">
            </GermplasmSelectorWithFilter>
          </n-form-item>

          <!-- Devices in provenance-->
          <DeviceSelector
              ref="deviceSelector"
              :multiple="true"
              v-model:value="filter.devices"
              class="searchFilter"
              @handlingEnterKey="refresh()"
          ></DeviceSelector>

          <!-- Operator -->
          <n-form-item class="compact-form-item">
            <PersonSelector
                v-model:persons="filter.operators"
                label="component.data.view.filter.operator"
                class="searchFilter"
                @handlingEnterKey="refresh()"
                :multiple="true"
            ></PersonSelector>
          </n-form-item>

          <!-- Facility -->
          <FacilitySelector
              ref="facilitySelector"
              label="component.experiment.facilities"
              v-model:facilities="filter.facilities"
              :multiple="true"
              class="searchFilter"
          ></FacilitySelector>

          <!-- Start Date -->
          <n-form-item class="compact-form-item">
            <DateTimeForm
                v-model:value="filter.start_date"
                label="component.common.date-time.begin"
                name="startDate"
                :max-date="filter.end_date ? filter.end_date : undefined"
                class="searchFilter"
            ></DateTimeForm>
          </n-form-item>

          <!-- End Date -->
          <n-form-item class="compact-form-item">
            <DateTimeForm
                v-model:value="filter.end_date"
                label="component.common.date-time.end"
                name="endDate"
                :min-date="filter.start_date ? filter.start_date : undefined"
                class="searchFilter"
            ></DateTimeForm>
          </n-form-item>

          <!-- Provenance -->
          <n-form-item class="compact-form-item">
            <DataProvenanceSelector
                ref="provSelector"
                v-model:provenances="filter.provenance"
                label="component.data.view.filter.provenance"
                @select="loadProvenance"
                :targets="filter.scientificObjects"
                :experiments="filter.experiments"
                :multiple="false"
                :viewHandler="showProvenanceDetails"
                :viewHandlerDetailsVisible="visibleDetails"
                :key="refreshKey"
                class="searchFilter"
                @handlingEnterKey="refresh()"
            ></DataProvenanceSelector>
          </n-form-item>

          <n-collapse-transition
              v-if="selectedProvenance"
              :show="visibleDetails"
          >
            <ProvenanceDetails
                :provenance="selectedProvenance"
            ></ProvenanceDetails>
          </n-collapse-transition>

          <!-- ADVANCED SEARCH -->
          <n-collapse
              :accordion="false"
              @update:expanded-names="loadAdvancedFilter"
              class="advancedFiltersSearch"
          >
            <n-collapse-item :title="t('component.common.advanced-search-title')" name="adv">
              <!-- Targets -->
              <n-form-item class="compact-form-item">
                <TagInputForm
                    v-if="loadAdvancedSearchFilters"
                    class="searchFilter"
                    v-model:value="filter.targets"
                    label="component.data.view.filter.targets"
                    helpMessage="component.data.view.filter.targets-help"
                    type="text"
                    @handlingEnterKey="refresh()"
                ></TagInputForm>
              </n-form-item>

              <!-- Batch URI -->
              <n-form-item :label="t('component.data.view.filter.batch-uri')" class="compact-form-item">
                <StringFilter
                    v-model:filter="filter.batch_uri"
                    placeholder="component.data.view.filter.uri-placeholder"
                    class="searchFilter"
                    @handlingEnterKey="refresh()"
                ></StringFilter>
              </n-form-item>
            </n-collapse-item>
          </n-collapse>
        </SearchFiltersSidebar>

        <n-layout-content class="data-content">
          <DataList
              ref="dataList"
              v-model:listFilter="filter"
              class="dataList">
          </DataList>
        </n-layout-content>
      </n-layout>
    </PageContent>

    <ResultModalView ref="resultModal" @onHide="refreshDataAfterImportation()"></ResultModalView>

    <DataProvenanceModalView
        ref="dataProvenanceModalView"
    ></DataProvenanceModalView>
  </div>
</template>

<script setup lang="ts">
import {computed, inject, nextTick, ref} from "vue";
import {useStore} from "vuex";
import {useI18n} from "vue-i18n";
import {
  NButton,
  NCollapse,
  NCollapseItem,
  NCollapseTransition,
  NFormItem,
  NLayout,
  NLayoutContent
} from "naive-ui";
import type HttpResponse from "@/lib/HttpResponse";
import type {OpenSilexResponse} from "@/lib/HttpResponse";
import type {DataService} from "opensilex-core/api/data.service";
import type {ProvenanceGetDTO} from "opensilex-core/index";
import type OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
// import DeleteByBatchModal from "./DeleteByBatchModal.vue";
import DataProvenanceModalView from "@/components/data/DataProvenanceModalView.vue";
import ProvenanceDetails from "@/components/data/ProvenanceDetails.vue";
// import DataExportModal from "@/components/data/DataExportModal.vue";
import DataList from "@/components/data/DataList.vue";
// import ResultModalView from "@/components/data/ResultModalView.vue";
// import DataProvenanceSelector from "@/components/data/DataProvenanceSelector.vue";
// import ModalForm from "@/components/common/forms/ModalForm.vue";
import PageActions from "@/components/layout/PageActions.vue";
import PageContent from "@/components/layout/PageContent.vue";
import CreateButton from "@/components/common/buttons/CreateButton.vue";
import Button from "@/components/common/buttons/Button.vue";
import SearchFiltersSidebar from "@/components/common/filters/SearchFiltersSidebar.vue";
import ExperimentSelector from "@/components/experiments/ExperimentSelector.vue";
import VariableSelectorWithFilter from "@/components/variables/views/VariableSelectorWithFilter.vue";
import ModalFormSelector from "@/components/variables/form/ModalFormSelector.vue";
// import GermplasmGroupSelector from "@/components/germplasmGroup/GermplasmGroupSelector.vue";
// import GermplasmSelectorWithFilter from "@/components/germplasm/GermplasmSelectorWithFilter.vue";
import PersonSelector from "@/components/persons/PersonSelector.vue";
import DateTimeForm from "@/components/common/forms/DateTimeForm.vue";
import TagInputForm from "@/components/common/forms/TagInputForm.vue";
import StringFilter from "@/components/common/filters/StringFilter.vue";
import DeviceSelector from "@/components/devices/DeviceSelector.vue";
import FacilitySelector from "@/components/facilities/FacilitySelector.vue";
import DataExportModal from "@/components/data/DataExportModal.vue";
import DataProvenanceSelector from "@/components/data/DataProvenanceSelector.vue";
import ResultModalView from "@/components/data/ResultModalView.vue";
import DeleteByBatchModal from "@/components/data/DeleteByBatchModal.vue";
import ModalForm from "@/components/common/forms/ModalForm.vue";
import DataImportForm from "@/components/data/form/DataImportForm.vue";
import GermplasmGroupSelector from "@/components/germplasm/GermplasmGroupSelector.vue";
import GermplasmSelectorWithFilter from "@/components/germplasm/GermplasmSelectorWithFilter.vue";

//#region Constant values
const $opensilex = inject<OpenSilexVuePlugin>("$opensilex");
const $store = useStore();
const {t} = useI18n();
const dataService = $opensilex.getService<DataService>("opensilex.DataService");
//#endregion
//#region Reactive Data
const visibleDetails = ref<boolean>(false);
const selectedProvenance = ref<ProvenanceGetDTO>(null);
const filterProvenanceLabel = ref<string>(null);
const refreshKey = ref<number>(0);
const resetExperimentSelectorKey = ref<number>(0);
const toggleSearchFilters = ref<boolean>(true);
const loadAdvancedSearchFilters = ref<boolean>(false);
const renderImportForm = ref<boolean>(false);

const filter = ref({
  germplasm_group: undefined,
  start_date: null,
  end_date: null,
  variables: [],
  provenance: null,
  experiments: [],
  scientificObjects: [],
  targets: [],
  devices: [],
  facilities: [],
  operators: [],
  germplasm: [],
  batch_uri: undefined
});

const soFilter = ref({
  name: "",
  experiment: undefined,
  germplasm: undefined,
  factorLevels: [],
  types: [],
  existenceDate: undefined,
  creationDate: undefined,
  criteriaDto: {criteria_list: []}
});
//#endregion
//#region Template refs
const dataList = ref<InstanceType<typeof DataList> | null>(null);
const modalDataForm = ref<InstanceType<typeof DataImportForm> | null>(null);
const provSelector = ref<InstanceType<typeof DataProvenanceSelector> | null>(null);
const resultModal = ref<InstanceType<typeof ResultModalView> | null>(null);
const soSelector = ref<InstanceType<typeof ModalFormSelector> | null>(null);
const exportModal = ref<InstanceType<typeof DataExportModal> | null>(null);
const deviceSelector = ref<InstanceType<typeof DeviceSelector> | null>(null);
const facilitySelector = ref<InstanceType<typeof FacilitySelector> | null>(null);
const deleteByBatchModal = ref<InstanceType<typeof DeleteByBatchModal> | null>(null);
const dataProvenanceModalView = ref<InstanceType<typeof DataProvenanceModalView> | null>(null);
//#endregion
//#region Computed
const user = computed(() => $store.state.user);
const credentials = computed(() => $store.state.credentials);

const activeFiltersCount = computed(() => {
  return Object.values(filter.value).filter(v => {
    if (Array.isArray(v)) return v.length > 0
    return v !== undefined && v !== null && String(v).trim() !== ''
  }).length
});
//#endregion
//#region Functions
function refreshSoSelector() {
  soSelector.value.refreshModalSearch();
  refreshComponent();
}

function refreshComponent() {
  refreshKey.value += 1;
}

function resetFilter() {
  filter.value = {
    germplasm_group: undefined,
    start_date: null,
    end_date: null,
    variables: [],
    provenance: null,
    experiments: [],
    scientificObjects: [],
    targets: [],
    devices: [],
    facilities: [],
    operators: [],
    germplasm: [],
    batch_uri: undefined
  };

  soSelector.value.refreshModalSearch();
}

function updateSOFilter() {
  refreshComponent();
  soFilter.value.experiment = filter.value.experiments[0];
  soSelector.value.refreshModalSearch();
}

function refresh() {
  dataList.value.refresh();
}

function reset() {
  resetFilter();
  refresh();
  resetExperimentSelectorKey.value += 1;
}

function showProvenanceDetails() {
  if (selectedProvenance.value != null) {
    visibleDetails.value = !visibleDetails.value;
  }
}

function getProvenance(uri: string) {
  if (uri != undefined) {
    dataService
        .getProvenance(uri)
        .then((http: HttpResponse<OpenSilexResponse<ProvenanceGetDTO>>) => {
          selectedProvenance.value = http.response.result;
        });
  }
}

function loadProvenance(selectedValue) {
  if (selectedValue != undefined) {
    getProvenance(selectedValue.id);
  }
}

function afterCreateData(results) {
  if (results instanceof Promise) {
    results.then((res) => {
      resultModal.value.setNbLinesImported(
          res.validation.dataErrors.nbLinesImported
      );
      let annotationsOnObjects: Array<any> = res.validation.dataErrors.annotationsOnObjects;
      if (annotationsOnObjects) {
        resultModal.value.setNbAnnotationsImported(
            annotationsOnObjects.length
        );
      }
      resultModal.value.setProvenance(res.form.provenance);
      resultModal.value.setBatch(res.validation.dataErrors.batchHistoryUri);
      resultModal.value.show();
      refreshKey.value += 1;
      clear();
      filter.value.provenance = res.form.provenance.uri;
    });
  } else {
    resultModal.value.setNbLinesImported(
        results.validation.dataErrors.nbLinesImported
    );
    let annotationsOnObjects: Array<any> = results.validation.dataErrors.annotationsOnObjects;
    if (annotationsOnObjects) {
      resultModal.value.setNbAnnotationsImported(
          annotationsOnObjects.length
      );
    }
    resultModal.value.setBatch(results.validation.dataErrors.batchHistoryUri);
    resultModal.value.setProvenance(results.form.provenance);
    resultModal.value.show();
    refreshKey.value += 1;
    clear();
    filter.value.provenance = results.form.provenance.uri;
  }
}

function clear() {
  selectedProvenance.value = null;
  resetFilter();
  filterProvenanceLabel.value = null;
}

function successMessage(form) {
  return t("component.data.view.data-imported");
}

function refreshDataAfterImportation() {
  refresh();
}

/**
 * Trigger render of advanced search filters selector (v-if)
 * This ensures that API methods corresponding with the selector are not executed
 * at the render of this component but only at the first opening of the advanced filter
 */
function loadAdvancedFilter(names: string[]) {
  if (names.includes('adv')) {
    loadAdvancedSearchFilters.value = true;
  }
}

function showImportForm() {
  renderImportForm.value = true;
  nextTick(() => {
    modalDataForm.value.show();
  });
}

//#endregion
</script>

<style scoped lang="scss">

.createButton {
  margin-bottom: 10px;
  margin-top: -15px
}

.data-layout {
  background: transparent;
}

.data-content {
  padding-left: 12px;
}

.advancedFiltersSearch {
  margin-top: 10px;
}
</style>
