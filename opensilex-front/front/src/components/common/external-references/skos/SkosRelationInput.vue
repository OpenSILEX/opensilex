<template>
  <ValidationProvider
      :name="$t('component.skos.uri')"
      :rules="{
        required: true,
        regex: uriRegex
      }"
      v-slot="{ errors }"
  >
    <b-input-group>
      <b-input
          class="v-step-manual-uri"
          v-model.trim="uri"
          type="text"
          required
          :placeholder="$t('component.skos.uri-placeholder')"
      >
      </b-input>
      <template #append>
        <opensilex-SkosSelector
            :selectedRelation.sync="relationDtoKey"
            @selected="onSelected"
        ></opensilex-SkosSelector>
      </template>
    </b-input-group>
    <div class="error-message alert alert-danger">{{ errors[0] }}</div>
  </ValidationProvider>
</template>

<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';

import {UriSkosRelation} from "../../../../models/SkosRelations";

@Component({})
export default class SkosRelationInput extends Vue {
  //region Data
  private uri: string = "";
  private relationDtoKey: string = null;
  //endregion

  //region Const data
  //@todo revoir cette regex qui me semble trop limitante
  private readonly uriRegex =
      /^(http:\/\/|https:\/\/)?[a-z0-9]+([-.]{1}[a-z0-9]+)*\.[a-z]{2,5}(:[0-9]{1,5})?(\/.*)?$/;
  //endregion

  //region Events
  private emitInput() {
    this.$emit("input", {
      uri: this.uri,
      relationDtoKey: this.relationDtoKey
    } as UriSkosRelation);
  }
  //endregion

  //region Event handlers
  private onSelected() {
    this.emitInput();
  }
  //endregion
}
</script>

<style scoped lang="scss">

</style>