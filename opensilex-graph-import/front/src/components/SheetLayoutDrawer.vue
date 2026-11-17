<template>
  <n-drawer v-model:show="show" width="80%" placement="right">
    <n-drawer-content :title="$t('GraphImport.layout.title', { sheet: sheet ?? '' })" closable>
      <p class="text-body-secondary small">{{ $t('GraphImport.layout.help') }}</p>
      <n-space :size="8" class="mb-2" align="center">
        <n-tag size="small" type="info">{{ $t('GraphImport.layout.header', { row: headerRow ?? '-' }) }}</n-tag>
        <n-tag size="small" type="success">{{ $t('GraphImport.layout.firstData', { row: firstDataRow ?? '-' }) }}</n-tag>
        <span v-if="sheetRows" class="text-body-secondary small">
          {{ $t('GraphImport.layout.shown', { shown: sheetRows.rows?.length ?? 0, total: sheetRows.total_rows ?? 0 }) }}
        </span>
      </n-space>
      <n-data-table
        size="small"
        :columns="columns"
        :data="tableRows"
        :row-key="rowKey"
        :row-class-name="rowClass"
        :loading="loading"
        :bordered="true"
        :single-line="false"
        :scroll-x="scrollWidth"
      />

      <template #footer>
        <n-space>
          <n-button tertiary :loading="saving" @click="onReset">
            {{ $t('GraphImport.layout.detected') }}
          </n-button>
          <n-button type="primary" :loading="saving" :disabled="!canApply" @click="onApply">
            {{ $t('GraphImport.layout.apply') }}
          </n-button>
        </n-space>
      </template>
    </n-drawer-content>
  </n-drawer>
</template>

<script setup lang="ts">
import { computed, h, inject, ref, resolveComponent, watch } from 'vue'
import type { DataTableColumns } from 'naive-ui'
import type { GraphImportService, GraphImportSessionDTO, SheetRowsDTO } from '../lib'
import { GRAPH_IMPORT_SERVICE, type OpenSilexPlugin } from '../opensilexPlugin'

/** One row of the sheet, numbered as the spreadsheet numbers it. */
interface LayoutRow {
  number: number
  cells: string[]
}

//#region Public
const emit = defineEmits<{
  applied: [session: GraphImportSessionDTO]
}>()

const props = defineProps<{
  sessionId: string
  /** The sheet to lay out, as the import names it. */
  sheet?: string
}>()

const show = defineModel<boolean>('show', { default: false })
//#endregion

//#region Private
//#region Plugins & services
const $opensilex = inject<OpenSilexPlugin>('$opensilex')!
const service = $opensilex.getService<GraphImportService>(GRAPH_IMPORT_SERVICE)
/** Globally registered by the host; resolved here for the cells of the table. */
const NButton = resolveComponent('n-button')
//#endregion

//#region Data & Computed
/** Width a column of cells gets before the table scrolls sideways. */
const COLUMN_WIDTH = 140
/** Width of the column holding the row number and its two buttons. */
const ROW_COLUMN_WIDTH = 230

const sheetRows = ref<SheetRowsDTO>()
const loading = ref(false)
const saving = ref(false)
/** The header row chosen, from 1. */
const headerRow = ref<number>()
/** The first row of values chosen, from 1. */
const firstDataRow = ref<number>()

const tableRows = computed<LayoutRow[]>(() =>
  (sheetRows.value?.rows ?? []).map((cells, index) => ({ number: index + 1, cells }))
)

const widest = computed(() => Math.max(0, ...tableRows.value.map((row) => row.cells.length)))

const columns = computed<DataTableColumns<LayoutRow>>(() => [
  {
    key: 'number',
    title: '#',
    width: ROW_COLUMN_WIDTH,
    fixed: 'left',
    render: renderRowActions
  },
  ...Array.from({ length: widest.value }, (_, index) => ({
    key: String(index),
    title: String(index + 1),
    minWidth: COLUMN_WIDTH / 2,
    ellipsis: { tooltip: true },
    render: (row: LayoutRow) => row.cells[index] ?? ''
  }))
])

const scrollWidth = computed(() => ROW_COLUMN_WIDTH + widest.value * COLUMN_WIDTH)

const canApply = computed(
  () => headerRow.value !== undefined && firstDataRow.value !== undefined && firstDataRow.value > headerRow.value
)
//#endregion

//#region Hooks & watchers
watch([show, () => props.sheet], ([open]) => {
  if (open && props.sheet) {
    loadRows()
  }
})
//#endregion

//#region Event handlers
/** Forgets the rows chosen: the sheet is read again as the reader found it. */
async function onReset() {
  if (!props.sheet) {
    return
  }
  saving.value = true
  try {
    const http = await service.resetSheetLayout(props.sessionId, props.sheet)
    emit('applied', http.response.result)
    show.value = false
  } catch (error) {
    $opensilex.errorHandler(error)
  } finally {
    saving.value = false
  }
}

async function onApply() {
  if (!props.sheet || !canApply.value) {
    return
  }
  saving.value = true
  try {
    const http = await service.setSheetLayout({
      session_id: props.sessionId,
      sheet: props.sheet,
      header_row: headerRow.value,
      first_data_row: firstDataRow.value
    })
    emit('applied', http.response.result)
    show.value = false
  } catch (error) {
    $opensilex.errorHandler(error)
  } finally {
    saving.value = false
  }
}
//#endregion

//#region Methods
async function loadRows() {
  loading.value = true
  try {
    const http = await service.getSheetRows(props.sessionId, props.sheet!)
    sheetRows.value = http.response.result
    headerRow.value = sheetRows.value?.layout?.header_row ?? 1
    firstDataRow.value = sheetRows.value?.layout?.first_data_row ?? 2
  } catch (error) {
    sheetRows.value = undefined
    $opensilex.errorHandler(error)
  } finally {
    loading.value = false
  }
}

/** The header row moves the values below it when they would start above. */
function chooseHeader(row: number) {
  headerRow.value = row
  if (firstDataRow.value === undefined || firstDataRow.value <= row) {
    firstDataRow.value = row + 1
  }
}

function chooseFirstData(row: number) {
  firstDataRow.value = row
}

function renderRowActions(row: LayoutRow) {
  const isHeader = row.number === headerRow.value
  const isFirst = row.number === firstDataRow.value
  return h('div', { class: 'd-flex align-items-center gap-1' }, [
    h('span', { class: 'graph-import-layout__number' }, String(row.number)),
    h(
      NButton,
      { size: 'tiny', type: isHeader ? 'info' : 'default', secondary: !isHeader, onClick: () => chooseHeader(row.number) },
      { default: () => $opensilex.$i18n.t('GraphImport.layout.asHeader') }
    ),
    h(
      NButton,
      {
        size: 'tiny',
        type: isFirst ? 'success' : 'default',
        secondary: !isFirst,
        disabled: headerRow.value !== undefined && row.number <= headerRow.value,
        onClick: () => chooseFirstData(row.number)
      },
      { default: () => $opensilex.$i18n.t('GraphImport.layout.asFirstData') }
    )
  ])
}

function rowKey(row: LayoutRow): number {
  return row.number
}

/** The header row stands out; the rows the import skips are dimmed. */
function rowClass(row: LayoutRow): string {
  if (row.number === headerRow.value) {
    return 'graph-import-layout__header'
  }
  if (headerRow.value === undefined || firstDataRow.value === undefined || row.number < firstDataRow.value) {
    return 'graph-import-layout__skipped'
  }
  return ''
}
//#endregion
//#endregion
</script>

<style scoped lang="scss">
:deep(.graph-import-layout__number) {
  display: inline-block;
  min-width: 2.5rem;
  font-variant-numeric: tabular-nums;
}

:deep(.graph-import-layout__header td) {
  font-weight: 600;
  background-color: rgba(32, 128, 240, 0.08);
}

:deep(.graph-import-layout__skipped td) {
  opacity: 0.55;
}
</style>
