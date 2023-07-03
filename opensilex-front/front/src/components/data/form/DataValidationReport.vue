<template>
  <div>
    <slot name="customErrors"></slot>

    <b-button
      v-if="validationErrors.length"
      class="float-right"
      @click="csvExportInvalidData"
      variant="outline-info"
      >{{ $t("DataValidationReport.exportResult") }}</b-button
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

<script lang="ts">
import { Component, Prop, PropSync, Ref } from "vue-property-decorator";
import Vue from "vue";
import VueRouter from "vue-router";
import Oeso from "../../../ontologies/Oeso";

@Component
export default class DataValidationReport extends Vue {
  $opensilex: any;
  $papa: any;
  $t: any;

  checkErrors: boolean = false;

  validationErrors: any[] = [];
  tooLargeDataset: boolean = false;
  nbLinesToImport: number = 0;

  sizeMaxNumber: number;

  get isValid(): boolean {
    return (
      this.checkErrors &&
      this.validationErrors.length == 0 &&
      !this.tooLargeDataset
    );
  }

  set sizeMax(sizeMax: number) {
    this.sizeMaxNumber = sizeMax;
  }

  get sizeMax(): number {
    return this.sizeMaxNumber;
  }

  reset() {
    this.checkErrors = false;
    this.validationErrors = [];
  }

  @Prop({ default: "DataValidationReport." })
  validMessage;

  /**
   * The only true button.
   * @dataErrors dataErrors validation model
   */
  checkValidation(dataErrors) {
    console.debug("Verification data", dataErrors);
    this.tooLargeDataset = dataErrors.tooLargeDataset;
    this.nbLinesToImport = dataErrors.nbLinesToImport;
    let errors = dataErrors;
    this.checkErrors = true;
    let globalErrors = {};

    this.loadErrorType("datatypeErrors", errors, globalErrors);
    this.loadErrorType("uriNotFoundErrors", errors, globalErrors);
    this.loadErrorType("invalidURIErrors", errors, globalErrors);

    this.loadErrorType("missingRequiredValueErrors", errors, globalErrors);
    this.loadErrorType("invalidValueErrors", errors, globalErrors);
    this.loadErrorType("invalidObjectErrors", errors, globalErrors);
    this.loadErrorType("invalidTargetErrors", errors, globalErrors);
    this.loadErrorType("invalidDateErrors", errors, globalErrors);
    this.loadErrorType("invalidExperimentErrors", errors, globalErrors);
    this.loadErrorType("invalidDeviceErrors", errors, globalErrors);
    this.loadErrorType("deviceChoiceAmbiguityErrors", errors, globalErrors);
    this.loadErrorType("invalidDataTypeErrors", errors, globalErrors);
    this.loadErrorType("duplicatedDataErrors", errors, globalErrors);
    this.loadErrorType("duplicatedExperimentErrors", errors, globalErrors);
    this.loadErrorType("duplicatedObjectErrors", errors, globalErrors);
    this.loadErrorType("duplicatedTargetErrors", errors, globalErrors);
    this.loadErrorType("duplicatedDeviceErrors", errors, globalErrors);
    this.loadErrorType("invalidAnnotationErrors", errors, globalErrors);

    this.loadErrorType("alreadyExistingURIErrors", errors, globalErrors);
    this.loadErrorType("duplicateURIErrors", errors, globalErrors);

    let generalErrors: any = {
      index: this.$t("DataValidationReport.generalErrors").toString(),
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
    this.validationErrors = [];

    if (generalErrors.firstErrorType) {
      generalErrors.listSize--;
      let firstErrorType = generalErrors.firstErrorType;
      generalErrors.firstErrorType = {
        type: firstErrorType,
        validationErrors: generalErrors.list[firstErrorType],
      };
      delete generalErrors.list[firstErrorType];
      this.validationErrors.push(generalErrors);
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
        this.validationErrors.push(globalErrors[i]);
      }
    }
  }

  getValidationErrorDetail(validationError, errorType) {
    switch (errorType) {
      case "missingHeaders":
        return this.$t(
          "DataValidationReport.validationErrorMissingHeaderMessage",
          {
            header: validationError,
          }
        );
      case "invalidHeaderURIs":
        return this.$t(
          "DataValidationReport.validationErrorMissingHeaderMessage",
          {
            header: validationError,
          }
        );
      case "missingRequiredValueErrors":
        return this.$t(
          "DataValidationReport.validationErrorMissingRequiredMessage",
          validationError
        );
      case "invalidObjectErrors":
        return this.$t(
          "DataValidationReport.invalidObjectErrorMessage",
          validationError
        );
      case "invalidTargetErrors":
        return this.$t(
        "DataValidationReport.invalidTargetErrorMessage",
        validationError
      );
      case "invalidDateErrors":
        return this.$t(
          "DataValidationReport.invalidDateErrorMessage",
          validationError
        );
      case "invalidExperimentErrors":
        return this.$t(
          "DataValidationReport.invalidExperimentErrorMessage",
          validationError
        );
      case "invalidDeviceErrors":
        return this.$t(
          "DataValidationReport.invalidDeviceErrorMessage",
          validationError
        );
      case "deviceChoiceAmbiguityErrors":
        return this.$t(
          "DataValidationReport.deviceChoiceAmbiguityErrors",
          validationError
        );
      case "duplicateURIErrors":
        return this.$t(
          "DataValidationReport.validationErrorDuplicateURIMessage",
          validationError
        );
      case "datatypeErrors":
        return this.$t(
          "DataValidationReport.validationErrorDatatypeMessage",
          validationError
        );
      case "duplicatedData":
        return this.$t(
          "DataValidationReport.validationErrorDuplicatedDataMessage",
          validationError
        );
      case "duplicatedExperimentErrors":
        return this.$t(
          "DataValidationReport.invalidExperimentErrorMessage",
          validationError
        );
      case "duplicatedObjectErrors":
        return this.$t(
          "DataValidationReport.invalidObjectErrorMessage",
          validationError
        );
      case "duplicatedTargetErrors":
        return this.$t(
          "DataValidationReport.invalidTargetErrorMessage",
          validationError
        );
      case "duplicatedDeviceErrors":
        return this.$t(
          "DataValidationReport.invalidDeviceErrorMessage",
          validationError
        );
      case "invalidValueErrors":
        return this.$t(
          "DataValidationReport.invalidValueErrorMessage",
          validationError
        );
      case "emptyHeaders":
        return this.$t(
          "DataValidationReport.validationErrorMissingRequiredMessage",
          {
            header: "#" + validationError,
          }
        );

      default:
        return this.$t(
          "DataValidationReport.validationErrorMessage",
          validationError
        );
    }
  }

  private loadErrorType(errorType, errors, globalErrors) {
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

  getErrKey(validationError, errorType) {
    return (
      "validationError-" +
      errorType +
      "-" +
      validationError.rowIndex +
      "-" +
      validationError.colIndex
    );
  }

  csvExportInvalidData() {
    let arrData = [
      [
        this.$t("DataValidationReport.errorLine").toString(),
        this.$t("DataValidationReport.errorType").toString(),
        this.$t("DataValidationReport.errorDetail").toString(),
      ],
    ];
    this.validationErrors.forEach((row, index) => {
      row.index == "number" ? row.index + 3 : row.index;
      let line = [];
      line.push[this.$t("DataValidationReport." + row.firstErrorType.type)];
      for (let validationErr in row.firstErrorType.validationErrors) {
        line.push(
          row.index,
          this.$t("DataValidationReport." + row.firstErrorType.type),
          this.getValidationErrorDetail(
            row.firstErrorType.validationErrors[validationErr],
            row.firstErrorType.type
          )
        );
      }
      arrData.push(line);
    });
    console.debug("CSV validationReport data :", arrData);
    this.$papa.download(
      this.$papa.unparse(arrData, { delimiter: "," }),
      "validationReport"
    );
  }
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
</i18n>
