<template>
  <div class="graph-import-graph">
    <GraphToolbar
      v-model:filter="filter"
      v-model:show-left-out="showLeftOut"
      :undecided="counts.undecided"
      :problems="counts.problems"
      @zoom-in="zoom(1 / ZOOM_STEP)"
      @zoom-out="zoom(ZOOM_STEP)"
      @fit="view = undefined"
    />

    <div v-if="undo" class="graph-import-graph__undo small mt-2">
      {{ $t('GraphImport.understandGraph.leftOutDone', { column: undo.header }) }}
      <n-button size="tiny" tertiary type="primary" :disabled="locked" @click="onUndoClick">
        {{ $t('GraphImport.understandGraph.undo') }}
      </n-button>
    </div>

    <div class="graph-import-graph__body mt-2">
      <div class="graph-import-graph__canvas">
        <GraphCanvas
          v-model:view="view"
          :graph="graph"
          :layout="placed"
          :label-text="labelText"
          :selected="selected"
          :highlight="highlight"
          :targets="targetIds"
          :busy="locked"
          @select="onGraphCanvasSelect"
          @accept="onGraphCanvasAccept"
          @move="onGraphCanvasMove"
          @moved="onGraphCanvasMoved"
          @drag-start="onGraphCanvasDragStart"
          @drop="onGraphCanvasDrop"
          @cancel="dragging = undefined"
          @remove="onGraphCanvasRemove"
        />
      </div>
      <div class="graph-import-graph__inspector">
        <GraphInspector
          :interpretation="interpretation"
          :validation="validation"
          :graph="graph"
          :selected="selected"
          :pending="pending"
          :dropped-on="droppedOn"
          :busy="locked"
          @decide="onGraphInspectorDecide"
          @forget="onGraphInspectorForget"
          @read="onGraphInspectorRead"
          @select="onGraphInspectorSelect"
          @unfold="onGraphInspectorUnfold"
          @choose="onGraphInspectorChoose"
          @cancel="pending = undefined"
          @declare="onGraphInspectorDeclare"
          @edit-declared="(id) => emit('editDeclared', id)"
        />
      </div>
      <div v-if="locked" class="graph-import-graph__veil" :aria-label="$t('GraphImport.understandGraph.busy')" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, inject, ref, watch } from 'vue'
import type { InterpretationDTO, SheetInterpretationDTO, ValidationDTO } from '../lib'
import type { OpenSilexPlugin } from '../opensilexPlugin'
import { decidedSoFar, leftOut, type SheetDecision, withColumn } from '../decisions'
import { type DropTarget, type PendingDrop, targetsFor } from '../dropTargets'
import { type GraphFilter, readPositions, writePositions } from '../graphViewState'
import type { DateReading } from '../problems'
import {
  buildUnderstandGraph,
  counters,
  hasProblems,
  isUndecided,
  type Subject,
  type UnderstandEdge,
  type UnderstandGraph
} from '../understandGraph'
import { type Box, type GraphLayout, layout, type Point } from '../understandLayout'
import GraphCanvas from './GraphCanvas.vue'
import GraphInspector from './GraphInspector.vue'
import GraphToolbar from './GraphToolbar.vue'

/** A column being dragged, and where it may land. */
interface Dragging {
  sheet: string
  column: number
  targets: DropTarget[]
}

/** A column just left out, and what it held, so that the user can take it back at once. */
interface Undo {
  sheet: string
  column: number
  header: string
  /** The property the user had chosen; undefined gives the column back to the suggestions. */
  property?: string
}

/** The size the canvas is laid out for; the drawing scales to the room it has. */
const CANVAS_SIZE = { width: 900, height: 640 }
const ZOOM_STEP = 1.25

//#region Public
const emit = defineEmits<{
  /** The whole decision about a sheet, which replaces the previous one. */
  decide: [sheet: string, decision: SheetDecision]
  /** Give a whole sheet back to the suggestions. */
  forget: [sheet: string]
  /** How the dates of a column are written. */
  read: [sheet: string, column: number, reading: DateReading]
  /** Declare a resource of this class under this name, since nothing of that name exists. */
  declare: [classUri: string, name: string]
  /** Open a declared resource to change it. */
  editDeclared: [id: string]
}>()

const props = defineProps<{
  sessionId: string
  interpretation?: InterpretationDTO
  validation?: ValidationDTO
  /** Whether a request is running: no gesture asking for another is accepted until it answers. */
  busy: boolean
}>()
//#endregion

//#region Private
//#region Plugins & services
const $opensilex = inject<OpenSilexPlugin>('$opensilex')!
//#endregion

//#region Data & Computed
const selected = ref<Subject>()
const filter = ref<GraphFilter>()
const showLeftOut = ref(false)
/** The sheets whose many columns of values are drawn one by one. */
const expanded = ref(new Set<string>())
/** Where the user put nodes, remembered by this browser for this import. */
const positions = ref(readPositions(props.sessionId))
/** The part of the canvas shown; undefined shows the whole graph. */
const view = ref<Box>()
const dragging = ref<Dragging>()
const pending = ref<PendingDrop>()
/** The name of the node the selected column was just dropped on, said in the inspector. */
const droppedOn = ref<string>()
const undo = ref<Undo>()
/**
 * Whether a request was sent and its answer not yet shown. The parent's busy flag reaches this
 * component only after it redraws: two gestures in a row would otherwise both be sent.
 */
const requested = ref(false)
const locked = computed(() => props.busy || requested.value)

const graph = computed<UnderstandGraph>(() =>
  buildUnderstandGraph(props.interpretation, props.validation, {
    showLeftOut: showLeftOut.value,
    expanded: expanded.value,
    extraExisting: dragging.value?.targets.map((target) => target.classUri).filter((classUri): classUri is string => !!classUri)
  })
)

const placed = computed<GraphLayout>(() => layout(graph.value, positions.value, CANVAS_SIZE, labelText))

const counts = computed(() => counters(graph.value))

const highlight = computed<Set<string> | undefined>(() => {
  if (!filter.value) {
    return undefined
  }
  const lit = filter.value === 'undecided' ? isUndecided : hasProblems
  return new Set([...graph.value.nodes, ...graph.value.edges].filter(lit).map((element) => element.id))
})

const targetIds = computed<Set<string> | undefined>(() =>
  dragging.value ? new Set(dragging.value.targets.map((target) => target.nodeId)) : undefined
)
//#endregion

//#region Hooks & watchers
watch(
  () => props.sessionId,
  (sessionId) => {
    positions.value = readPositions(sessionId)
    selected.value = undefined
    view.value = undefined
  }
)

watch(
  () => props.busy,
  (busy) => {
    if (!busy) {
      requested.value = false
    }
  }
)

// A drag or a choice started on the former interpretation would decide from stale facts.
watch(
  () => props.interpretation,
  () => {
    dragging.value = undefined
    pending.value = undefined
  }
)
//#endregion

//#region Event handlers
function onGraphCanvasSelect(subject: Subject | undefined) {
  selected.value = subject
  pending.value = undefined
  droppedOn.value = undefined
}

function onGraphCanvasAccept(edge: UnderstandEdge) {
  const sheet = sheetOf(edge.sheet)
  if (locked.value || !sheet || edge.column === undefined || !edge.property) {
    return
  }
  decide(sheet, withColumn(sheet, edge.column, edge.property.uri))
}

function onGraphCanvasMove(id: string, point: Point) {
  positions.value = new Map(positions.value).set(id, point)
}

function onGraphCanvasMoved() {
  writePositions(props.sessionId, positions.value)
}

function onGraphCanvasDragStart(subject: Subject) {
  if (locked.value || subject.kind !== 'column') {
    return
  }
  // The nodes drawn for the drag would reframe a view that shows everything: it stays where it is.
  view.value = view.value ?? placed.value.viewBox
  dragging.value = { sheet: subject.sheet, column: subject.column, targets: targetsFor(props.interpretation, subject.sheet, subject.column) }
}

function onGraphCanvasDrop(subject: Subject, nodeId: string) {
  const target = dragging.value?.targets.find((candidate) => candidate.nodeId === nodeId)
  dragging.value = undefined
  if (locked.value || !target || subject.kind !== 'column') {
    return
  }
  const node = graph.value.nodes.find((candidate) => candidate.id === nodeId)
  const targetLabel = node?.label || $opensilex.$i18n.t('GraphImport.understandGraph.existing')
  selected.value = subject
  if (target.properties.length === 1) {
    linkTo(subject.sheet, subject.column, target.properties[0].uri, targetLabel)
  } else {
    pending.value = { sheet: subject.sheet, column: subject.column, targetLabel, properties: target.properties }
  }
}

function onGraphCanvasRemove() {
  const subject = selected.value
  const sheet = subject?.kind === 'column' ? sheetOf(subject.sheet) : undefined
  const column = sheet && subject?.kind === 'column' ? sheet.columns?.find((candidate) => candidate.index === subject.column) : undefined
  if (locked.value || !sheet || sheet.declared || !column || column.type_column
    || column.property?.status === 'LEFT_OUT') {
    return
  }
  decide(sheet, leftOut(sheet, column.index!))
}

function onGraphInspectorDecide(sheetName: string, decision: SheetDecision) {
  const sheet = sheetOf(sheetName)
  if (!locked.value && sheet) {
    decide(sheet, decision)
  }
}

function onGraphInspectorForget(sheet: string) {
  if (!locked.value) {
    undo.value = undefined
    requested.value = true
    emit('forget', sheet)
  }
}

function onGraphInspectorRead(sheet: string, column: number, reading: DateReading) {
  if (!locked.value) {
    requested.value = true
    emit('read', sheet, column, reading)
  }
}

function onGraphInspectorSelect(subject: Subject) {
  onGraphCanvasSelect(subject)
}

function onGraphInspectorDeclare(classUri: string, name: string) {
  if (!locked.value) {
    requested.value = true
    emit('declare', classUri, name)
  }
}

function onGraphInspectorUnfold(sheet: string) {
  expanded.value = new Set(expanded.value).add(sheet)
  selected.value = { kind: 'sheet', sheet }
}

function onGraphInspectorChoose(property: string) {
  const choice = pending.value
  pending.value = undefined
  if (!locked.value && choice) {
    linkTo(choice.sheet, choice.column, property, choice.targetLabel)
  }
}

function onUndoClick() {
  const taken = undo.value
  const sheet = taken ? sheetOf(taken.sheet) : undefined
  if (locked.value || !taken || !sheet) {
    return
  }
  undo.value = undefined
  requested.value = true
  emit('decide', taken.sheet, withColumn(sheet, taken.column, taken.property))
}
//#endregion

//#region Methods
function sheetOf(name: string | undefined): SheetInterpretationDTO | undefined {
  return (props.interpretation?.sheets ?? []).find((sheet) => sheet.name === name)
}

/** Sends a column's link to a node, then says in the inspector where its cells landed. */
function linkTo(sheetName: string, column: number, property: string, targetLabel: string) {
  const sheet = sheetOf(sheetName)
  if (!sheet) {
    return
  }
  decide(sheet, withColumn(sheet, column, property))
  droppedOn.value = targetLabel
}

/**
 * Sends a decision about a sheet. A column it leaves out can be taken back at once: what the column
 * held is kept for the "undo" message.
 */
function decide(sheet: SheetInterpretationDTO, decision: SheetDecision) {
  const before = decidedSoFar(sheet)
  const added = decision.left_out.find((column) => !before.left_out.includes(column))
  if (added === undefined) {
    undo.value = undefined
  } else {
    const column = sheet.columns?.find((candidate) => candidate.index === added)
    undo.value = {
      sheet: sheet.name ?? '',
      column: added,
      header: column?.header ?? String(added + 1),
      property: column?.property?.status === 'CHOSEN' ? column.property.chosen_uri : undefined
    }
  }
  requested.value = true
  emit('decide', sheet.name ?? '', decision)
}

/** The text of an edge's label: its property, or how many resources two sheets share. */
function labelText(edge: UnderstandEdge): string {
  if (edge.kind === 'shared') {
    return $opensilex.$i18n.t('GraphImport.understandGraph.sharedLabel', { count: edge.count ?? 0 })
  }
  return edge.property?.label ?? ''
}

/** Zooms the view around its middle. */
function zoom(factor: number) {
  const current = view.value ?? placed.value.viewBox
  const centre = { x: current.x + current.width / 2, y: current.y + current.height / 2 }
  view.value = {
    x: centre.x - (current.width * factor) / 2,
    y: centre.y - (current.height * factor) / 2,
    width: current.width * factor,
    height: current.height * factor
  }
}
//#endregion
//#endregion
</script>

<style scoped lang="scss">
.graph-import-graph__body {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 0.75rem;
}

@media (min-width: 1200px) {
  .graph-import-graph__body {
    grid-template-columns: minmax(0, 1fr) 24rem;
  }
}

.graph-import-graph__canvas {
  height: 640px;
  border: 1px solid var(--bs-border-color, #dee2e6);
  border-radius: 0.5rem;
  background: var(--bs-body-bg, #fff);
  overflow: hidden;
}

.graph-import-graph__inspector {
  border: 1px solid var(--bs-border-color, #dee2e6);
  border-radius: 0.5rem;
  max-height: 640px;
  overflow-y: auto;
}

.graph-import-graph__veil {
  position: absolute;
  inset: 0;
  background: color-mix(in srgb, var(--bs-body-bg, #fff) 45%, transparent);
  cursor: progress;
  border-radius: 0.5rem;
}

.graph-import-graph__undo {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
</style>
