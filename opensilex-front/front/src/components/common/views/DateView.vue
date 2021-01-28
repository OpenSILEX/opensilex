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

  @Prop()
  isDatetime: boolean;

  formatDate(value: string): string {
    if(this.isDatetime){
      return this.$opensilex.formatDateTime(value); 
    }else{
      return this.$opensilex.formatDate(value);
    } 
  }
}
</script>

<style scoped lang="scss">
</style>

