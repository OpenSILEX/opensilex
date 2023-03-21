
<template>
  <div>
    <opensilex-TextView v-if="isNoVariableFound"
                        id="no-variable-text"
        :label="$t('FacilityAssociatedDevices.no-variable')">
    </opensilex-TextView>

    <GridLayout v-if="isItemsLoaded"
                class="grid-layout"
        :layout.sync="layout"
        :col-num="NB_COL"
        :is-draggable="true"
        :is-resizable="false"
        :is-mirrored="false"
        :vertical-compact="true"
        :is-bounded="true"
        :autoSize="false"
        :margin="[10, 10]"
        :use-css-transforms="true">

        <GridItem class="tile"
                  v-for="item in layout"
                    :x="item.x"
                    :y="item.y"
                    :w="item.w"
                    :h="item.h"
                    :i="item.i"
                    :key="item.i"
                    :dragIgnoreFrom="'#data-infos, #devices-list, #graphic, #btn-show'">
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
import {OrganizationsService} from "opensilex-core/api/organizations.service";
import {DevicesService} from "opensilex-core/api/devices.service";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import {PositionsService} from "opensilex-core/api/positions.service";
import {VariablesService} from "opensilex-core/api/variables.service";
import {DataService} from "opensilex-core/api/data.service";
import {NamedResourceDTOVariableModel} from "opensilex-core/model/namedResourceDTOVariableModel";


@Component
export default class FacilityAssociatedDevices extends Vue {
  $opensilex: OpenSilexVuePlugin;

  NB_COL = 4;

  uri: string = null;
  selected: OrganizationGetDTO = null;
  usedVariables: NamedResourceDTOVariableModel[] = [];
  layout = [];

  isNoVariableFound: boolean = false;
  isItemsLoaded: boolean = false;

  /// services
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
            this.loadVariables();
        });
  }

  /**
   * Get all variables with this facility as target.
   */
  loadVariables() {
    this.dataService
        .getUsedVariables(
            null,
            [this.uri],
            null,
            null)
        .then((http) => {
          let resultList = http.response.result;
          this.usedVariables = [];
          for (let i in resultList) {
            let variable = resultList[i];
            this.usedVariables.push({
              uri: variable.uri,
              name: variable.name
            });
          }

          if (this.usedVariables.length === 0) {
            this.isNoVariableFound = true;
            return;
          }

          this.loadTiles();
        });
  }

  /**
   * Create add fill tiles according to the previously collected variables.
   */
  loadTiles() {
    let i = 0;
    for (let v of this.usedVariables) {
      let x = i % this.NB_COL;
      let y = ~~(i / this.NB_COL);

      this.layout.push({
        "x":x, "y":y, "w":1, "h":2, "i":i,
        "content": { target: this.uri, variableUri: v }
      });
      ++i;
    }
    this.isItemsLoaded = true;
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
    no-variable: No environnemental variable found

fr:
  FacilityAssociatedDevices:
    no-data: Aucunes données trouvées
    no-variable: Aucunes variables environnementales trouvées
</i18n>
