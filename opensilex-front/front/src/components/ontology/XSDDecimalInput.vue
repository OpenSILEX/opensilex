<template>
  <opensilex-InputForm
    :value.sync="internalValue"
    :label="property.name"
    type="number"
    rules="decimal"
    :disabled="false"
    :required="property.is_required"
    :helpMessage="property.comment"
    placeholder="XSDDecimalInput.placeholder"
    ></opensilex-InputForm>
</template>

<script lang="ts">
import {
  Component,
  Prop,
  PropSync,
} from "vue-property-decorator";
import Vue from "vue";

@Component
export default class XSDDecimalInput extends Vue {

  @Prop()
  property;

  @PropSync("value")
  internalValue;
}
</script>

<style scoped lang="scss">
</style>

<i18n>

en:
    XSDDecimalInput:
        placeholder: "Enter a decimal number, ex : 8611.53"
fr:
    XSDDecimalInput:
        placeholder: "Saisir un nombre décimal, ex : 8611.53"
</i18n>

