<template>
  <n-form-item :rule="rule" :show-require-mark="property.is_required">
  <InputForm
    v-model:value="internalValue"
    type="number"
    rules="decimal"
    :disabled="false"
    :required="property?.is_required"
    :helpMessage="property?.comment"
    :placeholder="t('XSDDecimalInput.placeholder')"
  />
  </n-form-item>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { VueRDFTypePropertyDTO } from '@/lib'
import { useI18n } from 'vue-i18n'
import InputForm from "@/components/common/forms/InputForm.vue";
import {FormItemRule, NFormItem} from "naive-ui";

const { t } = useI18n()

const props = defineProps<{
    property: VueRDFTypePropertyDTO
  value?: string | number
}>()

const emit = defineEmits<{
  (e: 'update:value', value: string | number | undefined): void
}>()

const internalValue = computed({
  get() {
    return props.value
  },
  set(value: string | number | undefined) {
    emit('update:value', value)
  }
})

const rule = computed<FormItemRule>(() => {
  return {
    required: false,
    message: "A traduire",
    trigger: ['change', 'blur']
  }
})
</script>

<i18n>
en:
  XSDDecimalInput:
    placeholder: "Enter a decimal number, ex : 8611.53"
fr:
  XSDDecimalInput:
    placeholder: "Saisir un nombre décimal, ex : 8611.53"
</i18n>