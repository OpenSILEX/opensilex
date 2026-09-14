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

const props = withDefaults(defineProps<{
  clientErrors?: number;
  serverErrors?: number;
  total?: number;
}>(), {
  clientErrors: 0,
  serverErrors: 0,
  total: 0,
});

const $opensilex: any = inject('$opensilex');

function locale(): string {
  return $opensilex?.getLang?.() || 'en';
}

const failed = computed(() => props.clientErrors + props.serverErrors);

const rate = computed(() => (props.total === 0 ? null : (failed.value * 100) / props.total));

const formattedRate = computed(() =>
  rate.value === null ? '—' : `${rate.value.toFixed(2)} %`,
);

const formattedFailed = computed(() => failed.value.toLocaleString(locale()));
const formattedTotal = computed(() => props.total.toLocaleString(locale()));
const formattedClient = computed(() => props.clientErrors.toLocaleString(locale()));
const formattedServer = computed(() => props.serverErrors.toLocaleString(locale()));

const levelClass = computed(() => {
  if (rate.value === null) {
    return 'text-muted';
  }
  if (rate.value > 5) {
    return 'text-danger';
  }
  return rate.value > 1 ? 'text-warning' : 'text-success';
});
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
