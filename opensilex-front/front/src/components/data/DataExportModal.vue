<template>
  <Modal ref="modal">
    <template #header>
      <i>
        <h4>
          <Icon icon="fa#list" />
          {{ t("DataExportModal.title") }}
        </h4>
      </i>
    </template>

    <FormSelector
      label="DataExportModal.select-format"
      v-model:selected="format"
      :required="true"
      :multiple="false"
      :clearable="false"
      helpMessage="DataExportModal.select-format-help"
      :options="options"
    ></FormSelector>
    <FormField
      label="DataExportModal.check-raw-data"
      helpMessage="DataExportModal.raw-data-help"
    >
      <template #field>
        <div class="switch-field">
          <n-switch v-model:value="withRawData" :theme-overrides="switchThemeOverrides" />
          <span class="switch-field__label" @click="withRawData = !withRawData">{{ t("DataTemplateForm.raw-data") }}</span>
        </div>
      </template>
    </FormField>

    <template #footer>
      <button
        type="button"
        class="btn btn-secondary"
        v-on:click="hide()"
      >{{ t('component.common.close') }}</button>

      <button
        type="button"
        class="btn greenThemeColor"
        v-on:click="exportData()"
      >{{ t('DataExportModal.export') }}</button>
    </template>

  </Modal>
</template>

<script setup lang="ts">
import {computed, inject, ref, useTemplateRef} from "vue";
import {DataSearchDTO} from "opensilex-core/model/dataSearchDTO";
import FormSelector from "@/components/common/forms/FormSelector.vue";
import FormField from "@/components/common/forms/FormField.vue";
import {NSwitch} from "naive-ui";
import Icon from "@/components/common/views/Icon.vue";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import {useI18n} from "vue-i18n";
import {useStore} from "vuex";
import Modal from "@/components/common/views/Modal.vue";

const switchThemeOverrides = {
  railColorActive: "#00a38d",
  boxShadowFocus: "0 0 0 2px rgba(0, 163, 141, 0.2)"
};

const props = defineProps<{
  filter: any
}>()

const opensilex = inject<OpenSilexVuePlugin>('$opensilex')
const { t } = useI18n()
const store = useStore()
const longFormat = "long";
const wideFormat = "wide";
const modal = useTemplateRef<InstanceType<typeof Modal>>('modal')

const withRawData = ref(false);
const format = ref(wideFormat);

const options = computed(() => [
  {
    id: longFormat,
    label: t("DataExportModal.export-long"),
  },
  {
    id: wideFormat,
    label: t("DataExportModal.export-wide"),
  },
]);

  function show() {
    modal.value.show();
  }

  function hide() {
    modal.value.hide();
  }

  function exportData() {
    let path = "/core/data/export";
    let today = new Date();
    let filename =
      "export_data_" +
      today.getFullYear() +
      String(today.getMonth() + 1).padStart(2, "0") +
      String(today.getDate()).padStart(2, "0");

    let exportDto: DataSearchDTO = {
      start_date: props.filter.start_date,
      end_date: props.filter.end_date,
      targets: props.filter.scientificObjects,
      devices: props.filter.devices,
      experiments: props.filter.experiments,
      variables: props.filter.variables,
      provenances: props.filter.provenance ? [props.filter.provenance] : null,
      mode: format.value,
      with_raw_data: withRawData.value
    }

    hide();
    opensilex.downloadFilefromPostService(path, filename, "csv", exportDto, store.state.lang)
  }

  defineExpose({
    show,
    hide
  })

</script>

<style>
.switch-field {
  display: flex;
  align-items: center;
  gap: 8px;
}

.switch-field__label {
  cursor: pointer;
}

.validation-confirm-container {
  color: rgb(40, 167, 69);
  font-weight: bold;
}
</style>
<i18n>
  en: 
    DataExportModal:
      title: Data export
      export: Export
      select-format: File format
      select-format-help: "Long format: each line represents an observation (same as the result table).   Wide format: a given date, provenance, scientific object of an observation represents a row and each variable value is in a specific column."
      check-raw-data: With Raw data column
      raw-data-help: "If checked, the column \"raw_data\" will be added to the export file"
      export-long: Long format
      export-wide: Wide format
    DataTemplateForm:
      raw-data: "Raw data"

  fr: 
    DataExportModal:
      title: Export des données
      export: Exporter
      select-format: Format du fichier
      select-format-help: "Format long : une ligne représente une observation (identique au tableau de résultat). Format large : une date, une provenance, un objet scientifique donné d'une observation représente une ligne et chaque valeur de variable est dans une colonne spécifique."
      check-raw-data: "Avec la colonne Raw Data (données brutes)"
      raw-data-help: "Si coché, la colonne \"raw_data\" sera ajouté au fichier d'export"
      export-long: Format long
      export-wide: Format large
    DataTemplateForm:
      raw-data: "Données brutes"
</i18n>