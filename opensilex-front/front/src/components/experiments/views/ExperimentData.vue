<template>
  <div>
    <PageActions>
      <!-- Create Button -->
      <CreateButton
          v-if="user.hasCredential(credentials.CREDENTIAL_DATA_MODIFICATION_ID)"
          @click="showImportForm()"
          label="OntologyCsvImporter.import"
          class="greenThemeColor createButton"
      ></CreateButton>
      <!-- Export button-->
      <b-button
          @click="exportModal.show()"
          class="exportButton greenThemeColor createButton"
      >
        export
      </b-button>
      <!-- Delete by batch button -->
      <Button
          @click="deleteByBatchModal.show()"
          class="createButton greenThemeColor"
          icon="fa#trash-alt"
          :small="false"
          label="DataView.buttons.delete-by-batch"
          :disabled="false"
      ></Button>
    </PageActions>

    <DataExportModal
        ref="exportModal"
        :filter="filter"
    ></DataExportModal>

    <DeleteByBatchModal
        ref="deleteByBatchModal"
        :experimentUri="this.uri"
        @deleted="refresh"
    ></DeleteByBatchModal>

    <template>
      <PageContent class="pagecontent">
        <!-- Toggle Sidebar-->
        <div class="searchMenuContainer"
             v-on:click="toggleFilter()"
             :title="searchFiltersPannel()">
          <div class="searchMenuIcon">
            <i class="icon ik ik-search"></i>
          </div>
        </div>

        <!-- FILTERS -->
        <Transition>
          <div v-show="toggleSearchFilters">

            <SearchFilterField
                v-if="loadSearchFilters"
                ref="searchField"
                :withButton="true"
                label="DataView.filter.label"
                @search="refresh()"
                @clear="clear()"
                :showAdvancedSearch="true"
                class="searchFilterField"
            >
              <template v-slot:filters>
                <!-- Germplasm Group -->
                <div>
                  <FilterField>
                    <GermplasmGroupSelector
                        label="GermplasmList.filter.germplasm-group"
                        :multiple="false"
                        :germplasmGroup.sync="filter.germplasm_group"
                        class="searchFilter"
                        @handlingEnterKey="refresh()"
                    ></GermplasmGroupSelector>
                  </FilterField>
                </div>

                <!-- targets -->
                <div>
                  <FilterField halfWidth="true">
                    <TagInputForm
                        class="overflow-auto searchFilter"
                        :value.sync="filter.targets"
                        label="DataView.filter.targets"
                        helpMessage="DataView.filter.targets-help"
                        type="text"
                    ></TagInputForm>
                  </FilterField>
                </div>

                <!-- Scientific objects -->
                <div>
                  <FilterField halfWidth="true">
                    <ModalFormSelector
                        ref="soSelector"
                        label="DataView.filter.scientificObjects"
                        placeholder="DataView.filter.scientificObjects-placeholder"
                        :selected.sync="filter.scientificObjects"
                        modalComponent="ScientificObjectModalListByExp"
                        class="searchFilter"
                        :filter.sync="soFilter"
                        :clearable="true"
                        :multiple="true"
                        @clear="refreshSoSelector"
                        @onClose="refreshComponent"
                        @onValidate="refreshComponent"
                        :limit="1"
                    ></ModalFormSelector>
                  </FilterField>
                </div>

                <!-- Variables -->
                <div>
                  <FilterField halfWidth="true">
                    <VariableSelectorWithFilter
                        placeholder="VariableSelector.placeholder-multiple"
                        :variables.sync="filter.variables"
                        :experiment="[uri]"
                        :withAssociatedData="true"
                        class="searchFilter"
                    ></VariableSelectorWithFilter>
                  </FilterField>
                </div>

                <!-- Provenance -->
                <div>
                  <FilterField halfWidth="true">
                    <DataProvenanceSelector
                        ref="provSelector"
                        :provenances.sync="filter.provenance"
                        label="ExperimentData.provenance"
                        @select="loadProvenance"
                        :experiments="[uri]"
                        :targets="filter.scientificObjects"
                        :multiple="false"
                        :viewHandler="showProvenanceDetails"
                        :viewHandlerDetailsVisible="visibleDetails"
                        :key="refreshKey"
                        class="searchFilter"
                        @handlingEnterKey="refresh()"
                    ></DataProvenanceSelector>

                    <b-collapse
                        v-if="selectedProvenance"
                        id="collapse-4"
                        v-model="visibleDetails"
                        class="mt-2"
                    >
                      <ProvenanceDetails
                          :provenance="getSelectedProv"
                      ></ProvenanceDetails>
                    </b-collapse>
                  </FilterField>
                </div>
              </template>

              <template v-slot:advancedSearch>
                <!-- Start Date -->
                <div>
                  <FilterField>
                    <DateTimeForm
                        :value.sync="filter.start_date"
                        label="component.common.begin"
                        name="startDate"
                        :max-date="filter.end_date ? filter.end_date : undefined"
                        class="searchFilter"
                    ></DateTimeForm>
                  </FilterField>
                </div>

                <!-- End Date -->
                <div>
                  <FilterField>
                    <DateTimeForm
                        :value.sync="filter.end_date"
                        label="component.common.end"
                        name="endDate"
                        :min-date="filter.start_date ? filter.start_date : undefined"
                        class="searchFilter"
                    ></DateTimeForm>
                  </FilterField>
                </div>

                <!-- Batch URI -->
                <div>
                  <FilterField>
                    <label>{{ $t('ExperimentData.batch-uri') }}</label>
                    <StringFilter
                        :filter.sync="filter.batch_uri"
                        placeholder="ExperimentData.uri-placeholder"
                        class="searchFilter"
                        @handlingEnterKey="refresh()"
                    ></StringFilter>
                  </FilterField>
                  <br>
                </div>
              </template>
            </SearchFilterField>
          </div>
        </Transition>
        <div class="card">
          <div class="card-body">
            <DataList
                ref="dataList"
                :listFilter.sync="filter"
                :contextUri="uri"
                class="dataList">
            </DataList>
          </div>
        </div>
      </PageContent>
    </template>

    <Modal v-if="renderImportForm"
           ref="modalDataForm"
           :initForm="initFormData"
           createTitle="DataImportForm.create"
           editTitle="DataImportForm.update"
           component="DataImportForm"
           icon="ik#ik-bar-chart-line"
           modalSize="xl"
           @onCreate="afterCreateData"
           :successMessage="successMessage"
    ></Modal>

    <ResultModalView
        ref="resultModal"
        @onHide="refreshDataAfterImportation()"
    >
    </ResultModalView>
  </div>
</template>

<script setup lang="ts">
import {ProvenanceGetDTO, ScientificObjectNodeDTO} from "opensilex-core/index";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import DeleteByBatchModal from "../../data/DeleteByBatchModal.vue";
import PageActions from "@/components/layout/PageActions.vue";
import CreateButton from "@/components/common/buttons/CreateButton.vue";
import Button from "@/components/common/buttons/Button.vue";
import PageContent from "@/components/layout/PageContent.vue";
import TagInputForm from "@/components/common/forms/TagInputForm.vue";
import ModalFormSelector from "@/components/variables/form/ModalFormSelector.vue";
import VariableSelectorWithFilter from "@/components/variables/views/VariableSelectorWithFilter.vue";
import ProvenanceDetails from "@/components/data/ProvenanceDetails.vue";
import DateTimeForm from "@/components/common/forms/DateTimeForm.vue";
import StringFilter from "@/components/common/filters/StringFilter.vue";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import {useI18n} from "vue-i18n";
import {computed, inject, nextTick, onMounted, ref, useTemplateRef} from "vue";
import {useRoute} from "vue-router";
import Modal from "@/components/common/views/Modal.vue";
import ResultModalView from "@/components/data/ResultModalView.vue";
import DataExportModal from "@/components/data/DataExportModal.vue";
import GermplasmGroupSelector from "@/components/germplasm/GermplasmGroupSelector.vue";
import FilterField from "@/components/common/filters/FilterField.vue";
import DataList from "@/components/data/DataList.vue";
import SearchFilterField from "@/components/common/filters/SearchFilterField.vue";
import {type} from "node:os";
import {useStore} from "vuex";
import {DataService, ScientificObjectsService} from "../../../../../../opensilex-core/front/src/lib";

const opensilex = inject<OpenSilexVuePlugin>('$opensilex')
const {t} = useI18n()
const route = useRoute()
const store = useStore()

const uri = ref(null)
const visibleDetails = ref<boolean>(false)
const searchVisible = ref<boolean>(false)
const usedVariables = ref<any[]>([])
const selectedProvenance = ref<any>(null)
const refreshKey = ref<number>(0)
const toggleSearchFilters = ref<boolean>(false)
const loadSearchFilters = ref<boolean>(false)
const renderImportForm = ref<boolean>(false)

const filter = {
  germplasm_group: undefined,
  start_date: null,
  end_date: null,
  provenance: null,
  variables: [],
  experiments: [uri.value],
  scientificObjects: [],
  targets: [],
  devices: [],
  facilities: [],
  operators: [],
  batch_uri: undefined
};

const soFilter = {
  name: "",
  experiment: uri.value,
  germplasm: undefined,
  factorLevels: [],
  types: [],
  existenceDate: undefined,
  creationDate: undefined,
};

function data() {
  return {
    SearchFiltersToggle: false,
  }
}

const dataList = useTemplateRef<InstanceType<typeof DataList>>('dataList')
const modalDataForm = useTemplateRef<InstanceType<typeof Modal>>('modalDataForm')
const searchField = useTemplateRef<InstanceType<typeof SearchFilterField>>('searchField')
const provSelector = useTemplateRef<InstanceType<typeof FilterField>>('provSelector')
const resultModal = useTemplateRef<InstanceType<typeof ResultModalView>>('resultModal')
const soSelector = useTemplateRef<InstanceType<typeof ModalFormSelector>>('soSelector')
const exportModal = useTemplateRef<InstanceType<typeof DataExportModal>>('exportModal')
const deleteByBatchModal = useTemplateRef<InstanceType<typeof DeleteByBatchModal>>('deleteByBatchModal')

const user = computed(() => {
  return store.state.user
})

const credentials = computed(() => {
  return store.state.credentials
})


function refreshSoSelector() {
  const soFilter = ref({
    name: "",
    experiment: this.uri,
    germplasm: undefined,
    factorLevels: [],
    types: [],
    existenceDate: undefined,
    creationDate: undefined,
  });
  soSelector.value.refreshModalSearch();
  refreshComponent();
}

function refreshComponent() {
  refreshKey.value += 1
}

function created() {
  uri.value = decodeURIComponent(route.params.uri as string);
  resetFilters();


  const soFilter = ref({
    name: "",
    experiment: uri.value,
    germplasm: undefined,
    factorLevels: [],
    types: [],
    existenceDate: undefined,
    creationDate: undefined,
  });
}

function resetFilters() {
  const filter = {
    germplasm_group: undefined,
    start_date: null,
    end_date: null,
    provenance: null,
    variables: [],
    experiments: [this.uri],
    scientificObjects: [],
    targets: [],
    devices: [],
    facilities: [],
    operators: [],
    batch_uri: undefined
  };
  // Only if search and reset button are use in list
}

/**
 * Show or hide the search filter (v-show) on the filter div
 * Trigger render of search filters selector (v-if).
 * This ensures that API methods corresponding with the selector are not executed
 * at the render of this component but only at the first toggle of the filter
 *
 */
function toggleFilter() {
  toggleSearchFilters.value = !this.toggleSearchFilters;
  if (!loadSearchFilters.value) {
    loadSearchFilters.value = true;
  }
}

function showImportForm() {
  renderImportForm.value = true;
  nextTick(() => {
    modalDataForm.value.showCreateForm();
  });
}

function successMessage(form) {
  return t("ResultModalView.data-imported");
}

const getSelectedProv = computed(() => {
      return selectedProvenance;
    }
)

function refreshDataAfterImportation()
{
  loadProvenance({id: filter.provenance});
  refresh();
}

function afterCreateData(results)
{
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
      clear();
      filter.provenance = res.form.provenance.uri;
      refreshVariables();
      refreshKey.value += 1;
      loadProvenance({id: res.form.provenance.uri});
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
    resultModal.value.setProvenance(results.form.provenance);
    resultModal.value.setBatch(results.validation.dataErrors.batchHistoryUri);
    resultModal.value.show();
    clear();
    filter.provenance = results.form.provenance.uri;
    refreshVariables();
    refreshKey.value += 1;
    loadProvenance({id: results.form.provenance.uri});
  }
}

function initFormData(form)
{
  form.experiment = uri.value;
  return form;
}

function showProvenanceDetails()
{
  if (selectedProvenance != null) {
    visibleDetails.value = !visibleDetails.value;
  }
}

function clear()
{
  searchVisible.value = false;
  selectedProvenance.value = null;
  resetFilters();
  refresh();
}

onMounted(() =>
{
  searchVisible.value = false;
  refreshVariables();
})

function refreshVariables()
{
  opensilex
      .getService<DataService>("opensilex.DataService")
      .getUsedVariables([this.uri], null, null, null)
      .then((http) => {
        let variables = http.response.result;
        this.usedVariables = [];
        for (let i in variables) {
          let variable = variables[i];
          this.usedVariables.push({
            id: variable.uri,
            label: variable.name,
          });
        }
      });
}

function getProvenance(uri)
{
  if (uri != undefined && uri != null) {
    return opensilex
        .getService<DataService>("opensilex.DataService")
        .getProvenance(uri)
        .then((http: HttpResponse<OpenSilexResponse<ProvenanceGetDTO>>) => {
          return http.response.result;
        });
  }
}

function loadProvenance(selectedValue)
{
  if (selectedValue != undefined && selectedValue != null) {
    getProvenance(selectedValue.id).then((prov) => {
      selectedProvenance.value = prov;
    });
  }
}

function refresh()
{
  searchVisible.value = true;
  dataList.value.refresh();
  //remove experiments filter from URL
  nextTick(() => {
    opensilex.updateURLParameter("experiments", null, "");
  });
}

function loadSO(scientificObjectsURIs)
{
  const sos = scientificObjectsURIs.filter((x, i, a) => a.indexOf(x) == i); // distinct element on array
  return opensilex
      .getService<ScientificObjectsService>("opensilex.ScientificObjectsService")
      .searchScientificObjectsListByUris(this.uri, sos)
      .then(
          (
              http: HttpResponse<OpenSilexResponse<Array<ScientificObjectNodeDTO>>>
          ) => {
            return http && http.response ? http.response.result : undefined;
          }
      )
      .catch(opensilex.errorHandler);
}

function soGetDTOToSelectNode(dto)
{
  if (dto) {
    return {
      id: dto.uri,
      label: dto.name,
    };
  }
  return null;
}

function searchFiltersPannel()
{
  return t("searchfilter.label")
}

</script>

<style scoped lang="scss">

.pagecontent {
  margin-top: 10px
}

.createButton {
  margin-top: 10px;
}

.card-body {
  margin-bottom: -15px;
}
</style>

<i18n>
en:
  ExperimentData:
    object: Scientific Object
    date: Date
    value: Value
    variable: Variable
    provenance: Provenance
    export: Export
    export-wide: Wide format
    export-wide-help: A given date, provenance, scientific object of an observation represents a row and each variable value is in a specific column.
    export-long: Long format
    export-long-help: Each line represent an observation (Same as the result table)
    batch-uri: Batch URI
    uri-placeholder: Enter a part of an uri
fr:
  ExperimentData:
    object: Objet Scientifique
    date: Date
    value: Valeur
    variable: Variable
    provenance: Provenance
    export: Exporter
    export-wide: Format large
    export-wide-help: Une date, une provenance, un objet scientifique donné d'une observation représente une ligne et chaque valeur de variable est dans une colonne spécifique.
    export-long: Format long
    export-long-help: Une ligne représente une observation (identique au tableau de résultat)
    batch-uri: URI de Batch
    uri-placeholder: Entrer une partie d'une uri

</i18n>
