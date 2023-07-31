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
            :label="$t('FacilityMonitoringView.no-data')">
        </opensilex-TextView>

        <div
            id="data-infos"
            v-if="isDataLoaded">
          <div class="row">
            <div class="col" v-if="isDataOutOfReach">
              <opensilex-TextView
                  style="margin-bottom: 0;"
                  :value="$t('VariableVisualizationTile.lastDataStored')">
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
                  :value="$t('VariableVisualizationTile.lastMedianData')">
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
              <opensilex-Icon icon="fa#hourglass-half"/> {{ $t("Histogram.period." + period) }}
            </b-badge>

            <!-- Graphic Options menu dropdown-->
            <b-dropdown right size="lg" variant="link" toggle-class="text-decoration-none" no-caret>
              <template #button-content>
                <opensilex-Icon icon="fa#bars" size="lg" class="dashboardGraphicMenu" />
              </template>

              <b-dropdown-item @click="scatter">
                <opensilex-Icon icon="fa#braille" />
                {{ $t("Histogram.dataStyle") }}
              </b-dropdown-item>

              <b-dropdown-item @click="fullscreen">
                <opensilex-Icon icon="fa#expand" />
                {{ $t("VisualisationGraphic.fullscreen") }}
              </b-dropdown-item>

              <b-dropdown-item @click="exportPNG">
                <opensilex-Icon icon="fa#download" />
                {{ $t("VisualisationGraphic.download") }}
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
                                :label="$t('FacilityMonitoringView.no-data')">
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

<script lang="ts">
import {Component, Prop, Ref, Watch} from "vue-property-decorator";
import Vue from "vue";
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


@Component
export default class VariableVisualizationTile extends Vue {
  $opensilex: OpenSilexVuePlugin;

  get user() {
    return this.$store.state.user;
  }
  get credentials() {
    return this.$store.state.credentials;
  }
  get hasCalculatedSeries() {
    return (this.calculatedDataSeries.length > 0);
  }

  get isDataForGraph() {
    return !(this.dataSeries.length ===0 && this.calculatedDataSeries.length === 0)
  }

  @Prop()
  variableUri: NamedResourceDTOVariableModel;

  @Prop()
  target: string;

  period: Period = "week";

  @Prop()
  defaultStartDate: string;
  @Prop()
  defaultEndDate: string;

  graphicStartDate: string;
  graphicEndDate: string;

  @Watch('startDate')
  @Watch('endDate')
  onPropertyChanged(value: string, oldValue: string) {
    this.loadSimplifiedData();
  }

  variable: VariableDetailsDTO;

  dataSeries: Array<DataSerieGetDTO> = [];
  calculatedDataSeries: Array<DataSerieGetDTO> = [];
  availableProvenances: Array<DataSimpleProvenanceGetDTO> = [];

  medianSerie = [];
  lastData: any;

  isNoDataFound: boolean = false;
  isDataOutOfReach: boolean = false;
  isDataLoaded: boolean = false;
  isGraphicLoaded: boolean = true;
  isLoadAllProvToggled: boolean = false;

  variablesService: VariablesService;
  devicesService: DevicesService;
  dataService: DataService;

  @Ref("graphic-modal") readonly graphicModal!: any;
  @Ref("visuGraphic") readonly visuGraphic!: any;
  @Ref("histogramSettings") readonly histogramSettings!: any;

  created() {
    this.$opensilex.hideLoader();
    this.variablesService = this.$opensilex.getService<VariablesService>(
        "opensilex.VariablesService"
    );
    this.devicesService = this.$opensilex.getService<DevicesService>(
        "opensilex.DevicesService"
    );
    this.dataService = this.$opensilex.getService<DataService>(
        "opensilex-core.DataService"
    );

    this.initDatePeriod();
    this.refresh();
  }

  private langUnwatcher;
  mounted() {
    this.langUnwatcher = this.$store.watch(
      () => this.$store.getters.language,
      lang => {
        //this.prepareGraphic();
      }
    );
  }

  refresh() {
    this.variablesService
        .getVariable(this.variableUri.uri)
        .then( (http: HttpResponse<OpenSilexResponse<VariableDetailsDTO>>) => {
          if (http && http.response) {
            this.variable = http.response.result;

            this.loadSimplifiedData();
          }
        }
    )
  }

  initDatePeriod() {
    this.graphicStartDate = this.defaultStartDate;
    this.graphicEndDate = this.defaultEndDate;
  }

  onSettingsChanged(period: Period, begin: string, end: string, selectAll: boolean) {
    this.graphicStartDate = begin;
    this.graphicEndDate = end;
    this.period = period;
    this.isLoadAllProvToggled = selectAll;
    this.loadAllData();
  }

  loadSimplifiedData() {
    this.isNoDataFound = false;
    this.isDataOutOfReach = false;
    this.isDataLoaded = false;
    this.$opensilex.disableLoader();

    console.debug(this.defaultStartDate + " -> " + this.defaultEndDate);

    this.dataService.getDataSeriesByFacility(
        this.variableUri.uri,
        this.target,
        (this.defaultStartDate != "") ? this.defaultStartDate : undefined,
        (this.defaultEndDate != "") ? this.defaultEndDate : undefined,
        true
    )
        .then(
            (
                http: HttpResponse<OpenSilexResponse<DataVariableSeriesGetDTO>>
            ) => {
              if (http && http.response) {
                let seriesDTO: DataVariableSeriesGetDTO = http.response.result;

                if (!seriesDTO.last_data_stored) {
                  this.isNoDataFound = true;
                  return;
                }

                if (!seriesDTO.calculated_series.length) {
                  this.lastData = seriesDTO.last_data_stored;
                  this.isDataOutOfReach = true;
                  this.isDataLoaded = true;
                  return;
                }

                this.calculatedDataSeries = seriesDTO.calculated_series;
                this.availableProvenances = seriesDTO.provenances;

                this.medianSerie = seriesDTO.calculated_series[0].data
                    .sort((a, b) => (a.date > b.date) ? 1 : -1);

                this.updateLastMedianData();
                this.isDataLoaded = true;
              }
            }
        );
  }

  loadAllData() {
    this.isGraphicLoaded = false;
    this.$opensilex.disableLoader();

    this.dataService.getDataSeriesByFacility(
        this.variableUri.uri,
        this.target,
        (this.graphicStartDate != "") ? this.graphicStartDate : undefined,
        (this.graphicEndDate != "") ? this.graphicEndDate : undefined,
        !this.isLoadAllProvToggled
    )
        .then(
            (
                http: HttpResponse<OpenSilexResponse<DataVariableSeriesGetDTO>>
            ) => {
              if (http && http.response) {
                let seriesDTO: DataVariableSeriesGetDTO = http.response.result;

                this.dataSeries = seriesDTO.data_series;
                this.calculatedDataSeries = seriesDTO.calculated_series;

                this.showGraphic();
              }
            }
        );
  }

  updateLastMedianData() {
    if (this.medianSerie.length === 0) {
      return;
    }

    this.lastData = {
      value: this.medianSerie[this.medianSerie.length - 1].value,
      date: this.medianSerie[this.medianSerie.length - 1].date
    };
    this.lastData.value = this.lastData.value.toPrecision(4) + " " + this.variable.unit.symbol;
  }

  beforeDestroy() {
    this.langUnwatcher();
  }

  showGraphic() {
    this.graphicModal.show();
    this.prepareGraphic();
    this.isGraphicLoaded = true;
  }

  prepareGraphic() {
      console.debug(this.dataSeries.length + " " + this.calculatedDataSeries.length);

      this.$opensilex.disableLoader();
      let promises = [];
      let promise;

      for (let i = 0; i < this.calculatedDataSeries.length; ++i) {
        promise = this.buildDataSerie(this.calculatedDataSeries[i], !i);
        promises.push(promise);
      }
      for (let i = 0; i < this.dataSeries.length; ++i) {
        promise = this.buildDataSerie(this.dataSeries[i], !this.hasCalculatedSeries);
        promises.push(promise);
      }

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

  buildDataSerie(dataSerie, isVisible: boolean) {

    var data = dataSerie.data as Array<DataComputedGetDTO>;
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
            this.$t("VariableVisualizationTile.limitSizeMessage", {count: dataLength }).toString()
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

  scatter() {
    this.visuGraphic.scatter();
  }

  fullscreen() {
    this.visuGraphic.fullscreen();
  }

  exportPNG() {
    this.visuGraphic.exportPNG();
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

</style>

<i18n>
en:
    VariableVisualizationTile:
        datatypeMessageA: The variable datatype is
        datatypeMessageB: "At this time only decimal or integer are accepted"
        limitSizeMessage : "There are {count} data. Only the 50 000 first data are displayed."
        lastMedianData: Last median value
        lastDataStored: Last data imported on the date

fr:
    VariableVisualizationTile:
        datatypeMessageA:  le type de donnée de la variable est
        datatypeMessageB: "Pour le moment, seuls les types decimal ou entier sont acceptés "
        limitSizeMessage : "Il y a {count} données. Seules les 50 000 premières valeurs sont affichées."
        lastMedianData: Dernière valeur médiane
        lastDataStored: Dernière donnée importée à la date

</i18n>

