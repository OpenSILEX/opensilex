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
              <opensilex-FormSelector
                id="variableGroupSelector"
                :selected="selectedVariableGroup"
                :searchMethod="searchVariableGroups"
                :placeholder="$t('FacilityMonitoringView.no-variable-group-selected')"
                class="searchFilter"
                @clear="loadVariables"
                @onValidate="loadVariables"
                @onClose="loadVariables"
                @select="loadVariables"
                @handlingEnterKey="loadVariables"
              ></opensilex-FormSelector>
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

    <div v-if="isItemsLoaded"
         class="row">
      <div v-for="item in layout"
           class="item col"
           :key="item.i">
        <opensilex-VariableVisualizationTile
          class="tile-content"
          v-bind="item.content"
          :defaultStartDate="startDate"
          :defaultEndDate="endDate"
        >
        </opensilex-VariableVisualizationTile>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import HttpResponse, { OpenSilexResponse } from "../../../lib/HttpResponse";
import { FacilityGetDTO } from "opensilex-core/index";
import {OrganizationsService} from "opensilex-core/api/organizations.service";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import {VariablesService} from "opensilex-core/api/variables.service";
import {DataService} from "opensilex-core/api/data.service";
import {NamedResourceDTOVariableModel} from "opensilex-core/model/namedResourceDTOVariableModel";
import {VariableGetDTO} from "opensilex-core/model/variableGetDTO";
import {VariablesGroupGetDTO} from "opensilex-core/model/variablesGroupGetDTO";
import {computed, inject, onMounted, ref} from "vue";
import {useStore} from "vuex";
import {useRoute} from "vue-router";

//#region Constant values & Services
const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!;
const $store = useStore();
const $route = useRoute()
// services
const organizationService: OrganizationsService = $opensilex.getService<OrganizationsService>('opensilex.OrganizationsService');
const variablesService: VariablesService = $opensilex.getService<VariablesService>('opensilex.VariablesService');
const dataService: DataService = $opensilex.getService<DataService>('opensilex.DataService');
//#endregion

//#region Reactive variable values (Data)
/// GridLayout system
const layout = ref<Array<any>>([]);

const uri = ref<string>(null);
const selected = ref<FacilityGetDTO>(null);
const usedVariables = ref<NamedResourceDTOVariableModel[]>([]);
const selectedVariableGroup = ref(null);
const startDate = ref<string>(null);
const endDate = ref<string>(null);

const isNoVariableFound = ref<boolean>(false);
const isItemsLoaded = ref<boolean>(false);
//#endregion

//#region: Computed
const user = computed(() => {
  return $store.state.user;
});

const credentials = computed(() => {
  return $store.state.credentials;
});

const hasVariableGroup = computed(() => {
  return selected.value && (selected.value.variableGroups.length != 0);
});
//#endregion

//#region: Hooks
onMounted(()=>{
  //Route decoding
  let raw = $route.params.uri
  let param = Array.isArray(raw) ? raw[0] : raw
  uri.value = param ? decodeURIComponent(param) : ''

  initDatePeriod();
  refresh();
});
//#endregion

function refresh() {
  organizationService
    .getFacility(uri.value)
    .then((http: HttpResponse<OpenSilexResponse<FacilityGetDTO>>) => {
      let detailDTO: FacilityGetDTO = http.response.result;
      selected.value = detailDTO;
      loadVariables();
    });
}

function initDatePeriod() {
  endDate.value = new Date().toISOString();
  let begin = new Date();
  begin.setDate(begin.getDate() - 7);
  startDate.value = begin.toISOString();
}

function searchVariableGroups() {
  let variableGroupsURIs = selected.value.variableGroups.map(group => group.uri);
  return variablesService
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
    .catch($opensilex.errorHandler);
}

function loadVariables() {
  isNoVariableFound.value = false;
  isItemsLoaded.value = false;

  if (selectedVariableGroup != null) {
    loadVariablesFromGroup();
  }
  else {
    loadVariablesFromData();
  }
}

/**
 * Get all variables with this facility as target.
 */
function loadVariablesFromData() {
  dataService
    .getUsedVariables(
      null,
      [uri.value],
      null,
      null)
    .then((http) => {
      let resultList = http.response.result;
      setUsedVariables(resultList);
    });
}

/**
 * Get variables contained in the selected variable group.
 */
function loadVariablesFromGroup() {
  variablesService
    .searchVariables(
      undefined,
      undefined,
      undefined,
      undefined,
      undefined,
      undefined,
      selectedVariableGroup.value,
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
      setUsedVariables(resultList);
    });
}

function setUsedVariables(resultList) {
  usedVariables.value = [];
  for (let i in resultList) {
    let variable = resultList[i];
    usedVariables.value.push({
      uri: variable.uri,
      name: variable.name
    });
  }

  if (usedVariables.value.length === 0) {
    isNoVariableFound.value = true;
    return;
  }

  loadTiles();
}

/**
 * Create add fill tiles according to the previously collected variables.
 */
function loadTiles() {
  layout.value = [];
  let i = 0;
  for (let v of usedVariables.value) {
    layout.value.push({
      "i":''+i,
      "content": { target: uri, variableUri: v }
    });
    ++i;
  }
  isItemsLoaded.value = true;
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

.item {
  min-width: 400px;
  max-width: 400px;
}

</style>

<i18n>
en:
  FacilityMonitoringView:
    variable-group-selector: Environmental variable groups
    no-variable-group-selected: All environnemental variables
    no-data: No data found for this period
    no-variable: No environnemental variables found
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
    variable-group-help: Les groupes de variables associées à l'installation. Vous pouvez associer des groupes dans le formulaire de modification de l'installation.

</i18n>
