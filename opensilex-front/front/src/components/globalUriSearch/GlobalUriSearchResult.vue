<template>
  <div>

    <!-- All main info (non metadata stuff) -->
    <div class="main-info-style">
      <!-- URI -->
      <opensilex-UriView
        v-if="!hasNoDetailsPage"
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
          :uri="this.shortUri"
          :value="this.shortUri"
        ></opensilex-UriView>

        <opensilex-Button
          v-if="isSubTypeOfEvent || this.dataDto.uri"
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
        v-if="!hasNoDetailsPage"
        :value="name"
        label="component.common.name"
      ></opensilex-StringView>
      <!-- Type -->
      <opensilex-TypeView
        v-if="!isData"
        :type="type"
        :typeLabel="typeName"
        :copyableTypeUri="true"
      ></opensilex-TypeView>
      <opensilex-TypeView
        v-else
        :typeLabel="$t('GlobalUriSearch.dataTypeName')"
      ></opensilex-TypeView>
      <!-- rdfsComment -->
      <opensilex-TextView
        v-if="rdfsComment"
        label="GlobalUriSearch.comment"
        :value="rdfsComment"
      ></opensilex-TextView>
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
      v-if="hasNoDetailsPage"
      ref="dataProvenanceModalView"
    ></opensilex-DataProvenanceModalView>

    <!-- Event details -->
    <opensilex-EventModalView
      modalSize="lg"
      ref="eventModalView"
      :static="false"
    ></opensilex-EventModalView>
  </div>
</template>

<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import { URIGlobalSearchDTO , DataGetSearchDTO, ProvenanceGetDTO, UserGetDTO, DataFileGetDTO, MoveDetailsDTO, EventDetailsDTO} from "opensilex-core/index";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {Prop, Ref} from "vue-property-decorator";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import {UriSearchService} from "opensilex-core/api/uriSearch.service";
import {OntologyService} from "opensilex-core/api/ontology.service";
import {DataService} from "opensilex-core/api/data.service";
import DataProvenanceModalView from "../data/DataProvenanceModalView.vue";
import {EventsService} from "opensilex-core/api/events.service";
import EventModalView from "../events/view/EventModalView.vue";

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
  private eventsService: EventsService;
  //#endregion

  //#region: hooks
  created() {
    this.uriSearchService = this.$opensilex.getService("opensilex.UriSearchService");
    this.ontologyService = this.$opensilex.getService("opensilex.OntologyService");
    this.dataService = this.$opensilex.getService("opensilex.DataService");
    this.eventsService = this.$opensilex.getService("opensilex.EventsService");
  }

  //#endregion

  //#region: event handlers
  private async handleSeeDetails(){
    //If the result is an event:
    this.$opensilex.enableLoader();
    if(this.isSubTypeOfEvent()){
      let http: HttpResponse<OpenSilexResponse<EventDetailsDTO>> = await this.getEventPromise();
      await this.eventModalView.show(http);
      return;
    }

    //If the result is a data or datafile
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
  @Ref("eventModalView") readonly eventModalView!: EventModalView;

  //#endregion

  //#region: private functions
  private getProvenance(uri) {
    if (uri != undefined) {
      return this.dataService
        .getProvenance(uri)
        .then((http: HttpResponse<OpenSilexResponse<ProvenanceGetDTO>>) => {
          return http.response.result;
        });
    }
  }

  private isSubTypeOfEvent(){
    if(this.searchResult.super_types){
      for(let currentType of this.searchResult.super_types.rdf_types){
        if(this.$opensilex.Oeev.checkURIs(currentType, this.$opensilex.Oeev.EVENT_TYPE_URI)){
          return true;
        }
      }
    }
  }

  private getEventPromise(): Promise<HttpResponse<OpenSilexResponse>> {
    if (this.isMove()) {
      return this.eventsService.getMoveEvent(this.uri);
    } else {
      return this.eventsService.getEventDetails(this.uri);
    }
  }

  private isMove() {
    return this.$opensilex.Oeev.checkURIs(this.type, this.$opensilex.Oeev.MOVE_TYPE_URI);
  }

  private isFactorLevel(): boolean{
    return this.$opensilex.Oeev.checkURIs(this.type, this.$opensilex.Oeso.FACTOR_LEVEL_URI);
  }
  private isFactorLevelOrFactor(): boolean{
    return this.$opensilex.Oeev.checkURIs(this.type, this.$opensilex.Oeso.FACTOR_URI) || this.isFactorLevel();
  }
  //#endregion


  //#region: computed

  /**
   * Gets the path if the uri can lead to some page, A bunch of cases are handled separately
   */
  get detailsPath() : string{
    let formattedPath = "";
    if(!this.hasResult){
      return formattedPath;
    }
    //If type is a germplasm group then build path manually
    if(this.$opensilex.checkURIs(this.type, this.$opensilex.Oeso.GERMPLASM_GROUP_TYPE_URI)){
      return "/germplasm/group?selected="+ encodeURIComponent(this.uri);
    }
    //Check if type is one of the wierd other components on variables page that doesn't have its own page (Entities, etc...)
    formattedPath = this.$opensilex.getVariableComponentPath(this.type, this.uri);
    if(formattedPath !== null){
      return formattedPath;
    }
    if(this.searchResult.super_types !== null){
        let unformattedPath = this.$opensilex.getPathFromUriTypes(this.searchResult.super_types.rdf_types);
        //Pass factor as uri to getTargetPath if the uri was a FactorLevel (to navigate to its parent Factor)
        //Only pass context if we the type is factor or factor level, only things inside experiments that we can navigate to for now.
        //Errors get created otherwise as the context will always be the global graph instead of an xp
        formattedPath = this.$opensilex.getTargetPath((this.$opensilex.checkURIs(this.type, this.$opensilex.Oeso.FACTOR_LEVEL_URI) ? this.factorUri : this.uri), (this.isFactorLevelOrFactor() ? this.context : undefined), unformattedPath);
    }else if(this.searchResult.root_class !== null){
      return this.$opensilex.getVocabularyPath(this.uri, this.searchResult.root_class, this.searchResult.is_property);
    }

    return formattedPath;
  }

  /**
   * Get the graph that the uri belongs too, OR in some cases simply the uri of Experiment that this element is used in
   * (Factor for example is stored in its own graph but the webservice forces context to have the correct Experiment uri)
   */
  get context(){
    return this.searchResult.context;
  }

  /**
   * This will only ever not be null if the uri was a factor level, used for navigation
   */
  get factorUri(){
    return this.searchResult.factor_uri;
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
  get rdfsComment() : string{
    return this.searchResult.rdfs_comment;
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
   * Returns true if this is an element that does not have a URI link
   */
  get hasNoDetailsPage(): boolean{
    return this.searchResult.data_dto !== null ||
      this.searchResult.datafile_dto !== null ||
      this.isSubTypeOfEvent();
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

::v-deep .field-view-title {
  min-width: 100px; //override title width for the field components, as we are in a small area here
}

</style>

<i18n>
en:
  GlobalUriSearch:
    seeDetails: See details
    dataTypeName: Data
    comment: Description

fr:
  GlobalUriSearch:
    seeDetails: Voir détails
    dataTypeName: Donnée
    comment: Description

</i18n>