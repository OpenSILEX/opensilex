import { sheetNodeId, type UnderstandEdge, type UnderstandGraph, type UnderstandNode } from './understandGraph'

export interface Point {
  x: number
  y: number
}

/** A node where it is drawn, with the radius of its circle. */
export interface PlacedNode extends Point {
  r: number
  /** For a column, the direction away from its sheet, in which its name is written. */
  angle?: number
}

/** A rectangle, from its top left corner. */
export interface Box {
  x: number
  y: number
  width: number
  height: number
}

/** An edge as it is drawn: its path, the box of its label, and the stub of its cells that land nowhere. */
export interface PlacedEdge {
  path: string
  label: Box
  /** The short segment saying how many cells point to nothing settled, when there are some. */
  stub?: { from: Point; to: Point }
  /** Where the edge meets its target, where a handle lets the user drag the link elsewhere. */
  end: Point
}

/** The text an edge's label shows, which sets the width of its box. */
export type LabelText = (edge: UnderstandEdge) => string

/** The label of an edge when the screen gives none: its property, else its count. */
export function defaultLabel(edge: UnderstandEdge): string {
  return edge.property?.label ?? (edge.count !== undefined ? String(edge.count) : '')
}

export interface GraphLayout {
  nodes: Map<string, PlacedNode>
  edges: Map<string, PlacedEdge>
  viewBox: Box
}

/** The radius of a sheet with no rows, and of the sheet with the most. */
export const SHEET_RADIUS = { min: 34, max: 56 }
export const COLUMN_RADIUS = 14
export const EXISTING_RADIUS = 30
/** How far apart two edges between the same nodes are drawn. */
export const PARALLEL_GAP = 18
/** How far a column is drawn from the edge of its sheet. */
const COLUMN_DISTANCE = 78
/** The length of arc a column takes around its sheet. */
const COLUMN_SPACING = 34
/** The room left between two sheets on the circle of sheets. */
const SHEET_GAP = 60
/** How far beyond the circle of sheets the resources that exist already are drawn. */
const EXISTING_DISTANCE = 260
const LABEL_HEIGHT = 16
/** The longest a column's name is written, beyond which it is cut. */
export const COLUMN_NAME_LENGTH = 18
/** A rough width of a character of a label, enough to keep labels apart without measuring text. */
const CHARACTER_WIDTH = 7
const MARGIN = 40

/**
 * Where each node and edge of the graph is drawn. The same graph and saved positions give the same
 * drawing: sheets on a circle in the workbook's order, a lone sheet in the middle; their columns fanned
 * out away from the middle; the resources that exist already on an outer ring, near the sheets pointing
 * to them. A saved position wins over the computed one, and a sheet's columns follow it.
 *
 * @param saved where the user put nodes, by id
 * @param size  the size of the canvas, the smallest the view box is
 */
export function layout(
  graph: UnderstandGraph,
  saved: Map<string, Point>,
  size: { width: number; height: number },
  labelText: LabelText = defaultLabel
): GraphLayout {
  const nodes = new Map<string, PlacedNode>()
  const sheets = graph.nodes.filter((node) => node.kind === 'sheet')
  const maxRows = Math.max(0, ...sheets.map((sheet) => sheet.rowCount ?? 0))
  const radii = sheets.map((sheet) => sheetRadius(sheet.rowCount ?? 0, maxRows))
  const arcs = sheets.map((sheet, index) => sheetArc(radii[index], satellitesOf(graph, sheet.sheet ?? '')))
  const total = arcs.reduce((sum, arc) => sum + arc, 0)
  // Two neighbours never closer than their widths, however few the sheets.
  const ring = sheets.length > 1
    ? Math.max(total / (2 * Math.PI), (2 * SHEET_RADIUS.max + SHEET_GAP) / (2 * Math.sin(Math.PI / sheets.length)))
    : 0
  let travelled = 0
  sheets.forEach((sheet, index) => {
    const angle = -Math.PI / 2 + (2 * Math.PI * (travelled + arcs[index] / 2)) / Math.max(1, total)
    travelled += arcs[index]
    const computed = { x: ring * Math.cos(angle), y: ring * Math.sin(angle) }
    nodes.set(sheet.id, { ...(saved.get(sheet.id) ?? computed), r: radii[index] })
  })
  const centre = centreOf([...nodes.values()])
  placeColumns(graph, nodes, saved, centre)
  placeExisting(graph, nodes, saved, ring, centre)
  const edges = new Map<string, PlacedEdge>()
  placeEdges(graph.edges, nodes, edges, centre, labelText)
  separateLabels(graph.edges, edges)
  return { nodes, edges, viewBox: viewBoxOf(graph, nodes, edges, size) }
}

/** How many nodes are drawn around a sheet. */
function satellitesOf(graph: UnderstandGraph, sheet: string): number {
  return graph.nodes.filter((node) => (node.kind === 'column' || node.kind === 'bundle') && node.sheet === sheet).length
}

/**
 * The length of the circle of sheets a sheet takes: its own width, and the width of the fan of its
 * columns, so that a sheet without columns takes little room.
 */
function sheetArc(radius: number, satellites: number): number {
  const fan = satellites > 1 ? Math.min(Math.PI * 1.2, (COLUMN_SPACING * (satellites - 1)) / (radius + COLUMN_DISTANCE)) : 0
  return 2 * radius + SHEET_GAP + fan * (radius + COLUMN_DISTANCE) * 0.8
}

/** The radius of a sheet, by the logarithm of its rows, so that a large sheet does not fill the canvas. */
export function sheetRadius(rows: number, maxRows: number): number {
  if (maxRows <= 0) {
    return SHEET_RADIUS.min
  }
  return SHEET_RADIUS.min + ((SHEET_RADIUS.max - SHEET_RADIUS.min) * Math.log1p(Math.max(0, rows))) / Math.log1p(maxRows)
}

/** The columns of each sheet, fanned out on the side away from the middle, or all around a lone sheet. */
function placeColumns(graph: UnderstandGraph, nodes: Map<string, PlacedNode>, saved: Map<string, Point>, centre: Point): void {
  const bySheet = new Map<string, UnderstandNode[]>()
  graph.nodes
    .filter((node) => node.kind === 'column' || node.kind === 'bundle')
    .forEach((node) => bySheet.set(node.sheet ?? '', [...(bySheet.get(node.sheet ?? '') ?? []), node]))
  const lone = nodes.size === 1
  bySheet.forEach((columns, sheet) => {
    const owner = nodes.get(sheetNodeId(sheet))
    if (!owner) {
      return
    }
    const distance = owner.r + COLUMN_DISTANCE
    const outward = Math.hypot(owner.x - centre.x, owner.y - centre.y) > 1 ? Math.atan2(owner.y - centre.y, owner.x - centre.x) : -Math.PI / 2
    const step = COLUMN_SPACING / distance
    const spread = lone ? 2 * Math.PI : Math.min(Math.PI * 1.2, step * (columns.length - 1))
    const actualStep = columns.length > 1 ? (lone ? spread / columns.length : spread / (columns.length - 1)) : 0
    const first = lone ? outward : outward - spread / 2
    columns.forEach((column, index) => {
      const angle = first + actualStep * index
      const computed = { x: owner.x + distance * Math.cos(angle), y: owner.y + distance * Math.sin(angle) }
      const point = saved.get(column.id) ?? computed
      const away = Math.hypot(point.x - owner.x, point.y - owner.y) > 1 ? Math.atan2(point.y - owner.y, point.x - owner.x) : angle
      nodes.set(column.id, { ...point, r: COLUMN_RADIUS, angle: away })
    })
  })
}

/**
 * The resources that exist already, on a ring beyond the sheets, each at the mean angle of the sheets
 * pointing to it, then spread so that no two overlap.
 */
function placeExisting(
  graph: UnderstandGraph,
  nodes: Map<string, PlacedNode>,
  saved: Map<string, Point>,
  ring: number,
  centre: Point
): void {
  const existing = graph.nodes.filter((node) => node.kind === 'existing')
  if (!existing.length) {
    return
  }
  const radius = ring + SHEET_RADIUS.max + COLUMN_DISTANCE + EXISTING_DISTANCE
  const wanted = existing.map((node, index) => {
    const from = graph.edges.filter((edge) => edge.to === node.id).map((edge) => nodes.get(edge.from)).filter((point): point is PlacedNode => !!point)
    const x = from.reduce((sum, point) => sum + (point.x - centre.x), 0)
    const y = from.reduce((sum, point) => sum + (point.y - centre.y), 0)
    const angle = Math.hypot(x, y) > 1 ? Math.atan2(y, x) : Math.PI / 2 + (2 * Math.PI * index) / existing.length
    return { node, angle }
  })
  wanted.sort((a, b) => a.angle - b.angle || a.node.id.localeCompare(b.node.id))
  const gap = (2 * (EXISTING_RADIUS + LABEL_HEIGHT * 2)) / radius
  for (let index = 1; index < wanted.length; index++) {
    wanted[index].angle = Math.max(wanted[index].angle, wanted[index - 1].angle + gap)
  }
  wanted.forEach(({ node, angle }) => {
    const computed = { x: centre.x + radius * Math.cos(angle), y: centre.y + radius * Math.sin(angle) }
    nodes.set(node.id, { ...(saved.get(node.id) ?? computed), r: EXISTING_RADIUS })
  })
}

/**
 * The path of each edge. Edges between the same two nodes are quadratic curves set apart; an edge from a
 * sheet to itself is a loop on its outer side; a straight edge has no curve.
 */
function placeEdges(
  graphEdges: UnderstandEdge[],
  nodes: Map<string, PlacedNode>,
  edges: Map<string, PlacedEdge>,
  centre: Point,
  labelText: LabelText
): void {
  const pairs = new Map<string, UnderstandEdge[]>()
  const loops = new Map<string, UnderstandEdge[]>()
  for (const edge of graphEdges) {
    if (!nodes.has(edge.from) || !nodes.has(edge.to)) {
      continue
    }
    if (edge.from === edge.to) {
      loops.set(edge.from, [...(loops.get(edge.from) ?? []), edge])
    } else {
      const key = [edge.from, edge.to].sort().join('\u0000')
      pairs.set(key, [...(pairs.get(key) ?? []), edge])
    }
  }
  pairs.forEach((between) =>
    between.forEach((edge, index) => {
      const offset = (index - (between.length - 1) / 2) * PARALLEL_GAP
      const sign = edge.from < edge.to ? 1 : -1
      edges.set(edge.id, curve(labelText(edge), edge, nodes.get(edge.from)!, nodes.get(edge.to)!, offset * sign))
    })
  )
  loops.forEach((own, sheet) =>
    own.forEach((edge, index) => edges.set(edge.id, loop(labelText(edge), edge, nodes.get(sheet)!, centre, index)))
  )
}

/**
 * A quadratic curve from the border of one circle to the border of the other, its middle set apart from
 * the straight line by the offset. Its control point is twice as far, since the middle of a quadratic
 * curve is halfway to it.
 */
function curve(text: string, edge: UnderstandEdge, from: PlacedNode, to: PlacedNode, offset: number): PlacedEdge {
  const dx = to.x - from.x
  const dy = to.y - from.y
  const length = Math.hypot(dx, dy) || 1
  const normal = { x: -dy / length, y: dx / length }
  const control = { x: (from.x + to.x) / 2 + normal.x * offset * 2, y: (from.y + to.y) / 2 + normal.y * offset * 2 }
  const start = towards(from, control, from.r)
  const end = towards(to, control, to.r)
  const middle = { x: (start.x + 2 * control.x + end.x) / 4, y: (start.y + 2 * control.y + end.y) / 4 }
  const path = offset === 0 ? `M${round(start)} L${round(end)}` : `M${round(start)} Q${round(control)} ${round(end)}`
  return { path, label: labelBox(text, middle), stub: stubOf(edge, start, control, end, normal), end }
}

/** A loop on the outer side of a sheet, larger for each further loop of the same sheet. */
function loop(text: string, edge: UnderstandEdge, sheet: PlacedNode, centre: Point, index: number): PlacedEdge {
  const outward = Math.hypot(sheet.x - centre.x, sheet.y - centre.y) > 1 ? Math.atan2(sheet.y - centre.y, sheet.x - centre.x) : -Math.PI / 2
  const reach = sheet.r + 60 + index * PARALLEL_GAP
  const open = 0.45
  const at = (angle: number, distance: number) => ({ x: sheet.x + distance * Math.cos(angle), y: sheet.y + distance * Math.sin(angle) })
  const start = at(outward - open, sheet.r)
  const end = at(outward + open, sheet.r)
  const first = at(outward - open * 1.6, reach * 1.25)
  const second = at(outward + open * 1.6, reach * 1.25)
  const apex = at(outward, reach)
  const normal = { x: Math.cos(outward), y: Math.sin(outward) }
  return {
    path: `M${round(start)} C${round(first)} ${round(second)} ${round(end)}`,
    label: labelBox(text, { x: apex.x + normal.x * LABEL_HEIGHT, y: apex.y + normal.y * LABEL_HEIGHT }),
    stub: edge.unresolved ? { from: apex, to: { x: apex.x + normal.x * 28, y: apex.y + normal.y * 28 } } : undefined,
    end
  }
}

/** The stub of an edge's unresolved cells, near its end, standing off the curve. */
function stubOf(edge: UnderstandEdge, start: Point, control: Point, end: Point, normal: Point): PlacedEdge['stub'] {
  if (!edge.unresolved) {
    return undefined
  }
  const t = 0.8
  const from = {
    x: (1 - t) * (1 - t) * start.x + 2 * (1 - t) * t * control.x + t * t * end.x,
    y: (1 - t) * (1 - t) * start.y + 2 * (1 - t) * t * control.y + t * t * end.y
  }
  return { from, to: { x: from.x + normal.x * 26, y: from.y + normal.y * 26 } }
}

/** The box of an edge's label, centred on a point; its width guessed from its text. */
function labelBox(text: string, centre: Point): Box {
  const width = text.length * CHARACTER_WIDTH + 12
  return { x: centre.x - width / 2, y: centre.y - LABEL_HEIGHT / 2, width, height: LABEL_HEIGHT }
}

/** Labels that overlap one placed before are moved up or down, a little further each time. */
function separateLabels(graphEdges: UnderstandEdge[], edges: Map<string, PlacedEdge>): void {
  const placed: Box[] = []
  for (const edge of graphEdges) {
    const drawn = edges.get(edge.id)
    if (!drawn) {
      continue
    }
    const box = { ...drawn.label }
    for (let attempt = 1; attempt <= 8 && placed.some((other) => overlaps(box, other)); attempt++) {
      const shift = Math.ceil(attempt / 2) * (LABEL_HEIGHT + 2) * (attempt % 2 ? 1 : -1)
      box.y = drawn.label.y + shift
    }
    drawn.label = box
    placed.push(box)
  }
}

/** The smallest box holding every node and label, with a margin, never smaller than the canvas. */
function viewBoxOf(
  graph: UnderstandGraph,
  nodes: Map<string, PlacedNode>,
  edges: Map<string, PlacedEdge>,
  size: { width: number; height: number }
): Box {
  if (!nodes.size) {
    return { x: -size.width / 2, y: -size.height / 2, width: size.width, height: size.height }
  }
  const xs: number[] = []
  const ys: number[] = []
  const names = new Map(graph.nodes.map((node) => [node.id, node.label.length]))
  nodes.forEach((node, id) => {
    // A sheet's rows are written under it; a column's name beyond it, away from its sheet.
    xs.push(node.x - node.r - MARGIN, node.x + node.r + MARGIN)
    ys.push(node.y - node.r - MARGIN, node.y + node.r + MARGIN + LABEL_HEIGHT)
    if (node.angle !== undefined) {
      const reach = node.r + 4 + Math.min(COLUMN_NAME_LENGTH, names.get(id) ?? 0) * CHARACTER_WIDTH
      xs.push(node.x + Math.cos(node.angle) * reach + Math.sign(Math.cos(node.angle)) * MARGIN / 2)
      ys.push(node.y + Math.sin(node.angle) * reach)
    }
  })
  edges.forEach((edge) => {
    xs.push(edge.label.x - MARGIN / 2, edge.label.x + edge.label.width + MARGIN / 2)
    ys.push(edge.label.y - MARGIN / 2, edge.label.y + edge.label.height + MARGIN / 2)
  })
  const box = { x: Math.min(...xs), y: Math.min(...ys), width: Math.max(...xs) - Math.min(...xs), height: Math.max(...ys) - Math.min(...ys) }
  const width = Math.max(box.width, size.width)
  const height = Math.max(box.height, size.height)
  return { x: box.x - (width - box.width) / 2, y: box.y - (height - box.height) / 2, width, height }
}

function centreOf(points: Point[]): Point {
  if (!points.length) {
    return { x: 0, y: 0 }
  }
  return {
    x: points.reduce((sum, point) => sum + point.x, 0) / points.length,
    y: points.reduce((sum, point) => sum + point.y, 0) / points.length
  }
}

/** The point at a distance from a circle's centre towards another point, or the centre if they coincide. */
function towards(from: Point, to: Point, distance: number): Point {
  const length = Math.hypot(to.x - from.x, to.y - from.y)
  if (length < 1e-6) {
    return { x: from.x, y: from.y }
  }
  return { x: from.x + ((to.x - from.x) * distance) / length, y: from.y + ((to.y - from.y) * distance) / length }
}

function overlaps(a: Box, b: Box): boolean {
  return a.x < b.x + b.width && b.x < a.x + a.width && a.y < b.y + b.height && b.y < a.y + a.height
}

function round(point: Point): string {
  return `${Math.round(point.x * 10) / 10},${Math.round(point.y * 10) / 10}`
}
