<template>
  <div>
    <Card :noFooter="true" icon="fa#chart-bar" class="histogramCard">
      <template #header>
        <span class="graphicTitle">
          {{ dataLocationInformations || t("Histogram.graphicInformations") }}
        </span>
      </template>

      <template #rightHeader>
        <div class="graphicOptions">
          <n-tag round class="greenThemeColor">
            <Icon icon="fa#hourglass-half"/>
            {{ t(`Histogram.period.${period}`) }}
          </n-tag>
          <Button
              class="settingsButton"
              label="component.facility.monitoring.settings"
              icon="fa#cog"
              :small="true"
              @click="histogramSettings?.show()"
          ></Button>
        </div>
      </template>

      <template #body>
        <n-spin :show="loading">
          <DataVisuGraphic
              :series="series"
              :yAxisNames="yAxisNames"
              :elementName="variable?.name"
              :elementUri="variableChoice"
          ></DataVisuGraphic>
        </n-spin>
      </template>
    </Card>

    <HistogramSettings
        ref="histogramSettings"
        :devices="devices"
        @validate="onSettingsApplied"
    ></HistogramSettings>
  </div>
</template>

<script setup lang="ts">
import {inject, onMounted, ref, Ref, useTemplateRef} from "vue";
import {useI18n} from "vue-i18n";
import {NSpin, NTag} from "naive-ui";
import {DeviceGetDTO, DevicesService, VariableDetailsDTO} from "opensilex-core/index";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import DataVisuGraphic from "@/components/visualization/DataVisuGraphic.vue";
import type {DataVisuSerie} from "@/components/visualization/DataVisuTypes";
import HistogramSettings from "@/components/home/dashboard/HistogramSettings.vue";
import {Period} from "@/components/common/forms/DatePeriodPicker.vue";
import useDataVisualisation, {
  DataSeriesRequest,
  styleDeviceTargetSeries
} from "@/composables/useDataVisualisation";
import Button from "@/components/common/buttons/Button.vue";
import Card from "@/components/common/views/Card.vue";
import Icon from "@/components/common/views/Icon.vue";

/**
 * Graphic of the home dashboard: the data of the variable configured in the front configuration
 * (front.dashboard.graph1.variable), measured by devices over a recent period (last day by default).
 *
 * All the devices that measured the variable are displayed by default, the settings allow to choose the period and
 * the devices. One series per (scientific object, device) pair: one color per scientific object, one symbol per
 * device, see styleDeviceTargetSeries().
 */

//#region Public
const props = defineProps<{
  /** URI of the variable to display */
  variableChoice: string
}>();
//#endregion

//#region Private
//#region Plugins & services
const $opensilex: OpenSilexVuePlugin = inject<OpenSilexVuePlugin>("$opensilex")!;
const {t} = useI18n();
const {getNumericVariables, loadDataSeries, splitSeriesByTarget} = useDataVisualisation();

const devicesService: DevicesService = $opensilex.getService<DevicesService>("opensilex.DevicesService");
//#endregion

//#region Template refs
const histogramSettings = useTemplateRef<InstanceType<typeof HistogramSettings>>("histogramSettings");
//#endregion

//#region Data & Computed
/** Length of each period, the period always ends now */
const PERIOD_LENGTHS: Record<Period, { days?: number, months?: number }> = {
  day: {days: 1},
  week: {days: 7},
  month: {months: 1},
  "6month": {months: 6},
  year: {months: 12}
};

/** Text displayed in the header, set in the front configuration (front.dashboard.graph1.dataLocationInformations) */
const dataLocationInformations: string = $opensilex.getConfig().dashboard.graph1.dataLocationInformations;

const period: Ref<Period> = ref<Period>("day");
/** All the devices that measured the variable */
const devices: Ref<DeviceGetDTO[]> = ref<DeviceGetDTO[]>([]);
/** URIs of the devices to display, undefined for all the devices */
const selectedDevices: Ref<string[] | undefined> = ref<string[] | undefined>(undefined);
const variable: Ref<VariableDetailsDTO | undefined> = ref<VariableDetailsDTO | undefined>(undefined);

const loading: Ref<boolean> = ref<boolean>(false);
const series: Ref<DataVisuSerie[]> = ref<DataVisuSerie[]>([]);
const yAxisNames: Ref<string[]> = ref<string[]>([]);
//#endregion

//#region Hooks & watchers
onMounted(async () => {
  await loadDevices();
  await loadData();
});
//#endregion

//#region Event handlers
function onSettingsApplied(newPeriod: Period, newDevices: string[] | undefined): void {
  period.value = newPeriod;
  selectedDevices.value = newDevices;
  loadData();
}
//#endregion

//#region Methods
/**
 * Loads the devices that measured the variable
 */
async function loadDevices(): Promise<void> {
  try {
    const http = await devicesService.searchDevices(
        undefined,       // rdf_type
        true,            // include_subtypes
        undefined,       // name
        props.variableChoice,
        undefined,       // year
        undefined,       // existence_date
        undefined,       // facility
        undefined,       // brand
        undefined,       // model
        undefined,       // serial_number
        undefined,       // metadata
        undefined,       // relations
        ["name=asc"],
        0,
        0                // all the devices
    );
    devices.value = http.response.result;
  } catch (error) {
    $opensilex.errorHandler(error);
  }
}

async function loadData(): Promise<void> {
  loading.value = true;
  try {
    const variableDetails: VariableDetailsDTO[] | undefined = await getNumericVariables([props.variableChoice]);
    if (!variableDetails) {
      return;
    }
    variable.value = variableDetails[0];

    const deviceSeries: DataVisuSerie[] = await loadDataSeries(buildSeriesRequests());
    yAxisNames.value = [variable.value.unit?.name ? `${variable.value.name} (${variable.value.unit.name})` : variable.value.name];
    series.value = styleDeviceTargetSeries(deviceSeries, await splitSeriesByTarget(deviceSeries));
  } catch (error) {
    $opensilex.errorHandler(error);
  } finally {
    loading.value = false;
  }
}

/**
 * One request per displayed device, from the start of the period until now
 */
function buildSeriesRequests(): DataSeriesRequest[] {
  const endDate: Date = new Date();
  const startDate: Date = new Date();
  const length = PERIOD_LENGTHS[period.value];
  startDate.setDate(startDate.getDate() - (length.days ?? 0));
  startDate.setMonth(startDate.getMonth() - (length.months ?? 0));

  return devices.value
      .filter((device: DeviceGetDTO) => !selectedDevices.value || selectedDevices.value.includes(device.uri))
      .map((device: DeviceGetDTO): DataSeriesRequest => ({
        name: device.name,
        filter: {
          start_date: startDate.toISOString(),
          end_date: endDate.toISOString(),
          variables: [props.variableChoice],
          devices: [device.uri]
        }
      }));
}
//#endregion
//#endregion
</script>

<style scoped lang="scss">
.settingsButton {
  color: #00A28C;
  font-size: 1.2em;
  border: none;
  background: none;
}

.settingsButton:hover {
  background-color: #00A28C;
  color: #f1f1f1;
}

.histogramCard {
  height: 72.5vh
}

.graphicTitle {
  font-size: 18px;
  width: 60%;
}

@media (min-width: 1401px) and (max-width: 1700px) {
  .histogramCard {
    height: 75vh
  }
}

@media screen and (max-width: 1400px) {
  .histogramCard {
    height: 66vh
  }
}

.graphicOptions {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
