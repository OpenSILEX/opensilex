<template>
   <opensilex-TextAreaForm
      :value.sync="internalValue"
      :label="property.name"
      :required="property.is_required"
      :helpMessage="property.comment"
      placeholder="XSDStringInput.placeholder"
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

