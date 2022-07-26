<template>
  <div>
    <p @click="visible = !visible" style="cursor: pointer">
      <strong>{{ $t("DataHelp.exceptedFormat") }} </strong>
      <opensilex-Icon v-if="!visible" icon="fa#eye" class="DataHelpTableViewHelpEyeIcon" />
      <opensilex-Icon v-if="visible" icon="fa#eye-slash" class="DataHelpTableViewHelpEyeIcon" />
    </p>
    <b-collapse id="collapse-4" v-model="visible" class="mt-2">
      <b-table-simple responsive>
        <b-thead>
          <b-tr>
            <b-th>1</b-th>
            <b-th v-if="experiment == null">experiment</b-th>
            <b-th v-if="experiment != null">scientific_object<span class="required"> *</span></b-th>
            <b-th v-else>target</b-th>
            <b-th v-if="experiment == null">device</b-th>
            <b-th>date <span class="required"> *</span></b-th>
            <b-th>uri:variable1<span class="required"> *</span></b-th>
            <b-th>uri:variable...</b-th>
          </b-tr>
        </b-thead>
        <b-tbody>
          <b-tr>
            <b-th>2</b-th>
            <b-td v-if="experiment == null">{{ $t("DataHelp.experiment-help") }}</b-td>
            <b-td v-if="experiment != null">{{ $t("DataHelp.objectId-help") }}</b-td>
            <b-td v-else>{{ $t("DataHelp.targetId-help") }}</b-td>
            <b-td v-if="experiment == null">{{ $t("DataHelp.device-help") }}</b-td>
            <b-td>{{ $t("DataHelp.date-help") }}</b-td>
            <b-td>{{ $t("DataHelp.variable-help") }}</b-td>
            <b-td>{{ $t("DataHelp.variables-help") }}</b-td>
          </b-tr>
          <b-tr>
            <b-th>3</b-th>
            <b-td v-if="experiment == null"
              >{{ $t("DataHelp.column-type-help")
              }}<strong>{{ this.getDataTypeLabel("xsd:string") }}</strong
              ></b-td
            >
            <b-td v-if="experiment == null"
              >{{ $t("DataHelp.column-type-help")
              }}<strong>{{ this.getDataTypeLabel("xsd:string") }}</strong
              >
            </b-td>
            <b-td v-else
              >{{ $t("DataHelp.column-type-help")
              }}<strong>{{ this.getDataTypeLabel("xsd:string") }}<br />{{
                  $t("DataHelp.required")
                }}</strong
              >
            </b-td>
            <b-td v-if="experiment == null"
              >{{ $t("DataHelp.column-type-help")
              }}<strong>{{ this.getDataTypeLabel("xsd:string") }}</strong
              >
            </b-td>
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
          </b-tr>
          <b-tr class="alert alert-info">
            <b-th>4</b-th>
            <b-td v-if="experiment != null"
              :colspan = "6"
              v-html="
                $t('DataHelp.text-help', {
                  decimalSeparator: '<strong>.</strong>',
                  comma: this.$t('component.common.csv-delimiters.comma'),
                  semicolon: this.$t(
                    'component.common.csv-delimiters.semicolon'
                  ),
                })
              "
            >
            </b-td>
            <b-td v-else
              :colspan = "6"
              v-html="
                $t('DataHelp.text-help-global', {
                  decimalSeparator: '<strong>.</strong>',
                  comma: this.$t('component.common.csv-delimiters.comma'),
                  semicolon: this.$t(
                    'component.common.csv-delimiters.semicolon'
                  ),
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
import { Component, Prop, PropSync, Ref } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class DataHelpTableView extends Vue {
  $opensilex: any;
  $route: any;
  $store: any;
  $t: any;
  visible: boolean = true;

  stringType: string;
  integerType: string;

  @Prop()
  experiment;

  created() {
    this.stringType = this.getDataTypeLabel("xsd:string");
  }

  getDataTypeLabel(dataTypeUri: string): string {
    if (!dataTypeUri) {
      return undefined;
    }
    let label = this.$t(this.$opensilex.getDatatype(dataTypeUri).label_key);
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
    objectId : Scientific_object
    title: Generate Data template
    required: "Required : yes"
    variables-associated : Experiment variables associated
    download-template-example : Download template example
    csv-separator: CSV separator must be
    csv-decimal-separator: Decimal separator for numeric values must be 
    alias: Scientific object name
    date-help: "Acquisition date of the data (format: AAAA-MM-DDTHH:mm:ssZ or AAAA-MM-DD)"
    first-variable : "First Variable"
    other-variables : "Other variables, 1 per column...."
    column-type-help: "Column data type: "
    variable-data-help: Value of the variable (real number, text ou date)
    columns: CSV Files columns 
    file-rules: CSV editing rules
    objectId-help: Scientific object name or URI
    targetId-help: "Target (ex: scientific object) name or URI "
    experiment-help: Experiment name or URI
    device-help: Device name or URI
    variable-help : Variable 1 name
    variables-help : Other variables names
    text-help:  "You can insert data from this row. <br /> \n
            First three rows of CSV content will be ignored. <br /> \n
            <strong>You can't change the two first columns' order </strong> and \n
            you can add new ones as long as you don't change the variable URI \n
            the of the first row. \n
            <br /> \n
            Accepted CSV separators : <strong>{comma} or {semicolon}</strong><br /> \n
            Decimal separator is  <strong>\"{decimalSeparator}\"</strong><br /> \n
            <strong> If you don't specify offsets of date, the system will use the \n
            default timezone of the system.</strong>\n
            <br /> \n
            <strong>  Blank values will be ignored.<br>\n 
            Specials values authorized : NA, null and NaN for decimal</strong>\n"
    text-help-global:  "You can insert data from this row. <br /> \n
            First three rows of CSV content will be ignored. <br /> \n
            <strong>The \"experiment\", \"target\" and \"device\" columns are optional. You can remove them. In all three columns, you can give URIs or names. </strong> <br /> \n
            Target can be a scientific object, a facility or an event. <br /> \n
            If a device is already defined in the provenance, then it is not necessary to add the device column to the file.  If there is no device in the selected provenance, then it is mandatory to fill the \"device\" or the \"target\" column. <br />\n
            If needed, you can duplicate the columns \"experiment\" or \"device\" to link your data to several experiments or devices. <br />\n
            If needed, you can add a \"raw_data\" column to the right of each variable column. (see template generation) <br />\n
            Accepted CSV separators : <strong>{comma} or {semicolon}</strong><br /> \n
            Decimal separator is  <strong>\"{decimalSeparator}\"</strong><br /> \n
            <strong> If you don't specify offsets of date, the system will use the \n
            default timezone of the system.</strong>\n
            <br /> \n
            <strong>  Blank values will be ignored.<br>\n 
            Specials values authorized : NA, null and NaN for decimal</strong>\n"
fr :
  DataHelp:
    exceptedFormat: Format attendu
    objectId: Objet scientifique
    title: Générer un gabarit de données
    required: "Requis : oui"
    variables-associated: Variables associées à l'expérimentation
    download-template-example: Générer un gabarit de données d'exemple
    csv-separator: Le séparateur CSV doit être
    csv-decimal-separator: Le séparateur décimal des valeurs numériques doit être
    alias: Nom de l'objet scientifique
    first-variable : "1ère variable"
    other-variables : "Autres variables (1 par colonne) ..."
    date-help: "Date d'acquisition des données (format ISO 8601: AAAA-MM-JJTHH:mm:ssZ. Ex: 2020-09-21T00:00:00+0100)"
    column-type-help :  "Type de données colonne : "
    variable-data-help: Variable value (Real number, text or Date)
    columns: Colonnes du fichier CSV
    file-rules: Règles d'édition du CSV
    objectId-help: Nom ou l'URI de l'objet scientifique
    targetId-help: "Nom ou l'URI du target (ex: object scientifique)"
    experiment-help: Nom ou l'URI de l'expérimentation
    device-help: Nom ou l'URI du device
    variable-help : Nom de la variable 1 
    variables-help : Autres noms de variables
    text-help:  "Vous pouvez insérer les données à partir de cette ligne. <br /> \n
            Les trois premières lignes de contenu CSV seront ignorées. <br /> \n
            <strong>Vous ne pouvez pas changer l'ordre des deux premières colonnes  \n
            et vous pouvez en ajouter de nouvelles tant que vous ne changez pas l'URI variable de la première ligne.</strong> \n
            <br /> \n
            Les séparateurs CSV acceptés :<strong>{comma} or {semicolon}</strong><br /> \n
            Le séparateur décimal est le suivant : <strong>\"{decimalSeparator}\"</strong><br /> \n
            <strong> Si vous ne spécifiez pas de zone de temps dans vos dates, le système utilisera le fuseau horaire par défaut du système (UTC).</strong>\n
            <br /> \n
            <strong>Les valeurs vides seront ignorées.<br>\n
            Valeurs spéciales autorisées : NA, null et NaN pour les décimaux</strong> \n"
    text-help-global:  "Vous pouvez insérer les données à partir de cette ligne. <br /> \n
            Les trois premières lignes de contenu CSV seront ignorées. <br /> \n
            <strong>Les colonnes \"experiment\", \"target\" et \"device\" sont optionnelles. Vous pouvez les enlever. Dans ces 3 colonnes, vous pouvez mettre des URIs ou des noms. </strong> <br /> \n
            Le target peut être un objet scientifique ou une installation ou un événement. <br /> \n
            Si vous avez déjà renseigné le device dans la provenance, alors il n'est pas nécessaire d'ajouter la colonne device au fichier. Au contraire, si aucun device n'est renseigné dans la provenance sélectionnée alors il est nécessaire de remplir la colonne \"device\" ou \"target\". <br />\n
            Si besoin, vous pouvez dupliquer les colonnes \"experiment\" et \"device\" pour lier vos donner à plusieurs expérimentations ou équipements. <br />\n
            Si besoin, vous pouvez ajouter une colonne \"raw_data\" à droite de chaque colonne variable. (Voir la génération du template) <br />\n
            Les séparateurs CSV acceptés :<strong>{comma} or {semicolon}</strong><br /> \n
            Le séparateur décimal est le suivant : <strong>\"{decimalSeparator}\"</strong><br /> \n
            <strong> Si vous ne spécifiez pas de zone de temps dans vos dates, le système utilisera le fuseau horaire par défaut du système (UTC).</strong>\n
            <br /> \n
            <strong>Les valeurs vides seront ignorées.<br>\n
            Valeurs spéciales autorisées : NA, null et NaN pour les décimaux</strong> \n"

</i18n>