<template>
  <LoadingOverlay :show="!isDataLoaded && !isNoDataFound">
    <n-tooltip trigger="hover" placement="top">
      <template #trigger>
        <Card
          ref="tilePanel"
          class="item variableTile clickableTile"
          :label="variableUri.name"
          @click="showGraphic"
        >
          <template v-slot:body>

            <TextView v-if="isNoDataFound"
                                id="no-data-text"
                                label="component.data.no-data">
            </TextView>

            <div
              id="data-infos"
              v-if="isDataLoaded">
              <div v-if="isDataOutOfReach">
                <TextView
                  style="margin-bottom: 0;"
                  :value="t('component.data.lastDataStored')">
                </TextView>
                <DateView
                  style="font-size: x-large; margin-bottom: 0; color: #e53935;"
                  :value="lastData.date"
                  :isDatetime="true"
                  :useLocaleFormat="true"
                  :dateTimeFormatOptions="{ dateStyle: 'long', timeStyle: 'long' }">
                </DateView>
              </div>
              <div v-else>
                <TextView
                  style="margin-bottom: 0;"
                  :value="t('component.data.lastMedianData')">
                </TextView>
                <TextView
                  style="font-size: x-large; margin-bottom: 0;"
                  :value="lastData.value"
                >
                </TextView>
                <DateView
                  style="font-size: x-large; margin-bottom: 0;"
                  :value="lastData.date"
                  :isDatetime="true"
                  :useLocaleFormat="true"
                  :dateTimeFormatOptions="{ dateStyle: 'long', timeStyle: 'long' }">
                </DateView>
              </div>
            </div>

            <!-- Modal for graphic -->

            <Modal ref="graphicModal" modalSize="xl">
              <template #header>
                <h4>
                  {{ variableUri.name }}
                </h4>
              </template>

              <div class="graphic-modal-header-actions">
                <!-- Period badge-->
                <n-tag v-if="period" round class="greenThemeColor">
                  <Icon icon="fa#hourglass-half"/>
                  {{ t("component.common.date-time.period-descriptions." + period) }}
                </n-tag>

                <!-- Settings button -->
                <Button
                  label="component.facility.monitoring.settings"
                  class="settingsButton"
                  icon="fa#cog"
                  @click="histogramSettings.show()"
                ></Button>
              </div>

              <div class="d-flex justify-content-center">
                <TextView
                  v-if="!isDataForGraph && isGraphicLoaded"
                  id="no-data-text"
                  label="component.data.no-data"
                >
                </TextView>
              </div>

              <!-- Graphic, only created once the modal is opened -->
              <n-spin v-if="isGraphicOpened" :show="!isGraphicLoaded">
                <DataVisuGraphic
                    :series="graphicSeries"
                    :yAxisNames="yAxisNames"
                    :elementName="variableUri.name"
                    :elementUri="variableUri.uri"
                ></DataVisuGraphic>
              </n-spin>

              <!-- Modal settings component-->
              <FacilityHistogramSettings
                ref="histogramSettings"
                @update="onSettingsApplied"
              ></FacilityHistogramSettings>

              <template #footer></template>
            </Modal>

          </template>
        </Card>
      </template>
      {{ t("VariableVisualizationTile.clickToViewGraphic") }}
    </n-tooltip>
  </LoadingOverlay>
</template>

<script setup lang="ts">
import {computed, ComputedRef, inject, onMounted, ref, Ref, useTemplateRef} from "vue";
import {useStore} from "vuex";
import {useI18n} from "vue-i18n";
import {NSpin, NTag, NTooltip} from "naive-ui";
import {NamedResourceDTOVariableModel} from "opensilex-core/index";
import {VariablesService} from "opensilex-core/api/variables.service";
import {DataService} from "opensilex-core/api/data.service";
import {VariableDetailsDTO} from "opensilex-core/model/variableDetailsDTO";
import {DataSerieGetDTO} from "opensilex-core/model/dataSerieGetDTO";
import {DataVariableSeriesGetDTO} from "opensilex-core/model/dataVariableSeriesGetDTO";
import {DataComputedGetDTO} from "opensilex-core/model/dataComputedGetDTO";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import DataVisuGraphic from "@/components/visualization/DataVisuGraphic.vue";
import type {DataVisuPoint, DataVisuSerie} from "@/components/visualization/DataVisuTypes";
import {getGraphicColor} from "@/composables/useDataVisualisation";
import {Period} from "@/components/common/forms/DatePeriodPicker.vue";
import FacilityHistogramSettings from "@/components/facilities/FacilityHistogramSettings.vue";
import LoadingOverlay from "@/components/layout/LoadingOverlay.vue";
import Card from "@/components/common/views/Card.vue";
import TextView from "@/components/common/views/TextView.vue";
import DateView from "@/components/common/views/DateView.vue";
import Icon from "@/components/common/views/Icon.vue";
import Modal from "@/components/common/views/Modal.vue";
import Button from "@/components/common/buttons/Button.vue";

/**
 * Last data displayed on the tile: the last median (with its value), or the last stored data when there is no data
 * in the default period (then only its date is displayed)
 */
interface LastData {
  /** Value with the unit symbol, e.g. "12.35 °C" */
  value?: string
  /** ISO date */
  date: string
}

/**
 * Tile of one variable on the Monitoring tab of a facility: the last median data of the variable measured on the
 * facility in the default period, or the date of its last data if there is none in this period.
 *
 * Clicking the tile opens the graphic of the variable, whose period and provenances can be changed in its settings.
 */

//#region Public
const props = defineProps<{
  /** The variable displayed by the tile */
  variableUri: NamedResourceDTOVariableModel
  /** URI of the facility the data was measured on */
  target: string
  /** Default period of the tile and of the graphic, ISO dates */
  defaultStartDate?: string
  defaultEndDate?: string
}>();
//#endregion

//#region Private
//#region Plugins & services
const $opensilex: OpenSilexVuePlugin = inject<OpenSilexVuePlugin>("$opensilex")!;
const store = useStore();
const {t} = useI18n();

const variablesService: VariablesService = $opensilex.getService<VariablesService>("opensilex.VariablesService");
const dataService: DataService = $opensilex.getService<DataService>("opensilex.DataService");
//#endregion

//#region Template refs
const graphicModal = useTemplateRef<InstanceType<typeof Modal>>("graphicModal");
const histogramSettings = useTemplateRef<InstanceType<typeof FacilityHistogramSettings>>("histogramSettings");
//#endregion

//#region Data & Computed
const period: Ref<Period> = ref<Period>("week");
/** Period of the graphic, changed in its settings */
const graphicStartDate: Ref<string | undefined> = ref<string | undefined>(props.defaultStartDate);
const graphicEndDate: Ref<string | undefined> = ref<string | undefined>(props.defaultEndDate);
/** true to display the series of every provenance, and not only the calculated series */
const isLoadAllProvToggled: Ref<boolean> = ref<boolean>(false);

const variable: Ref<VariableDetailsDTO | undefined> = ref<VariableDetailsDTO | undefined>(undefined);
/** Series of each provenance, only loaded when all the provenances are displayed */
const dataSeries: Ref<DataSerieGetDTO[]> = ref<DataSerieGetDTO[]>([]);
/** Series calculated from the provenance series, e.g. the median per hour */
const calculatedDataSeries: Ref<DataSerieGetDTO[]> = ref<DataSerieGetDTO[]>([]);
const lastData: Ref<LastData | undefined> = ref<LastData | undefined>(undefined);

const isNoDataFound: Ref<boolean> = ref<boolean>(false);
/** true when there is data, but none in the default period: only the date of the last data is displayed */
const isDataOutOfReach: Ref<boolean> = ref<boolean>(false);
const isDataLoaded: Ref<boolean> = ref<boolean>(false);
const isGraphicLoaded: Ref<boolean> = ref<boolean>(true);
/** true once the graphic modal has been opened, the graphic is only created then */
const isGraphicOpened: Ref<boolean> = ref<boolean>(false);

const hasCalculatedSeries: ComputedRef<boolean> = computed<boolean>(() => calculatedDataSeries.value.length > 0);

const isDataForGraph: ComputedRef<boolean> = computed<boolean>(() =>
    dataSeries.value.length > 0 || calculatedDataSeries.value.length > 0
);

/**
 * The calculated series first (e.g. the median), then the series of each provenance. Only the first calculated
 * series is displayed at first, or the provenance series if there is no calculated series. The others can be
 * displayed from the legend.
 */
const graphicSeries: ComputedRef<DataVisuSerie[]> = computed<DataVisuSerie[]>(() => [
  ...calculatedDataSeries.value.map((serie: DataSerieGetDTO, index: number) => toDataVisuSerie(serie, index, index > 0)),
  ...dataSeries.value.map((serie: DataSerieGetDTO, index: number) =>
      toDataVisuSerie(serie, calculatedDataSeries.value.length + index, hasCalculatedSeries.value))
]);

const yAxisNames: ComputedRef<string[]> = computed<string[]>(() => {
  if (!variable.value) {
    return [];
  }
  return [variable.value.unit?.name ? `${variable.value.name} (${variable.value.unit.name})` : variable.value.name];
});
//#endregion

//#region Hooks & watchers
onMounted(() => {
  loadVariableAndLastData();
});
//#endregion

//#region Event handlers
function onSettingsApplied(newPeriod: Period, begin: string, end: string, selectAll: boolean): void {
  period.value = newPeriod;
  graphicStartDate.value = begin;
  graphicEndDate.value = end;
  isLoadAllProvToggled.value = selectAll;
  loadAllData();
}
//#endregion

//#region Methods
/**
 * Starts the request without the page-wide loader, as the tile and the graphic have their own. The HTTP client
 * shows the loader when a request starts, so the loader is enabled again right after, for the next requests.
 */
function withoutPageLoader<T>(request: () => Promise<T>): Promise<T> {
  $opensilex.disableLoader();
  try {
    return request();
  } finally {
    $opensilex.enableLoader();
  }
}

async function loadVariableAndLastData(): Promise<void> {
  try {
    variable.value = (await withoutPageLoader(() => variablesService.getVariable(props.variableUri.uri))).response.result;
    await loadLastData();
  } catch (error) {
    // Without data, the loading overlay of the tile is removed
    isNoDataFound.value = true;
    $opensilex.errorHandler(error);
  }
}

/**
 * Only loads the calculated series of the default period, to display the last median data on the tile
 */
async function loadLastData(): Promise<void> {
  isNoDataFound.value = false;
  isDataOutOfReach.value = false;
  isDataLoaded.value = false;

  const seriesDTO: DataVariableSeriesGetDTO = (await withoutPageLoader(() => dataService.getDataSeriesByFacility(
      props.variableUri.uri,
      props.target,
      props.defaultStartDate || undefined,
      props.defaultEndDate || undefined,
      true
  ))).response.result;

  if (!seriesDTO.last_data_stored) {
    isNoDataFound.value = true;
    return;
  }

  if (seriesDTO.calculated_series.length === 0) {
    lastData.value = {date: new Date(seriesDTO.last_data_stored.date).toISOString()};
    isDataOutOfReach.value = true;
    isDataLoaded.value = true;
    return;
  }

  calculatedDataSeries.value = seriesDTO.calculated_series;
  lastData.value = getLastMedianData(seriesDTO.calculated_series[0]);
  isDataLoaded.value = true;
}

/**
 * Loads the series of the graphic period, with the series of every provenance if they are toggled
 */
async function loadAllData(): Promise<void> {
  isGraphicLoaded.value = false;
  try {
    const seriesDTO: DataVariableSeriesGetDTO = (await withoutPageLoader(() => dataService.getDataSeriesByFacility(
        props.variableUri.uri,
        props.target,
        graphicStartDate.value || undefined,
        graphicEndDate.value || undefined,
        !isLoadAllProvToggled.value
    ))).response.result;

    dataSeries.value = seriesDTO.data_series ?? [];
    calculatedDataSeries.value = seriesDTO.calculated_series ?? [];
    warnIfDataLimitReached();
    showGraphic();
  } catch (error) {
    $opensilex.errorHandler(error);
  } finally {
    isGraphicLoaded.value = true;
  }
}

/**
 * @returns the most recent data of the median series, with the unit symbol of the variable
 */
function getLastMedianData(medianSerie: DataSerieGetDTO): LastData | undefined {
  const sortedData: DataComputedGetDTO[] = [...(medianSerie.data ?? [])]
      .sort((a: DataComputedGetDTO, b: DataComputedGetDTO) => new Date(a.date).getTime() - new Date(b.date).getTime());
  const latestData: DataComputedGetDTO | undefined = sortedData[sortedData.length - 1];
  if (!latestData) {
    return undefined;
  }
  const unitSymbol: string | undefined = variable.value?.unit?.symbol;
  return {
    value: Number(latestData.value).toPrecision(4) + (unitSymbol ? ` ${unitSymbol}` : ""),
    date: new Date(latestData.date).toISOString()
  };
}

function showGraphic(): void {
  isGraphicOpened.value = true;
  graphicModal.value?.show();
}

/**
 * Converts a series of the facility endpoint into a series of the graphic. Its points have no original Data: the
 * endpoint only gives dates and values (e.g. medians).
 */
function toDataVisuSerie(serie: DataSerieGetDTO, index: number, hidden: boolean): DataVisuSerie {
  const points: DataVisuPoint[] = (serie.data ?? [])
      .map((data: DataComputedGetDTO): DataVisuPoint => ({
        timestamp: new Date(data.date).getTime(),
        value: Number(data.value)
      }))
      .sort((a: DataVisuPoint, b: DataVisuPoint) => a.timestamp - b.timestamp);
  return {
    name: serie.provenance?.name ?? serie.provenance?.uri ?? "",
    points,
    color: getGraphicColor(index),
    hidden
  };
}

function warnIfDataLimitReached(): void {
  const limit: number = store.state.graphDataLimit;
  for (const serie of [...calculatedDataSeries.value, ...dataSeries.value]) {
    if ((serie.data?.length ?? 0) > limit) {
      $opensilex.showInfoToast(t("component.common.search.limitSizeMessageBasic", {count: serie.data.length}));
    }
  }
}
//#endregion
//#endregion
</script>

<style scoped lang="scss">
/* The tile opens the graphic when clicked */
.clickableTile {
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.clickableTile:hover {
  transform: scale(1.02);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}


.settingsButton {
  color: #00A28C;
  font-size: 1.2em;
  border: none;
  background: none;
}
.settingsButton:hover {
  background-color: #00A28C;
  color: #f1f1f1
}
.graphic-modal-header-actions {
  margin-left: auto;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.5rem;
}

</style>

