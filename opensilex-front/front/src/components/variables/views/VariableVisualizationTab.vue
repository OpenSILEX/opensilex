<template>
  <div ref="page" class="page">
    <opensilex-PageContent class="pagecontent">

      <!-- Toggle Sidebar-->
      <div class="searchMenuContainer"
        v-on:click="SearchFiltersToggle = !SearchFiltersToggle"
        :title="searchFiltersPannel()"
      >
        <div class="searchMenuIcon">
          <i class="icon ik ik-search"></i>
        </div>
      </div>
      <Transition>
        <div v-show="SearchFiltersToggle">
          <!--Form-->
          <opensilex-VariableVisualizationForm
              ref="variableVisualizationForm"
              :variable="variable"
              :devices="devicesURI"
              @search="onSearch"
              @update="onUpdate"
          ></opensilex-VariableVisualizationForm>
        </div>
      </Transition>

      <div class="d-flex justify-content-center mb-3" v-if="!isGraphicLoaded">
        <b-spinner label="Loading..."></b-spinner>
      </div>

      <!--Visualisation-->
      <opensilex-DataVisuGraphic
          v-if="isGraphicLoaded"
          ref="visuGraphic"
          :deviceType="false"
          :lType="true"
          :lWidth="true"
          @addEventIsClicked="showEventForm"
          @dataAnnotationIsClicked="showAnnotationForm"
          class="VariableVisualisationGraphic"
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
    </opensilex-PageContent>
  </div>
</template>

<script lang="ts">
import {Component, Prop, Ref} from "vue-property-decorator";
import Vue from "vue";
import {DataGetDTO, DevicesService, EventGetDTO, EventsService,} from "opensilex-core/index";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import {DataService} from "opensilex-core/api/data.service";
import HighchartsDataTransformer from "../../../models/HighchartsDataTransformer";
import DataVisuGraphic from "../../visualization/DataVisuGraphic.vue";

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
  SearchFiltersToggle: boolean = true;
  @Ref("page") readonly page!: any;
  @Ref("visuGraphic") readonly visuGraphic!: DataVisuGraphic;
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
    this.SearchFiltersToggle = !this.SearchFiltersToggle;
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
              let timestamp;
              if (element.start != null) {
                timestamp = new Date(element.start).getTime();
              } else {
                timestamp = new Date(element.end).getTime();
              }

              toAdd = {
                x: timestamp,
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
            const cleanData = HighchartsDataTransformer.transformDataForHighcharts(data, {deviceUri: concernedItem.uri});
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

  searchFiltersPannel() {
    return this.$t("searchfilter.label")
  }
}
</script>

<style scoped lang="scss">
.page {
  margin-top : 20px;
}

.VariableVisualisationGraphic{
  min-width: 100%;
  max-width: 100vw;
}
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

