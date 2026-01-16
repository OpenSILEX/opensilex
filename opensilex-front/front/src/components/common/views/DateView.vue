<template>
  <div v-if="label" class="static-field">
    <span class="field-view-title">{{ t(label) }}</span>
    <span :title="formattedISODate ?? ''">
      {{ formattedDate ?? '' }}
    </span>
  </div>

  <span v-else :title="formattedISODate ?? ''">
    {{ formattedDate ?? '' }}
  </span>
</template>

<script setup lang="ts">
import { computed, inject } from 'vue'
import { useI18n } from 'vue-i18n'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'

const props = withDefaults(defineProps<{
  value?: string
  label?: string
  isDatetime?: boolean
  dateTimeFormatOptions?: Intl.DateTimeFormatOptions
  useLocaleFormat?: boolean
}>(), {
  isDatetime: false,
  useLocaleFormat: false
})

const { t } = useI18n()
const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')

const formattedISODate = computed<string | undefined>(() => {
  if (!props.value) return undefined
  if (!$opensilex) return props.value // fallback

  if (props.isDatetime) {
    return $opensilex.$dateTimeFormatter.formatISODateTime(props.value)
  }
  return $opensilex.$dateTimeFormatter.formatISODate(props.value)
})

const formattedLocaleDate = computed<string | undefined>(() => {
  if (!props.value) return undefined
  if (!$opensilex) return props.value

  if (props.isDatetime) {
    return $opensilex.$dateTimeFormatter.formatLocaleDateTime(
      props.value,
      props.dateTimeFormatOptions
    )
  }
  return $opensilex.$dateTimeFormatter.formatLocaleDate(
    props.value,
    props.dateTimeFormatOptions
  )
})

const formattedDate = computed(() => {
  return props.useLocaleFormat ? formattedLocaleDate.value : formattedISODate.value
})
</script>

<style scoped lang="scss">
</style>
