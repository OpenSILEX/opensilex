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
          @handlingEnterKey="reload"
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
                {{ formatParams(row.query_parameters) }}
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
      <button class="btn btn-outline-secondary btn-sm" :disabled="page === 0 || loading" @click="go(page - 1)">
        <opensilex-Icon icon="bi#bi-chevron-left" />
      </button>
      <span>{{ $t('Monitoring.requests.page', { page: page + 1 }) }}</span>
      <button class="btn btn-outline-secondary btn-sm" :disabled="!hasNextPage || loading" @click="go(page + 1)">
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

const props = defineProps<{
  start: Date;
  end: Date;
}>();

const $opensilex: any = inject('$opensilex');

const methods = ['GET', 'POST', 'PUT', 'DELETE', 'PATCH'];
const pageSizes = [20, 50, 100];

const rows = ref<any[]>([]);
const totalCount = ref<number | null>(null);
const hasNextPage = ref(false);
const loading = ref(false);
const error = ref<string | null>(null);

const pathFilter = ref('');
const methodFilter = ref('');
const outcomeFilter = ref('');
const page = ref(0);
const pageSize = ref(20);

let debounceTimer: ReturnType<typeof setTimeout> | null = null;

function service() {
  return $opensilex.getService('opensilex-monitoring.MonitoringService');
}

function onPathFilter(value: string) {
  pathFilter.value = value;
  if (debounceTimer) {
    clearTimeout(debounceTimer);
  }
  debounceTimer = setTimeout(reload, 300);
}

function reload() {
  page.value = 0;
  load();
}

function go(next: number) {
  page.value = Math.max(0, next);
  load();
}

async function load() {
  loading.value = true;
  error.value = null;
  try {
    $opensilex.disableLoader?.();
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
    const body = response.response ?? response;
    rows.value = body?.result ?? [];
    const pagination = body?.metadata?.pagination;
    totalCount.value = pagination?.totalCount ?? null;
    hasNextPage.value = pagination?.hasNextPage ?? rows.value.length === pageSize.value;
  } catch (caught: any) {
    const detail = caught?.response?.result?.title ?? caught?.message;
    error.value = detail
      ? `${$opensilex.$i18n.t('Monitoring.requests.unavailable')} — ${detail}`
      : $opensilex.$i18n.t('Monitoring.requests.unavailable');
    rows.value = [];
  } finally {
    loading.value = false;
    $opensilex.enableLoader?.();
  }
}

function formatDate(value: any): string {
  if (!value) {
    return '—';
  }
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? '—'
    : date.toLocaleString($opensilex?.getLang?.() || 'en');
}

function hasParams(row: any): boolean {
  return row.query_parameters && Object.keys(row.query_parameters).length > 0;
}

function formatParams(params: Record<string, unknown>): string {
  return Object.entries(params)
    .map(([key, value]) => `${key}=${Array.isArray(value) ? value.join(',') : value}`)
    .join('  ·  ');
}

function statusClass(row: any): string {
  const status = row.status ?? 0;
  if (status >= 500) {
    return 'text-bg-danger';
  }
  if (status >= 400) {
    return 'text-bg-warning';
  }
  return 'text-bg-success';
}

watch([methodFilter, outcomeFilter], reload);
watch(pageSize, reload);
watch(() => [props.start, props.end], reload);

defineExpose({ reload });
load();
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
