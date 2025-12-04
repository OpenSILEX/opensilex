<template>
  <div>
    <opensilex-GeneticResourceSelector
      :label="property.name"
      :required="property.is_required"
      :multiple="property.is_list"
      :geneticResource.sync="internalValue"
    ></opensilex-GeneticResourceSelector>
  </div>
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
export default class GeneticResourcePropertySelector extends Vue {
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

