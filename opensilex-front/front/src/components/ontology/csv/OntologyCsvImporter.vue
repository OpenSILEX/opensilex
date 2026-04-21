<template>
  <n-modal v-model:show="showModal" :mask-closable="false">
    <n-card
      style="width: 1200px; max-width: 95vw"
      :bordered="false"
      role="dialog"
      aria-modal="true"
    >
      <template #header>
        <div class="container-fluid px-0">
          <div class="row mt-1 w-100 align-items-center">
            <div class="col-11">
              <i>
                <h4 class="mb-0">
                  <slot name="icon">
                    <opensilex-Icon icon="fa#eye" class="icon-title" />
                  </slot>
                  <span>{{ t(title) }}</span>
                </h4>
              </i>
            </div>
            <div class="col-1 text-end">
              <button
                type="button"
                class="btn-close"
                aria-label="Close"
                @click="hide"
              ></button>
            </div>
          </div>
        </div>
      </template>
      <hr>

      <div class="container-fluid">
        <label>
          {{ t(title) }}
          <span class="required">*</span>
        </label>

        <div class="row">
          <div class="col-md-4">
            <n-upload
              accept=".csv,text/csv"
              :max="1"
              :default-upload="false"
              @change="onFileChange"
            >
              <n-upload-dragger>
                <div>{{ t('OntologyCsvImporter.csv-file-placeholder') }}</div>
              </n-upload-dragger>
            </n-upload>
          </div>

          <slot name="generator"></slot>
        </div>

        <div class="row pt-3">
          <div class="col-md-12">
            <slot name="help"></slot>
          </div>
        </div>

        <!-- Validation Error  -->
        <div
          v-if="validationErrors && validationErrors.length > 0"
          class="error-container mt-3"
        >
          <div class="static-field">
            <span class="field-view-title">
              {{ t('OntologyCsvImporter.csvErrors') }}:
            </span>
          </div>
          <br>

          <div class="table-responsive">
            <table class="table table-striped table-hover table-sm align-middle">
              <thead class="table-light">
                <tr>
                  <th>{{ t('OntologyCsvImporter.line') }}</th>
                  <th>{{ t('OntologyCsvImporter.errorType') }}</th>
                  <th>{{ t('OntologyCsvImporter.detail') }}</th>
                </tr>
              </thead>
              <tbody>
                <template
                  v-for="row in validationErrors"
                  :key="`row-${row.index}`"
                >
                  <tr>
                    <th class="table-danger" :rowspan="row.listSize">
                      {{ row.index }}
                    </th>
                    <td>
                      <b>{{ t('OntologyCsvImporter.' + row.firstErrorType.type) }}</b>
                    </td>
                    <td>
                      <ul>
                        <li
                          v-for="validationErr in row.firstErrorType.validationErrors"
                          :key="getErrKey(validationErr, row.firstErrorType.type)"
                        >
                          {{
                            getValidationErrorDetail(
                              validationErr,
                              row.firstErrorType.type,
                              validation
                            )
                          }}
                        </li>
                      </ul>
                    </td>
                  </tr>

                  <tr
                    v-for="(validationError, errorType) in row.list"
                    :key="`${row.index}-${String(errorType)}`"
                  >
                    <td>
                      <b>{{ t('OntologyCsvImporter.' + String(errorType)) }}</b>
                    </td>
                    <td>
                      <ul>
                        <li
                          v-for="validationErr in validationError"
                          :key="getErrKey(validationErr, String(errorType))"
                        >
                          {{
                            getValidationErrorDetail(
                              validationErr,
                              String(errorType),
                              validation
                            )
                          }}
                        </li>
                      </ul>
                    </td>
                  </tr>
                </template>
              </tbody>
            </table>
          </div>
        </div>

        <div
          v-else-if="validationToken && nbLines"
          class="validation-confirm-container mt-3"
        >
          {{ t('OntologyCsvImporter.CSVIsValid', { nb_lines: nbLines }) }}
        </div>
      </div>

      <!-- Boutons Modal -->
      <template #footer>
        <div class="d-flex justify-content-end gap-2">
          <button type="button" class="btn btn-secondary" @click="hide">
            {{ t('component.common.close') }}
          </button>

          <button
            type="button"
            class="btn greenThemeColor"
            :disabled="!csvFile || (!!validationErrors && validationErrors.length > 0)"
            @click="importCSV"
          >
            {{ t('component.common.ok') }}
          </button>
        </div>
      </template>
    </n-card>
  </n-modal>
</template>

<script setup lang="ts">
import { ref, inject } from 'vue'
import { NModal, NCard, NUpload, NUploadDragger } from 'naive-ui'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import type { CSVValidationModel, OntologyService } from 'opensilex-core'
import type { CSVValidationDTO } from 'opensilex-core/model/cSVValidationDTO'
import type { UploadFileInfo } from 'naive-ui'
import { useI18n } from 'vue-i18n'

type FirstErrorType = {
  type: string
  validationErrors: any[]
}

type CsvError = {
  index: string
  list: Record<string, any[]>
  listSize: number
  firstErrorType: string | FirstErrorType | null | undefined
}

const props = withDefaults(defineProps<{
  baseType?: string
  title?: string
  validateCSV?: (csvFile: File) => Promise<any>
  uploadCSV?: (validationToken: string, csvFile: File) => Promise<any>
  successImportMsg?: string
  customColumns?: any[]
}>(), {
  title: 'OntologyCsvImporter.import',
  validateCSV: () => Promise.reject('validateCSV property is mandatory'),
  uploadCSV: () => Promise.reject('uploadCSV property is mandatory'),
  customColumns: () => []
})

const emit = defineEmits<{
  (e: 'csvValidated', response: any): void
  (e: 'csvImported', response: any): void
}>()

const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const { t } = useI18n()

const ontologyService = ref<OntologyService | null>(null)
const showModal = ref(false)

const rows = ref<any[]>([])
const csvFile = ref<File | null>(null)
const validationToken = ref<string | null>(null)
const nbLines = ref(0)
const validation = ref<CSVValidationDTO | null>(null)
const validationErrors = ref<CsvError[]>([])

ontologyService.value = $opensilex.getService<OntologyService>(
  'opensilex-core.OntologyService'
)

function resetState() {
  csvFile.value = null
  validationToken.value = null
  validationErrors.value = []
  nbLines.value = 0
  validation.value = null
}

function show() {
  resetState()
  showModal.value = true
}

function hide() {
  showModal.value = false
}

function getCellClass(index: string, _cell: any) {
  let classStr = 'multi-line-cell'
  if (index === 'URI') {
    classStr = 'uri-cell'
  }
  return classStr
}

function onFileChange(options: { file: UploadFileInfo }) {
  const file = options.file.file
  csvFile.value = file ?? null
  csvUploaded()
}

function csvUploaded() {
  validationToken.value = null
  validationErrors.value = []
  nbLines.value = 0

  if (csvFile.value != null) {
    props.validateCSV(csvFile.value).then((response) => {
      checkCSVValidation(response)
      if (validationToken.value) {
        emit('csvValidated', response)
      }
    })
  }
}

function importCSV() {
  validationErrors.value = []
  nbLines.value = 0

  if (!csvFile.value || !validationToken.value) return

  props.uploadCSV(validationToken.value, csvFile.value)
    .then((response) => {
      checkCSVValidation(response)
      if (validationToken.value) {
        $opensilex.showSuccessToast(
          response.result.nb_lines_imported +
            ' ' +
            t(props.successImportMsg || '')
        )
        emit('csvImported', response)
        hide()
      }
    })
    .catch($opensilex.errorHandler)
}

function checkCSVValidation(response: any) {
  const currentValidation: CSVValidationDTO = response.result
  validation.value = currentValidation
  validationToken.value = currentValidation.validation_token
  nbLines.value = currentValidation.nb_lines_imported

  if (!validationToken.value) {
    const errors = response.result.errors
    const errorsByRowIndex: Map<number, CsvError> = new Map()

    loadErrorType('datatypeErrors', errors, errorsByRowIndex)
    loadErrorType('invalidDateErrors', errors, errorsByRowIndex)
    loadErrorType('uriNotFoundErrors', errors, errorsByRowIndex)
    loadErrorType('invalidURIErrors', errors, errorsByRowIndex)
    loadErrorType('missingRequiredValueErrors', errors, errorsByRowIndex)
    loadErrorType('invalidValueErrors', errors, errorsByRowIndex)
    loadErrorType('alreadyExistingURIErrors', errors, errorsByRowIndex)
    loadErrorType('duplicateURIErrors', errors, errorsByRowIndex)
    loadErrorType('invalidRowSizeErrors', errors, errorsByRowIndex)
    loadErrorType('duplicateHeaderErrors', errors, errorsByRowIndex)

    const generalErrors: CsvError = {
      index: 'Erreurs générales',
      list: {},
      listSize: 1,
      firstErrorType: null
    }

    if (errors.missingHeaders.length > 0) {
      generalErrors.list.missingHeaders = errors.missingHeaders
      generalErrors.listSize++
      if (!generalErrors.firstErrorType) generalErrors.firstErrorType = 'missingHeaders'
    }

    if (errors.emptyHeaders.length > 0) {
      generalErrors.list.emptyHeaders = errors.emptyHeaders
      generalErrors.listSize++
      if (!generalErrors.firstErrorType) generalErrors.firstErrorType = 'emptyHeaders'
    }

    if (errors.invalidHeaderURIs.length > 0) {
      generalErrors.list.invalidHeaderURIs = errors.invalidHeaderURIs
      generalErrors.listSize++
      if (!generalErrors.firstErrorType) generalErrors.firstErrorType = 'invalidHeaderURIs'
    }

    if (errors.missingRequiredValueErrors.length > 0) {
      generalErrors.list.missingRequiredValueErrors = errors.missingRequiredValueErrors
      generalErrors.listSize++
      if (!generalErrors.firstErrorType) generalErrors.firstErrorType = 'missingRequiredValueErrors'
    }

    if (Object.keys(errors.invalidDuplicateHeaderByIndexes).length > 0) {
      const duplicateHeadersList = Object.values(errors.invalidDuplicateHeaderByIndexes)
      generalErrors.list.invalidHeaderURIs = duplicateHeadersList as any[]
      generalErrors.listSize = generalErrors.listSize + 1
      if (!generalErrors.firstErrorType) generalErrors.firstErrorType = 'duplicateHeaderErrors'
    }

    if (generalErrors.firstErrorType) {
      generalErrors.listSize--
      const firstErrorType = generalErrors.firstErrorType as string
      generalErrors.firstErrorType = {
        type: firstErrorType,
        validationErrors: generalErrors.list[firstErrorType]
      }
      delete generalErrors.list[firstErrorType]
      validationErrors.value.push(generalErrors)
    }

    errorsByRowIndex.forEach((error) => {
      if (error.firstErrorType) {
        error.listSize--
        const firstErrorType = error.firstErrorType as string
        error.firstErrorType = {
          type: firstErrorType,
          validationErrors: error.list[firstErrorType]
        }
        delete error.list[firstErrorType]
      }
    })

    validationErrors.value.push(...errorsByRowIndex.values())
  }
}

function getValidationErrorDetail(
  validationError: any,
  errorType: string,
  currentValidation: CSVValidationDTO | null
) {
  switch (errorType) {
    case 'missingHeaders':
      return t(
        'OntologyCsvImporter.validationErrorMissingHeaderMessage',
        { header: validationError }
      )
    case 'emptyHeaders':
      return t(
        'OntologyCsvImporter.validationErrorEmptyHeaderMessage',
        { header: validationError }
      )
    case 'missingRequiredValueErrors':
      return t(
        'OntologyCsvImporter.validationErrorMissingRequiredMessage',
        { header: validationError.header, message: validationError.message }
      )
    case 'duplicateURIErrors':
      return t(
        'OntologyCsvImporter.validationErrorDuplicateURIMessage',
        validationError
      )
    case 'datatypeErrors':
      return t(
        'OntologyCsvImporter.validationErrorDatatypeMessage',
        validationError
      )
    case 'invalidDateErrors':
      return t(validationError.message)
    case 'invalidRowSizeErrors':
      return t(
        'OntologyCsvImporter.validationErrorInvalidRowSizeErrorsMessage',
        {
          row_size: validationError.colIndex,
          header_size: currentValidation?.errors?.csvHeader?.realCsvHeaderLength
        }
      )
    case 'missingToValue':
      return t('OntologyCsvImporter.missingToValue', validationError)
    case 'duplicateHeaderErrors':
      return t(
        'OntologyCsvImporter.duplicateHeaderErrorMessage',
        validationError
      )
    default:
      return t(
        'OntologyCsvImporter.validationErrorMessage',
        validationError
      )
  }
}

function loadErrorType(
  errorType: string,
  errors: CSVValidationModel,
  globalErrors: Map<number, CsvError>
) {
  for (const i in (errors as any)[errorType]) {
    let errorList = (errors as any)[errorType][i]

    if (!Array.isArray(errorList)) {
      errorList = [(errors as any)[errorType][i]]
    }

    for (const j in errorList) {
      const errorItem = errorList[j]
      const rowIndex: number = errorItem.rowIndex

      let globalError = globalErrors.get(rowIndex)
      if (!globalError) {
        globalError = {
          index: '' + rowIndex,
          list: {},
          listSize: 1,
          firstErrorType: undefined
        }
        globalErrors.set(rowIndex, globalError)
      }

      if (!globalError.list) {
        globalError.list = {}
      }

      if (!globalError.firstErrorType) {
        globalError.firstErrorType = errorType
      }

      if (!globalError.list[errorType]) {
        globalError.list[errorType] = []
        globalError.listSize++
      }

      globalError.list[errorType].push(errorItem)
    }
  }
}

function getErrKey(validationError: any, errorType: string) {
  return (
    'validationError-' +
    errorType +
    '-' +
    validationError.rowIndex +
    '-' +
    validationError.colIndex
  )
}

defineExpose({
  show,
  hide
})
</script>

<style scoped>
.csv-import-helper {
  font-style: italic;
}

.row-header::first-letter {
  text-transform: none;
}

.csv-format {
  margin-top: 15px;
}

.error-container .field-view-title {
  color: rgb(245, 54, 92);
}

.validation-confirm-container {
  color: rgb(40, 167, 69);
  font-weight: bold;
}

.multi-line-cell {
  white-space: pre;
}

.uri-cell {
  white-space: pre-wrap;
}

.btn-close {
    width: 0.3em;
    height: 0.3em;
}

hr { 
    height: 2px; 
    background-color: darkgray; 
    border: none; 
} 
</style>

<i18n>
en:
  OntologyCsvImporter:
    import: CSV Import
    object-type: object type
    object-type-placeholder: Type here to search in object types
    expectedFormat: Expected CSV format
    objectURI: Object URI
    objectURIComment: You can set a custom URI or leave it empty to generate one.
    dataPlaceholder: Your can insert your data from this row.
    ignoredFirstRows: First three rows of CSV content will be ignored.
    columnModification: You can change columns' order and add new ones as long as you don't change the ID of the first row.
    csvErrors: Error(s) detected in CSV file
    missingHeaders: Missing column headers
    emptyHeaders: Header with empty column
    invalidHeaderURIs: Invalid header URI
    duplicateHeaderErrorMessage: An extra text based header is duplicated
    datatypeErrors: Data type error
    uriNotFoundErrors: URI not found
    invalidURIErrors: Invalid URI
    missingRequiredValueErrors: Missing required value
    invalidValueErrors: Invalid value
    alreadyExistingURIErrors: URI already existing
    duplicateURIErrors: Duplicate URI
    invalidRowSizeErrors: Invalid row size
    validationErrorMessage: "Column: '{header}' - Value: '{value}' - Details: '{message}'"
    validationErrorMissingRequiredMessage: "Column: '{header}' - Details : '{message}'"
    validationErrorMissingHeaderMessage: "Column: '{header}'"
    validationErrorEmptyHeaderMessage: "Header: column '{header}' - Empty column"
    validationErrorDuplicateURIMessage: "Header: column '{header}' - Value: '{value}' - Identical with row: '{previousRow}'"
    validationErrorDatatypeMessage: "Column: '{header}' - Value: '{value}' ({datatype})"
    validationErrorInvalidRowSizeErrorsMessage: "Invalid row size : {row_size}. Row size for this line must be equals to the CSV header size : {header_size}"
    CSVIsValid: Your CSV file has been successfully validated ({nb_lines} row(s) found), click OK to import it
    csv-file-placeholder: Drop or select CSV file here...
    csv-file-drop-placeholder: Drop CSV file here...
    csv-file-select-button: Select
    downloadTemplate: Download CSV template
    separator: separator
    csv-import-success-message: CSV file imported successfully
    line: Line
    errorType: "Error type"
    detail: Detail
    invalidDateErrors: Invalid date
    missingToValue: "Cannot declare a move with a 'From' value but without a 'To' value."
fr:
  OntologyCsvImporter:
    import: Import CSV
    object-type: type d'objet
    object-type-placeholder: Utiliser cette zone pour rechercher un type d'objet
    expectedFormat: Format de fichier CSV attendu
    objectURI: URI de l'objet
    objectURIComment: Vous pouvez définir une URI personnalisé ou laisser vide pour en générer une.
    dataPlaceholder: Vous pouvez insérer vos données à partir de cette ligne.
    ignoredFirstRows: Les trois premières lignes de contenu du CSV seront ignorées.
    columnModification: Vous pouvez changer l'ordre des colonnes et en ajouter tant que vous ne modifiez pas l'identifiant de la première ligne.
    csvErrors: Erreur(s) détectée(s) lors de la validation du fichier CSV
    missingHeaders: En-tête de colonne manquant
    emptyHeaders: En-tête avec une ou plusieurs colonne(s) vide(s)
    invalidHeaderURIs: URI d'en-tête invalide
    duplicateHeaderErrorMessage: An extra text based header is duplicated Un En-tête texte extra est dupliqué
    datatypeErrors: Type de donnée invalide
    uriNotFoundErrors: URI non trouvée
    invalidURIErrors: URI invalide
    missingRequiredValueErrors: Valeur obligatoire manquante
    invalidValueErrors: Valeur invalide
    alreadyExistingURIErrors: URI déjà existante
    duplicateURIErrors: URI dupliquée
    invalidRowSizeErrors: Taile de ligne invalide
    validationErrorMessage: "Colonne: '{header}' - Valeur: '{value}' - Détail: '{message}'"
    validationErrorMissingRequiredMessage: "Column: '{header}' - Details : '{message}'"
    validationErrorMissingHeaderMessage: "En-tête: '{header}'"
    validationErrorEmptyHeaderMessage: "En-tête: '{header}' - Colonne vide"
    validationErrorDuplicateURIMessage: "Colonne: '{header}' - Valeur: '{value}' - Identique à la ligne: '{previousRow}'"
    validationErrorDatatypeMessage: "Colonne: '{header}' - Valeur: '{value}' ({datatype})"
    validationErrorInvalidRowSizeErrorsMessage: "Taille de ligne invalide : {row_size}. Le nombre de colonne pour cette ligne doit être égal à la taille de l'entête CSV : {header_size}"
    CSVIsValid: Votre fichier CSV est valide ({nb_lines} ligne lue(s)), cliquer sur OK pour l'importer
    csv-file-placeholder: Déposer ou choisir un fichier CSV ici...
    csv-file-drop-placeholder: Déposer le fichier CSV ici...
    csv-file-select-button: Parcourir
    downloadTemplate: Télécharger le modèle de fichier CSV
    separator: séparateur
    csv-import-success-message: Fichier CSV importé
    line: Ligne
    errorType: "Type d'erreur"
    detail: Détail
    invalidDateErrors: Date invalide
    missingToValue: "Impossible de déclarer un déplacement avec une valeur 'De' mais sans valeur 'Vers'."
</i18n>