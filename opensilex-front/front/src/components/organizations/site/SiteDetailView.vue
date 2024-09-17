<!--
  - ******************************************************************************
  -                         SiteDetailView.vue
  - OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
  - Copyright © INRAE 2024.
  - Last Modification: 14/06/2024 13:29
  - Contact: yvan.roux@inrae.fr
  - ******************************************************************************
  -->

<template>
  <div class="container-fluid" v-if="selected">
    <opensilex-PageHeader
        icon="fa#map-marker-alt"
        :title="selected.name"
        :description="selected.rdf_type_name"
        class="detail-element-header"
    ></opensilex-PageHeader>
    <opensilex-PageActions :tabs="false" :returnButton="true">
    </opensilex-PageActions>
    <div id="detail-content">
      <div id="left-side">
        <opensilex-SiteDetail
            :selected="selected"
            :withActions="true"
            @onUpdate="refresh"
            @onDelete="redirectToSites"
        />
      </div>

      <div id="right-side">
        <opensilex-FacilitiesView
            :withActions="true"
            :site="selected"
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
  </div>
</template>

<script lang="ts">
import {Component} from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, {OpenSilexResponse} from "../../../lib/HttpResponse";
import {OrganizationsService} from "opensilex-core/api/organizations.service";
import {SiteGetDTO} from 'opensilex-core/index';
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import VueRouter from "vue-router";

@Component
export default class SiteDetailView extends Vue {
  //#region Plugin & Services
  public $opensilex: OpenSilexVuePlugin;
  private service: OrganizationsService;
  public $router: VueRouter;
  //#endregion

  //#region Data
  private selected: SiteGetDTO = null;
  private uri = null;
  //#endregion

  //#region Events handlers
  private redirectToSites() {
    this.$router.push({path: "/sites"});
  }

  //#endregion

  //#region Hooks
  private created() {
    this.uri = decodeURIComponent(this.$route.params.uri);
    this.service = this.$opensilex.getService("opensilex-core.OrganizationsService");
    this.refresh();
  }

  //#endregion

  //#region Private methods
  private refresh() {
    this.service
        .getSite(this.uri)
        .then((http: HttpResponse<OpenSilexResponse<SiteGetDTO>>) => {
          this.selected = http.response.result;
        })
  }

  //#endregion
}
</script>

<style scoped lang="scss">
#right-side{
  margin-top: 1.2vh;
}

#detail-content {
  display: flex;
  justify-content: space-between;
}

#detail-content > * {
  width: 48%;
}
</style>

<i18n>
en:
  SiteDetailView:
    create-facility: "Create facility"
fr:
  SiteDetailView:
    create-facility: "Créer une installation environnementale"
</i18n>
