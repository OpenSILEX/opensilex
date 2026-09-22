<template>
  <div class="monitoring-requests">
    <!--
      Same search affordance as the rest of the platform: the shared StringFilter, so the clear
      button, the debounce behaviour and the styling stay identical to every other list screen.
    -->
    <div class="monitoring-requests-filters">
      <div class="monitoring-requests-filter monitoring-requests-filter-wide">
        <opensilex-StringFilter
          :filter="pathFilter"
          placeholder="Monitoring.requests.pathPlaceholder"
          @update="onPathFilter"
          @handlingEnterKey="onFilterHandlingEnterKey"
        />
      </div>

      <select class="form-select form-select-sm monitoring-requests-select" v-model="methodFilter">
        <option value="">{{ $t('Monitoring.requests.anyMethod') }}</option>
        <option v-for="method in methods" :key="method" :value="method">{{ method }}</option>
      </select>

      <select class="form-select form-select-sm monitoring-requests-select" v-model="outcomeFilter">
        <option value="">{{ $t('Monitoring.requests.anyOutcome') }}</option>
        <option value="true">{{ $t('Monitoring.requests.successOnly') }}</option>
        <option value="false">{{ $t('Monitoring.requests.failuresOnly') }}</option>
      </select>

      <span class="monitoring-requests-count" v-if="totalCount !== null">
        {{ $t('Monitoring.requests.count', { count: totalCount }) }}
      </span>
    </div>

    <div class="alert alert-danger monitoring-alert" v-if="error">
      <opensilex-Icon icon="bi#bi-exclamation-octagon" />
      <span>{{ error }}</span>
    </div>

    <div class="monitoring-requests-scroll">
      <table class="table table-sm table-hover monitoring-requests-table">
        <thead>
          <tr>
            <th>{{ $t('Monitoring.requests.column.date') }}</th>
            <th>{{ $t('Monitoring.requests.column.account') }}</th>
            <th>{{ $t('Monitoring.requests.column.method') }}</th>
            <th>{{ $t('Monitoring.requests.column.path') }}</th>
            <th>{{ $t('Monitoring.requests.column.service') }}</th>
            <th class="text-end">{{ $t('Monitoring.requests.column.status') }}</th>
            <th class="text-end">{{ $t('Monitoring.requests.column.duration') }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(row, index) in rows" :key="index">
            <td class="monitoring-requests-nowrap">{{ formatDate(row.start_time) }}</td>
            <td>{{ row.account_email || $t('Monitoring.requests.anonymous') }}</td>
            <td class="monitoring-requests-nowrap">{{ row.http_method }}</td>
            <td>
              <div class="monitoring-requests-path">{{ row.path }}</div>
              <!-- Query parameters are only recorded for a plain GET, so this line is the whole
                   answer to "which call with which arguments". -->
              <div class="monitoring-requests-params" v-if="hasParams(row)">
                {{ formatParams(row.query_parameters!) }}
              </div>
            </td>
            <td class="monitoring-requests-service">{{ row.service || '—' }}</td>
            <td class="text-end">
              <span :class="['badge', statusClass(row)]">{{ row.status ?? '—' }}</span>
            </td>
            <td class="text-end monitoring-requests-nowrap">
              {{ row.duration_ms === null || row.duration_ms === undefined ? '—' : row.duration_ms + ' ms' }}
            </td>
          </tr>
          <tr v-if="!rows.length && !loading">
            <td colspan="7" class="monitoring-requests-empty">
              {{ $t('Monitoring.requests.none') }}
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="monitoring-requests-pager" v-if="rows.length || page > 0">
      <button class="btn btn-outline-secondary btn-sm" :disabled="page === 0 || loading" @click="onPreviousPage">
        <opensilex-Icon icon="bi#bi-chevron-left" />
      </button>
      <span>{{ $t('Monitoring.requests.page', { page: page + 1 }) }}</span>
      <button class="btn btn-outline-secondary btn-sm" :disabled="!hasNextPage || loading" @click="onNextPage">
        <opensilex-Icon icon="bi#bi-chevron-right" />
      </button>

      <select class="form-select form-select-sm monitoring-requests-select" v-model.number="pageSize">
        <option v-for="size in pageSizes" :key="size" :value="size">
          {{ $t('Monitoring.requests.perPage', { count: size }) }}
        </option>
      </select>
    </div>
  </div>
</template>

<script setup lang="ts">
import { inject, ref, watch } from 'vue';
import type { OpenSilexPlugin } from '../types/OpenSilexPlugin';
import { formatApiError, unwrapResult } from '../types/ApiResult';
import type { MonitoringService } from '../lib/api/monitoring.service';
import type { RequestLogDTO } from '../lib/model/requestLogDTO';

type OutcomeFilter = '' | 'true' | 'false';

//#region Public
const props = defineProps<{
  start: Date;
  end: Date;
}>();
//#endregion

//#region Private

//#region Plugins and services
const $opensilex = inject<OpenSilexPlugin>('$opensilex')!;

function service(): MonitoringService {
  return $opensilex.getService<MonitoringService>('opensilex-monitoring.MonitoringService');
}
//#endregion

//#region Data and computed
const methods: string[] = ['GET', 'POST', 'PUT', 'DELETE', 'PATCH'];
const pageSizes: number[] = [20, 50, 100];

const rows = ref<RequestLogDTO[]>([]);
const totalCount = ref<number | null>(null);
const hasNextPage = ref<boolean>(false);
const loading = ref<boolean>(false);
const error = ref<string | null>(null);

const pathFilter = ref<string>('');
const methodFilter = ref<string>('');
const outcomeFilter = ref<OutcomeFilter>('');
const page = ref<number>(0);
const pageSize = ref<number>(20);

let debounceTimer: ReturnType<typeof setTimeout> | null = null;
//#endregion

//#region Hooks and watcher
watch([methodFilter, outcomeFilter], reload);
watch(pageSize, reload);
watch(() => [props.start, props.end], reload);

// Fired here rather than in onMounted: <script setup> top-level code already runs exactly once per
// instance, before the first render, which is the earliest useful point to kick off the initial
// fetch.
load();
//#endregion

//#region Event handlers
function onPathFilter(value: string): void {
  pathFilter.value = value;
  if (debounceTimer) {
    clearTimeout(debounceTimer);
  }
  debounceTimer = setTimeout(reload, 300);
}

function onFilterHandlingEnterKey(): void {
  reload();
}

function onPreviousPage(): void {
  go(page.value - 1);
}

function onNextPage(): void {
  go(page.value + 1);
}
//#endregion

//#region Methods
function reload(): void {
  page.value = 0;
  load();
}

function go(next: number): void {
  page.value = Math.max(0, next);
  load();
}

async function load(): Promise<void> {
  loading.value = true;
  error.value = null;
  try {
    $opensilex.disableLoader();
    const response = await service().searchRequests(
      props.start.toISOString(),
      props.end.toISOString(),
      undefined,
      pathFilter.value || undefined,
      methodFilter.value || undefined,
      outcomeFilter.value === '' ? undefined : outcomeFilter.value === 'true',
      page.value,
      pageSize.value,
    );
    rows.value = unwrapResult(response);
    const pagination = response.response.metadata?.pagination;
    totalCount.value = pagination?.totalCount ?? null;
    hasNextPage.value = pagination?.hasNextPage ?? rows.value.length === pageSize.value;
  } catch (caught: unknown) {
    error.value = formatApiError(caught, $opensilex.$i18n.t('Monitoring.requests.unavailable'));
    rows.value = [];
  } finally {
    loading.value = false;
    $opensilex.enableLoader();
  }
}

/** The wire form of `start_time` is an ISO-8601 string; the generated model types it as a number
 *  only because swagger-core's default reflection on `java.time.Instant` guesses `int64`. */
function formatDate(value: string | number | undefined): string {
  if (!value) {
    return '—';
  }
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? '—' : date.toLocaleString($opensilex.getLang());
}

function hasParams(row: RequestLogDTO): boolean {
  return Boolean(row.query_parameters) && Object.keys(row.query_parameters!).length > 0;
}

function formatParams(params: Record<string, unknown>): string {
  return Object.entries(params)
    .map(([key, value]) => `${key}=${Array.isArray(value) ? value.join(',') : value}`)
    .join('  ·  ');
}

function statusClass(row: RequestLogDTO): string {
  const status = row.status ?? 0;
  if (status >= 500) {
    return 'text-bg-danger';
  }
  if (status >= 400) {
    return 'text-bg-warning';
  }
  return 'text-bg-success';
}
//#endregion

//#endregion

defineExpose({ reload });
</script>

<style scoped lang="scss">
// The platform's --color-gray sits near 2.6:1 on white, under the 4.5:1 that small text needs.
// This tone still reads as secondary but is actually legible.
$muted: #5b6875;

.monitoring-requests-filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
}

.monitoring-requests-filter-wide {
  flex: 1 1 260px;
  min-width: 200px;
  max-width: 420px;
}

.monitoring-requests-select {
  width: auto;
  min-width: 9rem;
  flex-shrink: 0;
}

.monitoring-requests-count {
  margin-left: auto;
  font-size: 0.85rem;
  color: #{$muted};
}

// A path plus its parameters can be long; scroll the table, never the page.
.monitoring-requests-scroll {
  overflow-x: auto;
  max-width: 100%;
}

.monitoring-requests-table {
  font-size: 0.85rem;
  margin-bottom: 0.5rem;
}

.monitoring-requests-nowrap {
  white-space: nowrap;
}

.monitoring-requests-path {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  word-break: break-all;
}

.monitoring-requests-params {
  font-size: 0.75rem;
  color: #{$muted};
  word-break: break-all;
}

.monitoring-requests-service {
  color: #{$muted};
  white-space: nowrap;
}

.monitoring-requests-empty {
  text-align: center;
  color: #{$muted};
  padding: 1.5rem 0;
}

.monitoring-requests-pager {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.85rem;
  color: #{$muted};
}

.monitoring-alert {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.75rem;
  margin-bottom: 0.75rem;
  font-size: 0.875rem;
}
</style>
