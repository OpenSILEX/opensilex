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
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {OrganizationsService} from "opensilex-core/api/organizations.service";

@Component
export default class OrganizationDetailView extends Vue {
  //#region Plugin and service
  private $opensilex: OpenSilexVuePlugin;
  private service: OrganizationsService;
  //#endregion

  //#region Data
  selected: OrganizationGetDTO = null;
  uri = null;
  siteFeatures: feature[] = [];
  //#endregion

  //#region Hook
  created() {
    this.uri = decodeURIComponent(this.$route.params.uri);
    this.service = this.$opensilex.getService(
      "opensilex-core.OrganizationsService"
    );
    this.refresh();
  }
  //#endregion

  //#region Private methods
  private refresh(): void {
    this.getOrganization().then( () => {
      this.getSitesFeatures(); });
  }

  private async getOrganization(): Promise<void> {
    this.service
        .getOrganization(this.uri)
        .then((http: HttpResponse<OpenSilexResponse<OrganizationGetDTO>>) => {
          let detailDTO: OrganizationGetDTO = http.response.result;
          this.selected = detailDTO;
        })
      .catch(this.$opensilex.errorHandler);
  }

  private deleteOrganization(): void {
    this.service
      .deleteOrganization(this.uri)
      .then(() => {
        this.$router.push({
          path: "/organizations",
        });
      })
      .catch(this.$opensilex.errorHandler);
  }

  private getSitesFeatures(): void {
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
  //#endregion
}
</script>

<style scoped lang="scss">
</style>

