<template>
  <!--
    A sheet as the preview shows it, a page at a time: its rows as written, each filled cell coloured by
    what the import would do with it. A click on a row opens the resource it is.
  -->
  <div class="graph-import-sheet-preview">
    <n-data-table
      remote
      size="small"
      :columns="columns"
      :data="rows?.rows ?? []"
      :loading="loading"
      :pagination="pagination"
      :row-key="rowKey"
      :row-props="rowProps"
      :scroll-x="scrollWidth"
      @update:page="onDataTablePage"
    />
    <p class="small text-body-secondary mt-2 mb-0">{{ $t('GraphImport.preview.grid.legend') }}</p>
  </div>
</template>

<script setup lang="ts">
import { computed, h, inject, onMounted, reactive, ref, resolveComponent, watch } from 'vue'
import type { DataTableColumns } from 'naive-ui'
import type { GraphImportService, PreviewCellDTO, PreviewRowDTO, PreviewRowsDTO } from '../lib'
import { GRAPH_IMPORT_SERVICE, type OpenSilexPlugin } from '../opensilexPlugin'
import { type NodeState, nodeStateType } from '../preview'

/** How many rows a page of the grid shows. */
const PAGE_SIZE = 20

//#region Public
const emit = defineEmits<{
  /** The user wants to see the resource a row is. */
  open: [node: number]
}>()

const props = defineProps<{
  sessionId: string
  sheet: string
  /** Keeps the rows whose resource is in this state; all without one. */
  state?: NodeState
  /** Keeps only the rows whose resource the file must be corrected for. */
  toFix: boolean
  /** Changes whenever the preview is worked out again. */
  stamp: number
}>()
//#endregion

//#region Private
//#region Plugins & services
const $opensilex = inject<OpenSilexPlugin>('$opensilex')!
const service = $opensilex.getService<GraphImportService>(GRAPH_IMPORT_SERVICE)
/** The host's tag, resolved once: the column renderers run outside this component's render. */
const tag = resolveComponent('n-tag')
//#endregion

//#region Data & Computed
const rows = ref<PreviewRowsDTO>()
const loading = ref(false)
const pagination = reactive({ page: 1, pageSize: PAGE_SIZE, itemCount: 0 })

const columns = computed<DataTableColumns<PreviewRowDTO>>(() => [
  {
    key: 'resource',
    title: t('GraphImport.preview.grid.resource'),
    fixed: 'left',
    width: 200,
    render: (row) => resourceCell(row)
  },
  ...(rows.value?.columns ?? []).map((column) => ({
    key: 'c' + column.index,
    title: () =>
      h('span', { class: 'graph-import-sheet-preview__header' }, [
        h('span', column.header ?? ''),
        h('small', { class: 'd-block text-body-secondary' },
          column.property_label ?? t('GraphImport.preview.grid.leftOut'))
      ]),
    minWidth: 140,
    render: (row: PreviewRowDTO) => valueCell(row.cells?.find((cell) => cell.column === column.index))
  }))
])

const scrollWidth = computed(() => 200 + 140 * (rows.value?.columns?.length ?? 0))
//#endregion

//#region Hooks & watchers
onMounted(load)
watch(
  () => [props.sheet, props.state, props.toFix, props.stamp],
  () => {
    pagination.page = 1
    load()
  }
)
//#endregion

//#region Event handlers
function onDataTablePage(page: number) {
  pagination.page = page
  load()
}
//#endregion

//#region Methods
async function load() {
  loading.value = true
  try {
    const http = await service.previewRows(props.sessionId, props.sheet, props.state, props.toFix,
      pagination.page - 1, PAGE_SIZE)
    rows.value = http.response.result
    pagination.itemCount = rows.value?.total_count ?? 0
  } catch (error) {
    $opensilex.errorHandler(error)
  } finally {
    loading.value = false
  }
}

function rowKey(row: PreviewRowDTO): number {
  return row.row ?? 0
}

/** A row that is a resource opens it on click. */
function rowProps(row: PreviewRowDTO) {
  if (row.node === undefined) {
    return {}
  }
  return { class: 'graph-import-sheet-preview__row', onClick: () => emit('open', row.node!) }
}

/** The resource a row is, with what the import would do to it. */
function resourceCell(row: PreviewRowDTO) {
  if (row.node === undefined) {
    return h('span', { class: 'text-body-secondary' }, t('GraphImport.preview.grid.noResource'))
  }
  const tags = [h(tag, { size: 'small', bordered: false, type: nodeStateType(row.node_state) },
    () => t('GraphImport.preview.state.' + row.node_state))]
  if (row.to_fix) {
    tags.push(h(tag, { size: 'small', bordered: false, type: 'warning' }, () => t('GraphImport.preview.grid.toFix')))
  }
  return h('div', [
    h('div', { class: 'fw-semibold text-truncate' }, row.node_name ?? ''),
    h('div', { class: 'd-flex gap-1' }, tags)
  ])
}

/** A cell as written, coloured by what the import would do with it. */
function valueCell(cell: PreviewCellDTO | undefined) {
  if (!cell) {
    return ''
  }
  const state = (cell.state ?? 'IGNORED').toLowerCase()
  return h('span', {
    class: 'graph-import-cell graph-import-cell--' + state,
    title: t('GraphImport.preview.cell.' + cell.state)
  }, cell.raw ?? '')
}

function t(key: string, params?: { [name: string]: unknown }): string {
  return String($opensilex.$i18n.t(key, params))
}
//#endregion
//#endregion
</script>

<style scoped lang="scss">
.graph-import-sheet-preview {
  :deep(.graph-import-sheet-preview__row) {
    cursor: pointer;
  }

  :deep(.graph-import-cell) {
    display: inline-block;
    padding: 0 4px;
    border-radius: 3px;
    border-left: 3px solid transparent;
  }

  :deep(.graph-import-cell--new) {
    border-left-color: var(--bs-info, #2080f0);
    background: rgba(32, 128, 240, 0.08);
  }

  :deep(.graph-import-cell--present) {
    color: var(--bs-secondary-color, #6c757d);
  }

  :deep(.graph-import-cell--conflict),
  :deep(.graph-import-cell--error) {
    border-left-color: var(--bs-danger, #d03050);
    background: rgba(208, 48, 80, 0.1);
  }

  :deep(.graph-import-cell--ambiguous) {
    border-left-color: var(--bs-warning, #f0a020);
    background: rgba(240, 160, 32, 0.12);
  }

  :deep(.graph-import-cell--identifier) {
    font-weight: 600;
  }

  :deep(.graph-import-cell--ignored) {
    color: var(--bs-secondary-color, #6c757d);
    font-style: italic;
  }
}
</style>
