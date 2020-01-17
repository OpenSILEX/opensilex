<template>
  <div class="col-lg-2 col-md-3 col-sm-4">
    <b-card :img-src="link" img-alt="Image" img-top tag="article" style="max-width: 10rem;" class>
      <b-card-text>{{formatedDateValue}}</b-card-text>
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
  formatedDateValue = "";
  created() {
    this.link =
      this.$opensilex.getBaseApi() +
      "/data/file/" +
      encodeURIComponent(this.image.uri);
    this.formatedDateValue = this.formatedDate(this.image.date);
  }
  formatedDate(date) {
    const newDate = new Date(date);

    const options = {
      year: "numeric",
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit"
    };
    return newDate.toLocaleDateString("fr-FR", options);
  }
}
</script>

<style scoped lang="scss">
img {
  width: 100%;
  max-width: 400px;
}
</style>
