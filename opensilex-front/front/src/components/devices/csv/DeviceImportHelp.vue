<template>
  <div class="table-responsive-xl">
    <p @click="visible = !visible" style="cursor: pointer">
      <strong>{{ t('component.common.import-files.expected-format') }} </strong>
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
              <td>{{ t('component.device.help-messages.uri-help') }}</td>
              <td>{{ t('component.device.help-messages.type-help') }}</td>
              <td>{{ t('component.device.help-messages.name-help') }}</td>
              <td>{{ t('component.experiment.comment') }}</td>

              <td>{{ t('component.device.import.help-text.brand-help') }}</td>
              <td>{{ t('component.device.import.help-text.constructor_model-help') }}</td>
              <td>{{ t('component.device.import.help-text.serial_number-help') }}</td>

              <td>{{ t('component.device.import.help-text.start_up-help') }}</td>
              <td>{{ t('component.device.import.help-text.removal-help') }}</td>
              <td>{{ t('component.device.import.help-text.person_in_charge-help') }}</td>

              <td>{{ t('component.device.import.help-text.properties-help') }}</td>
            </tr>

            <tr class="table-info">
              <th>3</th>
              <td colspan="100%">
                <div class="help-text">
                  <div>{{ t('component.common.import-files.help-text-common.line-1') }}</div>
                  <div>{{ t('component.common.import-files.help-text-common.line-2') }}</div>
                  <div>
                    <strong>{{ t('component.device.import.help-text.custom-properties') }}</strong>
                  </div>
                  <div>{{ t('component.common.import-files.help-text-common.multiple-values') }}</div>
                  <div>{{ t('component.common.import-files.help-text-common.order-no-matter') }}</div>
                  <div>
                    {{ t('component.common.import-files.help-text-common.accepted_separators') }}
                    <strong>{{ t('component.common.csv-delimiters.comma') }}</strong>
                    {{ t('component.common.or') }}
                    <strong>{{ t('component.common.csv-delimiters.semicolon') }}</strong>
                  </div>
                  <div>
                    {{ t('component.common.import-files.help-text-common.decimal-separator', { decimalSeparator: '.' }) }}
                  </div>
                  <div>
                    <strong>{{ t('component.common.import-files.help-text-common.timezone-help') }}</strong>
                  </div>
                  <br />
                  <div>
                    <strong>{{ t('component.common.import-files.help-text-common.blank-values-help') }}</strong>
                  </div>
                  <div>
                    <strong>{{ t('component.device.import.help-text.measures-help') }}</strong>
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
