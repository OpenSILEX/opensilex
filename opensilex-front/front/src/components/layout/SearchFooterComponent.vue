<template>
  <div id="searchFooter" class="footer">
    <div class="row justify-content-end">
      <b-input id="searchBar"
        @change="search"
      >

      </b-input>
    </div>
  </div>
</template>

<script lang="ts">
import { Component } from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import { versionInfoDTO } from "opensilex-core/index";

@Component
export default class SearchFooterComponent extends Vue {
  $opensilex: any;
  $store: any;

  versionInfo: versionInfoDTO;

  created() {
    this.versionInfo = this.$opensilex.versionInfo;
  }

  search(text: String) {
    console.debug(text);
  }

}
</script>

<style scoped lang="scss">

#searchFooter {
  padding-top: 10px;
  padding-bottom: 10px;
  justify-content: flex-end;
}

#searchBar {
  max-width: 20%;
  min-width: 200px;
}

</style>
