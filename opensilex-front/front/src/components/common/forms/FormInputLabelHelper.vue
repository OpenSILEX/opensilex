<template v-slot:label>
  <span>
    <label class="form-label" :for="labelFor">{{$t(label)}}</label>
    &nbsp;
    <font-awesome-icon v-if="helpMessage" tabindex="0" icon="question-circle" v-b-tooltip.hover.top="$t(helpMessage)" />
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

