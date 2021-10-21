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
      <div class="col-md-6">
        <!-- Infrastructure detail -->
        <opensilex-InfrastructureDetail
          :selected="selected"
          :withActions="true"
          @onDelete="deleteInfrastructure"
          @onUpdate="refresh"
        ></opensilex-InfrastructureDetail>
      </div>
      <div class="col-md-6">
        <!-- Infrastructure facilities -->
        <opensilex-InfrastructureFacilitiesView
          :selected="selected"
          @onUpdate="refresh"
          @onCreate="refresh"
          @onDelete="refresh"
        ></opensilex-InfrastructureFacilitiesView>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
// @ts-ignore
import { InfrastructureGetDTO } from "opensilex-core/index";

@Component
export default class InfrastructureDetailView extends Vue {
  $opensilex: any;

  selected = null;
  uri = null;
  service;

  created() {
    this.uri = decodeURIComponent(this.$route.params.uri);
    this.service = this.$opensilex.getService(
      "opensilex-core.OrganisationsService"
    );
    this.refresh();
  }

  refresh() {
    this.service
      .getInfrastructure(this.uri)
      .then((http: HttpResponse<OpenSilexResponse<InfrastructureGetDTO>>) => {
        let detailDTO: InfrastructureGetDTO = http.response.result;
        this.selected = detailDTO;
      });
  }

  deleteInfrastructure() {
    this.service
      .deleteInfrastructure(this.uri)
      .then(() => {
        this.$router.push({
          path: "/infrastructures",
        });
      })
      .catch(this.$opensilex.errorHandler);
  }
}
</script>

<style scoped lang="scss">
</style>

