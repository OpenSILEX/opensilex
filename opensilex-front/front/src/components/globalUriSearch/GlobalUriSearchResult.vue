<template>
  <div>
    <!-- Title-->
    <h3>{{ $t("GlobalUriSearch.resultTitle") }}:</h3>

    <!-- All main info (non metadata stuff) -->
    <div class="main-info-style">
      <!-- URI -->
      <opensilex-UriView
        v-if="!isData"
        class="uriLinkGlobalUriSearchRes"
        :uri="uri"
        :value="this.shortUri"
        :to="{
        path: detailsPath
      }"
      ></opensilex-UriView>
      <span
        v-else
        class="data-uri-details"
      >
        <opensilex-UriView
          class="data-uri-details-item"
          :uri="uri"
          :value="this.shortUri"
        ></opensilex-UriView>
        <opensilex-DetailButton
          class="data-uri-details-item"
          @click="handleSeeDetails"
          label="GlobalUriSearch.seeDetails"
        ></opensilex-DetailButton>
      </span>

      <!-- Name -->
      <opensilex-StringView
        v-if="!isData"
        :value="name"
        label="component.common.name"
      ></opensilex-StringView>
      <!-- Type -->
      <opensilex-TypeView
        v-if="!isData"
        :type="type"
        :typeLabel="typeName"
      ></opensilex-TypeView>
      <opensilex-TypeView
        v-else
        :typeLabel="$t('GlobalUriSearch.dataTypeName')"
      ></opensilex-TypeView>
    </div>

    <!-- Metadata -->
    <opensilex-MetadataView
      v-if="publisher"
      :publisher="publisher"
      :publicationDate="publicationDate"
      :lastUpdatedDate="updatedDate"
    ></opensilex-MetadataView>

    <!-- Data details -->
    <opensilex-DataProvenanceModalView
      v-if="isData"
      ref="dataProvenanceModalView"
    ></opensilex-DataProvenanceModalView>
  </div>
</template>

<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import { BasicMongoSparqlDTO , DataGetSearchDTO, ProvenanceGetDTO, UserGetDTO} from "opensilex-core/index";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {Prop, Ref} from "vue-property-decorator";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import {UriSearchService} from "opensilex-core/api/uriSearch.service";
import {OntologyService} from "opensilex-core/api/ontology.service";
import {DataService} from "opensilex-core/api/data.service";
import DataProvenanceModalView from "../data/DataProvenanceModalView.vue";

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
  private dataService: DataService;
  //#endregion

  //#region: hooks
  created() {
    this.uriSearchService = this.$opensilex.getService("opensilex.UriSearchService");
    this.ontologyService = this.$opensilex.getService("opensilex.OntologyService");
    this.dataService = this.$opensilex.getService("opensilex.DataService");
  }

  //#endregion

  //#region: event handlers
  /**
   * For now handles data
   * TODO data files or anything else that doesnt have a details page?
   */
  private handleSeeDetails(){
    this.$opensilex.enableLoader();
    this.getProvenance(this.dataDto.provenance.uri)
      .then(result => {
        let value = {
          provenance: result,
          data: this.dataDto
        }
        this.dataProvenanceModalView.setProvenance(value);
        this.dataProvenanceModalView.show();
      });

  }
  //#endregion

  //#region: refs

  @Ref("dataProvenanceModalView") readonly dataProvenanceModalView!: DataProvenanceModalView;

  //#endregion

  //#region: private functions
  getProvenance(uri) {
    if (uri != undefined) {
      return this.dataService
        .getProvenance(uri)
        .then((http: HttpResponse<OpenSilexResponse<ProvenanceGetDTO>>) => {
          return http.response.result;
        });
    }
  }
  //#endregion


  //#region: computed

  get detailsPath() : string{
    let formattedPath = "";
    if(this.hasResult && this.searchResult.super_types.length !== 0){
      for (let typeIndex in this.searchResult.super_types) {
        let type = this.searchResult.super_types[typeIndex];
        let unformattedPath = this.$opensilex.getPathFromUriTypes(type.rdf_types);
        formattedPath = this.$opensilex.getTargetPath(this.searchResult.uri, undefined, unformattedPath);
        console.debug("FUCK you!", JSON.stringify(unformattedPath), JSON.stringify(formattedPath));
      }
    }
    return formattedPath;
  }


  get hasResult() : boolean{
    return this.searchResult != null;
  }

  get name() : string {
    return this.searchResult.name;
  }
  get typeName() : string {
    return this.searchResult.rdf_type_name;
  }
  get type() : string{
    return this.searchResult.rdf_type;
  }
  get publisher() : UserGetDTO{
    return this.searchResult.publisher;
  }
  get publicationDate() :string{
    return this.searchResult.publication_date;
  }
  get updatedDate():string {
    return this.searchResult.last_updated_date;
  }
  get uri() :string{
    return this.searchResult.uri;
  }

  get shortUri() :string{
    if(this.searchResult.uri){
      return this.$opensilex.getShortUri(this.searchResult.uri);
    }else {
      return null;
    }
  }

  get dataDto() : DataGetSearchDTO{
    return this.searchResult.data_dto;
  }

  get isData(): boolean{
    return this.searchResult.data_dto !== null;
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

.data-uri-details{
  display: inline;
}

.data-uri-details {
  display: flex;
  gap: 10px;
  align-items: center;
}

.data-uri-details-item {
  padding: 5px;
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
    dataTypeName: Data

fr:
  GlobalUriSearch:
    resultTitle: Résultat de recherche
    seeDetails: Voir détails
    dataTypeName: Donnée

</i18n>