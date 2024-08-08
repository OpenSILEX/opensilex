<template>
  <opensilex-SelectForm
      ref="germplasmSelector"
      :label="label"
      placeholder="GermplasmSelectorWithFilter.placeholder-multiple"
      :selected.sync="germplasmUris"
      modalComponent="opensilex-GermplasmModalList"
      :isModalSearch="true"
      :clearable="true"
      :multiple="true"
      :selectedInJsonFormat="this.editMode ? germplasm : null"
      :experiment="experimentUri"
      @clear="refreshGermplasmSelector"
      :limit="4"
      @hide='$emit("hideSelector")'
      @shown='$emit("shownSelector")'
      class="searchFilter"
  ></opensilex-SelectForm>
</template>

<script lang="ts">
import {Component, Prop, PropSync, Ref} from "vue-property-decorator";
import Vue from "vue";
import SelectForm, {SelectableItem} from '../common/forms/SelectForm.vue';
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";

@Component
export default class GermplasmSelectorWithFilter extends Vue {
  $opensilex: OpenSilexVuePlugin;

  @Ref("germplasmSelector") readonly germplasmSelector!: SelectForm;

  @Prop()
  editMode: boolean;

  /**
   * Set an experiment uri, in this case we don't show experiment filter and show only germplasms of this experiment
   */
  @Prop()
  experimentUri: string;

  @PropSync("germplasms")
  germplasm: Array<SelectableItem>;

  @PropSync("germplasmsUris")
  germplasmUris: Array<string>;

  @Prop({default: "GermplasmSelectorWithFilter.placeholder-multiple"})
  placeholder: string;

  @Prop({default: "GermplasmView.title"})
  label: string;

  setGermplasmSelectorToFirstTimeOpen(){
    this.germplasmSelector.setSelectorToFirstTimeOpen();
  }

  refreshGermplasmSelector() {
    this.germplasmSelector.refreshModalSearch();
  }

}
</script>

<style scoped lang="scss">
</style>

<i18n>

en:
  GermplasmSelectorWithFilter:
    placeholder : Select a germplasm
    placeholder-multiple : Select one or more germplasm
    filter-search-no-result : No germplasm found

fr:
  GermplasmSelectorWithFilter:
    placeholder : Sélectionner une ressource génétique
    placeholder-multiple : Sélectionner un ou plusieurs éléments
    filter-search-no-result : Aucune ressource génétique trouvée

</i18n>