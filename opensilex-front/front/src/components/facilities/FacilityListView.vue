<template>
  <div class="container-fluid">
    <div class="row">
      <div class="col-md-6">
        <!-- Facilities -->
        <opensilex-FacilitiesView
          :withActions="true"
          @onUpdate="refresh"
          @onCreate="refresh"
          @onDelete="refresh"
          :isSelectable="true"
          ref="facilitiesView"
          @facilitySelected="onFacilitySelected"
          :displayButtonOnTop="true"
        ></opensilex-FacilitiesView>
      </div>
      <div class="col-md-6">
        <!-- Facility detail -->
        <opensilex-FacilityDescription
          :selected="selectedFacility"
          :experiments="experiments"
        >
        </opensilex-FacilityDescription>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">

import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {OrganizationsService} from "opensilex-core/api/organizations.service";
import {computed, inject, ref} from "vue";
import HttpResponse, {OpenSilexResponse} from "../../lib/HttpResponse";
import FacilitiesView from "./FacilitiesView.vue";
import { FacilityGetDTO } from 'opensilex-core/index';
import {ExperimentsService} from "opensilex-core/api/experiments.service";
import {ExperimentGetListDTO} from "opensilex-core/model/experimentGetListDTO";
import {useStore} from "vuex";

//#region: Constants
const $opensilex = inject<OpenSilexVuePlugin>('$opensilex');
const $store = useStore();

//Services
const service: OrganizationsService = $opensilex.getService<OrganizationsService>('opensilex.OrganizationsService');
const expService: ExperimentsService = $opensilex.getService("opensilex.ExperimentsService");
//#endregion

//#region: Data
let experiments: Array<ExperimentGetListDTO> = [];
//#endregion

//#region: Refs
//Component refs:
const facilitiesView = ref<InstanceType<typeof FacilitiesView>|null>(null);

//Value refs
const selectedFacility = ref<FacilityGetDTO|null>(null);
//#endregion

//#region: Computed
const user = computed(() => $store.state.user);
const credentials = computed(() => $store.state.credentials);
//#endregion

//#region: Event Handlers
/**
 * Event handler called when a line in Facility table was clicked (Not by clicking a checkbox)
 */
function onFacilitySelected(facility: FacilityGetDTO) {
  if (!facility || !facility.uri) {
    selectedFacility.value = undefined;

    experiments = [];
    return;
  }

  experiments = [];

  service.getFacility(facility.uri)
    .then((http: HttpResponse<OpenSilexResponse<FacilityGetDTO>>) => {
      selectedFacility.value = http.response.result;
      if(selectedFacility) {
        loadExperiments();
      }
    });
}

function refresh() {
  facilitiesView.value.refresh();
}
//#endregion

//#region: functions
function loadExperiments() {
  return expService
    .searchExperiments(
      undefined, // label
      undefined, // year
      false, // isEnded
      undefined, // species
      undefined, // factorCategories
      undefined, // projects
      undefined, // isPublic
      [selectedFacility.value.uri],
      undefined, // funding
      undefined,
      0,
      20)
    .then(
      (
        http: HttpResponse<OpenSilexResponse<Array<ExperimentGetListDTO>>>
      ) => {
        experiments = http.response.result;
        return http;
      }
    );
}
//#endregion

</script>


<i18n>
en:
  FacilityListView:
    description:
      Manage and configure facilities
fr:
  FacilityListView:
    description:
      Gérer et configurer les installations environnementales
</i18n>
