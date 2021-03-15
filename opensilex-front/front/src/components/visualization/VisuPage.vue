<template>
  <div class="container-fluid data-container">
    <opensilex-PageHeader
      icon="ik#ik-bar-chart-line"
      title="VisuView.title"
      description="VisuView.description"
    ></opensilex-PageHeader>
      <opensilex-VisuView
      ></opensilex-VisuView>
  </div>
</template>

<script lang="ts">
import { Component } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class VisuPage extends Vue {
}
</script>

<style scoped lang="scss">

</style>
<i18n>
en:
  VisuView:
    title: Visualization   
    description: ..

fr:
  VisuView:
    title: Visualisation
    description: ..
</i18n>