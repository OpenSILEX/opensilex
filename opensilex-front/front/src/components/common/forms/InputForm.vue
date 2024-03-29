<template>
  <opensilex-FormField
    :rules="rules"
    :required="isRequired"
    :label="label"
    :helpMessage="helpMessage"
    :vid="vid"
  >
    <template v-slot:field="field">
      <b-form-input
        :ref="inputRef"
        :id="field.id"
        :value="stringValue"
        @update="updateValue($event)"
        @change="change"
        @input="input"
        @blur="blur"
        @keyup.enter.native="onEnter"
        :disabled="disabled"
        :type="type"
        :required="isRequired"
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
  PropSync,
  Ref
} from "vue-property-decorator";
import Vue from "vue";

@Component
export default class InputForm extends Vue {
  @Ref("inputRef") readonly inputRef!: any;

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

  change(value){
    this.$emit("change",value);
  }

  input(value) {
    this.$emit("input", value);
  }

  blur() {
    this.$emit("blur");
  }

  onEnter() {
    this.$emit("handlingEnterKey")
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

  @PropSync("required")
  isRequired: boolean;

  @Prop()
  disabled: boolean;

  @Prop()
  rules: string | Function;

  @Prop()
  autocomplete: string;

  @Prop()
  vid;

}
</script>

<style scoped lang="scss">

// Remove arrows on number input
/* Chrome, Safari, Edge, Opera */
input::-webkit-outer-spin-button,
input::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

/* Firefox */
input[type=number] {
  -moz-appearance: textfield;
}

</style>

