<template>
  <n-form-item :rule="rule" :show-require-mark="property.is_required" ref="formItemRef">
  <InputForm
    v-model:value="internalValue"
    type="number"
    rules="decimal"
    :disabled="false"
    :required="property?.is_required"
    :helpMessage="property?.comment"
    :placeholder="t('component.ontology.externalOntologies.XSD-placehorlder.XSDDecimalInput-placeholder')"
  />
  </n-form-item>
</template>

<script setup lang="ts">
import {computed, useTemplateRef, watch} from 'vue'
import type { VueRDFTypePropertyDTO } from '@/lib'
import { useI18n } from 'vue-i18n'
import InputForm from "@/components/common/forms/InputForm.vue";
import {FormItemRule, NFormItem} from "naive-ui";

const { t } = useI18n()
const formItemRef = useTemplateRef<InstanceType<typeof NFormItem>>('formItemRef')
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

const rule = computed<FormItemRule>(() => ({
  trigger: ['blur'],
  validator: () => {
    const value = internalValue.value;
    const isEmpty = value === undefined || value === null || String(value).trim() === ""
    // field required
    if (isEmpty) {
      return props.property.is_required
          ? new Error(t("validations.required_if", {_field_: props.property.name}))
          : true;
    }
    // integer only (no decimal)
    if (!/^-?\d+\.\d+$/.test(String(value).trim())) {
      return new Error(t("validations.decimal", {_field_: props.property.name})
      );
    }
    return true;
  }
}));

watch(
    () => props.value,
    (newVal) => {
      if (newVal) {
        formItemRef.value?.validate()
      }
    }
);
</script>
