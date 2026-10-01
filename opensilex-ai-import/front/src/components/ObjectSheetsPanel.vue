<template>
  <!--
    The sheets whose rows become scientific objects, one tab each: the type their objects take, the
    rows as the file holds them, and above each column what it becomes. Every choice is kept with
    the conversation as soon as it is made; checking asks the platform without writing anything,
    and creating goes through the assistant's proposal card, like every other creation.
  -->
  <div v-if="sheets.length" class="card">
    <div class="card-body">
      <h5 class="card-title">{{ $t('AiImport.objects.title') }}</h5>
      <p class="card-text text-body-secondary small">{{ $t('AiImport.objects.hint') }}</p>

      <nav class="tabs mb-2">
        <button
          v-for="sheet in sheets"
          :key="sheet.sheet"
          type="button"
          :class="['tab', { active: sheet.sheet === currentName }]"
          @click="currentName = sheet.sheet"
        >
          {{ sheet.sheet }}
          <span class="tabBadge">{{ sheet.row_count }}</span>
        </button>
      </nav>

      <div v-if="current" class="d-flex flex-column gap-2">
        <div class="row g-2 align-items-end">
          <div class="col-md-7">
            <opensilex-TypeForm
              :type="current.type ?? undefined"
              :baseType="objectBaseType"
              :label="$t('AiImport.objects.type')"
              :required="true"
              @update:type="chooseType"
            />
          </div>
          <div class="col-md-auto">
            <div class="form-check mb-2">
              <input
                :id="'ai-import-include-' + current.sheet"
                class="form-check-input"
                type="checkbox"
                :checked="current.included"
                @change="include(($event.target as HTMLInputElement).checked)"
              />
              <label class="form-check-label small" :for="'ai-import-include-' + current.sheet">
                {{ $t('AiImport.objects.included') }}
              </label>
            </div>
          </div>
        </div>

        <div v-if="current.type_from_file" class="alert alert-info py-1 small mb-0">
          {{ $t('AiImport.objects.typeFromFile') }}
        </div>
        <div v-if="!current.type && current.included" class="alert alert-warning py-1 small mb-0">
          {{ $t('AiImport.objects.noType') }}
        </div>
        <ul v-if="current.problems.length" class="alert alert-warning py-1 small mb-0 ps-4">
          <li v-for="(problem, index) in current.problems" :key="index">{{ say(problem) }}</li>
        </ul>

        <n-data-table
          size="small"
          :columns="columns"
          :data="current.rows"
          :row-key="(row: any) => row.row"
          :max-height="320"
          :scroll-x="scrollWidth"
          virtual-scroll
        />
        <p v-if="current.row_count > current.rows.length" class="small text-body-secondary mb-0">
          {{ $t('AiImport.objects.preview', { shown: current.rows.length, total: current.row_count }) }}
        </p>
      </div>

      <hr />
      <div class="row g-2 align-items-end">
        <div class="col-md-6">
          <opensilex-ExperimentSelector
            v-model:experiments="experimentUri"
            :label="$t('AiImport.objects.experiment')"
            :required="true"
          />
        </div>
        <div class="col-md-auto d-flex gap-2">
          <n-button size="small" secondary :disabled="!experimentUri" :loading="validating" @click="validate">
            {{ $t('AiImport.objects.validate') }}
          </n-button>
          <!-- The treatments first: an object can only name a level the experiment already has. -->
          <n-button size="small" secondary @click="emit('create', { target: 'FACTORS', value: '' })">
            {{ $t('AiImport.objects.createFactors') }}
          </n-button>
          <n-button size="small" type="primary" :disabled="!ready" @click="create">
            {{ $t('AiImport.objects.create') }}
          </n-button>
        </div>
      </div>

      <div v-if="validation" class="mt-2">
        <div v-if="!validation.error_count" class="alert alert-success py-1 small mb-0">
          {{ $t('AiImport.objects.valid', { rows: validation.rows_checked }) }}
        </div>
        <opensilex-ai-import-ImportRowsGrid v-else :validation="validation" />
      </div>
      <div v-if="error" class="alert alert-danger py-1 small mt-2 mb-0">{{ error }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, h, inject, onMounted, ref, watch } from 'vue'
import { useReportMessage, type ReportMessage } from '../reportMessage'

interface ObjectSheet {
  sheet: string
  name_column?: string
  headers: string[]
  row_count: number
  rows: Array<{ row: number; values: { [header: string]: string } }>
  type?: string | null
  type_from_file: boolean
  included: boolean
  mapping: { [column: string]: string }
  problems: ReportMessage[]
}

interface TypeProperty {
  uri: string
  name: string
  is_object: boolean
  is_list: boolean
  is_required: boolean
}

const props = defineProps<{
  sessionId: string
  /** Changes whenever the report is recomputed: a creation may have changed the sheets. */
  reportStamp?: string
  /** The experiment the report found, offered first. */
  experiment?: string | null
}>()

const emit = defineEmits<{
  (e: 'create', payload: { target: string; value: string }): void
}>()

const $opensilex: any = inject('$opensilex')
const say = useReportMessage($opensilex)

/**
 * What a column can always become, whatever the type: the platform validates these itself. The
 * values are the headers its scientific-object import reads.
 */
const NAME = 'http://www.w3.org/2000/01/rdf-schema#label'
const FACTOR_LEVEL = 'http://www.opensilex.org/vocabulary/oeso#hasFactorLevel'
const COLUMN_WIDTH = 170

const objectBaseType = $opensilex.Oeso.SCIENTIFIC_OBJECT_TYPE_URI

const sheets = ref<ObjectSheet[]>([])
const currentName = ref<string | null>(null)
const propertiesByType = ref<{ [type: string]: TypeProperty[] }>({})
const experimentUri = ref<string | null>(props.experiment ?? null)
const validating = ref(false)
const validation = ref<any>(null)
const error = ref<string | null>(null)

function service() {
  return $opensilex.getService('opensilex-ai-import.AiImportService')
}

function t(key: string, params?: any): string {
  return String($opensilex.$i18n.t(key, params ?? {}))
}

const current = computed(() => sheets.value.find((sheet) => sheet.sheet === currentName.value))

/**
 * Creation is offered once every sheet taking part has a type its mapping agrees with, and an
 * experiment is chosen; the proposal card revalidates everything anyway.
 */
const ready = computed(() => {
  const included = sheets.value.filter((sheet) => sheet.included)
  return !!experimentUri.value && included.length > 0
    && included.every((sheet) => !!sheet.type && sheet.problems.length === 0)
})

async function load() {
  try {
    const http = await service().getObjectSheets(props.sessionId)
    sheets.value = http.response.result ?? []
    if (!sheets.value.some((sheet) => sheet.sheet === currentName.value)) {
      currentName.value = sheets.value.length ? sheets.value[0].sheet : null
    }
    sheets.value.forEach((sheet) => loadProperties(sheet.type))
  } catch (e) {
    // No object sheet is not an error worth a dialog: the panel simply stays hidden.
    console.warn('Could not load the object sheets', e)
  }
}

async function loadProperties(type?: string | null) {
  if (!type || propertiesByType.value[type]) {
    return
  }
  try {
    const http = await service().getTypeProperties(type)
    propertiesByType.value = { ...propertiesByType.value, [type]: http.response.result ?? [] }
  } catch (e) {
    console.warn('Could not load the properties of', type, e)
  }
}

/**
 * Sends one choice for the current sheet and puts the sheet the server returns in its place: the
 * server says what the choice contradicts, the panel does not guess it.
 */
async function save(change: { rdf_type?: string; included?: boolean; mapping?: { [column: string]: string } }) {
  const sheet = current.value
  if (!sheet) {
    return
  }
  error.value = null
  validation.value = null
  try {
    const http = await service().updateObjectSheet({ session_id: props.sessionId, sheet: sheet.sheet, ...change })
    const updated: ObjectSheet = http.response.result
    sheets.value = sheets.value.map((candidate) => (candidate.sheet === updated.sheet ? updated : candidate))
    loadProperties(updated.type)
  } catch (e) {
    error.value = t('AiImport.objects.saveFailed')
  }
}

function chooseType(type: string | string[] | undefined) {
  const chosen = Array.isArray(type) ? type[0] : type
  if (chosen && chosen !== current.value?.type) {
    save({ rdf_type: chosen })
  }
}

function include(included: boolean) {
  save({ included })
}

function mapColumn(column: string, target: string) {
  save({ mapping: { [column]: target } })
}

async function validate() {
  if (!experimentUri.value) {
    return
  }
  validating.value = true
  error.value = null
  try {
    const http = await service().validateObjectSheets({ session_id: props.sessionId, experiment: experimentUri.value })
    validation.value = http.response.result
  } catch (e) {
    error.value = t('AiImport.objects.validateFailed')
  } finally {
    validating.value = false
  }
}

function create() {
  emit('create', { target: 'SCIENTIFIC_OBJECTS', value: '' })
}

/**
 * What a column can be mapped to: nothing, a position, the factor level, or a property of the
 * type. A mapping the type no longer accepts stays listed, marked, so the user sees what to change.
 */
function options(sheet: ObjectSheet, column: string): Array<{ value: string; label: string }> {
  if (column === sheet.name_column) {
    return [{ value: NAME, label: t('AiImport.objects.name') }]
  }
  const list = [
    { value: '', label: t('AiImport.objects.notWritten') },
    { value: 'x', label: t('AiImport.objects.positionX') },
    { value: 'y', label: t('AiImport.objects.positionY') },
    { value: FACTOR_LEVEL, label: t('AiImport.objects.factorLevel') }
  ]
  const properties = sheet.type ? propertiesByType.value[sheet.type] ?? [] : []
  properties
    .filter((property) => property.uri !== NAME && property.uri !== FACTOR_LEVEL)
    .forEach((property) => list.push({
      value: property.uri,
      label: property.name + (property.is_list ? ' (' + t('AiImport.objects.list') + ')' : '')
    }))
  const mapped = sheet.mapping[column] ?? ''
  if (!list.some((option) => option.value === mapped)) {
    list.push({ value: mapped, label: mapped + ' — ' + t('AiImport.objects.unknownToType') })
  }
  return list
}

const scrollWidth = computed(() => ((current.value?.headers.length ?? 0) + 1) * COLUMN_WIDTH)

const columns = computed(() => {
  const sheet = current.value
  if (!sheet) {
    return []
  }
  return [
    { title: t('AiImport.rows.row'), key: 'row', width: 70, fixed: 'left' },
    ...sheet.headers.map((header) => ({
      key: header,
      width: COLUMN_WIDTH,
      ellipsis: { tooltip: true },
      title: () => h('div', { class: 'ai-import-objects__header' }, [
        h('div', { class: 'ai-import-objects__column' }, header),
        h('select', {
          class: 'form-select form-select-sm',
          disabled: header === sheet.name_column,
          title: header === sheet.name_column ? t('AiImport.objects.nameLocked') : undefined,
          onChange: (event: Event) => mapColumn(header, (event.target as HTMLSelectElement).value)
        }, options(sheet, header).map((option) => h('option', {
          value: option.value,
          selected: option.value === (sheet.mapping[header] ?? '')
        }, option.label)))
      ]),
      render: (row: any) => row.values[header] ?? ''
    }))
  ]
})

watch(() => props.reportStamp, load)
watch(() => props.experiment, (experiment) => {
  if (!experimentUri.value && experiment) {
    experimentUri.value = experiment
  }
})
onMounted(load)
</script>

<style scoped>
.ai-import-objects__header {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.ai-import-objects__column {
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
