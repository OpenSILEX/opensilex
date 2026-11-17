<template>
  <!--
    How many resources the import would create, complete, leave as they are or cannot settle, and how
    many the file must be corrected for. Each figure is a button that keeps only those rows in the grid.
  -->
  <n-space :size="6" align="center">
    <n-button
      size="small"
      :type="!chosen.state && !chosen.toFix ? 'primary' : 'default'"
      :secondary="!!chosen.state || chosen.toFix"
      @click="emit('choose', { toFix: false })"
    >
      {{ $t('GraphImport.preview.counts.all', { count: counts?.total ?? 0 }) }}
    </n-button>
    <n-button
      v-for="state in NODE_STATES"
      :key="state"
      size="small"
      :type="chosen.state === state ? 'primary' : nodeStateType(state)"
      :secondary="chosen.state !== state"
      @click="emit('choose', { state, toFix: false })"
    >
      {{ $t('GraphImport.preview.counts.' + state, { count: countOf(state) }) }}
    </n-button>
    <n-button
      size="small"
      :type="chosen.toFix ? 'primary' : 'warning'"
      :secondary="!chosen.toFix"
      @click="emit('choose', { toFix: true })"
    >
      {{ $t('GraphImport.preview.counts.toFix', { count: counts?.to_fix ?? 0 }) }}
    </n-button>
  </n-space>
</template>

<script setup lang="ts">
import type { PreviewCountsDTO } from '../lib'
import { NODE_STATES, type NodeState, nodeStateType, type PreviewFilter } from '../preview'

//#region Public
const emit = defineEmits<{
  /** The user wants the grid to keep only these rows. */
  choose: [filter: PreviewFilter]
}>()

const props = defineProps<{
  counts?: PreviewCountsDTO
  /** The rows the grid keeps now. */
  chosen: PreviewFilter
}>()
//#endregion

//#region Private
//#region Methods
function countOf(state: NodeState): number {
  switch (state) {
    case 'CREATED':
      return props.counts?.created ?? 0
    case 'COMPLETED':
      return props.counts?.completed ?? 0
    case 'UNCHANGED':
      return props.counts?.unchanged ?? 0
    default:
      return props.counts?.conflict ?? 0
  }
}
//#endregion
//#endregion
</script>
