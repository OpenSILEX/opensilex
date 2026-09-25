<template>
  <Modal
    ref="modal"
    :title="$t('DataExportModal.title')"
    no-close-on-backdrop
    no-close-on-esc
  >
    <template v-slot:modal-header>
      <b-row class="mt-1" style="width: 100%">
        <b-col cols="10">
          <i>
            <h4>
              <Icon icon="fa#list" />
              {{ $t("DataExportModal.title") }}
            </h4>
          </i>
        </b-col>
      </b-row>
    </template>
    <template>
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
        <template v-slot:field="field">
          <b-form-checkbox v-model="withRawData" switch>
            {{$t("DataTemplateForm.raw-data")}}
          </b-form-checkbox>
        </template>
      </FormField>

    </template>
        <template v-slot:modal-footer>
      <button
        type="button"
        class="btn btn-secondary"
        v-on:click="hide(false)"
      >{{ $t('component.common.close') }}</button>

      <button
        type="button"
        class="btn greenThemeColor"
        v-on:click="exportData()"
      >{{ $t('DataExportModal.export') }}</button> 
    </template>

  </Modal>
</template>

<script setup lang="ts">
import {computed, inject, ref, useTemplateRef} from "vue";
import {DataSearchDTO} from "opensilex-core/model/dataSearchDTO";
import FormSelector from "@/components/common/forms/FormSelector.vue";
import FormField from "@/components/common/forms/FormField.vue";
import Icon from "@/components/common/views/Icon.vue";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import {useI18n} from "vue-i18n";
import {useStore} from "vuex";
import Modal from "@/components/common/views/Modal.vue";

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
      start_date: this.filter.start_date,
      end_date: this.filter.end_date,
      targets: this.filter.scientificObjects,
      devices: this.filter.devices,
      experiments: this.filter.experiments,
      variables: this.filter.variables,
      provenances: this.filter.provenance ? [this.filter.provenance] : null,
      mode: this.format,
      with_raw_data: this.withRawData
    }

    hide();
    opensilex.downloadFilefromPostService(path, filename, "csv", exportDto, store.state.lang)
    
  }

</script>

<style>
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
</i18n>