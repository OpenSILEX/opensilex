
<template>
<div class="row">
   <ul  class="images" >
    <opensilex-VisuImageSingle
      v-for="(image, index) in images"
      v-bind:key="index"
      :image="image"
      :index="index"
    ></opensilex-VisuImageSingle>
  </ul>
</div>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class VisuImageGrid extends Vue {
  @Prop()
  images: any;
}
</script>

<style scoped lang="scss">
.row {
  margin-left: 0px;
}
.images {
    margin: 0;
    padding: 0;
    white-space: nowrap;
    width:100%;
   
    overflow-x: auto;
}
</style>
