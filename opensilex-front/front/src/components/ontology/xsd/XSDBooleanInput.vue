<template>
    <CheckboxForm
      v-model:value="internalValue"
      :label="property.name"
      :title="internalValue ? $t('component.common.yes') : $t('component.common.no')"
      :helpMessage="property.comment"
    ></CheckboxForm>
</template>

<script setup lang="ts">

import CheckboxForm from "@/components/common/forms/CheckboxForm.vue";

defineProps<{
  property: {
    name: string;
    is_required: boolean;
    comment?: string;
  };
}>();

const internalValue = defineModel<string>("value");

</script>

<style scoped lang="scss">
</style>

