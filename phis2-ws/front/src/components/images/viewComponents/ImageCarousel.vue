<template>
  <b-modal id="modal-center" v-model="show" hide-header hide-footer centered no-fade size="lg">
    <b-carousel
      ref="myCarousel"
      id="carousel-1"
      v-model="slide"
      :interval="0"
      controls
      indicators
      background="#ababab"
      img-width="1024"
      img-height="480"
      style="text-shadow: 1px 1px 2px #333;"
    >
      <!-- Slides with custom text -->
      <b-carousel-slide
        v-for="(image, index) in images"
        v-bind:key="index"
        :img-src="imageCall(image)"
      >
        <h1>Hello world!</h1>
      </b-carousel-slide>
    </b-carousel>
  </b-modal>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";
import { EventBus } from "./../event-bus";

@Component
export default class ImageGrid extends Vue {
  $opensilex: any;
  @Prop()
  images: any;
  slide=0;
  show: boolean = false;

  created() {
    EventBus.$on("imageIsClicked", index => {
      this.slide=index;
      this.show = true;
    });
  }

  imageCall(image) {

    return (
      this.$opensilex.getBaseAPI()+"/data/file/"+encodeURIComponent(image.uri)
    );
  }
}
</script>





<style scoped lang="scss">
</style>
