<template>
    <opensilex-FactorLevelSelector
      v-if="context && context.experimentURI"
      label="FactorLevelSelector.label"
      :factorLevels.sync="internalValue"
      :multiple="property.isList"
      :required="property.isRequired"
      :experimentURI="context.experimentURI"
    ></opensilex-FactorLevelSelector>
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
export default class FactorLevelPropertySelector extends Vue {
  $opensilex: any;

  @Prop()
  property;

  @PropSync("value")
  internalValue;

  @Prop()
  context;
}
</script>

<style scoped lang="scss">
</style>