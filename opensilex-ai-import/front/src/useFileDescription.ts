import { ref } from 'vue'
import type {
  AiImportService,
  FileDescriptionDTO,
  FileDescriptionUpdateDTO,
  TypePropertyDTO
} from './lib'
import { AI_IMPORT_SERVICE, type OpenSilexPlugin } from './opensilexPlugin'

/**
 * What the user said of the files of a conversation, as the server reads it.
 *
 * The server is the one that knows what a choice contradicts, what a default is and what the
 * ontology offers: this keeps what it answered and sends what the user chose, and guesses nothing.
 *
 * @param sessionId the conversation, read when each call is made so that the composable follows it
 */
export function useFileDescription($opensilex: OpenSilexPlugin, sessionId: () => string) {
  const service = () => $opensilex.getService<AiImportService>(AI_IMPORT_SERVICE)

  const description = ref<FileDescriptionDTO | null>(null)
  const busy = ref(false)

  /**
   * The properties of a type, by kind of sheet and type: they come from the ontology, so one is
   * asked once and kept for as long as the page is open.
   */
  const propertiesByType = ref<{ [key: string]: TypePropertyDTO[] }>({})

  async function load(): Promise<void> {
    try {
      const http = await service().getDescription(sessionId())
      description.value = http.response.result
    } catch (error) {
      $opensilex.errorHandler(error)
    }
  }

  /**
   * Sends a change. The server applies all of it or none, and answers with the description as it
   * now stands.
   *
   * @return whether the change was applied
   */
  async function apply(update: Omit<FileDescriptionUpdateDTO, 'session_id'>): Promise<boolean> {
    busy.value = true
    try {
      const http = await service().describe({ session_id: sessionId(), ...update })
      description.value = http.response.result
      return true
    } catch (error) {
      $opensilex.errorHandler(error)
      return false
    } finally {
      busy.value = false
    }
  }

  async function loadProperties(kind: string, type: string): Promise<void> {
    const key = kind + '|' + type
    if (propertiesByType.value[key]) {
      return
    }
    try {
      const http = await service().getDescriptionTypeProperties(sessionId(), kind, type)
      propertiesByType.value = { ...propertiesByType.value, [key]: http.response.result ?? [] }
    } catch (error) {
      console.warn('Could not load the properties of', type, error)
    }
  }

  function propertiesOf(kind: string, type?: string): TypePropertyDTO[] {
    return type ? propertiesByType.value[kind + '|' + type] ?? [] : []
  }

  return { description, busy, load, apply, loadProperties, propertiesOf }
}
