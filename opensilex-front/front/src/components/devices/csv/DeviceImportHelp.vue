<template>
  <div class="table-responsive-xl">
    <p @click="visible = !visible" style="cursor: pointer">
      <strong>{{ t('DeviceImportHelp.expected-format') }} </strong>
      <opensilex-Icon
        v-if="!visible"
        icon="fa#eye"
        class="DeviceImportHelpEyeIcon"
      />
      <opensilex-Icon
        v-else
        icon="fa#eye-slash"
        class="DeviceImportHelpEyeIcon"
      />
    </p>

    <div v-show="visible" class="mt-2">
      <div class="table-responsive">
        <table class="table help-table">
          <thead>
            <tr>
              <th>1</th>
              <th>URI</th>
              <th>type<span class="required">*</span></th>
              <th class="uri-field">rdfs:label</th>
              <th class="uri-field">rdfs:comment</th>

              <th class="uri-field">vocabulary:hasBrand</th>
              <th class="uri-field">vocabulary:hasModel</th>
              <th class="uri-field">vocabulary:hasSerialNumber</th>

              <th class="uri-field">vocabulary:startUp</th>
              <th class="uri-field">vocabulary:removal</th>
              <th class="uri-field">vocabulary:personInCharge</th>

              <th class="uri-field">uri:property...</th>
            </tr>
          </thead>

          <tbody>
            <tr>
              <th>2</th>
              <td>{{ t('DeviceImportHelp.uri-help') }}</td>
              <td>{{ t('DeviceImportHelp.type-help') }}</td>
              <td>{{ t('DeviceImportHelp.name-help') }}</td>
              <td>{{ t('DeviceImportHelp.comment-help') }}</td>

              <td>{{ t('DeviceImportHelp.brand-help') }}</td>
              <td>{{ t('DeviceImportHelp.constructor_model-help') }}</td>
              <td>{{ t('DeviceImportHelp.serial_number-help') }}</td>

              <td>{{ t('DeviceImportHelp.start_up-help') }}</td>
              <td>{{ t('DeviceImportHelp.removal-help') }}</td>
              <td>{{ t('DeviceImportHelp.person_in_charge-help') }}</td>

              <td>{{ t('DeviceImportHelp.properties-help') }}</td>
            </tr>

            <tr class="table-info">
              <th>3</th>
              <td colspan="100%">
                <div class="help-text">
                  <div>{{ t('DeviceImportHelp.text-help-line-1') }}</div>
                  <div>{{ t('DeviceImportHelp.text-help-line-2') }}</div>
                  <div>
                    <strong>{{ t('DeviceImportHelp.text-help-line-3') }}</strong>
                  </div>
                  <div>{{ t('DeviceImportHelp.text-help-line-4') }}</div>
                  <div>{{ t('DeviceImportHelp.text-help-line-5') }}</div>
                  <div>
                    {{ t('DeviceImportHelp.text-help-line-6') }}
                    <strong>{{ t('component.common.csv-delimiters.comma') }}</strong>
                    {{ t('DeviceImportHelp.text-help-line-6-bis') }}
                    <strong>{{ t('component.common.csv-delimiters.semicolon') }}</strong>
                  </div>
                  <div>
                    {{ t('DeviceImportHelp.text-help-line-7', { decimalSeparator: '.' }) }}
                  </div>
                  <div>
                    <strong>{{ t('DeviceImportHelp.text-help-line-8') }}</strong>
                  </div>
                  <br />
                  <div>
                    <strong>{{ t('DeviceImportHelp.text-help-line-9') }}</strong>
                  </div>
                  <div>
                    <strong>{{ t('DeviceImportHelp.text-help-line-10') }}</strong>
                  </div>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, inject } from 'vue'
import { useI18n } from 'vue-i18n'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'

const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const { t } = useI18n()
const visible = ref(true)

function getDataTypeLabel(dataTypeUri: string): string | undefined {
  if (!dataTypeUri) {
    return undefined
  }

  const label = t($opensilex.getDatatype(dataTypeUri).label_key)
  return label ? label.charAt(0).toUpperCase() + label.slice(1) : undefined
}
</script>

<style scoped>
.table-responsive {
  overflow-x: auto;
}

.help-table {
  width: max-content;
  min-width: 100%;
}

.help-table th,
.help-table td {
  white-space: nowrap;
  word-break: normal;
  overflow-wrap: normal;
  vertical-align: top;
}
</style>

<i18n>
en:
  DeviceImportHelp:
    brand-help: A brand of the device
    constructor_model-help: A constructor model of the device
    expected-format: "Expected format"
    title: "Generate Data template"
    required: "Required : yes"
    variables-associated: "Variables associated with the experiment"
    download-template-example: "Download template example"
    csv-separator: "CSV separator must be"
    csv-decimal-separator: "Decimal separator for numeric values must be"
    alias: "Device name"
    type-help: "URI of the device type"
    comment-help: "Description"
    properties-help: "Custom properties for device type..."
    column-type-help: "Column data type:"
    variable-data-help: "Value of the variable (real number, text ou date)"
    columns: "CSV Files columns"
    file-rules: "CSV editing rules"
    uri-help: "Device URI (autogenerated if empty)"
    name-help: "Device name"
    variables-help: "Other variables names"
    person_in_charge-help: Person in charge of the device
    serial_number-help: A serial number of the device
    removal-help: Date of removal
    start_up-help: Date of start up
    text-help-line-1: "Your can insert your data from this row."
    text-help-line-2: "First two rows of CSV content will be ignored."
    text-help-line-3: "You can add new columns corresponding to custom properties of device types."
    text-help-line-4: "If a property has multiple values, add a column for each with the same URI."
    text-help-line-5: "Column orders doesn't matter."
    text-help-line-6: "Accepted CSV separators:"
    text-help-line-6-bis: "or"
    text-help-line-7: 'Decimal separator is "{decimalSeparator}"'
    text-help-line-8: "If you don't specify offsets of date, the system will use the default timezone of the system."
    text-help-line-9: "Blank and unknown column identifier values will be ignored."
    text-help-line-10: "Only SensingDevice or Device with property vocabulary:measures can have variable."

fr:
  DeviceImportHelp:
    brand-help: Marque de l'appareil
    constructor_model-help: Modèle constructeur de l'appareil
    expected-format: "Format attendu"
    title: "Générer un gabarit de données"
    required: "Requis : oui"
    download-template-example: "Générer un gabarit de données d'exemple"
    csv-separator: "Le séparateur CSV doit être"
    csv-decimal-separator: "Le séparateur décimal des valeurs numériques doit être"
    alias: "Nom de l'équipement"
    type-help: "URI du type d'équipement"
    comment-help: "Description"
    properties-help: "Propriétés specifiques du type d'objet d'équipement..."
    column-type-help: "Type de données colonne :"
    columns: "Colonnes du fichier CSV"
    file-rules: "Règles d'édition du CSV"
    uri-help: "URI de l'équipement (auto-générée si vide)"
    name-help: "Nom de l'équipement"
    variables-help: "Autres noms de variables"
    person_in_charge-help: Personne responsable de l'appareil
    serial_number-help: Numéro de série de l'appareil
    start_up-help: Date d'obtention de l'appareil
    removal-help: Date de mise hors service de l'appareil
    text-help-line-1: "Vous pouvez insérer vos données à partir de cette ligne."
    text-help-line-2: "Les deux premières lignes de contenu CSV seront ignorées."
    text-help-line-3: "Vous pouvez ajouter de nouvelles colonnes correspondant aux propriétés particulières des équipements."
    text-help-line-4: "Si une propriété peut avoir plusieurs valeurs, ajoutez une colonne pour chaque avec la même URI."
    text-help-line-5: "L'ordre des colonnes n'a pas d'importance."
    text-help-line-6: "Les séparateurs CSV acceptés :"
    text-help-line-6-bis: "ou"
    text-help-line-7: 'Le séparateur décimal est le suivant : "{decimalSeparator}"'
    text-help-line-8: "Si vous ne spécifiez pas de zone de temps dans vos dates, le système utilisera le fuseau horaire par défaut du système (UTC)."
    text-help-line-9: "Les valeurs vides et les identifiant de colonnes inconnus seront ignorées."
    text-help-line-10: "Seuls les sous-types Capteurs ou les appareils avec la propriété vocabulary:measures peuvent être associés à une variable."
</i18n>