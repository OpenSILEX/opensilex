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
        :value="stringValue"
        @update="updateValue($event)"
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

  updateValue(newValue) {
    if (this.type == "date" && newValue == "") {
      this.stringValue = null;
    } else {
      this.stringValue = newValue;
    }
  }

  @Prop({
    default: "text"
  })
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

