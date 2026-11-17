import type { ReportMessageDTO } from './lib'
import type { OpenSilexPlugin } from './opensilexPlugin'

/** Where the server's report keys live in the module's messages. */
const REPORT_PREFIX = 'GraphImport.report.'

/**
 * The sentence a report message stands for, in the user's language: its key translated with its
 * parameters, or the English text the server sent when the key has no translation yet.
 */
export function reportText($opensilex: OpenSilexPlugin, message: ReportMessageDTO | undefined): string {
  if (!message) {
    return ''
  }
  if (message.key) {
    const key = REPORT_PREFIX + message.key
    const translated = $opensilex.$i18n.t(key, message.params ?? {})
    if (translated !== key) {
      return translated
    }
  }
  return message.text ?? ''
}

/** How a choice was reached, as the server names it. */
export type ChoiceStatus = 'CHOSEN' | 'DECIDED' | 'SUGGESTED' | 'UNDECIDED' | 'LEFT_OUT'

/** A weight as the evidence lists show it: "+0.70", "-0.50". */
export function signed(weight: number | undefined): string {
  const value = weight ?? 0
  return (value >= 0 ? '+' : '') + value.toFixed(2)
}

/** A score as a percentage: "80 %". */
export function percent(score: number | undefined): string {
  return Math.round((score ?? 0) * 100) + ' %'
}
