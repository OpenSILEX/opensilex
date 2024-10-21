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
    <div class="detail-content">
      <div class="left-side">
        <!-- Organization detail -->
        <opensilex-OrganizationDetail
          :selected="selected"
          :withActions="true"
          @onDelete="deleteOrganization"
          @onUpdate="refresh"
        ></opensilex-OrganizationDetail>

        <!-- Site list -->
        <b-card>
          <opensilex-SiteView
              :organizationsForFilter="[selected.uri]"
          />
        </b-card>
      </div>
      <div class="right-side">
        <!-- Organization facilities -->
        <opensilex-FacilitiesView
            :withActions="true"
            :organization="selected"
            :isSelectable="false"
            :facilities="selected.facilities"
            :fetchAndShowCurrentExperiments="true"
            createButtonLabel="SiteDetailView.create-facility"
            @onUpdate="refresh"
            @onCreate="refresh"
            @onDelete="refresh"
        ></opensilex-FacilitiesView>
      </div>
    </div>
    <opensilex-Card
        :noFooter="true"
    >
      <template v-slot:header>
        <h3>
          <opensilex-Icon icon="ik#ik-map-pin" size="lg" />
          {{ $t("OrganizationDetailView.site-map") }}
          &nbsp;
          <font-awesome-icon
              icon="question-circle"
              class="help-label"
              v-b-tooltip.hover.top="$t('OrganizationDetailView.site-map-help')"
          />
        </h3>
      </template>
      <template v-slot:body>
        <opensilex-MapCard
            class="site-map"
            :features="siteFeatures"
        />
      </template>
    </opensilex-Card>
  </div>
</template>

<script lang="ts">
import {Component} from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, {OpenSilexResponse} from "../../lib/HttpResponse";
import {OrganizationGetDTO} from "opensilex-core/index";
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
    this.getOrganization().then(() => {
      this.getSitesFeatures();
    });
  }

  private async getOrganization(): Promise<void> {
    await this.service
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

  private async getSitesFeatures(): Promise<void> {
    const sites = this.selected.sites;
    if (sites.length > 0) {
      const sitesUri: Array<string> = sites.map((site) => site.uri);
      const sitesWithLocation = await this.service.getSitesWithLocation(sitesUri);
      this.siteFeatures = this.convertSiteModelsIntoFeatures(sitesWithLocation.response.result);
    }
  }

  private convertSiteModelsIntoFeatures(sites): feature[] {
    const features: feature[] = [];

    sites.forEach(siteModel => {
      let feature = siteModel.geometry;
      feature.properties = siteModel;
      feature.id = siteModel.uri;
      delete feature.properties.geometry;
      features.push(feature)
    })

    return features;
  }

  //#endregion
}
</script>

<style scoped lang="scss">
.detail-content {
  display: flex;
  justify-content: space-between;
}

.detail-content>* {
  width: 49%;
}

.left-side {
  margin-top: 2.4vh;
}

.site-map {
  height: 40vh;
}

.help-label {
  font-size: 1.3em;
  background: #f1f1f1;
  color: #00A38D;
  border-radius: 50%;
}
</style>

<i18n>
en:
    OrganizationDetailView:
        site-map: "Sites"
        site-map-help: "Display only the sites with a location (address field)"
fr:
    OrganizationDetailView:
        site-map: "Sites"
        site-map-help: "Affiche uniquement les sites avec une localisation (champ adresse)"
</i18n>

