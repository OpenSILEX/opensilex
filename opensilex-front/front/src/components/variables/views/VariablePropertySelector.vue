<!--
  - OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
  - Copyright © INRAE 2022
  - Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
  -->

<template>
  <opensilex-VariableSelectorWithFilter
      :label="property.name"
      :variables.sync="internalValue"
      :maximumSelectedRows="property.is_list ? undefined : 1"
      :required="property.is_required"
  ></opensilex-VariableSelectorWithFilter>
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