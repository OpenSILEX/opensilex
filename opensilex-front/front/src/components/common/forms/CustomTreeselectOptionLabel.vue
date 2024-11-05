<template>
<div>
  <slot name="option-label" v-bind:node="node">
    <div class="label" :title="node.label">{{ node.label }}</div>
  </slot>
</div>
</template>

<script lang="ts">
import { Component, Prop, PropSync, Ref } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class CustomTreeselectOptionLabel extends Vue {
  $opensilex: any;

}
</script>