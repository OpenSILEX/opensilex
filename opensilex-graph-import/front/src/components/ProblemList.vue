<template>
  <div v-if="check.counts.length" class="graph-import-problems">
    <ul class="small mb-1 ps-3">
      <li v-for="(count, index) in check.counts" :key="'count' + index">
        <n-tag size="tiny" :type="count.severity === 'ERROR' ? 'error' : 'warning'" :bordered="false" class="me-1">
          {{ $t('GraphImport.check.times', { count: count.count ?? 0 }) }}
        </n-tag>
        {{ reportText($opensilex, count.example) }}
      </li>
    </ul>

    <n-list size="small" :show-divider="false">
      <n-list-item v-for="(problem, index) in shown" :key="index">
        <div class="small">
          <span class="text-body-secondary me-1">{{ where(problem) }}</span>
          <strong v-if="problem.node_name" class="me-1">{{ problem.node_name }}</strong>
          <span :class="problem.severity === 'ERROR' ? 'text-danger' : 'text-warning-emphasis'">
            {{ reportText($opensilex, problem.message) }}
          </span>
        </div>
        <div v-if="(problem.cells?.length ?? 0) > 1" class="small text-body-secondary">
          {{ $t('GraphImport.check.alsoIn', { places: otherPlaces(problem) }) }}
        </div>
        <div v-if="problem.suggestions?.length" class="small">
          {{ $t('GraphImport.check.didYouMean') }}
          <n-tag v-for="suggestion in problem.suggestions" :key="suggestion.name" size="tiny" :bordered="false" class="ms-1">
            {{ suggestion.name }} · {{ $t(suggestion.in_file ? 'GraphImport.check.inFile' : 'GraphImport.check.existing') }}
          </n-tag>
        </div>
      </n-list-item>
    </n-list>
    <p v-if="hidden > 0" class="small text-body-secondary mb-0">{{ $t('GraphImport.check.more', { count: hidden }) }}</p>
  </div>
</template>

<script setup lang="ts">
import { computed, inject } from 'vue'
import type { GraphProblemDTO } from '../lib'
import type { OpenSilexPlugin } from '../opensilexPlugin'
import { place, type SheetCheck } from '../problems'
import { reportText } from '../reasons'

/** How many problems one list shows: the counts above it give the rest. */
const SHOWN = 20

//#region Public
const props = defineProps<{
  check: SheetCheck
  /** The headers of the sheet, to name the columns. */
  headers?: string[]
}>()
//#endregion

//#region Private
//#region Plugins & services
const $opensilex = inject<OpenSilexPlugin>('$opensilex')!
//#endregion

//#region Data & Computed
const shown = computed<GraphProblemDTO[]>(() => props.check.problems.slice(0, SHOWN))

/** The problems counted but not listed, here or by the server. */
const hidden = computed(() => {
  const counted = props.check.counts.reduce((sum, count) => sum + (count.count ?? 0), 0)
  return counted - shown.value.length
})
//#endregion

//#region Methods
/** "Row 3 · Date" for a value, "Row 3" for a whole resource. */
function where(problem: GraphProblemDTO): string {
  const cell = place(problem.cells![0], props.headers)
  return problem.about_row
    ? $opensilex.$i18n.t('GraphImport.check.row', { row: cell.row })
    : $opensilex.$i18n.t('GraphImport.check.cell', { row: cell.row, column: cell.column })
}

/** The other cells of a problem, such as the second value of a conflict, given by another sheet. */
function otherPlaces(problem: GraphProblemDTO): string {
  return (problem.cells ?? [])
    .slice(1)
    .map((cell) => {
      const other = place(cell, cell.sheet === problem.cells![0].sheet ? props.headers : undefined)
      return $opensilex.$i18n.t('GraphImport.check.place', other)
    })
    .join(', ')
}
//#endregion
//#endregion
</script>

<style scoped lang="scss">
.graph-import-problems {
  padding: 0.25rem 0.5rem;
}
</style>
