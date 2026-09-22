<template>
  <div class="monitoring-view container-fluid">
    <!--
      The route is already filtered server-side for non-admins, so reaching this branch means a
      misconfiguration. It still refuses to issue a single request, because a half-populated page
      is worse than a clear refusal. This is ergonomics, never security: the endpoints are
      @ApiProtected(adminOnly) and that is what actually enforces the rule.
    -->
    <div class="alert alert-warning monitoring-alert" v-if="!isAdmin">
      <opensilex-Icon icon="bi#bi-shield-lock" />
      <span>{{ $t('Monitoring.errors.adminOnly') }}</span>
    </div>

    <template v-else>
      <!-- No page title here: the application layout already names the page from the route, and a
           second heading in the content area only repeats it. -->
      <div class="monitoring-header">
        <button type="button" class="btn btn-outline-secondary btn-sm" @click="onRefreshClick">
          <opensilex-Icon icon="bi#bi-arrow-clockwise" /> {{ $t('Monitoring.refresh') }}
        </button>
        <span class="monitoring-updated" v-if="lastUpdated">
          {{ $t('Monitoring.lastUpdated', { time: lastUpdated }) }}
        </span>
      </div>

      <div class="alert alert-danger monitoring-alert" v-if="statusError">
        <opensilex-Icon icon="bi#bi-exclamation-octagon" />
        <span>{{ statusError }}</span>
      </div>

      <opensilex-monitoring-MonitoringBanner :health="health" :stats="stats" />

      <!-- Same tab markup as the detail pages elsewhere in the platform, so it inherits the theme's
           .tabs / .tab / .tab.active styling instead of inventing a look of its own. -->
      <nav class="tabs mb-3">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          :class="['tab', { active: currentTab === tab.key }]"
          @click="onTabClick(tab.key)"
        >
          <opensilex-Icon :icon="tab.icon" /> {{ $t(tab.label) }}
        </button>
      </nav>

      <opensilex-monitoring-PeriodSelector
        v-model:preset="preset"
        v-model:granularity="granularity"
        v-model:start="start"
        v-model:end="end"
      />

      <template v-if="currentTab === 'overview'">
      <div class="alert alert-info monitoring-alert" v-if="report && report.truncated">
        <opensilex-Icon icon="bi#bi-info-circle" />
        <span>{{ $t('Monitoring.period.truncated', { granularity: appliedGranularityLabel }) }}</span>
      </div>

      <div class="alert alert-danger monitoring-alert" v-if="activityError">
        <opensilex-Icon icon="bi#bi-exclamation-octagon" />
        <span>{{ activityError }}</span>
      </div>

      <div class="row g-3 monitoring-row">
        <div class="col-12 col-xl-9">
          <opensilex-monitoring-ActivityChart
            :buckets="buckets"
            :granularity="appliedGranularity"
            :loading="activityLoading"
            @zoom="onZoom"
          />
        </div>
        <div class="col-12 col-xl-3">
          <opensilex-monitoring-FailureRateCard
            :client-errors="windowClientErrors"
            :server-errors="windowServerErrors"
            :total="windowRequests"
          />
        </div>
      </div>
      </template>

      <opensilex-monitoring-RequestLogTable
        v-else
        ref="requestTable"
        :start="start"
        :end="end"
      />
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, inject, onMounted, onUnmounted, ref, useTemplateRef, watch } from 'vue';
import { coarsestNeeded, presetRange, type Granularity, type PeriodPreset } from '../models/Granularity';
import type { OpenSilexPlugin } from '../types/OpenSilexPlugin';
import { formatApiError, unwrapResult } from '../types/ApiResult';
import type { MonitoringService } from '../lib/api/monitoring.service';
import type { HealthCheckDTO } from '../lib/model/healthCheckDTO';
import type { DbStatsDTO } from '../lib/model/dbStatsDTO';
import type { ActivityReportDTO } from '../lib/model/activityReportDTO';
import type { ActivityBucketDTO } from '../lib/model/activityBucketDTO';
import type { ZoomRange } from './ActivityChart.vue';
import type RequestLogTable from './RequestLogTable.vue';

// A page-level route component: no props, no emits, nothing to expose to a parent. Everything
// below is therefore private.
//#region Private

//#region Plugins and services
const $opensilex = inject<OpenSilexPlugin>('$opensilex')!;

function service(): MonitoringService {
  return $opensilex.getService<MonitoringService>('opensilex-monitoring.MonitoringService');
}
//#endregion

//#region Template refs
const requestTable = useTemplateRef<InstanceType<typeof RequestLogTable>>('requestTable');
//#endregion

//#region Data and computed
interface MonitoringTab {
  key: 'overview' | 'requests';
  label: string;
  icon: string;
}

const isAdmin = computed<boolean>(() => $opensilex.getUser()?.isAdmin() ?? false);

// The view owns every piece of shared state. That is what keeps the chart and the failure card from
// ever disagreeing: they read the same array and the same visible window.
const health = ref<HealthCheckDTO | null>(null);
const stats = ref<DbStatsDTO | null>(null);
const report = ref<ActivityReportDTO | null>(null);
const statusError = ref<string | null>(null);
const activityError = ref<string | null>(null);
const activityLoading = ref<boolean>(false);
const lastUpdated = ref<string | null>(null);

const initial = presetRange('7d');
const preset = ref<PeriodPreset>('7d');
const granularity = ref<Granularity>('DAY');
const start = ref<Date>(initial.from);
const end = ref<Date>(initial.to);

const visibleStart = ref<number>(0);
const visibleEnd = ref<number>(-1);

const tabs: MonitoringTab[] = [
  { key: 'overview', label: 'Monitoring.tabs.overview', icon: 'bi#bi-graph-up' },
  { key: 'requests', label: 'Monitoring.tabs.requests', icon: 'bi#bi-list-ul' },
];
const currentTab = ref<MonitoringTab['key']>('overview');

let statusTimer: ReturnType<typeof setInterval> | null = null;
let activityTimer: ReturnType<typeof setTimeout> | null = null;

const buckets = computed<ActivityBucketDTO[]>(() => report.value?.buckets ?? []);

const appliedGranularity = computed<Granularity>(
  // The backend only ever names one of the four known buckets, even though the DTO types the
  // field as a plain string: it is the wire form of the Java enum.
  () => (report.value?.granularity as Granularity | undefined) ?? granularity.value,
);

const appliedGranularityLabel = computed<string>(() =>
  $opensilex.$i18n.t('Monitoring.period.granularities.' + appliedGranularity.value.toLowerCase()),
);

const windowed = computed<ActivityBucketDTO[]>(() => {
  const list = buckets.value;
  const last = list.length - 1;
  const to = visibleEnd.value < 0 ? last : Math.min(visibleEnd.value, last);
  return list.slice(Math.max(0, visibleStart.value), to + 1);
});

const windowRequests = computed<number>(() =>
  windowed.value.reduce((sum, bucket) => sum + (bucket.requests ?? 0), 0),
);
const windowClientErrors = computed<number>(() =>
  windowed.value.reduce((sum, bucket) => sum + (bucket.client_errors ?? 0), 0),
);
const windowServerErrors = computed<number>(() =>
  windowed.value.reduce((sum, bucket) => sum + (bucket.server_errors ?? 0), 0),
);
//#endregion

//#region Hooks and watcher
onMounted(() => {
  if (!isAdmin.value) {
    return;
  }
  loadStatus();
  loadActivity();
  statusTimer = setInterval(() => {
    // No point polling a tab nobody is looking at.
    if (document.visibilityState === 'visible') {
      loadStatus();
    }
  }, 30_000);
});

onUnmounted(() => {
  if (statusTimer) {
    clearInterval(statusTimer);
    statusTimer = null;
  }
  if (activityTimer) {
    clearTimeout(activityTimer);
    activityTimer = null;
  }
});

watch([start, end], () => {
  // Promote rather than let the user request something unreadable.
  const needed = coarsestNeeded(granularity.value, start.value.getTime(), end.value.getTime());
  if (needed !== granularity.value) {
    granularity.value = needed;
    $opensilex.showInfoToast(
      $opensilex.$i18n.t('Monitoring.period.granularityAdjusted', {
        granularity: $opensilex.$i18n.t('Monitoring.period.granularities.' + needed.toLowerCase()),
      }),
    );
    return;
  }
  scheduleActivity();
});

watch(granularity, scheduleActivity);
//#endregion

//#region Event handlers
function onTabClick(tab: MonitoringTab['key']): void {
  currentTab.value = tab;
}

function onZoom(range: ZoomRange): void {
  visibleStart.value = range.startIndex;
  visibleEnd.value = range.endIndex;
}

function onRefreshClick(): void {
  refreshAll();
}
//#endregion

//#region Methods
/** The refresh button is global: it reloads whatever the visible tab is showing, plus the banner. */
function refreshAll(): void {
  loadStatus();
  if (currentTab.value === 'overview') {
    loadActivity();
  } else {
    requestTable.value?.reload();
  }
}

async function loadStatus(): Promise<void> {
  statusError.value = null;
  try {
    // Silence the global loading overlay: this runs every thirty seconds and would otherwise
    // flash the whole page.
    $opensilex.disableLoader();
    const [healthResponse, statsResponse] = await Promise.all([
      service().healthcheck(),
      service().dbStats(),
    ]);
    health.value = unwrapResult(healthResponse);
    stats.value = unwrapResult(statsResponse);
    lastUpdated.value = new Date().toLocaleTimeString($opensilex.getLang());
  } catch (error: unknown) {
    statusError.value = formatApiError(error, $opensilex.$i18n.t('Monitoring.errors.statusUnavailable'));
  } finally {
    $opensilex.enableLoader();
  }
}

async function loadActivity(): Promise<void> {
  activityError.value = null;
  activityLoading.value = true;
  try {
    $opensilex.disableLoader();
    const response = await service().activity(
      start.value.toISOString(),
      end.value.toISOString(),
      granularity.value,
    );
    report.value = unwrapResult(response);
    // A new series means the previous brush no longer refers to anything.
    visibleStart.value = 0;
    visibleEnd.value = -1;
  } catch (error: unknown) {
    activityError.value = formatApiError(error, $opensilex.$i18n.t('Monitoring.errors.activityUnavailable'));
    report.value = null;
  } finally {
    activityLoading.value = false;
    $opensilex.enableLoader();
  }
}

function scheduleActivity(): void {
  if (activityTimer) {
    clearTimeout(activityTimer);
  }
  activityTimer = setTimeout(loadActivity, 250);
}
//#endregion

//#endregion
</script>

<style scoped lang="scss">
// The platform's --color-gray sits near 2.6:1 on white, under the 4.5:1 that small text needs.
// This tone still reads as secondary but is actually legible.
$muted: #5b6875;

.monitoring-view {
  padding: 1rem;
  // Last line of defence: a stray wide child must never scroll the whole page sideways, because
  // that is what pushes the header button out of view.
  overflow-x: hidden;
  // Nothing on this page should ever push a horizontal scrollbar: the banner and the chart both
  // hold wide content, and the page header sits flush against the right edge.
  max-width: 100%;
}

// One line, not a block: these say something short and should not shout.
.monitoring-alert {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.75rem;
  margin-bottom: 0.75rem;
  font-size: 0.875rem;
}

// The chart and the KPI sit side by side; without this the KPI card floats at its content height
// and the row looks unbalanced.
.monitoring-row > [class^="col"] {
  display: flex;
  // Without this a flex item refuses to shrink below its content width. The chart canvas carries an
  // explicit pixel width, so the column would stay as wide as the canvas ever was, push the page
  // past the viewport, and every right-aligned action would end up clipped off-screen.
  min-width: 0;
}

.monitoring-row > [class^="col"] > * {
  width: 100%;
}

.monitoring-header {
  display: flex;
  align-items: center;
  // Refresh sits first, on the left: it is the one control an operator reaches for repeatedly,
  // and a right-aligned action is the one that gets pushed off screen when space runs short.
  justify-content: flex-start;
  flex-wrap: wrap;
  gap: 0.75rem;
  margin-bottom: 0.75rem;
}

.monitoring-header h2 {
  margin: 0;
  font-size: 1.35rem;
  min-width: 0;
}

.monitoring-header-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-shrink: 0;
  white-space: nowrap;
}

.monitoring-updated {
  font-size: 0.8rem;
  color: #{$muted};
}
</style>
