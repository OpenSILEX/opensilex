<template>
  <span class="static-field-line">{{$n(value)}}</span>
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
export default class XSDNumberView extends Vue {
  $opensilex: any;

  @Prop()
  value;
}
</script>

<style scoped lang="scss">
</style>

