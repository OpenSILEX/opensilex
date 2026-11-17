import type { InterpretationDTO, PropertyOptionDTO } from './lib'
import { existingNodeId, sheetNodeId, shortName, type PropertyRef } from './understandGraph'

/** A property a column may take when dropped on a node, with the score the evidence gave it. */
export interface DropProperty extends PropertyRef {
  /** The score of the property among the column's suggestions, undefined when it was not suggested. */
  score?: number
}

/** A column dropped on a node where several properties may link it, until the user picks one. */
export interface PendingDrop {
  sheet: string
  column: number
  /** The name of the node it was dropped on. */
  targetLabel: string
  properties: DropProperty[]
}

/** A node a column may be dropped on, and the properties that would link the column there. */
export interface DropTarget {
  nodeId: string
  /** For the resources that exist already, their class: the graph draws its node during the drag. */
  classUri?: string
  /** The best suggestion first. */
  properties: DropProperty[]
}

/**
 * The nodes a column of a sheet may be dropped on: the sheets whose rows its sheet's link properties
 * accept, and the resources that exist already of those properties' ranges. A column of values, the
 * type column and a sheet without a class have none: dropping only makes a column a link, and the link
 * properties are those of the sheet's class.
 */
export function targetsFor(interpretation: InterpretationDTO | undefined, sheet: string, column: number): DropTarget[] {
  const owner = (interpretation?.sheets ?? []).find((candidate) => candidate.name === sheet)
  const interpreted = (owner?.columns ?? []).find((candidate) => candidate.index === column)
  // A sheet the assistant added holds what the user declared: it changes in its drawer, not by a drop.
  if (!owner || !interpreted || interpreted.type_column || owner.declared) {
    return []
  }
  const properties = owner.properties ?? []
  const current = properties.find((property) => property.uri === interpreted.property?.chosen_uri)
  const settled = ['CHOSEN', 'DECIDED', 'SUGGESTED'].includes(interpreted.property?.status ?? '')
  if (settled && current?.kind !== 'OBJECT') {
    return []
  }
  const scores = new Map<string, number>()
  for (const candidate of interpreted.property?.candidates ?? []) {
    if (candidate.uri && candidate.score !== undefined) {
      scores.set(candidate.uri, candidate.score)
    }
  }
  const targets = new Map<string, DropProperty[]>()
  const classes = new Map<string, string>()
  const add = (nodeId: string, property: PropertyOptionDTO) => {
    const offered: DropProperty = {
      uri: property.uri!,
      label: property.label ?? shortName(property.uri!),
      score: scores.get(property.uri!)
    }
    targets.set(nodeId, [...(targets.get(nodeId) ?? []), offered])
  }
  for (const property of properties) {
    if (property.kind !== 'OBJECT' || !property.uri) {
      continue
    }
    for (const fitting of property.fitting_sheets ?? []) {
      add(sheetNodeId(fitting), property)
    }
    if (property.range) {
      add(existingNodeId(property.range), property)
      classes.set(existingNodeId(property.range), property.range)
    }
  }
  return [...targets.entries()].map(([nodeId, offered]) => ({ nodeId, classUri: classes.get(nodeId), properties: offered.sort(bestFirst) }))
}

/** The suggested properties first, by score, then the others by label. */
function bestFirst(a: DropProperty, b: DropProperty): number {
  if (a.score !== b.score) {
    return (b.score ?? -1) - (a.score ?? -1)
  }
  return a.label.localeCompare(b.label)
}
