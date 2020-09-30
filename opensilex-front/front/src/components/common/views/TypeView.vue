<template>
  <div class="static-field">
    <span class="field-view-title">{{$t("component.common.type")}}</span>
    <span class="static-field-line">
      <opensilex-Icon :icon="$opensilex.getRDFIcon(type)" />&nbsp;
      <span class="capitalize-first-letter">{{typeLabel}}</span>
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
}
</script>

<style scoped lang="scss">
</style>

