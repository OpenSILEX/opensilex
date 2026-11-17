import type { AiImportService } from './lib'

/**
 * What the components of this module use of the platform's `$opensilex` plugin.
 *
 * The host's own type cannot be imported from an extension module, which is built apart from it:
 * this is the part of it these components rely on, so that they are typed without a dependency on
 * the host.
 */
export interface OpenSilexPlugin {
  getService<T = AiImportService>(id: string): T
  errorHandler(error: unknown): void
  showErrorToast(message: string): void
  uploadFileToService(
    path: string,
    files: { [part: string]: File },
    query?: { [name: string]: string } | null
  ): Promise<{ result?: unknown }>
  downloadFilefromService(
    path: string,
    fileName: string,
    extension: string,
    params?: { [name: string]: string } | null
  ): Promise<void>
  $i18n: { t(key: string, params?: { [name: string]: unknown }): string }
}

export const AI_IMPORT_SERVICE = 'opensilex-ai-import.AiImportService'

/**
 * The file extensions an import accepts. The server holds the same list (`SheetSources`) and
 * refuses what is not on it; this one only keeps the file picker from offering what would be refused.
 */
export const ACCEPTED_EXTENSIONS = ['xlsx', 'xlsm', 'xls', 'csv', 'tsv']
