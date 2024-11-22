<template>
  <ValidationProvider
      :name="$t('component.skos.uri')"
      :rules="{
        required: true,
        regex: uriRegex
      }"
      v-slot="{ errors }"
  >
    <b-input-group
      class="v-step-skos-relation-input"
    >
      <b-input
          class="v-step-skos-relation-uri-input"
          v-model.trim="uri"
          type="text"
          required
          :placeholder="$t('component.skos.uri-placeholder')"
      >
      </b-input>
      <template #append>
        <opensilex-SkosSelector
            :selectedRelation.sync="relationDtoKey"
            @update:selectedRelation="onSelected"
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
  //#region Data
  private uri: string = "";
  private relationDtoKey: string = null;
  //#endregion

  //#region Const data
  /**
   * Modified URI regex from the RFC 3986. Two elements are changed from the RFC :
   *
   * - The scheme is required
   * - The path must contain at least one character
   *
   * The resulting regex is not perfect ; for instance strings like `http://` will match. Maybe find a better one or
   * use a more advanced rule (match the RFC regex, and check that the scheme and path groups are not empty).
   *
   * @see https://datatracker.ietf.org/doc/html/rfc3986#appendix-B
   * @private
   */
  private readonly uriRegex = /^(([^:/?#]+):)(\/\/([^/?#]*))?([^?#]+)(\?([^#]*))?(#(.*))?$/;
  //#endregion

  //#region Events
  private emitInput() {
    this.$emit("input", {
      uri: this.uri,
      relationDtoKey: this.relationDtoKey
    } as UriSkosRelation);
  }
  //#endregion

  //#region Event handlers
  private onSelected() {
    this.emitInput();
  }
  //#endregion
}
</script>

<style scoped lang="scss">

</style>