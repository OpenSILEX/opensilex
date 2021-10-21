<template>
  <div class="static-field">
    <span class="field-view-title">{{$t(label)}}: </span>
    <span class="static-field-line" v-if="inline">
      <span :key="index" v-for="(value, index) in values">
        <span>
          <slot v-bind:value="value">{{ value }}</slot>
        </span>
        <span v-if="index + 1 < values.length">, </span>
      </span>
    </span>
    <div v-if="!inline">
      <ul class="static-field-list" :key="index" v-for="(value, index) in values">
        <li class="inline-action">
          <slot v-bind:value="value">{{ value }}</slot>
        </li>
      </ul>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class ListView extends Vue {
  @Prop()
  label: string;

  @Prop()
  values: Array<string>;

  @Prop({
    default: true
  })
  inline: boolean;
}
</script>

<style scoped lang="scss">
.static-field-line {
  margin-right: 3px;
}
</style>

