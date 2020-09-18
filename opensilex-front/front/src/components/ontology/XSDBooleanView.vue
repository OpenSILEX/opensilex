<template>

  <span class="static-field-line">{{value=="true" ? $t("component.common.yes") : $t("component.common.no")}}</span>
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
export default class XSDBooleanView extends Vue {
  $opensilex: any;

  @Prop()
  value;
}
</script>

<style scoped lang="scss">
</style>

