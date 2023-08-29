<template>
  <opensilex-Overlay :show="isDataLoading">
    <div id="agroportal-results" class="container-fluid scrollable">
      <div class="wrapper">

        <div v-if="isNothingFound && !isDataLoading">
          Nothing found for '{{ this.text }}'
        </div>

        <div v-if="isAgroportalDown && !isDataLoading">
          Nothing found for '{{ this.text }}'
        </div>

        <div v-for="(entity, index) in entities" v-bind:key="entity.id">

          <opensilex-AgroportalResultItem
            v-on:click="selectResult(entity)"
            :entity="entity"
            @import="$emit('import', entity)"
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
                  label="Import"
                  title="Import"
                  @click="$emit('import', entity)"
              >
              </opensilex-CreateButton>
            </template>

          </opensilex-AgroportalResultItem>

        </div>
      </div>
    </div>
  </opensilex-Overlay>
</template>


<script lang="ts">

import {Component, Prop, Watch} from "vue-property-decorator";
import Vue from 'vue';
import {VariablesService} from "opensilex-core/api/variables.service";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {EntityAgroportalDTO} from "opensilex-core/model/entityAgroportalDTO";

@Component
export default class AgroportalResults extends Vue {

  $opensilex: OpenSilexVuePlugin;
  entityService: VariablesService;

  @Prop({
    default: ""
  })
  text: string;

  @Prop()
  mappingOptions;

  @Prop({
    default: false
  })
  isMappingMode: boolean;

  entities: Array<any> = [];
  selected: any = null;

  isAgroportalDown: boolean = false;
  isDataLoading: boolean = false;

  skosReferences = {
    EXACT_MATCH_JSON_PROPERTY: "exact-match",
    CLOSE_MATCH_JSON_PROPERTY: "close-match",
    BROAD_MATCH_JSON_PROPERTY: "broad-match",
    NARROW_MATCH_JSON_PROPERTY: "narrow-match"
  }

  get isNothingFound() : boolean {
    return this.entities.length === 0 && !(this.text.trim().length === 0);
  }

  @Watch("text")
  updateResults() {

    if (!this.text) {
      this.clear();
      return;
    }

    this.isDataLoading = true;

    this.$opensilex.disableLoader();
    this.entityService.searchThroughAgroportal(
        this.text,
        undefined,
        0,
        0
    ).then((http) => {
      if (http.response && http.response.result) {
        let results = http.response.result as Array<EntityAgroportalDTO>;
        console.debug(results);
        this.entities = [...new Map(results.map(item =>
            [item.id, item])).values()];
        this.isDataLoading = false;
      }
    })
    .catch((error) => {
      this.isAgroportalDown = true;
    });
  }

  selectResult(result: EntityAgroportalDTO) {
    this.selected = result;
  }

  clear() {
    this.entities = [];
    this.selected = null;
  }

  created() {
    this.entityService = this.$opensilex.getService<VariablesService>("opensilex.VariablesService");
  }

}
</script>


<style scoped>

#agroportal-results {
  min-height: 50px;
}

.scrollable {
  max-height: 500px;
  overflow-y: scroll;
  overflow-x: clip;
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

fr:
  AgroportalResults:
    exact-match: exact match
    close-match: close match
    broad-match: broad match
    narrow-match: narrow match

</i18n>