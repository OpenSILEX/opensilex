<template>
  <!--
    What the sheets of data would give: the data that could be created now, those waiting for the rows
    they are about to be created, the cells written as not available and the cells refused.
  -->
  <div class="graph-import-data-counters mb-2">
    <span class="fw-semibold small">{{ $t('GraphImport.measure.counts.title') }}</span>
    <div v-for="counted in sheets" :key="counted.sheet" class="d-flex align-items-center flex-wrap gap-2 small">
      <span>{{ counted.sheet }}</span>
      <n-tag size="small" type="success" :bordered="false">
        {{ $t('GraphImport.measure.counts.ready', { count: counted.ready ?? 0 }) }}
      </n-tag>
      <n-tag size="small" type="info" :bordered="false">
        {{ $t('GraphImport.measure.counts.waiting', { count: counted.waiting ?? 0 }) }}
      </n-tag>
      <n-tag size="small" :bordered="false">
        {{ $t('GraphImport.measure.counts.notAvailable', { count: counted.not_available ?? 0 }) }}
      </n-tag>
      <n-tag size="small" type="warning" :bordered="false">
        {{ $t('GraphImport.measure.counts.refused', { count: counted.refused ?? 0 }) }}
      </n-tag>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { DataSheetDTO } from '../lib'

//#region Public
defineProps<{
  /** One line per sheet of data. */
  sheets: DataSheetDTO[]
}>()
//#endregion
</script>
