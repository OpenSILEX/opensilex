import type { EChartsCoreOption } from 'echarts/core';
import type { Granularity } from './Granularity';

export interface ActivityPoint {
    bucket?: number | string;
    users?: number;
    requests?: number;
    client_errors?: number;
    server_errors?: number;
    error_percentage?: number;
}

export interface ChartLabels {
    users: string;
    requests: string;
    errors: string;
    average: string;
    errorRate: string;
}

/**
 * The subset of ECharts' tooltip callback parameters this formatter reads. The real type,
 * `CallbackDataParams`, is an internal type not re-exported from the `echarts/core` entry point
 * this module imports from, so a narrow local shape stands in for it rather than a deep import
 * into the library's private type files.
 */
interface TooltipDataPoint {
    dataIndex: number;
    axisValue: string;
    seriesName: string;
    marker: string;
    value: number;
}

/** The platform accent, plus the two series colours the existing dashboard already uses. */
const COLOR_USERS = '#00A38D';
const COLOR_REQUESTS = '#0f839c';
const COLOR_ERRORS = '#ca6434';

/**
 * Secondary text has to stay legible, not just quiet. The platform's --color-gray sits around a
 * 2.6:1 contrast ratio on white, well under the 4.5:1 that small text needs, so the chart uses its
 * own muted tone rather than inheriting one that cannot be read.
 */
const MUTED_TEXT = '#5b6875';

function themeColor(name: string, fallback: string): string {
    if (typeof getComputedStyle !== 'function') {
        return fallback;
    }
    const value = getComputedStyle(document.documentElement).getPropertyValue(name).trim();
    return value || fallback;
}

export function bucketDate(bucket: number | string | undefined): Date | null {
    if (bucket === undefined || bucket === null) {
        return null;
    }
    // The generated client types this field as a number (the Java Instant field is reflected as
    // int64 by default), but the server actually serialises it as an ISO-8601 string, so both
    // branches of the union are handled here rather than trusting either one.
    const date = new Date(bucket);
    return Number.isNaN(date.getTime()) ? null : date;
}

export function formatBucketLabel(
    bucket: number | string | undefined,
    granularity: Granularity,
    locale: string,
): string {
    const date = bucketDate(bucket);
    if (!date) {
        return '';
    }
    switch (granularity) {
        case 'HOUR':
            return date.toLocaleString(locale, { day: '2-digit', month: 'short', hour: '2-digit' });
        case 'MONTH':
            return date.toLocaleDateString(locale, { month: 'short', year: 'numeric' });
        default:
            return date.toLocaleDateString(locale, { day: '2-digit', month: 'short' });
    }
}

/**
 * Builds the chart option. A pure function on purpose: it is the only piece of chart logic worth
 * testing, and it is much easier to test without a canvas.
 *
 * Axis choice: users go left on their own scale because they are a different unit and typically two
 * to three orders of magnitude below the request count, so on a shared axis the bars would vanish.
 * Requests and errors share the right axis, because errors are a strict subset of requests — the
 * gap between the two lines *is* the successful volume. Rescaling errors onto a third axis would
 * make a 0.3 % failure rate look like a crisis.
 */
export function buildActivityOption(
    points: ActivityPoint[],
    granularity: Granularity,
    locale: string,
    labels: ChartLabels,
): EChartsCoreOption {
    const axisColor = MUTED_TEXT;
    const textColor = themeColor('--color-dark', '#212121');
    const categories = points.map((point) => formatBucketLabel(point.bucket, granularity, locale));
    const errors = points.map((point) => (point.client_errors ?? 0) + (point.server_errors ?? 0));

    return {
        // Hundreds of buckets: animation is jank, not polish.
        animation: false,
        grid: { left: 8, right: 8, top: 40, bottom: 60, containLabel: true },
        tooltip: {
            trigger: 'axis',
            axisPointer: { type: 'shadow' },
            formatter: (params: TooltipDataPoint[]) => {
                if (!params.length) {
                    return '';
                }
                const index = params[0].dataIndex;
                const point = points[index] ?? {};
                const requests = point.requests ?? 0;
                const failed = (point.client_errors ?? 0) + (point.server_errors ?? 0);
                const rate = requests === 0 ? 0 : (failed * 100) / requests;
                const lines = params.map(
                    (item) => `${item.marker} ${item.seriesName}: <b>${item.value ?? 0}</b>`,
                );
                lines.push(`${labels.errorRate}: <b>${rate.toFixed(2)} %</b>`);
                return `${params[0].axisValue}<br/>${lines.join('<br/>')}`;
            },
        },
        legend: {
            data: [labels.users, labels.requests, labels.errors],
            top: 0,
            left: 'center',
            textStyle: { color: textColor },
        },
        xAxis: {
            type: 'category',
            data: categories,
            axisLabel: { hideOverlap: true, color: axisColor, fontSize: 11 },
            axisTick: { alignWithLabel: true },
        },
        yAxis: [
            {
                type: 'value',
                position: 'left',
                // A fraction of a user is meaningless.
                minInterval: 1,
                axisLabel: { color: axisColor, fontSize: 11 },
            },
            {
                type: 'value',
                position: 'right',
                minInterval: 1,
                splitLine: { show: false },
                axisLabel: { color: axisColor, fontSize: 11 },
            },
        ],
        dataZoom: [
            // ctrl, so that a plain wheel still scrolls the page.
            { type: 'inside', xAxisIndex: 0, zoomOnMouseWheel: 'ctrl' },
            { type: 'slider', xAxisIndex: 0, height: 22, bottom: 8 },
        ],
        series: [
            {
                name: labels.users,
                type: 'bar',
                yAxisIndex: 0,
                barMaxWidth: 28,
                itemStyle: { color: COLOR_USERS },
                data: points.map((point) => point.users ?? 0),
            },
            {
                name: labels.requests,
                type: 'line',
                yAxisIndex: 1,
                showSymbol: false,
                lineStyle: { color: COLOR_REQUESTS, width: 2 },
                itemStyle: { color: COLOR_REQUESTS },
                data: points.map((point) => point.requests ?? 0),
            },
            {
                name: labels.errors,
                type: 'line',
                yAxisIndex: 1,
                showSymbol: false,
                areaStyle: { opacity: 0.15 },
                lineStyle: { color: COLOR_ERRORS, width: 2 },
                itemStyle: { color: COLOR_ERRORS },
                markLine: {
                    silent: true,
                    symbol: 'none',
                    data: [{ type: 'average', label: { formatter: labels.average } }],
                },
                data: errors,
            },
        ],
    };
}
