<template>
  <opensilex-InputForm
    :value.sync="internalValue"
    :label="property.name"
    type="number"
    rules="integer"
    :disabled="false"
    :required="property.is_required"
    :helpMessage="property.comment"
    placeholder="XSDIntegerInput.placeholder"
  ></opensilex-InputForm>
</template>

<script lang="ts">
import {
  Component,
  Prop,
  PropSync
} from "vue-property-decorator";
import Vue from "vue";

@Component
export default class XSDIntegerInput extends Vue {

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
    XSDIntegerInput:
        placeholder: "Enter an integer number, ex : 8611"
fr:
    XSDIntegerInput:
        placeholder: "Saisir un nombre entier, ex : 8611"
</i18n>