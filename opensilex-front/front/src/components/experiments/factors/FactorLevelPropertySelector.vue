<template>
    <opensilex-FactorLevelSelector
      v-if="context && context.experimentURI"
      :label="property.name"
      :factorLevels.sync="internalValue"
      :multiple="property.is_list"
      :required="property.is_required"
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