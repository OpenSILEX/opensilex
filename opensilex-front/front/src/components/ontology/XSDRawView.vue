<template>
  <span class="static-field-line">{{value}}</span>
</template>

<script lang="ts">
import {
  Component,
  Prop
} from "vue-property-decorator";
import Vue from "vue";

@Component
export default class XSDRawView extends Vue {
  $opensilex: any;

  @Prop()
  value;
}
</script>

<style scoped lang="scss">
</style>

