<template>
    <div>
        <phis-ImageGrid :images="images"></phis-ImageGrid>
        <phis-ImageCarousel :images="images"></phis-ImageCarousel>
    </div>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class ImageList extends Vue {
  @Prop()
  images: any;
}
</script>





<style scoped lang="scss">
</style>
