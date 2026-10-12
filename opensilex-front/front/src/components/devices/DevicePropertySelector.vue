<template>
  <DeviceSelector
      v-if="property"
      :label="property.name"
      v-model:value="internalValue"
      :multiple="property.is_list"
      :required="property.is_required"
      :type="property.target_property"
  ></DeviceSelector>
</template>

<script setup lang="ts">
import DeviceSelector from "@/components/devices/DeviceSelector.vue";

const props = defineProps<{
  property: {
    name: string;
    is_required: boolean;
    is_list: boolean;
    target_property: string;
    comment?: string;
  };
}>();

const internalValue = defineModel<string>("value");

</script>
