<template>
  <div>
    <p @click="visible = !visible" style="cursor: pointer">
      <strong>{{ t("component.common.import-files.expected-format") }} </strong>
      <Icon v-if="!visible" icon="fa#eye" class="ScientificObjectImportHelpEyeIcon" />
      <Icon v-if="visible" icon="fa#eye-slash" class="ScientificObjectImportHelpEyeIcon" />
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

            <th class="uri-field">vocabulary:hasCreationDate</th>
            <th class="uri-field">vocabulary:hasDestructionDate</th>
            <th class="uri-field">vocabulary:isPartOf</th>

            <th class="uri-field">uri:property...</th>

            <th>start_date_of_location</th>
            <th>end_date_of_location</th>
            <th>{{ t("component.common.geometry.from") }}</th>
            <th>{{ t("component.common.geometry.too") }}</th>
            <th>{{ t("component.common.geometry.coordinates") }}</th>
            <th>{{ t("component.common.geometry.x") }}</th>
            <th>{{ t("component.common.geometry.y") }}</th>
            <th>{{ t("component.common.geometry.z") }}</th>
            <th>{{ t("component.common.geometry.textual-position") }}</th>
          </tr>
          </thead>

          <tbody>
          <tr>
            <th>2</th>
            <td>{{ t('component.scientificObjects.help-messages.uri-help') }}</td>
            <td>{{ t('component.scientificObjects.help-messages.type-help') }}</td>
            <td>{{ t('component.scientificObjects.help-messages.name-help') }}</td>
            <td>{{ t('component.experiment.comment') }}</td>

            <td>{{ t("component.scientificObjects.import.help-column-headers.hasCreationDate-help") }}</td>
            <td>{{ t("component.scientificObjects.import.help-column-headers.hasDestructionDate-help") }}</td>
            <td>{{ t("component.scientificObjects.import.help-column-headers.isPartOf-help") }}</td>
            <td>{{ t("component.scientificObjects.import.help-column-headers.properties-help") }}</td>
            <td>{{ t("component.scientificObjects.geometry.startHelp") }}</td>
            <td>{{ t("component.scientificObjects.geometry.endHelp") }}</td>
            <td>{{ t("component.common.geometry.from-help") }}</td>
            <td>{{ t("component.common.geometry.to-help") }}</td>
            <td>{{ t("component.common.geometry.geometry-help") }}</td>
            <td>{{ t("component.common.geometry.x-help") }}</td>
            <td>{{ t("component.common.geometry.y-help") }}</td>
            <td>{{ t("component.common.geometry.z-help") }}</td>
            <td>{{ t("component.common.geometry.textual-position-help") }}</td>
          </tr>

          <tr class="table-info">
            <th>3</th>
            <td colspan="100%">
              <div class="help-text">
                <div>{{ t('component.common.import-files.help-text-common.line-1') }}</div>
                <div>{{ t('component.common.import-files.help-text-common.line-2') }}</div>
                <div>
                  <strong>{{ t('component.scientificObjects.import.help-text.custom-properties') }}</strong>
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
                  <strong>{{ t('component.scientificObjects.import.help-text.geospatial-line-1') }}</strong>
                </div>
                <div>
                  {{ t('component.scientificObjects.import.help-text.geospatial-line-2') }}
                </div>
                <div>
                  {{ t('component.scientificObjects.import.help-text.geospatial-line-3') }}
                </div>
                <div>
                  {{ t('component.scientificObjects.import.help-text.geospatial-line-4') }}
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
import {ref} from "vue";
import { useI18n } from "vue-i18n";
import Icon from "@/components/common/views/Icon.vue";

const {t} = useI18n();

const visible = ref<boolean>(true);

</script>

<style scoped lang="scss">
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
