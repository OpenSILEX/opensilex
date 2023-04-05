
<template>
  <div>
    <div class="d-flex">
      <div class="mr-auto p-2">
        <div v-if="hasVariableGroup">
          <label for="variableGroupSelector">{{ $t("FacilityAssociatedDevices.variable-group-selector") }}</label>
          <opensilex-SelectForm
              id="variableGroupSelector"
              :selected.sync="selectedVariableGroup"
              :searchMethod="searchVariableGroups"
              :placeholder="$t('FacilityAssociatedDevices.no-variable-group-selected')"
              class="searchFilter"
              @clear="loadVariables"
              @onValidate="loadVariables"
              @onClose="loadVariables"
              @select="loadVariables"
              @handlingEnterKey="loadVariables"
          ></opensilex-SelectForm>
        </div>
      </div>
    </div>

    <opensilex-TextView
        v-if="isNoVariableFound"
        id="no-variable-text"
        :label="$t('FacilityAssociatedDevices.no-variable')"
    >
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
import { Component } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../../lib/HttpResponse";
import { FacilityGetDTO } from "opensilex-core/index";
import {OrganizationsService} from "opensilex-core/api/organizations.service";
import {DevicesService} from "opensilex-core/api/devices.service";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import {PositionsService} from "opensilex-core/api/positions.service";
import {VariablesService} from "opensilex-core/api/variables.service";
import {DataService} from "opensilex-core/api/data.service";
import {NamedResourceDTOVariableModel} from "opensilex-core/model/namedResourceDTOVariableModel";
import {VariableGetDTO} from "opensilex-core/model/variableGetDTO";
import {VariablesGroupGetDTO} from "opensilex-core/model/variablesGroupGetDTO";


@Component
export default class FacilityAssociatedDevices extends Vue {
  $opensilex: OpenSilexVuePlugin;

  NB_COL = 4;

  uri: string = null;
  selected: FacilityGetDTO = null;
  usedVariables: NamedResourceDTOVariableModel[] = [];
  selectedVariableGroup;
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

  get hasVariableGroup() {
    return this.selected && (this.selected.variableGroups.length != 0);
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
        .then((http: HttpResponse<OpenSilexResponse<FacilityGetDTO>>) => {
          let detailDTO: FacilityGetDTO = http.response.result;
          this.selected = detailDTO;
          this.loadVariables();
        });
  }

  searchVariableGroups() {
    let variableGroupsURIs = this.selected.variableGroups.map(group => group.uri);
    return this.variablesService
        .getVariablesGroupByURIs(variableGroupsURIs, undefined)
        .then((http: HttpResponse<OpenSilexResponse<Array<VariablesGroupGetDTO>>>) => {
          let nodeList = [];
          for (let group of http.response.result) {
            nodeList.push({
              id: group.uri,
              label: group.name
            });
          }
          http.response.result = nodeList;
          return http;
        })
        .catch(this.$opensilex.errorHandler);
  }

  loadVariables() {
    this.isNoVariableFound = false;
    this.isItemsLoaded = false;

    if (this.selectedVariableGroup) {
      this.loadVariablesFromGroup();
    }
    else {
      this.loadVariablesFromData();
    }
  }

  /**
   * Get all variables with this facility as target.
   */
  loadVariablesFromData() {
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
   * Get variables contained in the selected variable group.
   */
  loadVariablesFromGroup() {
    this.variablesService
        .searchVariables(
            undefined,
            undefined,
            undefined,
            undefined,
            undefined,
            undefined,
            this.selectedVariableGroup,
            undefined,
            undefined,
            undefined,
            undefined,
            undefined,
            undefined,
            undefined,
            undefined,
            0,
            0)
        .then((http: HttpResponse<OpenSilexResponse<Array<VariableGetDTO>>>) => {
          console.debug(http.response.result);
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
    this.layout = [];
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
    variable-group-selector: Variable Groups
    no-variable-group-selected: All environnemental variables
    no-data: No data found
    no-variable: No environnemental variable found

fr:
  FacilityAssociatedDevices:
    variable-group-selector: Groupes de variables
    no-variable-group-selected: Toutes les variables environnementales
    no-data: Aucunes données trouvées
    no-variable: Aucunes variables environnementales trouvées

</i18n>
