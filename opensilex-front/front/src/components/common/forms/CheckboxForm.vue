<template>
  <opensilex-FormField
    :rules="rules"
    :required="required"
    :label="label"
    :helpMessage="helpMessage"
  >
    <template v-slot:field="field">
      <b-form-checkbox
        :id="field.id"
        v-model="booleanValue"
        :value="true"
        :disabled="disabled"
        :required="required"
        :unchecked-value="false"
        switches
      >{{$t(title)}}</b-form-checkbox>
    </template>
  </opensilex-FormField>
</template>

<script lang="ts">
import {
  Component,
  Prop,
  Model,
  Provide,
  PropSync
} from "vue-property-decorator";
import Vue from "vue";

@Component
export default class CheckboxForm extends Vue {
  $opensilex: any;

  @PropSync("value")
  booleanValue: boolean;

  @Prop()
  label: string;

  @Prop()
  title: string;

  @Prop()
  helpMessage: string;

  @Prop()
  required: boolean;

  @Prop()
  disabled: boolean;

  @Prop()
  rules: string | Function;
}
</script>

<style scoped lang="scss">
</style>

