<template v-slot:label>
  <span>
    <label class="form-label" :for="labelFor">
      {{$t(label)}}
      &nbsp;
      <font-awesome-icon
        v-if="helpMessage"
        tabindex="0"
        icon="question-circle"
        v-b-tooltip.hover.top.html="$t(helpMessage)"
      />
    </label>
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
.form-label {
  display: inline-block;
  min-width: 100%;
  margin-bottom: 5px;
}
</style>

