<template>
    <div class="row">
      <div class="col-md-6">
        <div class="facilityDescription">
            <opensilex-FacilityDescription
              :selected="selected"
              :devices="devices"
              :withActions="true"
              @onUpdate="refresh"
            >
            </opensilex-FacilityDescription>
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

<script lang="ts">
import { Component, Ref, Watch } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../../lib/HttpResponse";
import { OrganizationGetDTO } from "opensilex-core/index";
import { ExperimentGetListDTO } from "opensilex-core/model/experimentGetListDTO";
import { DeviceGetDTO } from "opensilex-core/model/deviceGetDTO";
import {OrganizationsService} from "opensilex-core/api/organizations.service";
import {ExperimentsService} from "opensilex-core/api/experiments.service";
import {DevicesService} from "opensilex-core/api/devices.service";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import AssociatedExperimentsList from "../../experiments/AssociatedExperimentsList.vue";

@Component
export default class FacilityDetails extends Vue {
  $opensilex: OpenSilexVuePlugin;

  selected: OrganizationGetDTO = null;
  experiments: Array<ExperimentGetListDTO> = [];
  devices: Array<DeviceGetDTO> = [];
  experimentName: string = "";
  uri = null;

  organizationService: OrganizationsService;
  experimentService: ExperimentsService;
  deviceService: DevicesService;

  @Ref("infrastructureFacilityForm") readonly infrastructureFacilityForm!: any;
  @Ref("experimentsView")
  experimentsView: AssociatedExperimentsList;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  created() {
    this.uri = decodeURIComponent(this.$route.params.uri);
    this.organizationService = this.$opensilex.getService(
      "opensilex-core.OrganizationsService"
    );
    this.experimentService = this.$opensilex.getService(
        "opensilex.ExperimentsService"
    );
    this.deviceService = this.$opensilex.getService(
        "opensilex-core.DevicesService"
    );
    this.refresh();
  }

  refresh() {
    this.organizationService
      .getFacility(this.uri)
      .then((http: HttpResponse<OpenSilexResponse<OrganizationGetDTO>>) => {
        let detailDTO: OrganizationGetDTO = http.response.result;
        this.selected = detailDTO;

        this.loadExperiments();
        this.loadDevices();
      });
  }

  loadExperiments() {
    this.experiments = [];
    return this.experimentService
        .searchExperiments(
            this.experimentName, // label
            undefined, // year
            undefined, // isEnded
            undefined, // species
            undefined, // factorCategories
            undefined, // projects
            undefined, // isPublic
            [this.uri],
            undefined,
            0,
            0)
        .then(
            (
              http: HttpResponse<OpenSilexResponse<Array<ExperimentGetListDTO>>>
            ) => {
              this.experiments = http.response.result;
              return http;
            }
        )
        .catch(this.$opensilex.errorHandler);
  }

  loadDevices() {
    this.devices = [];
    this.deviceService.searchDevices(
        undefined,
        undefined,
        undefined,
        undefined,
        undefined,
        undefined,
        this.uri,
        undefined,
        undefined,
        undefined,
        undefined,
        undefined,
        0,
        0)
        .then(
            (
                http: HttpResponse<OpenSilexResponse<Array<DeviceGetDTO>>>
            ) => {
              if (http && http.response) {
                this.devices = http.response.result;
              }
            }
        )
        .catch(this.$opensilex.errorHandler);
  }

}
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

