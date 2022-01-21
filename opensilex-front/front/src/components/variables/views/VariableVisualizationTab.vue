<template>
  <div ref="page">
    <opensilex-VariableVisualizationForm
        ref="variableVisualizationForm"
        :variable="variable"
        @search="onSearch"
        @update="onUpdate"
    ></opensilex-VariableVisualizationForm>

    <div class="d-flex justify-content-center mb-3" v-if="!isGraphicLoaded">
      <b-spinner label="Loading..."></b-spinner>
    </div>

    <opensilex-DataVisuGraphic
        v-if="isGraphicLoaded"
        ref="visuGraphic"
        :deviceType="false"
        :activateContextMenuShow="false"
    ></opensilex-DataVisuGraphic>
  </div>
</template>

<script lang="ts">
import {Component, Ref, Prop} from "vue-property-decorator";
import Vue from "vue";
import moment from "moment-timezone";
import Highcharts from "highcharts";
// @ts-ignore
import {
  DevicesService,
  DataGetDTO,
} from "opensilex-core/index";
// @ts-ignore
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import VariableForm from "../form/VariableForm.vue";
import {VariableDetailsDTO} from "opensilex-core/model/variableDetailsDTO";
import {DataService} from "opensilex-core/api/data.service";
import {DeviceGetDTO} from "opensilex-core/model/deviceGetDTO";

@Component
export default class VariableVisualizationTab extends Vue {
  $opensilex: any;
  dataService: DataService;
  service: DevicesService;

  @Prop()
  modificationCredentialId;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  @Prop()
  variable;

  isGraphicLoaded: boolean = true;

  form;
  selectedVariable;
  devicesService: DevicesService;
  deviceColorMap = [];

  @Ref("page") readonly page!: any;
  @Ref("visuGraphic") readonly visuGraphic!: any;
  @Ref("variableVisualizationForm") readonly variableVisualizationForm!: any;


  created() {
    this.dataService = this.$opensilex.getService("opensilex.DataService");
    this.$opensilex.disableLoader();
  }

  onUpdate(form) {
    this.form = form;
    const datatype = this.selectedVariable.datatype.split("#")[1];
    if (datatype == "decimal" || datatype == "integer") {
      this.isGraphicLoaded = false;
      this.$opensilex.enableLoader();
      this.loadSeries();
    } else {
      this.isGraphicLoaded = false;
      this.$opensilex.showInfoToast(
          this.$i18n.t("ExperimentDataVisuView.datatypeMessageA") +
          " " +
          datatype +
          " " +
          this.$i18n.t("ExperimentDataVisuView.datatypeMessageB")
      );
    }
  }

  private langUnwatcher;

  mounted() {
    this.langUnwatcher = this.$store.watch(
        () => this.$store.getters.language,
        lang => {
          if (this.isGraphicLoaded) {
            this.onUpdate(this.form);
          }
        }
    );
  }

  beforeDestroy() {
    this.langUnwatcher();
  }

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
    this.form.device.forEach((element, index) => {
      this.deviceColorMap[element] = colorPalette[index];
      index++;
      if (index === 24) {
        index = 0;
      }
    });
  }

  onSearch(form) {
    this.isGraphicLoaded = false;
    if (this.variable) {
      this.form = form;
      this.isGraphicLoaded = false;
      this.$opensilex.disableLoader();
      this.$opensilex
          .getService("opensilex.VariablesService")
          .getVariable(this.variable)
          .then((http: HttpResponse<OpenSilexResponse>) => {
            this.selectedVariable = http.response.result;
            const datatype = this.selectedVariable.datatype.split("#")[1];
            if (datatype == "decimal" || datatype == "integer") {
              this.buildColorsDevicesMap();
              this.loadSeries();
            } else {

              this.isGraphicLoaded = true;
              this.$opensilex.showInfoToast(
                  this.$i18n.t("DeviceDataTab.datatypeMessageA") +
                  " " +
                  datatype +
                  " " +
                  this.$i18n.t("DeviceDataTab.datatypeMessageB")
              );
            }
          })
          .catch(error => {
            this.isGraphicLoaded = true;
            this.$opensilex.errorHandler(error);
          });
    }
  }

  loadSeries() {
    if (this.variable) {
      this.buildSeries();
    }
  }

  buildSeries() {
    var promise;
    this.dataService = this.$opensilex.getService("opensilex.DataService");

    this.$opensilex.disableLoader();
    promise = this.buildDataSeries();

    promise.then(values => {
      let series = [];

      if (values) {
        values.forEach(serie => {
          series.push(serie);
        });
      }

      this.isGraphicLoaded = true;
      this.$nextTick(() => {
        this.visuGraphic.reload(series, this.selectedVariable, false);
      });
    });
  }


  buildDataSeries() {
    let series = [];
    let promises = [],
        promise;
    this.form.device.forEach((element, index) => {
      promise = this.buildDataSerie(element);
      promises.push(promise);
    });

    return Promise.all(promises).then(values => {
      values.forEach(serie => {
        if (serie !== undefined) {
          series.push(serie);
        }
      });
      return series;
    });
  }


  buildDataSerie(concernedItem) {
    return this.$opensilex
        .getService("opensilex.DevicesService")
        .getDevice(concernedItem)
        .then((http: HttpResponse<OpenSilexResponse<DeviceGetDTO>>) => {
          let deviceInfo = http.response.result;
          return this.dataService
              .searchDataList(
                  this.form.startDate != undefined && this.form.startDate != ""
                      ? this.form.startDate
                      : undefined,
                  this.form.endDate != undefined && this.form.endDate != ""
                      ? this.form.endDate
                      : undefined,
                  undefined,
                  undefined,
                  undefined,
                  [this.variable],
                  this.form.device ? [concernedItem] : undefined,
                  undefined,
                  undefined,
                  undefined,
                  undefined, //this.addMetadataFilter(),
                  ["date=asc"],
                  0,
                  50000
              )
              .then((http: HttpResponse<OpenSilexResponse<Array<DataGetDTO>>>) => {
                const data = http.response.result as Array<DataGetDTO>;
                let dataLength = data.length;
                if (dataLength > 0) {
                  const cleanData = this.dataTransforme(data, concernedItem);
                  if (dataLength > 50000) {
                    this.$opensilex.showInfoToast(
                        this.$i18n.t("ExperimentDataVisuView.limitSizeMessageA") +
                        " " +
                        dataLength +
                        " " +
                        this.$i18n.t("ExperimentDataVisuView.limitSizeMessageB") +
                        concernedItem +
                        this.$i18n.t("ExperimentDataVisuView.limitSizeMessageC")
                    );
                  }

                  let name = deviceInfo.name ? deviceInfo.name : concernedItem
                  return {
                    name: name,
                    data: cleanData,
                    visible: true,
                    color: this.deviceColorMap[concernedItem],
                    legendColor: this.deviceColorMap[concernedItem]
                  };
                }
              })
              .catch(error => {
              });
        })
        .catch(this.$opensilex.errorHandler);
  }


  // keep only date/value/provenanceUri properties
  dataTransforme(data, concernedItem) {
    let toAdd,
        cleanData = [];

    data.forEach(element => {
      let stringDateWithoutUTC =
          moment.parseZone(element.date).format("YYYYMMDD HHmmss") + "+00:00";
      let dateWithoutUTC = moment(stringDateWithoutUTC).valueOf();
      let highchartsDate = Highcharts.dateFormat(
          "%Y-%m-%dT%H:%M:%S",
          dateWithoutUTC
      );
      let offset = moment.parseZone(element.date).format("Z");
      toAdd = {
        x: dateWithoutUTC,
        y: element.value,
        offset: offset,
        dateWithOffset: highchartsDate + offset,
        provenanceUri: element.provenance.uri,
        deviceUri: concernedItem, //concernedItem,
        data: element
      };
      cleanData.push(toAdd);
    });

    return cleanData;
  }

  timestampToUTC(time) {
    // var day = moment.unix(time).utc().format();
    var day = Highcharts.dateFormat("%Y-%m-%dT%H:%M:%S+0000", time);
    return day;
  }
}
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  DeviceDataTab:
    visualization: Visualization
    data: Data
    add: Add data
    datatypeMessageA: The variable datatype is
    datatypeMessageB: "At this time only decimal or integer are accepted"
    limitSizeMessageA: "There are "
    limitSizeMessageB: " data .Only the 50 000 first data are displayed."

fr:
  DeviceDataTab:
    visualization: Visualisation
    data: Données
    add: Ajouter des données
    datatypeMessageA: le type de donnée de la variable est
    datatypeMessageB: "Pour le moment, seuls les types decimal ou entier sont acceptés "
    limitSizeMessageA: "Il y a "
    limitSizeMessageB: " données .Seules les 50 000 premières valeurs sont affichées. "


</i18n>

