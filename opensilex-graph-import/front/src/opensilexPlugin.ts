import type { ErrorDTO, GraphImportService } from './lib'

/**
 * What the host's upload helper answers. It resolves whatever the status: a refused file comes back
 * here with its status and the server's error, not as a rejected promise.
 */
export interface UploadAnswer<T> {
  metadata: { status: number }
  result?: T | ErrorDTO
}

/**
 * What the components of this module use of the platform's `$opensilex` plugin.
 *
 * The host's own type cannot be imported from an extension module, which is built apart from it:
 * this is the part of it these components rely on.
 */
export interface OpenSilexPlugin {
  getService<T = GraphImportService>(id: string): T
  errorHandler(error: unknown): void
  showErrorToast(message: string): void
  uploadFileToService<T>(
    path: string,
    files: { [part: string]: File },
    query?: { [name: string]: string } | null
  ): Promise<UploadAnswer<T>>
  /**
   * Downloads what a GET route answers, with the user's token, under the given name.
   *
   * @param path the route, after the API's base path
   */
  downloadFilefromService(
    path: string,
    fileName: string,
    extension: string,
    params?: { [name: string]: string } | null
  ): Promise<void>
  /** A URI in the platform's prefixed form, as its selectors spell the values they list. */
  getShortUri(uri: string): string
  /** Loads another module's bundle, which registers its components; rejects when it cannot be loaded. */
  loadModule(name: string): Promise<unknown>
  $i18n: { t(key: string, params?: { [name: string]: unknown }): string }
}

export const GRAPH_IMPORT_SERVICE = 'opensilex-graph-import.GraphImportService'

/** Where an import is opened and where its files are added, for the multipart upload helper. */
export const SESSIONS_PATH = '/graph-import/sessions'

/**
 * The file extensions an import accepts. The server holds the same list and refuses what is not on
 * it; this one only keeps the file picker from offering what would be refused.
 */
export const ACCEPTED_EXTENSIONS = ['xlsx', 'xlsm', 'xls', 'csv', 'tsv']
