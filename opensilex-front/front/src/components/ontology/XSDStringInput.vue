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
  PropSync
} from "vue-property-decorator";
import Vue from "vue";

@Component
export default class XSDStringInput extends Vue {

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
    XSDStringInput:
        placeholder: "Enter text, ex : Opensilex"
fr:
    XSDStringInput:
        placeholder: "Saisir du texte, ex : Opensilex"
</i18n>