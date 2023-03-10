
<template>
  <div>
    <div v-if="isNoDataFound"
      id="no-data-text">
      {{$t("FacilityAssociatedDevices.no-data")}}
    </div>

    <GridLayout v-if="isDataLoaded"
                class="grid-layout"
        :layout.sync="layout"
        :col-num="NB_COL"
        :is-draggable="true"
        :is-resizable="true"
        :is-mirrored="false"
        :vertical-compact="true"
        :autoSize="true"
        :margin="[10, 10]"
        :use-css-transforms="true">

        <GridItem v-for="item in layout"
                   :x="item.x"
                   :y="item.y"
                   :w="item.w"
                   :h="item.h"
                   :i="item.i"
                   :key="item.i"
            :dragIgnoreFrom="'#devices-list, #graphic, #btn-show'"
            class="tile">
          <opensilex-VariableVisualizationTile
              class="tile-content"
              v-bind="item.content">
          </opensilex-VariableVisualizationTile>
        </GridItem>
    </GridLayout>
  </div>
</template>

<script lang="ts">
import { Component, Ref, Watch } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../../lib/HttpResponse";
import { OrganizationGetDTO } from "opensilex-core/index";
import { DeviceGetDTO } from "opensilex-core/model/deviceGetDTO";
import {OrganizationsService} from "opensilex-core/api/organizations.service";
import {DevicesService} from "opensilex-core/api/devices.service";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import {PositionsService} from "opensilex-core/api/positions.service";
import {PositionGetDTO} from "opensilex-core/model/positionGetDTO";
import {VariableDetailsDTO} from "opensilex-core/model/variableDetailsDTO";
import {VariablesService} from "opensilex-core/api/variables.service";
import VariableVisualizationTile from "../../variables/views/VariableVisualizationTile.vue";
import {VariableGetDTO} from "opensilex-core/model/variableGetDTO";
import {DataService} from "opensilex-core/api/data.service";
import {DataSerieGetDTO} from "opensilex-core/model/dataSerieGetDTO";
import {DataVariableSeriesGetDTO} from "opensilex-core/model/dataVariableSeriesGetDTO";


@Component
export default class FacilityAssociatedDevices extends Vue {
  $opensilex: OpenSilexVuePlugin;

  NB_COL = 4;

  selected: OrganizationGetDTO = null;
  devices: Array<DeviceGetDTO> = [];
  variable: VariableDetailsDTO;
  uri = null;
  isNoDataFound: boolean = false;
  isDataLoaded: boolean = false;

  variables: Array<VariableGetDTO> = new Array<VariableGetDTO>();
  dataSeries: Array<DataVariableSeriesGetDTO> = new Array<DataVariableSeriesGetDTO>();

  layout = [];

  organizationService: OrganizationsService;
  deviceService: DevicesService;
  variablesService: VariablesService;
  positionService: PositionsService;
  dataService: DataService;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  created() {
    this.uri = decodeURIComponent(this.$route.params.uri);
    this.organizationService = this.$opensilex.getService<OrganizationsService>(
      "opensilex-core.OrganizationsService"
    );
    this.variablesService = this.$opensilex.getService<VariablesService>(
        "opensilex-core.VariablesService"
    );
    this.deviceService = this.$opensilex.getService<DevicesService>(
        "opensilex-core.DevicesService"
    );
    this.positionService = this.$opensilex.getService<PositionsService>(
        "opensilex-core.PositionsService"
    );
    this.dataService = this.$opensilex.getService<DataService>(
        "opensilex-core.DataService"
    );
    this.refresh();
  }

  refresh() {
    this.organizationService
        .getFacility(this.uri)
        .then((http: HttpResponse<OpenSilexResponse<OrganizationGetDTO>>) => {
            let detailDTO: OrganizationGetDTO = http.response.result;
            this.selected = detailDTO;
            this.loadData();
        });
  }

  loadData() {
    var today: Date = new Date();
    var aWeekBefore: Date = new Date(today);
    aWeekBefore.setDate(aWeekBefore.getDate() - 60);

    this.dataService.getDataSeriesByFacility(
        this.uri,
        aWeekBefore.toISOString(),
        today.toISOString(),
        ["date=asc"]
    )
    .then(
      (
        http: HttpResponse<OpenSilexResponse<Array<DataVariableSeriesGetDTO>>>
      ) => {
        if (http && http.response) {
          if (http.response.result.length === 0) {
            console.debug("no data found");
            this.isNoDataFound = true;
            return;
          }
          let detailsDTO: Array<DataVariableSeriesGetDTO> = http.response.result;
          this.dataSeries = detailsDTO;
          this.loadTiles();
        }
      }
    );
  }

  loadDevices() {
    this.devices = [];
    this.deviceService.getDevicesByFacility(this.uri)
        .then(
            (
                http: HttpResponse<OpenSilexResponse<Array<DeviceGetDTO>>>
            ) => {
              if (http && http.response) {
                this.devices = http.response.result;
                this.loadVariables();
                //this.loadPositionsHistory();
                //this.loadTiles();
              }
            }
        )
        .catch(this.$opensilex.errorHandler);
  }

  variablesMap: Map<string, Set<DeviceGetDTO>> = new Map<string, Set<DeviceGetDTO>>();

  addToVariablesMap(variables, device) {
    variables.forEach(variable => {
      if (!this.variablesMap.has(variable)) {
        this.variablesMap.set(variable, new Set<DeviceGetDTO>());
      }
      this.variablesMap.get(variable).add(device);
    });
  }

  getAssociatedVariables(device) {
    /*
    return this.deviceService.getDeviceVariables(device.uri)
        .then(
            (
                http: HttpResponse<OpenSilexResponse<Array<VariableGetDTO>>>
            ) => {
              if (http && http.response) {
                let variables = http.response.result as Array<VariableGetDTO>;
                variables.forEach(value => {
                  this.variables.push(value);
                })
                console.debug(this.variables);
                this.addToVariablesMap(variables.map(value => value.uri), device);

                variables.forEach( value => {
                  if (!this.variablesMap.has(value)) {
                    this.variablesMap.set(value, new Set(device));
                  }
                  else {
                    this.variablesMap.get(value).add(device);
                  }
                });


              }
            }
        )
        .catch(this.$opensilex.errorHandler);

     */
  }

  loadVariables() {
    var promises = [];
    let promise;

    this.devices.forEach((device) => {
      promise = this.getAssociatedVariables(device);
      promises.push(promise);
    });

    Promise.all(promises)
        .then( () => {
          console.debug(this.variablesMap);
          this.loadTiles();
        });
  }

  loadTiles() {
    let i = 0;
    for (let serie of this.dataSeries) {
      let x = i % this.NB_COL;
      let y = ~~(i / this.NB_COL);
      let v = serie.variable;
      let data = serie.data_series;
      let calculatedData = serie.calculated_series;

      this.layout.push({
        "x":x, "y":y, "w":1, "h":1, "i":i,
        "content": { variable: v, devices: [], dataSeries: data, calculatedDataSeries: calculatedData }
      });
      ++i;
    }
    this.isDataLoaded = true;
  }

}
</script>

<style scoped lang="scss">

.tile {
  display: table;
}

.tile-content {
  height: 100%;
  width: 100%;
}

#no-data-text {
  margin: 10px;
  font-size: 1em;
  font-weight: bold;
}

</style>

<i18n>
en:
  FacilityAssociatedDevices:
    no-data: No data found

fr:
  FacilityAssociatedDevices:
    no-data: Aucunes données trouvées
</i18n>
