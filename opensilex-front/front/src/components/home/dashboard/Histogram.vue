<template>
  <div>
    <opensilex-Card :noFooter="true" icon="fa#chart-bar" class="histogramCard"> 
      <template v-slot:header>
        <span class="graphicTitle">
        {{ dataLocationInformations ? dataLocationInformations : t("Histogram.graphicInformations") }}
        </span>

      </template>
      
      <template v-slot:rightHeader>
        <div class="graphicOptions">
        <!-- Period badge -->
        <span v-if="period" class="badge rounded-pill greenThemeColor">
        <opensilex-Icon icon="fa#hourglass-half" /> {{ t("Histogram.period." + period) }}
        </span>


        <!-- Dropdown menu -->
        <div class="dropdown">
          <button class="btn btn-link dropdown-toggle" type="button" data-bs-toggle="dropdown" aria-expanded="false">
            <opensilex-Icon icon="fa#bars" size="lg" class="dashboardGraphicMenu" />
          </button>
          <ul class="dropdown-menu dropdown-menu-end">
            <li>
                <a class="dropdown-item" href="#" @click="scatter">
                <opensilex-Icon icon="fa#braille" />
                {{ t("Histogram.dataStyle") }}
                </a>
            </li>
            <li>
                <a class="dropdown-item" href="#" @click="fullscreen">
                <opensilex-Icon icon="fa#expand" />
                {{ t("VisualisationGraphic.fullscreen") }}
                </a>
            </li>
            <li>
                <a class="dropdown-item" href="#" @click="exportPNG">
                <opensilex-Icon icon="fa#download" />
                {{ t("VisualisationGraphic.download") }}
                </a>
            </li>
          </ul>
        </div>

        <!-- Periods & Devices selection button -->
        <opensilex-Button class="btn settingsButton" @click="histogramSettings.show()" :label="t('Histogram.settings')" icon="fa#cog" :small="true">
        </opensilex-Button>
        </div>
      </template>

      <!-- <template v-slot:body> -->
        <div class="graphicVisualisationContainer">
          <div class="d-flex justify-content-center mb-3" v-if="!isGraphicLoaded">
            <div class="spinner-border text-primary" role="status">
              <span class="visually-hidden">Loading...</span>
            </div>
          </div>

          <!-- Graphic Container -->
          <opensilex-VisualisationGraphic
            ref="visualisationGraphic"
            :deviceType="false"
            :lType="true"
            :lWidth="true"
          ></opensilex-VisualisationGraphic>
        </div> 
      <!-- </template> -->
    </opensilex-Card>

    <!-- Modal settings component -->
    <opensilex-HistogramSettings
      ref="histogramSettings"
      @update="loadData"
      v-model:period="period"
      :devicesLoaded="devices"
    ></opensilex-HistogramSettings>

  </div>
</template>

<script lang="ts">
import { ref, watchEffect, onMounted, inject, defineProps, nextTick } from "vue";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import HistogramSettings from "./HistogramSettings.vue";
import VisualisationGraphic from "./VisualisationGraphic.vue";
import { DataService } from "opensilex-core/api/data.service";
import { VariablesService } from "opensilex-core/api/variables.service";
import { DevicesService } from "opensilex-core/api/devices.service";
import { DeviceGetDTO } from "opensilex-core/index";
import Xsd from "../../../ontologies/Xsd";
import { DataGetDTO } from "opensilex-core/index";
import HighchartsDataTransformer from "../../../models/HighchartsDataTransformer";
import { useI18n } from 'vue-i18n';
import { useStore } from 'vuex';
import type { HttpResponse, OpenSilexResponse } from "opensilex-security/HttpResponse";


export default {
  components: {
    HistogramSettings,
    VisualisationGraphic,
  },
  props: {
    variableChoice: String,
  },

  setup(props) {
    const $opensilex= inject<OpenSilexVuePlugin>("$opensilex");
    const isGraphicLoaded = ref(true);
    const period = ref("day");
    const dataLocationInformations = ref("");
    const devices = ref<Array<{ label: string; id: string }>>([]);
    const deviceColorMap = ref({});
    const selectedVariable = ref(null);
    const selectedVariableName = ref("");
    const { t } = useI18n();
    const store = useStore();
    let variableChoice = props.variableChoice;
    
    const histogramSettings = ref<InstanceType<typeof HistogramSettings>>();
    const visualisationGraphic = ref<InstanceType<typeof VisualisationGraphic>>();

    const dataService = $opensilex.getService<DataService>("opensilex.DataService");
    const variablesService = $opensilex?.getService<VariablesService>("opensilex.VariablesService");
    const devicesService = $opensilex.getService<DevicesService>("opensilex.DevicesService");

    const scatter = () => visualisationGraphic.value?.scatter();
    // const fullscreen = () => visualisationGraphic.value?.fullscreen();
    // const exportPNG = () => visualisationGraphic.value?.exportPNG();


    const loadDevices = async () => {
      console.log("variableChoice pour loadDevices : ", variableChoice);

      try {
        const response = await devicesService.searchDevices(undefined, undefined, undefined, variableChoice, undefined);
        console.log("loadDevices - response devices : ", response)
        devices.value = response.response.result.map((device: DeviceGetDTO) => ({
          id: device.uri,
          label: device.name,
        }));
      } catch (error) {
        console.log("loadDevices - error - variable choice : ", variableChoice)
        console.log("loadDevices - error ", error)
        $opensilex.errorHandler(error);
      }
    };

    const loadData = async () => {
      isGraphicLoaded.value = false;

        console.log("histogram loadDevices variableChoice : ", variableChoice)

      try {
        const response = await variablesService.getVariable(variableChoice);
        console.log("histogram loadDevices Réponse API (variable):", response);
        selectedVariable.value = response.response.result;
        selectedVariableName.value = selectedVariable.value.name;
        dataLocationInformations.value = $opensilex.getConfig().dashboard.graph1.dataLocationInformations;

        const datatype = selectedVariable.value.datatype;
        console.log("Datatype récupéré :", datatype);
        if ($opensilex.checkURIs(datatype, Xsd.DECIMAL) || $opensilex.checkURIs(datatype, Xsd.INTEGER)) {
          // Ok enter in - TODO : la construction des series de données, et migration Highcharts
          buildColorsDevicesMap();
          buildSeries(devices.value, period.value);
        } else {
          isGraphicLoaded.value = true;
          $opensilex.showInfoToast("error");
        }
      } catch (error) {
        console.error("Erreur lors du chargement des données :", error);
        isGraphicLoaded.value = true;
        $opensilex.errorHandler(error);
      }
    };

    const buildColorsDevicesMap = () => {
      const colorPalette = [
        "#ca6434", "#427775", "#f2dc7c", "#0f839c", "#a45354", "#d3b0ae", "#774e42", "#776942",
        "#5c4277", "#34a0ca", "#9334ca", "#caaf34", "#ff4000", "#ff8000", "#00ffbf", "#0080ff",
        "#8000ff", "#ff0080", "#808080", "#4d0000", "#ff73ff", "#0c7340", "#563a4f", "#0d00f2"
      ];
      devices.value.forEach((device, index) => {
        deviceColorMap.value[device.id] = colorPalette[index % colorPalette.length];
      });
    };

    const buildSeries = async (devicesToDisplay, period) => {
      console.log("buildSeries")
    let promises = [];
    let promise;
    const series = [];
    let serie;
    
    promise = buildDataSeries(devicesToDisplay, period);
    promises.push(promise);

    Promise.all(promises).then(values => {
      let series = [];

      if (values[0]) {
        values[0].forEach(serie => {
          series.push(serie);
        });
      }

      if (values[1]) {
        values[1].forEach(serie => {
          series.push(serie);
        });
      }

      isGraphicLoaded.value = true;
      nextTick(() => {
        visualisationGraphic.reload(series, selectedVariable, undefined);
      });
    }
    )}





function buildDataSeries(devicesToDisplay?: any[], period?: any): Promise<any[]> {
  console.log("buildDataSeries")
  const promises: Promise<any>[] = [];

  const devicesToUse = devicesToDisplay ?? devices.value;

  devicesToUse.forEach((device) => {
    const promise = buildDataSerie(device, period);
    promises.push(promise);
  });

  return Promise.all(promises).then((values) => {
    const series: any[] = [];
    let totalData = 0;

    values.forEach((serie) => {
      if (serie !== undefined) {
        series.push(serie);
        totalData += serie.data.length;
      }
    });

    if (totalData === 0) {
      $opensilex.showInfoToast(
        t("Histogram.noDataFound") + selectedVariableName.value
      );
    }

    return series;
  });
}





    const buildDataSerie = async (device, period) => {
      console.log("passage buildDataSerie")
        const todayDate = new Date();
            
            const yesterdayDate = new Date();
            yesterdayDate.setDate(todayDate.getDate()-1);

            const weekDate = new Date();
            weekDate.setDate(todayDate.getDate()-7);

            const monthDate = new Date();
            monthDate.setMonth(todayDate.getMonth()-1);

            const yearDate = new Date();
            yearDate.setDate(todayDate.getDate()-365);

            let chosenPeriod;

            if (period === "day") {
            chosenPeriod = yesterdayDate
            }
            if (period === "week") {
            chosenPeriod = weekDate
            }
            if (period === "month") {
            chosenPeriod = monthDate
            }
            if (period === "year") {
            chosenPeriod = yearDate
            }

            return dataService
            .searchDataList(
                chosenPeriod.toISOString(),
                todayDate.toISOString(),
                undefined,
                undefined,
                undefined,
                [variableChoice],
                [device.id],
                undefined,
                undefined,
                undefined,
                undefined,
                undefined,
                ["date=asc"],
                0,
                store.state.graphDataLimit,
            )
            .then((http: HttpResponse<OpenSilexResponse<Array<DataGetDTO>>>) => {
              console.log("buildDataSerie dataService  response ", http.response.result)
                const data = http.response.result as Array<DataGetDTO>;

                let dataLength = data.length;
                if (dataLength >= 0) {
                const cleanData = HighchartsDataTransformer.transformDataForHighcharts(data, {deviceUri: device.uri});

                if (dataLength > store.state.graphDataLimit) {
                    $opensilex.showErrorToast(
                    t("Histogram.limitSizeMessageA") +
                    " " +
                    dataLength +
                    " " +
                    t("Histogram.limitSizeMessageB") +
                    device.name +
                    t("Histogram.limitSizeMessageC")
                    );
                }

                let name = device.label ? device.label : device.id
                return {
                    name: name,
                    data: cleanData,
                    visible: true,
                    color: deviceColorMap[device.id],
                    legendColor: deviceColorMap[device.id]
                };
                }
            })
            .catch(error => {
              console.log("error buildDataSerie ", error)
            });
    };

    onMounted(() => {
      loadDevices().then(loadData);
    });

    return {
      period,
      isGraphicLoaded,
      dataLocationInformations,
      histogramSettings,
      visualisationGraphic,
      scatter,
      props,
    //   fullscreen,
    //   exportPNG,
      loadData,
      t,
    };
  },
};
</script>

<style scoped>
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

.graphicVisualisationContainer {
    height: 100%;
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
    display: flex
}
</style>


<i18n>
en:
  Histogram:
    dataStyle: "Change viewing mode"
    today: Today
    weekday:
      1: Monday
      2: Tuesday
      3: Wednesday
      4: Thursday
      5: Friday
      6: Saturday
      0: Sunday
    period:
      day: Last hours
      week: "Last week"
      month: "Last month"
      6month: "Last 6 month"
      year: "Last year"
    settings: "Choice of period and devices"
    noDataFound : "No data found at the selected period for : "
    limitSizeMessageA : "There are "
    limitSizeMessageB : "data for "
    limitSizeMessageC : " .Only the 50 000 first data are displayed."
    graphicInformations: "Add informations concerning your data, location for example"

fr:
  Histogram:
    dataStyle: "Changer de mode de vue"
    today: Aujourd'hui
    weekday:
      1: Lundi
      2: Mardi
      3: Mercredi
      4: Jeudi
      5: Vendredi
      6: Samedi
      0: Dimanche
    period:
      day: "24 dernieres heures"
      week: "Derniere semaine"
      month: "Dernier mois"
      6month: "6 dernier mois"
      year: "Derniere année"
    settings: "Choix de la période et des appareils"
    noDataFound: "Pas de données pour la période choisie"
    limitSizeMessageA : "Il y a "
    limitSizeMessageB : "données pour "
    limitSizeMessageC : ".Seules les 50 000 premières valeurs sont affichées. "
    graphicInformations: "Ajoutez des informations concernant vos données, leur localisation par exemple"

</i18n>

