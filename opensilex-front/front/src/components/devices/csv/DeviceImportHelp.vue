<!--
  - OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
  - Copyright © INRAE 2022
  - Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
  -->

<template>
  <div>
    <p @click="visible = !visible" style="cursor: pointer">
      <strong>{{ $t("DeviceImportHelp.exceptedFormat") }} </strong>
      <opensilex-Icon v-if="!visible" icon="fa#eye" class="text-primary" />
      <opensilex-Icon v-if="visible" icon="fa#eye-slash" class="text-primary" />
    </p>
    <b-collapse id="collapse-4" v-model="visible" class="mt-2">
      <b-table-simple responsive>
        <b-thead>
          <b-tr>
            <b-th>1</b-th>
            <b-th>URI</b-th>
            <b-th>type<span class="required">*</span></b-th>
            <b-th class="uri-field">rdfs:label </b-th>
            <b-th class="uri-field">rdfs:comment</b-th>

            <b-th class="uri-field">vocabulary:hasBrand</b-th>
            <b-th class="uri-field">vocabulary:hasModel</b-th>
            <b-th class="uri-field">vocabulary:hasSerialNumber</b-th>

            <b-th class="uri-field">vocabulary:startUp</b-th>
            <b-th class="uri-field">vocabulary:removal</b-th>
            <b-th class="uri-field">vocabulary:personInCharge</b-th>

            <b-th class="uri-field">uri:property...</b-th>
          </b-tr>
        </b-thead>
        <b-tbody>
          <b-tr>
            <b-th>2</b-th>
            <b-td>{{ $t("DeviceImportHelp.uri-help") }}</b-td>
            <b-td>{{ $t("DeviceImportHelp.type-help") }}</b-td>
            <b-td>{{ $t("DeviceImportHelp.name-help") }}</b-td>
            <b-td>{{ $t("DeviceImportHelp.comment-help") }}</b-td>

            <b-td>{{ $t("DeviceForm.brand-help") }}</b-td>
            <b-td>{{ $t("DeviceForm.constructor_model-help") }}</b-td>
            <b-td>{{ $t("DeviceForm.serial_number-help") }}</b-td>

            <b-td>{{ $t("DeviceForm.start_up-help") }}</b-td>
            <b-td>{{ $t("DeviceForm.removal-help") }}</b-td>
            <b-td>{{ $t("DeviceForm.person_in_charge-help") }}</b-td>

            <b-td>{{ $t("DeviceImportHelp.properties-help") }}</b-td>
          </b-tr>
          <b-tr class="alert alert-info">
            <b-th>3</b-th>
            <b-td
                colspan="10"
                v-html="
                $t('DeviceImportHelp.text-help', {
                  decimalSeparator: '.',
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
import { Component } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class DeviceImportHelp extends Vue {
  $opensilex: any;
  $route: any;
  $store: any;
  $t: any;
  visible: boolean = true;

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
  DeviceImportHelp:
    exceptedFormat: Expected format
    title: Generate Data template
    required: "Required : yes"
    variables-associated : Variables associated with the experiment
    download-template-example : Download template example
    csv-separator: CSV separator must be
    csv-decimal-separator: Decimal separator for numeric values must be
    alias: Device name
    type-help: "URI of the device type"
    comment-help: "Description"
    properties-help: "Custom properties for device type..."
    column-type-help: "Column data type: "
    variable-data-help: Value of the variable (real number, text ou date)
    columns: CSV Files columns
    file-rules: CSV editing rules
    uri-help: Device URI (autogenerated if empty)
    name-help : Device name
    variables-help : Other variables names
    text-help:  "Your can insert your data from this row. <br /> \n
            First two rows of CSV content will be ignored. <br /> \n
            <strong>You can add new columns corresponding to custom properties of device types</strong><br /> \n
            If a property has multiple values, add a column for each with the same URI.<br /> \n
            Column orders doesn't matter.<br /> \n
            CSV separator is <strong>\",\"</strong><br /> \n
            Accepted CSV separators : <strong>{comma} or {semicolon}</strong><br /> \n
            <strong> If you don't specify offsets of date, the system will use the \n
            default timezone of the system.</strong>\n
            <br /> \n
            <strong>Blank and unknown column identifier values will be ignored.<br>.
            Only SensingDevice or Device with property vocabulary:measures can have variable."
fr :
  DeviceImportHelp:
    exceptedFormat: Format attendu
    title: Générer un gabarit de données
    required: "Requis : oui"
    download-template-example: Générer un gabarit de données d'exemple
    csv-separator: Le séparateur CSV doit être
    csv-decimal-separator: Le séparateur décimal des valeurs numériques doit être
    alias: Nom de l'équipement
    type-help: "URI du type d'équipement"
    comment-help: "Description"
    properties-help: "Propriétés specifiques du type d'objet d'équipement..."
    column-type-help :  "Type de données colonne : "
    columns: Colonnes du fichier CSV
    file-rules: Règles d'édition du CSV
    uri-help: URI de l'équipement (auto-générée si vide)
    name-help : Nom de l'équipement
    variables-help : Autres noms de variables
    text-help:  "Vous pouvez insérer vos données à partir de cette ligne. <br /> \n
            Les deux premières lignes de contenu CSV seront ignorées. <br /> \n
            <strong>Vous pouvez ajouter de nouvelles colonnes correspondant aux propriétés particulières des équipements</strong><br /> \n
            Si une propriété peut avoir plusieurs valeurs, ajoutez une colonne pour chaque avec la même URI.<br /> \n
            L'ordre des colonnes n'a pas d'importance.<br /> \n
            Les séparateurs CSV acceptés :<strong>{comma} or {semicolon}</strong><br /> \n
            Le séparateur décimal est le suivant : <strong>\"{decimalSeparator}\"</strong><br /> \n
            <strong> Si vous ne spécifiez pas de zone de temps dans vos dates, le système utilisera le fuseau horaire par défaut du système (UTC).</strong>\n
            <br /> \n
            <strong>Les valeurs vides et les identifiant de colonnes inconnus seront ignorées.<br>
            Seuls les sous-types Capteurs ou les dispositifs avec la propriété vocabulary:measures peuvent être associés à une variable."
</i18n>