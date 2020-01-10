
<template>
  <div>
    <div class="row">
      <phis2ws-Image v-for="(image, index) in images" v-bind:key="index" :image="image.uri">
       
      </phis2ws-Image>
    </div>
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
