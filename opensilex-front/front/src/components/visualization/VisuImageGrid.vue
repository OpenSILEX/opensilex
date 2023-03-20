<template>
  <div class="row">
    <ul class="images">
      <opensilex-VisuImageSingle
          v-for="(image, index) in images"
          v-bind:key="index"
          :image="image"
          :index="index"
          :id="image.imageIndex"
          ref="visuImageSingle"
          @imageIsHovered=" $emit('imageIsHovered', $event)"
          @imageIsUnHovered=" $emit('imageIsUnHovered', $event)"
          @imageIsDeleted="$emit('imageIsDeleted', $event)"
          @onImageAnnotate="$emit('onImageAnnotate', $event)"
          @onImageDetails="$emit('onImageDetails', $event)"
          @onAnnotationDetails="$emit('onAnnotationDetails', $event)"
          @imageIsClicked="$emit('imageIsClicked',$event)"

      ></opensilex-VisuImageSingle>
    </ul>
  </div>
</template>

<script lang="ts">
import {Component, Prop, Ref} from "vue-property-decorator";
import Vue from "vue";

@Component
export default class VisuImageGrid extends Vue {
  @Prop()
  images: any;

  @Ref("visuImageSingle") readonly visuImageSingle!: any;

  onImagePointMouseEnter(toSend) {
    if (this.visuImageSingle) {
      this.visuImageSingle.forEach(element => {
        element.onImagePointMouseEnter(toSend);
      });
    }
  }

  onImagePointMouseOut() {
    if (this.visuImageSingle) {
      this.visuImageSingle.forEach(element => {
        element.onImagePointMouseOut();
      });
    }
  }

  onImagePointClick(toReturn) {
    if (this.visuImageSingle) {
      this.visuImageSingle.forEach(element => {
        element.onImagePointClick(toReturn);
        let el = document.getElementById(toReturn.imageIndex);
        if (el) {
          el.scrollIntoView(true);
        }
      });
    }
  }
}
</script>

<style scoped lang="scss">
.row {
  margin-left: 0px;
  margin-bottom: 15px;
}

.images {
  margin: 0;
  padding: 0;
  white-space: nowrap;
  width: 100%;

  overflow-x: auto;
  scroll-behavior: smooth;
}
</style>
