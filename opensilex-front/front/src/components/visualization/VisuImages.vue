<template>
  <div>
    <opensilex-VisuImageGrid
      ref="visuImageGrid"
      :key="key"
      :images="images"
      @imageIsHovered=" $emit('imageIsHovered', $event)"
      @imageIsUnHovered=" $emit('imageIsUnHovered', $event)"
      @imageIsDeleted="$emit('imageIsDeleted', $event)"
      @imageIsClicked="onImageIsClicked"
    ></opensilex-VisuImageGrid>
    <b-modal id="modal-center" v-model="show" hide-footer centered no-fade size="sm">
      <b-carousel
        ref="myCarousel"
        id="carousel-1"
        v-model="slide"
        :interval="0"
        controls
        indicators
        background="#ababab"
        style="text-shadow: 1px 1px 2px #333;"
      >
        <b-carousel-slide v-for="(image, index) in images" v-bind:key="index" :img-src="image.uri">
          <p>{{getObjectType(image)}}</p>
          <p v-if="image.objectAlias">{{image.objectAlias}}</p>
          <p v-if="!image.objectAlias">{{image.objectUri}}</p>
          <p>{{formatedDate(image.date)}}</p>
        </b-carousel-slide>
      </b-carousel>
    </b-modal>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class VisuImages extends Vue {
  key = 0; // fix VisuImageGrid reload
  images = [];
  image: any;
  slide = 0;
  show: boolean = false;
  showImages: boolean = true;
  objectType: string = "";
  objectUri: string = "";
  formatedDateValue: string = "";

  @Ref("visuImageGrid") readonly visuImageGrid!: any;

  onImagePointMouseEnter(toSend) {
    this.visuImageGrid.onImagePointMouseEnter(toSend);
  }

  onImagePointMouseOut() {
    this.visuImageGrid.onImagePointMouseOut();
  }

  onImageIsDeleted(index) {
    this.deleteImage(index);
  }

  onImageIsClicked(index) {
    this.slide = index;
    this.image = this.images[index].uri;
    this.show = true;
  }

  addImage(image) {
    this.images.push(image);
  }

  deleteImage(index) {
    let newArray = JSON.parse(JSON.stringify(this.images));
    newArray.splice(index, 1);
    this.images = newArray;
    this.key++;
  }

  getObjectType(image) {
    return image.objectType.split("#")[1];
  }

  formatedDate(date) {
    const newDate = new Date(date);
    const options: Intl.DateTimeFormatOptions = {
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




<style scoped >
div >>> .carousel-control-prev-icon,
div >>> .carousel-control-next-icon {
  width: 80px;
  height: 80px;
}
div >>> .modal-body {
  padding: 0 !important;
}

.carousel-inner img {
  margin: auto;
}

@media (min-width: 576px) {
  .modal-dialog-centered {
    min-height: calc(100% - 3.5rem);
  }
}

@media (min-width: 576px) {
  div >>> .modal-sm {
    max-height: 500px;
    max-width: 500px;
  }
}

.carousel-item:after {
  content: "";
  position: absolute;
  left: 0;
  bottom: 0;
  width: 100%;
  height: 40%;
  display: inline-block;
  background: -moz-linear-gradient(
    top,
    rgba(0, 0, 0, 0) 0%,
    rgba(0, 0, 0, 0.65) 100%
  ); /* FF3.6+ */
  background: -webkit-gradient(
    linear,
    left top,
    left bottom,
    color-stop(0%, rgba(0, 0, 0, 0.65)),
    color-stop(100%, rgba(0, 0, 0, 0))
  ); /* Chrome,Safari4+ */
  background: -webkit-linear-gradient(
    top,
    rgba(0, 0, 0, 0) 0%,
    rgba(0, 0, 0, 0.65) 100%
  ); /* Chrome10+,Safari5.1+ */
  background: -o-linear-gradient(
    top,
    rgba(0, 0, 0, 0) 0%,
    rgba(0, 0, 0, 0.65) 100%
  ); /* Opera 11.10+ */
  background: -ms-linear-gradient(
    top,
    rgba(0, 0, 0, 0) 0%,
    rgba(0, 0, 0, 0.65) 100%
  ); /* IE10+ */
  background: linear-gradient(
    to bottom,
    rgba(0, 0, 0, 0) 0%,
    rgba(0, 0, 0, 0.65) 100%
  ); /* W3C */
  filter: progid:DXImageTransform.Microsoft.gradient( startColorstr='#a6000000', endColorstr='#00000000',GradientType=0 ); /* IE6-9 */
}

p {
  margin-bottom: 0px;
}
.row {
  margin-left: 0px;
}
.images {
  margin: 0;
  padding: 0;
  white-space: nowrap;
  width: 100%;

  overflow-x: auto;
}
</style>
