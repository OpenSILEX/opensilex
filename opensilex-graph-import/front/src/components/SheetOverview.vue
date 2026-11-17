<template>
  <div>
    <n-space :size="6" class="mb-3">
      <n-button
        v-for="sheet in sheets"
        :key="sheet.name"
        size="small"
        :type="sheet.name === current?.name ? 'primary' : 'default'"
        :secondary="sheet.name !== current?.name"
        @click="selected = sheet.name"
      >
        {{ sheet.name }}
        <span v-if="sheet.is_tabular" class="ms-1 opacity-75">({{ sheet.data_row_count }})</span>
      </n-button>
    </n-space>

    <template v-if="current">
      <template v-if="!current.is_tabular">
        <div class="d-flex align-items-center gap-2 mb-2">
          <n-tag size="small">{{ $t('GraphImport.sheets.text') }}</n-tag>
          <n-button size="tiny" tertiary @click="emit('chooseRows', current.name)">
            {{ $t('GraphImport.layout.readAsTable') }}
          </n-button>
        </div>
        <pre class="graph-import-sheet__text">{{ current.text }}</pre>
      </template>
      <template v-else>
        <div class="d-flex align-items-center gap-2 mb-2">
          <span class="text-body-secondary small">
            {{ $t('GraphImport.sheets.size', { rows: current.data_row_count, columns: current.headers.length }) }}
          </span>
          <n-button size="tiny" tertiary @click="emit('chooseRows', current.name)">
            {{ $t('GraphImport.layout.choose') }}
          </n-button>
        </div>
        <n-data-table
          size="small"
          :columns="columns"
          :data="rows"
          :bordered="true"
          :single-line="false"
          :scroll-x="scrollWidth"
        />
        <p class="text-body-secondary small mt-2 mb-0">{{ $t('GraphImport.sheets.sample') }}</p>
      </template>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { DataTableColumns } from 'naive-ui'
import type { SheetStructureDTO } from '../lib'

/** One sample row, keyed by the position of its column. */
type SampleRow = { [column: string]: string }

//#region Public
const emit = defineEmits<{
  /** The user wants to choose the header row and the first row of values of this sheet. */
  chooseRows: [sheet: string]
}>()

const props = defineProps<{
  sheets: SheetStructureDTO[]
}>()
//#endregion

//#region Private
//#region Data & Computed
/** Width a column gets before the table scrolls sideways. */
const COLUMN_WIDTH = 160

const selected = ref<string>()

const current = computed<SheetStructureDTO | undefined>(
  () => props.sheets.find((sheet) => sheet.name === selected.value) ?? props.sheets[0]
)

const columns = computed<DataTableColumns<SampleRow>>(() =>
  (current.value?.headers ?? []).map((header, index) => ({
    key: String(index),
    title: header,
    minWidth: COLUMN_WIDTH / 2,
    ellipsis: { tooltip: true }
  }))
)

const rows = computed<SampleRow[]>(() =>
  (current.value?.sample_rows ?? []).map((row) => {
    const keyed: SampleRow = {}
    row.forEach((value, index) => {
      keyed[String(index)] = value
    })
    return keyed
  })
)

const scrollWidth = computed(() => (current.value?.headers?.length ?? 0) * COLUMN_WIDTH)
//#endregion

//#region Hooks & watchers
// A sheet that left with its file is no longer selected.
watch(
  () => props.sheets,
  (sheets) => {
    if (!sheets.some((sheet) => sheet.name === selected.value)) {
      selected.value = undefined
    }
  }
)
//#endregion
//#endregion
</script>

<style scoped lang="scss">
.graph-import-sheet__text {
  white-space: pre-wrap;
  max-height: 20rem;
  overflow: auto;
}
</style>
