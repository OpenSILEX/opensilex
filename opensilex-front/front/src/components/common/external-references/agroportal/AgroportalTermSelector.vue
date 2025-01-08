<template>
  <div>
    <div v-if="isAgroportalReachable">
      <opensilex-AgroportalSearch
          class="v-step-agroportal-search"
          ref="searchComponent"
          label="component.common.name"
          :placeholder="placeholder"
          :selected.sync="syncedSelectedOntologies"
          :isAllOntologies.sync="useAllOntologies"
          @change="onSearchTextChange"
          @inputValueHasChanged="inputValueHasChanged"
      ></opensilex-AgroportalSearch>

      <opensilex-AgroportalResults
          class="v-step-agroportal-results"
          ref="searchResults"
          :text="searchText"
          :ontologies="syncedSelectedOntologies"
          :isMappingMode="isMappingMode"
          @import="onImport"
          @importMapping="onImportMapping">
      </opensilex-AgroportalResults>
    </div>
    <div v-else>
      <b-alert show variant="warning">
        <span v-html="$t('AgroportalTermSelector.agroportal-not-reachable')"></span>
      </b-alert>
    </div>
  </div>
</template>

<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import AgroportalResults from "./AgroportalResults.vue";
import {Prop, Ref, Watch} from "vue-property-decorator";
import {AgroportalTermDTO} from "opensilex-core/model/agroportalTermDTO";
import {UriSkosRelation} from "../../../../models/SkosRelations";
import AgroportalSearch from "./AgroportalSearch.vue";
import OpenSilexVuePlugin from "../../../../models/OpenSilexVuePlugin";
import {AgroportalAPIService} from "opensilex-core/api/agroportalAPI.service";

@Component({})
export default class AgroportalTermSelector extends Vue {
  //#region Plugins and services
  private readonly $opensilex: OpenSilexVuePlugin;
  private service: AgroportalAPIService;
  //#endregion

  //#region Props
  @Prop({
    default: undefined
  })
  private readonly placeholder: string;

  @Prop({
    default: false
  })
  private readonly isMappingMode: boolean;

  @Prop({
    default: () => []
  })
  private readonly ontologies!: string[];
  //#endregion

  //#region Data
  private selectedOntologies: string[] = this.ontologies; // initialise with default values given by prop
  private searchText: string = "";
  private useAllOntologies: boolean = false;
  private isAgroportalReachable: boolean = false;
  //#endregion

  //#region Computed
  get syncedSelectedOntologies() {
    return this.selectedOntologies;
  }

  set syncedSelectedOntologies(value: string[]) {
    this.selectedOntologies = value;
    this.$emit("update:ontologies", value); // tjr besoin ?
  }
  //#endregion

  //#region Refs
  @Ref("searchComponent")
  private readonly searchComponent: AgroportalSearch;

  @Ref("searchResults")
  private readonly searchResults: AgroportalResults;
  //#endregion

  //#region Hooks
  private created() {
    this.service = this.$opensilex.getService("opensilex.AgroportalAPIService");
    this.service.pingAgroportal().then(http => {
      if (http.response.result) {
        this.isAgroportalReachable = true;
      }
    }).catch(() => {
      return;
    })
  }
  //#endregion

  //#region Events
  private emitImport(term: AgroportalTermDTO) {
    this.$emit("import", term);
  }

  private emitImportMapping(uriRelation: UriSkosRelation) {
    this.$emit("importMapping", uriRelation);
  }
  //#endregion

  //#region Event handlers
  private onSearchTextChange(text: string) {
    this.searchText = text;
    this.searchResults.search(text, this.useAllOntologies, this.selectedOntologies);
  }

  inputValueHasChanged(text: string) {
    this.searchText = text
    this.$emit("inputValueHasChanged", this.searchText)// faire condition si on crée sur createnew
  }

  private onImport(term: AgroportalTermDTO) {
    this.emitImport(term);
  }

  private onImportMapping(uriRelation: UriSkosRelation) {
    this.emitImportMapping(uriRelation);
  }
  //#endregion

  //#region Public modifier methods (mostly for tutorial purpose)
  public setSearchText(text: string) {
    this.searchComponent.setSearchText(text);
  }

  public selectFirstItem() {
    this.searchResults.selectItem(0);
  }

  public selectAndImportFirstItem() {
    this.searchResults.selectAndImportFirstItem();
  }

  public selectAndMapFirstItem() {
    this.searchResults.selectAndMapFirstItem();
  }
  //#endregion
}
</script>

<style scoped lang="scss">

</style>

<i18n>
en:
  AgroportalTermSelector:
    agroportal-not-reachable: >
      AgroPortal is not reachable for the moment, please try again later.
fr:
  AgroportalTermSelector:
    agroportal-not-reachable: >
      AgroPortal n'est pas accessible pour le moment, veuillez réessayer plus tard.

</i18n>