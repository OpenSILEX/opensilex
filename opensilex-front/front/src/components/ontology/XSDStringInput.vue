<template>
    <opensilex-InputForm
    :value.sync="internalValue"
    :label="property.name"
    type="text"
    :disabled="false"
    :required="property.is_required"
    :helpMessage="property.comment"
    placeholder="XSDStringInput.placeholder"
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
export default class XSDStringInput extends Vue {
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
    XSDStringInput:
        placeholder: "Saisissez un nombre entier"
en:
    XSDStringInput:
        placeholder: "Enter an number"
</i18n>