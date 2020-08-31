<template>
  <div class="static-field">
    <span class="field-view-title">{{$t(label)}}:</span>
    <span class="static-field-line capitalize-first-letter">
      <slot>{{value ? $t("component.common.yes") : $t("component.common.no")}}</slot>
    </span>
  </div>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class BooleanView extends Vue {
  @Prop()
  label: string;

  @Prop()
  value: boolean;
}
</script>

<style scoped lang="scss">
</style>

