<template>
  <div>
    <PageActions>
      <!-- Create Button -->
      <CreateButton
          v-if="user.hasCredential(credentials.CREDENTIAL_DATA_MODIFICATION_ID)"
          @click="showImportForm()"
          :label="t('component.common.import-files.csv-import')"
          class="greenThemeColor createButton"
      ></CreateButton>
      <!-- Export button-->
      <Button
          @click="exportModal.show()"
          class="exportButton greenThemeColor createButton"
          :small="false"
          :label="t('component.data.export')"
      ></Button>
      <!-- Delete by batch button -->
      <Button
          @click="deleteByBatchModal.show()"
          class="createButton greenThemeColor"
          icon="fa#trash-alt"
          :small="false"
          :label="t('component.data.deleteByBatch')"
          :disabled="false"
      ></Button>
    </PageActions>

    <DataExportModal
        ref="exportModal"
        :filter="filter"
    ></DataExportModal>

    <DeleteByBatchModal
        ref="deleteByBatchModal"
        :experimentUri="uri"
        @deleted="refresh"
    ></DeleteByBatchModal>

    <PageContent class="pagecontent">
      <n-layout has-sider class="data-layout">
        <SearchFiltersSidebar
            :activeFiltersCount="activeFiltersCount"
            v-model:filtersCollapsed="searchFiltersToggle"
            @refresh="refresh()"
            @reset="clear()"
        >
          <!-- Germplasm Group -->
          <n-form>
            <GermplasmGroupSelector
                :label="t('experimentData.germplasm-group')"
                :placeholder="t('experimentData.germplasm-group-placeholder')"
                :multiple="false"
                v-model:selected="filter.germplasm_group"
                class="searchFilter"
                @handlingEnterKey="refresh()"
            ></GermplasmGroupSelector>


          <!-- Targets -->
          <n-form-item  class="compact-form-item">
            <TagInputForm
                class="overflow-auto searchFilter"
                v-model:value="filter.targets"
                :label="t('experimentData.targets')"
                :helpMessage="t('experimentData.targets-help')"
                type="text"
            ></TagInputForm>
          </n-form-item>

          <!-- Scientific objects -->
          <n-form-item  class="compact-form-item">
            <ModalFormSelector
                ref="soSelector"
                :label="t('experimentData.scientific-objects')"
                placeholder="experimentData.scientific-objects-placeholder"
                v-model:selected="filter.scientificObjects"
                modalComponent="opensilex-ScientificObjectModalList"
                class="searchFilter"
                v-model:filter="soFilter"
                :clearable="true"
                :multiple="true"
                @clear="refreshSoSelector"
                @onClose="refreshComponent"
                @onValidate="refreshComponent"
                :limit="1"
            ></ModalFormSelector>
          </n-form-item>

          <!-- Variables -->
          <n-form-item  class="compact-form-item">
            <VariableSelectorWithFilter
                :label="t('experimentData.variables')"
                placeholder="component.variable.placeholder-multiple"
                v-model:variables="filter.variables"
                :experiment="[uri]"
                :withAssociatedData="true"
                class="searchFilter"
            ></VariableSelectorWithFilter>
          </n-form-item>

          <!-- Provenance -->
            <div class="w-100">
              <DataProvenanceSelector
                  ref="provSelector"
                  v-model:provenances="filter.provenance"
                  :label="t('experimentData.provenance')"
                  :placeholder="t('experimentData.provenance-placeholder')"
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

              <n-collapse-transition v-if="selectedProvenance" :show="visibleDetails" class="mt-2">
                <ProvenanceDetails
                    :provenance="getSelectedProv"
                ></ProvenanceDetails>
              </n-collapse-transition>
            </div>

          <!-- Advanced search -->
          <n-collapse :accordion="false" class="advancedFiltersSearch">
            <n-collapse-item :title="t('component.common.advanced-search-title')" name="adv">
              <!-- Start Date -->
              <n-form-item >
                <DateTimeForm
                    v-model:value="filter.start_date"
                    :label="t('experimentData.begin')"
                    name="startDate"
                    :max-date="filter.end_date ? filter.end_date : undefined"
                    class="searchFilter"
                ></DateTimeForm>
              </n-form-item>

              <!-- End Date -->
              <n-form-item  class="compact-form-item">
                <DateTimeForm
                    v-model:value="filter.end_date"
                    :label="t('experimentData.end')"
                    name="endDate"
                    :min-date="filter.start_date ? filter.start_date : undefined"
                    class="searchFilter"
                ></DateTimeForm>
              </n-form-item>

              <!-- Batch URI -->
              <n-form-item
                  :label="t('experimentData.batch-uri')"

                  class="compact-form-item"
              >
                <StringFilter
                    v-model:filter="filter.batch_uri"
                    :placeholder="t('experimentData.uri-placeholder')"
                    class="searchFilter"
                    @handlingEnterKey="refresh()"
                ></StringFilter>
              </n-form-item>
            </n-collapse-item>
          </n-collapse>
          </n-form>
        </SearchFiltersSidebar>

        <n-layout-content class="data-content">
          <div class="card">
            <div class="card-body">
              <DataList
                  ref="dataList"
                  v-model:listFilter="filter"
                  :contextUri="uri"
                  class="dataList">
              </DataList>
            </div>
          </div>
        </n-layout-content>
      </n-layout>
    </PageContent>

    <DataImportForm v-if="renderImportForm"
           ref="modalDataForm"
           :experiment="uri"
           @onCreate="afterCreateData"
    ></DataImportForm>

    <ResultModalView
        ref="resultModal"
        @onHide="refreshDataAfterImportation()"
    >
    </ResultModalView>
  </div>
</template>

<script setup lang="ts">
import {ProvenanceGetDTO} from "opensilex-core/index";
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
import DataImportForm from "@/components/data/form/DataImportForm.vue";
import ResultModalView from "@/components/data/ResultModalView.vue";
import DataExportModal from "@/components/data/DataExportModal.vue";
import GermplasmGroupSelector from "@/components/germplasm/GermplasmGroupSelector.vue";
import DataList from "@/components/data/DataList.vue";
import {useStore} from "vuex";
import {NCollapse, NCollapseItem, NCollapseTransition, NFormItem, NLayout, NLayoutContent, NForm} from "naive-ui";
import SearchFiltersSidebar from "@/components/common/filters/SearchFiltersSidebar.vue";
import DataProvenanceSelector from "@/components/data/DataProvenanceSelector.vue";
import {DataService} from "../../../../../../opensilex-core/front/src/lib";

const opensilex = inject<OpenSilexVuePlugin>('$opensilex')
const {t} = useI18n()
const route = useRoute()
const store = useStore()

const uri = ref<string>(decodeURIComponent(route.params.uri as string))
const visibleDetails = ref<boolean>(false)
const searchVisible = ref<boolean>(false)
const usedVariables = ref<any[]>([])
const selectedProvenance = ref<any>(null)
const refreshKey = ref<number>(0)
const searchFiltersToggle = ref<boolean>(true)
const renderImportForm = ref<boolean>(false)

function defaultFilter() {
  return {
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
}

function defaultSoFilter() {
  return {
    name: "",
    experiment: uri.value,
    germplasm: undefined,
    factorLevels: [],
    types: [],
    existenceDate: undefined,
    creationDate: undefined,
  };
}

const filter = ref<any>(defaultFilter())
const soFilter = ref<any>(defaultSoFilter())

const dataList = useTemplateRef<InstanceType<typeof DataList>>('dataList')
const modalDataForm = useTemplateRef<InstanceType<typeof DataImportForm>>('modalDataForm')
const provSelector = useTemplateRef<InstanceType<typeof DataProvenanceSelector>>('provSelector')
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

const activeFiltersCount = computed(() => {
  const f = filter.value
  const activeFilters = [
    f.germplasm_group,
    f.targets,
    f.scientificObjects,
    f.variables,
    f.provenance,
    f.start_date,
    f.end_date,
    f.batch_uri
  ]

  return activeFilters.filter(v => {
    if (Array.isArray(v)) return v.length > 0
    return v !== undefined && v !== null && String(v).trim() !== ''
  }).length
})


function refreshSoSelector() {
  soFilter.value = defaultSoFilter();
  soSelector.value.refreshModalSearch();
  refreshComponent();
}

function refreshComponent() {
  refreshKey.value += 1
}

function resetFilters() {
  filter.value = defaultFilter();
  // Only if search and reset button are use in list
}

function showImportForm() {
  renderImportForm.value = true;
  nextTick(() => {
    modalDataForm.value.show();
  });
}

const getSelectedProv = computed(() => {
  return selectedProvenance.value;
})

function refreshDataAfterImportation()
{
  loadProvenance({id: filter.value.provenance});
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
      filter.value.provenance = res.form.provenance.uri;
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
    filter.value.provenance = results.form.provenance.uri;
    refreshVariables();
    refreshKey.value += 1;
    loadProvenance({id: results.form.provenance.uri});
  }
}

function showProvenanceDetails()
{
  if (selectedProvenance.value != null) {
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
      .getUsedVariables([uri.value], null, null, null)
      .then((http) => {
        let variables = http.response.result;
        usedVariables.value = [];
        for (let i in variables) {
          let variable = variables[i];
          usedVariables.value.push({
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


</script>

<style scoped lang="scss">

.pagecontent {
  margin-top: 10px;
  width: 100%;
}

.data-layout {
  height: 100%;
  background: transparent;
}

.data-content {
  padding-left: 12px;
}

.advancedFiltersSearch {
  margin-top: 10px;
}

.createButton {
  margin-top: 10px;
}

.card-body {
  margin-bottom: -15px;
}
</style>
