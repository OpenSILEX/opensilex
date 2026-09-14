<template>
  <div class="monitoring-tile monitoring-tile-last">
    <div class="monitoring-tile-head">
      <opensilex-Icon icon="bi#bi-people" class="monitoring-tile-icon" />
      <span class="monitoring-tile-label">{{ $t('Monitoring.users.title') }}</span>
      <button
        v-if="accounts.length"
        type="button"
        class="btn btn-link btn-sm p-0"
        @click="expanded = !expanded"
      >
        {{ expanded ? $t('Monitoring.users.hideList') : $t('Monitoring.users.showList') }}
      </button>
    </div>

    <div class="monitoring-users-counts">
      <span class="monitoring-users-count">{{ connectedAccounts ?? 0 }}</span>
      <span class="monitoring-users-caption">{{ $t('Monitoring.users.connected') }}</span>
      <span class="monitoring-users-separator">·</span>
      <span class="monitoring-users-count">{{ activeAccounts ?? 0 }}</span>
      <span class="monitoring-users-caption">
        {{ $t('Monitoring.users.active', { minutes: activeWindowMinutes }) }}
      </span>
    </div>

    <!--
      The token registry stops being pruned when multi-connection is allowed, so the count then
      means "seen since the last restart". Saying so beats quietly showing a wrong number.
    -->
    <p class="monitoring-tile-message" v-if="connectedAccountsReliable === false">
      <opensilex-Icon icon="bi#bi-exclamation-triangle" />
      {{ $t('Monitoring.users.upperBound') }}
    </p>

    <p class="monitoring-tile-message" v-if="!accounts.length">
      {{ $t('Monitoring.users.none') }}
    </p>

    <div class="monitoring-users-chips" v-else-if="!expanded">
      <span class="badge text-bg-light" v-for="account in shown" :key="String(account.uri)">
        {{ displayName(account) }}
      </span>
      <span class="badge text-bg-light" v-if="accounts.length > shown.length">
        {{ $t('Monitoring.users.more', { count: accounts.length - shown.length }) }}
      </span>
    </div>

    <table class="table table-sm monitoring-users-table" v-else>
      <thead>
        <tr>
          <th>{{ $t('Monitoring.users.column.name') }}</th>
          <th>{{ $t('Monitoring.users.column.email') }}</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="account in accounts" :key="String(account.uri)">
          <td>{{ displayName(account) }}</td>
          <td>{{ account.email }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';

interface ConnectedUser {
  uri?: string;
  first_name?: string;
  last_name?: string;
  email?: string;
}

const props = withDefaults(defineProps<{
  connectedAccounts?: number | null;
  connectedAccountsReliable?: boolean | null;
  activeAccounts?: number | null;
  activeWindowMinutes?: number | null;
  accounts?: ConnectedUser[];
}>(), {
  connectedAccounts: 0,
  connectedAccountsReliable: true,
  activeAccounts: 0,
  activeWindowMinutes: 15,
  accounts: () => [],
});

const expanded = ref(false);

const shown = computed(() => props.accounts.slice(0, 6));

function displayName(account: ConnectedUser): string {
  const name = [account.first_name, account.last_name].filter(Boolean).join(' ').trim();
  return name || account.email || String(account.uri ?? '');
}
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

.monitoring-tile-last {
  border-right: none;
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

.monitoring-tile-message {
  font-size: 0.8rem;
  color: #{$muted};
  margin: 0.35rem 0 0 0;
}

.monitoring-users-counts {
  display: flex;
  align-items: baseline;
  gap: 0.35rem;
  flex-wrap: wrap;
}

.monitoring-users-count {
  font-size: 1.4rem;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.monitoring-users-caption,
.monitoring-users-separator {
  font-size: 0.8rem;
  color: #{$muted};
}

.monitoring-users-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.25rem;
  margin-top: 0.5rem;
}

.monitoring-users-table {
  margin-top: 0.5rem;
  font-size: 0.85rem;
}
</style>
