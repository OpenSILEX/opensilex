<template>
<div>
  <!-- UriSearch bar and close buttonn-->
  <div class="header-container">
    <div>
      <b-input-group class="uriSearchContainer">
        <opensilex-StringFilter
          :filter.sync="uriSearchValue"
          placeholder="component.header.uri-search-placeholder"
          class="searchFilter"
          @handlingEnterKey="launchUriGlobalSearch"
        ></opensilex-StringFilter>
        <b-input-group-append>
          <b-button class="greenThemeColor" @click="launchUriGlobalSearch">
            <i class="icon ik ik-search"></i>
          </b-button>
        </b-input-group-append>
      </b-input-group>
    </div>
    <opensilex-Button
      label="component.common.close"
      icon="ik#ik-x"
      class="closeResultBox"
      @click="$emit('hideUriSearch')"
    ></opensilex-Button>
  </div>

  <hr class="dashed-separator" v-if="uriSearchResultVisible"/>

  <!-- Result-->
  <opensilex-GlobalUriSearchResult
    v-if="uriSearchResultVisible"
    :searchResult="uriSearchResult"
  ></opensilex-GlobalUriSearchResult>
</div>
</template>

<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import {BasicMongoSparqlDTO} from "opensilex-core/index";
import {UriSearchService} from "opensilex-core/api/uriSearch.service";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";

@Component({})
export default class GlobalUriSearchBox extends Vue {
  //#region: data
  $opensilex: OpenSilexVuePlugin;
  private uriSearchResult: BasicMongoSparqlDTO = {};
  private uriSearchValue: string = "";
  private uriSearchService: UriSearchService;
  //To keep track of if the actual result, or message to say nothing found is visible
  private uriSearchResultVisible: boolean = false;
  //#endregion

  //#region: hooks
  created() {
    this.uriSearchService = this.$opensilex.getService("opensilex.UriSearchService");
  }
  //#endregion

  //#region: EventHandlers
  launchUriGlobalSearch(){
    //TODO handle no search results, and multiple results?
    this.uriSearchService.searchByUri(this.uriSearchValue).then( res => {
        this.uriSearchResult = res.response.result.pop();
      }
    );
    this.uriSearchResultVisible = true;
  }
  //#endregion

  //#region: computed
  //#endregion
}
</script>

<style scoped lang="scss">
.header-container{
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.closeResultBox{
  border: none;
  margin-top: -10px;
  font-size: 1.5em !important;
  color:rgba(101, 101, 101, 0.5);
  font-weight: bolder;
  cursor: pointer;
  background: none;
}

.dashed-separator {
  border: none;
  border-top: 1px dashed #000;
  margin: 20px 0;
}
</style>