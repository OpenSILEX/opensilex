<template>
  <div>
    <opensilex-AgroportalSearch
        class="v-step-agroportal-search"
        label="component.common.name"
        :placeholder="placeholder"
        :selected.sync="selectedOntologies"
        :isAllOntologies.sync="useAllOntologies"
        @change="onSearchTextChange"
    ></opensilex-AgroportalSearch>

    <opensilex-AgroportalResults
        class="v-step-agroportal-results"
        ref="searchResults"
        :text="searchText"
        :ontologies="selectedOntologies"
        :isMappingMode="isMappingMode"
        @import="onImport"
        @importMapping="onImportMapping">
    </opensilex-AgroportalResults>
  </div>
</template>

<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import AgroportalResults from "./AgroportalResults.vue";
import {Prop, PropSync, Ref} from "vue-property-decorator";
import {AgroportalTermDTO} from "opensilex-core/model/agroportalTermDTO";
import {UriSkosRelation} from "../../../../models/SkosRelations";

@Component({})
export default class AgroportalTermSelector extends Vue {
  //region Props
  @Prop({
    default: undefined
  })
  private readonly placeholder: string;

  @Prop({
    default: false
  })
  private readonly isMappingMode: boolean;

  @PropSync("ontologies")
  private selectedOntologies: string[];
  //endregion

  //region Refs
  @Ref("searchResults")
  private readonly results: AgroportalResults;
  //endregion

  //region Data
  private searchText: string = "";
  private useAllOntologies: boolean = false;
  //endregion

  //region Events
  private onSearchTextChange(text: string) {
    this.searchText = text;
    this.results.search(text, this.useAllOntologies, this.selectedOntologies);
  }

  private onImport(term: AgroportalTermDTO) {
    this.$emit("import", term);
  }

  private onImportMapping(uriRelation: UriSkosRelation) {
    this.$emit("importMapping", uriRelation);
  }
  //endregion

  //region Public modifier methods (mostly for tutorial purpose)
  public setSearchText(text: string) {
    this.searchText = text;
    this.results.search(text, this.useAllOntologies, this.selectedOntologies);
  }

  public selectFirstItem() {
    this.results.selectItem(0);
  }

  public selectAndImportFirstItem() {
    this.results.selectAndImportFirstItem();
  }

  public selectAndMapFirstItem() {
    this.results.selectAndMapFirstItem();
  }
  //endregion
}
</script>

<style scoped lang="scss">

</style>