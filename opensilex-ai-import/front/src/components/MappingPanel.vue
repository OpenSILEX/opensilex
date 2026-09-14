<template>
  <div class="card">
    <div class="card-header d-flex align-items-center justify-content-between flex-wrap gap-2">
      <div>
        <span class="fw-semibold">{{ $t('AiImport.mapping.title') }}</span>
        <div class="text-body-secondary small">{{ $t('AiImport.mapping.subtitle') }}</div>
      </div>
      <n-checkbox v-model:checked="onlyIssues">
        {{ $t('AiImport.mapping.onlyIssues') }}
      </n-checkbox>
    </div>

    <div class="card-body">
      <p v-if="!visible.length" class="text-body-secondary mb-0">
        {{ $t('AiImport.mapping.none') }}
      </p>

      <n-collapse v-else accordion>
        <n-collapse-item
          v-for="(entry, index) in visible"
          :key="entry.column + index"
          :name="entry.column"
        >
          <template #header>
            <div class="d-flex align-items-center gap-2 flex-wrap">
              <code>{{ entry.column }}</code>
              <n-tag size="small" :type="roleTagType(entry.role)">
                {{ roleLabel(entry.role) }}
              </n-tag>
              <span class="text-body-secondary small">{{ sheetsLabel(entry) }}</span>
              <n-tag v-if="entry.issues && entry.issues.length" size="small" type="warning">
                {{ $t('AiImport.mapping.issuesCount', { count: entry.issues.length }) }}
              </n-tag>
            </div>
          </template>

          <dl class="row mb-2 small">
            <dt class="col-sm-4">{{ $t('AiImport.mapping.entity') }}</dt>
            <dd class="col-sm-8">{{ entry.target_entity || '—' }}</dd>

            <template v-if="entry.resolved_name">
              <dt class="col-sm-4">{{ $t('AiImport.mapping.matched') }}</dt>
              <dd class="col-sm-8">{{ entry.resolved_name }}</dd>
            </template>

            <template v-if="entry.expected_datatype">
              <dt class="col-sm-4">{{ $t('AiImport.mapping.expected') }}</dt>
              <dd class="col-sm-8"><code>{{ shortType(entry.expected_datatype) }}</code></dd>
            </template>

            <dt class="col-sm-4">{{ $t('AiImport.mapping.observed') }}</dt>
            <dd class="col-sm-8">
              {{ kindLabel(entry.observed_kind) }}
              <span class="text-body-secondary">
                — {{ $t('AiImport.mapping.counts', {
                  values: entry.value_count, missing: entry.missing_count
                }) }}
              </span>
            </dd>

            <template v-if="entry.sample_values && entry.sample_values.length">
              <dt class="col-sm-4">{{ $t('AiImport.mapping.examples') }}</dt>
              <dd class="col-sm-8">
                <code v-for="(value, i) in entry.sample_values" :key="i" class="me-2">{{ value }}</code>
              </dd>
            </template>
          </dl>

          <div v-if="entry.suggestion" class="alert alert-warning py-2 small">
            <div class="fw-semibold">{{ $t('AiImport.mapping.suggestion') }}</div>
            {{ say(entry.suggestion_message, entry.suggestion) }}
          </div>

          <div v-if="entry.issues && entry.issues.length" class="table-responsive mb-2">
            <table class="table table-sm align-middle small mb-0">
              <thead>
                <tr>
                  <th>{{ $t('AiImport.mapping.sheet') }}</th>
                  <th>{{ $t('AiImport.mapping.row') }}</th>
                  <th>{{ $t('AiImport.mapping.value') }}</th>
                  <th>{{ $t('AiImport.mapping.problem') }}</th>
                  <th>{{ $t('AiImport.mapping.suggestion') }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(issue, i) in entry.issues" :key="i">
                  <td class="text-nowrap">{{ issue.sheet }}</td>
                  <td>{{ issue.row_number }}</td>
                  <td><code>{{ issue.value }}</code></td>
                  <td>{{ say(issue.problem_message, issue.problem) }}</td>
                  <td>{{ say(issue.suggestion_message, issue.suggestion) }}</td>
                </tr>
              </tbody>
            </table>
          </div>

          <n-button
            v-if="entry.suggestion || (entry.issues && entry.issues.length)"
            size="small"
            secondary
            @click="askForHelp(entry)"
          >
            {{ $t('AiImport.mapping.askForHelp') }}
          </n-button>
        </n-collapse-item>
      </n-collapse>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, inject, ref } from 'vue'
import { useReportMessage, type ReportMessage } from '../reportMessage'


interface TypeIssue {
  sheet: string
  row_number: number
  value: string
  problem: string
  suggestion?: string
  problem_message?: ReportMessage
  suggestion_message?: ReportMessage
}

interface ColumnMapping {
  sheets: string[]
  column: string
  role: string
  target_entity?: string
  resolved_name?: string
  expected_datatype?: string
  observed_kind: string
  value_count: number
  missing_count: number
  sample_values?: string[]
  issues?: TypeIssue[]
  suggestion?: string
  suggestion_message?: ReportMessage
}

/**
 * How each column of the file was mapped onto an OpenSILEX concept, and where the values disagree
 * with the type their variable expects.
 *
 * Read-only on purpose: correcting a value means correcting the file, and the button hands the
 * question to the assistant rather than rewriting anything here.
 */
const props = defineProps<{ mapping: ColumnMapping[] }>()

const emit = defineEmits<{ (event: 'ask', question: string): void }>()

const $opensilex: any = inject('$opensilex')
const say = useReportMessage($opensilex)


const onlyIssues = ref(true)

const visible = computed(() =>
  props.mapping.filter((entry) => {
    if (!onlyIssues.value) {
      return true
    }
    return Boolean(entry.suggestion) || (entry.issues?.length ?? 0) > 0 || entry.role === 'UNKNOWN'
  })
)

function roleLabel(role: string): string {
  return $opensilex.$i18n.t('AiImport.mapping.role_' + role)
}

function kindLabel(kind: string): string {
  return $opensilex.$i18n.t('AiImport.mapping.kind_' + kind)
}

/**
 * The variable roles are the ones that carry measurements, so they are the ones worth colouring;
 * everything else is context and stays neutral.
 */
function roleTagType(role: string): string {
  switch (role) {
    case 'VARIABLE':
      return 'info'
    case 'UNKNOWN':
      return 'warning'
    case 'COMMENT':
      return 'default'
    default:
      return 'success'
  }
}

/**
 * A column is one entry however many sheets it spans, so the header names them without letting a
 * fifteen-sheet template push everything else off the line.
 */
function sheetsLabel(entry: ColumnMapping): string {
  const sheets = entry.sheets ?? []
  if (sheets.length <= 2) {
    return sheets.join(', ')
  }
  return `${sheets[0]}, ${sheets[1]} +${sheets.length - 2}`
}

function shortType(datatype: string): string {
  const hash = datatype.lastIndexOf('#')
  return hash >= 0 ? datatype.substring(hash + 1) : datatype
}

function askForHelp(entry: ColumnMapping) {
  const problem = entry.suggestion
    ?? entry.issues?.[0]?.problem
    ?? ''
  emit('ask', $opensilex.$i18n.t('AiImport.mapping.askTemplate', {
    column: entry.column,
    sheet: (entry.sheets ?? []).join(', '),
    problem
  }))
}
</script>

<style scoped>
/*
 * The app forces every .btn-sm to 32px wide for the icon-only action buttons in its tables
 * (opensilex-front/front/src/styles/common.scss). A labelled button in this panel needs its width
 * back; a scoped selector is specific enough to win without !important.
 */
.btn-sm {
  width: auto;
}

dl.row dt {
  font-weight: 600;
}

dl.row dd {
  margin-bottom: 0.25rem;
}
</style>
