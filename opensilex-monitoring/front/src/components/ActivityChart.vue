<template>
  <opensilex-Card :no-footer="true" label="Monitoring.chart.title" icon="bi-bi-graph-up">
    <template #rightHeader>
      <button
        type="button"
        class="btn btn-link btn-sm"
        :disabled="!buckets.length"
        @click="onExportClick"
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
import { useECharts, type EChartsInstance } from '../composables/useECharts';
import { buildActivityOption, type ActivityPoint } from '../models/activityChartOption';
import type { Granularity } from '../models/Granularity';
import type { OpenSilexPlugin } from '../types/OpenSilexPlugin';

/** The bucket-index window the user has brushed the slider down to. */
export interface ZoomRange {
  startIndex: number;
  endIndex: number;
}

//#region Public
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
  zoom: [value: ZoomRange];
}>();
//#endregion

//#region Private

//#region Plugins and services
const $opensilex = inject<OpenSilexPlugin>('$opensilex')!;
//#endregion

//#region Template refs
const container = ref<HTMLElement | null>(null);
const { chart, setOption, toDataURL } = useECharts(container);
//#endregion

//#region Data and computed
let zoomTimer: ReturnType<typeof setTimeout> | null = null;

/** The live zoom range ECharts computed internally, read back off the instance. Not part of the
 *  option this component sets, so it is narrowed locally rather than typed against the library's
 *  full (and much wider) runtime option shape. */
interface RuntimeDataZoom {
  startValue?: number;
  endValue?: number;
}
//#endregion

//#region Hooks and watcher
watch(chart, (instance) => {
  if (!instance) {
    return;
  }
  render();
  // Brushing the slider narrows the window the KPI reports on, with no round trip: the buckets are
  // already client-side, so the card and the chart cannot disagree.
  instance.on('dataZoom', () => onDataZoom(instance));
});

watch(() => [props.buckets, props.granularity], render, { deep: true });
//#endregion

//#region Event handlers
function onDataZoom(instance: EChartsInstance): void {
  if (zoomTimer) {
    clearTimeout(zoomTimer);
  }
  zoomTimer = setTimeout(() => emit('zoom', readZoomRange(instance, props.buckets.length)), 120);
}

function onExportClick(): void {
  exportPng();
}
//#endregion

//#region Methods
function render(): void {
  if (!chart.value) {
    return;
  }
  setOption(buildActivityOption(
    props.buckets,
    props.granularity,
    $opensilex.getLang(),
    {
      users: $opensilex.$i18n.t('Monitoring.chart.series.users'),
      requests: $opensilex.$i18n.t('Monitoring.chart.series.requests'),
      errors: $opensilex.$i18n.t('Monitoring.chart.series.errors'),
      average: $opensilex.$i18n.t('Monitoring.chart.averageErrors'),
      errorRate: $opensilex.$i18n.t('Monitoring.chart.errorRate'),
    },
  ));
}

function readZoomRange(instance: EChartsInstance, bucketCount: number): ZoomRange {
  const option = instance.getOption() as { dataZoom?: RuntimeDataZoom[] };
  const zoom = option.dataZoom?.[0];
  const last = Math.max(0, bucketCount - 1);
  const startIndex = typeof zoom?.startValue === 'number' ? Math.floor(zoom.startValue) : 0;
  const endIndex = typeof zoom?.endValue === 'number' ? Math.ceil(zoom.endValue) : last;
  return {
    startIndex: Math.max(0, Math.min(startIndex, last)),
    endIndex: Math.max(0, Math.min(endIndex, last)),
  };
}

function exportPng(): void {
  const url = toDataURL();
  if (!url) {
    return;
  }
  const link = document.createElement('a');
  link.href = url;
  link.download = 'opensilex-activity.png';
  link.click();
}
//#endregion

//#endregion
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
