<template>
  <opensilex-FormField
    :helpMessage="helpMessage"
    :label="label"
    :required="isRequired"
  >
    <template #field="{ id }">
      <n-date-picker
        :input-props="{ id }"
        v-model:formatted-value="dateProxy"
        value-format="yyyy-MM-dd"
        format="dd-MM-yyyy"
        type="date"
        clearable
        :disabled="disabled"
        :is-date-disabled="isOutOfRange"
        :first-day-of-week="firstDayOfWeek"
        :placeholder="t(placeholder)"
      />
    </template>
  </opensilex-FormField>
</template>

<script setup lang="ts">
import { computed, inject } from 'vue'
import { useI18n } from 'vue-i18n'
import { NDatePicker } from 'naive-ui'

type Rules = string | ((val: any) => true | string)

const props = withDefaults(defineProps<{
  label?: string
  helpMessage?: string
  placeholder?: string
  required?: boolean
  disabled?: boolean
  /** bornes optionnelles (Date ou string ISO) */
  maxDate?: Date | string
  minDate?: Date | string
  /** v-model */
  value?: string | null
}>(), {
  placeholder: 'DateForm.placeholder',
  required: false,
  disabled: false,
  value: null
})

const emit = defineEmits<{
  (e: 'update:value', v: string | null): void
}>()

const { t } = useI18n()
const isRequired = computed(() => !!props.required)

// On expose un v-model “formatted-value” (string 'yyyy-mm-dd' pour l'API)
const dateProxy = computed<string | null>({
  get: () => props.value ?? null,
  set: (v) => emit('update:value', v ?? null)
})

// min/max en timestamp pour le disable
function toTimestamp(date?: Date | string): number | null {
  if (!date) return null
  if (typeof date === 'string') {
    const timestamp = Date.parse(date)
    return Number.isNaN(timestamp) ? null : timestamp
  }
  return date.getTime()
}
const minTs = computed(() => toTimestamp(props.minDate))
const maxTs = computed(() => toTimestamp(props.maxDate))

// Naive UI -> is-date-disabled(dateTimestamp: number) => boolean
function isOutOfRange(ts: number) {
  if (minTs.value != null && ts < stripTime(minTs.value)) return true
  if (maxTs.value != null && ts > stripTime(maxTs.value)) return true
  return false
}
function stripTime(ts: number) {
  const date = new Date(ts)
  date.setHours(0,0,0,0)
  return date.getTime()
}

// First day of week
const user = (inject('$store') as any)?.state?.user
const firstDayOfWeek = computed(() => (user?.locale?.startsWith?.('fr') ? 1 : 0))
</script>

<style scoped>
</style>

<i18n>
en:
  DateForm:
    placeholder: MM/DD/YYYY
fr:
  DateForm:
    placeholder: JJ/MM/AAAA
</i18n>
