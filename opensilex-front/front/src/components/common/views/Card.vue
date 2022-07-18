<template>
  <div class="card">
    <div class="card-header">
      <h3>
        <opensilex-Icon :icon="icon" class="icon-title" />&nbsp;
        <span class="capitalize-first-letter">{{ $t(label) }}</span> &nbsp;
      </h3>
      <slot name="header"></slot>
      <div class="card-header-right">
        <slot name="rightHeader"></slot>
      </div>
    </div>
    <div class="card-body">
      <slot name="body"></slot>
    </div>
    <div class="card-footer text-center">
      <slot name="footer"></slot>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class DescriptionCard extends Vue {
  $opensilex: any;
  $store: any;
  $router: any;
  $t: any;
  $i18n: any;

  @Prop()
  label: string;
  @Prop()
  icon: string;
}
</script>

<style scoped lang="scss">
</style>
