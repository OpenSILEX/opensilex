<template>
    <opensilex-Card
        ref="tilePanel"
        class="tile"
        :label="variable.name"
        icon=""
    >

      <template v-slot:body>

        <opensilex-UriListView
            label="Devices"
            :list="deviceUriList"
            :inline="true"
        >
        </opensilex-UriListView>

        <opensilex-Button
            label="Show graphic"
            icon=""
            :small="false"
            @click="prepareGraphic()"
        >
        </opensilex-Button>

        <opensilex-DataVisuGraphic
          v-if="isGraphicLoaded"
          ref="visuGraphic"
          :deviceType="false"
          class="DeviceVisualisationGraphic"
          v-bind:class ="{
            'DeviceVisualisationGraphic': false,
            'DeviceVisualisationGraphicWithoutForm': true
          }"
        ></opensilex-DataVisuGraphic>

      </template>
    </opensilex-Card>
</template>

<script lang="ts">
import {Component, Prop, Ref, Watch} from "vue-property-decorator";
import Vue from "vue";
import {DataGetDTO, DevicesService, EventGetDTO, EventsService} from "opensilex-core/index";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import HighchartsDataTransformer from "../../../models/HighchartsDataTransformer";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import {VariablesService} from "opensilex-core/api/variables.service";
import {DataService} from "opensilex-core/api/data.service";
import {DeviceGetDTO} from "opensilex-core/model/deviceGetDTO";
import {VariableDetailsDTO} from "opensilex-core/model/variableDetailsDTO";


@Component
export default class VariableVisualizationTile extends Vue {
  $opensilex: OpenSilexVuePlugin;

  get user() {
    return this.$store.state.user;
  }
  get credentials() {
    return this.$store.state.credentials;
  }

  @Prop()
  variable: VariableDetailsDTO;

  @Prop()
  devices: Array<DeviceGetDTO>;

  isGraphicLoaded = true;
  target = [] ;
  eventCreatedTime = "";

  variablesService: VariablesService;
  devicesService: DevicesService;
  eventsService: EventsService;

  @Ref("visuGraphic") readonly visuGraphic!: any;
  @Ref("annotationModalForm") readonly annotationModalForm!: any;
  @Ref("eventsModalForm") readonly eventsModalForm!: any;

  created() {
    this.variablesService = this.$opensilex.getService<VariablesService>(
        "opensilex.VariablesService"
    );
    this.devicesService = this.$opensilex.getService<DevicesService>(
        "opensilex.DevicesService"
    );
    this.eventsService = this.$opensilex.getService<EventsService>(
        "opensilex.EventsService"
    );
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

  get deviceUriList() {
    if (!this.devices) {
      return [];
    }

    return this.devices.map(device => {
      return {
        uri: device.uri,
        value: device.name,
        to: {
          path: "/device/details/" + encodeURIComponent(device.uri),
        },
      };
    });
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

  onSearch() {
    this.isGraphicLoaded = false;
    if (this.variable) {
      const datatype = this.variable.datatype.split("#")[1];
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
    }
  }

  prepareGraphic() {
      this.$opensilex.disableLoader();
      var promises = [];
      let promise;

      this.devices.forEach((device) => {
        promise = this.buildDataSerie(device.uri);
        promises.push(promise);
      });
      //promise = this.buildEventsSerie();
      //promises.push(promise);

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
          console.log(series);
          this.$nextTick(() => {
            this.$opensilex.enableLoader();
            this.visuGraphic.reload(
              series,
              this.variable
            );
          });
        })
        .catch(error => {
          this.isGraphicLoaded = true;
          this.$opensilex.errorHandler(error);
        });
  }

  buildEventsSerie() {
      return this.eventsService
        .searchEvents(
          undefined,
          undefined,
          undefined,
          this.devices[0].uri,
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
  }

  buildDataSerie(deviceUri) {
    return this.$opensilex
        .getService<DataService>("opensilex.DataService")
        .searchDataList(
            undefined, // start_date
            undefined, // end_date
            undefined, // timezone,
            undefined, // experiments
            undefined, // scientific_object
            [this.variable.uri], // variables,
            [deviceUri], // devices
            undefined, // min_confidence
            undefined, // max_confidence
            undefined,
            undefined, //this.addMetadataFilter(),
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
            name: this.variable.name,
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

