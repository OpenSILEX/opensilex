<template>
  <div ref="page">

    <opensilex-PageContent class="pagecontent">
      <!-- Toggle Sidebar-->
      <div class="searchMenuContainer"
          v-on:click="searchFiltersToggle = !searchFiltersToggle"
          :title="searchFiltersPannel()">
        <div class="searchMenuIcon">
          <i class="icon ik ik-search"></i>
        </div>
      </div>

        <!--Form-->
      <Transition>
        <div v-show="searchFiltersToggle">
          <opensilex-DeviceVisualizationForm
            ref="deviceVisualizationForm"
            :device="device"
            @search="onSearch"
          ></opensilex-DeviceVisualizationForm>
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
        :elementName="elementName"
        :graphicTitle="device"
        @addEventIsClicked="showAddEventComponent"
        @dataAnnotationIsClicked="showAnnotationForm"
        class="DeviceVisualisationGraphic"
        v-bind:class ="{
          'DeviceVisualisationGraphic': searchFiltersToggle,
          'DeviceVisualisationGraphicWithoutForm': !searchFiltersToggle
        }"
      ></opensilex-DataVisuGraphic>

      <opensilex-AnnotationModalForm ref="annotationModalForm" @onCreate="onAnnotationCreated"></opensilex-AnnotationModalForm>

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
import {Component, Prop, Ref, Watch} from "vue-property-decorator";
import Vue from "vue";
import {DataGetDTO, DevicesService, EventGetDTO, EventsService} from "opensilex-core/index";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import HighchartsDataTransformer from "../../models/HighchartsDataTransformer";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {VariablesService} from "opensilex-core/api/variables.service";
import {DataService} from "opensilex-core/api/data.service";

@Component
export default class DeviceVisualizationTab extends Vue {
  $opensilex: OpenSilexVuePlugin;

  get user() {
    return this.$store.state.user;
  }
  get credentials() {
    return this.$store.state.credentials;
  }

  @Prop()
  device;

  isGraphicLoaded = true;
  target = [] ;
  eventCreatedTime = "";

  form;
  selectedVariable;
  devicesService: DevicesService;
  eventsService: EventsService;
  searchFiltersToggle: boolean = true;
  @Ref("page") readonly page!: any;
  @Ref("visuGraphic") readonly visuGraphic!: any;
  @Ref("annotationModalForm") readonly annotationModalForm!: any;
  @Ref("eventsModalForm") readonly eventsModalForm!: any;
  @Ref("deviceVisualizationForm") readonly deviceVisualizationForm!: any;

  @Prop()
  elementName;

  created() {
    if (this.device == null) {
      this.device = decodeURIComponent(this.$route.params.uri);
    }

    this.devicesService = this.$opensilex.getService(
      "opensilex.DevicesService"
    );
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

  // simulate window resizing to resize the graphic when the filter panel display changes
  @Watch("searchFiltersToggle")
  onSearchFilterToggleChange(){
    this.$nextTick(()=> { 
      window.dispatchEvent(new Event('resize'));
    })  
  }

  beforeDestroy() {
    this.langUnwatcher();
  }

  onAnnotationCreated() {
    this.visuGraphic.updateDataAnnotations();
  }

  onEventCreated() {
    this.prepareGraphic();
    this.deviceVisualizationForm.getEvents();
  }

  showAddEventComponent(time) {
    this.target = this.device;
    this.eventCreatedTime = time;
    this.eventsModalForm.showCreateForm();
  }

  showAnnotationForm(target) {
    this.annotationModalForm.showCreateForm([target]);
  }

  onSearch(form) {
    this.searchFiltersToggle = !this.searchFiltersToggle
    this.isGraphicLoaded = false;
    if (form.variable) {
      this.form = form;
      this.$opensilex.disableLoader();
      this.$opensilex
        .getService<VariablesService>("opensilex.VariablesService")
        .getVariable(form.variable)
        .then((http: HttpResponse<OpenSilexResponse>) => {
          this.selectedVariable = http.response.result;
          const datatype = this.selectedVariable.datatype.split("#")[1];
          if (datatype == "decimal" || datatype == "integer") {
            this.prepareGraphic();
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
              this.form
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
          this.device,
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
                fillColor: eventTypesColorArray[element.rdf_type]
              };

              cleanEventsData.push(toAdd);
            });
            return {
              type: "flags",
              allowOverlapX: false,
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
    return this.$opensilex
        .getService<DataService>("opensilex.DataService")
        .searchDataList(
            this.form.startDate != undefined && this.form.startDate != ""
                ? this.form.startDate
                : undefined, // start_date
            this.form.endDate != undefined && this.form.endDate != ""
                ? this.form.endDate
                : undefined, // end_date
            undefined, // timezone,
            undefined, // experiments
            undefined, // scientific_object
            [this.form.variable], // variables,
            [this.device], // devices
            undefined, // min_confidence
            undefined, // max_confidence
            this.form.provenance ? [this.form.provenance] : undefined,
            undefined, //this.addMetadataFilter(),
            undefined,  // operators
            ["date=asc"], //order by
            0,
            50000
        )
      .then((http: HttpResponse<OpenSilexResponse<Array<DataGetDTO>>>) => {
        const data = http.response.result as Array<DataGetDTO>;
        let dataLength = data.length;

        if (dataLength === 0){
          this.$opensilex.showInfoToast(
          this.$t("component.common.search.noDataFound").toString());
        }

        if (dataLength >= 0) {
          const cleanData = HighchartsDataTransformer.transformDataForHighcharts(data);
          if (dataLength > 50000) {
            this.$opensilex.showInfoToast(
              this.$i18n.t("DeviceDataTab.limitSizeMessageA") +
                " " +
                dataLength +
                " " +
                this.$i18n.t("DeviceDataTab.limitSizeMessageB")
            );
          }

          return {
            name: this.selectedVariable.name,
            data: cleanData,
            visible: true
          };
        }
      });
  }

  searchFiltersPannel() {
    return this.$t("searchfilter.label")
  }
}
</script>

<style scoped lang="scss">

.DeviceVisualisationGraphic {
  min-width: calc(100% - 450px);
  max-width: calc(100vw - 380px);
}

.DeviceVisualisationGraphicWithoutForm{
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
        limitSizeMessageA : "There are "
        limitSizeMessageB : " data .Only the 50 000 first data are displayed."

fr:
    DeviceDataTab:
        visualization: Visualisation
        data: Données
        add: Ajouter des données
        datatypeMessageA:  le type de donnée de la variable est
        datatypeMessageB: "Pour le moment, seuls les types decimal ou entier sont acceptés "
        limitSizeMessageA : "Il y a "
        limitSizeMessageB : " données .Seules les 50 000 premières valeurs sont affichées. "

</i18n>

