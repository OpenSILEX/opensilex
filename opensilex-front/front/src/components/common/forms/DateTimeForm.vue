<template>
  <opensilex-FormField
    :required="required"
    :label="label"
    :helpMessage="helpMessage"
  >
    <template v-slot:field="field">
      <b-form-input
        :id="field.id"
        :value="stringValue"
        @update="updateValue($event)"
        @change="change"
        :disabled="disabled"
        type="text"
        :required="isRequired"
        :placeholder="$t(placeholder)"
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
export default class DateTimeForm extends Vue {
  $opensilex: any;

  @PropSync("value")
  stringValue: string;

  updateValue(newValue) {
    if (newValue == "") {
      this.stringValue = null;
    } else {
      this.stringValue = newValue;
    }
  }

  change(value){
    this.$emit("change",value);
  }

  @Prop()
  label: string;

  @Prop()
  helpMessage: string;

  @Prop()
  placeholder: string;

  @PropSync("required")
  isRequired: boolean;

  @Prop()
  disabled: boolean;

}
</script>

<style scoped lang="scss">
</style>

