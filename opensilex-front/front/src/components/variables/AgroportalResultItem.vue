<template>
    <b-container class="result"
         v-on:click="$emit('item-clicked', index)"
      :class="(isSelected) ? 'selectedResult' : ''">

      <b-row id="result-header" class="mx-0 jqx-max-size">
        <b-col col lg="12">
          <div id="result-name">
            {{entity.name}} -
            <span id="result-ontology">{{getOntologyAcronym(entity.links.ontology)}}</span>
          </div>
          <div id="result-link">
            <a v-bind:href="entity.id" target="_blank" rel="noopener noreferrer">{{entity.id}}</a>
          </div>
        </b-col>
      </b-row>

      <b-row id="result-body" class="mx-0 jqx-max-size">
        <b-col col lg="12" id="result-definition">
          {{entity.definitions[0]}}
        </b-col>
      </b-row>

      <b-row v-if="isSelected" align-h="end">
        <b-col cols="auto">
          <slot name="btnValidate"></slot>
        </b-col>
      </b-row>

    </b-container>
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

  @Prop()
  index;

  isSelected: boolean = false;

  getOntologyAcronym(url: string) {
    return url.split('/').pop();
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

a {
  color: #007bff;
}

.result {
  font-size: medium;
  margin-bottom: 10px;
  padding: 10px;
  margin-right: 1px;
}

.result:hover {
  background: rgba(0, 0, 0, .1);
}

.selectedResult {
  border: solid 2px #00a38d;
  border-radius: 3px;
}

</style>


<i18n>

</i18n>