<template>
    <b-container class="result"
       @click="emitClicked"
      :class="{
         selectedResult: isSelected
      }"
    >
      <!-- Name -->
      <b-row id="result-header" class="mx-0">
        <b-col col lg="12">
          <div id="result-name">
            {{ entity.name }} - <span id="result-ontology">{{entity.ontologyName}}</span>
          </div>
          <div id="result-link">
            <a v-bind:href="entity.id" target="_blank" rel="noopener noreferrer">{{entity.id}}</a>
          </div>
        </b-col>
      </b-row>

      <!-- Definition -->
      <b-row id="result-body" class="mx-0">
        <b-col col lg="12" id="result-definition">
          {{entity.definitions[0]}}
        </b-col>
      </b-row>

      <!-- Select button -->
      <b-row v-if="isSelected" align-h="end">
        <b-col cols="auto">
          <slot name="validationButton"></slot>
        </b-col>
      </b-row>
    </b-container>
</template>


<script lang="ts">

import {Component, Prop} from "vue-property-decorator";
import Vue from 'vue';
import OpenSilexVuePlugin from "../../../../models/OpenSilexVuePlugin";
import {AgroportalTermDTO} from "opensilex-core/index";

@Component
export default class AgroportalResultItem extends Vue {
  //#region Props
  @Prop()
  private readonly entity: AgroportalTermDTO;
  //#endregion

  //#region Data
  private isSelected: boolean = false;
  //#endregion

  //#region Public methods
  public setSelected(selected: boolean) {
    this.isSelected = selected;
  }
  //#endregion

  //#region Events
  private emitClicked() {
    this.$emit('item-clicked');
  }
  //#endregion
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
</style>

<i18n>

</i18n>