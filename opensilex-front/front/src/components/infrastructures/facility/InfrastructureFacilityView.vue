<template>
  <div class="container-fluid" v-if="selected">
    <opensilex-PageHeader
      icon="ik#ik-globe"
      :title="selected.name"
      :description="selected.rdf_type_name"
    ></opensilex-PageHeader>
    <opensilex-PageActions :tabs="false" :returnButton="true">
    </opensilex-PageActions>
    <div class="row">
      <div class="col-md-12">
        <opensilex-OrganizationFacilityDetail
          :selected="selected"
          :withActions="true"
          @onUpdate="refresh"
        >
        </opensilex-OrganizationFacilityDetail>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Ref, Watch } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../../lib/HttpResponse";
// @ts-ignore
import { InfrastructureGetDTO } from "opensilex-core/index";

@Component
export default class InfrastructureFacilityView extends Vue {
  $opensilex: any;

  selected: InfrastructureGetDTO = null;
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
      "opensilex-core.OrganisationsService"
    );
    this.refresh();
  }

  refresh() {
    this.service
      .getInfrastructureFacility(this.uri)
      .then((http: HttpResponse<OpenSilexResponse<InfrastructureGetDTO>>) => {
        let detailDTO: InfrastructureGetDTO = http.response.result;
        this.selected = detailDTO;
      });
  }
}
</script>

<style scoped lang="scss">
</style>

