<template>
   <opensilex-TextAreaForm
      :value.sync="internalValue"
      :label="property.name"
      :required="property.is_required"
      :helpMessage="property.comment"
      placeholder="XSDLongStringInput.placeholder"
   ></opensilex-TextAreaForm>
</template>

<script lang="ts">
import {
  Component,
  Prop,
  PropSync
} from "vue-property-decorator";
import Vue from "vue";

@Component
export default class XSDLongStringInput extends Vue {

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
    XSDLongStringInput:
        placeholder: "Enter a long text, ex : OpenSILEX is an ontology-driven Information System designed for life science data."
fr:
    XSDLongStringInput:
        placeholder: "Saisissez un texte long, ex : OpenSILEX est un système d'information guidé par des ontologies, conçu pour les données issues des sciences de la vie "
</i18n>

