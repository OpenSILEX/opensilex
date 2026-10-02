<template>
    <FactorLevelSelector
      v-if="context && context.experimentURI"
      :label="property.name"
      v-model:factorLevels="internalValue"
      :multiple="property.is_list"
      :required="property.is_required"
      :experimentURI="context.experimentURI"
    ></FactorLevelSelector>
</template>

<script setup lang="ts">

  import FactorLevelSelector from "@/components/experiments/factors/FactorLevelSelector.vue";

  const props = defineProps<{
    property: {
      name: string;
      is_required: boolean;
      is_list: boolean;
      target_property: string;
      comment?: string;
    };
    context?: {
      experimentURI?: string;
      organizationURI?: string;
    };
  }>();
  const internalValue = defineModel<string>("value");
</script>

<style scoped lang="scss">
</style>