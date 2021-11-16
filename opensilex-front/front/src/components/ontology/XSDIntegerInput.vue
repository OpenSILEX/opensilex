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
  Model,
  Provide,
  PropSync
} from "vue-property-decorator";
import Vue from "vue";

@Component
export default class XSDIntegerInput extends Vue {
  $opensilex: any;

  @Prop()
  property;

  @PropSync("value")
  internalValue;
}
</script>

<style scoped lang="scss">
</style>

<i18n>

fr:
    XSDIntegerInput:
        placeholder: "Saisissez un nombre entier"
en:
    XSDIntegerInput:
        placeholder: "Enter an number"
</i18n>