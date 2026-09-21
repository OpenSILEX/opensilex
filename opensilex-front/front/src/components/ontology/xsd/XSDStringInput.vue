<template>
  <InputForm
      v-model:value="internalValue"
      :label="property.name"
      type="text"
      :disabled="false"
      :required="property.is_required"
      :helpMessage="property.comment"
      :placeholder="$t('XSDStringInput.placeholder')"
  />
</template>

<script setup lang="ts">
import InputForm from "@/components/common/forms/InputForm.vue";

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

<i18n>
en:
  XSDStringInput:
    placeholder: "Enter text, ex : Opensilex"

fr:
  XSDStringInput:
    placeholder: "Saisir du texte, ex : Opensilex"
</i18n>