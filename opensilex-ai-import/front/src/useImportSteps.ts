import { ref } from 'vue'
import type { AiImportService, ImportStepDTO } from './lib'
import { AI_IMPORT_SERVICE, type OpenSilexPlugin } from './opensilexPlugin'

/**
 * Where each step of the import stands, as the server works it out. Nothing is computed here: the
 * order, what waits for what, and what is done all come from the server's table of steps.
 */
export function useImportSteps($opensilex: OpenSilexPlugin, sessionId: () => string) {
  const steps = ref<ImportStepDTO[]>([])

  async function load(): Promise<void> {
    try {
      const http = await $opensilex.getService<AiImportService>(AI_IMPORT_SERVICE).getSteps(sessionId())
      steps.value = http.response.result ?? []
    } catch (error) {
      // The path is a guide: without it the report and the creations still work.
      console.warn('Could not load the steps of the import', error)
    }
  }

  return { steps, load }
}
