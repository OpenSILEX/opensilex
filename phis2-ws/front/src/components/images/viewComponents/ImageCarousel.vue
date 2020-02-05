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
        :img-src="image.uri"
      >
        <p>{{getObjectType(image)}}</p> 
        <p>{{image.objectAlias}}</p>
        <p>{{formatedDate(image.date)}}</p>
      </b-carousel-slide>
    </b-carousel>
  </b-modal>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";
import { EventBus } from "./../event-bus";
import { Image } from "./../image";
@Component
export default class ImageGrid extends Vue {
  $opensilex: any;
  @Prop()
  images: Array<Image>;
  slide = 0;
  show: boolean = false;
  objectType: string = "";
  objectUri: string = "";
  formatedDateValue: string = "";

  created() {
    EventBus.$on("imageIsClicked", index => {
      this.slide = index;
      this.show = true;
    });
  }

  getObjectType(image) {
    return image.objectType.split("#")[1];
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
</style>
