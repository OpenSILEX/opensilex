import type { AiImportService, CreationProposalDTO, FacilityDraftDTO, FactorPlanDTO } from './lib'
import { AI_IMPORT_SERVICE, type OpenSilexPlugin } from './opensilexPlugin'

/**
 * Creating from a conversation without the assistant: what the files say, set out for the
 * platform's own forms and for the confirmation card. Each call only reads or drafts; nothing is
 * written until the user confirms in a form or on the card.
 *
 * @param sessionId the conversation, read when each call is made so that the composable follows it
 */
export function useCreationDraft($opensilex: OpenSilexPlugin, sessionId: () => string) {
  const service = () => $opensilex.getService<AiImportService>(AI_IMPORT_SERVICE)

  /**
   * The draft of a creation, made from what the files say: the card the assistant would have shown.
   *
   * @param values what the user already chose, by field name
   */
  async function draft(target: string, values?: { [field: string]: string }): Promise<CreationProposalDTO | null> {
    try {
      const http = await service().draft({ session_id: sessionId(), target, values })
      return http.response.result
    } catch (error) {
      $opensilex.errorHandler(error)
      return null
    }
  }

  /**
   * The factors of the files, each against the experiment it is declared in.
   *
   * @param experiment the experiment, or none for the one the files name
   */
  async function factorPlans(experiment?: string): Promise<FactorPlanDTO[]> {
    try {
      const http = await service().getFactorPlans(sessionId(), experiment)
      return http.response.result ?? []
    } catch (error) {
      $opensilex.errorHandler(error)
      return []
    }
  }

  /**
   * The facility a sheet of facilities describes under this name, with its type and properties.
   */
  async function facilityDraftOf(name: string): Promise<FacilityDraftDTO | undefined> {
    try {
      const http = await service().getFacilityDrafts(sessionId())
      return (http.response.result ?? []).find((candidate) => candidate.name === name)
    } catch (error) {
      // The form still opens with what the report says; a missing draft only costs the prefill.
      console.warn('Could not read the facilities the files describe', error)
      return undefined
    }
  }

  return { draft, factorPlans, facilityDraftOf }
}
