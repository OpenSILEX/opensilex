<template>
  <div v-if="uri" class="container-fluid">
    <opensilex-PageHeader
      icon="ik#ik-layers"
      title="component.experiment.view.title"
      :description="name"
    ></opensilex-PageHeader>

    <opensilex-PageActions :tabs="true" :returnButton="true" :returnTo="{path: '/experiments'}">
      <template v-slot>
        <b-nav-item
          :active="isDetailsTab()"
          :to="{path: '/experiment/details/' + encodeURIComponent(uri)}"
        >{{ $t('ExperimentView.details') }}</b-nav-item>
        <b-nav-item
          :active="isScientificObjectsTab()"
          :to="{path: '/experiment/scientific-objects/' + encodeURIComponent(uri)}"
        >{{ $t('ExperimentView.scientific-objects') }}</b-nav-item>
        <b-nav-item
          :active="isDataTab()"
          :to="{path: '/experiment/data/' + encodeURIComponent(uri)}"
        >{{ $t('ExperimentView.data') }}</b-nav-item>
      </template>
    </opensilex-PageActions>

    <opensilex-PageContent>
      <template v-slot>
        <opensilex-ExperimentDetail v-if="isDetailsTab()" :uri="uri"></opensilex-ExperimentDetail>
        <opensilex-ExperimentScientificObjects v-else-if="isScientificObjectsTab()" :uri="uri"></opensilex-ExperimentScientificObjects>
        <opensilex-ExperimentData v-else-if="isDataTab()" :uri="uri"></opensilex-ExperimentData>
      </template>
    </opensilex-PageContent>
  </div>
</template>

<script lang="ts">
import { Component } from "vue-property-decorator";
import Vue from "vue";
import VueRouter from "vue-router";

@Component
export default class ExperimentView extends Vue {
  $route: any;

  uri = null;
  name: string = "";

  created() {
    this.uri = this.$route.params.uri;
    let query: any = this.$route.query;
    if (query.name) {
      this.name = decodeURIComponent(query.name);
    }
  }

  isDetailsTab() {
    return this.$route.path.startsWith("/experiment/details/");
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