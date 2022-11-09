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
    <div class="facilityDescription">
      <div class="col-md-12">
        <opensilex-FacilityDetail
          :selected="selected"
          :withActions="true"
          @onUpdate="refresh"
        >
        </opensilex-FacilityDetail>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Ref, Watch } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
// @ts-ignore
import { OrganizationGetDTO } from "opensilex-core/index";

@Component
export default class FacilityView extends Vue {
  $opensilex: any;

  selected: OrganizationGetDTO = null;
  uri = null;
  service;

  @Ref("infrastructureFacilityForm") readonly infrastructureFacilityForm!: any;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  created() {
    this.uri = decodeURIComponent(this.$route.params.uri);
    this.service = this.$opensilex.getService(
      "opensilex-core.OrganizationsService"
    );
    this.refresh();
  }

  refresh() {
    this.service
      .getInfrastructureFacility(this.uri)
      .then((http: HttpResponse<OpenSilexResponse<OrganizationGetDTO>>) => {
        let detailDTO: OrganizationGetDTO = http.response.result;
        this.selected = detailDTO;
      });
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

