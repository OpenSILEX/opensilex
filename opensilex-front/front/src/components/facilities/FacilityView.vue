<template>
  <div class="container-fluid" v-if="selected">
    <opensilex-PageHeader
      icon="ik#ik-globe"
      :title="selected.name"
      :description="selected.rdf_type_name"
      class="detail-element-header"
    ></opensilex-PageHeader>
    <opensilex-PageActions :tabs="false" :returnButton="true" class="FacilityViewReturnButton">
    </opensilex-PageActions>
    <div class="row">
      <div class="col-md-6">
        <div class="facilityDescription">
            <opensilex-FacilityDetail
              :selected="selected"
              :devices="devices"
              :withActions="true"
              @onUpdate="refresh"
            >
            </opensilex-FacilityDetail>
        </div>
      </div>

      <div class="col-md-6">
        <opensilex-AssociatedExperimentsList
            :searchMethod="loadExperiments"
            ref="experimentsView"
        ></opensilex-AssociatedExperimentsList>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Ref, Watch } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
// @ts-ignore
import { InfrastructureGetDTO } from "opensilex-core/index";
import { ExperimentGetListDTO } from "opensilex-core/model/experimentGetListDTO";
import { DeviceGetDTO } from "opensilex-core/model/deviceGetDTO";
import {PositionGetDTO} from "opensilex-core/model/positionGetDTO";
import {OrganizationsService} from "opensilex-core/api/organizations.service";
import {ExperimentsService} from "opensilex-core/api/experiments.service";
import {DevicesService} from "opensilex-core/api/devices.service";
import {PositionsService} from "opensilex-core/api/positions.service";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import AssociatedExperimentsList from "../experiments/AssociatedExperimentsList.vue";

@Component
export default class FacilityView extends Vue {
  $opensilex: OpenSilexVuePlugin;

  selected: InfrastructureGetDTO = null;
  experiments: Array<ExperimentGetListDTO> = [];
  devices: Array<DeviceGetDTO> = [];
  uri = null;
  organizationService: OrganizationsService;
  experimentService: ExperimentsService;
  deviceService: DevicesService;
  positionService: PositionsService;

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
    this.positionService = this.$opensilex.getService(
        "opensilex.PositionsService"
    );
    this.refresh();
  }

  refresh() {
    this.organizationService
      .getInfrastructureFacility(this.uri)
      .then((http: HttpResponse<OpenSilexResponse<InfrastructureGetDTO>>) => {
        let detailDTO: InfrastructureGetDTO = http.response.result;
        this.selected = detailDTO;

        this.loadExperiments();
        this.loadDevices();
      });
  }

  loadExperiments() {
    this.experiments = [];
    return this.experimentService
        .searchExperiments(
            undefined, // label
            undefined, // year
            undefined, // isEnded
            undefined, // species
            undefined, // factorCategories
            undefined, // projects
            undefined, // isPublic
            [this.uri],
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
        )
        .catch(this.$opensilex.errorHandler);
  }

  loadDevices() {
    this.devices = [];
    this.deviceService.searchDevices()
      .then(
          (
            http: HttpResponse<OpenSilexResponse<Array<DeviceGetDTO>>>
          ) => {
            if (http && http.response) {
              http.response.result.forEach(dto => {
                this.loadDeviceIfRelatedToFacility(dto);
              })
            }
          }
      )
      .catch(this.$opensilex.errorHandler);
  }

  // TODO: rework uri comparison
  loadDeviceIfRelatedToFacility(dto : DeviceGetDTO){
    // Get moves with the device uri (target) by date in descending order
    this.positionService.searchPositionHistory(
        dto.uri,
        undefined,
        undefined,
        ["end=desc"],
        undefined,
        undefined
    )
      .then(
          (
            http: HttpResponse<OpenSilexResponse<Array<PositionGetDTO>>>
          ) => {
            console.log(http);
            if (!http && !http.response && http.response.result.length === 0) {
              return;
            }
            let lastPosition = http.response.result[0];
            if (!lastPosition) {
              return;
            }

            let isMatchUri = this.$opensilex.checkURIs(lastPosition.to.uri, this.uri)
            if (isMatchUri) {
              this.devices.push(dto);
            }
      })
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

