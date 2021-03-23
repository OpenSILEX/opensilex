<template>
    <opensilex-InputForm
    :value.sync="internalValue"
    :label="property.name"
    type="text"
    :disabled="false"
    :required="property.is_required"
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
export default class XSDStringInput extends Vue {
  $opensilex: any;

  @Prop()
  property;

  @PropSync("value")
  internalValue;
  
}
</script>

<style scoped lang="scss">
</style>

