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
  </div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
// @ts-ignore
import { OrganizationGetDTO } from "opensilex-core/index";

@Component
export default class OrganizationDetailView extends Vue {
  $opensilex: any;

  selected: OrganizationGetDTO = null;
  uri = null;
  service;

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
</style>

