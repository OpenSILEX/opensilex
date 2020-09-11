 <template>
  <div>
    <opensilex-CreateButton @click="show" label="OntologyCsvImporter.import"></opensilex-CreateButton>
    <b-modal ref="OntologyCsvImporter" @ok.prevent="importCSV" size="xl" :static="true" :ok-disabled="!csvFile || !!validationErrors">
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
          :type.sync="scientificObjectType"
          @update:type="typeSwitch"
          :baseType="$opensilex.Oeso.SCIENTIFIC_OBJECT_TYPE_URI"
          :required="true"
          label="OntologyCsvImporter.object-type"
          placeholder="OntologyCsvImporter.object-type-placeholder"
        ></opensilex-TypeForm>

        <div v-if="scientificObjectType && fields.length > 0">
          <div class="row">
            <div class="col-md-4">
              <b-form-file
                size="sm"
                ref="inputFile"
                accept="text/csv, .csv"
                @input="csvUploaded"
                v-model="csvFile"
                placeholder="Drop or select CSV file here..."
                drop-placeholder="Drop file here..."
              ></b-form-file>
            </div>
            <div class="col">
              <b-button @click="downloadCSVTemplate">Download CSV template</b-button>
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
                    <b-td v-for="cell in row" v-bind:key="cell">{{cell}}</b-td>
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
                  <b-th :rowspan="row.listSize">{{row.index}}</b-th>
                  <b-td>{{$t("OntologyCsvImporter." + row.firstErrorType.type)}}</b-td>
                  <b-td
                  >
                  <ul>
                    <li v-for="validationErr in row.firstErrorType.validationErrors" v-bind:key="getErrKey(validationErr, row.firstErrorType.type)">
                      {{getValidationErrorDetail(validationErr, row.firstErrorType.type)}}
                    </li>
                  </ul>
                  </b-td>
                </b-tr>
                <b-tr
                  v-for="(validationError, errorType) in row.list"
                  v-bind:key="index + errorType">
                  <b-td
                  >{{$t("OntologyCsvImporter." + errorType)}}</b-td>
                  <b-td
                  >
                   <ul>
                    <li v-for="validationErr in validationError" v-bind:key="getErrKey(validationErr, errorType)">
                      {{getValidationErrorDetail(validationErr, errorType)}}
                    </li>
                  </ul>
                  </b-td>
                </b-tr>
               </slot>
              </b-tbody>
            </b-table-simple>
          </div>
        </div>
      </ValidationObserver>
    </b-modal>
  </div>
</template>

<script lang="ts">
import { Component, Prop, PropSync, Ref } from "vue-property-decorator";
import Vue from "vue";
import {
  ExperimentsService,
  InfrastructuresService,
  ResourceTreeDTO,
  OntologyService,
  RDFClassDTO
} from "opensilex-core/index";

@Component
export default class OntologyCsvImporter extends Vue {
  $opensilex: any;
  ontologyService: OntologyService;
  infraService: InfrastructuresService;
  $t: any;

  @Ref("OntologyCsvImporter")
  readonly OntologyCsvImporter!: any;

  @Ref("validatorRef") readonly validatorRef!: any;
  @Ref("inputFile") readonly inputFile!: any;

  scientificObjectType = null;

  @Prop()
  experimentUri;

  show() {
    this.scientificObjectType = null;
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
      csvRows.push(rowValues.join(","));
    }

    csvContent += csvRows.join("\r\n");

    let link = document.createElement("a");
    link.setAttribute("href", encodeURI(csvContent));
    link.setAttribute("download", "template.csv");
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }

  fields = [];
  rows = [];

  typeSwitch() {
    this.fields = [];
    this.rows = [];

    if (this.scientificObjectType != null) {
      return this.$opensilex
        .getService("opensilex.VueJsOntologyExtensionService")
        .getClassProperties(this.scientificObjectType)
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

            propertiesByURI[dataProperty.property] = dataProperty;
          }

          for (let i in classModel.objectProperties) {
            let objectProperty = classModel.objectProperties[i];

            this.fields.push({
              key: objectProperty.property,
              label: objectProperty.property
            });

            propertiesByURI[objectProperty.property] = objectProperty;
          }

          // TODO sort by properties order

          let nameRow = {};
          let commentRow = {};
          this.fields.forEach(field => {
            if (field.key == "URI") {
              nameRow[field.key] = this.$t(
                "OntologyCsvImporter.objectURI"
              );
              commentRow[field.key] = this.$t(
                "OntologyCsvImporter.objectURIComment"
              );
            } else {
              let fieldKey = field.key;
              let property = propertiesByURI[fieldKey];

              nameRow[fieldKey] = property.name;
              commentRow[fieldKey] = property.comment;
            }
          });

          this.rows = [nameRow, commentRow];
        });
    }
  }

  csvFile = null;

  validationToken = null;

  csvUploaded() {
    this.validationToken = null;
    this.validationErrors = null;
    this.$opensilex
      .uploadFileToService("/core/scientific-object/csv-validate", {
        description: {
          experiment: this.experimentUri,
          type: this.scientificObjectType
        },
        file: this.csvFile
      })
      .then(this.checkCSVValidation);
  }

  importCSV() {
    this.validationErrors = null;
    this.$opensilex
      .uploadFileToService("/core/scientific-object/csv-import", {
        description: {
          experiment: this.experimentUri,
          type: this.scientificObjectType,
          validationToken: this.validationToken
        },
        file: this.csvFile
      })
      .then((response) => {
        this.checkCSVValidation(response);
        if (this.validationToken) {
          this.$emit("csvImported");
          this.hide();
        }
      });
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
        generalErrors.listSize =  generalErrors.listSize + 1;
        if (!generalErrors.firstErrorType) {
          generalErrors.firstErrorType = "invalidHeaderURIs";
        }
      }

      this.validationErrors = [];

      if (generalErrors.firstErrorType) {
        generalErrors.listSize--;
        let firstErrorType = generalErrors.firstErrorType;
        generalErrors.firstErrorType =
        {
          type: firstErrorType,
          validationErrors: generalErrors.list[firstErrorType]
        }
        delete generalErrors.list[firstErrorType];
        this.validationErrors.push(generalErrors);
      }

      for (let i in globalErrors) {
        if (globalErrors[i].firstErrorType) {
          globalErrors[i].listSize =  globalErrors[i].listSize - 1;
          let firstErrorType = globalErrors[i].firstErrorType;
          globalErrors[i].firstErrorType =
          {
            type: firstErrorType,
            validationErrors: globalErrors[i].list[firstErrorType]
        }
            delete globalErrors[i].list[firstErrorType];
            this.validationErrors.push(globalErrors[i]);
        }
  
      }
    }
  }

  validationErrors = null;

  getValidationErrorDetail(validationError, errorType) {
    switch (errorType) {
      case "missingRequiredValueErrors": 
        return  this.$t("OntologyCsvImporter.validationErrorMissingRequiredMessage", validationError);
      case "duplicateURIErrors": 
        return  this.$t("OntologyCsvImporter.validationErrorDuplicateURIMessage", validationError);
      default: 
        return this.$t("OntologyCsvImporter.validationErrorMessage", validationError);
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
      console.error(rowIndex, errorType, errorItem, globalErrors)
      }
    }
  }

  getErrKey(validationError, errorType) {
    return "validationError-" + errorType + "-" + validationError.rowIndex + "-" + validationError.colIndex;
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
  margin-top:15px;
}

.error-container .field-view-title {
  color: red;
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
    validationErrorDuplicateURIMessage: "Column: '{header}' - Value: '{value}' - Identical with row: '{previousRow}'"

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
    validationErrorDuplicateURIMessage: "Colonne: '{header}' - Valeur: '{value}' - Identique à la ligne: '{previousRow}'"
</i18n>
