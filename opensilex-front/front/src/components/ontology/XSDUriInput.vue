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
  PropSync
} from "vue-property-decorator";
import Vue from "vue";

@Component
export default class XSDUriInput extends Vue {

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
    XSDUriInput:
        placeholder: "Enter an URI, ex : http://www.opensilex.org/"
fr:
    XSDUriInput:
        placeholder: "Saisir un URI, ex : http://www.opensilex.org/"
</i18n>


