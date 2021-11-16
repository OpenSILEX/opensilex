<template>
  <opensilex-DateForm
    :disabled="false"
    :label="property.name"
    :required="property.is_required"
    :value.sync="internalValue"
    :helpMessage="property.comment"
  ></opensilex-DateForm>
</template>

<script lang="ts">
import {
  Component,
  Prop,
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

