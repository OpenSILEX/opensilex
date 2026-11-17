import type {
  ClassOptionDTO,
  ColumnInterpretationDTO,
  ColumnLinkDTO,
  InterpretationDTO,
  SheetInterpretationDTO,
  ValidationDTO
} from './lib'
import { columnTitle } from './declared'
import { ofColumn, sheetCheck, tally, type SheetCheck, type Tally } from './problems'

/** What a node of the graph stands for. */
export type NodeKind = 'sheet' | 'existing' | 'column' | 'bundle'

/**
 * What a column drawn as a node is: a column of values, one to decide, the column giving the class of
 * each row, or one the user left out.
 */
export type ColumnRole = 'value' | 'undecided' | 'type' | 'leftOut'

/**
 * What an edge stands for: a sheet's column of values, a sheet's column still to decide, a link column
 * to another node or to its own sheet, or two sheets describing the same resources.
 */
export type EdgeKind = 'value' | 'pending' | 'link' | 'loop' | 'shared'

/** A property as the graph names it. */
export interface PropertyRef {
  uri: string
  label: string
}

export interface UnderstandNode {
  /** Stable across redraws, so that positions and selection survive a new interpretation. */
  id: string
  kind: NodeKind
  /** The user's text or the ontology's label, drawn as text, never as markup. */
  label: string
  /** The sheet of a sheet, column or bundle node. */
  sheet?: string
  /** The column of a column node, from 0. */
  column?: number
  role?: ColumnRole
  /** How the sheet's class or the column's property was settled. */
  status?: string
  classUri?: string
  rowCount?: number
  /** The header of the sheet's identifier column. */
  identifier?: string
  /** How the sheet's identifier column was settled. */
  identifierStatus?: string
  property?: PropertyRef
  /** The columns a bundle folds. */
  columns?: number[]
  /** For a sheet, whether the assistant added it for resources the user declared. */
  declared?: boolean
  /** For a column, whether it holds a value the user gave for every row. */
  given?: boolean
  /** The problems of the sheet's rows, of the column, or of the columns a bundle folds. */
  tally: Tally
}

export interface UnderstandEdge {
  id: string
  kind: EdgeKind
  from: string
  to: string
  sheet?: string
  column?: number
  property?: PropertyRef
  status?: string
  /** How many cells land there, or how many resources two sheets share. */
  count?: number
  /** How many cells of the column point to nothing settled; on the first edge of the column only. */
  unresolved?: number
  tally: Tally
}

export interface UnderstandGraph {
  nodes: UnderstandNode[]
  edges: UnderstandEdge[]
}

/** What the user asked the graph to show. */
export interface GraphOptions {
  /** Whether the columns left out are drawn. */
  showLeftOut: boolean
  /** The sheets whose values are drawn one by one even when there are many. */
  expanded: Set<string>
  /**
   * The classes whose resources are drawn though no column reaches them yet: where a column being
   * dragged may be dropped.
   */
  extraExisting?: string[]
}

/**
 * What the user looks at, whatever element draws it: a column is a node while it is to decide, then one
 * edge or more once it is a link. The selection holds the subject, so that it survives a decision.
 */
export type Subject =
  | { kind: 'sheet'; sheet: string }
  | { kind: 'column'; sheet: string; column: number }
  | { kind: 'bundle'; sheet: string }
  | { kind: 'existing'; classUri?: string }
  | { kind: 'shared'; from: string; to: string }

/** The subject an element of the graph draws. */
export function subjectOf(element: UnderstandNode | UnderstandEdge): Subject {
  switch (element.kind) {
    case 'sheet':
      return { kind: 'sheet', sheet: element.sheet ?? '' }
    case 'bundle':
      return { kind: 'bundle', sheet: element.sheet ?? '' }
    case 'existing':
      return { kind: 'existing', classUri: (element as UnderstandNode).classUri }
    case 'shared':
      return { kind: 'shared', from: element.from.substring(SHEET_PREFIX.length), to: element.to.substring(SHEET_PREFIX.length) }
    default:
      return element.column === undefined
        ? { kind: 'sheet', sheet: element.sheet ?? '' }
        : { kind: 'column', sheet: element.sheet ?? '', column: element.column }
  }
}

export function sameSubject(a: Subject | undefined, b: Subject | undefined): boolean {
  return !!a && !!b && JSON.stringify(a) === JSON.stringify(b)
}

/** Whether a node asks the user for a choice: a column to decide, or a sheet without a class. */
export function isUndecided(element: UnderstandNode | UnderstandEdge): boolean {
  return (
    (element.kind === 'column' && (element as UnderstandNode).role === 'undecided') ||
    (element.kind === 'sheet' && element.status === 'UNDECIDED')
  )
}

/**
 * Whether an element carries problems. The edges of values and of columns to decide repeat the problems
 * of the column node they lead to, so only the node counts.
 */
export function hasProblems(element: UnderstandNode | UnderstandEdge): boolean {
  return countsProblems(element) && element.tally.errors + element.tally.warnings > 0
}

function countsProblems(element: UnderstandNode | UnderstandEdge): boolean {
  return element.kind !== 'value' && element.kind !== 'pending' && element.kind !== 'shared' && element.kind !== 'existing'
}

/**
 * The places the check says the cells of a column land, when the check is of the column's current
 * property: a check one decision behind says nothing about it.
 */
export function linksOfColumn(validation: ValidationDTO | undefined, sheet: string, column: number | undefined, property: string | undefined): ColumnLinkDTO[] {
  return (validation?.column_links ?? []).filter(
    (link) => link.sheet === sheet && link.column === column && !!property && link.property === property
  )
}

/** Above this many columns of values, a sheet draws them as one bundle until the user unfolds it. */
export const BUNDLE_THRESHOLD = 12

/** The id of the node of the resources that exist already when the link's class is not known. */
const ANY_CLASS = '*'

const SHEET_PREFIX = 'sheet:'

export function sheetNodeId(sheet: string): string {
  return SHEET_PREFIX + sheet
}

export function columnNodeId(sheet: string, column: number): string {
  return `col:${sheet}:${column}`
}

export function existingNodeId(classUri: string | undefined): string {
  return `existing:${classUri ?? ANY_CLASS}`
}

export function bundleNodeId(sheet: string): string {
  return `bundle:${sheet}`
}

/** The last segment of a URI, for a property or class the server gave no label for. */
export function shortName(uri: string): string {
  const cut = Math.max(uri.lastIndexOf('#'), uri.lastIndexOf('/'), uri.lastIndexOf(':'))
  return cut >= 0 && cut < uri.length - 1 ? uri.substring(cut + 1) : uri
}

/** What the graph knows of a column's property, wherever the server said it. */
interface PropertyFacts {
  ref: PropertyRef
  link: boolean
  range?: string
  rangeLabel?: string
}

/**
 * The graph of the "understand" step: the sheets, the columns they draw as nodes, the resources their
 * links reach, and the edges between them. A link column without settled cells, or whose check is one
 * decision behind, points to the resources of its property's range.
 */
export function buildUnderstandGraph(
  interpretation: InterpretationDTO | undefined,
  validation: ValidationDTO | undefined,
  options: GraphOptions
): UnderstandGraph {
  const nodes: UnderstandNode[] = []
  const edges: UnderstandEdge[] = []
  const existing = new Map<string, UnderstandNode>()
  const classLabels = labelsOf(interpretation?.classes ?? [])
  const sheets = interpretation?.sheets ?? []
  for (const sheet of sheets) {
    nodes.push(sheetNode(sheet, sheetCheck(validation, sheet.name ?? ''), classLabels))
  }
  for (const sheet of sheets) {
    const name = sheet.name ?? ''
    const check = sheetCheck(validation, name)
    const classUri = sheet.sheet_class?.chosen_uri
    const values: { node: UnderstandNode; edge: UnderstandEdge }[] = []
    for (const column of sheet.columns ?? []) {
      const columnTally = tally(ofColumn(check, column.index).counts)
      const facts = propertyOf(sheet, column)
      if (facts?.link && isSettled(column)) {
        edges.push(...linkEdges(name, column, facts, columnTally, validation, existing, classLabels))
        continue
      }
      const role = roleOf(column)
      // Without a class there is no property to choose: the sheet itself asks for its class.
      if ((role === 'leftOut' && !options.showLeftOut) || (role === 'undecided' && !classUri)) {
        continue
      }
      const node = columnNode(sheet, column, role, facts, columnTally)
      const edge: UnderstandEdge = {
        id: `value:${name}:${column.index}`,
        kind: role === 'undecided' ? 'pending' : 'value',
        from: sheetNodeId(name),
        to: node.id,
        sheet: name,
        column: column.index,
        property: role === 'value' ? facts?.ref : undefined,
        status: node.status,
        tally: columnTally
      }
      if (role === 'value') {
        values.push({ node, edge })
      } else {
        nodes.push(node)
        edges.push(edge)
      }
    }
    if (values.length > BUNDLE_THRESHOLD && !options.expanded.has(name)) {
      const bundle = bundleNode(name, values.map((value) => value.node))
      nodes.push(bundle)
      edges.push({ id: `value:${name}:bundle`, kind: 'value', from: sheetNodeId(name), to: bundle.id, sheet: name, tally: bundle.tally })
    } else {
      values.forEach((value) => {
        nodes.push(value.node)
        edges.push(value.edge)
      })
    }
  }
  for (const classUri of options.extraExisting ?? []) {
    existingOf(classUri, rangeLabelOf(sheets, classUri), existing, classLabels)
  }
  nodes.push(...existing.values())
  for (const shared of validation?.shared_resources ?? []) {
    edges.push({
      id: `shared:${shared.from}:${shared.to}`,
      kind: 'shared',
      from: sheetNodeId(shared.from ?? ''),
      to: sheetNodeId(shared.to ?? ''),
      count: shared.count,
      tally: { errors: 0, warnings: 0 }
    })
  }
  return { nodes, edges }
}

/** How many choices are left to the user, and how many problems the check found, for the toolbar. */
export function counters(graph: UnderstandGraph): { undecided: number; problems: Tally } {
  const elements = [...graph.nodes, ...graph.edges]
  const problems = elements
    .filter(countsProblems)
    .reduce<Tally>(
      (sum, element) => ({ errors: sum.errors + element.tally.errors, warnings: sum.warnings + element.tally.warnings }),
      { errors: 0, warnings: 0 }
    )
  return { undecided: graph.nodes.filter(isUndecided).length, problems }
}

function sheetNode(sheet: SheetInterpretationDTO, check: SheetCheck, classLabels: Map<string, string>): UnderstandNode {
  const name = sheet.name ?? ''
  const classUri = sheet.sheet_class?.chosen_uri
  const identifierColumn = sheet.identifier?.chosen_column
  const identifier = (sheet.columns ?? []).find((column) => column.index === identifierColumn)
  return {
    id: sheetNodeId(name),
    kind: 'sheet',
    label: name,
    sheet: name,
    status: sheet.sheet_class?.status,
    classUri,
    property: classUri ? { uri: classUri, label: classLabels.get(classUri) ?? shortName(classUri) } : undefined,
    rowCount: sheet.row_count ?? 0,
    identifier: identifier?.header,
    identifierStatus: sheet.identifier?.status,
    declared: sheet.declared,
    tally: tally(ofColumn(check, undefined).counts)
  }
}

function columnNode(
  owner: SheetInterpretationDTO,
  column: ColumnInterpretationDTO,
  role: ColumnRole,
  facts: PropertyFacts | undefined,
  columnTally: Tally
): UnderstandNode {
  const sheet = owner.name ?? ''
  return {
    id: columnNodeId(sheet, column.index ?? 0),
    kind: 'column',
    label: columnTitle(column, owner),
    given: column.given,
    sheet,
    column: column.index,
    role,
    status: role === 'type' ? 'DECIDED' : column.property?.status,
    property: role === 'value' ? facts?.ref : undefined,
    tally: columnTally
  }
}

function bundleNode(sheet: string, values: UnderstandNode[]): UnderstandNode {
  return {
    id: bundleNodeId(sheet),
    kind: 'bundle',
    label: String(values.length),
    sheet,
    columns: values.map((value) => value.column ?? 0),
    tally: values.reduce<Tally>(
      (sum, value) => ({ errors: sum.errors + value.tally.errors, warnings: sum.warnings + value.tally.warnings }),
      { errors: 0, warnings: 0 }
    )
  }
}

/**
 * The edges of a link column: one for each place its cells land, from the check when the check is of
 * the column's current property, else one to the resources of the property's range.
 */
function linkEdges(
  sheet: string,
  column: ColumnInterpretationDTO,
  facts: PropertyFacts,
  columnTally: Tally,
  validation: ValidationDTO | undefined,
  existing: Map<string, UnderstandNode>,
  classLabels: Map<string, string>
): UnderstandEdge[] {
  const links = linksOfColumn(validation, sheet, column.index, facts.ref.uri)
  const unresolved = links.filter((link) => link.kind === 'UNRESOLVED').reduce((sum, link) => sum + (link.count ?? 0), 0)
  const landed = links.filter((link) => link.kind === 'ROW' || link.kind === 'INSTANCE')
  const targets = landed.length
    ? landed.map((link) => ({
        to: link.kind === 'ROW' ? sheetNodeId(link.to_sheet ?? '') : existingOf(link.to_class, link.to_class_label, existing, classLabels),
        count: link.count
      }))
    : [{ to: existingOf(facts.range, facts.rangeLabel, existing, classLabels), count: undefined }]
  const from = sheetNodeId(sheet)
  return targets.map((target, index) => ({
    id: `link:${sheet}:${column.index}:${target.to}`,
    kind: target.to === from ? 'loop' : 'link',
    from,
    to: target.to,
    sheet,
    column: column.index,
    property: facts.ref,
    status: column.property?.status,
    count: target.count,
    unresolved: index === 0 && unresolved > 0 ? unresolved : undefined,
    tally: index === 0 ? columnTally : { errors: 0, warnings: 0 }
  }))
}

/** The id of the node of the resources of a class, the node added the first time it is reached. */
function existingOf(
  classUri: string | undefined,
  label: string | undefined,
  existing: Map<string, UnderstandNode>,
  classLabels: Map<string, string>
): string {
  const id = existingNodeId(classUri)
  if (!existing.has(id)) {
    existing.set(id, {
      id,
      kind: 'existing',
      label: label ?? (classUri ? classLabels.get(classUri) ?? shortName(classUri) : ''),
      classUri,
      tally: { errors: 0, warnings: 0 }
    })
  }
  return id
}

/**
 * The property of a column, its kind and range: from the sheet's class when it has one, else from the
 * column's candidates, since a header written as the predicate settles it without a class.
 */
function propertyOf(sheet: SheetInterpretationDTO, column: ColumnInterpretationDTO): PropertyFacts | undefined {
  const uri = column.property?.chosen_uri
  if (!uri) {
    return undefined
  }
  const option = (sheet.properties ?? []).find((property) => property.uri === uri)
  const candidate = (column.property?.candidates ?? []).find((property) => property.uri === uri)
  const kind = option?.kind ?? candidate?.kind
  return {
    ref: { uri, label: option?.label ?? candidate?.label ?? shortName(uri) },
    link: kind === 'OBJECT',
    range: option?.range ?? candidate?.range,
    rangeLabel: option?.range_label
  }
}

/** Whether the column has a property, by the user, the file or the evidence. */
function isSettled(column: ColumnInterpretationDTO): boolean {
  const status = column.property?.status
  return !column.type_column && !!column.property?.chosen_uri && (status === 'CHOSEN' || status === 'DECIDED' || status === 'SUGGESTED')
}

/** What a column drawn as a node is; the type column first, since its property is never decided. */
function roleOf(column: ColumnInterpretationDTO): ColumnRole {
  if (column.type_column) {
    return 'type'
  }
  if (column.property?.status === 'LEFT_OUT') {
    return 'leftOut'
  }
  if (isSettled(column)) {
    return 'value'
  }
  return 'undecided'
}

/** The label the server gave a class as the range of a property, if any property has it as its range. */
function rangeLabelOf(sheets: SheetInterpretationDTO[], classUri: string): string | undefined {
  for (const sheet of sheets) {
    const property = (sheet.properties ?? []).find((option) => option.range === classUri && option.range_label)
    if (property) {
      return property.range_label
    }
  }
  return undefined
}

/** The label of every class of the catalogue, by URI. */
function labelsOf(classes: ClassOptionDTO[], labels = new Map<string, string>()): Map<string, string> {
  for (const option of classes) {
    if (option.uri) {
      labels.set(option.uri, option.label ?? shortName(option.uri))
    }
    labelsOf(option.children ?? [], labels)
  }
  return labels
}
