<template>
  <span>{{formatDate(value)}}</span>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";

@Component
export default class DateView extends Vue {
  $i18n: any;
  $opensilex: OpenSilexVuePlugin;

  @Prop()
  value: string;

  @Prop()
  isDatetime: boolean;

  formatDate(value: string): string {
    if(this.isDatetime){
      return this.$opensilex.$dateTimeFormatter.formatISODateTime(value);
    }else{
      return this.$opensilex.$dateTimeFormatter.formatISODate(value);
    } 
  }
}
</script>

<style scoped lang="scss">
</style>

