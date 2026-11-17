import { ref } from 'vue'
import type { AiImportService, FileTemplateDTO } from './lib'
import { AI_IMPORT_SERVICE, type OpenSilexPlugin } from './opensilexPlugin'

const TEMPLATES_PATH = '/ai-import/templates'

/**
 * The templates the user may use — their own and those shared with them — and what they can do
 * with them. Every change goes to the server, which answers with the template as it now stands: a
 * template that is not the user's is never changed from here, and the server says so.
 */
export function useFileTemplates($opensilex: OpenSilexPlugin) {
  const service = () => $opensilex.getService<AiImportService>(AI_IMPORT_SERVICE)

  const templates = ref<FileTemplateDTO[]>([])

  async function load(): Promise<void> {
    try {
      const http = await service().getTemplates()
      templates.value = http.response.result ?? []
    } catch (error) {
      $opensilex.errorHandler(error)
    }
  }

  async function update(id: string, change: { name?: string; shared?: boolean }): Promise<void> {
    try {
      await service().updateTemplate({ id, ...change })
      await load()
    } catch (error) {
      $opensilex.errorHandler(error)
    }
  }

  async function remove(id: string): Promise<void> {
    try {
      await service().deleteTemplate(id)
      await load()
    } catch (error) {
      $opensilex.errorHandler(error)
    }
  }

  /**
   * Uploads a template written as CSVW: it becomes a new template of the user.
   */
  async function importCsvw(file: File): Promise<void> {
    try {
      // Multipart, so it goes through the upload helper rather than the generated client.
      await $opensilex.uploadFileToService(TEMPLATES_PATH + '/import', { file })
      await load()
    } catch (error) {
      $opensilex.errorHandler(error)
    }
  }

  /**
   * Downloads a template through the platform's own download helper, which carries the token.
   *
   * @param format csvw, which the module reads back, or rml, which it only writes
   */
  async function download(template: FileTemplateDTO, format: 'csvw' | 'rml'): Promise<void> {
    const extension = format === 'csvw' ? 'csvm.json' : 'rml.ttl'
    try {
      await $opensilex.downloadFilefromService(
        TEMPLATES_PATH + '/' + encodeURIComponent(template.id ?? '') + '/' + format,
        template.name ?? 'template',
        extension
      )
    } catch (error) {
      $opensilex.errorHandler(error)
    }
  }

  return { templates, load, update, remove, importCsvw, download }
}
