<template>
  <span>{{formatDate(value)}}</span>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";
import moment from "moment";

@Component
export default class DateView extends Vue {
  $i18n: any;
  $opensilex: any;
  @Prop()
  value: string;

  formatDate(value: string): string {
    return this.$opensilex.formatDate(value);
  }
}
</script>

<style scoped lang="scss">
</style>

