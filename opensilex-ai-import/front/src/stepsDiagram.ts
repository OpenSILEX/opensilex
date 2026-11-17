import type { ImportStepDTO } from './lib'

/**
 * The status of the declarations as a Mermaid flowchart: a node per step the files name, an arrow
 * from each step to the ones that wait for it, and a class per status so that what is done, what can
 * be done and what waits read at a glance. Pure: the same steps always give the same text, which is
 * also what the user can copy to put the diagram elsewhere.
 *
 * @param label the translated name of a step
 * @param counts the translated "done of total" shown under the name, or nothing for a step with none
 */
export function stepsDiagram(
  steps: ImportStepDTO[],
  label: (step: ImportStepDTO) => string,
  counts: (step: ImportStepDTO) => string
): string {
  const shown = steps.filter((step) => step.status !== 'NOT_APPLICABLE' && step.step)
  const names = new Set(shown.map((step) => step.step))
  const lines = ['flowchart LR']
  for (const step of shown) {
    const text = [label(step), counts(step)].filter((part) => !!part).map(escapeLabel).join('<br/>')
    lines.push(`  ${step.step}["${text}"]:::${styleOf(step.status)}`)
  }
  for (const step of shown) {
    for (const dependency of step.depends_on ?? []) {
      if (names.has(dependency)) {
        lines.push(`  ${dependency} --> ${step.step}`)
      }
    }
  }
  lines.push(
    '  classDef done fill:#d1e7dd,stroke:#198754,color:#0f5132',
    '  classDef todo fill:#cfe2ff,stroke:#0d6efd,color:#052c65',
    '  classDef waiting fill:#fff3cd,stroke:#ffc107,color:#664d03'
  )
  return lines.join('\n')
}

/**
 * The class a status is drawn with.
 */
function styleOf(status?: string): string {
  switch (status) {
    case 'DONE':
      return 'done'
    case 'POSSIBLE':
      return 'todo'
    default:
      return 'waiting'
  }
}

/**
 * A text safe inside a quoted Mermaid label: the quote and the angle brackets would end the label
 * or be read as markup, so they are written as the entities Mermaid understands.
 */
export function escapeLabel(text: string): string {
  return text.replace(/"/g, '#quot;').replace(/</g, '#lt;').replace(/>/g, '#gt;')
}
