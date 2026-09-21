<template>
<div>
  <DateTimeForm
    v-model:value="internalValue"
    :label="property.name"
    :disabled="false"
    :required="property.is_required"
    :helpMessage="property.comment"
  ></DateTimeForm>
</div>
</template>

<script setup lang="ts">

import DateTimeForm from "@/components/common/forms/DateTimeForm.vue";

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

