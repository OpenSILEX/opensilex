<template>
  <div>
    <opensilex-Card
        :no-footer="true"
        :no-header="true"
        icon=""
    >
      <template v-slot:body>
        <div class="row">
          <div class="col-sm-6">

            <!-- Variable Group Selector -->
            <div v-if="hasVariableGroup">
              <label for="variableGroupSelector">
                {{ $t("FacilityMonitoringView.variable-group-selector") }}
              </label>
              <font-awesome-icon
                  icon="question-circle"
                  class="variable-group-help"
                  v-b-tooltip.hover.top="$t('FacilityMonitoringView.variable-group-help')"
              />
              <opensilex-SelectForm
                  id="variableGroupSelector"
                  :selected.sync="selectedVariableGroup"
                  :searchMethod="searchVariableGroups"
                  :placeholder="$t('FacilityMonitoringView.no-variable-group-selected')"
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
      </template>
    </opensilex-Card>

    <opensilex-TextView
        v-if="isNoVariableFound"
        id="no-variable-text"
        :label="$t('FacilityMonitoringView.no-variable')"
    >
    </opensilex-TextView>

    <!-- Grid layout for tiles -->

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
              v-bind="item.content"
              :defaultStartDate="startDate"
              :defaultEndDate="endDate"
          >
          </opensilex-VariableVisualizationTile>
        </GridItem>
    </GridLayout>
  </div>
</template>

<script lang="ts">
import {Component, Ref} from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../../lib/HttpResponse";
import { FacilityGetDTO } from "opensilex-core/index";
import {OrganizationsService} from "opensilex-core/api/organizations.service";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import {VariablesService} from "opensilex-core/api/variables.service";
import {DataService} from "opensilex-core/api/data.service";
import {NamedResourceDTOVariableModel} from "opensilex-core/model/namedResourceDTOVariableModel";
import {VariableGetDTO} from "opensilex-core/model/variableGetDTO";
import {VariablesGroupGetDTO} from "opensilex-core/model/variablesGroupGetDTO";
import {GridItemData} from "vue-grid-layout";


@Component
export default class FacilityMonitoringView extends Vue {
  $opensilex: OpenSilexVuePlugin;

  /// GridLayout system
  NB_COL = 4;
  layout: Array<GridItemData> = [];

  uri: string = null;
  selected: FacilityGetDTO = null;
  usedVariables: NamedResourceDTOVariableModel[] = [];
  selectedVariableGroup = null;
  startDate: string;
  endDate: string;

  isNoVariableFound: boolean = false;
  isItemsLoaded: boolean = false;

  /// services
  organizationService: OrganizationsService;
  variablesService: VariablesService;
  dataService: DataService;

  @Ref("periodPicker") readonly periodPicker!: any;


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
    this.dataService = this.$opensilex.getService<DataService>(
        "opensilex-core.DataService"
    );

    this.initDatePeriod();
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

  initDatePeriod() {
    this.endDate = new Date().toISOString();
    let begin = new Date();
    begin.setDate(begin.getDate() - 7);
    this.startDate = begin.toISOString();
    console.debug(this.startDate + "->" + this.endDate);
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

    if (this.selectedVariableGroup != null) {
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
          this.setUsedVariables(resultList);
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
            undefined,
            0,
            0)
        .then((http: HttpResponse<OpenSilexResponse<Array<VariableGetDTO>>>) => {
          let resultList = http.response.result;
          this.setUsedVariables(resultList);
        });
  }

  setUsedVariables(resultList) {
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
  }

  /**
   * Create add fill tiles according to the previously collected variables.
   */
  loadTiles() {
    this.layout = [];
    let i = 0;
    for (let v of this.usedVariables) {
      let x = i % this.NB_COL;
      let y = Math.floor(i / this.NB_COL);

      this.layout.push({
        "x":x, "y":y, "w":1, "h":2, "i":''+i,
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

.periodBtn{
  border-color:#018371;
  background: #fff;
  color: #018371
}

.active {
  background-color: #00A38D;
  border-color:#00A38D;
  color: #fff;
}

</style>

<i18n>
en:
  FacilityMonitoringView:
    variable-group-selector: Environmental variable groups
    no-variable-group-selected: All environnemental variables
    no-data: No data found for this period
    no-variable: No environnemental variable found
    start-date-help: Start date of data displayed
    end-date-help: End date of data displayed
    variable-group-help: Groups of variables associated with the facility. You can associate groups in the update form of the facility.


fr:
  FacilityMonitoringView:
    variable-group-selector: Groupes de variables environnementales
    no-variable-group-selected: Toutes les variables environnementales
    no-data: Aucune donnée trouvée pour cette période
    no-variable: Aucune variable environnementale trouvée
    start-date-help: Date de début des données affichées
    end-date-help: Date de fin des données affichées
    variable-group-help: Les groupes de variables associées à l'infrastructure. Vous pouvez associer des groupes dans le formulaire de modification de l'infrastructure.

</i18n>
