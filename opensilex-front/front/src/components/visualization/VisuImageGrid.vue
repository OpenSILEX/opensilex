
<template>
<div class="row">
   <ul  class="images" >
    <opensilex-VisuImageSingle
      v-for="(image, index) in images"
      v-bind:key="index"
      :image="image"
      :index="index"
      ref="visuImageSingle"
      @imageIsHovered=" $emit('imageIsHovered', $event)"
      @imageIsUnHovered=" $emit('imageIsUnHovered', $event)"
      @imageIsDeleted="$emit('imageIsDeleted', $event)"
      @onImageAnnotate="$emit('onImageAnnotate', $event)"
      @onImageDetails="$emit('onImageDetails', $event)"
      @imageIsClicked="$emit('imageIsClicked',$event)"

    ></opensilex-VisuImageSingle>
  </ul>
</div>
</template>

<script lang="ts">
import { Component, Prop,Ref } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class VisuImageGrid extends Vue {
  @Prop()
  images: any;

  @Ref("visuImageSingle") readonly visuImageSingle!: any;

  onImagePointMouseEnter(toSend){
    if(this.visuImageSingle){
       this.visuImageSingle.forEach(element => {
       element.onImagePointMouseEnter(toSend);
    });
    }
  }

  onImagePointMouseOut(){
    if(this.visuImageSingle){
       this.visuImageSingle.forEach(element => {
       element.onImagePointMouseOut();
    });
    }
  }
}
</script>

<style scoped lang="scss">
.row {
  margin-left: 0px;
}
.images {
    margin: 0;
    padding: 0;
    white-space: nowrap;
    width:100%;
   
    overflow-x: auto;
}
</style>
