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
          <!-- Variable -->
          <n-form-item class="compact-form-item">
            <VariableSelectorWithFilter
                :label="t('experimentData.variable')"
                placeholder="DataVisualisation.variablePlaceholder"
                v-model:variables="filter.variables"
                :devices="[device]"
                :withAssociatedData="true"
                :maximumSelectedRows="1"
                :required="true"
                class="searchFilter"
            ></VariableSelectorWithFilter>
          </n-form-item>

          <!-- Scientific objects -->
          <n-form-item class="compact-form-item">
            <ModalFormSelector
                :label="t('experimentData.scientific-objects')"
                placeholder="experimentData.scientific-objects-placeholder"
                v-model:selected="filter.scientificObjects"
                v-model:filter="soFilter"
                modalComponent="opensilex-ScientificObjectModalList"
                :modalComponentProps="{variables: filter.variables}"
                :devices="[device]"
                :multiple="true"
                :clearable="true"
                :maximumSelectedItems="SCIENTIFIC_OBJECTS_LIMIT"
                class="searchFilter"
            ></ModalFormSelector>
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

          <!-- Events -->
          <n-form-item :label="t('DataVisualisation.showEvents')" class="compact-form-item">
            <n-switch v-model:value="filter.showEvents"></n-switch>
            <n-spin v-if="eventsCountLoading" :size="14" class="eventsCount"></n-spin>
            <n-badge v-else :value="eventsCount" :show-zero="true" type="info" class="eventsCount"></n-badge>
          </n-form-item>

          <!-- Advanced search -->
          <n-collapse :accordion="false" class="advancedFiltersSearch">
            <n-collapse-item :title="t('component.common.advanced-search-title')" name="adv">
              <!-- Provenance -->
              <DataProvenanceSelector
                  v-model:provenances="filter.provenance"
                  :label="t('experimentData.provenance')"
                  :placeholder="t('experimentData.provenance-placeholder')"
                  :devices="[device]"
                  :variables="filter.variables"
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
                  :elementUri="device"
                  :events="events"
              ></DataVisuGraphic>
            </n-spin>
          </div>
        </div>
      </n-layout-content>
    </n-layout>
  </PageContent>
</template>

<script setup lang="ts">
import {computed, ComputedRef, inject, onMounted, ref, Ref, watch} from "vue";
import {useI18n} from "vue-i18n";
import {NBadge, NCollapse, NCollapseItem, NForm, NFormItem, NLayout, NLayoutContent, NSpin, NSwitch} from "naive-ui";
import {VariableDetailsDTO} from "opensilex-core/index";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import PageContent from "@/components/layout/PageContent.vue";
import SearchFiltersSidebar from "@/components/common/filters/SearchFiltersSidebar.vue";
import VariableSelectorWithFilter from "@/components/variables/views/VariableSelectorWithFilter.vue";
import ModalFormSelector from "@/components/variables/form/ModalFormSelector.vue";
import DateTimeForm from "@/components/common/forms/DateTimeForm.vue";
import DataProvenanceSelector from "@/components/data/DataProvenanceSelector.vue";
import DataVisuGraphic, {DataVisuEvent, DataVisuSerie} from "@/components/visualization/DataVisuGraphic.vue";
import {ScientificObjectFilter} from "@/components/scientificObjects/ScientificObjectList.vue";
import useDataVisualisation, {
  colorEventsByType,
  DataSeriesRequest,
  getGraphicColor,
  getLastFifteenDaysStartDate,
  TargetSerie
} from "@/composables/useDataVisualisation";

interface VisualisationFilter {
  /** At most one variable, an array as it is the v-model of the variable selector */
  variables: string[]
  /** Optional: without scientific objects, all the data of the device for the variable is displayed */
  scientificObjects: string[]
  start_date: string | undefined
  end_date: string | undefined
  provenance: string | undefined
  /** Not a data filter: true to also draw the events of the device */
  showEvents: boolean
}

/**
 * "Visualization" tab of a device: graphic of the data of one variable measured by this device, optionally only on
 * some scientific objects, with the events of the device.
 *
 * One series and one color per scientific object, whether scientific objects are selected or not.
 */

//#region Public
const props = defineProps<{
  /** URI of the device of the details page */
  device: string
  /** Name of the device, written in the title of the downloaded image */
  elementName?: string
}>();
//#endregion

//#region Private
//#region Plugins & services
const opensilex: OpenSilexVuePlugin = inject<OpenSilexVuePlugin>("$opensilex");
const {t} = useI18n();
const {getNumericVariables, loadDataSeries, loadEvents, countEvents, splitSeriesByTarget} = useDataVisualisation();
//#endregion

//#region Data & Computed
const SCIENTIFIC_OBJECTS_LIMIT: number = 5;

const filter: Ref<VisualisationFilter> = ref<VisualisationFilter>(defaultFilter());
/** Filter of the scientific objects selection modal */
const soFilter: Ref<ScientificObjectFilter> = ref<ScientificObjectFilter>({});
/** true when the filters sidebar is collapsed */
const searchFiltersToggle: Ref<boolean> = ref<boolean>(false);
const loading: Ref<boolean> = ref<boolean>(false);
const series: Ref<DataVisuSerie[]> = ref<DataVisuSerie[]>([]);
const yAxisNames: Ref<string[]> = ref<string[]>([]);
const events: Ref<DataVisuEvent[]> = ref<DataVisuEvent[]>([]);
/** Number of events of the device between the selected dates, displayed next to the switch */
const eventsCount: Ref<number> = ref<number>(0);
const eventsCountLoading: Ref<boolean> = ref<boolean>(false);

const activeFiltersCount: ComputedRef<number> = computed<number>(() => {
  const f: VisualisationFilter = filter.value;
  return [f.variables, f.scientificObjects, f.start_date, f.end_date, f.provenance]
      .filter((v: string[] | string | undefined) => Array.isArray(v) ? v.length > 0 : !!v)
      .length;
});
//#endregion

//#region Hooks and watcher
onMounted(() => {
  refreshEventsCount();
});

watch(() => [filter.value.start_date, filter.value.end_date], refreshEventsCount);
//#endregion

//#region Event handlers
async function onSearch(): Promise<void> {
  if (filter.value.variables.length === 0) {
    opensilex.showWarningToast(t("DataVisualisation.missingVariable"));
    return;
  }

  loading.value = true;
  try {
    const variableDetails: VariableDetailsDTO[] | undefined = await getNumericVariables(filter.value.variables);
    if (!variableDetails) {
      return;
    }
    const variable: VariableDetailsDTO = variableDetails[0];

    const [deviceSeries, loadedEvents]: [DataVisuSerie[], DataVisuEvent[]] = await Promise.all([
      loadDataSeries([buildSeriesRequest(variable)]),
      filter.value.showEvents ? loadDeviceEvents() : Promise.resolve([])
    ]);

    yAxisNames.value = [variable.unit?.name ? `${variable.name} (${variable.unit.name})` : variable.name];
    series.value = toObjectSeries(variable, await splitSeriesByTarget(deviceSeries));
    events.value = loadedEvents;
    searchFiltersToggle.value = true;
  } catch (error) {
    opensilex.errorHandler(error);
  } finally {
    loading.value = false;
  }
}
//#endregion

//#region Methods
/**
 * Only the data of the last 15 days by default
 */
function defaultFilter(): VisualisationFilter {
  return {
    variables: [],
    scientificObjects: [],
    start_date: getLastFifteenDaysStartDate(),
    end_date: undefined,
    provenance: undefined,
    showEvents: false
  };
}

function resetFilters(): void {
  filter.value = defaultFilter();
}

/**
 * All the data of the device for the variable, only on the selected scientific objects if any. It is then split by
 * scientific object, see toObjectSeries().
 */
function buildSeriesRequest(variable: VariableDetailsDTO): DataSeriesRequest {
  const f: VisualisationFilter = filter.value;
  return {
    name: variable.name,
    filter: {
      start_date: f.start_date,
      end_date: f.end_date,
      targets: f.scientificObjects.length > 0 ? f.scientificObjects : undefined,
      variables: [variable.uri],
      devices: [props.device],
      provenances: f.provenance ? [f.provenance] : undefined
    }
  };
}

/**
 * Names and colors the series of each scientific object. The data without scientific object is named after the
 * variable.
 */
function toObjectSeries(variable: VariableDetailsDTO, targetSeries: TargetSerie[]): DataVisuSerie[] {
  return targetSeries.map((targetSerie: TargetSerie, index: number): DataVisuSerie => ({
    name: targetSerie.target ? targetSerie.targetName : variable.name,
    points: targetSerie.points,
    color: getGraphicColor(index)
  }));
}

/**
 * Loads the events of the device, in a single "Events" legend entry, with one color per event type
 */
async function loadDeviceEvents(): Promise<DataVisuEvent[]> {
  const f: VisualisationFilter = filter.value;
  return colorEventsByType(await loadEvents([{
    target: props.device,
    start_date: f.start_date,
    end_date: f.end_date,
    seriesName: t("DataVisualisation.events")
  }]));
}

async function refreshEventsCount(): Promise<void> {
  const f: VisualisationFilter = filter.value;
  eventsCountLoading.value = true;
  try {
    eventsCount.value = await countEvents([props.device], f.start_date, f.end_date);
  } catch (error) {
    opensilex.errorHandler(error);
  } finally {
    eventsCountLoading.value = false;
  }
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

.eventsCount {
  margin-left: 8px;
}
</style>
