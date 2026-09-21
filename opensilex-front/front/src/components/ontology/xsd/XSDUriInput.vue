<template>
  <opensilex-InputForm
    v-model:value="internalValue"
    :label="property?.name"
    type="url"
    rules="url"
    :disabled="false"
    :required="property?.is_required"
    :helpMessage="property?.comment"
    :placeholder="t('XSDUriInput.placeholder')"
  />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { VueRDFTypePropertyDTO } from '@/lib'
import { useI18n } from 'vue-i18n'

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
</script>

<i18n>
en:
  XSDUriInput:
    placeholder: "Enter an URI, ex : http://www.opensilex.org/"
fr:
  XSDUriInput:
    placeholder: "Saisir un URI, ex : http://www.opensilex.org/"
</i18n>