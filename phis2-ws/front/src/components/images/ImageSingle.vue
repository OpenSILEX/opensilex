<template>
  <div class="col-lg-3 col-md-4 col-6">
    <a href="#" class="d-block mb-4 h-100">
      <b-img-lazy :src="link" alt="Image 1" ></b-img-lazy>
    </a>
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
    this.link = this.$opensilex.getBaseApi() + "/data/file/" + encodeURIComponent(this.image);
  }
}
</script>

<style scoped lang="scss">
img {
  width: 100%;
  max-width: 400px;
}
</style>
