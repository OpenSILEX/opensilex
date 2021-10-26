<template>
  <div class="main-content">
    <div>
      <!-- <h5><b-button @click="getGuideFile">{{ $t("component.header.download-user-guide") }}</b-button></h5> -->
      <b-img @click="getGuideFile" height="500" width="650" thumbnail v-bind:src="$opensilex.getResourceURI('images/userguide.png')" alt="component.header.download-user-guide"></b-img>
    </div>
  </div> 
</template>

<script lang="ts">
import { Component, Ref } from 'vue-property-decorator';
import Vue from 'vue';

@Component
export default class DefaultHomeComponent extends Vue {
  $opensilex: any;

  getGuideFile() {
    let path = this.$opensilex.getResourceURI('documents/GuideOpenSilex_V1-1Oct21.pdf');
    fetch(path)
      .then((response) => response.blob())
      .then(function(blob){
        var fileURL = URL.createObjectURL(blob);
        var fileLink = document.createElement('a');
        fileLink.href = fileURL;
        fileLink.setAttribute('download', 'GuideOpenSilex_V1-1Oct21.pdf');
        document.body.appendChild(fileLink);
        fileLink.click();
      }) 
  }

}
</script>

<style scoped lang="scss">
</style>
