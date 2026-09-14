<template>
  <div class="monitoring-period">
    <div class="monitoring-period-group">
      <span class="monitoring-period-label">{{ $t('Monitoring.period.label') }}</span>
      <div class="btn-group btn-group-sm" role="group">
        <button
          v-for="option in presets"
          :key="option"
          type="button"
          :class="['btn', option === preset ? 'btn-primary' : 'btn-outline-secondary']"
          @click="selectPreset(option)"
        >
          {{ $t('Monitoring.period.presets.' + option) }}
        </button>
      </div>
    </div>

    <div class="monitoring-period-group" v-if="preset === 'custom'">
      <input
        type="date"
        class="form-control form-control-sm"
        :value="startDay"
        :max="endDay"
        @change="onStart($event)"
      />
      <span class="monitoring-period-label">→</span>
      <input
        type="date"
        class="form-control form-control-sm"
        :value="endDay"
        :min="startDay"
        @change="onEnd($event)"
      />
    </div>

    <div class="monitoring-period-group">
      <span class="monitoring-period-label">{{ $t('Monitoring.period.granularity') }}</span>
      <div class="btn-group btn-group-sm" role="group">
        <!--
          An illegal granularity is disabled rather than allowed and then rejected: the request for
          four thousand buckets is never issued in the first place, and the tooltip says why the
          button is greyed out instead of leaving the user to guess.
        -->
        <button
          v-for="option in granularities"
          :key="option"
          type="button"
          :disabled="!allowed(option)"
          :title="allowed(option) ? '' : tooManyBuckets(option)"
          :class="['btn', option === granularity ? 'btn-primary' : 'btn-outline-secondary']"
          @click="emit('update:granularity', option)"
        >
          {{ $t('Monitoring.period.granularities.' + option.toLowerCase()) }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, inject } from 'vue';
import {
  GRANULARITIES,
  MAX_BUCKETS,
  PERIOD_PRESETS,
  bucketCount,
  defaultGranularity,
  isAllowed,
  presetRange,
  type Granularity,
  type PeriodPreset,
} from '../models/Granularity';

const props = defineProps<{
  preset: PeriodPreset;
  granularity: Granularity;
  start: Date;
  end: Date;
}>();

const emit = defineEmits<{
  (event: 'update:preset', value: PeriodPreset): void;
  (event: 'update:granularity', value: Granularity): void;
  (event: 'update:start', value: Date): void;
  (event: 'update:end', value: Date): void;
}>();

const $opensilex: any = inject('$opensilex');

const presets = PERIOD_PRESETS;
const granularities = GRANULARITIES;

const startDay = computed(() => toDay(props.start));
const endDay = computed(() => toDay(props.end));

function toDay(date: Date): string {
  return date.toISOString().slice(0, 10);
}

function allowed(option: Granularity): boolean {
  return isAllowed(option, props.start.getTime(), props.end.getTime());
}

function tooManyBuckets(option: Granularity): string {
  return $opensilex.$i18n.t('Monitoring.period.tooManyBuckets', {
    count: bucketCount(option, props.start.getTime(), props.end.getTime()),
    max: MAX_BUCKETS,
  });
}

function selectPreset(option: PeriodPreset) {
  emit('update:preset', option);
  if (option === 'custom') {
    return;
  }
  const range = presetRange(option as Exclude<PeriodPreset, 'custom'>);
  emit('update:start', range.from);
  emit('update:end', range.to);
  emit('update:granularity', defaultGranularity(option));
}

function onStart(event: Event) {
  const value = (event.target as HTMLInputElement).value;
  if (value) {
    emit('update:start', new Date(`${value}T00:00:00`));
  }
}

function onEnd(event: Event) {
  const value = (event.target as HTMLInputElement).value;
  if (value) {
    emit('update:end', new Date(`${value}T23:59:59`));
  }
}
</script>

<style scoped lang="scss">
// The platform's --color-gray sits near 2.6:1 on white, under the 4.5:1 that small text needs.
// This tone still reads as secondary but is actually legible.
$muted: #5b6875;

.monitoring-period {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 1rem;
  margin-bottom: 0.75rem;
}

.monitoring-period-group {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.monitoring-period-label {
  font-size: 0.85rem;
  color: #{$muted};
}
</style>
