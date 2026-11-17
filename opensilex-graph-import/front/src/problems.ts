import type { CellRefDTO, DateColumnDTO, GraphProblemDTO, ProblemCountDTO, ValidationDTO } from './lib'

/** What the check of the graph says about one sheet. */
export interface SheetCheck {
  problems: GraphProblemDTO[]
  counts: ProblemCountDTO[]
  dateColumns: DateColumnDTO[]
}

/** How the user says the dates of a column are written; undefined leaves it to the values. */
export interface DateReading {
  date_order?: 'DAY_FIRST' | 'MONTH_FIRST'
  zone?: string
}

/** How many problems stop the import, and how many only warn. */
export interface Tally {
  errors: number
  warnings: number
}

/**
 * The part of the check that concerns one sheet: the problems with a cell in it. A conflict between two
 * sheets is shown in both.
 */
export function sheetCheck(validation: ValidationDTO | undefined, sheet: string): SheetCheck {
  return {
    problems: (validation?.problems ?? [])
      .filter((problem) => (problem.cells ?? []).some((cell) => cell.sheet === sheet))
      .map((problem) => ({ ...problem, cells: inSheetFirst(problem.cells ?? [], sheet) })),
    counts: (validation?.counts ?? []).filter((count) => count.sheet === sheet),
    dateColumns: (validation?.date_columns ?? []).filter((column) => column.sheet === sheet)
  }
}

/** The cells of a problem, those of the sheet first, so that a problem is shown where the user looks. */
function inSheetFirst(cells: CellRefDTO[], sheet: string): CellRefDTO[] {
  return [...cells.filter((cell) => cell.sheet === sheet), ...cells.filter((cell) => cell.sheet !== sheet)]
}

/** The problems of one column of a sheet check, or of whole rows when the column is undefined. */
export function ofColumn(check: SheetCheck, column: number | undefined): SheetCheck {
  const sheet = check.problems[0]?.cells?.[0]?.sheet
  const aboutColumn = (problem: GraphProblemDTO) =>
    column === undefined
      ? problem.about_row === true
      : !problem.about_row && (problem.cells ?? []).some((cell) => cell.sheet === sheet && cell.column === column)
  return {
    problems: check.problems.filter(aboutColumn),
    counts: check.counts.filter((count) => (column === undefined ? count.column === undefined : count.column === column)),
    dateColumns: check.dateColumns.filter((dates) => dates.column === column)
  }
}

/** Every problem counted, the ones the list leaves out included. */
export function tally(counts: ProblemCountDTO[]): Tally {
  return counts.reduce<Tally>(
    (sum, count) =>
      count.severity === 'ERROR'
        ? { ...sum, errors: sum.errors + (count.count ?? 0) }
        : { ...sum, warnings: sum.warnings + (count.count ?? 0) },
    { errors: 0, warnings: 0 }
  )
}

/** A cell as the user finds it: its sheet, its row of values and its column, counted from 1. */
export function place(cell: CellRefDTO, headers: string[] | undefined): { sheet: string; row: number; column: string } {
  const column = cell.column ?? 0
  return { sheet: cell.sheet ?? '', row: (cell.row ?? 0) + 1, column: headers?.[column] ?? String(column + 1) }
}
