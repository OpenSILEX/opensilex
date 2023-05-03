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
              style="margin-bottom: 0;"
              v-on:click.native="showGraphic"
              :value="$t('VariableVisualizationTile.lastMedianData')">
          </opensilex-TextView>
          <opensilex-TextView
              style="font-size: xx-large; margin-bottom: 0;"
              v-on:click.native="showGraphic"
              :value.sync="lastMedianData.value"
          >
          </opensilex-TextView>
          <opensilex-DateView
              v-on:click.native="showGraphic"
            :value.sync="lastMedianData.date"
            :isDateTime="true"
            :useLocaleFormat="true"
            :dateTimeFormatOptions="{ dateStyle: 'long', timeStyle: 'long' }">
          </opensilex-DateView>
        </div>

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
import {DataGetDTO, DevicesService, EventGetDTO, EventsService, NamedResourceDTOVariableModel} from "opensilex-core/index";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import HighchartsDataTransformer from "../../../models/HighchartsDataTransformer";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import {VariablesService} from "opensilex-core/api/variables.service";
import {DataService} from "opensilex-core/api/data.service";
import {VariableDetailsDTO} from "opensilex-core/model/variableDetailsDTO";
import {DataSerieGetDTO} from "opensilex-core/model/dataSerieGetDTO";
import {DataVariableSeriesGetDTO} from "opensilex-core/model/dataVariableSeriesGetDTO";


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

  @Prop()
  startDate: string;
  @Prop()
  endDate: string;

  @Watch('startDate')
  @Watch('endDate')
  onPropertyChanged(value: string, oldValue: string) {
    this.loadData();
  }


  variable: VariableDetailsDTO;

  dataSeries: Array<DataSerieGetDTO>;
  calculatedDataSeries: Array<DataSerieGetDTO>;

  lastMedianData: any;

  isNoDataFound: boolean = false;
  isDataLoaded: boolean = false;
  isGraphicLoaded: boolean = true;

  variablesService: VariablesService;
  devicesService: DevicesService;
  eventsService: EventsService;
  dataService: DataService;

  @Ref("graphic-modal") readonly graphicModal!: any;
  @Ref("visuGraphic") readonly visuGraphic!: any;

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
    this.isNoDataFound = false;
    this.isDataLoaded = false;

    this.dataService.getDataSeriesByFacility(
        this.variableUri.uri,
        this.target,
        (this.startDate != "") ? this.startDate : undefined,
        (this.endDate != "") ? this.endDate : undefined,
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

                this.updateLastMedianData();
                this.isDataLoaded = true;
              }
            }
        );
  }

  get hasCalculatedSeries() {
    return (this.calculatedDataSeries.length > 0);
  }

  updateLastMedianData() {
    let data;
    if (this.calculatedDataSeries.length === 0) {
      data = this.dataSeries[0].data
    }
    else {
      data = this.calculatedDataSeries[0].data;
    }

    this.lastMedianData = {
      value: data[data.length - 1].value,
      date: data[data.length - 1].date
    };
    this.lastMedianData.value = this.lastMedianData.value.toPrecision(4) + " " + this.variable.unit.symbol;
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

  showGraphic() {
    this.prepareGraphic();
    this.graphicModal.show();
  }

  prepareGraphic() {
      this.$opensilex.disableLoader();
      var promises = [];
      let promise;

      for (let i = 0; i < this.calculatedDataSeries.length; ++i) {
        promise = this.buildDataSerie(this.calculatedDataSeries[i], true);
        promises.push(promise);
      }
      for (let i = 0; i < this.dataSeries.length; ++i) {
        promise = this.buildDataSerie(this.dataSeries[i], !this.hasCalculatedSeries);
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

  buildDataSerie(dataSerie, isVisible: boolean) {

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
            this.$i18n.t("VariableVisualizationTile.limitSizeMessageA") +
            " " +
            dataLength +
            " " +
            this.$i18n.t("VariableVisualizationTile.limitSizeMessageB")
        );
      }

      let prov = dataSerie.provenance;

      return {
        name: prov.uri,
        data: cleanData,
        visible: isVisible
      };
    }
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
    VariableVisualizationTile:
        datatypeMessageA: The variable datatype is
        datatypeMessageB: "At this time only decimal or integer are accepted"
        limitSizeMessageA : "There are "
        limitSizeMessageB : " data .Only the 50 000 first data are displayed."
        lastMedianData: Last median value

fr:
    VariableVisualizationTile:
        datatypeMessageA:  le type de donnée de la variable est
        datatypeMessageB: "Pour le moment, seuls les types decimal ou entier sont acceptés "
        limitSizeMessageA : "Il y a "
        limitSizeMessageB : " données .Seules les 50 000 premières valeurs sont affichées. "
        lastMedianData: Dernière valeur médiane

</i18n>

