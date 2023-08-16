<template>
  <div id="searchFooter" class="footer">
    <div id="searchBlock" class="row dropup justify-content-end"
         v-on:mouseleave="isVisible = false"
    >
      <b-input id="searchBar"
         placeholder="search"
         :clearable="true"
         v-on:change="search"
         v-on:mouseenter="isVisible = true"
      >
      </b-input>
      <div id="global-results" class="container-fluid scrollable"
        v-if="isVisible"
      >
        <div class="wrapper">
          <div v-if="isNothingFound && !isDataLoading">
            No result
          </div>
          <div v-for="(uri, index) in uriList" v-bind:key="uri">
            <opensilex-UriLink
                :uri="uri"
                :value="uri"
                :allowCopy=true
            >
            </opensilex-UriLink>
          </div>
        </div>
      </div>
    </div>

      <!--
      <opensilex-Overlay :show="isDataLoading">
        <div id="global-results" class="container-fluid scrollable">
          <div class="wrapper">
            <div v-if="isNothingFound && !isDataLoading">
              Nothing found for '{{ this.text }}'
            </div>
            <div v-for="(entity, index) in entities" v-bind:key="entity.id">
              <opensilex-AgroportalResultItem
                  v-on:click="selectResult(entity)"
                  :entity="entity"
                  @import="$emit('import', entity)"
              >
              </opensilex-AgroportalResultItem>
            </div>
          </div>
        </div>
      </opensilex-Overlay>
      -->
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

  text: string = "";

  uriList = [];

  isVisible: boolean = false;
  isDataLoading: boolean = false;
  isNothingFound: boolean = false;

  created() {
    this.versionInfo = this.$opensilex.versionInfo;
    this.ontologyService = this.$opensilex.getService("opensilex.OntologyService");
  }

  search(text: string) {

    this.isNothingFound = false;

    if (!text) {
      this.uriList = [];
      return;
    }
    console.debug(text);

    this.isDataLoading = true;

    this.$opensilex.disableLoader();
    this.ontologyService.searchURIs(text)
        .then((http) => {
          if (http.response.result.length === 0) {
            this.isNothingFound = true;
          }

          this.uriList = http.response.result;
          this.isDataLoading = false;
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

#global-results {
  position: absolute;
  bottom: 35px;
  background-color: #f1f1f1;
  width: 20%;
  min-width: 200px;
  z-index: 10;
}

</style>
