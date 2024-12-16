<template>
<div>
  <!-- UriSearch bar and close buttonn-->
  <div class="header-container">
    <div>
      <b-input-group>
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

  <br>

  <!-- Result-->
  <div
    v-if="uriSearchResultVisible"
  >
    <opensilex-GlobalUriSearchResult
      v-if="resultsFound"
      :searchResult="uriSearchResult"
      @hideUriSearch="$emit('hideUriSearch')"
    ></opensilex-GlobalUriSearchResult>
    <div v-else class="no-results">
      {{this.$t('GlobalUriSearchBox.noResultsMessage')}}
    </div>
  </div>

</div>
</template>

<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import {URIGlobalSearchDTO} from "opensilex-core/index";
import {UriSearchService} from "opensilex-core/api/uriSearch.service";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";

@Component({})
export default class GlobalUriSearchBox extends Vue {
  //#region: data
  $opensilex: OpenSilexVuePlugin;
  private uriSearchResult: URIGlobalSearchDTO = {};
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
    this.uriSearchService.searchByUri(this.uriSearchValue).then( res => {
        this.uriSearchResult = res.response.result;
        this.uriSearchResultVisible = true;
      }
    ).catch(error =>{
      this.uriSearchResult = null;
      this.uriSearchResultVisible = true;
    });
  }
  //#endregion

  //#region: computed
  get resultsFound(): boolean{
    return !!this.uriSearchResult;
  }
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

.no-results {
  font-style: italic;
  font-size: 1.3em;
  text-align: center;
  padding: 10px;
  display: flex;
  justify-content: center;
  align-items: center;
  color: #666666;
}

</style>

<i18n>
en:
  GlobalUriSearchBox:
    noResultsMessage: Not found

fr:
  GlobalUriSearchBox:
    noResultsMessage: Pas trouvé

</i18n>