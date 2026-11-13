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
          <!-- Devices, not in an n-form-item: the selector renders its own -->
          <DeviceSelector
              :value="filter.devices"
              @update:value="onDeviceSelectorUpdate"
              :variable="variable"
              :multiple="true"
              :required="true"
              class="searchFilter"
          ></DeviceSelector>

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
              :targets="filter.devices"
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
                  :variables="[variable]"
                  :devices="filter.devices.length > 0 ? filter.devices : undefined"
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
            <n-spin :show="loading">
              <DataVisuGraphic
                  :series="series"
                  :yAxisNames="yAxisNames"
                  :elementName="elementName"
                  :elementUri="variable"
                  :events="events"
                  @eventCreated="onGraphicEventCreated"
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
import {DeviceGetDTO, DevicesService, VariableDetailsDTO} from "opensilex-core/index";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import PageContent from "@/components/layout/PageContent.vue";
import Card from "@/components/common/views/Card.vue";
import SearchFiltersSidebar from "@/components/common/filters/SearchFiltersSidebar.vue";
import DeviceSelector from "@/components/devices/DeviceSelector.vue";
import DateTimeForm from "@/components/common/forms/DateTimeForm.vue";
import DataProvenanceSelector from "@/components/data/DataProvenanceSelector.vue";
import DataVisuGraphic from "@/components/visualization/DataVisuGraphic.vue";
import DataVisuEventsSwitch from "@/components/visualization/DataVisuEventsSwitch.vue";
import type {DataVisuEvent, DataVisuSerie} from "@/components/visualization/DataVisuTypes";
import useDataVisualisation, {
  colorEventsByType,
  DataSeriesRequest,
  EventsRequest,
  getLastFifteenDaysStartDate,
  styleDeviceTargetSeries
} from "@/composables/useDataVisualisation";

interface VisualisationFilter {
  devices: string[]
  start_date: string | undefined
  end_date: string | undefined
  provenance: string | undefined
  /** Not a data filter: true to also draw the events of the devices */
  showEvents: boolean
}

/**
 * "Device associated data visualization" tab of a variable: graphic of the data of this variable measured by the
 * selected devices (among the devices that measured it), with the events of the devices.
 *
 * One series per (scientific object, device) pair: one color per scientific object, one symbol per device.
 */

//#region Public
const props = defineProps<{
  /** URI of the variable of the details page */
  variable: string
  /** Name of the variable, written in the title of the downloaded image */
  elementName?: string
}>();
//#endregion

//#region Private
//#region Plugins & services
const $opensilex: OpenSilexVuePlugin = inject<OpenSilexVuePlugin>("$opensilex")!;
const {t} = useI18n();
const {getNumericVariables, loadDataSeries, loadEvents, splitSeriesByTarget} = useDataVisualisation();

const devicesService: DevicesService = $opensilex.getService<DevicesService>("opensilex.DevicesService");
//#endregion

//#region Template refs
const eventsSwitch = useTemplateRef<InstanceType<typeof DataVisuEventsSwitch>>("eventsSwitch");
//#endregion

//#region Data & Computed
const filter: Ref<VisualisationFilter> = ref<VisualisationFilter>(defaultFilter());
/** true when the filters sidebar is collapsed */
const searchFiltersToggle: Ref<boolean> = ref<boolean>(false);
const loading: Ref<boolean> = ref<boolean>(false);
const series: Ref<DataVisuSerie[]> = ref<DataVisuSerie[]>([]);
const yAxisNames: Ref<string[]> = ref<string[]>([]);
const events: Ref<DataVisuEvent[]> = ref<DataVisuEvent[]>([]);

const activeFiltersCount: ComputedRef<number> = computed<number>(() => {
  const f: VisualisationFilter = filter.value;
  return [f.devices, f.start_date, f.end_date, f.provenance]
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

/**
 * The selector gives null when it is cleared, the filter always keeps an array
 */
function onDeviceSelectorUpdate(value: string | string[] | null): void {
  filter.value.devices = Array.isArray(value) ? value : (value ? [value] : []);
}

async function onSearch(): Promise<void> {
  if (filter.value.devices.length === 0) {
    $opensilex.showWarningToast(t("VariableVisualizationTab.missingDevices"));
    return;
  }

  loading.value = true;
  try {
    const variableDetails: VariableDetailsDTO[] | undefined = await getNumericVariables([props.variable]);
    if (!variableDetails) {
      return;
    }
    const variable: VariableDetailsDTO = variableDetails[0];
    const deviceNames: Map<string, string> = await getDeviceNames(filter.value.devices);

    const [deviceSeries, loadedEvents]: [DataVisuSerie[], DataVisuEvent[]] = await Promise.all([
      loadDataSeries(buildSeriesRequests(deviceNames)),
      filter.value.showEvents ? loadEvents(buildEventsRequests(deviceNames)).then(colorEventsByType) : Promise.resolve([])
    ]);

    yAxisNames.value = [variable.unit?.name ? `${variable.name} (${variable.unit.name})` : variable.name];
    series.value = styleDeviceTargetSeries(deviceSeries, await splitSeriesByTarget(deviceSeries));
    events.value = loadedEvents;
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
    devices: [],
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
 * @returns the name of each device by URI, to name the series and the events
 */
async function getDeviceNames(deviceUris: string[]): Promise<Map<string, string>> {
  const devices: DeviceGetDTO[] = (await devicesService.getDeviceByUris(deviceUris)).response.result;
  return new Map(devices.map((device: DeviceGetDTO) => [device.uri, device.name]));
}

/**
 * One request per selected device, named after the device. The series are then split by scientific object, see
 * styleDeviceTargetSeries().
 */
function buildSeriesRequests(deviceNames: Map<string, string>): DataSeriesRequest[] {
  const f: VisualisationFilter = filter.value;
  return f.devices.map((deviceUri: string): DataSeriesRequest => ({
    name: deviceNames.get(deviceUri) ?? deviceUri,
    filter: {
      start_date: f.start_date,
      end_date: f.end_date,
      variables: [props.variable],
      devices: [deviceUri],
      provenances: f.provenance ? [f.provenance] : undefined
    }
  }));
}

/**
 * The events of each device, in one legend entry per device, with one color per event type (the colors of the
 * series are the scientific objects)
 */
function buildEventsRequests(deviceNames: Map<string, string>): EventsRequest[] {
  const f: VisualisationFilter = filter.value;
  return f.devices.map((deviceUri: string): EventsRequest => ({
    target: deviceUri,
    start_date: f.start_date,
    end_date: f.end_date,
    seriesName: `${t("DataVisualisation.events")} -> ${deviceNames.get(deviceUri) ?? deviceUri}`
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
