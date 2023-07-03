
<template>
    <div class="imagesGrid">
      <opensilex-ImageSingle
        v-for="(image, index) in images"
        v-bind:key="index"
        :image="image"
        :index="index"
        @click=" $emit('click', $event)"
        @annotate=" $emit('annotate', $event)"
      ></opensilex-ImageSingle>
    </div>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";
import {DataFileImageDTO} from "../data/DataFileImageDTO";

@Component
export default class ImageGrid extends Vue {
  @Prop()
  images: Array<DataFileImageDTO>;
}
</script>

<style scoped lang="scss">
.imagesGrid {
  display: flex;
  flex-wrap: wrap;
}
</style>
