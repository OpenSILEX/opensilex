<template>
  <div>
    <opensilex-GermplasmSelector
      :required="property.isRequired"
      :multiple="property.isList"
      :germplasm.sync="internalValue"
      :experimentURI="context.experimentURI"
    ></opensilex-GermplasmSelector>
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

