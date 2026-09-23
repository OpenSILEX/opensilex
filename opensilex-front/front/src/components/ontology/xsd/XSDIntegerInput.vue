<template>
  <n-form-item :rule="rule" :show-require-mark="property.is_required">
  <InputForm
    v-model:value="internalValue"
    type="number"
    rules="integer"
    :disabled="false"
    :required="property.is_required"
    :helpMessage="property.comment"
    :placeholder="$t('XSDIntegerInput.placeholder')"
  ></InputForm>
  </n-form-item>
</template>

<script setup lang="ts">

  import InputForm from "@/components/common/forms/InputForm.vue";
  import {FormItemRule, NFormItem} from "naive-ui";
  import {VueRDFTypePropertyDTO} from "@/lib";
  import {computed} from "vue";

  defineProps<{
    property: VueRDFTypePropertyDTO
    name: string;
    is_required: boolean;
    comment?: string;
}>();

const internalValue = defineModel<string>("value");

  const rule = computed<FormItemRule>(() => {
    return {
      required: false,
      message: "A traduire",
      trigger: ['change', 'blur']
    }
  })

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