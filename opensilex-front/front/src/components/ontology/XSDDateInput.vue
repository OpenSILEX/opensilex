<template>
  <opensilex-FormField v-if="property"
    rules="integer"
    :required="property.required"
    :label="property.label"
    :helpMessage="property.comment"
  >
    <template v-slot:field="field">
      <b-form-input
        :id="field.id"
        v-model="internalValue"
        type="date"
        step="any"
        :required="property.required"
      ></b-form-input>
    </template>
  </opensilex-FormField>
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
export default class XSDDateInput extends Vue {
  $opensilex: any;

  @Prop()
  property;

  @PropSync("value")
  internalValue;

}
</script>

<style scoped lang="scss">
</style>

