<template>
  <div>

    <!-- All main info (non metadata stuff) -->
    <div class="main-info-style">
      <!-- URI -->
      <opensilex-UriView
        v-if="!isDataOrDataFile"
        class="uriLinkGlobalUriSearchRes"
        :uri="uri"
        :value="this.shortUri"
        :to="{
        path: detailsPath
        }"
        @linkClicked="$emit('hideUriSearch')"
      ></opensilex-UriView>

      <span
        v-else
        class="data-uri-details"
      >
        <opensilex-UriView
          :uri="uri"
          :value="this.shortUri"
        ></opensilex-UriView>

        <opensilex-Button
          :small="true"
          @click="handleSeeDetails"
          label="GlobalUriSearch.seeDetails"
          class="data-uri-details-item"
        >
          <template v-slot:icon>
            <opensilex-Icon icon="fa#eye" />
          </template>
        </opensilex-Button>

      </span>

      <!-- Name -->
      <opensilex-StringView
        v-if="!isDataOrDataFile"
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
      v-if="isDataOrDataFile"
      ref="dataProvenanceModalView"
    ></opensilex-DataProvenanceModalView>
  </div>
</template>

<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import { URIGlobalSearchDTO , DataGetSearchDTO, ProvenanceGetDTO, UserGetDTO, DataFileGetDTO} from "opensilex-core/index";
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
  searchResult: URIGlobalSearchDTO;
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
   * For now handles data and datafiles
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

  /**
   * Like the isDataOrDataFile computed, this will get the dto of either data or datafile (both can never not be null at same time
   */
  get dataDto() : DataGetSearchDTO | DataFileGetDTO{
    if(!this.searchResult){
      return null;
    }
    if(this.searchResult.data_dto){
      return this.searchResult.data_dto;
    }
    return this.searchResult.datafile_dto;
  }

  /**
   * isDataOrDataFile = is data OR datafile
   */
  get isDataOrDataFile(): boolean{
    return this.searchResult.data_dto !== null || this.searchResult.datafile_dto !== null;
  }

  get isData(): boolean{
    return this.searchResult.data_dto !== null;
  }

  //#endregion
}
</script>

<style scoped lang="scss">

.close-button-container {
  right: 0;
}

.main-info-style{
  margin-bottom: 5px;
}

.data-uri-details {
  display: flex;
  gap: 5px;
  align-items: center;
}

.data-uri-details-item {
  background: none;
  border: none;
  color: #00A38D;
}

.data-uri-details-item:hover {
  color: #02c5ab
}

.closeResultBox:hover{
  color : #00A28C;
  background: none;
}

</style>

<i18n>
en:
  GlobalUriSearch:
    seeDetails: See details
    dataTypeName: Data

fr:
  GlobalUriSearch:
    seeDetails: Voir détails
    dataTypeName: Donnée

</i18n>