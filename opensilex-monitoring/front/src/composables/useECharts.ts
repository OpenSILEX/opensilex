import { onMounted, onUnmounted, ref, watch, type Ref } from 'vue';
import * as echarts from 'echarts/core';
import { BarChart, LineChart } from 'echarts/charts';
import {
    DataZoomInsideComponent,
    DataZoomSliderComponent,
    GridComponent,
    LegendComponent,
    MarkLineComponent,
    TooltipComponent,
} from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';

// Selective registration, not `import * from 'echarts'`: the barrel import roughly doubles the
// bundle for charts this page never draws.
echarts.use([
    BarChart,
    LineChart,
    GridComponent,
    TooltipComponent,
    LegendComponent,
    DataZoomInsideComponent,
    DataZoomSliderComponent,
    MarkLineComponent,
    CanvasRenderer,
]);

export type EChartsInstance = echarts.ECharts;

/**
 * Owns one ECharts instance bound to a div: creation, option updates, resizing and — the part that
 * is easy to lose in a refactor and expensive to lose — disposal.
 *
 * The route component is recreated on every navigation, so an undisposed chart leaks a canvas and
 * keeps its animation loop running for the life of the tab.
 */
export function useECharts(container: Ref<HTMLElement | null>) {
    const chart = ref<EChartsInstance | null>(null);
    let observer: ResizeObserver | null = null;
    let pending = false;

    function resize() {
        // Coalesce bursts of resize events into one layout pass per frame.
        if (pending) {
            return;
        }
        pending = true;
        requestAnimationFrame(() => {
            pending = false;
            chart.value?.resize();
        });
    }

    function setOption(option: unknown) {
        // notMerge, because series lengths change between periods and merging would leave stale
        // points from the previous query on screen.
        chart.value?.setOption(option as never, { notMerge: true });
    }

    function toDataURL(): string | undefined {
        return chart.value?.getDataURL({ type: 'png', pixelRatio: 2, backgroundColor: '#ffffff' });
    }

    onMounted(() => {
        if (!container.value) {
            return;
        }
        chart.value = echarts.init(container.value, undefined, { renderer: 'canvas' });

        // Both listeners are needed. The ResizeObserver catches layout changes; the window listener
        // catches the synthetic resize event the app dispatches after the side menu is collapsed.
        window.addEventListener('resize', resize);
        observer = new ResizeObserver(resize);
        observer.observe(container.value);
    });

    onUnmounted(() => {
        window.removeEventListener('resize', resize);
        observer?.disconnect();
        observer = null;
        chart.value?.dispose();
        chart.value = null;
    });

    watch(container, (element) => {
        if (element && !chart.value) {
            chart.value = echarts.init(element, undefined, { renderer: 'canvas' });
        }
    });

    return { chart, setOption, resize, toDataURL };
}
