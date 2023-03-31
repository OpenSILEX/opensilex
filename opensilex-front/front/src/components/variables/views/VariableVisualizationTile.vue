<template>
  <opensilex-Overlay :show="!isDataLoaded && !isNoDataFound">
    <opensilex-Card
        ref="tilePanel"
        :label="variableUri.name"
        icon=""
    >

      <template v-slot:body>

        <opensilex-TextView v-if="isNoDataFound"
                            id="no-data-text"
            :label="$t('FacilityAssociatedDevices.no-data')">
        </opensilex-TextView>

        <div
            id="data-infos"
            v-if="isDataLoaded">
          <opensilex-TextView
              style="font-size: xxx-large; margin-bottom: 0;"
              v-on:click.native="showGraphic"
              :value="lastMedianData.value + ' ' + variable.unit.symbol">
          </opensilex-TextView>
          <opensilex-DateView
            :value="lastMedianData.date"
            :isDateTime="true"
            :useLocaleFormat="true"
            :dateTimeFormatOptions="{ dateStyle: 'short', timeStyle: 'long' }">
          </opensilex-DateView>
        </div>

        <!--
        <opensilex-Button
            id="btn-show"
            label="Show graphic"
            icon=""
            :small="false"
            @click="prepareGraphic()"
        >
        </opensilex-Button>
        -->

        <b-modal
            ref="graphic-modal"
            size="xl"
            :title="variableUri.name"
        >
          <opensilex-DataVisuGraphic
            v-if="isGraphicLoaded"
            id="graphic"
            ref="visuGraphic"
            :deviceType="false"
            :lType="true"
            :lWidth="2"
            class="DeviceVisualisationGraphic"
            v-bind:class ="{
              'DeviceVisualisationGraphic': false,
              'DeviceVisualisationGraphicWithoutForm': true
            }"
          ></opensilex-DataVisuGraphic>
        </b-modal>

      </template>
    </opensilex-Card>
  </opensilex-Overlay>
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
import {VariableDetailsDTO} from "opensilex-core/model/variableDetailsDTO";
import {DataSerieGetDTO} from "opensilex-core/model/dataSerieGetDTO";
import {DataVariableSeriesGetDTO} from "opensilex-core/model/dataVariableSeriesGetDTO";
import {NamedResourceDTOVariableModel} from "opensilex-core/model/namedResourceDTOVariableModel";


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
  variableUri: NamedResourceDTOVariableModel;

  @Prop()
  target: string;

  variable: VariableDetailsDTO;

  dataSeries: Array<DataSerieGetDTO>;

  calculatedDataSeries: Array<DataSerieGetDTO>;

  isNoDataFound: boolean = false;
  isDataLoaded: boolean = false;
  isGraphicLoaded: boolean = true;
  eventCreatedTime = "";

  variablesService: VariablesService;
  devicesService: DevicesService;
  eventsService: EventsService;
  dataService: DataService;

  @Ref("graphic-modal") readonly graphicModal!: any;
  @Ref("visuGraphic") readonly visuGraphic!: any;
  @Ref("annotationModalForm") readonly annotationModalForm!: any;
  @Ref("eventsModalForm") readonly eventsModalForm!: any;

  created() {
    this.$opensilex.hideLoader();
    this.variablesService = this.$opensilex.getService<VariablesService>(
        "opensilex.VariablesService"
    );
    this.devicesService = this.$opensilex.getService<DevicesService>(
        "opensilex.DevicesService"
    );
    this.eventsService = this.$opensilex.getService<EventsService>(
        "opensilex.EventsService"
    );
    this.dataService = this.$opensilex.getService<DataService>(
        "opensilex-core.DataService"
    );
    this.refresh();
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

  refresh() {
    this.variablesService
        .getVariable(this.variableUri.uri)
        .then( (http: HttpResponse<OpenSilexResponse<VariableDetailsDTO>>) => {
          if (http && http.response) {
            this.variable = http.response.result;

            this.loadData();
          }
        }
    )
  }

  loadData() {
    var today: Date = new Date();
    var aWeekBefore: Date = new Date(today);
    aWeekBefore.setDate(aWeekBefore.getDate() - 60);

    this.dataService.getDataSeriesByFacility(
        this.variableUri.uri,
        this.target,
        aWeekBefore.toISOString(),
        today.toISOString(),
        ["date=asc"]
    )
        .then(
            (
                http: HttpResponse<OpenSilexResponse<DataVariableSeriesGetDTO>>
            ) => {
              if (http && http.response) {
                let seriesDTO: DataVariableSeriesGetDTO = http.response.result;

                if (!seriesDTO.data_series.length) {
                  this.isNoDataFound = true;
                  return;
                }

                this.dataSeries = seriesDTO.data_series;
                this.calculatedDataSeries = seriesDTO.calculated_series;

                this.isDataLoaded = true;
              }
            }
        );
  }

  get lastMedianData() {
    let data;
    if (this.calculatedDataSeries.length === 0) {
      data = this.dataSeries[0].data
    }
    else {
      data = this.calculatedDataSeries[0].data;
    }
    return data[data.length - 1];
  }

  get deviceUriList() {
    if (!this.dataSeries) {
      return [];
    }

    return this.dataSeries.map(serie => {
      if (serie.provenance) {
        return {
          uri: serie.provenance.prov_was_associated_with[0].uri,
          value: serie.provenance.prov_was_associated_with[0].uri,
          to: {
            path: "/device/details/" + encodeURIComponent(serie.provenance.prov_was_associated_with[0].uri),
          },
        }
      }
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

  showGraphic() {
    console.log("show graphic");
    this.prepareGraphic();
    this.graphicModal.show();
  }

  prepareGraphic() {
      this.$opensilex.disableLoader();
      var promises = [];
      let promise;

      for (let i = 0; i < this.calculatedDataSeries.length; ++i) {
        console.debug(this.calculatedDataSeries[i]);
        promise = this.buildDataSerie(this.calculatedDataSeries[i]);
        promises.push(promise);
      }
      for (let i = 0; i < this.dataSeries.length; ++i) {
        promise = this.buildDataSerie(this.dataSeries[i]);
        promises.push(promise);
      }
      //promise = this.buildEventsSerie();
      //promises.push(promise);

      Promise.all(promises)
        .then(values => {
          let series = [];
          values.forEach(serie => {
            series.push(serie);
          });

          this.isGraphicLoaded = true;

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
          undefined,
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

  buildDataSerie(dataSerie) {

    var data = dataSerie.data as Array<DataGetDTO>;
    data.sort((a, b) => (a.date > b.date) ? 1 : -1);
    let dataLength = data.length;

    if (dataLength === 0){
      this.$opensilex.showInfoToast(
      this.$t("component.common.search.noDataFound").toString());
    }

    if (dataLength >= 0) {
      const cleanData = HighchartsDataTransformer.transformSimpleDataForHighcharts(data, dataSerie.provenance);
      if (dataLength > 50000) {
        this.$opensilex.showInfoToast(
            this.$i18n.t("DeviceDataTab.limitSizeMessageA") +
            " " +
            dataLength +
            " " +
            this.$i18n.t("DeviceDataTab.limitSizeMessageB")
        );
      }

      console.debug(cleanData);

      // TODO rework prov dto
      let provenance = dataSerie.provenance.prov_was_associated_with;
      let prov = dataSerie.provenance;
      if (provenance) {
        prov = dataSerie.provenance.prov_was_associated_with[0];
      }

      return {
        name: prov.uri,
        data: cleanData,
        visible: (prov.uri.startsWith("dev")) ? false : true
      };
    }
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
        datatypeMessageA: The variable datatype is
        datatypeMessageB: "At this time only decimal or integer are accepted"
        limitSizeMessageA : "There are "
        limitSizeMessageB : " data .Only the 50 000 first data are displayed."
        lastData: Last data collected


fr:
    DeviceDataTab:
        datatypeMessageA:  le type de donnée de la variable est
        datatypeMessageB: "Pour le moment, seuls les types decimal ou entier sont acceptés "
        limitSizeMessageA : "Il y a "
        limitSizeMessageB : " données .Seules les 50 000 premières valeurs sont affichées. "
        lastData: Dernière donnée collectée

</i18n>

