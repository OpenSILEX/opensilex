<template>
  <div>
    <p @click="visible = !visible" style="cursor: pointer;" >
      <strong>{{ $t("DataHelp.exceptedFormat") }} </strong>
      <opensilex-Icon v-if="!visible" icon="fa#eye" class="text-primary" />
      <opensilex-Icon v-if="visible" icon="fa#eye-slash" class="text-primary"/>
     </p>
    <b-collapse id="collapse-4" v-model="visible" class="mt-2">
      <b-table-simple responsive>
        <b-thead>
          <b-tr>
            <b-th>1</b-th>
            <b-th>URI<span class="required"> *</span></b-th>
            <b-th>type<span class="required"> *</span></b-th>
            <b-th>name<span class="required"> *</span></b-th>
            <b-th class="uri-field">vocabulary:hasCreationDate</b-th>
            <b-th class="uri-field">vocabulary:hasDestructionDate</b-th>
            <b-th class="uri-field">vocabulary:hasFacility</b-th>
            <b-th class="uri-field">vocabulary:isPartOf</b-th>
            <b-th class="uri-field">rdfs:comment</b-th>
            <b-th>geometry</b-th>
            <b-th class="uri-field">uri:property...</b-th>
          </b-tr>
        </b-thead>
        <b-tbody>
          <b-tr>
            <b-th>2</b-th>
            <b-td>{{ $t("DataHelp.objectId-help") }}</b-td>
            <b-td>{{ $t("DataHelp.type-help") }}</b-td>
            <b-td>{{ $t("DataHelp.name-help") }}</b-td>
            <b-td>{{ $t("DataHelp.hasCreationDate-help") }}</b-td>
            <b-td>{{ $t("DataHelp.hasDestructionDate-help") }}</b-td>
            <b-td>{{ $t("DataHelp.hasFacility-help") }}</b-td>
            <b-td>{{ $t("DataHelp.isPartOf-help") }}</b-td>
            <b-td>{{ $t("DataHelp.comment-help") }}</b-td>
            <b-td>{{ $t("DataHelp.geometry-help") }}</b-td>
            <b-td>{{ $t("DataHelp.properties-help") }}</b-td>
          </b-tr>
          <!-- <b-tr>
            <b-th>3</b-th>
            <b-td
              >{{ $t("DataHelp.column-type-help")
              }}<strong>{{ this.getDataTypeLabel("xsd:string") }}</strong
              ><br /><strong>{{ $t("DataHelp.required") }}</strong></b-td
            >
            <b-td>
              {{ $t("DataHelp.column-type-help") }}
              <strong>
                {{ this.getDataTypeLabel("xsd:date") }}<br />{{
                  $t("DataHelp.required")
                }}</strong
              ></b-td
            >
            <b-td
              >{{ $t("DataHelp.column-type-help") }}
              <strong>
                {{
                  this.getDataTypeLabel("xsd:string") +
                  ", " +
                  this.getDataTypeLabel("xsd:integer") +
                  ", " +
                  this.getDataTypeLabel("xsd:boolean") +
                  ", " +
                  this.getDataTypeLabel("xsd:date")
                }}</strong
              ></b-td
            >
            <b-td
              >{{ $t("DataHelp.column-type-help") }}
              <strong>
                {{
                  this.getDataTypeLabel("xsd:string") +
                  ", " +
                  this.getDataTypeLabel("xsd:integer") +
                  ", " +
                  this.getDataTypeLabel("xsd:boolean") +
                  ", " +
                  this.getDataTypeLabel("xsd:date")
                }}</strong
              ></b-td
            >
          </b-tr> -->
          <b-tr class="alert alert-info">
            <b-th>3</b-th>
            <b-td
              colspan="10"
              v-html="
                $t('DataHelp.text-help', {
                  decimalSeparator: '.'
                })
              "
            >
            </b-td>
          </b-tr>
        </b-tbody>
      </b-table-simple>
    </b-collapse>
  </div>
</template>

<script lang="ts">
import { Component } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class ScientificObjectImportHelp extends Vue {
  $opensilex: any;
  $route: any;
  $store: any;
  $t: any;
  visible: boolean = true;

  getDataTypeLabel(dataTypeUri: string): string {
    if (!dataTypeUri) {
      return undefined;
    }
    let label = this.$t(this.$opensilex.getDatatype(dataTypeUri).labelKey);
    return label.charAt(0).toUpperCase() + label.slice(1);
  }
}
</script>

<style scoped lang="scss">
table.b-table-selectable tbody tr td span {
  line-height: 24px;
  text-align: center;
  position: relative;
  margin-bottom: 0;
  vertical-align: top;
}

table.b-table-selectable tbody tr td span.checkbox,
.custom-control.custom-checkbox {
  top: -4px;
  line-height: unset;
}

.modal .custom-control-label:after,
.modal .custom-control-label:before {
  left: 0rem;
}

.custom-checkbox {
  padding-left: 12px;
}

.modal table.b-table-selectable tbody tr td span.checkbox:after,
.modal table.b-table-selectable tbody tr td span.checkbox:before {
  left: 0.75rem;
  width: 1rem;
  height: 1rem;
  content: "";
}

table.b-table-selectable tbody tr td span.checkbox:after,
table.b-table-selectable tbody tr td span.checkbox:before {
  position: absolute;
  top: 0.25rem;
  left: 0;
  display: block;
  width: 1rem;
  height: 1rem;
  content: "";
}

table.b-table-selectable tbody tr td span.checkbox:before {
  border-radius: 4px;
  pointer-events: none;
  background-color: #fff;
  border: 1px solid #adb5bd;
}

table.b-table-selectable tbody tr.b-table-row-selected td span.checkbox:before {
  color: #fff;
  border-color: #007bff;
  background-color: #007bff;
}

table.b-table-selectable tbody tr.b-table-row-selected td span.checkbox:after {
  background-image: none;
  content: "\e83f";
  line-height: 16px;
  font-family: "iconkit";
  color: #fff;
}
</style>

<i18n>
en :
  DataHelp:
    exceptedFormat: Expected format
    title: Generate Data template
    required: "Required : yes"
    variables-associated : Experiment variables associated
    download-template-example : Download template example
    csv-separator: CSV separator must be
    csv-decimal-separator: Decimal separator for numeric values must be 
    alias: Scientific object name
    type-help: "URI of the scientific object type"
    first-variable : "First Variable"
    other-variables : "Other variables, 1 per column...."
    column-type-help: "Column data type: "
    variable-data-help: Value of the variable (real number, text ou date)
    columns: CSV Files columns 
    file-rules: CSV editing rules
    objectId-help: Scientific object URI (autogenerated if empty)
    name-help : Scientific object name
    variables-help : Other variables names
    text-help:  "Your can insert your data from this row. <br /> \n
            First two rows of CSV content will be ignored. <br /> \n
            <strong>You can add new columns corresponding to custom properties of scientific objet types</strong><br /> \n
            If a property has multiple values, add a column for each with the same URI.<br /> \n
            Column orders doesn't matter.<br /> \n
            CSV separator is <strong>\",\"</strong><br /> \n
            Decimal separator is  <strong>\"{decimalSeparator}\"</strong><br /> \n
            <strong> If you don't specify offsets of date, the system will use the \n
            default timezone of the system.</strong>\n
            <br /> \n
            <strong>Blank and unknown column identifier values will be ignored.<br>"
fr :
  DataHelp:
    exceptedFormat: Format attendu
    title: Générer un gabarit de données
    required: "Requis : oui"
    variables-associated: Variables associées à l'expériementation
    download-template-example: Générer un gabarit de données d'exemple
    csv-separator: Le séparateur CSV doit être
    csv-decimal-separator: Le séparateur décimal des valeurs numériques doit être
    alias: Nom de l'objet scientifique
    first-variable : "1ère variable"
    other-variables : "Autres variables (1 par colonne) ..."
    type-help: "URI du type d'objet scientifique"
    column-type-help :  "Type de données colonne : "
    variable-data-help: Variable value (Real number, text or Date)
    columns: Colonnes du fichier CSV
    file-rules: Règles d'édition du CSV
    objectId-help: URI de l'objet scientifique (auto-générée si vide)
    name-help : Nom de l'objet scientifique
    variables-help : Autres noms de variables
    text-help:  "Vous pouvez insérer vos données à partir de cette ligne. <br /> \n
            Les deux premières lignes de contenu CSV seront ignorées. <br /> \n
            <strong>Vous pouvez ajouter de nouvelles colonnes correspondant aux propriétés particulières des objets scientifiques</strong><br /> \n
            Si une propriété peut avoir plusieurs valeurs, ajoutez une colonne pour chaque avec la même URI.<br /> \n
            L'ordre des colonnes n'a pas d'importance.<br /> \n
            Le séparateur CSV est le suivant :<strong>\";\"</strong><br /> \n
            Le séparateur décimal est le suivant : <strong>\"{decimalSeparator}\"</strong><br /> \n
            <strong> Si vous ne spécifiez pas de zone de temps dans vos dates, le système utilisera le fuseau horaire par défaut du système (UTC).</strong>\n
            <br /> \n
            <strong>Les valeurs vides et les identifiant de colonnes inconnus seront ignorées.<br>"

</i18n>