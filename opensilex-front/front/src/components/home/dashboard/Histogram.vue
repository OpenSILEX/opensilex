<template>
  <div>
    <opensilex-Card :no-footer="true" icon="fa#chart-bar"
      class="histogramCard"> 

      <template v-slot:header>
        <span class="graphicTitle">{{selectedVariableName }}</span>
      </template>
      
      <template v-slot:rightHeader>
        <!-- Period badge-->
        <b-badge v-if="period" pill class="greenThemeColor">
          <opensilex-Icon icon="fa#hourglass-half"/> {{ $t("Histogram.period." + period) }}
        </b-badge>

        <!-- Graphic Options menu dropdown-->
        <b-dropdown right size="lg" variant="link" toggle-class="text-decoration-none" no-caret>
          <template #button-content>
            <opensilex-Icon icon="fa#bars" size="lg" class="dashboardGraphicMenu" />
          </template>

          <b-dropdown-item href="#" @click="scatter">
            <opensilex-Icon icon="fa#braille" />
            {{ $t("Histogram.dataStyle") }}
          </b-dropdown-item>

          <b-dropdown-item href="#" @click="fullscreen">
            <opensilex-Icon icon="fa#expand" />
            {{ $t("VisualisationGraphic.fullscreen") }}
          </b-dropdown-item>

          <b-dropdown-item href="#" @click="exportPNG">
            <opensilex-Icon icon="fa#download" />
            {{ $t("VisualisationGraphic.download") }}
          </b-dropdown-item>
        </b-dropdown>

        <!-- Periods & Devices selection button -->
        <opensilex-Button
            label=Histogram.settings
            class="settingsButton"
            icon="fa#cog"
            @click="histogramSettings.show()"
        ></opensilex-Button>
      </template>

      <template v-slot:body>
        <div class="graphicVisualisationContainer">

          <div class="d-flex justify-content-center mb-3" v-if="!isGraphicLoaded">
            <b-spinner label="Loading..."></b-spinner>
          </div>
          <!--Graphic Container -->
          <opensilex-VisualisationGraphic
            ref="visualisationGraphic"
            :deviceType="false"
            :lType="true"
            :lWidth="true"
          ></opensilex-VisualisationGraphic>
        </div> 
      </template>
    </opensilex-Card>

    <!-- Modal settings component-->
    <opensilex-HistogramSettings
      ref="histogramSettings"
      @update="loadData"
      :period.sync="period"
      :devicesLoaded="devices"
    ></opensilex-HistogramSettings>
  </div>
</template>

<script lang="ts">
import {Component, Ref, Prop} from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";
import {DataService} from "opensilex-core/api/data.service";
import {DataGetDTO} from "opensilex-core/index";
import {VariablesService} from "opensilex-core/api/variables.service";
import HistogramSettings from "./HistogramSettings.vue";
import HighchartsDataTransformer from "../../../models/HighchartsDataTransformer";
import {DevicesService} from "opensilex-core/api/devices.service";
import {DeviceGetDTO} from "opensilex-core/index";
import VisualisationGraphic from "./VisualisationGraphic.vue";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import Xsd from "../../../ontologies/Xsd";


@Component
export default class Histogram extends Vue {
 $opensilex: OpenSilexVuePlugin;
  isLoaded: boolean = false;

  period: string = "day";
  dataService: DataService;
  variablesService: VariablesService;
  selectedVariable;
  selectedVariableName: string = "";
  deviceColorMap = [];
  devices : Array<{label: string; id: string}> = [] ;

  chosenPeriod = undefined;
  isGraphicLoaded: boolean = true;


  @Ref("histogramSettings") readonly histogramSettings!: HistogramSettings;
  @Ref("highcharts") readonly highcharts!: any;
  @Ref("visualisationGraphic") readonly visualisationGraphic!: VisualisationGraphic;

  @Prop()
  variableChoice: string ;

  scatter() {
    this.visualisationGraphic.scatter();
  }

  fullscreen() {
    this.visualisationGraphic.fullscreen();
  }

  exportPNG() {
    this.visualisationGraphic.exportPNG();
  }


  created() {
    this.variablesService = this.$opensilex.getService("opensilex.VariablesService");
    this.dataService = this.$opensilex.getService("opensilex.DataService");
        this.loadDevices().then(() => {
      this.loadData()
    });
}

    /**
     * called on creation
     * for each device linked to selected variable
     * push uri & name on the Devices array
     */
    loadDevices() {
      const service: DevicesService = this.$opensilex.getService("opensilex.DevicesService");
      return service.searchDevices(
          undefined,
          undefined,
          undefined,
          this.variableChoice,
          undefined,
      )
      .then((http: HttpResponse<OpenSilexResponse<Array<DeviceGetDTO>>>) => {
        this.devices=http.response.result.map((deviceDTO)=>{
          return { 
            id: deviceDTO.uri,
            label:deviceDTO.name
          }
        })
      }).catch(this.$opensilex.errorHandler);
    }
  
  
    /**
     * called on creation and on settings update
     */
    loadData(devicesToDisplay?){
  
      this.isGraphicLoaded = false;
      this.$opensilex.disableLoader();
  
      this.variablesService
      .getVariable(this.variableChoice)
      .then((http: HttpResponse<OpenSilexResponse>) => {
        this.selectedVariable = http.response.result;
        this.selectedVariableName = this.selectedVariable.name;
        const datatype = this.selectedVariable.datatype;
        if (this.$opensilex.checkURIs(datatype, Xsd.DECIMAL) || this.$opensilex.checkURIs(datatype, Xsd.INTEGER)){
          this.buildColorsDevicesMap();
          this.buildSeries(devicesToDisplay, this.period);
        } else {
          this.isGraphicLoaded = true;
          this.$opensilex.showInfoToast("error");
        }
      }).catch(error => {
        this.isGraphicLoaded = true;
        this.$opensilex.errorHandler(error)
      })
    }

    /**
     * Color palet for data series
     */
    buildColorsDevicesMap() {
    const colorPalette = [
      "#ca6434",
      "#427775",
      "#f2dc7c",
      "#0f839c",
      "#a45354",
      "#d3b0ae",
      "#774e42",
      "#776942",
      "#5c4277",
      "#34a0ca",
      "#9334ca",
      "#caaf34",
      "#ff4000",
      "#ff8000",
      "#00ffbf",
      "#0080ff",
      "#8000ff",
      "#ff0080",
      "#808080",
      "#4d0000",
      "#ff73ff",
      "#0c7340",
      "#563a4f",
      "#0d00f2"
    ];
    let index = 0;
    this.devices.forEach((element, index) => {
      this.deviceColorMap[element.id] = colorPalette[index];
      index++;
      if (index === 24) {
        index = 0;
      }
    });
  }

  buildSeries(devicesToDisplay, period) {
    let promises = [];
    let promise;
    const series = [];
    let serie;
    this.dataService = this.$opensilex.getService("opensilex.DataService");
    
    promise = this.buildDataSeries(devicesToDisplay, period);
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

      this.isGraphicLoaded = true;
      this.$nextTick(() => {
        this.visualisationGraphic.reload(series, this.selectedVariable, undefined);
      });
    });
  }

  buildDataSeries(devicesToDisplay, period) {
    let series = [],
        serie;
    let promises = [],
        promise;
      if (devicesToDisplay === undefined) {
    this.devices.forEach((device, index) => {
      promise = this.buildDataSerie(device, period);
      promises.push(promise);
    });
      }
      else {
        devicesToDisplay.forEach((device, index) => {
      promise = this.buildDataSerie(device, period);
      promises.push(promise);
    });
      }

    return Promise.all(promises).then(values => {
      let totalData = 0;
      values.forEach(serie => {
        if (serie !== undefined) {
          series.push(serie);
          totalData += serie.data.length;
        } 
      });
      if (totalData === 0) {
            this.$opensilex.showInfoToastWithoutDelay(
              this.$i18n.t("Histogram.noDataFound") +
              this.selectedVariableName
            )
      }
      return series;
    });
  }

  buildDataSerie(device, period) {
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

    return this.dataService
      .searchDataList(
        chosenPeriod.toISOString(),
        todayDate.toISOString(),
        undefined,
        undefined,
        undefined,
        [this.variableChoice],
        [device.id],
        undefined,
        undefined,
        undefined,
        undefined,
        undefined,
        ["date=asc"],
        0,
        50000,
      )
      .then((http: HttpResponse<OpenSilexResponse<Array<DataGetDTO>>>) => {
        const data = http.response.result as Array<DataGetDTO>;

        let dataLength = data.length;
        if (dataLength >= 0) {
          const cleanData = HighchartsDataTransformer.transformDataForHighcharts(data, {deviceUri: device.uri});

          if (dataLength > 50000) {
            this.$opensilex.showErrorToast(
              this.$i18n.t("Histogram.limitSizeMessageA") +
              " " +
              dataLength +
              " " +
              this.$i18n.t("Histogram.limitSizeMessageB") +
              device.name +
              this.$i18n.t("Histogram.limitSizeMessageC")
            );
          }

          let name = device.label ? device.label : device.id
          return {
            name: name,
            data: cleanData,
            visible: true,
            color: this.deviceColorMap[device.id],
            legendColor: this.deviceColorMap[device.id]
          };
        }
      })
      .catch(error => {
      });
  }
}
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
      day: "Last 24 hours"
      week: "Last week"
      month: "Last month"
      year: "Last year"
    settings: "Choice of period and devices"
    noDataFound : "No data found at the selected period for : "
    limitSizeMessageA : "There are "
    limitSizeMessageB : "data for "
    limitSizeMessageC : " .Only the 50 000 first data are displayed."

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
      year: "Derniere année"
    settings: "Choix de la période et des appareils"
    noDataFound: "Pas de données pour la période choisie"
    limitSizeMessageA : "Il y a "
    limitSizeMessageB : "données pour "
    limitSizeMessageC : ".Seules les 50 000 premières valeurs sont affichées. "

</i18n>
