<template>
  <div>
    <opensilex-SpeciesSelector
      :required="property.isRequired"
      :multiple="false"
      :species.sync="internalValue"
      :experimentURI="context.experimentURI"
    ></opensilex-SpeciesSelector>
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
export default class GermplasmPropertySelector extends Vue {
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

