<template>
  <div>SpeciesEdit - {{value}}</div>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class SpeciesEdit extends Vue {
  @Prop()
  value: any;
}
</script>

<style scoped lang="scss">
</style>
