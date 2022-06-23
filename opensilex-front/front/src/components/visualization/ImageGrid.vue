
<template>
    <div class="row">
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

@Component
export default class ImageGrid extends Vue {
  @Prop()
  images: any;
}
</script>

<style scoped lang="scss">
</style>
