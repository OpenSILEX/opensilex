<template>
  <DateForm
    :disabled="false"
    :label="property.name"
    :required="property.is_required"
    v-model:value="internalValue"
    :helpMessage="property.comment"
  ></DateForm>
</template>

<script setup lang="ts">
import DateForm from "@/components/common/forms/DateForm.vue";


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

