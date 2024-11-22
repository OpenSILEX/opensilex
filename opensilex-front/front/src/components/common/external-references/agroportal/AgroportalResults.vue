<template>
  <opensilex-Overlay :show="isDataLoading">
    <div
        id="agroportal-results"
        class="container-fluid scrollable"
        v-show="terms.length > 0 || isNothingFound || isAgroportalDown"
    >
      <div class="wrapper">
        <div v-if="(isNothingFound || isAgroportalDown) && !isDataLoading">
          {{ this.$t("AgroportalResults.nothing-found") }}
        </div>

        <!-- List of results -->
        <opensilex-AgroportalResultItem
            v-for="(term, index) in terms"
            ref="resultItems"
            :key="term.id"
            :entity="term"
            @item-clicked="selectItem(index)"
        >
          <!-- Select button for mapping mode -->
          <template v-if="isMappingMode" v-slot:validationButton>
            <opensilex-SkosSelector
                @update:selectedRelation="relation => onRelationSelected(term, relation)"
                right
            ></opensilex-SkosSelector>
          </template>

          <!-- Default select button -->
          <template v-else v-slot:validationButton>
            <opensilex-CreateButton
                class="v-step-result-import-button"
                :label="$t('AgroportalResults.btn-select')"
                :title="$t('AgroportalResults.btn-select')"
                @click="emitImport(term)"
            >
            </opensilex-CreateButton>
          </template>
        </opensilex-AgroportalResultItem>
      </div>
    </div>
  </opensilex-Overlay>
</template>


<script lang="ts">

import {Component, Prop, Ref} from "vue-property-decorator";
import Vue from 'vue';
import OpenSilexVuePlugin from "../../../../models/OpenSilexVuePlugin";
import AgroportalResultItem from "./AgroportalResultItem.vue";
import {AgroportalAPIService} from "opensilex-core/api/agroportalAPI.service";
import {AgroportalTermDTO} from "opensilex-core/model/agroportalTermDTO";
import {CLOSE_MATCH, UriSkosRelation} from "../../../../models/SkosRelations";

@Component({})
export default class AgroportalResults extends Vue {
  //#region Plugin
  private $opensilex: OpenSilexVuePlugin;
  //#endregion

  //#region Props
  @Prop({
    default: false
  })
  private readonly isMappingMode: boolean;
  //#endregion

  //#region Refs
  @Ref("resultItems")
  private readonly resultItems!: Array<AgroportalResultItem>;
  //#endregion

  //#region Data
  private service: AgroportalAPIService;
  private terms: Array<AgroportalTermDTO> = [];
  private selectedIndex: number = null;
  private isAgroportalDown: boolean = false;
  private isDataLoading: boolean = false;
  private isNothingFound: boolean = false;
  //#endregion

  //#region Hooks
  created() {
    this.service = this.$opensilex.getService<AgroportalAPIService>("opensilex.AgroportalAPIService");
  }

  //#endregion

  //#region Public methods
  public search(searchedText: string, withAllOntologies: boolean, ontologies: Array<string>) {
    if (!searchedText) {
      this.clear();
      return;
    }

    this.isDataLoading = true;
    this.terms = [];

    this.$opensilex.disableLoader();
    this.service.searchThroughAgroportal(
        searchedText,
        !withAllOntologies ? ontologies : undefined
    ).then((http) => {
      if (http.response && http.response.result) {
        this.terms = http.response.result as Array<AgroportalTermDTO>;
        this.isDataLoading = false;
        this.isNothingFound = this.terms.length === 0;
      }
    }).catch((error) => {
      this.isAgroportalDown = true;
      this.$opensilex.errorHandler(error)
    });
  }

  /**
   * For tutorial purpose
   */
  selectItem(index) {
    if (this.selectedIndex != null) {
      this.resultItems[this.selectedIndex].setSelected(false);
    }
    this.resultItems[index].setSelected(true);
    this.selectedIndex = index;
  }

  /**
   * For tutorial purpose
   */
  selectAndImportFirstItem() {
    this.selectItem(0);
    this.$emit('import', this.terms[0]);
  }

  /**
   * For tutorial purpose
   */
  selectAndMapFirstItem() {
    this.selectItem(0);
    this.emitImportMapping({
      relationDtoKey: CLOSE_MATCH.dtoKey,
      uri: this.terms[0].id
    });
  }

  //#endregion

  //#region Private methods
  clear() {
    this.terms = [];
    this.selectedIndex = null;
    this.isNothingFound = false;
  }

  //#endregion

  //#region Events
  private emitImport(term: AgroportalTermDTO) {
    this.$emit('import', term);
  }

  private emitImportMapping(uriRelation: UriSkosRelation) {
    console.debug("results import mapping", uriRelation);
    this.$emit("importMapping", uriRelation);
  }

  //#endregion

  //#region Event handlers
  private onRelationSelected(term: AgroportalTermDTO, selectedRelation: string) {
    this.emitImportMapping({
      relationDtoKey: selectedRelation,
      uri: term.id
    });
  }
  //#endregion
}
</script>


<style scoped>
#agroportal-results {
  min-height: 50px;
  background: rgba(0, 0, 0, .05);
}

.scrollable {
  max-height: 500px;
  overflow-y: scroll;
}
</style>

<i18n>
en:
  AgroportalResults:
    nothing-found: No result was found
    btn-select: Select
fr:
  AgroportalResults:
    nothing-found: Aucun résultat n'a été trouvé
    btn-select: Selectionner
</i18n>