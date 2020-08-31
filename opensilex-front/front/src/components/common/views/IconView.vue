<template>
  <div class="static-field">
    <span class="field-view-title">{{$t(label)}}:</span>
    <span class="static-field-line" v-if="value">
      <opensilex-Icon :icon="value"></opensilex-Icon>&nbsp;({{value}})
    </span>
  </div>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class IconView extends Vue {
  @Prop()
  label: string;

  @Prop()
  value: string;
}
</script>

<style scoped lang="scss">
</style>

