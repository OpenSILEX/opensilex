<template>
  <div class="ai-import-rows">
    <!-- What is wrong, counted by family, before the detail. -->
    <div class="ai-import-rows__summary">
      <span class="fw-semibold">
        {{ $t('AiImport.rows.summary', {
          rows: validation.rows_in_error,
          checked: validation.rows_checked,
          errors: validation.error_count
        }) }}
      </span>
      <span v-for="family in families" :key="family.kind" class="ai-import-rows__family">
        {{ $t('AiImport.rows.family.' + family.kind) }} : {{ family.count }}
      </span>
    </div>

    <p v-if="validation.error_count > validation.errors.length" class="small text-body-secondary mb-1">
      {{ $t('AiImport.rows.sampled', { shown: validation.errors.length, total: validation.error_count }) }}
    </p>

    <!-- About the file as a whole: not pinned on a row it does not belong to. -->
    <ul v-if="fileErrors.length" class="ai-import-rows__file small">
      <li v-for="(error, index) in fileErrors" :key="index">{{ say(error.message) }}</li>
    </ul>

    <!-- One sheet at a time, as the user reads their workbook. -->
    <div v-if="validation.sheets.length > 1" class="ai-import-rows__sheets">
      <n-button
        v-for="sheet in validation.sheets"
        :key="sheet.sheet"
        size="tiny"
        :type="sheet.sheet === currentSheet ? 'primary' : 'default'"
        :secondary="sheet.sheet !== currentSheet"
        @click="currentSheet = sheet.sheet"
      >
        {{ sheet.sheet }} ({{ sheet.rows.length }})
      </n-button>
    </div>

    <n-data-table
      v-if="current"
      size="small"
      :columns="columns"
      :data="current.rows"
      :row-key="(row) => row.row"
      :max-height="360"
      :scroll-x="scrollWidth"
      virtual-scroll
    />
  </div>
</template>

<script setup lang="ts">
import { computed, h, inject, ref, watch } from 'vue'
import { useReportMessage, type ReportMessage } from '../reportMessage'

interface RowError {
  sheet?: string
  row: number
  column?: string
  value?: string
  kind: string
  message: ReportMessage
}

interface SheetRows {
  sheet: string
  headers: string[]
  rows: Array<{ row: number; values: { [header: string]: string } }>
}

interface BulkValidation {
  target: string
  rows_checked: number
  rows_in_error: number
  error_count: number
  errors: RowError[]
  sheets: SheetRows[]
}

/**
 * The faulty rows of a bulk validation, drawn as the user's own rows with the faulty cells marked.
 *
 * Only rows with an error are shown, in the columns of their sheet, so the user sees in one glance
 * which cell of which row to fix in their file — the platform validated an intermediate CSV, but
 * every error has been brought back to the workbook before reaching this component.
 */
const props = defineProps<{ validation: BulkValidation }>()

const $opensilex: any = inject('$opensilex')
const say = useReportMessage($opensilex)

const currentSheet = ref<string | null>(null)
watch(
  () => props.validation,
  (validation) => {
    currentSheet.value = validation.sheets.length ? validation.sheets[0].sheet : null
  },
  { immediate: true }
)

const current = computed(() => props.validation.sheets.find((sheet) => sheet.sheet === currentSheet.value))

const fileErrors = computed(() => props.validation.errors.filter((error) => !error.sheet))

const families = computed(() => {
  const counts: { [kind: string]: number } = {}
  props.validation.errors.forEach((error) => {
    counts[error.kind] = (counts[error.kind] ?? 0) + 1
  })
  return Object.keys(counts).map((kind) => ({ kind, count: counts[kind] }))
})

/** Errors of the current sheet, by row then by column, for the cells to look themselves up. */
const errorsByCell = computed(() => {
  const index: { [row: number]: { [column: string]: RowError[] } } = {}
  props.validation.errors
    .filter((error) => error.sheet === currentSheet.value)
    .forEach((error) => {
      const byColumn = (index[error.row] ??= {})
      ;(byColumn[error.column ?? ''] ??= []).push(error)
    })
  return index
})

const COLUMN_WIDTH = 140

const scrollWidth = computed(() => ((current.value?.headers.length ?? 0) + 2) * COLUMN_WIDTH)

const columns = computed(() => {
  const headers = current.value?.headers ?? []
  return [
    {
      title: String($opensilex.$i18n.t('AiImport.rows.row')),
      key: 'row',
      width: 70,
      fixed: 'left'
    },
    ...headers.map((header) => ({
      title: header,
      key: header,
      width: COLUMN_WIDTH,
      ellipsis: { tooltip: true },
      render: (row: any) => {
        const errors = errorsByCell.value[row.row]?.[header]
        const value = row.values[header] ?? ''
        if (!errors) {
          return value
        }
        // The message sits on the cell itself: the user reads it where the problem is.
        return h('span', {
          class: 'ai-import-rows__faulty',
          title: errors.map((error) => say(error.message)).join('\n')
        }, value === '' ? '∅' : value)
      }
    })),
    {
      title: String($opensilex.$i18n.t('AiImport.rows.errors')),
      key: '__errors',
      width: 320,
      render: (row: any) => {
        const byColumn = errorsByCell.value[row.row] ?? {}
        const messages = Object.values(byColumn).flat().map((error) => say(error.message))
        return h('span', { class: 'ai-import-rows__messages' }, messages.join(' · '))
      }
    }
  ]
})
</script>

<style scoped>
.ai-import-rows {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.ai-import-rows__summary {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 0.75rem;
  font-size: 0.875rem;
}

.ai-import-rows__family {
  color: var(--bs-secondary-color, #6c757d);
}

.ai-import-rows__sheets {
  display: flex;
  flex-wrap: wrap;
  gap: 0.3rem;
}

.ai-import-rows__file {
  margin: 0;
  padding-left: 1.1rem;
  color: var(--bs-danger, #dc3545);
}

/* The faulty cell: marked without hiding its value, which is what the user has to find. */
:deep(.ai-import-rows__faulty) {
  display: inline-block;
  padding: 0 0.3rem;
  border-radius: 3px;
  background: var(--bs-danger-bg-subtle, #f8d7da);
  color: var(--bs-danger-text-emphasis, #58151c);
  cursor: help;
}

:deep(.ai-import-rows__messages) {
  font-size: 0.8rem;
  color: var(--bs-danger, #dc3545);
}
</style>
