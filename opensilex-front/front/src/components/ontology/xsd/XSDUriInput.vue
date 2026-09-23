<template>
  <n-form-item :rule="rule" :show-require-mark="property.is_required">
    <InputForm
        v-model:value="internalValue"
        type="url"
        :disabled="false"
        :required="property?.is_required"
        :helpMessage="property?.comment"
        :placeholder="t('XSDUriInput.placeholder')"
    />
  </n-form-item>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { VueRDFTypePropertyDTO } from '@/lib'
import { useI18n } from 'vue-i18n'
import {FormItemRule, NFormItem} from "naive-ui";
import InputForm from "@/components/common/forms/InputForm.vue";

const { t } = useI18n()

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
  XSDUriInput:
    placeholder: "Enter an URI, ex : http://www.opensilex.org/"
fr:
  XSDUriInput:
    placeholder: "Saisir un URI, ex : http://www.opensilex.org/"
</i18n>