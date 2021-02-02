 <template>
  <b-modal
    ref="OntologyCsvImporter"
    @ok.prevent="importCSV"
    size="xl"
    :static="true"
    :ok-disabled="!csvFile || !!validationErrors"
  >
    <template v-slot:modal-ok>{{ $t("component.common.ok") }}</template>
    <template v-slot:modal-cancel>{{ $t("component.common.cancel") }}</template>

    <template class="mt-1" v-slot:modal-header>
      <b-row class="mt-1" style="width: 100%">
        <b-col cols="11">
          <i>
            <h4>
              <slot name="icon">
                <opensilex-Icon icon="fa#eye" class="icon-title" />
              </slot>
              <span>{{ $t("OntologyCsvImporter.import") }}</span>
            </h4>
          </i>
        </b-col>
        <b-col cols="1">
          <!-- Emulate built in modal header close button action -->
          <button
            type="button"
            class="close"
            @click="OntologyCsvImporter.hide()"
            data-dismiss="modal"
            aria-label="Close"
          >
            <span aria-hidden="true">&times;</span>
          </button>
        </b-col>
      </b-row>
    </template>
    <div class="container-fluid">
      <ValidationObserver ref="validatorRef">
        <label
          >{{ $t("OntologyCsvImporter.import") }}
          <span class="required">*</span></label
        >
        <div class="row">
          <div class="col-md-4">
            <b-form-file
              size="sm"
              ref="inputFile"
              accept="text/csv, .csv"
              @input="csvUploaded"
              v-model="csvFile"
              :placeholder="$t('OntologyCsvImporter.csv-file-placeholder')"
              :drop-placeholder="
                $t('OntologyCsvImporter.csv-file-drop-placeholder')
              "
              :browse-text="$t('OntologyCsvImporter.csv-file-select-button')"
            ></b-form-file>
          </div>
          <slot name="generator"></slot>
        </div>
        <div class="row" style="padding-top: 15px">
          <div class="col-md-12">
            <slot name="help"></slot>
          </div>
        </div>
        <div class="error-container" v-if="validationErrors">
          <div class="static-field">
            <span class="field-view-title"
              >{{ $t("OntologyCsvImporter.csvErrors") }}:</span
            >
          </div>

          <b-table-simple hover small responsive sticky-header>
            <b-thead head-variant="light">
              <b-tr>
                <b-th>Ligne</b-th>
                <b-th>Type d'erreur</b-th>
                <b-th>Détail</b-th>
              </b-tr>
            </b-thead>
            <b-tbody>
              <slot v-for="(row, index) in validationErrors">
                <b-tr>
                  <b-th :rowspan="row.listSize">{{ row.index }}</b-th>
                  <b-td>{{
                    $t("OntologyCsvImporter." + row.firstErrorType.type)
                  }}</b-td>
                  <b-td>
                    <ul>
                      <li
                        v-for="validationErr in row.firstErrorType
                          .validationErrors"
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
                  <b-td>{{ $t("OntologyCsvImporter." + errorType) }}</b-td>
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
        <div class="validation-confirm-container" v-else-if="validationToken">
          {{ $t("OntologyCsvImporter.CSVIsValid") }}
        </div>
      </ValidationObserver>
    </div>
  </b-modal>
</template>

<script lang="ts">
import { Component, Prop, PropSync, Ref } from "vue-property-decorator";
import Vue from "vue";
import { OntologyService } from "opensilex-core/index";

@Component
export default class OntologyCsvImporter extends Vue {
  $opensilex: any;
  ontologyService: OntologyService;
  $t: any;

  @Ref("OntologyCsvImporter")
  readonly OntologyCsvImporter!: any;

  @Ref("validatorRef") readonly validatorRef!: any;
  @Ref("inputFile") readonly inputFile!: any;

  @Prop()
  baseType;

  @Prop({
    default: () => Promise.reject("validateCSV property is mandatory"),
  })
  validateCSV;

  @Prop({
    default: () => Promise.reject("uploadCSV property is mandatory"),
  })
  uploadCSV;

  @Prop({
    default: () => [],
  })
  customColumns;

  show() {
    this.validatorRef.reset();
    this.csvFile = null;
    this.validationToken = null;
    this.validationErrors = null;
    this.OntologyCsvImporter.show();
  }

  hide() {
    this.OntologyCsvImporter.hide();
  }

  mounted() {
    this.ontologyService = this.$opensilex.getService(
      "opensilex-core.OntologyService"
    );
  }

  getCellClass(index, cell) {
    let classStr = "multi-line-cell";
    if (index == "URI") {
      classStr = "uri-cell";
    }

    return classStr;
  }

  rows = [];

  csvFile = null;
  validationToken = null;

  csvUploaded() {
    this.validationToken = null;
    this.validationErrors = null;
    if (this.csvFile != null) {
      this.validateCSV(this.csvFile).then(this.checkCSVValidation);
    }
  }

  importCSV() {
    this.validationErrors = null;
    this.uploadCSV(this.validationToken, this.csvFile)
      .then((response) => {
        this.checkCSVValidation(response);
        if (this.validationToken) {
          this.$emit("csvImported", response);
          this.hide();
        }
      })
      .catch(this.$opensilex.errorHandler);
  }

  checkCSVValidation(response) {
    this.validationToken = response.result.validationToken;
    if (!this.validationToken) {
      let errors = response.result.errors;

      let globalErrors = {};

      this.loadErrorType("datatypeErrors", errors, globalErrors);
      this.loadErrorType("uriNotFoundErrors", errors, globalErrors);
      this.loadErrorType("invalidURIErrors", errors, globalErrors);
      this.loadErrorType("missingRequiredValueErrors", errors, globalErrors);
      this.loadErrorType("invalidValueErrors", errors, globalErrors);
      this.loadErrorType("alreadyExistingURIErrors", errors, globalErrors);
      this.loadErrorType("duplicateURIErrors", errors, globalErrors);

      let generalErrors: any = {
        index: "Erreurs générales",
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

      if (errors.invalidHeaderURIs.length > 0) {
        generalErrors.list.invalidHeaderURIs = errors.invalidHeaderURIs;
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
        globalErrors[i].index = globalErrors[i].index + 1;
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
  }

  validationErrors = null;

  getValidationErrorDetail(validationError, errorType) {
    console.error(errorType, validationError);
    switch (errorType) {
      case "missingHeaders":
        return this.$t(
          "OntologyCsvImporter.validationErrorMissingHeaderMessage",
          { header: validationError }
        );
      case "missingRequiredValueErrors":
        return this.$t(
          "OntologyCsvImporter.validationErrorMissingRequiredMessage",
          validationError
        );
      case "duplicateURIErrors":
        return this.$t(
          "OntologyCsvImporter.validationErrorDuplicateURIMessage",
          validationError
        );
      case "datatypeErrors":
        return this.$t(
          "OntologyCsvImporter.validationErrorDatatypeMessage",
          validationError
        );
      default:
        return this.$t(
          "OntologyCsvImporter.validationErrorMessage",
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

.multi-line-cell {
  white-space: pre;
}

.uri-cell {
  white-space: pre-warp;
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
    csvErrors: Error detected in CSV file
    missingHeaders: Missing column headers
    invalidHeaderURIs: Invalid header URI
    datatypeErrors: Data type error
    uriNotFoundErrors: URI not found
    invalidURIErrors: Invalid URI
    missingRequiredValueErrors: Missing required value
    invalidValueErrors: Invalid value
    alreadyExistingURIErrors:  URI already existing
    duplicateURIErrors: Duplicate URI
    validationErrorMessage: "Column: '{header}' - Value: '{value}'"
    validationErrorMissingRequiredMessage: "Column: '{header}'"
    validationErrorMissingHeaderMessage: "Header: '{header}'"
    validationErrorDuplicateURIMessage: "Column: '{header}' - Value: '{value}' - Identical with row: '{previousRow}'"
    validationErrorDatatypeMessage: "Column: '{header}' - Value: '{value}' ({datatype})"
    CSVIsValid: Your CSV file has been successfully validated, click OK to import it
    csv-file-placeholder: Drop or select CSV file here...
    csv-file-drop-placeholder: Drop CSV file here...
    csv-file-select-button: Select
    downloadTemplate: Download CSV template
    separator: separator
    csv-import-success-message: CSV file imported sucessfully

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
    csvErrors: Erreurs détectées lors de la validation du fichier CSV
    missingHeaders: En-tête de colonne manquant
    invalidHeaderURIs: URI d'en-tête invalide
    datatypeErrors: Type de donnée invalide
    uriNotFoundErrors: URI non trouvée
    invalidURIErrors: URI invalide
    missingRequiredValueErrors: Valeur obligatoire manquante
    invalidValueErrors: Valeur invalide
    alreadyExistingURIErrors: URI déjà existante
    duplicateURIErrors: URI dupliquée
    validationErrorMessage: "Colonne: '{header}' - Valeur: '{value}'"
    validationErrorMissingRequiredMessage: "Colonne: '{header}'"
    validationErrorMissingHeaderMessage: "En-tête: '{header}'"
    validationErrorDuplicateURIMessage: "Colonne: '{header}' - Valeur: '{value}' - Identique à la ligne: '{previousRow}'"
    validationErrorDatatypeMessage: "Colonne: '{header}' - Valeur: '{value}' ({datatype})"
    CSVIsValid: Votre fichier CSV est valide, cliquer sur OK pour l'importer
    csv-file-placeholder: Déposer ou choisir un fichier CSV ici...
    csv-file-drop-placeholder: Déposer le fichier CSV ici...
    csv-file-select-button: Parcourir
    downloadTemplate: Télécharger le modèle de fichier CSV
    separator: séparateur
    csv-import-success-message: Fichier CSV importé
</i18n>
