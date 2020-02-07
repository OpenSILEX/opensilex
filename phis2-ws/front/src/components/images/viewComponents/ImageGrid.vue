
<template>
    <div class="row">
      <phis2ws-ImageSingle
        v-for="(image, index) in images"
        v-bind:key="index"
        :image="image"
        :index="index"
      ></phis2ws-ImageSingle>
    </div>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class ImageGrid extends Vue {
  @Prop()
  images: any;
}
</script>

<style scoped lang="scss">
</style>
