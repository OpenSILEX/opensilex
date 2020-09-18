<template>
  <div v-if="uri" class="container-fluid">
    <opensilex-PageHeader icon="ik#ik-layers" title="component.experiment.view.title"></opensilex-PageHeader>

    <opensilex-PageActions :returnButton="true" :returnTo="{path: '/experiments'}">
      <template v-slot>
        <b-nav-item
          :active="isDetailsTab()"
          :to="{path: '/experiment/details/' + encodeURIComponent(uri)}"
        >{{ $t('Details') }}</b-nav-item>
        <b-nav-item
          :active="isScientificObjectsTab()"
          :to="{path: '/experiment/scientific-objects/' + encodeURIComponent(uri)}"
        >{{ $t('Objets scientifiques') }}</b-nav-item>
      </template>
    </opensilex-PageActions>

    <opensilex-PageContent>
      <template v-slot>
        <opensilex-ExperimentDetail v-if="isDetailsTab()" :uri="uri"></opensilex-ExperimentDetail>
        <opensilex-ExperimentScientificObjects v-else-if="isScientificObjectsTab()" :uri="uri"></opensilex-ExperimentScientificObjects>
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
  uri = null;

  created() {
    this.uri = this.$route.params.uri;
  }

  isDetailsTab() {
    return this.$route.path.startsWith("/experiment/details/");
  }

  isScientificObjectsTab() {
    return this.$route.path.startsWith("/experiment/scientific-objects/");
  }

  goBack() {
    window.history.back();
  }
}
</script>

<style scoped lang="scss">
</style>
