<!--
  - OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
  - Copyright © INRAE 2022
  - Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
  -->

<template>
  <opensilex-VariableSelector
      :label="property.name"
      :variables.sync="internalValue"
      :multiple="property.is_list"
      :required="property.is_required"
  ></opensilex-VariableSelector>
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
import {VueRDFTypePropertyDTO} from "../../../lib";

@Component
export default class VariablePropertySelector extends Vue {

  @Prop()
  property : VueRDFTypePropertyDTO;

  @PropSync("value")
  internalValue;

}
</script>

<style scoped lang="scss">
</style>