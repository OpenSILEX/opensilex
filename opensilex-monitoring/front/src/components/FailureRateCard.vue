<template>
  <opensilex-Card :no-footer="true" label="Monitoring.failure.title" icon="bi-bi-exclamation-octagon">
    <template #body>
      <div class="monitoring-failure">
        <span :class="['monitoring-failure-value', levelClass]">{{ formattedRate }}</span>
        <span class="monitoring-failure-caption" v-if="total > 0">
          {{ $t('Monitoring.failure.ratio', { failed: formattedFailed, total: formattedTotal }) }}
        </span>
        <span class="monitoring-failure-caption" v-else>{{ $t('Monitoring.failure.noTraffic') }}</span>
      </div>

      <!--
        Client and server errors are shown apart because they are not the same event: a 4xx is
        usually somebody mistyping a filter, a 5xx is the platform failing. Only the second one is
        an alert, and a single blended percentage hides that.
      -->
      <dl class="monitoring-failure-split" v-if="total > 0">
        <dt>{{ $t('Monitoring.failure.clientErrors') }}</dt>
        <dd>{{ formattedClient }}</dd>
        <dt>{{ $t('Monitoring.failure.serverErrors') }}</dt>
        <dd :class="{ 'text-danger': serverErrors > 0 }">{{ formattedServer }}</dd>
      </dl>
    </template>
  </opensilex-Card>
</template>

<script setup lang="ts">
import { computed, inject } from 'vue';
import type { OpenSilexPlugin } from '../types/OpenSilexPlugin';

//#region Public
const props = withDefaults(defineProps<{
  clientErrors?: number;
  serverErrors?: number;
  total?: number;
}>(), {
  clientErrors: 0,
  serverErrors: 0,
  total: 0,
});
//#endregion

//#region Private

//#region Plugins and services
const $opensilex = inject<OpenSilexPlugin>('$opensilex')!;
//#endregion

//#region Data and computed
const failed = computed<number>(() => props.clientErrors + props.serverErrors);

const rate = computed<number | null>(() => (props.total === 0 ? null : (failed.value * 100) / props.total));

const formattedRate = computed<string>(() =>
  rate.value === null ? '—' : `${rate.value.toFixed(2)} %`,
);

const formattedFailed = computed<string>(() => failed.value.toLocaleString($opensilex.getLang()));
const formattedTotal = computed<string>(() => props.total.toLocaleString($opensilex.getLang()));
const formattedClient = computed<string>(() => props.clientErrors.toLocaleString($opensilex.getLang()));
const formattedServer = computed<string>(() => props.serverErrors.toLocaleString($opensilex.getLang()));

const levelClass = computed<string>(() => {
  if (rate.value === null) {
    return 'text-muted';
  }
  if (rate.value > 5) {
    return 'text-danger';
  }
  return rate.value > 1 ? 'text-warning' : 'text-success';
});
//#endregion

//#endregion
</script>

<style scoped lang="scss">
// The platform's --color-gray sits near 2.6:1 on white, under the 4.5:1 that small text needs.
// This tone still reads as secondary but is actually legible.
$muted: #5b6875;

// Sits next to the chart card, so it has to match its height rather than float at content height.
:deep(.card) {
  width: 100%;
}

.monitoring-failure {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.monitoring-failure-value {
  font-size: 2rem;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  line-height: 1.1;
}

.monitoring-failure-caption {
  font-size: 0.8rem;
  color: #{$muted};
}

.monitoring-failure-split {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 0.1rem 0.75rem;
  margin: 0.75rem 0 0 0;
  font-size: 0.85rem;
}

.monitoring-failure-split dt {
  font-weight: 400;
  color: #{$muted};
}

.monitoring-failure-split dd {
  margin: 0;
  text-align: right;
  font-variant-numeric: tabular-nums;
}
</style>
