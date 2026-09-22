<template>
  <div class="monitoring-tile">
    <div class="monitoring-tile-head">
      <opensilex-Icon :icon="icon" class="monitoring-tile-icon" />
      <span class="monitoring-tile-label">{{ label }}</span>
      <span :class="['badge', 'rounded-pill', badgeClass]">{{ statusLabel }}</span>
    </div>

    <div class="monitoring-tile-latency" v-if="responseTimeMs !== null && responseTimeMs !== undefined">
      {{ $t('Monitoring.banner.latency', { ms: responseTimeMs }) }}
    </div>

    <p class="monitoring-tile-message" v-if="message">{{ message }}</p>

    <!--
      A down component shows no figures at all rather than zeros: "0 triples" is a different and
      far more alarming claim than "cannot reach the store".
    -->
    <dl class="monitoring-tile-stats" v-if="status === 'UP' && stats.length">
      <template v-for="stat in stats" :key="stat.key">
        <dt>{{ stat.label }}</dt>
        <dd>{{ stat.value }}</dd>
      </template>
    </dl>

    <p class="monitoring-tile-message" v-else-if="status === 'UP' && statsMessage">
      {{ statsMessage }}
    </p>
  </div>
</template>

<script setup lang="ts">
import { computed, inject } from 'vue';
import type { OpenSilexPlugin } from '../types/OpenSilexPlugin';

export interface TileStat {
  key: string;
  label: string;
  value: string;
}

//#region Public
const props = withDefaults(defineProps<{
  label: string;
  icon: string;
  status: string;
  responseTimeMs?: number | null;
  message?: string | null;
  stats?: TileStat[];
  statsMessage?: string | null;
}>(), {
  responseTimeMs: null,
  message: null,
  stats: () => [],
  statsMessage: null,
});
//#endregion

//#region Private

//#region Plugins and services
const $opensilex = inject<OpenSilexPlugin>('$opensilex')!;
//#endregion

//#region Data and computed
const badgeClass = computed<string>(() => {
  switch (props.status) {
    case 'UP':
      return 'text-bg-success';
    case 'DEGRADED':
      return 'text-bg-warning';
    case 'DOWN':
      return 'text-bg-danger';
    default:
      return 'text-bg-secondary';
  }
});

const statusLabel = computed<string>(() =>
  $opensilex.$i18n.t('Monitoring.banner.state.' + (props.status || 'UNKNOWN').toLowerCase()),
);
//#endregion

//#endregion
</script>

<style scoped lang="scss">
// The platform's --color-gray sits near 2.6:1 on white, under the 4.5:1 that small text needs.
// This tone still reads as secondary but is actually legible.
$muted: #5b6875;

.monitoring-tile {
  padding: 0.85rem 1.15rem;
  border-right: 1px solid rgba(0, 0, 0, 0.08);
  height: 100%;
}

// Stacked on a narrow screen the vertical rule is meaningless and a horizontal one reads better.
@media (max-width: 991.98px) {
  .monitoring-tile {
    border-right: none;
    border-bottom: 1px solid rgba(0, 0, 0, 0.08);
  }
}

.monitoring-tile-head {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.25rem;
}

.monitoring-tile-label {
  font-weight: 600;
  flex: 1;
  // The label gives way, never the badge or the action beside it.
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.monitoring-tile-head > :not(.monitoring-tile-label) {
  flex-shrink: 0;
}

.monitoring-tile-icon {
  color: #{$muted};
}

.monitoring-tile-latency,
.monitoring-tile-message {
  font-size: 0.8rem;
  color: #{$muted};
  margin: 0 0 0.35rem 0;
}

.monitoring-tile-stats {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 0.1rem 0.75rem;
  margin: 0;
  font-size: 0.85rem;
}

.monitoring-tile-stats dt {
  font-weight: 400;
  color: #{$muted};
}

.monitoring-tile-stats dd {
  margin: 0;
  text-align: right;
  font-variant-numeric: tabular-nums;
}
</style>
