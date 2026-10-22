<template>
  <PageContent class="pagecontent">
    <n-layout has-sider class="visualisation-layout">
      <SearchFiltersSidebar
          :activeFiltersCount="activeFiltersCount"
          v-model:filtersCollapsed="searchFiltersToggle"
          @refresh="onSearch()"
          @reset="resetFilters()"
      >
        <n-form>
          <!-- Scientific objects -->
          <n-form-item class="compact-form-item">
            <ModalFormSelector
                :label="t('experimentData.scientific-objects')"
                placeholder="experimentData.scientific-objects-placeholder"
                v-model:selected="filter.scientificObjects"
                v-model:selectedInJsonFormat="selectedScientificObjectsWithLabels"
                v-model:filter="localSoFilter"
                modalComponent="opensilex-ScientificObjectModalList"
                :multiple="true"
                :clearable="true"
                :required="true"
                :maximumSelectedItems="SCIENTIFIC_OBJECTS_LIMIT"
                class="searchFilter"
            ></ModalFormSelector>
          </n-form-item>

          <!-- Variables -->
          <n-form-item class="compact-form-item">
            <VariableSelectorWithFilter
                :label="t('experimentData.variables')"
                placeholder="component.variable.placeholder-multiple"
                v-model:variables="filter.variables"
                :experiment="[uri]"
                :objects="filter.scientificObjects"
                :withAssociatedData="true"
                :maximumSelectedRows="VARIABLES_LIMIT"
                :required="true"
                class="searchFilter"
            ></VariableSelectorWithFilter>
          </n-form-item>

          <!-- Start Date -->
          <n-form-item class="compact-form-item">
            <DateTimeForm
                v-model:value="filter.start_date"
                :label="t('experimentData.begin')"
                name="startDate"
                :max-date="filter.end_date ? filter.end_date : undefined"
                class="searchFilter"
            ></DateTimeForm>
          </n-form-item>

          <!-- End Date -->
          <n-form-item class="compact-form-item">
            <DateTimeForm
                v-model:value="filter.end_date"
                :label="t('experimentData.end')"
                name="endDate"
                :min-date="filter.start_date ? filter.start_date : undefined"
                class="searchFilter"
            ></DateTimeForm>
          </n-form-item>

          <!-- Advanced search -->
          <n-collapse :accordion="false" class="advancedFiltersSearch">
            <n-collapse-item :title="t('component.common.advanced-search-title')" name="adv">
              <!-- Provenance -->
              <DataProvenanceSelector
                  v-model:provenances="filter.provenance"
                  :label="t('experimentData.provenance')"
                  :placeholder="t('experimentData.provenance-placeholder')"
                  :experiments="[uri]"
                  :targets="filter.scientificObjects"
                  :multiple="false"
                  class="searchFilter"
              ></DataProvenanceSelector>
            </n-collapse-item>
          </n-collapse>
        </n-form>
      </SearchFiltersSidebar>

      <n-layout-content class="visualisation-content">
        <div class="card">
          <div class="card-body">
            <n-spin :show="loading">
              <DataVisuGraphic
                  :series="series"
                  :yAxisNames="yAxisNames"
                  :elementName="elementName"
                  :elementUri="uri"
              ></DataVisuGraphic>
            </n-spin>
          </div>
        </div>
      </n-layout-content>
    </n-layout>
  </PageContent>
</template>

<script setup lang="ts">
import {computed, ComputedRef, inject, onMounted, ref, Ref} from "vue";
import {useI18n} from "vue-i18n";
import {useRoute} from "vue-router";
import {useStore} from "vuex";
import {NCollapse, NCollapseItem, NForm, NFormItem, NLayout, NLayoutContent, NSpin} from "naive-ui";
import {DataGetSearchDTO, DataService, ExperimentsService, VariableDetailsDTO, VariablesService} from "opensilex-core/index";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import PageContent from "@/components/layout/PageContent.vue";
import SearchFiltersSidebar from "@/components/common/filters/SearchFiltersSidebar.vue";
import ModalFormSelector from "@/components/variables/form/ModalFormSelector.vue";
import VariableSelectorWithFilter from "@/components/variables/views/VariableSelectorWithFilter.vue";
import DateTimeForm from "@/components/common/forms/DateTimeForm.vue";
import DataProvenanceSelector from "@/components/data/DataProvenanceSelector.vue";
import DataVisuGraphic, {DataVisuSerie} from "@/components/visualization/DataVisuGraphic.vue";
import {ScientificObjectFilter} from "@/components/scientificObjects/ScientificObjectList.vue";

interface VisualisationFilter {
  scientificObjects: string[]
  variables: string[]
  start_date: string | undefined
  end_date: string | undefined
  provenance: string | undefined
}

interface NamedScientificObject {
  uri: string
  name: string
}

//#region Public
const props = defineProps<{
  /** Initial filter of the scientific objects selection modal */
  soFilter?: ScientificObjectFilter
  elementName?: string
}>();
//#endregion

//#region Private
//#region Plugins & services
const opensilex: OpenSilexVuePlugin = inject<OpenSilexVuePlugin>("$opensilex");
const {t} = useI18n();
const route = useRoute();
const store = useStore();

const dataService: DataService = opensilex.getService<DataService>("opensilex.DataService");
const variablesService: VariablesService = opensilex.getService<VariablesService>("opensilex.VariablesService");
const experimentsService: ExperimentsService = opensilex.getService<ExperimentsService>("opensilex.ExperimentsService");
//#endregion

//#region Data & Computed
const SCIENTIFIC_OBJECTS_LIMIT: number = 15;
const VARIABLES_LIMIT: number = 2;
const NUMERIC_DATATYPES: string[] = ["decimal", "integer"];
/** One color per scientific object */
const COLOR_PALETTE: string[] = [
  "#ca6434", "#427775", "#f2dc7c", "#0f839c", "#a45354",
  "#d3b0ae", "#774e42", "#776942", "#5c4277", "#34a0ca",
  "#9334ca", "#caaf34", "#4F72D6", "#DB78E1", "#97CA68",
];
/** One symbol per variable */
const VARIABLE_SYMBOLS: string[] = ["circle", "rect"];

const uri: string = decodeURIComponent(route.params.uri as string);

const filter: Ref<VisualisationFilter> = ref<VisualisationFilter>(defaultFilter());
/** Labels of the selected scientific objects, used as series names */
const selectedScientificObjectsWithLabels: Ref<NamedScientificObject[] | null> = ref<NamedScientificObject[] | null>(null);
const localSoFilter: Ref<ScientificObjectFilter> = ref<ScientificObjectFilter>({...props.soFilter});
/** true when the filters sidebar is collapsed */
const searchFiltersToggle: Ref<boolean> = ref<boolean>(false);
const loading: Ref<boolean> = ref<boolean>(false);
const series: Ref<DataVisuSerie[]> = ref<DataVisuSerie[]>([]);
const yAxisNames: Ref<string[]> = ref<string[]>([]);

const activeFiltersCount: ComputedRef<number> = computed<number>(() => {
  const f: VisualisationFilter = filter.value;
  return [f.scientificObjects, f.variables, f.start_date, f.end_date, f.provenance]
      .filter((v: string[] | string | undefined) => Array.isArray(v) ? v.length > 0 : !!v)
      .length;
});
//#endregion

//#region Hooks and watcher
onMounted(() => {
  initDefaultDates();
});
//#endregion

//#region Event handlers
async function onSearch(): Promise<void> {
  const {scientificObjects, variables} = filter.value;
  if (scientificObjects.length === 0 || variables.length === 0) {
    opensilex.showWarningToast(t("ExperimentDataVisualisationView.missingFilters"));
    return;
  }

  loading.value = true;
  try {
    const variableDetails: VariableDetailsDTO[] = (await variablesService.getVariablesByURIs(variables)).response.result;

    const nonNumericVariable: VariableDetailsDTO | undefined = variableDetails.find(
        (v: VariableDetailsDTO) => !NUMERIC_DATATYPES.includes(getDatatypeName(v))
    );
    if (nonNumericVariable) {
      opensilex.showInfoToast(t("ExperimentDataVisualisationView.datatypeMessage", {
        variable: nonNumericVariable.name,
        datatype: getDatatypeName(nonNumericVariable)
      }));
      return;
    }

    const loadedSeries: DataVisuSerie[] = await loadSeries(scientificObjects, variableDetails);

    if (loadedSeries.every((s: DataVisuSerie) => s.data.length === 0)) {
      opensilex.showInfoToast(t("component.common.search.noDataFound"));
    }

    yAxisNames.value = variableDetails.map((v: VariableDetailsDTO) => v.unit?.name ? `${v.name} (${v.unit.name})` : v.name);
    series.value = loadedSeries;
    searchFiltersToggle.value = true;
  } catch (error) {
    opensilex.errorHandler(error);
  } finally {
    loading.value = false;
  }
}
//#endregion

//#region Methods
function defaultFilter(): VisualisationFilter {
  return {
    scientificObjects: [],
    variables: [],
    start_date: undefined,
    end_date: undefined,
    provenance: undefined,
  };
}

/**
 * If the experiment is over, display its whole period. Otherwise only display the last 15 days.
 */
function initDefaultDates(): void {
  experimentsService.getExperiment(uri)
      .then(http => {
        const experiment = http.response.result;
        if (experiment.end_date && new Date(experiment.end_date).getTime() < Date.now()) {
          filter.value.start_date = experiment.start_date;
          filter.value.end_date = experiment.end_date;
        } else {
          const lastFifteenDays: Date = new Date();
          lastFifteenDays.setDate(lastFifteenDays.getDate() - 15);
          lastFifteenDays.setHours(0, 0, 0, 0);
          filter.value.start_date = lastFifteenDays.toISOString();
        }
      })
      .catch(error => opensilex.errorHandler(error));
}

function resetFilters(): void {
  filter.value = defaultFilter();
  selectedScientificObjectsWithLabels.value = null;
}

/**
 * @returns the local name of the variable datatype, e.g. "decimal" for "xsd:decimal"
 */
function getDatatypeName(variable: VariableDetailsDTO): string {
  return variable.datatype?.split("#")[1] ?? "";
}

/**
 * Loads one series per (scientific object, variable) pair, with one call each.
 * Each scientific object gets its own color, and each variable its own symbol and Y axis.
 */
function loadSeries(scientificObjects: string[], variables: VariableDetailsDTO[]): Promise<DataVisuSerie[]> {
  const soLabels: Map<string, string> = new Map(
      (selectedScientificObjectsWithLabels.value ?? []).map((so: NamedScientificObject) => [so.uri, so.name])
  );
  const multipleVariables: boolean = variables.length > 1;

  const seriesPromises: Promise<DataVisuSerie>[] = variables.flatMap((variable: VariableDetailsDTO, variableIndex: number) =>
      scientificObjects.map(async (soUri: string, soIndex: number): Promise<DataVisuSerie> => {
        const soLabel: string = soLabels.get(soUri) ?? soUri;
        const name: string = multipleVariables ? `${soLabel} / ${variable.name}` : soLabel;
        const data: DataGetSearchDTO[] = await searchData(soUri, variable.uri, name);
        return {
          name,
          data: data.map((d: DataGetSearchDTO): [number, number] => [new Date(d.date).getTime(), Number(d.value)]),
          color: COLOR_PALETTE[soIndex % COLOR_PALETTE.length],
          symbol: VARIABLE_SYMBOLS[variableIndex],
          yAxisIndex: variableIndex
        };
      })
  );
  return Promise.all(seriesPromises);
}

/**
 * Searches the data of one scientific object for one variable, warning the user if the data limit is reached.
 */
async function searchData(soUri: string, variableUri: string, serieName: string): Promise<DataGetSearchDTO[]> {
  const limit: number = store.state.graphDataLimit;
  const f: VisualisationFilter = filter.value;
  const http = await dataService.searchDataList(
      f.start_date || undefined,
      f.end_date || undefined,
      undefined,
      [uri],
      [soUri],
      [variableUri],
      undefined,
      undefined,
      undefined,
      f.provenance ? [f.provenance] : undefined,
      undefined,
      undefined,
      ["date=asc"],
      0,
      limit
  );
  const data: DataGetSearchDTO[] = http.response.result;
  const totalCount: number | undefined = http.response.metadata?.pagination?.totalCount;
  if (data.length >= limit) {
    opensilex.showWarningToast(t("ExperimentDataVisualisationView.dataLimit", {
      total: totalCount ?? data.length,
      serie: serieName,
      limit
    }));
  }
  return data;
}
//#endregion
//#endregion
</script>

<style scoped lang="scss">
.pagecontent {
  margin-top: 10px;
  width: 100%;
}

.visualisation-layout {
  height: 100%;
  background: transparent;
}

.visualisation-content {
  padding-left: 12px;
}

.advancedFiltersSearch {
  margin-top: 10px;
}
</style>
