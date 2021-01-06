<template>
  <div v-if="uri" class="container-fluid">
    <opensilex-PageHeader
      :description="name"
      icon="ik#ik-globe"
      title="InfrastructureDetailView.page-title"
    ></opensilex-PageHeader>

    <opensilex-PageActions :tabs="true" :returnButton="true">
      <template v-slot>
        <b-nav-item
          :active="isDetailsTab()"
          :to="{ path: '/infrastructures/details/' + encodeURIComponent(uri) }"
          >{{ $t("ExperimentView.details") }}</b-nav-item
        >
        <b-nav-item
          :active="isDataTab()"
          :disabled="true"
          :to="{ path: '/infrastructures/data/' + encodeURIComponent(uri) }"
          >{{ $t("ExperimentView.data") }}</b-nav-item
        >
        <b-nav-item
          :active="isMap()"
          :disabled="true"
          :to="{ path: '/infrastructures/map/' + encodeURIComponent(uri) }"
          >{{ $t("Map") }}
        </b-nav-item>
      </template>
    </opensilex-PageActions>

    <opensilex-PageContent>
      <template v-slot>
        <opensilex-InfrastructureDetailScientificObjects
          v-if="isDetailsTab()"
          :uri="uri"
          :name.sync="name"
        ></opensilex-InfrastructureDetailScientificObjects>
        <!-- <opensilex-ExperimentData
          v-else-if="isDataTab()"
          :uri="uri"
        ></opensilex-ExperimentData>
        <opensilex-MapView :uri="uri" v-else-if="isMap()"></opensilex-MapView> -->
      </template>
    </opensilex-PageContent>
  </div>
</template>

<script lang="ts">
import { Component } from "vue-property-decorator";
import Vue from "vue";
import { ExperimentsService, ExperimentGetDTO } from "opensilex-core/index";

import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";
@Component
export default class InfrastructureDetailView extends Vue {
  $route: any;

  $opensilex: any;
  service: ExperimentsService;
  uri = null;
  name: string = "";

  created() {
       this.uri = decodeURIComponent(this.$route.params.uri);
  }

  isDetailsTab() {
    return this.$route.path.startsWith("/infrastructures/details/");
  }

  isMap() {
    return this.$route.path.startsWith("/infrastructures/map/");
  }

  isDataTab() {
    return this.$route.path.startsWith("/infrastructures/data/");
  }
}
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  InfrastructureDetailView:
    details: Details
    scientific-objects: Scientific objects
    data: Data
fr:
  InfrastructureDetailView:
    details: Détail
    scientific-objects: Objets scientifiques
    data: Data
</i18n>