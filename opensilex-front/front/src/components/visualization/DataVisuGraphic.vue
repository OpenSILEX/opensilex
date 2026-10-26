<template>
  <div ref="graphicCard" class="dataVisuGraphic">
    <div v-if="series.length > 0" class="graphicHeader">
      <n-button v-if="isZoomed" size="small" @click="resetZoom()">
        {{ t("DataVisuGraphic.resetZoom") }}
      </n-button>
      <n-dropdown
          trigger="click"
          placement="bottom-end"
          :options="menuOptions"
          :to="graphicCard ?? 'body'"
          @select="onMenuSelect"
      >
        <n-button quaternary circle :title="t('DataVisuGraphic.menu')">
          <i class="bi bi-list menuIcon"></i>
        </n-button>
      </n-dropdown>
    </div>
    <div v-show="series.length > 0" ref="chartContainer" class="chartContainer"></div>
    <n-empty v-if="series.length === 0" :description="t('component.common.search.noDataFound')" class="emptyGraphic"/>
  </div>
</template>

<script lang="ts">
import type {DataGetSearchDTO, EventGetDTO} from "opensilex-core/index";

/**
 * One point of a series
 */
export interface DataVisuPoint {
  /** Date of the point, in milliseconds */
  timestamp: number
  value: number
  /** The Data this point was made from, kept for the interactions on the point (e.g. displaying its provenance) */
  data?: DataGetSearchDTO
}

/**
 * Type to represent one series drawn on the graphic, e.g. the data of one (ScientificObject, Variable) pair.
 */
export interface DataVisuSerie {
  name: string
  /** Points sorted by date */
  points: DataVisuPoint[]
  color?: string
  /** ECharts symbol name (circle, rect, triangle, diamond...) */
  symbol?: string
  /** Index in the yAxisNames prop of the axis this series is drawn on */
  yAxisIndex?: number
}

/**
 * One event, drawn as a pin at the bottom of the graphic
 */
export interface DataVisuEvent {
  /** Date where the pin is drawn, in milliseconds */
  timestamp: number
  /** Text written in the pin, e.g. the first letter of the event type */
  label: string
  /**
   * Name of the legend entry grouping this event with others, e.g. "Events -> plot 1". Events with the same
   * seriesName can be hidden together by clicking the legend.
   */
  seriesName: string
  color?: string
  event: EventGetDTO
}
</script>

<script setup lang="ts">
import {computed, ComputedRef, h, onBeforeUnmount, onMounted, ref, Ref, useTemplateRef, VNode, watch} from "vue";
import {useI18n} from "vue-i18n";
import {DropdownOption, NButton, NDropdown, NEmpty} from "naive-ui";
import * as echarts from "echarts/core";
import {LineChart, ScatterChart} from "echarts/charts";
import type {LineSeriesOption, ScatterSeriesOption} from "echarts/charts";
import {
  DataZoomInsideComponent,
  GridComponent,
  LegendComponent,
  TitleComponent,
  ToolboxComponent,
  TooltipComponent
} from "echarts/components";
import type {
  DataZoomComponentOption,
  GridComponentOption,
  LegendComponentOption,
  TitleComponentOption,
  ToolboxComponentOption,
  TooltipComponentOption
} from "echarts/components";
import type {
  DefaultLabelFormatterCallbackParams,
  TooltipComponentFormatterCallbackParams,
  YAXisComponentOption
} from "echarts";
import {CanvasRenderer} from "echarts/renderers";

echarts.use([
  ScatterChart, LineChart,
  GridComponent, LegendComponent, TooltipComponent, TitleComponent, ToolboxComponent, DataZoomInsideComponent,
  CanvasRenderer
]);

/**
 * Option type restricted to the charts and components registered above
 */
type DataVisuOption = echarts.ComposeOption<
    ScatterSeriesOption | LineSeriesOption | GridComponentOption | LegendComponentOption | TooltipComponentOption
    | TitleComponentOption | ToolboxComponentOption | DataZoomComponentOption
>;

/** Keys of the entries of the tasty burger menu */
type MenuKey = "toggleLineMode" | "fullscreen" | "download";

/**
 * Current zoom of the X axis, in percent of the full date range
 */
interface ZoomRange {
  start: number
  end: number
}

/**
 * Draws Opensilex data as a scatter plot (or line chart) over time, using Apache ECharts.
 *
 * This component only displays the series it receives: fetching the data, choosing the colors and symbols is done by
 * the parent (e.g. ExperimentDataVisualisationView), so that every use-case can share this component.
 *
 * Zoom on the X axis by dragging across the chart, or with the mouse wheel. The burger menu toggles the line mode,
 * the fullscreen, and downloads the chart as a PNG image.
 */

//#region Public
const props = withDefaults(defineProps<{
  /** The series to draw, see DataVisuSerie */
  series: DataVisuSerie[]
  /** One Y axis per entry, the first one on the left and the second one on the right */
  yAxisNames?: string[]
  /** Name of the element the data belongs to (e.g. the experiment), written in the title of the downloaded image */
  elementName?: string
  /** URI of the element the data belongs to, written in the title of the downloaded image */
  elementUri?: string
  /** Events drawn as pins at the bottom of the graphic, see DataVisuEvent */
  events?: DataVisuEvent[]
}>(), {
  yAxisNames: () => [],
  events: () => []
});
//#endregion

//#region Private
//#region Plugins & services
const {t, locale} = useI18n();
//#endregion

//#region Template refs
const graphicCard = useTemplateRef<HTMLDivElement>("graphicCard");
const chartContainer = useTemplateRef<HTMLDivElement>("chartContainer");
//#endregion

//#region Data & Computed
/** Size of the downloaded image, before the pixel ratio is applied */
const EXPORT_WIDTH: number = 1600;
const EXPORT_HEIGHT: number = 800;
/** Prefix of the ECharts ids of the events series, to tell them apart from the data series */
const EVENT_SERIES_ID_PREFIX: string = "event:";
/** Size of the event pins, in pixels. Pins closer than this on the X axis would overlap, so they are stacked */
const PIN_SIZE: number = 28;

/**
 * Events grouped by the ECharts id of the series drawing them, in the order of the series data. Used to find the
 * event of a hovered pin. Rebuilt with the series.
 */
let eventsBySeriesId: Map<string, DataVisuEvent[]> = new Map<string, DataVisuEvent[]>();

/**
 * The ECharts instance drawing in chartContainer. Not reactive on purpose: ECharts manages its own state, and
 * wrapping it in a Vue ref would only add overhead.
 */
let chart: echarts.ECharts | undefined;
/** Resizes the chart whenever chartContainer changes size */
let resizeObserver: ResizeObserver | undefined;

/** true to connect the points of each series with a line, false to only draw the points */
const lineMode: Ref<boolean> = ref<boolean>(false);
/** true when the X axis is zoomed, to display the "Reset zoom" button */
const isZoomed: Ref<boolean> = ref<boolean>(false);

const menuOptions: ComputedRef<DropdownOption[]> = computed<DropdownOption[]>(() => [
  lineMode.value
      ? {key: "toggleLineMode", label: t("DataVisuGraphic.scatterPlotView"), icon: renderIcon("bi-grip-horizontal")}
      : {key: "toggleLineMode", label: t("DataVisuGraphic.chartLineView"), icon: renderIcon("bi-graph-up")},
  {key: "fullscreen", label: t("DataVisuGraphic.fullscreen"), icon: renderIcon("bi-arrows-fullscreen")},
  {key: "download", label: t("DataVisuGraphic.download"), icon: renderIcon("bi-download")},
]);
//#endregion

//#region Hooks and watcher
onMounted(() => {
  chart = echarts.init(chartContainer.value);
  chart.on("datazoom", onChartDataZoom);
  // Hidden events series don't take space anymore, the pins of the others can move down
  chart.on("legendselectchanged", () => layoutEventPins(chart));
  render();
  // The container size changes when the filters sidebar is toggled or in fullscreen, not only when the window is resized
  resizeObserver = new ResizeObserver(() => {
    chart?.resize();
    layoutEventPins(chart);
  });
  resizeObserver.observe(chartContainer.value);
});

onBeforeUnmount(() => {
  resizeObserver?.disconnect();
  chart?.dispose();
  chart = undefined;
});

// Redraw the whole chart when the parent passes new series, axes or events
watch(() => [props.series, props.yAxisNames, props.events], render);

// Only the series change, so the zoom and the hidden series are kept
watch(lineMode, () => {
  chart?.setOption({series: [...buildSeries(), ...buildEventSeries()]});
  layoutEventPins(chart);
});
//#endregion

//#region Event handlers
function onMenuSelect(key: MenuKey): void {
  switch (key) {
    case "toggleLineMode":
      lineMode.value = !lineMode.value;
      break;
    case "fullscreen":
      toggleFullscreen();
      break;
    case "download":
      downloadImage();
      break;
  }
}

/**
 * Called by ECharts after each zoom, by dragging, with the mouse wheel or with resetZoom()
 */
function onChartDataZoom(): void {
  const zoomRange: ZoomRange = getZoomRange();
  isZoomed.value = zoomRange.start > 0 || zoomRange.end < 100;
  // Pins that overlapped may be far enough apart after zooming, and the other way round
  layoutEventPins(chart);
}
//#endregion

//#region Methods
/**
 * @returns a render function of a bootstrap icon, for the icon of a naive-ui dropdown entry
 */
function renderIcon(iconClass: string): () => VNode {
  return () => h("i", {class: `bi ${iconClass}`});
}

/**
 * Builds the HTML content of the tooltip, the little popup displayed when hovering a point or an event.
 * For a point, it displays the series' colored marker and name, the date of the point and its value.
 */
function formatTooltip(params: TooltipComponentFormatterCallbackParams): string {
  // The tooltip is triggered on a single item, so there is only one param
  const item = Array.isArray(params) ? params[0] : params;
  // The colored dot of the series, it is an HTML string with the default tooltip renderMode
  const marker: string = typeof item.marker === "string" ? item.marker : "";
  const event: DataVisuEvent | undefined = eventsBySeriesId.get(item.seriesId)?.[item.dataIndex];
  if (event) {
    return formatEventTooltip(event, marker);
  }
  const [timestamp, value] = item.value as [number, number];
  return `${marker}${item.seriesName}<br/>`
      + `${new Date(timestamp).toLocaleString(locale.value)}<br/>`
      + `<b>${value}</b>`;
}

/**
 * Event tooltip: its type, and its begin and end dates (only one date for an instant event).
 */
function formatEventTooltip(event: DataVisuEvent, marker: string): string {
  const dto: EventGetDTO = event.event;
  const title: string = `${marker}<b>${dto.rdf_type_name ?? dto.rdf_type ?? ""}</b><br/>`;
  if (!dto.start) {
    return title + formatDate(dto.end);
  }
  return title
      + `${t("DataVisuGraphic.eventBegin")} : ${formatDate(dto.start)}<br/>`
      + `${t("DataVisuGraphic.eventEnd")} : ${dto.end ? formatDate(dto.end) : t("DataVisuGraphic.eventOngoing")}`;
}

function formatDate(date: string): string {
  return new Date(date).toLocaleString(locale.value);
}

/**
 * Converts the props into an ECharts option object, which describes everything ECharts must draw.
 * See https://echarts.apache.org/en/option.html for the documentation of each option.
 *
 * @param zoomRange initial zoom of the X axis, the full date range if undefined
 */
function buildOption(zoomRange?: ZoomRange): DataVisuOption {
  const yAxisNames: string[] = props.yAxisNames.length > 0 ? props.yAxisNames : [""];
  const option: DataVisuOption = buildBaseOption(yAxisNames, zoomRange);
  if (props.events.length > 0) {
    (option.yAxis as YAXisComponentOption[]).push({
      /**
       * Hidden axis of the event pins, from 0 to 1. The pins are at 0, so at the bottom of the chart, whatever the
       * values of the data
       */
      type: "value",
      show: false,
      min: 0,
      max: 1
    });
  }
  return option;
}

/**
 * The option without the events axis, see buildOption()
 */
function buildBaseOption(yAxisNames: string[], zoomRange?: ZoomRange): DataVisuOption {
  return {
    /** Popup displayed when hovering a point */
    tooltip: {
      /** Show the tooltip for the hovered point only ("axis" would show every series at the hovered date) */
      trigger: "item",
      formatter: formatTooltip
    },
    /** List of the series names under the chart. Clicking a name hides / shows its series */
    legend: {
      /** Legend items that don't fit on one line are paginated with arrows, instead of overflowing */
      type: "scroll",
      bottom: 0
    },
    /** Position of the drawing area (axes and points) inside the container, in pixels */
    grid: {
      left: 20,
      right: 20,
      top: 40,
      bottom: 40,
      /** The margins above are the outer limit of the chart, labels included ("none" would let labels overflow them) */
      outerBoundsMode: "same",
      /** Keep both the axis labels (tick values) and the axis names (titles) inside these limits */
      outerBoundsContain: "all"
    },
    /**
     * The toolbox is ECharts' own button bar. Its icons are hidden (size 0) as we use our burger menu instead,
     * but it must be rendered: its "dataZoom" feature is what allows zooming by dragging across the chart.
     * The drag zoom is switched on in enableDragZoom().
     */
    toolbox: {
      itemSize: 0,
      showTitle: false,
      feature: {
        /** yAxisIndex "none": the drag only selects a date range, the Y axes adapt to the visible points */
        dataZoom: {yAxisIndex: "none"}
      }
    },
    /** Zoom on the X axis with the mouse wheel */
    dataZoom: [{
      /** "inside" = zoomed by interacting directly with the chart, without a slider */
      type: "inside",
      xAxisIndex: 0,
      zoomOnMouseWheel: true,
      /** Dragging is used to select a zoom range, not to move the zoomed range */
      moveOnMouseMove: false,
      moveOnMouseWheel: false,
      start: zoomRange?.start ?? 0,
      end: zoomRange?.end ?? 100
    }],
    xAxis: {
      /** Continuous date axis, ECharts picks readable date labels depending on the zoom level */
      type: "time"
    },
    yAxis: yAxisNames.map((name: string, index: number) => ({
      /** Continuous numeric axis */
      type: "value",
      /** Axis title, e.g. "Height (cm)" */
      name,
      position: index === 0 ? "left" : "right",
      /** Fit the axis range to the data, instead of always starting at 0 */
      scale: true,
      /** Horizontal background lines: only drawn by the first axis, otherwise the two axes' lines overlap */
      splitLine: {show: index === 0}
    })),
    series: [...buildSeries(), ...buildEventSeries()]
  };
}

/**
 * Converts the series prop into ECharts series, drawn as points or lines depending on lineMode.
 */
function buildSeries(): Array<ScatterSeriesOption | LineSeriesOption> {
  return props.series.map((serie: DataVisuSerie, index: number): ScatterSeriesOption | LineSeriesOption => {
    /** Options shared by the scatter and the line series */
    const common = {
      /** Identifies the series when only the series are updated (line mode), so ECharts updates the right one */
      id: `data:${index}`,
      /** Displayed in the legend and the tooltip */
      name: serie.name,
      /**
       * [timestamp, value] couples, read as [x, y]. ECharts only gets plain numbers (faster for big series), the
       * index of a point in this array is the same as in serie.points, to find its original data
       */
      data: serie.points.map((point: DataVisuPoint): [number, number] => [point.timestamp, point.value]),
      /** Color of the points (and lines), and of the series in the legend */
      color: serie.color,
      /** Shape of the points */
      symbol: serie.symbol ?? "circle",
      /** Diameter of the points, in pixels */
      symbolSize: 6,
      /** Index in the yAxis array of the axis this series is drawn on */
      yAxisIndex: serie.yAxisIndex ?? 0,
      /**
       * Emphasis = the "highlighted" state of a point, when it is hovered.
       * focus "series" fades out every other series, so the hovered point's series stands out.
       */
      emphasis: {focus: "series" as const},
    };

    if (lineMode.value) {
      return {
        ...common,
        /** Points connected with a line */
        type: "line",
        /** Also draw the points on the line, not only when hovered */
        showSymbol: true
      };
    }
    return {
      ...common,
      /** Independent points */
      type: "scatter",
      /**
       * Optimised rendering for series with a lot of points (up to 50 000 here). Only kicks in above
       * "largeThreshold" points (2000 by default), smaller series are drawn normally.
       */
      large: true
    };
  });
}

/**
 * Converts the events prop into ECharts series: one series of pins per seriesName, so each can be hidden from the
 * legend. Always drawn as independent pins, even in line mode.
 *
 * @param pinLevels stacking level of each pin, see layoutEventPins(). Pins without a level are at the bottom (0).
 */
function buildEventSeries(pinLevels: Map<DataVisuEvent, number> = new Map<DataVisuEvent, number>()): ScatterSeriesOption[] {
  const eventsAxisIndex: number = Math.max(props.yAxisNames.length, 1);

  eventsBySeriesId = new Map<string, DataVisuEvent[]>();
  for (const event of props.events) {
    const seriesId: string = EVENT_SERIES_ID_PREFIX + event.seriesName;
    eventsBySeriesId.set(seriesId, [...(eventsBySeriesId.get(seriesId) ?? []), event]);
  }

  return Array.from(eventsBySeriesId, ([seriesId, events]: [string, DataVisuEvent[]]): ScatterSeriesOption => ({
    type: "scatter",
    id: seriesId,
    name: events[0].seriesName,
    yAxisIndex: eventsAxisIndex,
    data: events.map((event: DataVisuEvent) => ({
      value: [event.timestamp, 0],
      /**
       * Moves the pin up by half its height, so its tip (and not its center) is on the bottom of the chart, plus one
       * pin height per stacking level
       */
      symbolOffset: [0, -(PIN_SIZE / 2 + (pinLevels.get(event) ?? 0) * PIN_SIZE)]
    })),
    color: events[0].color,
    /** Map pin shape, its tip is on the event date */
    symbol: "pin",
    symbolSize: PIN_SIZE,
    /** The letter written in the pin */
    label: {
      show: true,
      position: "inside",
      formatter: (params: DefaultLabelFormatterCallbackParams) => events[params.dataIndex].label,
      color: "#fff",
      fontWeight: "bold"
    }
    // No emphasis focus: hovering a pin must not fade the data series
  }));
}

/**
 * Stacks the event pins that would overlap: going from left to right, each pin is put on the lowest level where it
 * is at least PIN_SIZE pixels away from the previous pin of that level. The overlap depends on the zoom and on the
 * chart width, so this is done again after each zoom and resize.
 *
 * A level is a horizontal row of pins across the whole chart: level 0 is the bottom of the chart, where pins
 * normally are, level 1 is one pin height above, etc. Levels are not tied to a group of close events, any pin can
 * use any level. Most pins stay on level 0, a pin only goes up when the levels below already have a pin too close
 * to it. For example, with pins at 100, 110, 115 and 200 pixels:
 *
 *   level 2 |     ▼115
 *   level 1 |    ▼110
 *   level 0 |   ▼100            ▼200
 *           └──────────────────────── X axis
 *
 * 100, 110 and 115 are too close, so they are stacked. 200 is far enough from 100, so it goes back to level 0.
 *
 * @param target the chart to update, the displayed one or the one drawing the downloaded image
 */
function layoutEventPins(target: echarts.ECharts | undefined): void {
  if (!target || props.events.length === 0) {
    return;
  }
  // Events of series hidden from the legend are ignored. Series missing from "selected" are visible.
  const legend = (target.getOption() as DataVisuOption).legend as LegendComponentOption[];
  const selected: Record<string, boolean> = legend?.[0]?.selected ?? {};
  const visibleEvents: Array<{ event: DataVisuEvent, x: number }> = props.events
      .filter((event: DataVisuEvent) => selected[event.seriesName] !== false)
      .map((event: DataVisuEvent) => ({event, x: target.convertToPixel({xAxisIndex: 0}, event.timestamp) as number}))
      .sort((a, b) => a.x - b.x);

  const pinLevels: Map<DataVisuEvent, number> = new Map<DataVisuEvent, number>();
  /** X position of the last pin placed on each level */
  const lastXPerLevel: number[] = [];
  for (const {event, x} of visibleEvents) {
    let level: number = lastXPerLevel.findIndex((lastX: number) => x - lastX >= PIN_SIZE);
    if (level === -1) {
      level = lastXPerLevel.length;
    }
    lastXPerLevel[level] = x;
    pinLevels.set(event, level);
  }

  target.setOption({series: buildEventSeries(pinLevels)});
}

/**
 * Draws the chart from the current props. Does nothing before the chart is mounted.
 */
function render(): void {
  if (!chart) {
    return;
  }
  // notMerge so series removed from the props are removed from the graphic
  chart.setOption(buildOption(), {notMerge: true});
  isZoomed.value = false;
  enableDragZoom();
  layoutEventPins(chart);
}

/**
 * Switches on the zoom by dragging across the chart. It is the same as clicking the zoom icon of the toolbox, so it
 * must be done again each time the toolbox is recreated (after each render).
 */
function enableDragZoom(): void {
  chart?.dispatchAction({
    type: "takeGlobalCursor",
    key: "dataZoomSelect",
    dataZoomSelectActive: true
  });
}

/**
 * @returns the current zoom of the X axis, read from the mouse wheel dataZoom (which ECharts keeps in sync with the
 * drag zoom, as they control the same axis)
 */
function getZoomRange(): ZoomRange {
  const dataZoom = (chart?.getOption() as DataVisuOption | undefined)?.dataZoom as DataZoomComponentOption[] | undefined;
  return {
    start: dataZoom?.[0]?.start ?? 0,
    end: dataZoom?.[0]?.end ?? 100
  };
}

function resetZoom(): void {
  chart?.dispatchAction({type: "dataZoom", start: 0, end: 100});
}

/**
 * Puts the whole component (header included, so the menu stays usable) in fullscreen, or exits the fullscreen.
 */
function toggleFullscreen(): void {
  if (document.fullscreenElement) {
    document.exitFullscreen();
  } else {
    graphicCard.value?.requestFullscreen();
  }
}

/**
 * Downloads the chart as a PNG image, with the element's name and URI as title.
 *
 * The image is drawn by a temporary chart of a fixed size, without animations, so it doesn't depend on the size of
 * the screen. It keeps the current zoom, line mode and hidden series.
 */
function downloadImage(): void {
  if (!chart) {
    return;
  }
  const exportContainer: HTMLDivElement = document.createElement("div");
  const exportChart: echarts.ECharts = echarts.init(exportContainer, null, {width: EXPORT_WIDTH, height: EXPORT_HEIGHT});

  const currentLegend = (chart.getOption() as DataVisuOption).legend as LegendComponentOption[];
  exportChart.setOption({
    ...buildOption(getZoomRange()),
    animation: false,
    title: {
      text: `${t("DataVisuGraphic.name")} : ${props.elementName ?? ""}\nURI : ${props.elementUri ?? ""}`,
      left: "center"
    },
    grid: {
      left: 20,
      right: 20,
      top: 80,
      bottom: 40,
      outerBoundsMode: "same",
      outerBoundsContain: "all"
    },
    legend: {
      type: "plain",
      bottom: 0,
      /** Series hidden by clicking the legend stay hidden in the image */
      selected: currentLegend[0]?.selected
    }
  });
  layoutEventPins(exportChart);

  const link: HTMLAnchorElement = document.createElement("a");
  link.href = exportChart.getDataURL({type: "png", pixelRatio: 2, backgroundColor: "#fff"});
  link.download = `${formatExportDate(new Date())}_graphic_opensilex.png`;
  link.click();

  exportChart.dispose();
}

/**
 * @returns the date as YYYY-MM-DD, in local time, for the name of the downloaded image
 */
function formatExportDate(date: Date): string {
  const month: string = (date.getMonth() + 1).toString().padStart(2, "0");
  const day: string = date.getDate().toString().padStart(2, "0");
  return `${date.getFullYear()}-${month}-${day}`;
}
//#endregion
//#endregion
</script>

<style scoped lang="scss">
.graphicHeader {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
}

.menuIcon {
  font-size: 1.4rem;
}

.chartContainer {
  width: 100%;
  height: 500px;
}

.emptyGraphic {
  padding: 40px 0;
}

/* In fullscreen, the chart takes all the height left under the header */
.dataVisuGraphic:fullscreen {
  display: flex;
  flex-direction: column;
  padding: 16px;
  background-color: #fff;

  .chartContainer {
    flex: 1;
    height: auto;
  }
}
</style>
