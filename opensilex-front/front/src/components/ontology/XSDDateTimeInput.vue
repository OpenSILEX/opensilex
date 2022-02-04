<template>
<div>
  <opensilex-DateTimeForm
    :value.sync="internalValue"
    :label="property.name"
    :disabled="false"
    :required="property.is_required"
    :helpMessage="property.comment"
  ></opensilex-DateTimeForm>
</div>
</template>

<script lang="ts">
import {Component, Prop, PropSync} from "vue-property-decorator";
import Vue from "vue";

@Component
export default class XSDDateTimeInput extends Vue {

  @Prop()
  property;

  @PropSync("value")
  internalValue;
}
</script>

<style scoped lang="scss">
</style>

