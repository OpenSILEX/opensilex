<template>
  <opensilex-Overlay :show="isDataLoading">
    <div id="agroportal-results" class="container-fluid scrollable" v-if="!isTextEmpty">
      <div class="wrapper">

        <div v-if="isNothingFound && !isDataLoading">
          {{ this.$t("AgroportalResults.nothing-found", [this.text]) }}
        </div>

        <div v-if="isAgroportalDown && !isDataLoading">
          {{ this.$t("AgroportalResults.nothing-found", [this.text]) }}
        </div>

        <opensilex-AgroportalResultItem v-for="(entity, index) in entities" v-bind:key="entity.id"
          ref="AgroportalResultItem"
          :entity="entity"
          :index="index"
          @import="$emit('import', entity)"
          @item-clicked="selectItem"
        >

          <template v-if="isMappingMode" v-slot:btnValidate>
            <b-dropdown
                dropright
                class="mb-2 mr-2"
                :small="true"
                text="Map term as">

              <b-dropdown-item v-for="(relation, index) in mappingOptions" v-bind:key="relation.id"
                 class="btn-dropdown"
                 @click="$emit('importMapping', entity, relation)"
              >
                {{relation.label}}
              </b-dropdown-item>
            </b-dropdown>
          </template>

          <template v-else v-slot:btnValidate>
            <opensilex-CreateButton
                :label="$t('AgroportalResults.btn-choose')"
                :title="$t('AgroportalResults.btn-choose')"
                @click="$emit('import', entity)"
            >
            </opensilex-CreateButton>
          </template>

        </opensilex-AgroportalResultItem>

      </div>
    </div>
  </opensilex-Overlay>
</template>


<script lang="ts">

import {Component, Prop, Ref, Watch} from "vue-property-decorator";
import Vue from 'vue';
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import AgroportalResultItem from "./AgroportalResultItem.vue";
import {EntityAgroportalDTO} from "opensilex-core/model/entityAgroportalDTO";
import {AgroportalAPIService} from "opensilex-core/api/agroportalAPI.service";

@Component
export default class AgroportalResults extends Vue {

  $opensilex: OpenSilexVuePlugin;
  service: AgroportalAPIService;

  @Prop({
    default: ""
  })
  text: string;

  @Prop({
    default: () => []
  })
  ontologies: string[];

  @Prop()
  mappingOptions;

  @Prop({
    default: false
  })
  isMappingMode: boolean;

  entities: Array<any> = [];
  selectedIndex: number = null;

  isAgroportalDown: boolean = false;
  isDataLoading: boolean = false;

  @Ref("AgroportalResultItem") readonly resultItems!: any;

  get isNothingFound() : boolean {
    return (this.entities.length === 0) && !(this.text.trim().length === 0);
  }

  get isTextEmpty() : boolean {
    return (this.text.trim().length === 0);
  }

  //@Watch("text")
  updateResults(searchedText: string, withAllOntologies: boolean) {

    console.debug(searchedText);

    if (!searchedText) {
      this.clear();
      return;
    }

    this.isDataLoading = true;
    this.entities = [];

    this.$opensilex.disableLoader();
    this.service.searchThroughAgroportal(
        searchedText,
        !withAllOntologies? this.ontologies.join(",") : undefined,
        undefined,
        0,
        0
    ).then((http) => {
      if (http.response && http.response.result) {
        let results = http.response.result as Array<EntityAgroportalDTO>;
        this.entities = [...new Map(results.map(item =>
            [item.id, item])).values()];
        this.isDataLoading = false;
      }
    })
    .catch((error) => {
      this.isAgroportalDown = true;
    });
  }

  selectItem(index) {
    if(this.selectedIndex != null) {
      this.resultItems[this.selectedIndex].isSelected = false;
    }
    this.resultItems[index].isSelected = true;
    this.selectedIndex = index;
  }

  clear() {
    this.entities = [];
    this.selectedIndex = null;
  }

  created() {
    this.service = this.$opensilex.getService<AgroportalAPIService>("opensilex.AgroportalAPIService");
  }

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

.btn-dropdown {
  z-index: 10;
}

</style>


<i18n>

en:
  AgroportalResults:
    exact-match: exact match
    close-match: close match
    broad-match: broad match
    narrow-match: narrow match
    nothing-found: Nothing found for '{0}'
    btn-map-term: Map term as
    btn-choose: Choose

fr:
  AgroportalResults:
    exact-match: exact match
    close-match: close match
    broad-match: broad match
    narrow-match: narrow match
    nothing-found: Aucun résultat pour '{0}'
    btn-map-term: Lier en tant que
    btn-choose: Choisir

</i18n>