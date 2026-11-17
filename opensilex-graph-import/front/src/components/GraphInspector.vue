<template>
  <div class="graph-import-inspector">
    <template v-if="pending">
      <h6>{{ $t('GraphImport.understandGraph.pending.title', { column: pendingColumn?.header ?? '', target: pending.targetLabel }) }}</h6>
      <p class="small text-body-secondary">{{ $t('GraphImport.understandGraph.pending.help') }}</p>
      <n-space vertical :size="6">
        <n-button v-for="property in pending.properties" :key="property.uri" size="small" @click="emit('choose', property.uri)">
          {{ property.label }}
          <span v-if="property.score !== undefined" class="ms-1 text-body-secondary">{{ percent(property.score) }}</span>
        </n-button>
        <n-button size="small" tertiary @click="emit('cancel')">{{ $t('GraphImport.understandGraph.cancel') }}</n-button>
      </n-space>
    </template>

    <template v-else-if="selected?.kind === 'column' && sheet && column">
      <div class="d-flex align-items-center gap-2 mb-1">
        <h6 class="mb-0">{{ columnTitle(column, sheet) }}</h6>
        <n-tag v-if="column.given" size="tiny" type="info" :bordered="false">{{ $t('GraphImport.missing.givenTag') }}</n-tag>
        <ChoiceStatusTag :status="column.type_column ? 'DECIDED' : column.property?.status" />
      </div>
      <p class="small text-body-secondary mb-2">
        {{ $t('GraphImport.understandGraph.columnOf', { sheet: sheet.name, index: (column.index ?? 0) + 1 }) }}
      </p>
      <p v-if="droppedOn" class="small text-success mb-1">{{ $t('GraphImport.understandGraph.dropped', { target: droppedOn }) }}</p>
      <ul v-if="landing.length" class="small mb-2 ps-3">
        <li v-for="(line, index) in landing" :key="index" :class="{ 'text-danger': line.unresolved }">{{ line.text }}</li>
      </ul>
      <p v-if="column.type_column" class="small mb-2">{{ $t('GraphImport.understandGraph.typeColumnHelp') }}</p>
      <MeasureVariable
        v-if="isDataSheet(sheet) && !column.type_column && !column.identifier"
        :sheet="sheet"
        :column="column"
        :busy="busy"
        @decide="onSheetChoicesDecide"
      />
      <ColumnEvidence
        v-else
        :column="column"
        :properties="sheet.properties ?? []"
        @choose="onColumnEvidenceChoose"
        @leave-out="onColumnEvidenceLeaveOut"
      />
      <DateReadingForm v-if="columnCheck.dateColumns.length" :date-column="columnCheck.dateColumns[0]" @read="onDateReadingFormRead" />
      <ProblemList v-if="columnCheck.counts.length" :check="columnCheck" :headers="headers" />
      <n-button size="tiny" tertiary class="mt-2" @click="emit('select', { kind: 'sheet', sheet: sheet.name ?? '' })">
        {{ $t('GraphImport.understandGraph.backToSheet', { sheet: sheet.name }) }}
      </n-button>
    </template>

    <template v-else-if="(selected?.kind === 'sheet' || selected?.kind === 'bundle') && sheet">
      <div class="d-flex align-items-center gap-2 mb-1">
        <h6 class="mb-0">{{ sheet.name }}</h6>
        <span class="small text-body-secondary">{{ $t('GraphImport.understand.rows', { count: sheet.row_count ?? 0 }) }}</span>
      </div>
      <p v-for="(note, index) in sheet.notes ?? []" :key="'note' + index" class="small text-warning-emphasis mb-1">
        {{ reportText($opensilex, note) }}
      </p>
      <template v-if="selected.kind === 'bundle'">
        <p class="small">{{ $t('GraphImport.understandGraph.bundleHelp') }}</p>
        <n-button size="small" class="mb-2" @click="emit('unfold', sheet.name ?? '')">{{ $t('GraphImport.understandGraph.unfold') }}</n-button>
      </template>
      <template v-else-if="sheet.declared">
        <n-tag size="small" type="info" :bordered="false" class="mb-2">{{ $t('GraphImport.declared.tag') }}</n-tag>
        <div v-for="id in sheet.declared_ids ?? []" :key="id" class="d-flex align-items-center gap-2 mb-1">
          <span>{{ declaredName(declaredById(id)) }}</span>
          <n-button size="tiny" tertiary :disabled="busy" @click="emit('editDeclared', id)">{{ $t('GraphImport.declared.edit') }}</n-button>
        </div>
      </template>
      <template v-else-if="sheet.columns?.length">
        <p v-if="decidedColumns" class="small text-warning-emphasis mb-1">{{ $t('GraphImport.understandGraph.classChangeWarning') }}</p>
        <SheetChoices class="mb-2" :sheet="sheet" :classes="interpretation?.classes ?? []" @decide="onSheetChoicesDecide" />
        <MissingValues
          class="mb-2"
          :sheet="sheet"
          :validation="validation"
          :classes="interpretation?.classes ?? []"
          :busy="busy"
          @decide="onSheetChoicesDecide"
          @declare="(classUri, name) => emit('declare', classUri, name)"
        />
        <n-space :size="6" class="mb-2">
          <n-button v-if="hasSuggestions" size="small" type="primary" secondary @click="onAcceptAllClick">
            {{ $t('GraphImport.understandGraph.acceptAll') }}
          </n-button>
          <n-button v-if="hasDecisions" size="small" tertiary @click="emit('forget', sheet.name ?? '')">
            {{ $t('GraphImport.understand.forget') }}
          </n-button>
        </n-space>
      </template>
      <div class="small fw-semibold mb-1">{{ $t('GraphImport.understandGraph.columns') }}</div>
      <div class="graph-import-inspector__columns mb-2">
        <n-button
          v-for="listed in listedColumns"
          :key="listed.index"
          size="tiny"
          :type="listed.property?.status === 'UNDECIDED' && !listed.type_column ? 'warning' : 'default'"
          secondary
          @click="emit('select', { kind: 'column', sheet: sheet.name ?? '', column: listed.index ?? 0 })"
        >
          {{ listed.header }}
        </n-button>
      </div>
      <n-collapse>
        <n-collapse-item :title="$t('GraphImport.understand.why')" name="why">
          <ReasonList :choice="sheet.sheet_class" :title="$t('GraphImport.understand.class')" />
          <ReasonList :choice="sheet.identifier" :title="$t('GraphImport.understand.identifier')" />
        </n-collapse-item>
        <n-collapse-item v-if="rowCheck.counts.length" :title="$t('GraphImport.check.rowProblems', { count: rowCount })" name="rows">
          <ProblemList :check="rowCheck" :headers="headers" />
        </n-collapse-item>
      </n-collapse>
    </template>

    <template v-else-if="selected?.kind === 'existing'">
      <h6>{{ existingNode?.label || $t('GraphImport.understandGraph.existing') }}</h6>
      <p class="small">{{ $t('GraphImport.understandGraph.existingHelp') }}</p>
      <div class="graph-import-inspector__columns">
        <n-button
          v-for="edge in edgesToExisting"
          :key="edge.id"
          size="tiny"
          secondary
          @click="emit('select', { kind: 'column', sheet: edge.sheet ?? '', column: edge.column ?? 0 })"
        >
          {{ edge.sheet }} · {{ headerOf(edge.sheet, edge.column) }}
        </n-button>
      </div>
    </template>

    <template v-else-if="selected?.kind === 'shared'">
      <h6>{{ $t('GraphImport.understandGraph.sharedTitle') }}</h6>
      <p class="small">{{ $t('GraphImport.understandGraph.sharedHelp', { count: sharedCount, from: selected.from, to: selected.to }) }}</p>
    </template>

    <p v-else class="small text-body-secondary">{{ $t('GraphImport.understandGraph.inspectorHelp') }}</p>
  </div>
</template>

<script setup lang="ts">
import { computed, inject } from 'vue'
import type {
  ColumnInterpretationDTO,
  DeclaredResourceDTO,
  InterpretationDTO,
  SheetInterpretationDTO,
  ValidationDTO
} from '../lib'
import type { OpenSilexPlugin } from '../opensilexPlugin'
import {
  acceptSheetSuggestions,
  decidedSoFar,
  hasDecisions as sheetHasDecisions,
  isDataSheet,
  leftOut,
  type SheetDecision,
  withColumn
} from '../decisions'
import { columnTitle, declaredName } from '../declared'
import type { PendingDrop } from '../dropTargets'
import { type DateReading, ofColumn, type SheetCheck, sheetCheck, tally } from '../problems'
import { percent, reportText } from '../reasons'
import { existingNodeId, linksOfColumn, shortName, type Subject, type UnderstandEdge, type UnderstandGraph, type UnderstandNode } from '../understandGraph'
import ChoiceStatusTag from './ChoiceStatusTag.vue'
import ColumnEvidence from './ColumnEvidence.vue'
import DateReadingForm from './DateReadingForm.vue'
import MeasureVariable from './MeasureVariable.vue'
import MissingValues from './MissingValues.vue'
import ProblemList from './ProblemList.vue'
import ReasonList from './ReasonList.vue'
import SheetChoices from './SheetChoices.vue'

/** One line saying where some cells of a link column land. */
interface LandingLine {
  text: string
  unresolved: boolean
}

//#region Public
const emit = defineEmits<{
  /** The whole decision about a sheet, which replaces the previous one. */
  decide: [sheet: string, decision: SheetDecision]
  /** Give a whole sheet back to the suggestions. */
  forget: [sheet: string]
  /** How the dates of a column are written. */
  read: [sheet: string, column: number, reading: DateReading]
  select: [subject: Subject]
  /** Draw the columns of a sheet one by one. */
  unfold: [sheet: string]
  /** The property picked for the pending drop. */
  choose: [property: string]
  /** Abandon the pending drop. */
  cancel: []
  /** Declare a resource of this class under this name, since nothing of that name exists. */
  declare: [classUri: string, name: string]
  /** Open a declared resource to change it. */
  editDeclared: [id: string]
}>()

const props = defineProps<{
  interpretation?: InterpretationDTO
  validation?: ValidationDTO
  graph: UnderstandGraph
  selected?: Subject
  pending?: PendingDrop
  /** The name of the node the selected column was just dropped on. */
  droppedOn?: string
  /** Whether a request is running. */
  busy: boolean
}>()
//#endregion

//#region Private
//#region Plugins & services
const $opensilex = inject<OpenSilexPlugin>('$opensilex')!
//#endregion

//#region Data & Computed
const sheet = computed<SheetInterpretationDTO | undefined>(() => {
  const name = props.pending?.sheet ?? (props.selected && 'sheet' in props.selected ? props.selected.sheet : undefined)
  return (props.interpretation?.sheets ?? []).find((candidate) => candidate.name === name)
})

const column = computed<ColumnInterpretationDTO | undefined>(() =>
  props.selected?.kind === 'column' ? columnOf(props.selected.column) : undefined
)

const pendingColumn = computed<ColumnInterpretationDTO | undefined>(() => (props.pending ? columnOf(props.pending.column) : undefined))

const headers = computed<string[]>(() => (sheet.value?.columns ?? []).map((listed) => listed.header ?? ''))

const check = computed<SheetCheck>(() => sheetCheck(props.validation, sheet.value?.name ?? ''))

const columnCheck = computed<SheetCheck>(() => ofColumn(check.value, column.value?.index))

const rowCheck = computed<SheetCheck>(() => ofColumn(check.value, undefined))

const rowCount = computed(() => {
  const counted = tally(rowCheck.value.counts)
  return counted.errors + counted.warnings
})

const landing = computed<LandingLine[]>(() =>
  linksOfColumn(props.validation, sheet.value?.name ?? '', column.value?.index, column.value?.property?.chosen_uri).map((link) => {
    const count = link.count ?? 0
    if (link.kind === 'ROW') {
      return { text: $opensilex.$i18n.t('GraphImport.understandGraph.landsOnRows', { count, sheet: link.to_sheet ?? '' }), unresolved: false }
    }
    if (link.kind === 'INSTANCE') {
      const name = link.to_class_label ?? (link.to_class ? shortName(link.to_class) : '')
      return { text: $opensilex.$i18n.t('GraphImport.understandGraph.landsOnExisting', { count, class: name }), unresolved: false }
    }
    return { text: $opensilex.$i18n.t('GraphImport.understandGraph.unresolved', { count }), unresolved: true }
  })
)

/** The columns listed in the sheet's panel; those a bundle folds when a bundle is selected. */
const listedColumns = computed<ColumnInterpretationDTO[]>(() => {
  const all = sheet.value?.columns ?? []
  if (props.selected?.kind !== 'bundle') {
    return all
  }
  const folded = props.graph.nodes.find((node) => node.kind === 'bundle' && node.sheet === sheet.value?.name)?.columns ?? []
  return all.filter((listed) => folded.includes(listed.index ?? -1))
})

const decidedColumns = computed(() => (sheet.value ? decidedSoFar(sheet.value).columns.length > 0 : false))

const hasDecisions = computed(() => (sheet.value ? sheetHasDecisions(sheet.value) : false))

const hasSuggestions = computed(
  () =>
    sheet.value?.sheet_class?.status === 'SUGGESTED' ||
    sheet.value?.identifier?.status === 'SUGGESTED' ||
    (sheet.value?.columns ?? []).some((listed) => !listed.type_column && listed.property?.status === 'SUGGESTED')
)

const existingNode = computed<UnderstandNode | undefined>(() => {
  const selected = props.selected
  return selected?.kind === 'existing' ? props.graph.nodes.find((node) => node.id === existingNodeId(selected.classUri)) : undefined
})

const edgesToExisting = computed<UnderstandEdge[]>(() =>
  existingNode.value ? props.graph.edges.filter((edge) => edge.to === existingNode.value!.id) : []
)

const sharedCount = computed(
  () =>
    (props.validation?.shared_resources ?? []).find(
      (shared) => props.selected?.kind === 'shared' && shared.from === props.selected.from && shared.to === props.selected.to
    )?.count ?? 0
)
//#endregion

//#region Event handlers
function onColumnEvidenceChoose(property: string | undefined) {
  emit('decide', sheet.value!.name!, withColumn(sheet.value!, column.value!.index!, property))
}

function onColumnEvidenceLeaveOut() {
  emit('decide', sheet.value!.name!, leftOut(sheet.value!, column.value!.index!))
}

function onDateReadingFormRead(reading: DateReading) {
  emit('read', sheet.value!.name!, column.value!.index!, reading)
}

function onSheetChoicesDecide(decision: SheetDecision) {
  emit('decide', sheet.value!.name!, decision)
}

function onAcceptAllClick() {
  emit('decide', sheet.value!.name!, acceptSheetSuggestions(sheet.value!))
}
//#endregion

//#region Methods
function columnOf(index: number): ColumnInterpretationDTO | undefined {
  return (sheet.value?.columns ?? []).find((candidate) => candidate.index === index)
}

function declaredById(id: string): DeclaredResourceDTO | undefined {
  return (props.interpretation?.declared ?? []).find((resource) => resource.id === id)
}

function headerOf(sheetName: string | undefined, index: number | undefined): string {
  const owner = (props.interpretation?.sheets ?? []).find((candidate) => candidate.name === sheetName)
  return (owner?.columns ?? []).find((candidate) => candidate.index === index)?.header ?? ''
}
//#endregion
//#endregion
</script>

<style scoped lang="scss">
.graph-import-inspector {
  padding: 0.75rem;
}

.graph-import-inspector__columns {
  display: flex;
  flex-wrap: wrap;
  gap: 0.25rem;
}
</style>
