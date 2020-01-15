<template>
  <div class=" col-lg-2 col-md-3 col-sm-4">
    <b-card
      :img-src="link"
      img-alt="Image"
      img-top
      tag="article"
      style="max-width: 10rem;"
      class
    >
      <b-card-text>
        {{image.date}}
      </b-card-text>
    </b-card>
  </div>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class ImageSingle extends Vue {
  @Prop()
  image: any;
  $opensilex: any;

  link: any = "";

  created() {
    this.link =
      this.$opensilex.getBaseApi() +
      "/data/file/" +
      encodeURIComponent(this.image.uri);
  }
}
</script>

<style scoped lang="scss">
</style>
