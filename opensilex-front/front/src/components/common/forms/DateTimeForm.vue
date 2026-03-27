<template>
  <opensilex-FormField :helpMessage="helpMessage" :label="label" :required="isRequired">
    <template #field="field">
      <n-input-group>
        <n-date-picker
          v-model:value="timestampValue"
          type="datetime"
          :disabled="disabled"
          :placeholder="t(placeholder)"
          :is-date-disabled="isDateDisabled"
          :is-time-disabled="isTimeDisabled"
          :clearable="false"
          :input-props="{ id: field.id }"
          format="dd/MM/yyyy HH:mm"
          :update-value-on-close="true"
          :actions="['now']"
        />
        <n-button
          class="clear-btn"
          quaternary
          :disabled="disabled"
          @click="clear"
          aria-label="clear"
        >
          <opensilex-Icon icon="fa#times" />
        </n-button>
      </n-input-group>
    </template>
  </opensilex-FormField>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { NDatePicker, NInputGroup, NButton } from 'naive-ui'

const { t } = useI18n()

const props = withDefaults(
  defineProps<{
    label?: string
    helpMessage?: string
    placeholder?: string
    required?: boolean
    disabled?: boolean
    value?: string | undefined
    maxDate?: Date | string | undefined
    minDate?: Date | string | undefined
    timezone?: string
  }>(),
  {
    placeholder: 'DateTimeForm.placeholder',
    required: false,
    disabled: false,
    timezone: ''
  }
)

const emit = defineEmits<{
  (e: 'update:value', v: string | undefined): void
  (e: 'update:required', v: boolean): void
  (e: 'clear'): void
  (e: 'input', v: string | undefined): void
}>()

// local timestamp for Naive UI (number in ms)
const timestampValue = ref<number | null>(null)

const isRequired = computed(() => !!props.required)

function toDate(v: Date | string | undefined): Date | undefined {
  if (!v) return undefined
  return v instanceof Date ? v : new Date(v)
}

function toIsoStringFromTs(v: number | null): string | undefined {
  if (v == null) return undefined
  // Naive date picker gives local timestamp; convert to ISO string
  return new Date(v).toISOString()
}

function parseIsoToTs(v: string | undefined): number | null {
  if (!v) return null
  const d = new Date(v)
  return isNaN(d.getTime()) ? null : d.getTime()
}

// keep timestampValue in sync with incoming v-model value
watch(
  () => props.value,
  (v) => {
    timestampValue.value = parseIsoToTs(v)
  },
  { immediate: true }
)

// emit ISO string when user changes picker
watch(timestampValue, (v) => {
  const iso = toIsoStringFromTs(v)
  emit('update:value', iso)
  emit('input', iso) // compatibility
})

function clear() {
  timestampValue.value = null
  emit('update:value', undefined)
  emit('input', undefined)
  emit('clear')
}

/**
 * Disable dates outside min/max.
 * Naive UI expects: (timestamp: number) => boolean
 */
function isDateDisabled(timestamp: number) {
  const d = new Date(timestamp)
  const min = toDate(props.minDate)
  const max = toDate(props.maxDate)

  if (min && d.getTime() < startOfDay(min).getTime()) return true
  if (max && d.getTime() > endOfDay(max).getTime()) return true
  return false
}

function startOfDay(d: Date) {
  const x = new Date(d)
  x.setHours(0, 0, 0, 0)
  return x
}
function endOfDay(d: Date) {
  const x = new Date(d)
  x.setHours(23, 59, 59, 999)
  return x
}

/**
 * CheckIfHourIsValid :
 * - si minDate et maxDate sont le même jour, alors on interdit les heures < (minHour + 2)
 * - sinon on autorise tout
 *
 * Naive UI expects:
 * isTimeDisabled: (time: number, type: 'hour' | 'minute' | 'second', currentValue?: number) => boolean
 */
function isTimeDisabled(timeValue: number, type: 'hour' | 'minute' | 'second') {
  const min = toDate(props.minDate)
  const max = toDate(props.maxDate)
  if (!min || !max) return false

  //  UTC day
  const minDay = min.getUTCDate()
  const maxDay = max.getUTCDate()

  if (minDay !== maxDay) return false

  if (type === 'hour') {
    const minHour = min.getUTCHours()
    const minAllowed = minHour + 2
    return timeValue < minAllowed || timeValue > 23
  }

  return false
}
</script>

<style scoped lang="scss">
::v-deep(.n-input-group) {
  flex-wrap: nowrap;
}
</style>

<i18n>
en:
  DateTimeForm:
    placeholder: MM/DD/YYYY hh:mm
fr:
  DateTimeForm:
    placeholder: JJ/MM/AAAA hh:mm
</i18n>
