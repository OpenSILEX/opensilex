<template>
  <div class="card-body p-0">
    <slot></slot>
  </div>
</template>

<script lang="ts">
import {
  Component,
  Prop,
  Model,
  Provide,
  PropSync
} from "vue-property-decorator";
import Vue from "vue";

@Component
export default class PageContent extends Vue {}
</script>

<style scoped lang="scss">
</style>

