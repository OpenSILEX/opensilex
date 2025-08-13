<template>
  <opensilex-FormField
    :rules="rules"
    :required="required"
    :label="label"
    :helpMessage="helpMessage"
  >
    <template v-slot:field="field">
      <b-form-textarea
        v-bind="$attrs"
        v-on="$listeners"
        :id="field.id"
        v-model="stringValue"
        :disabled="disabled"
        :required="required"
        :placeholder="$t(placeholder)"
        :autocomplete="autocomplete"
      ></b-form-textarea>
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

@Component({
  inheritAttrs: false
})
export default class TextAreaForm extends Vue {
  $opensilex: any;

  @PropSync("value")
  stringValue: string;

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

