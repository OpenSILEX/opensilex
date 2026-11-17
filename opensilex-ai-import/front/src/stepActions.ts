import type { ImportStepDTO } from './lib'

/**
 * What declaring a step asks the page to open, and where the first thing still missing for it is
 * named in the report. One table, so that a new step is an entry here and nothing else changes.
 */
const STEP_ACTIONS: { [step: string]: { target: string; category?: string } } = {
  PROJECTS: { target: 'PROJECT', category: 'projects' },
  VARIABLES: { target: 'VARIABLE', category: 'variables' },
  FACILITIES: { target: 'FACILITY', category: 'facilities' },
  EXPERIMENT: { target: 'EXPERIMENT', category: 'experiments' },
  FACTORS: { target: 'FACTORS' },
  FACTOR_LEVELS: { target: 'FACTORS' },
  OBJECTS: { target: 'SCIENTIFIC_OBJECTS' },
  DATA: { target: 'DATA' },
  EVENTS: { target: 'EVENT' }
}

interface ReportItem {
  source_value?: string
  status?: string
}

interface ReportCategories {
  [category: string]: unknown
}

/**
 * The creation to request for a step: its target, and the first name the report finds missing for
 * it, which a form can be opened with.
 */
export function creationOf(step: ImportStepDTO, report?: ReportCategories): { target: string; value: string } {
  const action = STEP_ACTIONS[step.step ?? '']
  if (!action) {
    return { target: step.target ?? '', value: '' }
  }
  const items = (action.category && report ? (report[action.category] as ReportItem[]) : []) ?? []
  const missing = items.find((item) => item.status !== 'FOUND')
  return { target: action.target, value: missing?.source_value ?? '' }
}
