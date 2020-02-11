<template>
    <div>
        <phis2ws-ImageGrid :images="images"></phis2ws-ImageGrid>
        <phis2ws-ImageCarousel :images="images"></phis2ws-ImageCarousel>
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
