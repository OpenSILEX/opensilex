<template>
  <div>
    <slot name="customErrors"></slot>

    <Button
      v-if="validationErrors.length"
      class="float-right"
      @click="csvExportInvalidData"
      variant="outline-info"
      >{{ $t("DataValidationReport.exportResult") }}</Button
    >

    <div v-if="isValid" class="validation-confirm-container">
      {{ $t("DataValidationReport.CSVIsValid") }}
    </div>

    <div class="error-container" v-if="tooLargeDataset">
      <div class="static-field">
        <span class="field-view-title">{{
          $t("DataValidationReport.tooLargeDataset", {
            sizeMax: sizeMax,
            nbLinesToImport: nbLinesToImport,
          })
        }}</span>
      </div>
    </div>
    <div class="error-container" v-if="validationErrors.length">
      <div class="static-field">
        <span class="field-view-title"
          >{{ $t("DataValidationReport.csvErrors") }}:</span
        >
      </div>
      <b-table-simple hover small responsive sticky-header>
        <b-thead head-variant="light">
          <b-tr>
            <b-th>{{ $t("DataValidationReport.errorLine") }}</b-th>
            <b-th>{{ $t("DataValidationReport.errorType") }}</b-th>
            <b-th>{{ $t("DataValidationReport.errorDetail") }}</b-th>
          </b-tr>
        </b-thead>
        <b-tbody>
          <slot v-for="(row, index) in validationErrors">
            <b-tr>
              <b-th :rowspan="row.listSize">{{
                typeof row.index == "number" ? row.index + 4 : row.index
              }}</b-th>
              <b-td>{{
                $t("DataValidationReport." + row.firstErrorType.type)
              }}</b-td>
              <b-td>
                <ul>
                  <li
                    v-for="validationErr in row.firstErrorType.validationErrors"
                    v-bind:key="
                      getErrKey(validationErr, row.firstErrorType.type)
                    "
                  >
                    {{
                      getValidationErrorDetail(
                        validationErr,
                        row.firstErrorType.type
                      )
                    }}
                  </li>
                </ul>
              </b-td>
            </b-tr>
            <b-tr
              v-for="(validationError, errorType) in row.list"
              v-bind:key="index + errorType"
            >
              <b-td>{{ $t("DataValidationReport." + errorType) }}</b-td>
              <b-td>
                <ul>
                  <li
                    v-for="validationErr in validationError"
                    v-bind:key="getErrKey(validationErr, errorType)"
                  >
                    {{ getValidationErrorDetail(validationErr, errorType) }}
                  </li>
                </ul>
              </b-td>
            </b-tr>
          </slot>
        </b-tbody>
      </b-table-simple>
    </div>
    <br />
  </div>
</template>

<script setup lang="ts">
import Button from "@/components/common/buttons/Button.vue";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import {computed, inject, ref} from "vue";
import {useI18n} from "vue-i18n";

withDefaults(
    defineProps<{
      validMessage?: string;
    }>(),
    {
      validMessage: "DataValidationReport.",
    }
);

const opensilex = inject<OpenSilexVuePlugin>('$opensilex')
const { t } = useI18n()

const checkErrors = ref<boolean>(false)
const validationErrors = ref<any[]>([])
const tooLargeDataset = ref<boolean>(false)
const nbLinesToImport = ref<number>(0)
const sizeMaxNumber = ref<number>(0);

// Getter isValid
const isValid = computed<boolean>(() => {
  return (
      checkErrors.value &&
      validationErrors.value.length === 0 &&
      !tooLargeDataset.value
  );
});

// Getter et setter sizeMax
const sizeMax = computed<number>({
  get: () => sizeMaxNumber.value,
  set: (sizeMax: number) => {
    sizeMaxNumber.value = sizeMax;
  },
});

function reset() {
    checkErrors.value = false;
    validationErrors.value = [];
  }


  /**
   * The only true button.
   * @dataErrors dataErrors validation model
   */
 function checkValidation(dataErrors) {
    tooLargeDataset.value = dataErrors.tooLargeDataset;
    nbLinesToImport.value = dataErrors.nbLinesToImport;
    let errors = dataErrors;
    checkErrors.value = true;
    let globalErrors = {};

    loadErrorType("datatypeErrors", errors, globalErrors);
    loadErrorType("uriNotFoundErrors", errors, globalErrors);
    loadErrorType("invalidURIErrors", errors, globalErrors);

    loadErrorType("missingRequiredValueErrors", errors, globalErrors);
    loadErrorType("invalidValueErrors", errors, globalErrors);
    loadErrorType("invalidObjectErrors", errors, globalErrors);
    loadErrorType("invalidTargetErrors", errors, globalErrors);
    loadErrorType("invalidDateErrors", errors, globalErrors);
    loadErrorType("invalidExperimentErrors", errors, globalErrors);
    loadErrorType("invalidDeviceErrors", errors, globalErrors);
    loadErrorType("deviceChoiceAmbiguityErrors", errors, globalErrors);
    loadErrorType("invalidDataTypeErrors", errors, globalErrors);
    loadErrorType("duplicatedDataErrors", errors, globalErrors);
    loadErrorType("duplicatedExperimentErrors", errors, globalErrors);
    loadErrorType("duplicatedObjectErrors", errors, globalErrors);
    loadErrorType("duplicatedTargetErrors", errors, globalErrors);
    loadErrorType("duplicatedDeviceErrors", errors, globalErrors);
    loadErrorType("invalidAnnotationErrors", errors, globalErrors);

    loadErrorType("alreadyExistingURIErrors", errors, globalErrors);
    loadErrorType("duplicateURIErrors", errors, globalErrors);

    let generalErrors: any = {
      index: t("DataValidationReport.generalErrors").toString(),
      list: {},
      listSize: 1,
      firstErrorType: null,
    };
    if (errors.missingHeaders.length > 0) {
      generalErrors.list.missingHeaders = errors.missingHeaders;
      generalErrors.listSize++;
      if (!generalErrors.firstErrorType) {
        generalErrors.firstErrorType = "missingHeaders";
      }
    }
    if (errors.invalidAnnotationErrors.length > 0) {
      generalErrors.list.invalidAnnotationErrors = errors.invalidAnnotationErrors;
      generalErrors.listSize++;
      if (!generalErrors.firstErrorType) {
        generalErrors.firstErrorType = "invalidAnnotationErrors";
      }
    }
    if (errors.emptyHeaders.length > 0) {
      generalErrors.list.emptyHeaders = errors.emptyHeaders;
      generalErrors.listSize++;
      if (!generalErrors.firstErrorType) {
        generalErrors.firstErrorType = "emptyHeaders";
      }
    }
    if (Object.keys(errors.invalidHeaderURIs).length > 0) {
      let invalidHeaderURIsList = Object.values(errors.invalidHeaderURIs);
      generalErrors.list.invalidHeaderURIs = invalidHeaderURIsList;
      generalErrors.listSize = generalErrors.listSize + 1;
      if (!generalErrors.firstErrorType) {
        generalErrors.firstErrorType = "invalidHeaderURIs";
      }
    }
    validationErrors.value = [];

    if (generalErrors.firstErrorType) {
      generalErrors.listSize--;
      let firstErrorType = generalErrors.firstErrorType;
      generalErrors.firstErrorType = {
        type: firstErrorType,
        validationErrors: generalErrors.list[firstErrorType],
      };
      delete generalErrors.list[firstErrorType];
      validationErrors.value.push(generalErrors);
    }
    for (let i in globalErrors) {
      if (globalErrors[i].firstErrorType) {
        globalErrors[i].listSize = globalErrors[i].listSize - 1;
        let firstErrorType = globalErrors[i].firstErrorType;
        globalErrors[i].firstErrorType = {
          type: firstErrorType,
          validationErrors: globalErrors[i].list[firstErrorType],
        };
        delete globalErrors[i].list[firstErrorType];
        validationErrors.value.push(globalErrors[i]);
      }
    }
  }

 function getValidationErrorDetail(validationError, errorType) {
    switch (errorType) {
      case "missingHeaders":
        return t(
          "DataValidationReport.validationErrorMissingHeaderMessage",
          {
            header: validationError,
          }
        );
      case "invalidHeaderURIs":
        return t(
          "DataValidationReport.validationErrorMissingHeaderMessage",
          {
            header: validationError,
          }
        );
      case "missingRequiredValueErrors":
        return t(
          "DataValidationReport.validationErrorMissingRequiredMessage",
          validationError
        );
      case "invalidObjectErrors":
        return t(
          "DataValidationReport.invalidObjectErrorMessage",
          validationError
        );
      case "invalidTargetErrors":
        return t(
        "DataValidationReport.invalidTargetErrorMessage",
        validationError
      );
      case "invalidDateErrors":
        return t(
          "DataValidationReport.invalidDateErrorMessage",
          validationError
        );
      case "invalidExperimentErrors":
        return t(
          "DataValidationReport.invalidExperimentErrorMessage",
          validationError
        );
      case "invalidDeviceErrors":
        return t(
          "DataValidationReport.invalidDeviceErrorMessage",
          validationError
        );
      case "deviceChoiceAmbiguityErrors":
        return t(
          "DataValidationReport.deviceChoiceAmbiguityErrors",
          validationError
        );
      case "duplicateURIErrors":
        return t(
          "DataValidationReport.validationErrorDuplicateURIMessage",
          validationError
        );
      case "datatypeErrors":
        return t(
          "DataValidationReport.validationErrorDatatypeMessage",
          validationError
        );
      case "duplicatedData":
        return t(
          "DataValidationReport.validationErrorDuplicatedDataMessage",
          validationError
        );
      case "duplicatedExperimentErrors":
        return t(
          "DataValidationReport.invalidExperimentErrorMessage",
          validationError
        );
      case "duplicatedObjectErrors":
        return t(
          "DataValidationReport.invalidObjectErrorMessage",
          validationError
        );
      case "duplicatedTargetErrors":
        return t(
          "DataValidationReport.invalidTargetErrorMessage",
          validationError
        );
      case "duplicatedDeviceErrors":
        return t(
          "DataValidationReport.invalidDeviceErrorMessage",
          validationError
        );
      case "invalidValueErrors":
        return t(
          "DataValidationReport.invalidValueErrorMessage",
          validationError
        );
      case "emptyHeaders":
        return t(
          "DataValidationReport.validationErrorMissingRequiredMessage",
          {
            header: "#" + validationError,
          }
        );

      default:
        return t(
          "DataValidationReport.validationErrorMessage",
          validationError
        );
    }
  }

  function loadErrorType(errorType, errors, globalErrors) {
    for (let i in errors[errorType]) {
      let errorList = errors[errorType][i];
      if (!Array.isArray(errorList)) {
        errorList = [errors[errorType][i]];
      }
      for (let j in errorList) {
        let errorItem = errorList[j];
        let rowIndex = errorItem.rowIndex;
        if (!globalErrors[rowIndex]) {
          globalErrors[rowIndex] = {
            index: rowIndex,
            list: {},
            listSize: 1,
          };
        }

        if (!globalErrors[rowIndex].list) {
          globalErrors[rowIndex].list = {};
        }

        if (!globalErrors[rowIndex].firstErrorType) {
          globalErrors[rowIndex].firstErrorType = errorType;
        }

        if (!globalErrors[rowIndex].list[errorType]) {
          globalErrors[rowIndex].list[errorType] = [];
          globalErrors[rowIndex].listSize = globalErrors[rowIndex].listSize + 1;
        }

        globalErrors[rowIndex].list[errorType].push(errorItem);
      }
    }
  }

 function getErrKey(validationError, errorType) {
    return (
      "validationError-" +
      errorType +
      "-" +
      validationError.rowIndex +
      "-" +
      validationError.colIndex
    );
  }

 function csvExportInvalidData() {
    let arrData = [
      [
        t("DataValidationReport.errorLine").toString(),
        t("DataValidationReport.errorType").toString(),
        t("DataValidationReport.errorDetail").toString(),
      ],
    ];
    validationErrors.value.forEach((row, index) => {
      row.index == "number" ? row.index + 3 : row.index;
      let line = [];
      line.push[t("DataValidationReport." + row.firstErrorType.type)];
      for (let validationErr in row.firstErrorType.validationErrors) {
        line.push(
          row.index,
          t("DataValidationReport." + row.firstErrorType.type),
          getValidationErrorDetail(
            row.firstErrorType.validationErrors[validationErr],
            row.firstErrorType.type
          )
        );
      }
      arrData.push(line);
    });
    console.debug("CSV validationReport data :", arrData);
    papa.download(
      papa.unparse(arrData, { delimiter: "," }),
      "validationReport"
    );
  }
</script>
<style scoped lang="scss">
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

.validation-warning-container {
  color: #a58524;
  font-weight: bold;
}

.multi-line-cell {
  white-space: pre;
}

.uri-cell {
  white-space: pre-warp;
}
</style>
<i18n>
en:
  DataValidationReport:
    errorLine: Line
    errorType: error Type
    errorDetail : Detail
    object-type: object type
    object-type-placeholder: Type here to search in object types
    expectedFormat: Expected CSV format
    objectURI: Object URI
    objectURIComment: You can set a custom URI or leave it empty to generate one.
    dataPlaceholder: Yourcan insert data from this row.
    ignoredFirstRows: First three rows of CSV content will be ignored.
    columnModification: You can change columns' order and add new ones as long as you don't change the ID of the first row.
    csvErrors: Error(s) detected in CSV file
    duplicatedDataErrors : Duplicate data in CSV File
    missingHeaders: Missing column headers
    emptyHeaders: Missing column headers
    invalidHeaderURIs: "Invalid header URI, valid URIs are required on the line N°1"
    datatypeErrors: Data type error
    uriNotFoundErrors: URI not found
    invalidURIErrors: Invalid URI
    invalidDataTypeErrors: Invalid datatype
    missingRequiredValueErrors: Missing required value
    invalidValueErrors: Invalid value
    invalidObjectErrors: Object name or uri not found in this experiment
    invalidTargetErrors: Target name or uri not found 
    invalidDateErrors: Invalid date format
    invalidExperimentErrors: Experiment name or uri not found
    invalidDeviceErrors: Device name or uri not found
    deviceChoiceAmbiguityErrors: Ambiguity in Device choice
    duplicatedExperimentErrors: Duplicate experiment name (you must use uri)
    duplicatedDeviceErrors: Duplicate device name (you must use uri)
    duplicatedObjectErrors: Duplicate object name (you must use uri)
    duplicatedTargetErrors: Duplicate target name (you must use uri)
    alreadyExistingURIErrors:  URI already existing
    duplicateURIErrors: Duplicate URI
    invalidDataTypeErrorMessage: Invalid value data type
    validationErrorMessage: "Column: '{header}' - Value: '{value}'"
    validationErrorMissingRequiredMessage: "Column: '{header}'"
    validationErrorMissingHeaderMessage: "Header: '{header}'"
    validationErrorDuplicateURIMessage: "Column: '{header}' - Value: '{value}' - Identical with row: '{previousRow}'"
    validationErrorDatatypeMessage: "Column: '{header}' - Value: '{value}' ({datatype})"
    validationErrorDuplicatedDataMessage : "Column: '{header}' - Value: '{value}'"
    invalidValueErrorMessage: "Column: '{header}' - Value: '{value}'"
    invalidObjectErrorMessage: "Column: scientific_object - Value: '{value}'" 
    invalidTargetErrorMessage: "Column: target - Value: '{value}'" 
    invalidExperimentErrorMessage: "Column: experiment - Value: '{value}'" 
    invalidDeviceErrorMessage: "Column: device - Value: '{value}'" 
    invalidDateErrorMessage: "Column: Date - Value: '{value}'" 
    CSVIsValid: Your CSV file has passed the first validation step, click OK to continue
    CSVIsInvalid: Your CSV has failed the first validation step
    csv-file-placeholder: Drop or select CSV file here...
    csv-file-drop-placeholder: Drop CSV file here...
    csv-file-select-button: Select
    downloadTemplate: Download CSV template
    separator: separator
    exportResult : "Export validation results"
    csv-import-success-message: CSV file has been imported sucessfully
    OBJECT_ID : "Scientific Object name / URI"
    OBJECT_NAME_AMBIGUITY_IN_GLOBAL_CONTEXT: "Object can't be referenced by name when importing outside of experimental context"
    generalErrors: "General errors"
    tooLargeDataset: "Too large data :'{sizeMax}' observations expected, '{nbLinesToImport}' observations submitted"
    invalidAnnotationErrors: "The target column must be filled to annotate."

fr:
  DataValidationReport:
    errorLine: Ligne
    errorType: Type d'erreur
    errorDetail : Détail
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
    duplicatedDataErrors: Données dupliquées
    invalidHeaderURIs: "URI d'en-tête invalide, des URIs valides sont requises sur la ligne n°1"
    datatypeErrors: Type de donnée invalide
    uriNotFoundErrors: URI non trouvée
    invalidURIErrors: URI invalide
    invalidDataTypeErrors: Type de données invalide
    missingRequiredValueErrors: Valeur obligatoire manquante
    invalidValueErrors: Valeur invalide
    invalidObjectErrors: Le nom ou l'uri de l'objet n'est pas présent dans cette expérimentation
    invalidTargetErrors: Le nom ou l'uri de la cible n'existe pas
    invalidDateErrors: Format de date invalide
    invalidExperimentErrors: Le nom ou l'uri de l'expérimentation n'existe pas
    invalidDeviceErrors: Le nom ou l'uri de l'appareil n'existe pas
    deviceChoiceAmbiguityErrors: Ambiguité sur le choix de l'appareil
    duplicatedExperimentErrors: Doublon sur le nom de l'experimentation (utilisez l'uri)
    duplicatedDeviceErrors: Doublon sur le nom de l'appareil (utilisez l'uri)
    duplicatedObjectErrors: Doublon sur le nom de l'objet (utilisez l'uri)
    duplicatedTargetErrors: Doublon sur le nom de la cible (utilisez l'uri)
    alreadyExistingURIErrors: URI déjà existante
    duplicateURIErrors: URI dupliquée
    validationErrorMessage: "Colonne: '{header}' - Valeur: '{value}'"
    validationErrorMissingRequiredMessage: "Colonne: '{header}'"
    validationErrorMissingHeaderMessage: "En-tête: '{header}'"
    validationErrorDuplicateURIMessage: "Colonne: '{header}' - Valeur: '{value}' - Identique à la ligne: '{previousRow}'"
    validationErrorDatatypeMessage: "Colonne: '{header}' - Valeur: '{value}' ({datatype})"
    validationErrorDuplicatedDataMessage : "Colonne: '{header}' - Valeur: '{value}'"
    invalidValueErrorMessage: "Colonne: '{header}' - Valeur: '{value}'"
    invalidObjectErrorMessage: "Colonne: scientific_object - Valeur: '{value}'" 
    invalidTargetErrorMessage: "Colonne: cible - Valeur: '{value}'" 
    invalidExperimentErrorMessage: "Colonne: experiment - Valeur: '{value}'" 
    invalidDeviceErrorMessage: "Colonne: appareil - Valeur: '{value}'" 
    invalidDateErrorMessage: "Colonne:  Date - Valeur: '{value}'" 
    invalidDataTypeErrorMessage: Le type de données attendu n'est pas valide
    CSVIsValid: La première étape de validation est un succès, cliquez OK pour continuer
    CSVIsInvalid: La première étape de validation a échoué
    csv-file-placeholder: Déposer ou choisir un fichier CSV ici...
    csv-file-drop-placeholder: Déposer le fichier CSV ici...
    csv-file-select-button: Parcourir
    downloadTemplate: Télécharger le modèle de fichier CSV
    separator: séparateur
    csv-import-success-message: Fichier CSV importé avec succès
    exportResult : "Exportation des Résultats de validation"
    OBJECT_ID: "Objet scientifique Nom/ URI"
    OBJECT_NAME_AMBIGUITY_IN_GLOBAL_CONTEXT: "L'objet scientifique ne peut être réferencé par son nom lors d'un import en dehors du cadre experimental"
    generalErrors: "Erreurs générales"
    tooLargeDataset: "Jeu de données trop volumineux : '{sizeMax}' observations attendues, '{nbLinesToImport}' observations soumises"
    invalidAnnotationErrors: "La colonne cible (Target) doit être remplie pour annoter."
</i18n>
