<template>
  <div class="static-field">

    <span v-if="displayLabel" class="field-view-title">{{ $t(label) }}</span>
    <span class="static-field-line capitalize-first-letter">
      <slot>{{ wktValue }}</slot>
    </span>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Watch } from "vue-property-decorator";
import Vue from "vue";
import { stringify } from "wkt";

@Component
export default class StringView extends Vue {
  @Prop()
  label: string;

  @Prop()
  value: string;

  @Prop({ default: true })
  displayLabel : boolean;

  get wktValue() {
    try {
      return stringify(this.value);
    } catch (error) {
      return "";
    }
  }
}
</script>

<style scoped lang="scss">
</style>

