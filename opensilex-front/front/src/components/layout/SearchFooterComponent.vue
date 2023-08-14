<template>
  <div id="searchFooter" class="footer">
    <div class="row justify-content-end">
      <b-input id="searchBar"
         placeholder="search"
         v-on:change="search"
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
import { OntologyService } from "opensilex-core/api/ontology.service";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";

@Component
export default class SearchFooterComponent extends Vue {
  $opensilex: any;
  $store: any;

  ontologyService: OntologyService;

  versionInfo: versionInfoDTO;

  created() {
    this.versionInfo = this.$opensilex.versionInfo;
    this.ontologyService = this.$opensilex.getService("opensilex.OntologyService");
  }

  search(text: string) {

    if (!text) return;
    console.debug(text);

    this.$opensilex.disableLoader();
    this.ontologyService.searchURIs(text)
        .then((http) => {

        });
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
