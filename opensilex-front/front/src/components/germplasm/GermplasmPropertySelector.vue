<template>
  <div>
    <GermplasmSelector
      :label="property.name"
      :required="property.is_required"
      :multiple="property.is_list"
      v-model:germplasm="internalValue"
    ></GermplasmSelector>
  </div>
</template>

<script setup lang="ts">

  import GermplasmSelector from "@/components/germplasm/GermplasmSelector.vue";

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

