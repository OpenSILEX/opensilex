import type { PreviewSheetDTO, SheetLinkDTO } from './lib'

/** What the import would do to a resource, as the server names it. */
export type NodeState = 'CREATED' | 'COMPLETED' | 'UNCHANGED' | 'CONFLICT'

export const NODE_STATES: NodeState[] = ['CREATED', 'COMPLETED', 'UNCHANGED', 'CONFLICT']

/** Which rows the grid keeps: those of one state, or those the file must be corrected for. */
export interface PreviewFilter {
  state?: NodeState
  toFix: boolean
}

/** The colours the tags of the preview take. */
export type TagType = 'success' | 'info' | 'warning' | 'error' | 'default'

/** The colour of a resource's state: new in blue, completed in green, conflicts in red. */
export function nodeStateType(state: string | undefined): TagType {
  switch (state) {
    case 'CREATED':
      return 'info'
    case 'COMPLETED':
      return 'success'
    case 'CONFLICT':
      return 'error'
    default:
      return 'default'
  }
}

/** The colour of a value's state, the same way. */
export function valueStateType(state: string | undefined): TagType {
  switch (state) {
    case 'NEW':
      return 'info'
    case 'CONFLICT':
    case 'UNUSABLE':
      return 'error'
    case 'AMBIGUOUS':
      return 'warning'
    default:
      return 'default'
  }
}

/** Translates a key with parameters; given by the caller, which holds the i18n plugin. */
export type Translate = (key: string, params?: { [name: string]: unknown }) => string

/**
 * The map of the sheets in Mermaid's language: a box per sheet, a rounded box per class of existing
 * resources the sheets point to, a dotted line between sheets that describe the same resources, an
 * arrow from a sheet to the sheet or resources its cells point to.
 *
 * Ids are made up, and every label is escaped: sheet names and headers are the user's own text.
 */
export function sheetMapDiagram(sheets: PreviewSheetDTO[], links: SheetLinkDTO[], t: Translate): string {
  const lines = ['flowchart LR']
  const ids = new Map<string, string>()
  sheets.forEach((sheet, index) => {
    const id = 's' + index
    ids.set(sheet.name ?? '', id)
    const detail = t('GraphImport.preview.map.box', {
      class: sheet.class_label ?? t('GraphImport.preview.map.noClass'),
      rows: sheet.row_count ?? 0
    })
    lines.push(`  ${id}["${escape(sheet.name ?? '')}<br/>${escape(detail)}"]`)
  })
  const instances = new Map<string, string>()
  for (const link of links) {
    const from = ids.get(link.from ?? '')
    if (!from) {
      continue
    }
    if (link.kind === 'INSTANCE') {
      let id = instances.get(link.to ?? '')
      if (!id) {
        id = 'i' + instances.size
        instances.set(link.to ?? '', id)
        lines.push(`  ${id}(["${escape(t('GraphImport.preview.map.existing', { class: link.to ?? '' }))}"])`)
      }
      lines.push(`  ${from} -->|"${escape(edgeLabel(link, t))}"| ${id}`)
      continue
    }
    const to = ids.get(link.to ?? '')
    if (!to) {
      continue
    }
    const arrow = link.kind === 'SAME_RESOURCE' ? '-.-' : '-->'
    lines.push(`  ${from} ${arrow}|"${escape(edgeLabel(link, t))}"| ${to}`)
  }
  return lines.join('\n')
}

/** What a line of the map says: how many shared resources, or which property and how many links. */
export function edgeLabel(link: SheetLinkDTO, t: Translate): string {
  if (link.kind === 'SAME_RESOURCE') {
    return t('GraphImport.preview.map.same', { count: link.count ?? 0 })
  }
  return t('GraphImport.preview.map.links', { property: link.property ?? '', count: link.count ?? 0 })
}

/** Keeps the user's text from closing a label or reading as markup. */
function escape(text: string): string {
  return text
    .replace(/&/g, '#amp;')
    .replace(/"/g, '#quot;')
    .replace(/</g, '#lt;')
    .replace(/>/g, '#gt;')
    .replace(/[\r\n|]+/g, ' ')
}
