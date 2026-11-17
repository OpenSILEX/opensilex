import type { ColumnInterpretationDTO, DeclaredResourceDTO, SheetInterpretationDTO } from './lib'

/** The property naming a resource, by which rows of the files point to a declared one. */
export const LABEL = 'http://www.w3.org/2000/01/rdf-schema#label'

/** The properties the platform fills by itself when it writes a resource (dates, publisher). */
export const PLATFORM_KEPT_PREFIX = 'http://purl.org/dc/terms/'

/** The values of a declared resource, by property, in the order they were given. */
export function valuesByProperty(resource: DeclaredResourceDTO | undefined): Record<string, string[]> {
  const byProperty: Record<string, string[]> = {}
  for (const value of resource?.values ?? []) {
    byProperty[value.property] = [...(byProperty[value.property] ?? []), value.value]
  }
  return byProperty
}

/** The name of a declared resource, or its identifier when it has none. */
export function declaredName(resource: DeclaredResourceDTO | undefined): string {
  return valuesByProperty(resource)[LABEL]?.[0] ?? resource?.id ?? ''
}

/**
 * The name a column is shown under. A column the assistant added, or a header written as a property URI,
 * is shown under the property's label rather than its URI.
 */
export function columnTitle(column: ColumnInterpretationDTO, sheet: SheetInterpretationDTO): string {
  const header = column.header ?? String((column.index ?? 0) + 1)
  if (!column.given && column.header_form !== 'FULL_URI') {
    return header
  }
  const property = (sheet.properties ?? []).find((option) => option.uri === header)
  return property?.label ?? header
}
