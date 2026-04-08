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
import ModalForm from "@/components/common/forms/ModalForm.vue";
import {useI18n} from "vue-i18n";

//#region: Constants
const $opensilex = inject<OpenSilexVuePlugin>('$opensilex');
const $store = inject<any>('$store')!;

//Services
const service: OrganizationsService = $opensilex.getService<OrganizationsService>('opensilex.OrganizationsService');
const expService: ExperimentsService = $opensilex.getService("opensilex.ExperimentsService");
//#endregion

//#region: Data
let selectedFacility: FacilityGetDTO = null;
let experiments: Array<ExperimentGetListDTO> = [];
//#endregion

//#region: Refs
const facilitiesView = ref<InstanceType<typeof FacilitiesView>|null>(null);
//#endregion

//#region: Computed
const user = computed(() => $store.state.user);
const credentials = computed(() => $store.state.credentials);
//#endregion

//#region: Event Handlers
function onFacilitySelected(facility: FacilityGetDTO) {
  console.debug("SELECTING FACILITY, ", facility)
  if (!facility || !facility.uri) {
    selectedFacility = undefined;
    experiments = [];
    return;
  }

  experiments = [];

  service.getFacility(facility.uri)
    .then((http: HttpResponse<OpenSilexResponse<FacilityGetDTO>>) => {
      selectedFacility = http.response.result;
      if(selectedFacility) {
        //$nextTick(() => {loadExperiments();});
        loadExperiments();
      }
    });
}

function refresh() {
  //TODO MAX this line wasnt working i think because private by default
  //facilitiesView.refresh();
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
      [selectedFacility.uri],
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
