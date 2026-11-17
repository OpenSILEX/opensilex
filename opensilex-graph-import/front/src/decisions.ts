import type { ColumnDecisionDTO, PropertyValueDTO, SheetInterpretationDTO } from './lib'

/** What the user decided about a sheet, without the import and sheet names the request adds. */
export interface SheetDecision {
  class_uri?: string
  identifier_column?: number
  columns: ColumnDecisionDTO[]
  /** The columns left out: they give no value and are not suggested again. */
  left_out: number[]
  /** Values given for every row of the sheet, for properties no column gives. */
  given: PropertyValueDTO[]
}

/**
 * What the user decided about a sheet so far, read from its interpretation: only the answers whose
 * status is CHOSEN. A new decision starts from it, so that deciding one thing never forgets another.
 */
export function decidedSoFar(sheet: SheetInterpretationDTO): SheetDecision {
  return {
    class_uri: sheet.sheet_class?.status === 'CHOSEN' ? sheet.sheet_class.chosen_uri : undefined,
    identifier_column: sheet.identifier?.status === 'CHOSEN' ? sheet.identifier.chosen_column : undefined,
    columns: (sheet.columns ?? [])
      .filter((column) => column.property?.status === 'CHOSEN' && column.property.chosen_uri)
      .map((column) => ({
        column: column.index!,
        property: column.property!.chosen_uri!,
        variable: column.variable_status === 'CHOSEN' ? column.variable : undefined
      })),
    left_out: (sheet.columns ?? [])
      .filter((column) => column.property?.status === 'LEFT_OUT')
      .map((column) => column.index!),
    given: (sheet.given ?? []).map((value) => ({ property: value.property, value: value.value }))
  }
}

/** Whether the user decided anything about the sheet, which they may then give back to the suggestions. */
export function hasDecisions(sheet: SheetInterpretationDTO): boolean {
  const decided = decidedSoFar(sheet)
  return (
    decided.class_uri !== undefined ||
    decided.identifier_column !== undefined ||
    decided.columns.length > 0 ||
    decided.left_out.length > 0 ||
    decided.given.length > 0
  )
}

/**
 * The decision with a value given for every row, or without it when the value is undefined. Giving a
 * value accepts the class the sheet shows: the property is one of that class's.
 */
export function withGiven(sheet: SheetInterpretationDTO, property: string, value: string | undefined): SheetDecision {
  const decision = decidedSoFar(sheet)
  const given = decision.given.filter((kept) => kept.property !== property)
  if (value?.trim()) {
    given.push({ property, value: value.trim() })
  }
  return { ...decision, class_uri: decision.class_uri ?? sheet.sheet_class?.chosen_uri, given }
}

/**
 * The decision with a column's property set, or given back to the suggestions when the property is
 * undefined. Choosing a property accepts the class the sheet shows, chosen or suggested: the property
 * was chosen among that class's.
 */
export function withColumn(sheet: SheetInterpretationDTO, column: number, property: string | undefined): SheetDecision {
  const decision = decidedSoFar(sheet)
  const columns = decision.columns.filter((decided) => decided.column !== column)
  if (property) {
    columns.push({ column, property })
  }
  return {
    ...decision,
    class_uri: decision.class_uri ?? sheet.sheet_class?.chosen_uri,
    columns,
    left_out: decision.left_out.filter((left) => left !== column)
  }
}

/**
 * The decision with a column left out: it loses its property, gives no value, and stays out until a
 * decision takes it back with {@link withColumn}.
 */
export function leftOut(sheet: SheetInterpretationDTO, column: number): SheetDecision {
  const decision = withColumn(sheet, column, undefined)
  return { ...decision, left_out: [...decision.left_out, column] }
}

/**
 * The decision accepting everything the sheet suggests: its class, its identifier column and the
 * property of every suggested column. What the user decided already stays, the columns left out too.
 */
export function acceptSheetSuggestions(sheet: SheetInterpretationDTO): SheetDecision {
  const decision = decidedSoFar(sheet)
  const decided = new Set(decision.columns.map((column) => column.column))
  const suggested = (sheet.columns ?? [])
    .filter(
      (column) =>
        !column.type_column &&
        column.property?.status === 'SUGGESTED' &&
        !!column.property.chosen_uri &&
        !decided.has(column.index!)
    )
    .map((column) => ({ column: column.index!, property: column.property!.chosen_uri! }))
  return {
    ...decision,
    class_uri: decision.class_uri ?? sheet.sheet_class?.chosen_uri,
    identifier_column: decision.identifier_column ?? sheet.identifier?.chosen_column,
    columns: [...decision.columns, ...suggested]
  }
}

/** The property a column of measures holds: the value of each datum. */
export const MEASURE_PROPERTY = 'http://www.w3.org/1999/02/22-rdf-syntax-ns#value'

/** The class of the rows of a sheet of data. */
export const DATA_CLASS = 'http://www.opensilex.org/vocabulary/oeso#Data'

/** Whether the sheet holds data, by the class it shows. */
export function isDataSheet(sheet: SheetInterpretationDTO): boolean {
  return sheet.sheet_class?.chosen_uri === DATA_CLASS
}

/**
 * The decision with a column measuring a variable, or given back to the suggestions when the variable
 * is undefined. Like choosing a property, it accepts the class the sheet shows.
 */
export function withMeasure(sheet: SheetInterpretationDTO, column: number, variable: string | undefined): SheetDecision {
  const decision = withColumn(sheet, column, undefined)
  if (variable) {
    decision.columns.push({ column, property: MEASURE_PROPERTY, variable })
  }
  return decision
}
