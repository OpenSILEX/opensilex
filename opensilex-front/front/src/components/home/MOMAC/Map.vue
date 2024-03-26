<template>
  <vl-map
    :load-tiles-while-animating="true"
    :load-tiles-while-interacting="true"
    data-projection="EPSG:4326"
    style="height: 40%, width: 100%"
  >
    <vl-view
      :zoom.sync="zoom"
      :center.sync="center"
      :rotation.sync="rotation"
      :minZoom="minZoom"
    ></vl-view>
    <vl-layer-tile id="osm">
      <vl-source-osm></vl-source-osm>
    </vl-layer-tile>
    <GeoDataComponent></GeoDataComponent>
  </vl-map>
</template>

<script lang="ts">
import { Component } from "vue-property-decorator";
import Vue from "vue";
import GeoData from "./GeoData.vue";

@Component({
  components: {
    GeoDataComponent: GeoData,
  },
})
export default class Map extends Vue {
  zoom = 2;
  minZoom = 2;
  center = [0, 0];
  rotation = 0;
}
</script>

<style></style>
