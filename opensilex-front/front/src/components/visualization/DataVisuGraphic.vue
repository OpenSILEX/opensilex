<template>
  <div class="dataVisuGraphic">
    <div v-show="series.length > 0" ref="chartContainer" class="chartContainer"></div>
    <n-empty v-if="series.length === 0" :description="t('component.common.search.noDataFound')" class="emptyGraphic"/>
  </div>
</template>

<script lang="ts">
/**
 * One series drawn on the graphic, e.g. the data of one (ScientificObject, Variable) pair.
 */
export interface DataVisuSerie {
  name: string
  /** [timestamp in ms, value] couples, sorted by date */
  data: Array<[number, number]>
  color?: string
  /** ECharts symbol name (circle, rect, triangle, diamond...) */
  symbol?: string
  /** Index in the yAxisNames prop of the axis this series is drawn on */
  yAxisIndex?: number
}
</script>

<script setup lang="ts">
import {onBeforeUnmount, onMounted, useTemplateRef, watch} from "vue";
import {useI18n} from "vue-i18n";
import {NEmpty} from "naive-ui";
import * as echarts from "echarts/core";
import {ScatterChart} from "echarts/charts";
import type {ScatterSeriesOption} from "echarts/charts";
import {GridComponent, LegendComponent, TooltipComponent} from "echarts/components";
import type {GridComponentOption, LegendComponentOption, TooltipComponentOption} from "echarts/components";
import type {TooltipComponentFormatterCallbackParams} from "echarts";
import {CanvasRenderer} from "echarts/renderers";

echarts.use([ScatterChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer]);

/**
 * Option type restricted to the charts and components registered above
 */
type DataVisuOption = echarts.ComposeOption<
    ScatterSeriesOption | GridComponentOption | LegendComponentOption | TooltipComponentOption
>;

//#region Public
const props = withDefaults(defineProps<{
  series: DataVisuSerie[]
  /** One Y axis per entry, the first one on the left and the second one on the right */
  yAxisNames?: string[]
}>(), {
  yAxisNames: () => []
});
//#endregion

//#region Private
//#region Plugins & services
const {t, locale} = useI18n();
//#endregion

//#region Template refs
const chartContainer = useTemplateRef<HTMLDivElement>("chartContainer");
//#endregion

//#region Data & Computed
let chart: echarts.ECharts | undefined;
let resizeObserver: ResizeObserver | undefined;
//#endregion

//#region Hooks and watcher
onMounted(() => {
  chart = echarts.init(chartContainer.value);
  render();
  // The container width changes when the filters sidebar is toggled, not only when the window is resized
  resizeObserver = new ResizeObserver(() => chart?.resize());
  resizeObserver.observe(chartContainer.value);
});

onBeforeUnmount(() => {
  resizeObserver?.disconnect();
  chart?.dispose();
  chart = undefined;
});

watch(() => [props.series, props.yAxisNames], render);
//#endregion

//#region Methods
function formatTooltip(params: TooltipComponentFormatterCallbackParams): string {
  // The tooltip is triggered on a single item, so there is only one param
  const item = Array.isArray(params) ? params[0] : params;
  const [timestamp, value] = item.value as [number, number];
  return `${item.marker}${item.seriesName}<br/>`
      + `${new Date(timestamp).toLocaleString(locale.value)}<br/>`
      + `<b>${value}</b>`;
}

function buildOption(): DataVisuOption {
  const yAxisNames: string[] = props.yAxisNames.length > 0 ? props.yAxisNames : [""];

  return {
    tooltip: {
      trigger: "item",
      formatter: formatTooltip
    },
    legend: {
      type: "scroll",
      bottom: 0
    },
    grid: {
      left: 20,
      right: 20,
      top: 40,
      bottom: 40,
      containLabel: true
    },
    xAxis: {
      type: "time"
    },
    yAxis: yAxisNames.map((name: string, index: number) => ({
      type: "value",
      name,
      position: index === 0 ? "left" : "right",
      scale: true,
      // only the first axis draws the background grid lines, otherwise they overlap
      splitLine: {show: index === 0}
    })),
    series: props.series.map((serie: DataVisuSerie): ScatterSeriesOption => ({
      type: "scatter",
      name: serie.name,
      data: serie.data,
      color: serie.color,
      symbol: serie.symbol ?? "circle",
      symbolSize: 6,
      yAxisIndex: serie.yAxisIndex ?? 0,
      emphasis: {focus: "series"},
      large: true
    }))
  };
}

function render(): void {
  if (!chart) {
    return;
  }
  // notMerge so series removed from the props are removed from the graphic
  chart.setOption(buildOption(), {notMerge: true});
}
//#endregion
//#endregion
</script>

<style scoped lang="scss">
.chartContainer {
  width: 100%;
  height: 500px;
}

.emptyGraphic {
  padding: 40px 0;
}
</style>
