<template>
  <div ref="page">
    <opensilex-ScientificObjectVisualizationForm
      ref="scientificObjectVisualizationForm"
      :scientificObject="scientificObject"
      @search="onSearch"
    ></opensilex-ScientificObjectVisualizationForm>

    <div class="d-flex justify-content-center mb-3" v-if="!isGraphicLoaded">
      <b-spinner label="Loading..."></b-spinner>
    </div>

    <opensilex-DataVisuGraphic
      v-if="isGraphicLoaded"
      ref="visuGraphic"
      @addEventIsClicked="showAddEventComponent"
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
import { Component, Ref, Prop } from "vue-property-decorator";
import Vue from "vue";
import moment from "moment-timezone";
import Highcharts from "highcharts";
import {
  DataService,
  DataGetDTO,
  ProvenanceGetDTO,
  AnnotationsService,
  AnnotationGetDTO,
  EventsService,
  EventCreationDTO,
  EventDetailsDTO,
  EventGetDTO,
  EventUpdateDTO
} from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";
@Component
export default class ScientificObjectVisualizationTab extends Vue {
  $opensilex: any;

  @Prop()
  modificationCredentialId;

  get user() {
    return this.$store.state.user;
  }
  get credentials() {
    return this.$store.state.credentials;
  }

  @Prop()
  scientificObject;

  isGraphicLoaded = true;
  target = [];
  eventCreatedTime = "";

  form;
  selectedVariable;
  dataService: DataService;
  annotationService: AnnotationsService;
  eventsService: EventsService;
  @Ref("page") readonly page!: any;
  @Ref("visuGraphic") readonly visuGraphic!: any;
  @Ref("annotationModalForm") readonly annotationModalForm!: any;
  @Ref("eventsModalForm") readonly eventsModalForm!: any;
  @Ref("scientificObjectVisualizationForm") readonly scientificObjectVisualizationForm!: any;

  created() {
    this.dataService = this.$opensilex.getService("opensilex.DataService");
    this.eventsService = this.$opensilex.getService("opensilex.EventsService");
  }

  private langUnwatcher;
  mounted() {
    this.langUnwatcher = this.$store.watch(
      () => this.$store.getters.language,
      lang => {
        this.prepareGraphic();
      }
    );
  }

  onAnnotationCreated() {
    this.visuGraphic.updateDataAnnotations();
  }

  onEventCreated(){
    this.prepareGraphic();
    this.scientificObjectVisualizationForm.getEvents();
  }

  beforeDestroy() {
    this.langUnwatcher();
  }

  showAddEventComponent(time) {
    this.target = this.scientificObject;
    this.eventCreatedTime = time;
    this.eventsModalForm.showCreateForm();
  }

  showAnnotationForm(target) {
    this.annotationModalForm.showCreateForm([target]);
  }

  onSearch(form) {
    this.isGraphicLoaded = false;
    if (form.variable) {
      this.form = form;
      this.$opensilex
        .getService("opensilex.VariablesService")
        .getVariable(form.variable)
        .then((http: HttpResponse<OpenSilexResponse>) => {
          this.selectedVariable = http.response.result;
          const datatype = this.selectedVariable.datatype.split("#")[1];
          if (datatype == "decimal" || datatype == "integer") {
            this.prepareGraphic();
          } else {
            this.isGraphicLoaded = true;
            this.$opensilex.showInfoToast(
              this.$i18n.t(
                "ScientificObjectVisualizationTab.datatypeMessageA"
              ) +
                " " +
                datatype +
                " " +
                this.$i18n.t(
                  "ScientificObjectVisualizationTab.datatypeMessageB"
                )
            );
          }
        })
        .catch(error => {
          this.$opensilex.errorHandler(error);
        });
    }
  }

  prepareGraphic() {
    if (this.form) {
      
      this.$opensilex.disableLoader();
      var promises = [];
      let promise;
      promise = this.buildDataSerie();
      promises.push(promise);
      promise = this.buildEventsSerie();
      promises.push(promise);

      Promise.all(promises)
        .then(values => {
          this.isGraphicLoaded = true;
          let series = [];

          if (values[0]) {
            series.push(values[0]);
          }
          if (values[1]) {
            series.push(values[1]);
          }
          this.$nextTick(() => {
            this.$opensilex.enableLoader();
            this.visuGraphic.reload(
              series,
              this.selectedVariable,
              this.form.showEvents
            );
          });
        })
        .catch(error => {
          this.isGraphicLoaded = true;
          this.$opensilex.errorHandler(error);
        });
    }
  }

  buildEventsSerie() {
    if (this.form && this.form.showEvents) {
      return this.eventsService
        .searchEvents(
          undefined,
          this.form.startDate != undefined && this.form.startDate != ""
            ? this.form.startDate
            : undefined,
          this.form.endDate != undefined && this.form.endDate != ""
            ? this.form.endDate
            : undefined,
          this.scientificObject,
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

            let eventTypesColorArray = [];
            const colorPalette = [
              "#ca6434 ",
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
              "#caaf34"
            ];
            let index = 0;

            events.forEach(element => {
              if (!eventTypesColorArray[element.rdf_type]) {
                eventTypesColorArray[element.rdf_type] = colorPalette[index];
                index++;
                if (index === 12) {
                  index = 0;
                }
              }
              label = element.rdf_type_name
                ? element.rdf_type_name
                : element.rdf_type;
              if (element.start != null) {
                let endTime = element.end ? element.end : "en cours..";
                label = label + "(End: " + endTime + ")";
              }
              // if (element.end) {
              //   if (element.is_instant) {
              //     title = label;
              //   } else {
              //     title = label + "(End)";
              //   }
              // }
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
                fillColor: eventTypesColorArray[element.rdf_type]
              };

              cleanEventsData.push(toAdd);
            });
            return {
              type: "flags",
              allowOverlapX: false, // when flags have same datetimes
              name: "Events",
              lineWidth: 1,
              yAxis: 1,
              data: cleanEventsData,
              style: {
                // text style
                color: "white"
              }
            };
          } else {
            return undefined;
          }
        });
    } else {
      return null;
    }
  }

  buildDataSerie() {
    if (this.form) {
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
          [this.scientificObject],
          [this.form.variable],
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
            const cleanData = this.dataTransforme(data);
            if (dataLength > 50000) {
              this.$opensilex.showInfoToast(
                this.$i18n.t(
                  "ScientificObjectVisualizationTab.limitSizeMessageA"
                ) +
                  " " +
                  dataLength +
                  " " +
                  this.$i18n.t(
                    "ScientificObjectVisualizationTab.limitSizeMessageB"
                  )
              );
            }

            return {
              name: this.selectedVariable.name,
              data: cleanData,
              yAxis: 0,
              visible: true
            };
          }
        });
    } else {
      return null;
    }
  }

  // addMetadataFilter() {
  //   let metadata = undefined;
  //   if (
  //     this.form.metadataKey != undefined &&
  //     this.form.metadataKey != "" &&
  //     this.form.metadataValue != undefined &&
  //     this.form.metadataValue != ""
  //   ) {
  //     metadata =
  //       '{"' + this.form.metadataKey + '":"' + this.form.metadataValue + '"}';
  //     return metadata;
  //   }
  // }

  // keep only date/value/uriprovenance properties
  dataTransforme(data) {
    let toAdd,
      cleanData = [];

    data.forEach(element => {
      let stringDateWithoutUTC =
        moment.parseZone(element.date).format("YYYY-MM-DDTHH:mm:ss") + "+00:00";
      let dateWithoutUTC = moment(stringDateWithoutUTC).valueOf();
      let highchartsDate = Highcharts.dateFormat(
        "%Y-%m-%dT%H:%M:%S",
        dateWithoutUTC
      );
      let offset = moment.parseZone(element.date).format("Z");
      let stringDate = moment.parseZone(element.date).format("YYYY-MM-DDTHH:mm:ss") + offset;
      toAdd = {
        x: dateWithoutUTC,
        y: element.value,
        offset: offset,
        dateWithOffset: stringDate,
        provenanceUri: element.provenance.uri,
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
    ScientificObjectVisualizationTab:
        visualization: Visualization
        data: Data
        add: Add data
        datatypeMessageA: The variable datatype is
        datatypeMessageB: At this time only decimal or integer are accepted
        limitSizeMessageA : "There are "
        limitSizeMessageB : " data .Only the 50 000 first data are displayed."

fr:
    ScientificObjectVisualizationTab:
        visualization: Visualisation
        data: Données
        add: Ajouter des données
        datatypeMessageA:  le type de donnée de la variable est
        datatypeMessageB: Pour le moment, seuls les types decimal ou entier sont acceptés
        limitSizeMessageA : "Il y a "
        limitSizeMessageB : " données .Seules les 50 000 premières valeurs sont affichées. "
</i18n>

