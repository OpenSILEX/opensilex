<template>
  <div class="static-field">
    <span class="field-view-title">{{$t("component.common.type")}}</span>
    <span class="static-field-line">
      <opensilex-Icon :icon="$opensilex.getRDFIcon(type)" />&nbsp;
      <opensilex-UriLink
        v-if="copyableTypeUri"
        :uri="$opensilex.getShortUri(type)"
        :value="typeLabel"
      ></opensilex-UriLink>
      <span v-else class="capitalize-first-letter">{{typeLabel}}</span>
    </span>
  </div>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class TypeView extends Vue {
  @Prop()
  type: string;

  @Prop()
  typeLabel: string;

  /**
   * If this is set to true then a uri link is used instead of a simple string for the type value
   */
  @Prop({default: false})
  copyableTypeUri: boolean
}
</script>

<style scoped lang="scss">
</style>

