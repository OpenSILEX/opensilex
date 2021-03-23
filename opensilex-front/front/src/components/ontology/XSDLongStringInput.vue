<template>
   <opensilex-TextAreaForm
      :value.sync="internalValue"
      :label="property.name"
      :required="property.is_required"
    ></opensilex-TextAreaForm>
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
export default class XSDLongStringInput extends Vue {
  $opensilex: any;

  @Prop()
  property;

  @PropSync("value")
  internalValue;

}
</script>

<style scoped lang="scss">
</style>

