<template v-slot:label>
  <span>
    <span> <b> {{$t(label)}} </b> </span>
    <font-awesome-icon icon="question-circle" v-b-tooltip.hover :title="$t(helpMessage)" />
  </span>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class FormInputLabelHelper extends Vue {
  $opensilex: any;
  $store: any;

  @Prop()
  helpMessage: string;

  @Prop()
  label;

  @Prop()
  labelFor;
}
</script>

<style scoped lang="scss">
</style>

