<template>
        <GridLayout v-if="isDataLoaded"
           :layout.sync="layout"
           :col-num="NB_COL"
           :row-height="300"
           :is-draggable="true"
           :is-resizable="true"
           :is-mirrored="false"
           :vertical-compact="false"
           :margin="[10, 10]"
           :use-css-transforms="true">

            <GridItem v-for="item in layout"
                       :x="item.x"
                       :y="item.y"
                       :w="item.w"
                       :h="item.h"
                       :i="item.i"
                       :key="item.i">
              <opensilex-VariableVisualizationTile
                  class="tile"
                  v-bind="item.content">
              </opensilex-VariableVisualizationTile>
            </GridItem>
        </GridLayout>

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


@Component
export default class FacilityAssociatedDevices extends Vue {
  $opensilex: OpenSilexVuePlugin;

  NB_COL = 4;

  selected: OrganizationGetDTO = null;
  devices: Array<DeviceGetDTO> = [];
  variable: VariableDetailsDTO;
  uri = null;
  isDataLoaded: boolean = false;

  variables: Array<VariableGetDTO> = new Array<VariableGetDTO>();

  layout = [];

  organizationService: OrganizationsService;
  deviceService: DevicesService;
  variablesService: VariablesService;
  positionService: PositionsService;

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
    this.refresh();
  }

  refresh() {
    this.organizationService
        .getFacility(this.uri)
        .then((http: HttpResponse<OpenSilexResponse<OrganizationGetDTO>>) => {
            let detailDTO: OrganizationGetDTO = http.response.result;
            this.selected = detailDTO;
            this.loadDevices();
        });
    this.variablesService
        .getVariable("dev:id/variable/abelmoschus_height_standard_method_centimetre")
        .then((http: HttpResponse<OpenSilexResponse<VariableDetailsDTO>>) => {
          let detailDTO: VariableDetailsDTO = http.response.result;
          this.variable = detailDTO;
          console.log("GET VAR ", this.variable);
        });
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
                /*
                variables.forEach( value => {
                  if (!this.variablesMap.has(value)) {
                    this.variablesMap.set(value, new Set(device));
                  }
                  else {
                    this.variablesMap.get(value).add(device);
                  }
                });

                 */
              }
            }
        )
        .catch(this.$opensilex.errorHandler);
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
    for (let i = 0; i < this.variables.length; ++i) {
      let x = i % this.NB_COL;
      let y = ~~(i / this.NB_COL);
      let v = this.variables.find(obj => {
        return obj.uri === this.variables[i].uri
      });
      console.debug(v);
      this.layout.push({
        "x":x, "y":y, "w":1, "h":1, "i":i,
        "content": { variable: v, devices: [...this.variablesMap.get(v.uri)]}
      });
    }
    this.isDataLoaded = true;
  }

  loadPositionsHistory() {
    this.devices.forEach(device => {
        this.positionService.searchPositionHistory(device.uri)
            .then(
                (
                    http: HttpResponse<OpenSilexResponse<Array<PositionGetDTO>>>
                ) => {
                  if (http && http.response) {
                    console.debug(http);
                  }
                }
            )
      }
    )

  }

}
</script>

<style scoped lang="scss">

.tile {
  height: 100%;
  width: 100%;
}
</style>

