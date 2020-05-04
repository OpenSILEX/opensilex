<template>
  <div class="card">
    <div class="card-header row clearfix">
      <div class="col col-sm-3">
        <div class="d-inline-block">
          <slot></slot>
        </div>
      </div>
    </div>
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
export default class PageActions extends Vue {}
</script>

<style scoped lang="scss">
</style>

