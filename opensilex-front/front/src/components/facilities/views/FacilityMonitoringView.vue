<template>
  <div class="monitoringView">
    <!-- Only displayed if the facility has variable groups, otherwise it would be empty -->
    <Card
      v-if="hasVariableGroup"
      :no-footer="true"
      :no-header="true"
      icon=""
    >
      <template v-slot:body>
        <!-- Variable Group Selector -->
        <div class="variableGroupFilter">
          <label for="variableGroupSelector">
            {{ t("component.variable.groupVariable.enviromental-variable-groups") }}
          </label>
          <n-tooltip trigger="hover" placement="top">
            <template #trigger>
              <font-awesome-icon
                icon="question-circle"
                class="variable-group-help"
              />
            </template>
            {{ t('component.variable.groupVariable.variable-group-help') }}
          </n-tooltip>
          <FormSelector
            id="variableGroupSelector"
            v-model:selected="selectedVariableGroup"
            :searchMethod="searchVariableGroups"
            :placeholder="t('component.variable.groupVariable.all-enviromental-variables')"
            class="searchFilter"
          ></FormSelector>
        </div>
      </template>
    </Card>

    <TextView
      v-if="isNoVariableFound"
      id="no-variable-text"
      label="component.variable.no-enviromental-variable"
    >
    </TextView>

    <!-- One tile per variable -->
    <div v-if="isItemsLoaded"
         class="tiles">
      <div v-for="variable in usedVariables"
           class="item"
           :key="variable.uri">
        <VariableVisualizationTile
          class="tile-content"
          :variableUri="variable"
          :target="uri"
          :defaultStartDate="startDate"
          :defaultEndDate="endDate"
        >
        </VariableVisualizationTile>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import {computed, ComputedRef, inject, onMounted, ref, Ref, watch} from "vue";
import {useI18n} from "vue-i18n";
import {NTooltip} from "naive-ui";
import HttpResponse, {OpenSilexResponse} from "@/lib/HttpResponse";
import {FacilityGetDTO} from "opensilex-core/index";
import {OrganizationsService} from "opensilex-core/api/organizations.service";
import {VariablesService} from "opensilex-core/api/variables.service";
import {DataService} from "opensilex-core/api/data.service";
import {NamedResourceDTOVariableModel} from "opensilex-core/model/namedResourceDTOVariableModel";
import {VariablesGroupGetDTO} from "opensilex-core/model/variablesGroupGetDTO";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import Card from "@/components/common/views/Card.vue";
import FormSelector from "@/components/common/forms/FormSelector.vue";
import TextView from "@/components/common/views/TextView.vue";
import VariableVisualizationTile from "@/components/variables/views/VariableVisualizationTile.vue";

/**
 * Monitoring tab of a facility: one tile per variable measured on the facility, or per variable of the selected
 * environmental variable group. Each tile displays the last median data of its variable over the last 7 days, and
 * opens its graphic when clicked.
 */

//#region Public
const props = defineProps<{
  /** URI of the facility */
  uri: string
}>();
//#endregion

//#region Private
//#region Plugins & services
const $opensilex: OpenSilexVuePlugin = inject<OpenSilexVuePlugin>("$opensilex")!;
const {t} = useI18n();

const organizationService: OrganizationsService = $opensilex.getService<OrganizationsService>("opensilex.OrganizationsService");
const variablesService: VariablesService = $opensilex.getService<VariablesService>("opensilex.VariablesService");
const dataService: DataService = $opensilex.getService<DataService>("opensilex.DataService");
//#endregion

//#region Data & Computed
/** Default period of the tiles: the last 7 days, ISO dates */
const endDate: string = new Date().toISOString();
const startDate: string = getSevenDaysAgo().toISOString();

const facility: Ref<FacilityGetDTO | undefined> = ref<FacilityGetDTO | undefined>(undefined);
/** URI of the selected environmental variable group, all the variables of the facility are displayed without it */
const selectedVariableGroup: Ref<string | undefined> = ref<string | undefined>(undefined);
/** Variables displayed as tiles */
const usedVariables: Ref<NamedResourceDTOVariableModel[]> = ref<NamedResourceDTOVariableModel[]>([]);

const isNoVariableFound: Ref<boolean> = ref<boolean>(false);
const isItemsLoaded: Ref<boolean> = ref<boolean>(false);

const hasVariableGroup: ComputedRef<boolean> = computed<boolean>(() => facility.value?.variableGroups?.length > 0);
//#endregion

//#region Hooks & watchers
onMounted(() => {
  loadFacility();
});

// Selecting, changing or clearing the variable group (the selector has no clear event, only its value changes)
watch(selectedVariableGroup, loadVariables);
//#endregion

//#region Methods
function getSevenDaysAgo(): Date {
  const date: Date = new Date();
  date.setDate(date.getDate() - 7);
  return date;
}

async function loadFacility(): Promise<void> {
  try {
    facility.value = (await organizationService.getFacility(props.uri)).response.result;
    await loadVariables();
  } catch (error) {
    $opensilex.errorHandler(error);
  }
}

/**
 * Search method of the variable group selector: the environmental variable groups of the facility. The groups are
 * given as they are ({uri, name}), the selector converts them into options itself.
 */
function searchVariableGroups(): Promise<HttpResponse<OpenSilexResponse<VariablesGroupGetDTO[]>>> {
  const variableGroupsURIs: string[] = facility.value?.variableGroups?.map(group => group.uri) ?? [];
  return variablesService.getVariablesGroupByURIs(variableGroupsURIs, undefined);
}

/**
 * Loads the variables of the selected group, or all the variables measured on the facility if no group is selected
 */
async function loadVariables(): Promise<void> {
  isNoVariableFound.value = false;
  isItemsLoaded.value = false;
  try {
    const variables: NamedResourceDTOVariableModel[] = selectedVariableGroup.value
        ? await loadVariablesFromGroup(selectedVariableGroup.value)
        : await loadVariablesFromData();

    usedVariables.value = variables.map((variable: NamedResourceDTOVariableModel) => ({
      uri: variable.uri,
      name: variable.name
    }));
    isNoVariableFound.value = usedVariables.value.length === 0;
    isItemsLoaded.value = !isNoVariableFound.value;
  } catch (error) {
    $opensilex.errorHandler(error);
  }
}

/**
 * @returns the variables having data with this facility as target
 */
async function loadVariablesFromData(): Promise<NamedResourceDTOVariableModel[]> {
  return (await dataService.getUsedVariables(null, [props.uri], null, null)).response.result;
}

/**
 * @returns the variables of the variable group
 */
async function loadVariablesFromGroup(variableGroupUri: string): Promise<NamedResourceDTOVariableModel[]> {
  return (await variablesService.searchVariables(
      undefined,         // name
      undefined,         // entity
      undefined,         // entity_of_interest
      undefined,         // characteristic
      undefined,         // method
      undefined,         // unit
      variableGroupUri,  // group_of_variables
      undefined,         // not_included_in_group_of_variables
      undefined,         // data_type
      undefined,         // time_interval
      undefined,         // species
      undefined,         // withAssociatedData
      undefined,         // experiments
      undefined,         // scientific_objects
      undefined,         // devices
      undefined,         // order_by
      0,                 // page
      0                  // page_size: all the variables
  )).response.result;
}
//#endregion
//#endregion
</script>

<style scoped lang="scss">

.monitoringView {
  margin-top: 10px;
}

/* The selector does not need the whole width of the card */
.variableGroupFilter {
  max-width: 50%;
}

.tiles {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.tile-content {
  height: 100%;
  width: 100%;
}

.item {
  min-width: 400px;
  max-width: 400px;
}

</style>

