<template>
  <div>
    <opensilex-FormSelector
      :label="label"
      :helpMessage="helpMessage"
      v-model:selected="selectedSeparator"
      :options="delimiterOptions"
      :required="required"
      :rules="rules"
      :disabled="disabled"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'

const props = withDefaults(defineProps<{
  separator?: string
  label?: string
  helpMessage?: string
  required?: boolean
  disabled?: boolean
  rules?: string | Function
}>(), {
  label: 'component.common.csv-delimiters.label',
  helpMessage: 'component.common.csv-delimiters.placeholder',
  required: true,
  disabled: false
})

const emit = defineEmits<{
  (e: 'update:separator', value: string | undefined): void
}>()

const { t, locale } = useI18n()

const selectedSeparator = computed({
  get: () => props.separator,
  set: (value) => emit('update:separator', value)
})

const delimiterOptions = computed(() => [
  {
    id: ',',
    label: t('component.common.csv-delimiters.comma')
  },
  {
    id: ';',
    label: t('component.common.csv-delimiters.semicolon')
  }
])

function getDefaultSeparatorFromLocale(lang: string) {
  return lang === 'fr' ? ';' : ','
}

watch(
  () => locale.value,
  (lang) => {
    selectedSeparator.value = getDefaultSeparatorFromLocale(lang)
  },
  { immediate: true }
)
</script>

<style scoped>
</style>