<template>
  <n-form-item :rule="rule" :show-require-mark="property.is_required" :path="property.uri" ref="formItemRef">
    <InputForm
        v-model:value="internalValue"
        type="url"
        :disabled="false"
        :required="property?.is_required"
        :helpMessage="property?.comment"
        :placeholder="t('component.ontology.externalOntologies.XSD-placehorlder.XSDUriInput-placeholder')"
    />
  </n-form-item>
</template>

<script setup lang="ts">
import {computed, useTemplateRef, watch} from 'vue'
import type { VueRDFTypePropertyDTO } from '@/lib'
import { useI18n } from 'vue-i18n'
import {FormItemRule, NFormItem} from "naive-ui";
import InputForm from "@/components/common/forms/InputForm.vue";

const { t } = useI18n()
const formItemRef = useTemplateRef<InstanceType<typeof NFormItem>>('formItemRef')

const props = defineProps<{
  property: VueRDFTypePropertyDTO
  value?: string
}>()

const emit = defineEmits<{
  (e: 'update:value', value: string | undefined): void
}>()

const internalValue = computed({
  get() {
    return props.value
  },
  set(value: string | undefined) {
    emit('update:value', value)
  }
})

const rule = computed<FormItemRule[]>(() => [
  {
    required: props.property.is_required,
    validator: () => {
      const value = props.value?.trim()
      if (!value) {
        return props.property.is_required
            ? new Error(t('validations.required_if', {_field_: props.property.name}))
            : true
      }
      try {
        new URL(value)
        return true
      } catch {
        return new Error(t('validations.url', {_field_: props.property.name}))
      }
    },
    trigger: ['input', 'blur']
  }
]);

watch(
    () => props.value,
    (newVal) => {
      if (newVal) {
        formItemRef.value?.validate()
      }
    }
);


</script>
