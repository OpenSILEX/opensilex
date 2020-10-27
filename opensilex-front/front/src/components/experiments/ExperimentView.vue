<template>
  <div v-if="uri" class="container-fluid">
    <opensilex-PageHeader
      icon="ik#ik-layers"
      :title="name"
      description="component.experiment.view.title"
    ></opensilex-PageHeader>

    <opensilex-PageActions :tabs="true" :returnButton="true">
      <template v-slot>
        <b-nav-item
          :active="isDetailsTab()"
          :to="{path: '/experiment/details/' + encodeURIComponent(uri)}"
        >{{ $t('ExperimentView.details') }}</b-nav-item>
        <b-nav-item
          :active="isScientificObjectsTab()"
          :to="{path: '/experiment/scientific-objects/' + encodeURIComponent(uri)}"
        >{{ $t('ExperimentView.scientific-objects') }}</b-nav-item>
        <!--   <b-nav-item
         :active="isDataTab()"
         :to="{path: '/experiment/data/' + encodeURIComponent(uri)}"
       >{{ $t('ExperimentView.data') }}</b-nav-item> -->
        <b-nav-item
            :active="isMap()"
            :to="{path: '/experiment/map/'+ encodeURIComponent(uri)}"
        >{{ $t('Map') }}
        </b-nav-item>
      </template>
    </opensilex-PageActions>

    <opensilex-PageContent>
      <template v-slot>
        <opensilex-ExperimentDetail v-if="isDetailsTab()" :uri="uri"></opensilex-ExperimentDetail>
        <opensilex-ExperimentScientificObjects v-else-if="isScientificObjectsTab()" :uri="uri"></opensilex-ExperimentScientificObjects>
        <opensilex-ExperimentData v-else-if="isDataTab()" :uri="uri"></opensilex-ExperimentData>
        <opensilex-GeometryView :uri="uri" v-else-if="isMap()"></opensilex-GeometryView>
      </template>
    </opensilex-PageContent>
  </div>
</template>

<script lang="ts">
import { Component } from "vue-property-decorator";
import Vue from "vue";
import {
  ExperimentsService,ExperimentGetDTO
} from "opensilex-core/index";

import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";
@Component
export default class ExperimentView extends Vue {
  $route: any;

  $opensilex: any;
  service: ExperimentsService;
  uri = null;
  name: string = "";

  created() {
     this.service = this.$opensilex.getService("opensilex.ExperimentsService");
   
    this.uri = decodeURIComponent(this.$route.params.uri);
  if (this.uri) {
      this.service
        .getExperiment(this.uri)
        .then((http: HttpResponse<OpenSilexResponse<ExperimentGetDTO>>) => {
          this.name = http.response.result.label;
        })
        .catch(error => {
          this.$opensilex.errorHandler(error);
        });
    }
  }

  isDetailsTab() {
    return this.$route.path.startsWith("/experiment/details/");
  }

  isMap() {
    return this.$route.path.startsWith("/experiment/map/");
  }

  isScientificObjectsTab() {
    return this.$route.path.startsWith("/experiment/scientific-objects/");
  }

  isDataTab() {
    return this.$route.path.startsWith("/experiment/data/");
  }
}
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  ExperimentView:
    details: Details
    scientific-objects: Scientific objects
    data: Data

fr:
  ExperimentView:
    details: Détail
    scientific-objects: Objets scientifiques
    data: Data
</i18n>