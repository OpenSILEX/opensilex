<template>
  <div class="graph-import-toolbar">
    <n-space :size="6" align="center">
      <n-button
        size="small"
        :type="filter === 'undecided' ? 'warning' : 'default'"
        :secondary="filter !== 'undecided'"
        :disabled="!undecided"
        @click="onFilterClick('undecided')"
      >
        {{ $t('GraphImport.understandGraph.toDecide', { count: undecided }) }}
      </n-button>
      <n-button
        size="small"
        :type="filter === 'problems' ? 'error' : 'default'"
        :secondary="filter !== 'problems'"
        :disabled="!problems.errors && !problems.warnings"
        @click="onFilterClick('problems')"
      >
        {{ $t('GraphImport.understandGraph.problems', { count: problems.errors + problems.warnings }) }}
      </n-button>
      <n-divider vertical />
      <n-button size="small" tertiary :aria-label="$t('GraphImport.understandGraph.zoomIn')" @click="emit('zoomIn')">+</n-button>
      <n-button size="small" tertiary :aria-label="$t('GraphImport.understandGraph.zoomOut')" @click="emit('zoomOut')">−</n-button>
      <n-button size="small" tertiary @click="emit('fit')">{{ $t('GraphImport.understandGraph.fit') }}</n-button>
      <n-divider vertical />
      <n-checkbox v-model:checked="showLeftOut" size="small">{{ $t('GraphImport.understandGraph.showLeftOut') }}</n-checkbox>
    </n-space>

    <n-collapse class="mt-1">
      <n-collapse-item :title="$t('GraphImport.understandGraph.legend.title')" name="legend">
        <ul class="graph-import-toolbar__legend small mb-0">
          <li v-for="entry in LEGEND" :key="entry.key">
            <svg width="34" height="16" viewBox="0 0 34 16" aria-hidden="true" :class="'graph-import-toolbar__swatch--' + entry.key">
              <circle v-if="entry.shape === 'circle'" cx="17" cy="8" r="6" />
              <line v-else x1="2" y1="8" x2="32" y2="8" />
            </svg>
            {{ $t('GraphImport.understandGraph.legend.' + entry.key) }}
          </li>
        </ul>
        <p class="small text-body-secondary mb-0">{{ $t('GraphImport.understandGraph.legend.gestures') }}</p>
      </n-collapse-item>
    </n-collapse>
  </div>
</template>

<script setup lang="ts">
import type { GraphFilter } from '../graphViewState'
import type { Tally } from '../problems'

/** One line of the legend: a mark and what it means. */
interface LegendEntry {
  key: string
  shape: 'circle' | 'line'
}

const LEGEND: LegendEntry[] = [
  { key: 'sheet', shape: 'circle' },
  { key: 'declared', shape: 'circle' },
  { key: 'existing', shape: 'circle' },
  { key: 'value', shape: 'circle' },
  { key: 'undecided', shape: 'circle' },
  { key: 'chosen', shape: 'line' },
  { key: 'suggested', shape: 'line' },
  { key: 'error', shape: 'line' },
  { key: 'shared', shape: 'line' }
]

//#region Public
const emit = defineEmits<{
  zoomIn: []
  zoomOut: []
  /** Show the whole graph. */
  fit: []
}>()

defineProps<{
  undecided: number
  problems: Tally
}>()

/** The elements brought forward; undefined for none. */
const filter = defineModel<GraphFilter | undefined>('filter')

/** Whether the columns left out are drawn. */
const showLeftOut = defineModel<boolean>('showLeftOut', { required: true })
//#endregion

//#region Private
//#region Event handlers
function onFilterClick(clicked: GraphFilter) {
  filter.value = filter.value === clicked ? undefined : clicked
}
//#endregion
//#endregion
</script>

<style scoped lang="scss">
.graph-import-toolbar__legend {
  list-style: none;
  padding-left: 0;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(16rem, 1fr));
  gap: 0.15rem 1rem;

  svg {
    vertical-align: middle;
    margin-right: 0.25rem;
  }

  circle {
    stroke-width: 2;
  }

  line {
    stroke-width: 2;
  }
}

.graph-import-toolbar__swatch--sheet circle {
  fill: color-mix(in srgb, var(--bs-purple, #6f42c1) 16%, var(--bs-body-bg, #fff));
  stroke: var(--bs-purple, #6f42c1);
}

.graph-import-toolbar__swatch--declared circle {
  fill: color-mix(in srgb, var(--bs-purple, #6f42c1) 16%, var(--bs-body-bg, #fff));
  stroke: var(--bs-orange, #fd7e14);
  stroke-dasharray: 2 3;
}

.graph-import-toolbar__swatch--existing circle {
  fill: color-mix(in srgb, #f07167 18%, var(--bs-body-bg, #fff));
  stroke: #e05d53;
}

.graph-import-toolbar__swatch--value circle {
  fill: color-mix(in srgb, var(--bs-teal, #20c997) 22%, var(--bs-body-bg, #fff));
  stroke: var(--bs-teal, #20c997);
}

.graph-import-toolbar__swatch--undecided circle {
  fill: var(--bs-tertiary-bg, #f8f9fa);
  stroke: var(--bs-secondary-color, #6c757d);
  stroke-dasharray: 3 3;
}

.graph-import-toolbar__swatch--chosen line {
  stroke: var(--bs-purple, #6f42c1);
}

.graph-import-toolbar__swatch--suggested line {
  stroke: var(--bs-purple, #6f42c1);
  stroke-dasharray: 6 4;
}

.graph-import-toolbar__swatch--error line {
  stroke: var(--bs-danger, #d03050);
}

.graph-import-toolbar__swatch--shared line {
  stroke: var(--bs-secondary-color, #6c757d);
  stroke-dasharray: 1 5;
  stroke-linecap: round;
  stroke-width: 2.5;
}
</style>
