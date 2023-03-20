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
            @facilitySelected="updateSelectedFacility"
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

<script lang="ts">
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {OrganizationsService} from "opensilex-core/api/organizations.service";
import {Component, Ref} from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, {OpenSilexResponse} from "../../lib/HttpResponse";
import FacilitiesView from "./FacilitiesView.vue";
import { FacilityGetDTO } from 'opensilex-core/index';
import {ExperimentsService} from "opensilex-core/api/experiments.service";
import {ExperimentGetListDTO} from "opensilex-core/model/experimentGetListDTO";

@Component
export default class FacilityListView extends Vue {
  $opensilex: OpenSilexVuePlugin;

  service: OrganizationsService;
  expService: ExperimentsService;

  selectedFacility: FacilityGetDTO = null;
  experiments: Array<ExperimentGetListDTO> = [];

  @Ref("facilitiesView")
  facilitiesView: FacilitiesView;

  created() {
    this.service = this.$opensilex.getService(
        "opensilex-core.OrganizationsService"
    );
    this.expService = this.$opensilex.getService(
        "opensilex.ExperimentsService"
    );
  }

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  updateSelectedFacility(facility: FacilityGetDTO) {
    if (!facility || !facility.uri) {
      this.selectedFacility = undefined;
      this.experiments = [];
      return;
    }

    this.experiments = [];

    this.service
        .getFacility(facility.uri)
        .then((http: HttpResponse<OpenSilexResponse<FacilityGetDTO>>) => {
          this.selectedFacility = http.response.result;
          if(this.selectedFacility) {
            this.$nextTick(() => {this.loadExperiments();});
          }
        });
  }

  loadExperiments() {
    return this.expService
        .searchExperiments(
            undefined, // label
            undefined, // year
            false, // isEnded
            undefined, // species
            undefined, // factorCategories
            undefined, // projects
            undefined, // isPublic
            [this.selectedFacility.uri],
            undefined,
            0,
            20)
        .then(
            (
                http: HttpResponse<OpenSilexResponse<Array<ExperimentGetListDTO>>>
            ) => {
              this.experiments = http.response.result;
              return http;
            }
        );
  }

  refresh() {
    this.facilitiesView.refresh();
  }
}
</script>

<style scoped>

</style>

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