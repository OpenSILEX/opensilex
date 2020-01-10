<template>
  <div>
    <b-button
      :class="showSearchComponent ? null : 'collapsed'"
      :aria-expanded="showSearchComponent ? 'true' : 'false'"
      aria-controls="collapse-1"
      @click="onSearchButtonClick"
      variant="primary"
    >Image Search</b-button>

    <b-collapse id="collapse-1" v-model="showSearchComponent" class="mt-2">
      <phis2ws-ImageSearch @onSearchFormSubmit="onSearchFormSubmit"></phis2ws-ImageSearch>
    </b-collapse>

    <b-collapse id="collapse-2" class="mt-2">
      <phis2ws-ImageGrid ref="imageGrid" ></phis2ws-ImageGrid>
    </b-collapse>
  </div>
</template>

<script lang="ts">
import { Component } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";

@Component
export default class ImageView extends Vue {
  showSearchComponent: boolean = true;

  onSearchFormSubmit(form) {
    this.showSearchComponent = false;
    let imageList: any = this.$refs.imageList;
    imageList.loadTable(form);
  }

  onSearchButtonClick() {
    this.showSearchComponent = !this.showSearchComponent;
  }

}
</script>

<style scoped lang="scss">
</style>

