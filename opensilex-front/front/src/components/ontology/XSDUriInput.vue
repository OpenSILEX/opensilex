<template>
  <opensilex-InputForm
    :value.sync="internalValue"
    :label="property.name"
    type="text"
    rules="url"
    :disabled="false"
    :required="property.is_required"
    :helpMessage="property.comment"
    placeholder="XSDUriInput.placeholder"
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
export default class XSDUriInput extends Vue {
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
    XSDUriInput:
        placeholder: "Saisissez un nombre entier"
en:
    XSDUriInput:
        placeholder: "Enter an number"
</i18n>


