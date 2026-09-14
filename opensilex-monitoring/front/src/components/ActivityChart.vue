<template>
  <opensilex-Card :no-footer="true" label="Monitoring.chart.title" icon="bi-bi-graph-up">
    <template #rightHeader>
      <button
        type="button"
        class="btn btn-link btn-sm"
        :disabled="!buckets.length"
        @click="exportPng"
      >
        <opensilex-Icon icon="bi#bi-download" /> {{ $t('Monitoring.chart.exportPng') }}
      </button>
    </template>

    <template #body>
      <p class="monitoring-chart-empty" v-if="!loading && !buckets.length">
        {{ $t('Monitoring.chart.noData') }}
      </p>
      <div ref="container" class="monitoring-chart" v-show="buckets.length"></div>
    </template>
  </opensilex-Card>
</template>

<script setup lang="ts">
import { inject, ref, watch } from 'vue';
import { useECharts } from '../composables/useECharts';
import { buildActivityOption, type ActivityPoint } from '../models/activityChartOption';
import type { Granularity } from '../models/Granularity';

const props = withDefaults(defineProps<{
  buckets?: ActivityPoint[];
  granularity?: Granularity;
  loading?: boolean;
}>(), {
  buckets: () => [],
  granularity: 'DAY',
  loading: false,
});

const emit = defineEmits<{
  (event: 'zoom', value: { startIndex: number; endIndex: number }): void;
}>();

const $opensilex: any = inject('$opensilex');

const container = ref<HTMLElement | null>(null);
const { chart, setOption, toDataURL } = useECharts(container);

let zoomTimer: ReturnType<typeof setTimeout> | null = null;

function render() {
  if (!chart.value) {
    return;
  }
  setOption(buildActivityOption(
    props.buckets,
    props.granularity,
    $opensilex?.getLang?.() || 'en',
    {
      users: $opensilex.$i18n.t('Monitoring.chart.series.users'),
      requests: $opensilex.$i18n.t('Monitoring.chart.series.requests'),
      errors: $opensilex.$i18n.t('Monitoring.chart.series.errors'),
      average: $opensilex.$i18n.t('Monitoring.chart.averageErrors'),
      errorRate: $opensilex.$i18n.t('Monitoring.chart.errorRate'),
    },
  ));
}

watch(chart, (instance) => {
  if (!instance) {
    return;
  }
  render();
  // Brushing the slider narrows the window the KPI reports on, with no round trip: the buckets are
  // already client-side, so the card and the chart cannot disagree.
  instance.on('dataZoom', () => {
    if (zoomTimer) {
      clearTimeout(zoomTimer);
    }
    zoomTimer = setTimeout(() => {
      const option: any = instance.getOption();
      const zoom = option?.dataZoom?.[0];
      if (!zoom) {
        return;
      }
      const last = Math.max(0, props.buckets.length - 1);
      const startIndex = typeof zoom.startValue === 'number' ? Math.floor(zoom.startValue) : 0;
      const endIndex = typeof zoom.endValue === 'number' ? Math.ceil(zoom.endValue) : last;
      emit('zoom', {
        startIndex: Math.max(0, Math.min(startIndex, last)),
        endIndex: Math.max(0, Math.min(endIndex, last)),
      });
    }, 120);
  });
});

watch(() => [props.buckets, props.granularity], render, { deep: true });

function exportPng() {
    const url = toDataURL();
    if (!url) {
        return;
    }
    const link = document.createElement('a');
    link.href = url;
    link.download = 'opensilex-activity.png';
    link.click();
}
</script>

<style scoped lang="scss">
// The platform's --color-gray sits near 2.6:1 on white, under the 4.5:1 that small text needs.
// This tone still reads as secondary but is actually legible.
$muted: #5b6875;

// The export action belongs to the card header; it must wrap rather than spill past the card edge.
:deep(.card-header) {
  flex-wrap: wrap;
  gap: 0.5rem;
}

:deep(.card-header-right) {
  flex-shrink: 0;
}

.monitoring-chart {
  width: 100%;
  min-width: 0;
  height: 340px;
}

// An empty period is a normal state, not an error: keep it quiet and compact instead of
// reserving the full chart height for a single sentence.
.monitoring-chart-empty {
  color: #{$muted};
  text-align: center;
  margin: 2rem 0;
  font-size: 0.9rem;
}
</style>
