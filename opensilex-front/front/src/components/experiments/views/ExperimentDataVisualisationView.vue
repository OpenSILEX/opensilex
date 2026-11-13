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

          <!-- Events -->
          <DataVisuEventsSwitch
              ref="eventsSwitch"
              v-model="filter.showEvents"
              :targets="filter.scientificObjects"
              :startDate="filter.start_date"
              :endDate="filter.end_date"
          ></DataVisuEventsSwitch>

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
        <Card :noHeader="true" :noFooter="true">
          <template #body>
            <VisuImagePanel
                v-if="panelImages.length > 0"
                ref="imagePanel"
                :images="panelImages"
                :highlightedImageUri="highlightedImageUri"
                @hide="onImagePanelHide"
                @hover="highlightedImageUri = $event"
                @annotationCreated="onImagePanelAnnotationCreated"
            ></VisuImagePanel>
            <n-spin :show="loading">
              <DataVisuGraphic
                  :series="series"
                  :yAxisNames="yAxisNames"
                  :elementName="elementName"
                  :elementUri="uri"
                  :events="events"
                  :eventContext="uri"
                  @eventCreated="onGraphicEventCreated"
                  :highlightedImageUri="highlightedImageUri"
                  @imageClick="onGraphicImageClicked"
                  @imageHover="highlightedImageUri = $event"
              ></DataVisuGraphic>
            </n-spin>
          </template>
        </Card>
      </n-layout-content>
    </n-layout>
  </PageContent>
</template>

<script setup lang="ts">
import {computed, ComputedRef, inject, onMounted, ref, Ref, useTemplateRef} from "vue";
import {useI18n} from "vue-i18n";
import {useRoute} from "vue-router";
import {NCollapse, NCollapseItem, NForm, NFormItem, NLayout, NLayoutContent, NSpin} from "naive-ui";
import {ExperimentsService, VariableDetailsDTO} from "opensilex-core/index";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import PageContent from "@/components/layout/PageContent.vue";
import Card from "@/components/common/views/Card.vue";
import SearchFiltersSidebar from "@/components/common/filters/SearchFiltersSidebar.vue";
import ModalFormSelector from "@/components/variables/form/ModalFormSelector.vue";
import VariableSelectorWithFilter from "@/components/variables/views/VariableSelectorWithFilter.vue";
import DateTimeForm from "@/components/common/forms/DateTimeForm.vue";
import DataProvenanceSelector from "@/components/data/DataProvenanceSelector.vue";
import DataVisuGraphic from "@/components/visualization/DataVisuGraphic.vue";
import DataVisuEventsSwitch from "@/components/visualization/DataVisuEventsSwitch.vue";
import type {DataVisuEvent, DataVisuSerie} from "@/components/visualization/DataVisuTypes";
import VisuImagePanel from "@/components/visualization/VisuImagePanel.vue";
import {ScientificObjectFilter} from "@/components/scientificObjects/ScientificObjectList.vue";
import useDataVisualisation, {
  DataSeriesRequest,
  EventsRequest,
  getGraphicColor,
  getGraphicSymbol,
  getLastFifteenDaysStartDate
} from "@/composables/useDataVisualisation";
import useVisuImagePanel from "@/composables/useVisuImagePanel";

interface VisualisationFilter {
  scientificObjects: string[]
  variables: string[]
  start_date: string | undefined
  end_date: string | undefined
  provenance: string | undefined
  /** Not a data filter: true to also draw the events of the scientific objects */
  showEvents: boolean
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
const $opensilex: OpenSilexVuePlugin = inject<OpenSilexVuePlugin>("$opensilex")!;
const {t} = useI18n();
const route = useRoute();
const {getNumericVariables, loadDataSeries, loadEvents} = useDataVisualisation();

const experimentsService: ExperimentsService = $opensilex.getService<ExperimentsService>("opensilex.ExperimentsService");
//#endregion

//#region Template refs
const eventsSwitch = useTemplateRef<InstanceType<typeof DataVisuEventsSwitch>>("eventsSwitch");
const imagePanel = useTemplateRef<InstanceType<typeof VisuImagePanel>>("imagePanel");
//#endregion

//#region Data & Computed
/** State and handlers of the image panel, see useVisuImagePanel */
const {
  panelImages,
  highlightedImageUri,
  onGraphicImageClicked,
  onImagePanelAnnotationCreated,
  onImagePanelHide,
  clearImagePanel
} = useVisuImagePanel(imagePanel);

const SCIENTIFIC_OBJECTS_LIMIT: number = 15;
const VARIABLES_LIMIT: number = 2;

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
const events: Ref<DataVisuEvent[]> = ref<DataVisuEvent[]>([]);

const activeFiltersCount: ComputedRef<number> = computed<number>(() => {
  const f: VisualisationFilter = filter.value;
  return [f.scientificObjects, f.variables, f.start_date, f.end_date, f.provenance]
      .filter((v: string[] | string | undefined) => Array.isArray(v) ? v.length > 0 : !!v)
      .length;
});
//#endregion

//#region Hooks & watchers
onMounted(() => {
  initDefaultDates();
});
//#endregion

//#region Event handlers
/**
 * An event was created from the menu of a data point: the count is refreshed, and the graphic too if it displays the
 * events
 */
function onGraphicEventCreated(): void {
  eventsSwitch.value?.refresh();
  if (filter.value.showEvents) {
    onSearch();
  }
}

async function onSearch(): Promise<void> {
  const {scientificObjects, variables} = filter.value;
  if (scientificObjects.length === 0 || variables.length === 0) {
    $opensilex.showWarningToast(t("ExperimentDataVisualisationView.missingFilters"));
    return;
  }

  loading.value = true;
  try {
    const variableDetails: VariableDetailsDTO[] | undefined = await getNumericVariables(variables);
    if (!variableDetails) {
      return;
    }

    const [loadedSeries, loadedEvents]: [DataVisuSerie[], DataVisuEvent[]] = await Promise.all([
      loadDataSeries(buildSeriesRequests(scientificObjects, variableDetails), {withImages: true}),
      filter.value.showEvents ? loadEvents(buildEventsRequests(scientificObjects)) : Promise.resolve([])
    ]);

    yAxisNames.value = variableDetails.map((v: VariableDetailsDTO) => v.unit?.name ? `${v.name} (${v.unit.name})` : v.name);
    series.value = loadedSeries;
    events.value = loadedEvents;
    clearImagePanel();
    searchFiltersToggle.value = true;
  } catch (error) {
    $opensilex.errorHandler(error);
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
    showEvents: false
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
          filter.value.start_date = getLastFifteenDaysStartDate();
        }
      })
      .catch(error => $opensilex.errorHandler(error));
}

function resetFilters(): void {
  filter.value = defaultFilter();
  selectedScientificObjectsWithLabels.value = null;
}

/**
 * @returns the label of the scientific object, or its URI if its label isn't known
 */
function getScientificObjectLabel(soUri: string): string {
  return selectedScientificObjectsWithLabels.value?.find((so: NamedScientificObject) => so.uri === soUri)?.name ?? soUri;
}

/** One color per scientific object */
function getScientificObjectColor(soIndex: number): string {
  return getGraphicColor(soIndex);
}

/**
 * One series per (scientific object, variable) pair.
 * Each scientific object gets its own color, and each variable its own symbol and Y axis.
 */
function buildSeriesRequests(scientificObjects: string[], variables: VariableDetailsDTO[]): DataSeriesRequest[] {
  const f: VisualisationFilter = filter.value;
  const multipleVariables: boolean = variables.length > 1;

  return variables.flatMap((variable: VariableDetailsDTO, variableIndex: number) =>
      scientificObjects.map((soUri: string, soIndex: number): DataSeriesRequest => {
        const soLabel: string = getScientificObjectLabel(soUri);
        return {
          name: multipleVariables ? `${soLabel} / ${variable.name}` : soLabel,
          filter: {
            start_date: f.start_date,
            end_date: f.end_date,
            experiments: [uri],
            targets: [soUri],
            variables: [variable.uri],
            provenances: f.provenance ? [f.provenance] : undefined
          },
          color: getScientificObjectColor(soIndex),
          symbol: getGraphicSymbol(variableIndex),
          yAxisIndex: variableIndex
        };
      })
  );
}

/**
 * The events of each scientific object, with the color of the scientific object.
 */
function buildEventsRequests(scientificObjects: string[]): EventsRequest[] {
  const f: VisualisationFilter = filter.value;
  return scientificObjects.map((soUri: string, soIndex: number): EventsRequest => ({
    target: soUri,
    start_date: f.start_date,
    end_date: f.end_date,
    seriesName: `${t("DataVisualisation.events")} -> ${getScientificObjectLabel(soUri)}`,
    color: getScientificObjectColor(soIndex)
  }));
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
