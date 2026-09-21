<template>
  <InputForm
    v-model:value="internalValue"
    :label="property.name"
    type="number"
    rules="integer"
    :disabled="false"
    :required="property.is_required"
    :helpMessage="property.comment"
    :placeholder="$t('XSDIntegerInput.placeholder')"
  ></InputForm>
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
    XSDIntegerInput:
        placeholder: "Enter an integer number, ex : 8611"
fr:
    XSDIntegerInput:
        placeholder: "Saisir un nombre entier, ex : 8611"
</i18n>