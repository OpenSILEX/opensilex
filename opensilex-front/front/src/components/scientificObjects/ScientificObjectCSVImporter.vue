 <template>
  <div>
    <opensilex-CreateButton @click="show" label="ScientificObjectCSVImporter.import"></opensilex-CreateButton>
    <b-modal ref="ScientificObjectCSVImporter" @ok.prevent="importCSV" size="xl" :static="true">
      <template v-slot:modal-ok>{{$t('component.common.ok')}}</template>
      <template v-slot:modal-cancel>{{$t('component.common.cancel')}}</template>

      <template v-slot:modal-title>
        <i>
          <slot name="icon">
            <opensilex-Icon icon="fa#eye" class="icon-title" />
          </slot>
          <span>{{$t("ScientificObjectCSVImporter.import")}}</span>
        </i>
      </template>
      <ValidationObserver ref="validatorRef">
        <!-- Type -->
        <opensilex-TypeForm
          :type.sync="scientificObjectType"
          @update:type="typeSwitch"
          :baseType="$opensilex.Oeso.SCIENTIFIC_OBJECT_TYPE_URI"
          :required="true"
          label="ScientificObjectCSVImporter.object-type"
          placeholder="ScientificObjectCSVImporter.object-type-placeholder"
        ></opensilex-TypeForm>

        <div v-if="scientificObjectType && fields.length > 0">
          <div class="row">
            <div class="col-md-12">
              <div class="static-field">
                <span class="field-view-title">{{$t("ScientificObjectCSVImporter.expectedFormat")}}:</span>
              </div>

              <b-table-simple hover small responsive :items="rows" :fields="fields">
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
                        {{$t("ScientificObjectCSVImporter.dataPlaceholder")}}
                        <br />
                        {{$t("ScientificObjectCSVImporter.ignoredFirstRows")}}
                        <br />
                        {{$t("ScientificObjectCSVImporter.columnModification")}}
                      </div>
                    </b-td>
                  </b-tr>
                </b-tfoot>
              </b-table-simple>
            </div>
          </div>
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
export default class ScientificObjectCSVImporter extends Vue {
  $opensilex: any;
  ontologyService: OntologyService;
  infraService: InfrastructuresService;
  $t: any;

  @Ref("ScientificObjectCSVImporter")
  readonly ScientificObjectCSVImporter!: any;

  @Ref("validatorRef") readonly validatorRef!: any;
  @Ref("inputFile") readonly inputFile!: any;

  scientificObjectType = null;

  @Prop()
  experimentUri;

  show() {
    this.scientificObjectType = null;
    this.validatorRef.reset();
    this.ScientificObjectCSVImporter.show();
  }

  hide() {
    this.ScientificObjectCSVImporter.hide();
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
                "ScientificObjectCSVImporter.objectURI"
              );
              commentRow[field.key] = this.$t(
                "ScientificObjectCSVImporter.objectURIComment"
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
    this.$opensilex.uploadFileToService(
      "/core/scientific-object/csv-validate",
      {
        description: {
          experiment: this.experimentUri,
          type: this.scientificObjectType
        },
        file: this.csvFile
      }
    ).then((response) => {
       this.validationToken = response.result.validationToken;
    });
  }

  importCSV() {
    this.$opensilex.uploadFileToService("/core/scientific-object/csv-import", {
      description: {
        experiment: this.experimentUri,
        type: this.scientificObjectType,
        validationToken: this.validationToken
      },
      file: this.csvFile
    });
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
</style>


<i18n>
en:
  ScientificObjectCSVImporter:
    import: CSV Import
    object-type: object type
    object-type-placeholder: Type here to search in object types
    expectedFormat: Expected CSV format
    objectURI: Object URI
    objectURIComment: You can set a custom URI or leave it empty to generate one.
    dataPlaceholder: Your can insert your data from this row.
    ignoredFirstRows: First three rows of CSV content will be ignored.
    columnModification: You can change columns' order and add new ones as long as you don't change the ID of the first row.

fr:
  ScientificObjectCSVImporter:
    import: Import CSV
    object-type: type d'objet
    object-type-placeholder: Utiliser cette zone pour rechercher un type d'objet
    expectedFormat: Format de fichier CSV attendu
    objectURI: URI de l'objet
    objectURIComment: Vous pouvez définir une URI personnalisé ou laisser vide pour en générer une.
    dataPlaceholder: Vous pouvez insérer vos données à partir de cette ligne.
    ignoredFirstRows: Les trois premières lignes de contenu du CSV seront ignorées.
    columnModification: Vous pouvez changer l'ordre des colonnes et en ajouter tant que vous ne modifiez pas l'identifiant de la première ligne.
</i18n>
