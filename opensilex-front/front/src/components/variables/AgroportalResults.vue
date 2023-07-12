<template>
  <div class="container-fluid scrollable">
    <div class="wrapper">
      <div v-for="(entity, index) in entities" v-bind:key="entity.id">
        <div class="row result"
          v-on:click="selectResult(entity)"
          :class="(selected == entity) ? 'selectedResult' : ''">
          <div id="result-header" class="row mx-0 jqx-max-size">
            <div class="col-lg-12">
              <div id="result-name">
                {{entity.name}} -
                <a id="result-link" v-bind:href="entity.id" target="_blank">{{getOntologyAcronym(entity.links.ontology)}}</a>
              </div>
            </div>
          </div>
          <div id="result-body" class="row jqx-max-size">
            <div id="result-definition" class="col-lg-12">
              {{entity.definitions[0]}}
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
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

  getOntologyAcronym(url: string) {
    return url.split('/').pop();
  }

  @Watch("text")
  updateResults() {

    if (!this.text) {
      this.clear();
      return;
    }

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

#result-name {
  font-weight: bold;
  font-size: large;
  margin-bottom: 5px;
}

#result-link {
  font-weight: normal;
  font-size: medium;
}

.result {
  font-size: medium;
  margin-bottom: 10px;
  padding: 5px;
  margin-right: 1px;
}

.scrollable {
  max-height: 500px;
  overflow-y: scroll;
  overflow-x: clip;
}

.selectedResult {
  border: solid 2px #00a38d;
  border-radius: 3px;
}

</style>


<i18n>

</i18n>