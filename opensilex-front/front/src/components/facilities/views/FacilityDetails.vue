<template>
  <div class="row">
    <div class="col-md-6">
      <div class="facilityDescription">
        <opensilex-FacilityDescription
          :selected="selected"
          :withActions="true"
          @onUpdate="refresh"
        >
        </opensilex-FacilityDescription>

        <opensilex-AssociatedVariableList
          v-if="selected"
          :variableList="selected.variables"
          :deviceList="selected.devices"
          :facilityUri="selected.uri"
        ></opensilex-AssociatedVariableList>
<!--        <div>Asooc Variable list</div>-->
      </div>
    </div>

    <div class="col-md-6">
      <opensilex-AssociatedExperimentsList
        :searchMethod="loadExperiments"
        :nameFilter.sync="experimentName"
        ref="experimentsView"
      ></opensilex-AssociatedExperimentsList>
    </div>
  </div>
</template>


<script setup lang="ts">
import HttpResponse, { OpenSilexResponse } from "../../../lib/HttpResponse";
import { FacilityGetDTO } from "opensilex-core/index";
import { ExperimentGetListDTO } from "opensilex-core/model/experimentGetListDTO";
import { DeviceGetDTO } from "opensilex-core/model/deviceGetDTO";
import {OrganizationsService} from "opensilex-core/api/organizations.service";
import {ExperimentsService} from "opensilex-core/api/experiments.service";
import {DevicesService} from "opensilex-core/api/devices.service";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import AssociatedExperimentsList from "../../experiments/AssociatedExperimentsList.vue";
import {computed, inject, onMounted, ref} from "vue";
import {useRoute} from "vue-router";

//#region constant values & Services
const $opensilex: OpenSilexVuePlugin = inject<OpenSilexVuePlugin>('$opensilex');
const $route = useRoute();
const organizationService: OrganizationsService = $opensilex.getService<OrganizationsService>('opensilex.OrganizationsService');
const experimentService: ExperimentsService = $opensilex.getService<ExperimentsService>('opensilex.ExperimentsService');
const deviceService: DevicesService = $opensilex.getService<DevicesService>('opensilex.DevicesService');
//#endregion

//#region reactive data
const selected = ref<FacilityGetDTO>(null);
const experiments = ref<Array<ExperimentGetListDTO>>([]);
const devices = ref<Array<DeviceGetDTO>>([]);
const experimentName = ref<string>("");
//#endregion

//#region: Component refs
const experimentsView = ref<InstanceType<typeof AssociatedExperimentsList> | null>(null);
//#endregion

//#region: Computed
const uri = computed(() => {
  return $route.params.uri
    ? decodeURIComponent($route.params.uri as string)
    : null
});
//#endregion

//#region: Hooks
onMounted(() => {
  refresh();
});
//#endregion

//#region: Functions
function refresh() {
  if(uri.value){
    organizationService
      .getFacility(uri.value)
      .then((http: HttpResponse<OpenSilexResponse<FacilityGetDTO>>) => {
        selected.value = http.response.result;
        loadExperiments();
        loadDevices();
      });
  }
}

function loadExperiments() {
  experiments.value = [];
  return experimentService
    .searchExperiments(
      experimentName.value, // label
      undefined, // year
      undefined, // isEnded
      undefined, // species
      undefined, // factorCategories
      undefined, // projects
      undefined, // isPublic
      [uri.value],
      undefined, // funding
      undefined,
      0,
      0)
    .then(
      (
        http: HttpResponse<OpenSilexResponse<Array<ExperimentGetListDTO>>>
      ) => {
        experiments.value = http.response.result;
        return http;
      }
    )
    .catch($opensilex.errorHandler);
}

function loadDevices() {
  devices.value = [];
  deviceService.getDevicesByFacility(uri.value)
    .then(
      (
        http: HttpResponse<OpenSilexResponse<Array<DeviceGetDTO>>>
      ) => {
        if (http && http.response) {
          devices.value = http.response.result;
        }
      }
    )
    .catch($opensilex.errorHandler);
}
//#endregion

</script>

<style scoped lang="scss">
.facilityDescription {
  margin-top: -25px;
}

@media (max-width: 769px) {
  .facilityDescription {
    margin-top: 0;
  }
}
</style>
