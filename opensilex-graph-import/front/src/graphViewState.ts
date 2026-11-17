import type { Point } from './understandLayout'

/** How the "understand" step shows the sheets: as tables or as a graph. */
export type UnderstandView = 'table' | 'graph'

/** What the counters of the graph bring forward: the choices left, or the problems. */
export type GraphFilter = 'undecided' | 'problems'

const VIEW_KEY = 'graph-import.understand.view'
const POSITIONS_PREFIX = 'graph-import.positions.'

/**
 * What this browser remembers of the graph view. The storage may be missing or refuse access (private
 * window, blocked site data): reading then gives the defaults, and writing does nothing.
 */
function storage(): Storage | undefined {
  try {
    return globalThis.localStorage ?? undefined
  } catch {
    return undefined
  }
}

/** The view the user chose last in this browser; tables when none. */
export function readView(): UnderstandView {
  try {
    return storage()?.getItem(VIEW_KEY) === 'graph' ? 'graph' : 'table'
  } catch {
    return 'table'
  }
}

export function writeView(view: UnderstandView): void {
  try {
    storage()?.setItem(VIEW_KEY, view)
  } catch {
    // Nothing to do: the choice is only a convenience.
  }
}

/** Where the user put the nodes of an import's graph, by node id; nothing when unknown or unreadable. */
export function readPositions(sessionId: string): Map<string, Point> {
  try {
    const stored = storage()?.getItem(POSITIONS_PREFIX + sessionId)
    const parsed: unknown = stored ? JSON.parse(stored) : {}
    const positions = new Map<string, Point>()
    if (parsed && typeof parsed === 'object') {
      for (const [id, value] of Object.entries(parsed as Record<string, unknown>)) {
        if (isPoint(value)) {
          positions.set(id, { x: value.x, y: value.y })
        }
      }
    }
    return positions
  } catch {
    return new Map()
  }
}

export function writePositions(sessionId: string, positions: Map<string, Point>): void {
  try {
    storage()?.setItem(POSITIONS_PREFIX + sessionId, JSON.stringify(Object.fromEntries(positions)))
  } catch {
    // Nothing to do: the positions are only a convenience.
  }
}

function isPoint(value: unknown): value is Point {
  const point = value as Point | null
  return !!point && typeof point === 'object' && Number.isFinite(point.x) && Number.isFinite(point.y)
}
