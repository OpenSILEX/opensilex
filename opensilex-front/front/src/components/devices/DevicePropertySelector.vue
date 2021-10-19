<template>
  <opensilex-DeviceSelector
      v-if="property"
      :label="property.name"
      :value.sync="internalValue"
      :multiple="property.is_list"
      :required="property.is_required"
  ></opensilex-DeviceSelector>
</template>

<script lang="ts">
import {
  Component,
  Prop,
  PropSync
} from "vue-property-decorator";
import Vue from "vue";

@Component
export default class DevicePropertySelector extends Vue {

  @Prop()
  property;

  @PropSync("value")
  internalValue;
}
</script>
