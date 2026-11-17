<template>
  <div class="graph-import-dates">
    <div class="small fw-semibold mb-1">{{ $t('GraphImport.dates.title') }}</div>
    <div class="graph-import-dates__fields">
      <opensilex-FormSelector
        label="GraphImport.dates.order"
        :selected="dateColumn.date_order ?? DETECTED"
        :options="orderOptions"
        :multiple="false"
        @update:selected="onOrderSelectorUpdate"
      />
      <opensilex-FormSelector
        v-if="isDateTime"
        label="GraphImport.dates.zone"
        :selected="dateColumn.zone ?? NO_ZONE"
        :options="zoneOptions"
        :multiple="false"
        @update:selected="onZoneSelectorUpdate"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, inject } from 'vue'
import type { DateColumnDTO } from '../lib'
import type { OpenSilexPlugin } from '../opensilexPlugin'
import type { DateReading } from '../problems'

/** An option of the shared selector. */
interface SelectorOption {
  id: string
  label: string
}

/** The option that lets the values decide the order. */
const DETECTED = 'DETECTED'
/** The option that refuses times written without a zone. */
const NO_ZONE = 'NONE'
const XSD_DATE_TIME = 'http://www.w3.org/2001/XMLSchema#dateTime'

//#region Public
const emit = defineEmits<{
  read: [reading: DateReading]
}>()

const props = defineProps<{
  dateColumn: DateColumnDTO
}>()
//#endregion

//#region Private
//#region Plugins & services
const $opensilex = inject<OpenSilexPlugin>('$opensilex')!
//#endregion

//#region Data & Computed
const isDateTime = computed(() => props.dateColumn.datatype === XSD_DATE_TIME)

const orderOptions = computed<SelectorOption[]>(() => [
  {
    id: DETECTED,
    label: $opensilex.$i18n.t('GraphImport.dates.detected', {
      order: $opensilex.$i18n.t('GraphImport.dates.' + (props.dateColumn.detected_order ?? 'UNDECIDED'))
    })
  },
  { id: 'DAY_FIRST', label: $opensilex.$i18n.t('GraphImport.dates.DAY_FIRST') },
  { id: 'MONTH_FIRST', label: $opensilex.$i18n.t('GraphImport.dates.MONTH_FIRST') }
])

/** Every whole and half-hour offset in use, from -12:00 to +14:00. */
const zoneOptions = computed<SelectorOption[]>(() => {
  const options: SelectorOption[] = [{ id: NO_ZONE, label: $opensilex.$i18n.t('GraphImport.dates.noZone') }]
  for (let minutes = -12 * 60; minutes <= 14 * 60; minutes += 30) {
    const sign = minutes < 0 ? '-' : '+'
    const hours = String(Math.floor(Math.abs(minutes) / 60)).padStart(2, '0')
    const rest = String(Math.abs(minutes) % 60).padStart(2, '0')
    const offset = `${sign}${hours}:${rest}`
    options.push({ id: offset, label: 'UTC' + offset })
  }
  return options
})
//#endregion

//#region Event handlers
function onOrderSelectorUpdate(selected: string | string[] | undefined) {
  const order = Array.isArray(selected) ? selected[0] : selected
  if (order === undefined || order === (props.dateColumn.date_order ?? DETECTED)) {
    return
  }
  emit('read', {
    date_order: order === DETECTED ? undefined : (order as DateReading['date_order']),
    zone: props.dateColumn.zone
  })
}

function onZoneSelectorUpdate(selected: string | string[] | undefined) {
  const zone = Array.isArray(selected) ? selected[0] : selected
  if (zone === undefined || zone === (props.dateColumn.zone ?? NO_ZONE)) {
    return
  }
  emit('read', {
    date_order: props.dateColumn.date_order as DateReading['date_order'],
    zone: zone === NO_ZONE ? undefined : zone
  })
}
//#endregion
//#endregion
</script>

<style scoped lang="scss">
.graph-import-dates {
  padding: 0.25rem 0.5rem;
}

.graph-import-dates__fields {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(16rem, 22rem));
  gap: 1rem;
}
</style>
