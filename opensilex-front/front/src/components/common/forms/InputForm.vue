<template>
  <opensilex-FormField
    :rules="rules"
    :required="required"
    :label="label"
    :helpMessage="helpMessage"
  >
    <template v-slot:field="field">
      <b-form-input
        :id="field.id"
        v-model="stringValue"
        :disabled="disabled"
        :type="type"
        :required="required"
        :placeholder="$t(placeholder)"
        :autocomplete="autocomplete"
      ></b-form-input>
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
export default class InputForm extends Vue {
  $opensilex: any;

  @PropSync("value")
  stringValue: string;

  @Prop()
  type: string;

  @Prop()
  label: string;

  @Prop()
  helpMessage: string;

  @Prop()
  placeholder: string;

  @Prop()
  required: boolean;

  @Prop()
  disabled: boolean;

  @Prop()
  rules: string | Function;

  @Prop()
  autocomplete: string;
}
</script>

<style scoped lang="scss">
</style>

