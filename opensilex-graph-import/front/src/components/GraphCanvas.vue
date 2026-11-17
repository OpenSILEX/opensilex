<template>
  <svg
    ref="svg"
    class="graph-import-canvas"
    :class="{ 'graph-import-canvas--dragging': !!drag?.linking }"
    :viewBox="viewBoxText"
    preserveAspectRatio="xMidYMid meet"
    role="group"
    :aria-label="$t('GraphImport.understandGraph.canvasLabel')"
    tabindex="0"
    @wheel.prevent="onWheel"
    @pointerdown="onBackgroundPointerDown"
    @pointermove="onPointerMove"
    @pointerup="onPointerUp"
    @pointercancel="onPointerCancel"
    @keydown="onKeydown"
  >
    <defs>
      <marker :id="markerId" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse">
        <path d="M0,0 L10,5 L0,10 z" class="graph-import-canvas__arrow" />
      </marker>
      <marker :id="markerId + '-error'" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse">
        <path d="M0,0 L10,5 L0,10 z" class="graph-import-canvas__arrow graph-import-canvas__arrow--error" />
      </marker>
    </defs>
    <rect class="graph-import-canvas__background" :x="view.x" :y="view.y" :width="view.width" :height="view.height" />

    <g v-for="edge in drawnEdges" :key="edge.edge.id" :class="edgeClass(edge.edge)">
      <path
        :d="edge.placed.path"
        class="graph-import-canvas__edge-line"
        :marker-end="edge.edge.kind === 'link' || edge.edge.kind === 'loop' ? `url(#${markerId}${hasProblems(edge.edge) ? '-error' : ''})` : undefined"
        @pointerdown.stop="(event) => onElementPointerDown(event, edge.edge)"
      />
      <template v-if="edge.placed.stub">
        <line
          class="graph-import-canvas__stub"
          :x1="edge.placed.stub.from.x"
          :y1="edge.placed.stub.from.y"
          :x2="edge.placed.stub.to.x"
          :y2="edge.placed.stub.to.y"
        />
        <text class="graph-import-canvas__stub-text" :x="edge.placed.stub.to.x" :y="edge.placed.stub.to.y + 12" text-anchor="middle">
          {{ $t('GraphImport.understandGraph.unresolved', { count: edge.edge.unresolved ?? 0 }) }}
        </text>
      </template>
      <g
        v-if="labelText(edge.edge)"
        class="graph-import-canvas__label"
        tabindex="0"
        role="button"
        :aria-label="labelText(edge.edge)"
        @pointerdown.stop="(event) => onElementPointerDown(event, edge.edge)"
        @keydown.enter.stop="emit('select', subjectOf(edge.edge))"
      >
        <title>{{ labelText(edge.edge) }}</title>
        <rect :x="edge.placed.label.x" :y="edge.placed.label.y" :width="edge.placed.label.width" :height="edge.placed.label.height" rx="4" />
        <text :x="edge.placed.label.x + edge.placed.label.width / 2" :y="edge.placed.label.y + 12" text-anchor="middle">
          {{ labelText(edge.edge) }}
        </text>
      </g>
      <g
        v-if="edge.edge.status === 'SUGGESTED' && (edge.edge.kind === 'link' || edge.edge.kind === 'loop' || edge.edge.kind === 'value')"
        class="graph-import-canvas__accept"
        tabindex="0"
        role="button"
        :aria-label="$t('GraphImport.understandGraph.acceptSuggestion', { property: edge.edge.property?.label ?? '' })"
        @pointerdown.stop
        @click.stop="emit('accept', edge.edge)"
        @keydown.enter.stop="emit('accept', edge.edge)"
      >
        <title>{{ $t('GraphImport.understandGraph.acceptSuggestion', { property: edge.edge.property?.label ?? '' }) }}</title>
        <circle :cx="edge.placed.label.x + edge.placed.label.width + 10" :cy="edge.placed.label.y + 8" r="8" />
        <text :x="edge.placed.label.x + edge.placed.label.width + 10" :y="edge.placed.label.y + 12" text-anchor="middle">✓</text>
      </g>
      <circle
        v-if="edge.edge.kind === 'link' || edge.edge.kind === 'loop'"
        class="graph-import-canvas__handle"
        :cx="edge.placed.end.x"
        :cy="edge.placed.end.y"
        r="6"
        @pointerdown.stop="(event) => onElementPointerDown(event, edge.edge, true)"
      >
        <title>{{ $t('GraphImport.understandGraph.dragLink') }}</title>
      </circle>
    </g>

    <line
      v-if="drag?.linking && drag.pointer"
      class="graph-import-canvas__ghost"
      :x1="drag.origin.x"
      :y1="drag.origin.y"
      :x2="drag.pointer.x"
      :y2="drag.pointer.y"
    />

    <g
      v-for="node in drawnNodes"
      :key="node.node.id"
      :class="nodeClass(node.node)"
      :transform="`translate(${node.placed.x},${node.placed.y})`"
      tabindex="0"
      role="button"
      :aria-label="nodeName(node.node)"
      @pointerdown.stop="(event) => onElementPointerDown(event, node.node)"
      @keydown.enter.stop="emit('select', subjectOf(node.node))"
    >
      <title>{{ nodeName(node.node) }}</title>
      <circle :r="node.placed.r" class="graph-import-canvas__node-shape" />
      <template v-if="node.node.kind === 'sheet'">
        <text y="-8" text-anchor="middle" class="graph-import-canvas__name">{{ clip(node.node.label, 12) }}</text>
        <text y="6" text-anchor="middle" class="graph-import-canvas__small">
          {{ node.node.property ? clip(node.node.property.label, 14) : $t('GraphImport.understandGraph.noClass') }}
        </text>
        <text v-if="node.node.identifier" y="19" text-anchor="middle" class="graph-import-canvas__small">
          {{ $t('GraphImport.understandGraph.key', { column: clip(node.node.identifier, 10) }) }}
        </text>
        <text :y="node.placed.r + 14" text-anchor="middle" class="graph-import-canvas__under">
          {{ $t('GraphImport.understand.rows', { count: node.node.rowCount ?? 0 }) }}
        </text>
      </template>
      <template v-else-if="node.node.kind === 'existing'">
        <text y="-2" text-anchor="middle" class="graph-import-canvas__name">{{ clip(node.node.label || '?', 12) }}</text>
        <text y="12" text-anchor="middle" class="graph-import-canvas__small">{{ $t('GraphImport.understandGraph.existing') }}</text>
      </template>
      <template v-else>
        <text y="4" text-anchor="middle" class="graph-import-canvas__glyph">{{ glyph(node.node) }}</text>
        <text v-bind="besideAttributes(node.placed)" class="graph-import-canvas__under">{{ clip(columnText(node.node), COLUMN_NAME_LENGTH) }}</text>
      </template>
      <g v-if="node.node.tally.errors + node.node.tally.warnings > 0" class="graph-import-canvas__badge" :class="{ 'graph-import-canvas__badge--warning': !node.node.tally.errors }">
        <circle :cx="node.placed.r * 0.72" :cy="-node.placed.r * 0.72" r="9" />
        <text :x="node.placed.r * 0.72" :y="-node.placed.r * 0.72 + 4" text-anchor="middle">
          {{ badge(node.node.tally.errors || node.node.tally.warnings) }}
        </text>
      </g>
    </g>
  </svg>
</template>

<script setup lang="ts">
import { computed, inject, ref, useId, useTemplateRef } from 'vue'
import type { OpenSilexPlugin } from '../opensilexPlugin'
import {
  hasProblems,
  sameSubject,
  subjectOf,
  type Subject,
  type UnderstandEdge,
  type UnderstandGraph,
  type UnderstandNode
} from '../understandGraph'
import { type Box, COLUMN_NAME_LENGTH, type GraphLayout, type LabelText, type PlacedEdge, type PlacedNode, type Point } from '../understandLayout'

/** A press on the canvas, until it is released: a click, a pan, a node moved or a column dragged. */
interface Press {
  pointerId: number
  start: Point
  /** The node or edge pressed; none for the background. */
  element?: UnderstandNode | UnderstandEdge
  /** Whether the press moved far enough to be a drag rather than a click. */
  moved: boolean
  /** Whether it drags a column towards a node, rather than moving a node. */
  linking: boolean
  /** Where a column dragged comes from, or where the node moved was. */
  origin: Point
  /** How far from the node's centre it was grabbed, in the canvas's units, so that it does not jump. */
  grab: Point
  pointer?: Point
  /** The view when a pan started. */
  view?: Box
}

/** How far a press moves, in screen pixels, before it is a drag. */
const DRAG_THRESHOLD = 4
/** How much one step of the wheel or of the keyboard zooms. */
const ZOOM_STEP = 1.2
/** How far around a node, in the canvas's units, a drop still lands on it. */
const DROP_SLACK = 8

//#region Public
const emit = defineEmits<{
  /** What the user clicked; undefined for the background. */
  select: [subject: Subject | undefined]
  /** Accept the suggested property an edge shows. */
  accept: [edge: UnderstandEdge]
  /** A node moved to a point, while it is dragged. */
  move: [id: string, point: Point]
  /** A node was released where it was moved. */
  moved: []
  /** A column starts being dragged towards a node. */
  dragStart: [subject: Subject]
  /** A column dragged was released on a node it may link to. */
  drop: [subject: Subject, nodeId: string]
  /** A column dragged was released elsewhere, or the drag was abandoned. */
  cancel: []
  /** The user asked to leave out what is selected. */
  remove: []
}>()

const props = defineProps<{
  graph: UnderstandGraph
  layout: GraphLayout
  /** The text of each edge's label, the one the layout measured. */
  labelText: LabelText
  selected?: Subject
  /** The ids of the elements to bring forward; the others fade. Undefined brings none forward. */
  highlight?: Set<string>
  /** While a column is dragged, the ids of the nodes it may be dropped on. */
  targets?: Set<string>
  /** Whether a request is running: nothing that asks for one is accepted. */
  busy: boolean
}>()

/** The part of the canvas shown; undefined shows the whole graph. */
const viewModel = defineModel<Box | undefined>('view')
//#endregion

//#region Private
//#region Plugins & services
const $opensilex = inject<OpenSilexPlugin>('$opensilex')!
//#endregion

//#region Template refs
const svgRef = useTemplateRef<SVGSVGElement>('svg')
//#endregion

//#region Data & Computed
const markerId = 'graph-import-arrow-' + useId()

const drag = ref<Press>()

const view = computed<Box>(() => viewModel.value ?? props.layout.viewBox)

const viewBoxText = computed(() => `${view.value.x} ${view.value.y} ${view.value.width} ${view.value.height}`)

const drawnEdges = computed<{ edge: UnderstandEdge; placed: PlacedEdge }[]>(() =>
  props.graph.edges
    .map((edge) => ({ edge, placed: props.layout.edges.get(edge.id)! }))
    .filter((drawn) => !!drawn.placed)
)

const drawnNodes = computed<{ node: UnderstandNode; placed: PlacedNode }[]>(() =>
  props.graph.nodes
    .map((node) => ({ node, placed: props.layout.nodes.get(node.id)! }))
    .filter((drawn) => !!drawn.placed)
)
//#endregion

//#region Event handlers
function onWheel(event: WheelEvent) {
  zoomAt(event.deltaY > 0 ? ZOOM_STEP : 1 / ZOOM_STEP, toCanvas(event))
}

function onBackgroundPointerDown(event: PointerEvent) {
  press(event, undefined, false)
}

function onElementPointerDown(event: PointerEvent, element: UnderstandNode | UnderstandEdge, handle = false) {
  const subject = subjectOf(element)
  const linking = subject.kind === 'column' && (element.kind === 'column' || handle)
  press(event, element, linking)
}

function onPointerMove(event: PointerEvent) {
  const current = drag.value
  if (!current || current.pointerId !== event.pointerId) {
    return
  }
  if (!current.moved && Math.hypot(event.clientX - current.start.x, event.clientY - current.start.y) < DRAG_THRESHOLD) {
    return
  }
  if (!current.moved) {
    current.moved = true
    if (current.linking) {
      if (props.busy) {
        drag.value = undefined
        return
      }
      emit('dragStart', subjectOf(current.element!))
    }
  }
  const point = toCanvas(event)
  if (!point) {
    return
  }
  if (!current.element) {
    pan(current, event)
  } else if (current.linking) {
    current.pointer = point
  } else if (props.layout.nodes.has(current.element.id)) {
    emit('move', current.element.id, { x: point.x + current.grab.x, y: point.y + current.grab.y })
  }
}

function onPointerUp(event: PointerEvent) {
  const current = drag.value
  if (!current || current.pointerId !== event.pointerId) {
    return
  }
  drag.value = undefined
  if (!current.moved) {
    emit('select', current.element ? subjectOf(current.element) : undefined)
  } else if (current.linking) {
    const point = toCanvas(event)
    const target = point ? nodeAt(point) : undefined
    if (target) {
      emit('drop', subjectOf(current.element!), target)
    } else {
      emit('cancel')
    }
  } else if (current.element) {
    emit('moved')
  }
}

function onPointerCancel() {
  if (drag.value?.linking && drag.value.moved) {
    emit('cancel')
  }
  drag.value = undefined
}

function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    if (drag.value?.linking && drag.value.moved) {
      emit('cancel')
    } else {
      emit('select', undefined)
    }
    drag.value = undefined
  } else if (event.key === 'Delete') {
    emit('remove')
  } else if (event.key === '+' || event.key === '=') {
    zoomAt(1 / ZOOM_STEP)
  } else if (event.key === '-') {
    zoomAt(ZOOM_STEP)
  }
}
//#endregion

//#region Methods
function press(event: PointerEvent, element: UnderstandNode | UnderstandEdge | undefined, linking: boolean) {
  if (event.button !== 0) {
    return
  }
  const origin = (element && (props.layout.nodes.get(element.id) ?? props.layout.edges.get(element.id)?.end)) || { x: 0, y: 0 }
  const pressed = toCanvas(event)
  drag.value = {
    pointerId: event.pointerId,
    start: { x: event.clientX, y: event.clientY },
    element,
    moved: false,
    linking,
    origin: { x: origin.x, y: origin.y },
    grab: pressed ? { x: origin.x - pressed.x, y: origin.y - pressed.y } : { x: 0, y: 0 },
    view: { ...view.value }
  }
  try {
    svgRef.value?.setPointerCapture(event.pointerId)
  } catch {
    // A synthetic event has no pointer to capture; the canvas still follows it.
  }
}

/** Moves the view by what the pointer travelled since the press, in the canvas's units. */
function pan(current: Press, event: PointerEvent) {
  const scale = unitsPerPixel()
  if (!scale || !current.view) {
    return
  }
  viewModel.value = {
    ...current.view,
    x: current.view.x - (event.clientX - current.start.x) * scale,
    y: current.view.y - (event.clientY - current.start.y) * scale
  }
}

/** Zooms by a factor, around a point of the canvas, or around the middle of the view. */
function zoomAt(factor: number, around?: Point) {
  const current = view.value
  const centre = around ?? { x: current.x + current.width / 2, y: current.y + current.height / 2 }
  viewModel.value = {
    x: centre.x - (centre.x - current.x) * factor,
    y: centre.y - (centre.y - current.y) * factor,
    width: current.width * factor,
    height: current.height * factor
  }
}

/** The node a drop lands on, among those the column may be dropped on. */
function nodeAt(point: Point): string | undefined {
  for (const id of props.targets ?? []) {
    const placed = props.layout.nodes.get(id)
    if (placed && Math.hypot(point.x - placed.x, point.y - placed.y) <= placed.r + DROP_SLACK) {
      return id
    }
  }
  return undefined
}

/** A point of the screen in the canvas's units; undefined while the canvas is not shown. */
function toCanvas(event: MouseEvent): Point | undefined {
  const matrix = svgRef.value?.getScreenCTM()
  if (!matrix) {
    return undefined
  }
  const point = new DOMPoint(event.clientX, event.clientY).matrixTransform(matrix.inverse())
  return { x: point.x, y: point.y }
}

function unitsPerPixel(): number | undefined {
  const matrix = svgRef.value?.getScreenCTM()
  return matrix && matrix.a ? 1 / matrix.a : undefined
}

function edgeClass(edge: UnderstandEdge): string[] {
  return [
    'graph-import-canvas__edge',
    'graph-import-canvas__edge--' + edge.kind,
    edge.status === 'SUGGESTED' ? 'graph-import-canvas__edge--suggested' : '',
    hasProblems(edge) ? 'graph-import-canvas__edge--error' : '',
    ...stateClasses(edge)
  ]
}

function nodeClass(node: UnderstandNode): string[] {
  return [
    'graph-import-canvas__node',
    'graph-import-canvas__node--' + node.kind,
    node.role ? 'graph-import-canvas__node--' + node.role : '',
    node.kind === 'sheet' ? 'graph-import-canvas__node--class-' + (node.status ?? 'UNDECIDED').toLowerCase() : '',
    node.declared ? 'graph-import-canvas__node--declared' : '',
    node.given ? 'graph-import-canvas__node--given' : '',
    props.targets?.has(node.id) ? 'graph-import-canvas__node--target' : '',
    ...stateClasses(node)
  ]
}

/** Whether an element is selected, brought forward, or faded behind others. */
function stateClasses(element: UnderstandNode | UnderstandEdge): string[] {
  const faded = props.targets ? !props.targets.has(element.id) : props.highlight ? !props.highlight.has(element.id) : false
  return [
    sameSubject(subjectOf(element), props.selected) ? 'graph-import-canvas--selected' : '',
    faded ? 'graph-import-canvas--faded' : ''
  ]
}

/** The full name of a node, for its tooltip and for screen readers. */
function nodeName(node: UnderstandNode): string {
  if (node.kind === 'sheet') {
    return [node.label, node.property?.label].filter(Boolean).join(' · ')
  }
  if (node.kind === 'existing') {
    return [node.label, '(' + $opensilex.$i18n.t('GraphImport.understandGraph.existing') + ')'].join(' ')
  }
  return columnText(node)
}

function columnText(node: UnderstandNode): string {
  return node.kind === 'bundle' ? $opensilex.$i18n.t('GraphImport.understandGraph.bundle', { count: node.columns?.length ?? 0 }) : node.label
}

/** The mark inside a column's circle. */
function glyph(node: UnderstandNode): string {
  if (node.kind === 'bundle') {
    return String(node.columns?.length ?? 0)
  }
  return node.role === 'type' ? 'T' : node.role === 'undecided' ? '?' : node.role === 'leftOut' ? '–' : ''
}

/**
 * Where a column's name is written: beyond its circle, away from its sheet, so that the names of a fan
 * of columns spread like its spokes instead of piling up under it.
 */
function besideAttributes(placed: PlacedNode): Record<string, string | number> {
  const angle = placed.angle ?? Math.PI / 2
  const cos = Math.cos(angle)
  const sin = Math.sin(angle)
  const distance = placed.r + 4
  // Text hangs from its baseline: below the circle it needs a line's height, above it a little less.
  const baseline = sin > 0.7 ? 9 : sin < -0.7 ? -1 : 4
  return {
    x: Math.round(cos * distance * 10) / 10,
    y: Math.round((sin * distance + baseline) * 10) / 10,
    'text-anchor': Math.abs(cos) < 0.3 ? 'middle' : cos > 0 ? 'start' : 'end'
  }
}

function badge(count: number): string {
  return count > 99 ? '99+' : String(count)
}

/** A text cut to a length, with an ellipsis, since a circle does not stretch. */
function clip(text: string, length: number): string {
  return text.length > length ? text.substring(0, length - 1) + '…' : text
}
//#endregion
//#endregion
</script>

<style scoped lang="scss">
.graph-import-canvas {
  display: block;
  width: 100%;
  height: 100%;
  touch-action: none;
  user-select: none;
  font-size: 12px;
  outline: none;

  &:focus-visible {
    box-shadow: 0 0 0 2px var(--bs-primary, #18a058);
  }

  [tabindex]:focus-visible {
    outline: 2px solid var(--bs-primary, #18a058);
  }
}

.graph-import-canvas__background {
  fill: transparent;
  cursor: grab;
}

.graph-import-canvas__arrow {
  fill: var(--bs-secondary-color, #6c757d);

  &--error {
    fill: var(--bs-danger, #d03050);
  }
}

.graph-import-canvas__edge-line {
  fill: none;
  stroke: var(--bs-secondary-color, #6c757d);
  stroke-width: 1.5;
}

.graph-import-canvas__edge--link .graph-import-canvas__edge-line,
.graph-import-canvas__edge--loop .graph-import-canvas__edge-line {
  stroke: var(--bs-purple, #6f42c1);
  stroke-width: 2;
  cursor: pointer;
}

.graph-import-canvas__edge--suggested .graph-import-canvas__edge-line {
  stroke-dasharray: 6 4;
}

.graph-import-canvas__edge--pending .graph-import-canvas__edge-line {
  stroke-dasharray: 3 3;
  opacity: 0.7;
}

.graph-import-canvas__edge--shared .graph-import-canvas__edge-line {
  stroke-dasharray: 1 5;
  stroke-linecap: round;
  stroke-width: 2.5;
}

.graph-import-canvas__edge--error .graph-import-canvas__edge-line {
  stroke: var(--bs-danger, #d03050);
}

.graph-import-canvas__label {
  cursor: pointer;

  rect {
    fill: var(--bs-body-bg, #fff);
    stroke: var(--bs-border-color, #dee2e6);
  }

  text {
    fill: var(--bs-body-color, #212529);
  }
}

.graph-import-canvas__edge--error .graph-import-canvas__label {
  rect {
    stroke: var(--bs-danger, #d03050);
  }

  text {
    fill: var(--bs-danger, #d03050);
  }
}

.graph-import-canvas__accept {
  cursor: pointer;

  circle {
    fill: var(--bs-success, #18a058);
  }

  text {
    fill: #fff;
    font-weight: 700;
  }
}

.graph-import-canvas__handle {
  fill: var(--bs-body-bg, #fff);
  stroke: var(--bs-purple, #6f42c1);
  stroke-width: 1.5;
  cursor: grab;
  opacity: 0;

  .graph-import-canvas__edge:hover &,
  .graph-import-canvas--selected & {
    opacity: 1;
  }
}

.graph-import-canvas__stub {
  stroke: var(--bs-danger, #d03050);
  stroke-width: 2;
}

.graph-import-canvas__stub-text {
  fill: var(--bs-danger, #d03050);
  font-size: 10px;
}

.graph-import-canvas__ghost {
  stroke: var(--bs-purple, #6f42c1);
  stroke-width: 2;
  stroke-dasharray: 4 3;
  pointer-events: none;
}

.graph-import-canvas__node {
  cursor: pointer;

  text {
    pointer-events: none;
    fill: var(--bs-body-color, #212529);
  }
}

.graph-import-canvas__node-shape {
  stroke-width: 2;
}

.graph-import-canvas__node--sheet .graph-import-canvas__node-shape {
  fill: color-mix(in srgb, var(--bs-purple, #6f42c1) 16%, var(--bs-body-bg, #fff));
  stroke: var(--bs-purple, #6f42c1);
  stroke-width: 3;
}

.graph-import-canvas__node--class-suggested .graph-import-canvas__node-shape {
  stroke-dasharray: 7 4;
}

.graph-import-canvas__node--class-undecided .graph-import-canvas__node-shape {
  stroke: var(--bs-secondary-color, #6c757d);
  fill: var(--bs-tertiary-bg, #f8f9fa);
}

.graph-import-canvas__node--declared .graph-import-canvas__node-shape {
  stroke: var(--bs-orange, #fd7e14);
  stroke-dasharray: 2 3;
}

.graph-import-canvas__node--given .graph-import-canvas__node-shape {
  stroke: var(--bs-orange, #fd7e14);
}

.graph-import-canvas__node--existing .graph-import-canvas__node-shape {
  fill: color-mix(in srgb, #f07167 18%, var(--bs-body-bg, #fff));
  stroke: #e05d53;
}

.graph-import-canvas__node--column .graph-import-canvas__node-shape,
.graph-import-canvas__node--bundle .graph-import-canvas__node-shape {
  fill: color-mix(in srgb, var(--bs-teal, #20c997) 22%, var(--bs-body-bg, #fff));
  stroke: var(--bs-teal, #20c997);
}

.graph-import-canvas__node--undecided.graph-import-canvas__node--column .graph-import-canvas__node-shape {
  fill: var(--bs-tertiary-bg, #f8f9fa);
  stroke: var(--bs-secondary-color, #6c757d);
  stroke-dasharray: 3 3;
  cursor: grab;
}

.graph-import-canvas__node--leftOut {
  opacity: 0.55;

  .graph-import-canvas__node-shape {
    fill: var(--bs-tertiary-bg, #f8f9fa);
    stroke: var(--bs-secondary-color, #6c757d);
  }

  .graph-import-canvas__under {
    text-decoration: line-through;
  }
}

.graph-import-canvas__node--target .graph-import-canvas__node-shape {
  stroke: var(--bs-success, #18a058);
  stroke-width: 4;
}

.graph-import-canvas__name {
  font-weight: 600;
}

.graph-import-canvas__small,
.graph-import-canvas__under {
  font-size: 10.5px;
}

.graph-import-canvas__under {
  fill: var(--bs-secondary-color, #6c757d) !important;
}

.graph-import-canvas__glyph {
  font-weight: 700;
}

.graph-import-canvas__badge {
  circle {
    fill: var(--bs-danger, #d03050);
  }

  text {
    fill: #fff !important;
    font-size: 9px;
    font-weight: 700;
  }

  &--warning circle {
    fill: var(--bs-warning, #f0a020);
  }
}

.graph-import-canvas--selected {
  .graph-import-canvas__node-shape {
    stroke: var(--bs-primary, #18a058);
    stroke-width: 4;
  }

  .graph-import-canvas__edge-line {
    stroke-width: 3.5;
  }

  .graph-import-canvas__label rect {
    stroke: var(--bs-primary, #18a058);
    stroke-width: 2;
  }
}

.graph-import-canvas--faded {
  opacity: 0.2;
}
</style>
