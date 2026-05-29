<template>
  <opensilex-Overlay :show="!isDataLoaded && !isNoDataFound">
    <opensilex-Card
      ref="tilePanel"
      class="item variableTile"
      :label="variableUri.name"
      @click="showGraphic"
    >
      <template v-slot:body>

        <opensilex-TextView v-if="isNoDataFound"
                            id="no-data-text"
                            :label="t('component.data.no-data')">
        </opensilex-TextView>

        <div
          id="data-infos"
          v-if="isDataLoaded">
          <div class="row">
            <div class="col" v-if="isDataOutOfReach">
              <opensilex-TextView
                style="margin-bottom: 0;"
                :value="t('component.data.lastDataStored')">
              </opensilex-TextView>
              <opensilex-DateView
                style="font-size: x-large; margin-bottom: 0; color: #e53935;"
                :value.sync="lastData.date"
                :isDateTime="true"
                :useLocaleFormat="true"
                :dateTimeFormatOptions="{ dateStyle: 'long', timeStyle: 'long' }">
              </opensilex-DateView>
            </div>
            <div class="col" v-else>
              <opensilex-TextView
                style="margin-bottom: 0;"
                :value="t('component.data.lastMedianData')">
              </opensilex-TextView>
              <opensilex-TextView
                style="font-size: x-large; margin-bottom: 0;"
                :value.sync="lastData.value"
              >
              </opensilex-TextView>
              <opensilex-DateView
                style="font-size: x-large; margin-bottom: 0;"
                :value.sync="lastData.date"
                :isDateTime="true"
                :useLocaleFormat="true"
                :dateTimeFormatOptions="{ dateStyle: 'long', timeStyle: 'long' }">
              </opensilex-DateView>
            </div>
          </div>
        </div>

        <!-- Modal for graphic -->

        <b-modal
          ref="graphic-modal"
          size="xl"
          :title="variableUri.name"
          hide-footer
        >
          <div class="text-right card-header-right">
            <!-- Period badge-->
            <b-badge v-if="period" pill class="greenThemeColor">
              <opensilex-Icon icon="fa#hourglass-half"/> {{ t("component.common.period-descriptions." + period) }}
            </b-badge>

            <!-- Graphic Options menu dropdown-->
            <b-dropdown right size="lg" variant="link" toggle-class="text-decoration-none" no-caret>
              <template #button-content>
                <opensilex-Icon icon="fa#bars" size="lg" class="dashboardGraphicMenu" />
              </template>

              <b-dropdown-item @click="scatter">
                <opensilex-Icon icon="fa#braille" />
                {{ t("component.common.change-viewing-mode") }}
              </b-dropdown-item>

              <b-dropdown-item @click="fullscreen">
                <opensilex-Icon icon="fa#expand" />
                {{ t("component.common.fullscreen") }}
              </b-dropdown-item>

              <b-dropdown-item @click="exportPNG">
                <opensilex-Icon icon="fa#download" />
                {{ t("component.datafile.download-image") }}
              </b-dropdown-item>
            </b-dropdown>

            <!-- Settings button -->
            <opensilex-Button
              label=Histogram.settings
              class="settingsButton"
              icon="fa#cog"
              @click="histogramSettings.show()"
              @update="onSettingsChanged"
            ></opensilex-Button>
          </div>

          <div class="row justify-content-center">
            <opensilex-TextView v-if="!isDataForGraph && isGraphicLoaded"
                                id="no-data-text"
                                :label="t('component.data.no-data')">
            </opensilex-TextView>
          </div>

          <!-- Graphic -->
          <div class="graphicVisualisationContainer">
            <div class="d-flex justify-content-center mb-3" v-if="!isGraphicLoaded">
              <b-spinner label="Loading..."></b-spinner>
            </div>
            <opensilex-VisualisationGraphic
              v-if="isGraphicLoaded"
              id="graphic"
              ref="visuGraphic"
              :deviceType="false"
              :lType="true"
              :lWidth="true"
            ></opensilex-VisualisationGraphic>
          </div>

          <!-- Modal settings component-->
          <opensilex-FacilityHistogramSettings
            ref="histogramSettings"
            @update="onSettingsChanged"
          ></opensilex-FacilityHistogramSettings>
        </b-modal>

      </template>
    </opensilex-Card>
  </opensilex-Overlay>
</template>

<script setup lang="ts">
import {DevicesService, NamedResourceDTOVariableModel} from "opensilex-core/index";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import HighchartsDataTransformer from "../../../models/HighchartsDataTransformer";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import {VariablesService} from "opensilex-core/api/variables.service";
import {DataService} from "opensilex-core/api/data.service";
import {VariableDetailsDTO} from "opensilex-core/model/variableDetailsDTO";
import {DataSerieGetDTO} from "opensilex-core/model/dataSerieGetDTO";
import {DataVariableSeriesGetDTO} from "opensilex-core/model/dataVariableSeriesGetDTO";
import {DataSimpleProvenanceGetDTO} from "opensilex-core/model/dataSimpleProvenanceGetDTO";
import {DataComputedGetDTO} from "opensilex-core/model/dataComputedGetDTO";
import {Period} from "../../facilities/DatePeriodPicker.vue";
import {computed, inject, onMounted, ref, useTemplateRef, nextTick} from "vue";
import { useStore } from "vuex";
import { useI18n } from 'vue-i18n'
import AssociatedExperimentsList from "@/components/experiments/AssociatedExperimentsList.vue";
import VisualisationGraphic from "@/components/home/dashboard/VisualisationGraphic.vue";

//#region Constants & Services
const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!;
const $store = useStore();
const { t } = useI18n()

const variablesService: VariablesService = $opensilex.getService<VariablesService>('opensilex.VariablesService');
const devicesService: DevicesService = $opensilex.getService<DevicesService>('opensilex.DevicesService');
const dataService: DataService = $opensilex.getService<DataService>('opensilex.DataService');
//#endregion

//#region Props

interface Props {
  variableUri: NamedResourceDTOVariableModel;
  target: string;
  defaultStartDate?: string;
  defaultEndDate?: string;
}
const props = defineProps<Props>();
//#endregion

//#region Computed
const user = computed(() => {
  return $store.state.user;
});

const credentials = computed(() => {
  return $store.state.credentials;
});

const hasCalculatedSeries = computed(() => {
  return (calculatedDataSeries.value.length > 0);
});

const isDataForGraph = computed(() => {
  return !(dataSeries.value.length ===0 && calculatedDataSeries.value.length === 0)
});
//#endregion

//#region Data
const period = ref<Period>("week");
const graphicStartDate = ref<string>(null);
const graphicEndDate = ref<string>(null);

const variable = ref<VariableDetailsDTO>(null);

const dataSeries = ref<Array<DataSerieGetDTO>>([]);
const calculatedDataSeries = ref<Array<DataSerieGetDTO>>([]);
const availableProvenances = ref<Array<DataSimpleProvenanceGetDTO>>([]);

const medianSerie = ref<Array<DataComputedGetDTO>>([]);
const lastData = ref(null);

const isNoDataFound = ref<boolean>(false);
const isDataOutOfReach = ref<boolean>(false);
const isDataLoaded = ref<boolean>(false);
const isGraphicLoaded= ref<boolean>(true);
const isLoadAllProvToggled = ref<boolean>(false);

//#endregion

/* TODO MAX This was old vue2 code, these watchers didnt seem to be doing anything as there was never a startDate and endDate. Delete if confimed pointless
@Watch('startDate')
@Watch('endDate')
onPropertyChanged(value: string, oldValue: string) {
  this.loadSimplifiedData();
}*/

//#region Template Refs
const graphicModal = useTemplateRef<any>("graphic-modal")
const visuGraphic = useTemplateRef<InstanceType<typeof VisualisationGraphic>>("visuGraphic")
const histogramSettings = useTemplateRef<InstanceType<typeof FacilityHistogramSettings>>("histogramSettings")
//#endregion

//#region Hooks
onMounted(()=>{
  $opensilex.hideLoader();
  initDatePeriod();
  refresh();
});
//#endregion

//#region EventHandlers
function onSettingsChanged(period: Period, begin: string, end: string, selectAll: boolean) {
  graphicStartDate.value = begin;
  graphicEndDate.value = end;
  period.value = period;
  isLoadAllProvToggled.value = selectAll;
  loadAllData();
}
//#endregion

//#region Functions
function refresh() {
  variablesService
    .getVariable(props.variableUri.uri)
    .then( (http: HttpResponse<OpenSilexResponse<VariableDetailsDTO>>) => {
        if (http && http.response) {
          variable.value = http.response.result;

          loadSimplifiedData();
        }
      }
    )
}

function initDatePeriod() {
  graphicStartDate.value = props.defaultStartDate;
  graphicEndDate.value = props.defaultEndDate;
}

function loadSimplifiedData() {
  isNoDataFound.value = false;
  isDataOutOfReach.value = false;
  isDataLoaded.value = false;
  $opensilex.disableLoader();

  dataService.getDataSeriesByFacility(
    props.variableUri.uri,
    props.target,
    (props.defaultStartDate != "") ? props.defaultStartDate : undefined,
    (props.defaultEndDate != "") ? props.defaultEndDate : undefined,
    true
  )
    .then(
      (
        http: HttpResponse<OpenSilexResponse<DataVariableSeriesGetDTO>>
      ) => {
        if (http && http.response) {
          let seriesDTO: DataVariableSeriesGetDTO = http.response.result;

          if (!seriesDTO.last_data_stored) {
            isNoDataFound.value = true;
            return;
          }

          if (!seriesDTO.calculated_series.length) {
            lastData.value = seriesDTO.last_data_stored;
            isDataOutOfReach.value = true;
            isDataLoaded.value = true;
            return;
          }

          calculatedDataSeries.value = seriesDTO.calculated_series;
          availableProvenances.value = seriesDTO.provenances;

          medianSerie.value = seriesDTO.calculated_series[0].data
            .sort((a, b) => (a.date > b.date) ? 1 : -1);

          updateLastMedianData();
          isDataLoaded.value = true;
        }
      }
    );
}

function loadAllData() {
  isGraphicLoaded.value = false;
  $opensilex.disableLoader();

  dataService.getDataSeriesByFacility(
    props.variableUri.uri,
    props.target,
    (graphicStartDate.value != "") ? graphicStartDate.value : undefined,
    (graphicEndDate.value != "") ? graphicEndDate.value : undefined,
    !isLoadAllProvToggled.value
  )
    .then(
      (
        http: HttpResponse<OpenSilexResponse<DataVariableSeriesGetDTO>>
      ) => {
        if (http && http.response) {
          let seriesDTO: DataVariableSeriesGetDTO = http.response.result;

          dataSeries.value = seriesDTO.data_series;
          calculatedDataSeries.value = seriesDTO.calculated_series;

          showGraphic();
        }
      }
    );
}

function updateLastMedianData() {
  if (medianSerie.value.length === 0) {
    return;
  }

  lastData.value = {
    value: medianSerie.value[medianSerie.value.length - 1].value,
    date: medianSerie.value[medianSerie.value.length - 1].date
  };
  lastData.value = lastData.value.toPrecision(4) + " " + variable.value.unit.symbol;
}

//TODO MAX i think we no longer need this because i removed the pointless langUnwatcher in mounted
/*function beforeDestroy() {
  langUnwatcher();
}*/

function showGraphic() {
  graphicModal.value.show();
  prepareGraphic();
  isGraphicLoaded.value = true;
}

function prepareGraphic() {
  $opensilex.disableLoader();
  let promises = [];
  let promise: any;

  for (let i = 0; i < calculatedDataSeries.value.length; ++i) {
    promise = buildDataSerie(calculatedDataSeries.value[i], !i);
    promises.push(promise);
  }
  for (let i = 0; i < dataSeries.value.length; ++i) {
    promise = buildDataSerie(dataSeries.value[i], !hasCalculatedSeries);
    promises.push(promise);
  }

  Promise.all(promises)
    .then(values => {
      let series = [];
      values.forEach(serie => {
        series.push(serie);
      });

      isGraphicLoaded.value = true;

      nextTick(() => {
        $opensilex.enableLoader();
        visuGraphic.value.reload(
          series,
          variable
        );
      });
    })
    .catch(error => {
      isGraphicLoaded.value = true;
      $opensilex.errorHandler(error);
    });
}

function buildDataSerie(dataSerie, isVisible: boolean) {

  var data = dataSerie.data as Array<DataComputedGetDTO>;
  data.sort((a, b) => (a.date > b.date) ? 1 : -1);
  let dataLength = data.length;

  if (dataLength === 0){
    $opensilex.showInfoToast(
      t("component.common.search.noDataFound").toString());
  }

  if (dataLength >= 0) {
    const cleanData = HighchartsDataTransformer.transformSimpleDataForHighcharts(data, dataSerie.provenance);
    if (dataLength > $store.state.graphDataLimit) {
      $opensilex.showInfoToast(
        t("component.common.search.limitSizeMessageBasic", {count: dataLength }).toString()
      );
    }

    let prov = dataSerie.provenance;

    return {
      name: prov.name,
      data: cleanData,
      visible: isVisible
    };
  }
}

function scatter() {
  visuGraphic.value.scatter();
}

function fullscreen() {
  visuGraphic.value.fullscreen();
}

function exportPNG() {
  visuGraphic.value.exportPNG();
}
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
  color: #f1f1f1
}

</style>

