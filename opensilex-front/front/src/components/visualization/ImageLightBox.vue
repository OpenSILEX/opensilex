
<template>
    <div class="row">
      <vue-easy-lightbox
      :visible="visible"
      :imgs="imgs"
      :index="index"
      @hide="visible = false"
    ></vue-easy-lightbox>
    </div>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class ImageLightBox extends Vue {
  @Prop()
  images: any;

  visible = false;
  imgs = [];
  index = 0;

  created() {
    this.imgs = this.images.map(image => image.url);
  }

  openOnIndex(index){
    this.index = index ;
    this.visible = true;
  }
}
</script>

<style scoped lang="scss">
</style>
