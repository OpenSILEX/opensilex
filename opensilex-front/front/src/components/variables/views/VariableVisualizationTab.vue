<template>
  <div ref="page">
    <opensilex-VariableVisualizationForm
        ref="variableVisualizationForm"
        :variable="variable"
        :devices="devicesURI"
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
        :lType="true"
        :lWidth="true"
        @addEventIsClicked="showEventForm"
        @dataAnnotationIsClicked="showAnnotationForm"
    ></opensilex-DataVisuGraphic>

    <opensilex-AnnotationModalForm
        ref="annotationModalForm"
        @onCreate="onAnnotationCreated"
    ></opensilex-AnnotationModalForm>

    <opensilex-EventModalForm
        ref="eventsModalForm"
        :target="target"
        :eventCreatedTime="eventCreatedTime"
        @onCreate="onEventCreated"
    ></opensilex-EventModalForm>
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
  EventsService,
  EventGetDTO,
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
  target = [];
  eventCreatedTime = "";

  form;
  selectedVariable;
  devicesInfo = [];
  devicesService: DevicesService;
  eventsService: EventsService;
  deviceColorMap = [];
  @Ref("page") readonly page!: any;
  @Ref("visuGraphic") readonly visuGraphic!: any;
  @Ref("annotationModalForm") readonly annotationModalForm!: any;
  @Ref("eventsModalForm") readonly eventsModalForm!: any;
  @Ref("variableVisualizationForm") readonly variableVisualizationForm!: any;

  get devicesURI() {
    return this.devicesInfo.map(dev => {
      return dev.uri;
    });
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

  onAnnotationCreated() {
    this.visuGraphic.updateDataAnnotations();
  }

  onEventCreated() {
    this.onUpdate(this.form)
    this.variableVisualizationForm.getTotalEventsCount();
  }

  showEventForm(value) {
    this.target = value.target;
    this.eventCreatedTime = value;
    this.eventsModalForm.showCreateForm();
  }

  showAnnotationForm(target) {
    this.annotationModalForm.showCreateForm([target]);
  }

  created() {
    this.dataService = this.$opensilex.getService("opensilex.DataService");
    this.eventsService = this.$opensilex.getService("opensilex.EventsService");
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
      return this.$opensilex
          .getService("opensilex.DevicesService")
          .getDeviceByUris(this.form.device)
          .then(http => {
            this.devicesInfo = http.response.result;
            this.buildSeries();
          })

    }
  }

  buildSeries() {
    var promises = [];
    var promise;
    const series = [];
    let serie;
    this.dataService = this.$opensilex.getService("opensilex.DataService");

    this.$opensilex.disableLoader();
    promise = this.buildEventsSeries();
    promises.push(promise);
    promise = this.buildDataSeries();
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
        this.visuGraphic.reload(series, this.selectedVariable, this.form);
      });
    });
  }

  buildDataSeries() {
    let series = [],
        serie;
    let promises = [],
        promise;
    this.devicesInfo.forEach((device, index) => {
      promise = this.buildDataSerie(device);
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

  buildEventsSeries() {
    if (this.form && this.form.showEvents) {
      let series = [],
          serie;
      let promises = [],
          promise;
      this.devicesInfo.forEach((device, index) => {
        promise = this.buildEventsSerie(device);
        promises.push(promise);
      })
      return Promise.all(promises).then(values => {
        values.forEach(serie => {
          if (serie !== undefined) {
            series.push(serie);
          }
        });
        return series;
      });
    } else {
      return null;
    }
  }

  buildEventsSerie(concernedItem) {
    return this.eventsService
        .searchEvents(
            undefined,
            this.form.startDate != undefined && this.form.startDate != ""
                ? this.form.startDate
                : undefined,
            this.form.endDate != undefined && this.form.endDate != ""
                ? this.form.endDate
                : undefined,
            concernedItem.uri,
            undefined,
            undefined,
            0,
            0
        )
        .then((http: HttpResponse<OpenSilexResponse<Array<EventGetDTO>>>) => {
          const events = http.response.result as Array<EventGetDTO>;
          if (events.length > 0) {
            const cleanEventsData = [];
            let convertedDate, toAdd, label, title;

            events.forEach(element => {
              label = element.rdf_type_name
                  ? element.rdf_type_name
                  : element.rdf_type;
              if (element.start != null) {
                let endTime = element.end ? element.end : "en cours..";
                label = label + "(End: " + endTime + ")";
              }
              title = label.charAt(0).toUpperCase();
              let stringDateWithoutUTC;
              if (element.start != null) {
                stringDateWithoutUTC =
                    moment.parseZone(element.start).format("YYYYMMDD HHmmss") +
                    "+00:00";
              } else {
                stringDateWithoutUTC =
                    moment.parseZone(element.end).format("YYYYMMDD HHmmss") +
                    "+00:00";
              }

              let dateWithoutUTC = moment(stringDateWithoutUTC).valueOf();
              toAdd = {
                x: dateWithoutUTC,
                title: title,
                text: label,
                eventUri: element.uri,
              };

              cleanEventsData.push(toAdd);
            });
            let name = concernedItem.name ? concernedItem.name : concernedItem.uri
            return {
              type: "flags",
              allowOverlapX: false,
              name: "Events ->" + name,
              lineWidth: 1,
              yAxis: 1,
              data: cleanEventsData,
              style: {
                // text style
                color: "white"
              },
              fillColor: this.deviceColorMap[concernedItem.uri],
              legendColor: this.deviceColorMap[concernedItem.uri]

            };
          } else {
            return undefined;
          }
        });
  }

  buildDataSerie(concernedItem) {
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
            this.form.device ? [concernedItem.uri] : undefined,
            undefined,
            undefined,
            this.form.provenance ? [this.form.provenance] : undefined,
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
                  concernedItem.name +
                  this.$i18n.t("ExperimentDataVisuView.limitSizeMessageC")
              );
            }

            let name = concernedItem.name ? concernedItem.name : concernedItem.uri
            return {
              name: name,
              data: cleanData,
              visible: true,
              color: this.deviceColorMap[concernedItem.uri],
              legendColor: this.deviceColorMap[concernedItem.uri]
            };
          }
        })
        .catch(error => {
        });
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
        deviceUri: concernedItem.uri, //concernedItem,
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

