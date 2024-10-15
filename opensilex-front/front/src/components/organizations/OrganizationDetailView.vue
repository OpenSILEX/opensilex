<template>
  <div class="container-fluid" v-if="selected">
    <opensilex-PageHeader
      icon="ik#ik-globe"
      :title="selected.name"
      :description="selected.rdf_type_name"
      class="detail-element-header"
    ></opensilex-PageHeader>
    <opensilex-PageActions :tabs="false" :returnButton="true">
    </opensilex-PageActions>
    <div class="row">
      <div class="col-md-6">
        <!-- Organization detail -->
        <opensilex-OrganizationDetail
          :selected="selected"
          :withActions="true"
          @onDelete="deleteOrganization"
          @onUpdate="refresh"
        ></opensilex-OrganizationDetail>
        <opensilex-MapCard
            :features="siteFeatures"
        />

      </div>
      <div class="col-md-6">
        <!-- Organization facilities -->
        <opensilex-FacilitiesView
          :facilities="selected.facilities"
          :organization="selected"
          :selected="selected"
          :withActions="true"
          :isSelectable="false"
          @onUpdate="refresh"
          @onCreate="refresh"
          @onDelete="refresh"
        ></opensilex-FacilitiesView>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import {Component} from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, {OpenSilexResponse} from "../../lib/HttpResponse";
import {OrganizationGetDTO} from "opensilex-core/index";
import {SiteGetWithGeometryDTO} from "opensilex-core/model/siteGetWithGeometryDTO";
import {feature} from "../geometry/MapCard.vue";

@Component
export default class OrganizationDetailView extends Vue {
  $opensilex: any;

  selected: OrganizationGetDTO = null;
  uri = null;
  service;
  siteFeatures: feature[] = [];

  created() {
    this.uri = decodeURIComponent(this.$route.params.uri);
    this.service = this.$opensilex.getService(
      "opensilex-core.OrganizationsService"
    );
    this.refresh();
  }

  refresh() {
    this.service
      .getOrganization(this.uri)
      .then((http: HttpResponse<OpenSilexResponse<OrganizationGetDTO>>) => {
        let detailDTO: OrganizationGetDTO = http.response.result;
        this.selected = detailDTO;
      });
    this.getSitesFeatures();
  }

  deleteOrganization() {
    this.service
      .deleteOrganization(this.uri)
      .then(() => {
        this.$router.push({
          path: "/organizations",
        });
      })
      .catch(this.$opensilex.errorHandler);
  }

  private getSitesFeatures() {
    this.service.getSitesWithLocation().then((http: HttpResponse<OpenSilexResponse<Array<SiteGetWithGeometryDTO>>>) => {
      let results: SiteGetWithGeometryDTO[] = http.response.result;
      if (http.response.result.length > 0) {
        this.siteFeatures = this.convertObjectIntoGeoJson(results);
      }
    })
  }

  private convertObjectIntoGeoJson(results): feature[] {
    const features: feature[] = [];

    results.forEach(result => {
      let feature = result.geometry;
      feature.properties = result;
      feature.id = result.uri;
      delete feature.properties.geometry;
      features.push(feature)
    })

    return features;
  }
}
</script>

<style scoped lang="scss">
</style>

