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
                :objects="[scientificObject.uri]"
                :withAssociatedData="true"
                :maximumSelectedRows="1"
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
              :targets="[scientificObject.uri]"
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
                  :targets="[scientificObject.uri]"
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
                  :elementUri="scientificObject.uri"
                  :events="events"
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
import {computed, ComputedRef, inject, ref, Ref, useTemplateRef} from "vue";
import {useI18n} from "vue-i18n";
import {NCollapse, NCollapseItem, NForm, NFormItem, NLayout, NLayoutContent, NSpin} from "naive-ui";
import {VariableDetailsDTO} from "opensilex-core/index";
import {ScientificObjectDetailByExperimentsDTO} from "opensilex-core/model/scientificObjectDetailByExperimentsDTO";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import PageContent from "@/components/layout/PageContent.vue";
import Card from "@/components/common/views/Card.vue";
import SearchFiltersSidebar from "@/components/common/filters/SearchFiltersSidebar.vue";
import VariableSelectorWithFilter from "@/components/variables/views/VariableSelectorWithFilter.vue";
import DateTimeForm from "@/components/common/forms/DateTimeForm.vue";
import DataProvenanceSelector from "@/components/data/DataProvenanceSelector.vue";
import DataVisuGraphic from "@/components/visualization/DataVisuGraphic.vue";
import DataVisuEventsSwitch from "@/components/visualization/DataVisuEventsSwitch.vue";
import type {DataVisuEvent, DataVisuSerie} from "@/components/visualization/DataVisuTypes";
import VisuImagePanel from "@/components/visualization/VisuImagePanel.vue";
import useDataVisualisation, {
  colorEventsByType,
  DataSeriesRequest,
  getGraphicColor,
  getLastFifteenDaysStartDate
} from "@/composables/useDataVisualisation";
import useVisuImagePanel from "@/composables/useVisuImagePanel";

interface VisualisationFilter {
  /** At most one variable, an array as it is the v-model of the variable selector */
  variables: string[]
  start_date: string | undefined
  end_date: string | undefined
  provenance: string | undefined
  /** Not a data filter: true to also draw the events of the scientific object */
  showEvents: boolean
}

/**
 * "Visualization" tab of a scientific object: graphic of the data of one variable measured on this object, with its
 * events and images.
 */

//#region Public
const props = defineProps<{
  /** The scientific object of the details page */
  scientificObject: ScientificObjectDetailByExperimentsDTO
  /** Name of the scientific object, written in the title of the downloaded image */
  elementName?: string
}>();
//#endregion

//#region Private
//#region Plugins & services
const $opensilex: OpenSilexVuePlugin = inject<OpenSilexVuePlugin>("$opensilex")!;
const {t} = useI18n();
const {getNumericVariables, loadDataSeries, loadEvents} = useDataVisualisation();
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

const filter: Ref<VisualisationFilter> = ref<VisualisationFilter>(defaultFilter());
/** true when the filters sidebar is collapsed */
const searchFiltersToggle: Ref<boolean> = ref<boolean>(false);
const loading: Ref<boolean> = ref<boolean>(false);
const series: Ref<DataVisuSerie[]> = ref<DataVisuSerie[]>([]);
const yAxisNames: Ref<string[]> = ref<string[]>([]);
const events: Ref<DataVisuEvent[]> = ref<DataVisuEvent[]>([]);

const activeFiltersCount: ComputedRef<number> = computed<number>(() => {
  const f: VisualisationFilter = filter.value;
  return [f.variables, f.start_date, f.end_date, f.provenance]
      .filter((v: string[] | string | undefined) => Array.isArray(v) ? v.length > 0 : !!v)
      .length;
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
  if (filter.value.variables.length === 0) {
    $opensilex.showWarningToast(t("DataVisualisation.missingVariable"));
    return;
  }

  loading.value = true;
  try {
    const variableDetails: VariableDetailsDTO[] | undefined = await getNumericVariables(filter.value.variables);
    if (!variableDetails) {
      return;
    }
    const variable: VariableDetailsDTO = variableDetails[0];

    const [loadedSeries, loadedEvents]: [DataVisuSerie[], DataVisuEvent[]] = await Promise.all([
      loadDataSeries([buildSeriesRequest(variable)], {withImages: true}),
      filter.value.showEvents ? loadObjectEvents() : Promise.resolve([])
    ]);

    yAxisNames.value = [variable.unit?.name ? `${variable.name} (${variable.unit.name})` : variable.name];
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
/**
 * Only the data of the last 15 days by default
 */
function defaultFilter(): VisualisationFilter {
  return {
    variables: [],
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
 * The only series: the data of the variable measured on the scientific object, named after the object
 */
function buildSeriesRequest(variable: VariableDetailsDTO): DataSeriesRequest {
  const f: VisualisationFilter = filter.value;
  return {
    name: props.scientificObject.name,
    filter: {
      start_date: f.start_date,
      end_date: f.end_date,
      targets: [props.scientificObject.uri],
      variables: [variable.uri],
      provenances: f.provenance ? [f.provenance] : undefined
    },
    color: getGraphicColor(0)
  };
}

/**
 * Loads the events of the scientific object, in a single "Events" legend entry, with one color per event type
 */
async function loadObjectEvents(): Promise<DataVisuEvent[]> {
  const f: VisualisationFilter = filter.value;
  return colorEventsByType(await loadEvents([{
    target: props.scientificObject.uri,
    start_date: f.start_date,
    end_date: f.end_date,
    seriesName: t("DataVisualisation.events")
  }]));
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
