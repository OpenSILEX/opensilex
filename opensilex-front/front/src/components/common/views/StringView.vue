<template>
  <div class="static-field">
    <span class="field-view-title">{{$t(label)}}:</span>
    <span class="static-field-line capitalize-first-letter">
      <slot>{{value}}</slot>
    </span>
  </div>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class StringView extends Vue {
  @Prop()
  label: string;

  @Prop()
  value: string;
}
</script>

<style scoped lang="scss">
</style>

