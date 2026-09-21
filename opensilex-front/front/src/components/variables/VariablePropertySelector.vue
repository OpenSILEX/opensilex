<template>
  <!--
  <opensilex-VariableSelectorWithFilter
      :label="property.name"
      :variables.sync="internalValue"
      :maximumSelectedRows="property.is_list ? undefined : 1"
      :required="property.is_required"
  ></opensilex-VariableSelectorWithFilter>
  -->

  <!-- Temporary usage of the old VariableSelector, since
       VariableSelectorWithFilter doesn't display variables
       when loading the form in edit mode. -->
  <VariableSelector
      :label="property.name"
      :variables="internalValue"
      @update:variables="internalValue = $event"
      :multiple="property.is_list"
      :required="property.is_required"
  />
</template>

<script setup lang="ts">
import { computed } from "vue";
import VariableSelector from "@/components/variables/views/VariableSelector.vue";
import {VueRDFTypePropertyDTO} from "@/lib";

const props = defineProps<{
  property: VueRDFTypePropertyDTO;
  value: unknown;
}>();

const emit = defineEmits<{
  (e: "update:value", value: unknown): void;
}>();

const internalValue = computed({
  get: () => props.value,
  set: (value) => emit("update:value", value),
});
</script>

<style scoped lang="scss">
</style>