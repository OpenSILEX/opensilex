/**
 * A sentence the report produced, rendered in the user's language.
 *
 * The server sends both a translation key with its parameters and the English text it gave the
 * language model. The key wins when there is one; the English is the fallback for a sentence a
 * profile composed itself, and showing that beats showing nothing.
 *
 * Shared rather than repeated: three panels render these, and a copy that drifted would show one
 * of them the untranslated text.
 */
export interface ReportMessage {
  key?: string
  params?: { [name: string]: string }
  text?: string
}

export function useReportMessage($opensilex: any) {
  return function say(message?: ReportMessage, fallback?: string): string {
    if (message?.key) {
      return String($opensilex.$i18n.t(message.key, message.params ?? {}))
    }
    return message?.text ?? fallback ?? ''
  }
}
