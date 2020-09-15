<template>
  <span class="static-field-text">{{value}}</span>
</template>

<script lang="ts">
import {
  Component,
  Prop,
  Model,
  Provide,
  PropSync,
  Watch
} from "vue-property-decorator";
import Vue from "vue";

@Component
export default class XSDLongStringView extends Vue {
  $opensilex: any;

  @Prop()
  value;
}
</script>

<style scoped lang="scss">
</style>

