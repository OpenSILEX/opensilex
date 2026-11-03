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
                placeholder="ScientificObjectVisualizationTab.variablePlaceholder"
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
                  :targets="[scientificObject.uri]"
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
                  :highlightedImageUri="highlightedImageUri"
                  @imageClick="onGraphicImageClick"
                  @imageHover="highlightedImageUri = $event"
              ></DataVisuGraphic>
            </n-spin>
          </div>
        </div>
      </n-layout-content>
    </n-layout>
  </PageContent>
</template>

<script setup lang="ts">
import {computed, ComputedRef, inject, onMounted, ref, Ref, useTemplateRef, watch} from "vue";
import {useI18n} from "vue-i18n";
import {NBadge, NCollapse, NCollapseItem, NForm, NFormItem, NLayout, NLayoutContent, NSpin, NSwitch} from "naive-ui";
import {VariableDetailsDTO} from "opensilex-core/index";
import {ScientificObjectDetailByExperimentsDTO} from "opensilex-core/model/scientificObjectDetailByExperimentsDTO";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import PageContent from "@/components/layout/PageContent.vue";
import SearchFiltersSidebar from "@/components/common/filters/SearchFiltersSidebar.vue";
import VariableSelectorWithFilter from "@/components/variables/views/VariableSelectorWithFilter.vue";
import DateTimeForm from "@/components/common/forms/DateTimeForm.vue";
import DataProvenanceSelector from "@/components/data/DataProvenanceSelector.vue";
import DataVisuGraphic, {DataVisuEvent, DataVisuSerie} from "@/components/visualization/DataVisuGraphic.vue";
import VisuImagePanel from "@/components/visualization/VisuImagePanel.vue";
import useDataVisualisation, {DataSeriesRequest, GRAPHIC_COLOR_PALETTE} from "@/composables/useDataVisualisation";
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
const opensilex: OpenSilexVuePlugin = inject<OpenSilexVuePlugin>("$opensilex");
const {t} = useI18n();
const {getNumericVariables, loadDataSeries, loadEvents, countEvents} = useDataVisualisation();
//#endregion

//#region Template refs
const imagePanel = useTemplateRef<InstanceType<typeof VisuImagePanel>>("imagePanel");
//#endregion

//#region Data & Computed
/** State and handlers of the image panel, see useVisuImagePanel */
const {
  panelImages,
  highlightedImageUri,
  onGraphicImageClick,
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
/** Number of events of the scientific object between the selected dates, displayed next to the switch */
const eventsCount: Ref<number> = ref<number>(0);
const eventsCountLoading: Ref<boolean> = ref<boolean>(false);

const activeFiltersCount: ComputedRef<number> = computed<number>(() => {
  const f: VisualisationFilter = filter.value;
  return [f.variables, f.start_date, f.end_date, f.provenance]
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
    opensilex.showWarningToast(t("ScientificObjectVisualizationTab.missingVariable"));
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
  const lastFifteenDays: Date = new Date();
  lastFifteenDays.setDate(lastFifteenDays.getDate() - 15);
  lastFifteenDays.setHours(0, 0, 0, 0);
  return {
    variables: [],
    start_date: lastFifteenDays.toISOString(),
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
    color: GRAPHIC_COLOR_PALETTE[0]
  };
}

/**
 * Loads the events of the scientific object, in a single "Events" legend entry, with one color per event type
 */
async function loadObjectEvents(): Promise<DataVisuEvent[]> {
  const f: VisualisationFilter = filter.value;
  const loadedEvents: DataVisuEvent[] = await loadEvents([{
    target: props.scientificObject.uri,
    start_date: f.start_date,
    end_date: f.end_date,
    seriesName: t("DataVisualisation.events")
  }]);

  const eventTypes: string[] = [...new Set(loadedEvents.map((event: DataVisuEvent) => event.event.rdf_type))];
  return loadedEvents.map((event: DataVisuEvent): DataVisuEvent => ({
    ...event,
    color: GRAPHIC_COLOR_PALETTE[eventTypes.indexOf(event.event.rdf_type) % GRAPHIC_COLOR_PALETTE.length]
  }));
}

async function refreshEventsCount(): Promise<void> {
  const f: VisualisationFilter = filter.value;
  eventsCountLoading.value = true;
  try {
    eventsCount.value = await countEvents([props.scientificObject.uri], f.start_date, f.end_date);
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
