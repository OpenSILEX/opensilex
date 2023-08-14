<template>
  <opensilex-Overlay :show="isDataLoading">
    <div id="agroportal-results" class="container-fluid scrollable">
      <div class="wrapper">
        <div v-if="isNothingFound && !isDataLoading">
          Nothing found for '{{ this.text }}'
        </div>
        <div v-for="(entity, index) in entities" v-bind:key="entity.id">
          <opensilex-AgroportalResultItem
            v-on:click="selectResult(entity)"
            :entity="entity"
            @import="$emit('import', entity)"
          >
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
  text;

  entities: Array<EntityAgroportalDTO> = [];
  selected: EntityAgroportalDTO = null;

  isDataLoading: boolean = false;

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

</style>


<i18n>

</i18n>