 <template>
  <span>
    <opensilex-CreateButton @click="show" label="OntologyCsvImporter.import"></opensilex-CreateButton>
    <b-modal
      ref="OntologyCsvImporter"
      @ok.prevent="importCSV"
      size="xl"
      :static="true"
      :ok-disabled="!csvFile || !!validationErrors"
    >
      <template v-slot:modal-ok>{{$t('component.common.ok')}}</template>
      <template v-slot:modal-cancel>{{$t('component.common.cancel')}}</template>

      <template v-slot:modal-title>
        <i>
          <slot name="icon">
            <opensilex-Icon icon="fa#eye" class="icon-title" />
          </slot>
          <span>{{$t("OntologyCsvImporter.import")}}</span>
        </i>
      </template>
      <ValidationObserver ref="validatorRef">
        <!-- Type -->
        <opensilex-TypeForm
          :type.sync="objectType"
          @update:type="typeSwitch"
          :baseType="baseType"
          :required="true"
          label="OntologyCsvImporter.object-type"
          placeholder="OntologyCsvImporter.object-type-placeholder"
        ></opensilex-TypeForm>

        <div v-if="objectType && fields.length > 0">
          <div class="row">
            <div class="col-md-4">
              <b-form-file
                size="sm"
                ref="inputFile"
                accept="text/csv, .csv"
                @input="csvUploaded"
                v-model="csvFile"
                :placeholder="$t('OntologyCsvImporter.csv-file-placeholder')"
                :drop-placeholder="$t('OntologyCsvImporter.csv-file-drop-placeholder')"
                :browse-text="$t('OntologyCsvImporter.csv-file-select-button')"
              ></b-form-file>
            </div>
            <div class="col">
              <b-button @click="downloadCSVTemplate">{{$t("OntologyCsvImporter.downloadTemplate")}}</b-button>
            </div>
          </div>
          <div class="row">
            <div class="col-md-12 csv-format">
              <div class="static-field">
                <span class="field-view-title">{{$t("OntologyCsvImporter.expectedFormat")}}:</span>
              </div>

              <b-table-simple hover small responsive>
                <b-thead>
                  <b-tr>
                    <b-th>1</b-th>
                    <b-th
                      v-for="field in fields"
                      v-bind:key="field.key"
                      class="row-header"
                    >{{field.key}}</b-th>
                  </b-tr>
                </b-thead>
                <b-tbody>
                  <b-tr v-for="(row, index) in rows" v-bind:key="index">
                    <b-th>{{index + 2}}</b-th>
                    <b-td
                      :class="getCellClass(cellIndex, cell)"
                      v-for="(cell, cellIndex) in row"
                      v-bind:key="cell"
                    >{{cell}}</b-td>
                  </b-tr>
                </b-tbody>
                <b-tfoot>
                  <b-tr>
                    <b-th variant="secondary">4</b-th>
                    <b-td :colspan="fields.length" variant="secondary">
                      <div class="csv-import-helper">
                        {{$t("OntologyCsvImporter.dataPlaceholder")}}
                        <br />
                        {{$t("OntologyCsvImporter.ignoredFirstRows")}}
                        <br />
                        {{$t("OntologyCsvImporter.columnModification")}}
                      </div>
                    </b-td>
                  </b-tr>
                </b-tfoot>
              </b-table-simple>
            </div>
          </div>
          <div class="error-container" v-if="validationErrors">
            <div class="static-field">
              <span class="field-view-title">{{$t("OntologyCsvImporter.csvErrors")}}:</span>
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
                    <b-th :rowspan="row.listSize">{{row.index + 3}}</b-th>
                    <b-td>{{$t("OntologyCsvImporter." + row.firstErrorType.type)}}</b-td>
                    <b-td>
                      <ul>
                        <li
                          v-for="validationErr in row.firstErrorType.validationErrors"
                          v-bind:key="getErrKey(validationErr, row.firstErrorType.type)"
                        >{{getValidationErrorDetail(validationErr, row.firstErrorType.type)}}</li>
                      </ul>
                    </b-td>
                  </b-tr>
                  <b-tr
                    v-for="(validationError, errorType) in row.list"
                    v-bind:key="index + errorType"
                  >
                    <b-td>{{$t("OntologyCsvImporter." + errorType)}}</b-td>
                    <b-td>
                      <ul>
                        <li
                          v-for="validationErr in validationError"
                          v-bind:key="getErrKey(validationErr, errorType)"
                        >{{getValidationErrorDetail(validationErr, errorType)}}</li>
                      </ul>
                    </b-td>
                  </b-tr>
                </slot>
              </b-tbody>
            </b-table-simple>
          </div>
          <div
            class="validation-confirm-container"
            v-else-if="validationToken"
          >{{$t("OntologyCsvImporter.CSVIsValid")}}</div>
        </div>
      </ValidationObserver>
    </b-modal>
  </span>
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

  objectType = null;

  @Prop()
  baseType;

  @Prop({
    default: () => Promise.reject("validateCSV property is mandatory")
  })
  validateCSV;

  @Prop({
    default: () => Promise.reject("uploadCSV property is mandatory")
  })
  uploadCSV;

  @Prop({
    default: () => []
  })
  customColumns;

  show() {
    this.objectType = null;
    this.validatorRef.reset();
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

  downloadCSVTemplate() {
    let csvContent = "data:text/csv;charset=utf-8,";

    let columnIDs = [];
    let rowsValues = [];

    for (let i in this.fields) {
      let fieldKey = this.fields[i].key;
      columnIDs.push(fieldKey);

      for (let j in this.rows) {
        let row = this.rows[j];
        let value = row[fieldKey];
        if (!rowsValues[j]) {
          rowsValues[j] = [];
        }
        rowsValues[j].push(row[fieldKey]);
      }
    }

    let csvRows = [];
    csvRows.push(columnIDs.join(","));

    for (let i in rowsValues) {
      let rowValues = rowsValues[i];
      csvRows.push('"' + rowValues.join('","') + '"');
    }

    csvContent += csvRows.join("\r\n");

    let link = document.createElement("a");
    link.setAttribute("href", encodeURI(csvContent));
    link.setAttribute("download", "template.csv");
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }

  getFieldDescription(field, lineSeparator) {
    let fieldDescription = "";
    if (field.comment) {
      fieldDescription +=
        this.$t("component.common.comment") +
        ": " +
        field.comment +
        lineSeparator;
    }
    if (field.propertyType == "OBJECT") {
      let targetName = "URI";
      fieldDescription +=
        this.$t("component.common.type") + ": " + targetName + lineSeparator;
    } else if (field.propertyType == "DATA") {
      if (field.targetProperty) {
        let targetType = this.$opensilex.getType(field.targetProperty);
        let targetName = "";
        if (targetType) {
          targetName = this.$t(targetType.labelKey);
        }
        fieldDescription +=
          this.$t("component.common.type") + ": " + targetName + lineSeparator;
      }
    } else {
      fieldDescription +=
        this.$t("component.common.type") + ": " + field.type + lineSeparator;
    }

    fieldDescription +=
      this.$t("OntologyClassDetail.required") +
      ": " +
      (field.isRequired
        ? this.$t("component.common.yes")
        : this.$t("component.common.no")) +
      lineSeparator;

    fieldDescription +=
      this.$t("OntologyClassDetail.list") +
      ": " +
      (field.isList
        ? this.$t("component.common.yes") +
          " (" +
          this.$t("OntologyCsvImporter.separator") +
          ": |)"
        : this.$t("component.common.no")) +
      lineSeparator;

    return fieldDescription;
  }

  fields = [];
  rows = [];

  csvFile = null;
  validationToken = null;

  typeSwitch() {
    this.fields = [];
    this.rows = [];

    this.csvFile = null;
    this.validationToken = null;
    this.validationErrors = null;

    if (this.objectType != null) {
      return this.$opensilex
        .getService("opensilex.VueJsOntologyExtensionService")
        .getClassProperties(this.objectType, this.baseType)
        .then(http => {
          let classModel: any = http.response.result;

          this.fields = [
            {
              key: "URI",
              label: "URI"
            }
          ];

          let propertiesByURI = {};

          for (let i in classModel.dataProperties) {
            let dataProperty = classModel.dataProperties[i];

            this.fields.push({
              key: dataProperty.property,
              label: dataProperty.property
            });

            dataProperty.propertyType = "DATA";
            propertiesByURI[dataProperty.property] = dataProperty;
          }

          for (let i in classModel.objectProperties) {
            let objectProperty = classModel.objectProperties[i];

            this.fields.push({
              key: objectProperty.property,
              label: objectProperty.property
            });

            objectProperty.propertyType = "OBJECT";
            propertiesByURI[objectProperty.property] = objectProperty;
          }

          let nameRow = {};
          let commentRow = {};

          // TODO sort by properties order

          this.fields.forEach(field => {
            if (field.key == "URI") {
              nameRow[field.key] = this.$t("OntologyCsvImporter.objectURI");
              commentRow[field.key] = this.$t(
                "OntologyCsvImporter.objectURIComment"
              ).replace("\\n", "\n");
            } else if (propertiesByURI[field.key]) {
              let fieldKey = field.key;
              let property = propertiesByURI[fieldKey];

              nameRow[fieldKey] = property.name;
              commentRow[fieldKey] = this.getFieldDescription(property, "\n");
            }
          });

          for (let i in this.customColumns) {
            let customColumn = this.customColumns[i];

            this.fields.push({
              key: customColumn.id,
              label: customColumn.id
            });

            nameRow[customColumn.id] = customColumn.label;
            commentRow[customColumn.id] = this.getFieldDescription(
              customColumn,
              "\n"
            );
          }

          this.rows = [nameRow, commentRow];
        });
    }
  }

  csvUploaded() {
    this.validationToken = null;
    this.validationErrors = null;

    this.validateCSV(this.objectType, this.csvFile).then(
      this.checkCSVValidation
    );
  }

  importCSV() {
    this.validationErrors = null;
    this.uploadCSV(this.objectType, this.validationToken, this.csvFile)
      .then(response => {
        this.checkCSVValidation(response);
        if (this.validationToken) {
          this.$opensilex.showSuccessToast(
            this.$i18n.t("OntologyCsvImporter.csv-import-success-message")
          );
          this.$emit("csvImported");
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
        firstErrorType: null
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
          validationErrors: generalErrors.list[firstErrorType]
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
            validationErrors: globalErrors[i].list[firstErrorType]
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
            listSize: 1
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
