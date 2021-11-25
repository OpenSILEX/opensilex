<template>
  <div class="container-fluid">
    <opensilex-PageHeader
      icon="ik#ik-globe"
      title="component.menu.environmental-data"
      :isExperimentalFeature="true"
    ></opensilex-PageHeader>
    HELLO
  </div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class EnvironmentalDataView extends Vue {

}
</script>
