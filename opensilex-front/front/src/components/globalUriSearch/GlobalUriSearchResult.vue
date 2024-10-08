<template>
  <div>
    <!-- Title-->
    <h3>{{ $t("GlobalUriSearch.resultTitle") }}:</h3>

    <!-- All main info (non metadata stuff) -->
    <div class="main-info-style">
      <!-- URI -->
      <opensilex-UriView
        class="uriLinkGlobalUriSearchRes"
        :uri="uri"
        :value="this.$opensilex.getShortUri(uri)"
        :to="{
        path: detailsPath
      }"
      ></opensilex-UriView>
      <!-- Name -->
      <opensilex-StringView
        :value="name"
        label="component.common.name"
      ></opensilex-StringView>
      <!-- Type -->
      <opensilex-TypeView
        :type="type"
        :typeLabel="typeName"
      ></opensilex-TypeView>
    </div>

    <!-- Metadata -->
    <opensilex-MetadataView
      v-if="publisher"
      :publisher="publisher"
      :publicationDate="publicationDate"
      :lastUpdatedDate="updatedDate"
    ></opensilex-MetadataView>
    <!-- Details button -->
    <opensilex-DetailButton
      @click="handleSeeDetails"
      label="GlobalUriSearch.seeDetails"
    ></opensilex-DetailButton>
  </div>
  <!--            <button @click="handleUriSearchBoxClose">Close</button>-->
</template>

<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import {UriSearchService} from "opensilex-core/api/uriSearch.service";
import { BasicMongoSparqlDTO } from "opensilex-core/index";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {Prop} from "vue-property-decorator";
import {OntologyService} from "opensilex-core/api/ontology.service";

@Component
export default class GlobalUriSearchResult extends Vue {

  //#region: props
  @Prop()
  searchResult: BasicMongoSparqlDTO;
  //#endregion
  
  //#region: data
  $opensilex: OpenSilexVuePlugin;
  private uriSearchService: UriSearchService;
  private ontologyService: OntologyService;
  //#endregion

  //#region: hooks
  created() {
    this.uriSearchService = this.$opensilex.getService("opensilex.UriSearchService");
    this.ontologyService = this.$opensilex.getService("opensilex.OntologyService");
  }

  //#endregion

  //#region: event handlers
  private handleSeeDetails(){


  }
  //#endregion

  //#region: computed

  get detailsPath(){
    let formattedPath = "";
    if(this.hasResult && this.searchResult.super_types.length !== 0){
      for (let typeIndex in this.searchResult.super_types) {
        let type = this.searchResult.super_types[typeIndex];
        let unformattedPath = this.$opensilex.getPathFromUriTypes(type.rdf_types);
        formattedPath = this.$opensilex.getTargetPath(this.searchResult.uri, undefined, unformattedPath);
      }
    }
    return formattedPath;
  }

  get hasResult() : boolean{
    return this.searchResult != null;
  }

  get name() {
    return this.searchResult.name;
  }
  get typeName() {
    return this.searchResult.rdf_type_name;
  }
  get type() {
    return this.searchResult.rdf_type;
  }
  get publisher() {
    return this.searchResult.publisher;
  }
  get publicationDate() {
    return this.searchResult.publication_date;
  }
  get updatedDate() {
    return this.searchResult.last_updated_date;
  }
  get uri(){
    return this.searchResult.uri;
  }

  //#endregion
}
</script>

<style scoped lang="scss">

.close-button-container {
  //position: inherit;
  right: 0;
}

.main-info-style{
  margin-bottom: 15px;
}


.closeResultBox:hover{
  color : #00A28C;
  background: none;
}
</style>

<i18n>
en:
  GlobalUriSearch:
    resultTitle: Search Result
    seeDetails: See details

fr:
  GlobalUriSearch:
    resultTitle: Résultat de recherche
    seeDetails: Voir détails

</i18n>