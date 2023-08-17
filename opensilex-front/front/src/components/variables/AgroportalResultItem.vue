<template>
    <div class="row result"
         v-on:click="selectItem(!isSelected)"
      :class="(isSelected) ? 'selectedResult' : ''">
      <div id="result-header" class="row mx-0 jqx-max-size">
        <div class="col-lg-12">
          <div id="result-name">
            {{entity.name}} -
            <span id="result-ontology">{{getOntologyAcronym(entity.links.ontology)}}</span>
          </div>
          <div id="result-link">
            <a v-bind:href="entity.id" target="_blank">{{entity.id}}</a>
          </div>
        </div>
      </div>
      <div id="result-body" class="row jqx-max-size">
        <div id="result-definition" class="col-lg-12">
          {{entity.definitions[0]}}
        </div>
      </div>
      <div v-if="isSelected">
        <slot name="btnValidate"></slot>
      </div>
    </div>
</template>


<script lang="ts">

import {Component, Prop, Watch} from "vue-property-decorator";
import Vue from 'vue';
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {EntityAgroportalDTO} from "opensilex-core/model/entityAgroportalDTO";

@Component
export default class AgroportalResultItem extends Vue {

  $opensilex: OpenSilexVuePlugin;

  @Prop()
  entity: EntityAgroportalDTO;

  isSelected: boolean = false;

  getOntologyAcronym(url: string) {
    return url.split('/').pop();
  }

  selectItem(isSelected: boolean) {
    this.isSelected = isSelected;
  }

}
</script>


<style scoped>

#result-name {
  font-weight: bold;
  font-size: large;
  margin-bottom: 5px;
}

#result-ontology {
  font-weight: normal;
  font-size: medium;
}

.result {
  font-size: medium;
  margin-bottom: 10px;
  padding: 5px;
  margin-right: 1px;
}

.selectedResult {
  border: solid 2px #00a38d;
  border-radius: 3px;
}

</style>


<i18n>

</i18n>