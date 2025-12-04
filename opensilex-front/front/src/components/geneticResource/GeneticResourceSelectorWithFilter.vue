<template>
  <opensilex-ModalFormSelector
      ref="geneticResourceSelector"
      :label="label"
      placeholder="GeneticResourceSelectorWithFilter.placeholder-multiple"
      :selected.sync="geneticResourceUris"
      modalComponent="opensilex-GeneticResourceModalList"
      :clearable="true"
      :multiple="true"
      :selectedInJsonFormat="this.editMode ? geneticResource : null"
      :experiment="experimentUri"
      @clear="refreshGeneticResourceSelector"
      :limit="4"
      @hide='$emit("hideSelector")'
      @shown='$emit("shownSelector")'
      class="searchFilter"
  ></opensilex-ModalFormSelector>
</template>

<script lang="ts">
import {Component, Prop, PropSync, Ref} from "vue-property-decorator";
import Vue from "vue";
import ModalFormSelector, {SelectableItem} from '../common/forms/ModalFormSelector.vue';
import {GeneticResourceService} from "opensilex-core/api/geneticResource.service";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";

@Component
export default class GeneticResourceSelectorWithFilter extends Vue {
  $opensilex: OpenSilexVuePlugin;

  @Ref("geneticResourceSelector") readonly geneticResourceSelector!: ModalFormSelector;

  @Prop()
  editMode: boolean;

  /**
   * Set an experiment uri, in this case we don't show experiment filter and show only geneticResources of this experiment
   */
  @Prop()
  experimentUri: string;

  @PropSync("geneticResources")
  geneticResource: Array<SelectableItem>;

  @PropSync("geneticResourcesUris")
  geneticResourceUris: Array<string>;

  @Prop({default: "GeneticResourceSelectorWithFilter.placeholder-multiple"})
  placeholder: string;

  @Prop({default: "GeneticResourceView.title"})
  label: string;

  setGeneticResourceSelectorToFirstTimeOpen(){
    this.geneticResourceSelector.setSelectorToFirstTimeOpen();
  }

  refreshGeneticResourceSelector() {
    this.geneticResourceSelector.refreshModalSearch();
  }

}
</script>

<style scoped lang="scss">
</style>

<i18n>

en:
  GeneticResourceSelectorWithFilter:
    placeholder : Select a genetic resource
    placeholder-multiple : Select one or more genetic resource
    filter-search-no-result : No genetic resource found

fr:
  GeneticResourceSelectorWithFilter:
    placeholder : Sélectionner une ressource génétique
    placeholder-multiple : Sélectionner un ou plusieurs éléments
    filter-search-no-result : Aucune ressource génétique trouvée

</i18n>