<template>
  <opensilex-Card :no-header="true" :no-footer="true" class="monitoring-banner">
    <template #body>
      <div class="row g-0">
        <div class="col-12 col-lg-4">
          <opensilex-monitoring-DatabaseStatusTile
            :label="$t('Monitoring.banner.rdf4j')"
            icon="bi#bi-diagram-3"
            :status="componentStatus('rdf4j')"
            :response-time-ms="componentLatency('rdf4j')"
            :message="componentMessage('rdf4j')"
            :stats="tripleStoreStats"
            :stats-message="statsMessage(stats?.triple_store)"
          />
        </div>
        <div class="col-12 col-lg-4">
          <opensilex-monitoring-DatabaseStatusTile
            :label="$t('Monitoring.banner.mongodb')"
            icon="bi#bi-hdd-stack"
            :status="componentStatus('mongodb')"
            :response-time-ms="componentLatency('mongodb')"
            :message="componentMessage('mongodb')"
            :stats="mongoStats"
            :stats-message="statsMessage(stats?.mongodb)"
          />
        </div>
        <div class="col-12 col-lg-4">
          <opensilex-monitoring-ConnectedUsersPanel
            :connected-accounts="stats?.users?.connected_accounts"
            :connected-accounts-reliable="stats?.users?.connected_accounts_reliable"
            :active-accounts="stats?.users?.active_accounts"
            :active-window-minutes="stats?.users?.active_window_minutes"
            :accounts="stats?.users?.accounts || []"
          />
        </div>
      </div>

      <!--
        The access log drops entries rather than blocking the API. That is the right trade, but a
        silent drop would be a lie by omission, so it surfaces here.
      -->
      <div class="alert alert-warning monitoring-banner-alert" v-if="logWarning">
        <opensilex-Icon icon="bi#bi-exclamation-triangle" /> {{ logWarning }}
      </div>
    </template>
  </opensilex-Card>
</template>

<script setup lang="ts">
import { computed, inject } from 'vue';
import type { OpenSilexPlugin } from '../types/OpenSilexPlugin';
import type { HealthCheckDTO } from '../lib/model/healthCheckDTO';
import type { DbStatsDTO } from '../lib/model/dbStatsDTO';
import type { ComponentHealthDTO } from '../lib/model/componentHealthDTO';
import type { TileStat } from './DatabaseStatusTile.vue';

//#region Public
const props = defineProps<{
  health?: HealthCheckDTO | null;
  stats?: DbStatsDTO | null;
}>();
//#endregion

//#region Private

//#region Plugins and services
const $opensilex = inject<OpenSilexPlugin>('$opensilex')!;
//#endregion

/** Both the RDF4J and MongoDB volumetry sections share this shape closely enough that one helper
 *  covers reading their optional error message. */
interface StatsSection {
  message?: string;
}

//#region Data and computed
const tripleStoreStats = computed<TileStat[]>(() => {
  const section = props.stats?.triple_store;
  if (!section || section.triple_count === null || section.triple_count === undefined) {
    return [];
  }
  return [
    { key: 'triples', label: $opensilex.$i18n.t('Monitoring.banner.tripleCount'), value: count(section.triple_count) },
    { key: 'graphs', label: $opensilex.$i18n.t('Monitoring.banner.namedGraphs'), value: count(section.graph_count) },
  ];
});

const mongoStats = computed<TileStat[]>(() => {
  const section = props.stats?.mongodb;
  if (!section || section.data_size_bytes === null || section.data_size_bytes === undefined) {
    return [];
  }
  return [
    { key: 'documents', label: $opensilex.$i18n.t('Monitoring.banner.documents'), value: count(section.object_count) },
    { key: 'data', label: $opensilex.$i18n.t('Monitoring.banner.dataSize'), value: bytes(section.data_size_bytes) },
    { key: 'storage', label: $opensilex.$i18n.t('Monitoring.banner.storageSize'), value: bytes(section.storage_size_bytes) },
    { key: 'index', label: $opensilex.$i18n.t('Monitoring.banner.indexSize'), value: bytes(section.index_size_bytes) },
  ];
});

const logWarning = computed<string | null>(() => {
  const log = props.stats?.request_log;
  if (!log) {
    return null;
  }
  if (log.paused) {
    return $opensilex.$i18n.t('Monitoring.banner.logPaused');
  }
  if (log.dropped && log.dropped > 0) {
    return $opensilex.$i18n.t('Monitoring.banner.logDropped', { count: log.dropped });
  }
  return null;
});
//#endregion

//#region Methods
function component(name: string): ComponentHealthDTO | undefined {
  return props.health?.components?.find((item) => item.name === name);
}

function componentStatus(name: string): string {
  return component(name)?.status ?? 'UNKNOWN';
}

function componentLatency(name: string): number | null {
  const value = component(name)?.response_time_ms;
  return typeof value === 'number' ? value : null;
}

function componentMessage(name: string): string | null {
  return component(name)?.message ?? null;
}

function statsMessage(section: StatsSection | undefined): string | null {
  return section?.message ?? null;
}

function count(value: number | null | undefined): string {
  return value === null || value === undefined ? '—' : value.toLocaleString($opensilex.getLang());
}

/** Bytes are unreadable past a few million; give the operator a unit they can reason about. */
function bytes(value: number | null | undefined): string {
  if (value === null || value === undefined) {
    return '—';
  }
  const units = ['B', 'KiB', 'MiB', 'GiB', 'TiB'];
  let size = value;
  let unit = 0;
  while (size >= 1024 && unit < units.length - 1) {
    size /= 1024;
    unit++;
  }
  return `${size.toLocaleString($opensilex.getLang(), { maximumFractionDigits: 1 })} ${units[unit]}`;
}
//#endregion

//#endregion
</script>

<style scoped lang="scss">
.monitoring-banner :deep(.card-body) {
  padding: 0;
}

// The three tiles are separated by their own borders, so the card adds none of its own padding —
// but each tile then has to breathe on its own, and the last one must not sit flush against the
// card edge.
.monitoring-banner :deep(.row) {
  margin: 0;
}

.monitoring-banner-alert {
  margin: 0.75rem 1rem;
  font-size: 0.85rem;
}
</style>
