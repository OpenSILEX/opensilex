<template>
  <span class="d-flex gap-1">
    <n-tag v-if="tally.errors" size="small" type="error" :bordered="false">{{ tally.errors }}</n-tag>
    <n-tag v-if="tally.warnings" size="small" type="warning" :bordered="false">{{ tally.warnings }}</n-tag>
  </span>
</template>

<script setup lang="ts">
import type { Tally } from '../problems'

//#region Public
defineProps<{
  /** How many errors and warnings to show; nothing is shown for none. */
  tally: Tally
}>()
//#endregion
</script>
