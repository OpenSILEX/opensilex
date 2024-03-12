<template>
  <vl-layer-vector render-mode="image">
    <vl-source-vector :features.sync="data.geometry.type"></vl-source-vector>
  </vl-layer-vector>
</template>

<script lang="ts">
import { Component } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class GeoData extends Vue {}
</script>

<style></style>
