<template>
  <b-modal
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
              <opensilex-Icon icon="fa#list" />
              {{ $t("DataExportModal.title") }}
            </h4>
          </i>
        </b-col>
      </b-row>
    </template>
    <template>
      <opensilex-SelectForm
        label="DataExportModal.select-format"
        :selected.sync="format"
        :required="true"
        :multiple="false"
        :clearable="false"
        helpMessage="DataExportModal.select-format-help"
        :options="options"
      ></opensilex-SelectForm>
      <opensilex-FormField
        label="DataExportModal.check-raw-data"
        helpMessage="DataExportModal.raw-data-help"
      >
        <template v-slot:field="field">
          <b-form-checkbox v-model="withRawData" switch>
            {{$t("DataTemplateForm.raw-data")}}
          </b-form-checkbox>
        </template>
      </opensilex-FormField>

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

  </b-modal>
</template>

<script lang="ts">
import Vue from "vue";
import { Component, Prop, Ref } from "vue-property-decorator";

@Component
export default class DataExportModal extends Vue {
  $opensilex: any;
  $t: any;

  readonly longFormat = "long";
  readonly wideFormat = "wide";

  withRawData = false;
  format = this.wideFormat;

  options = [
    { id: this.longFormat, label: this.$t("DataExportModal.export-long")},
    {  id: this.wideFormat, label: this.$t("DataExportModal.export-wide")}
  ]

  @Ref("modal") readonly modal!: any;

  @Prop()
  filter;

  show() {
    this.modal.show();
  }

  hide() {
    this.modal.hide();
  }

  exportData() {
    let path = "/core/data/export";
    let today = new Date();
    let filename =
      "export_data_" +
      today.getFullYear() +
      String(today.getMonth() + 1).padStart(2, "0") +
      String(today.getDate()).padStart(2, "0");

    let params = {
      start_date: this.filter.start_date,
      end_date: this.filter.end_date,
      targets: this.filter.scientificObjects,
      experiments: this.filter.experiments,
      variables: this.filter.variables,
      provenances: [this.filter.provenance],
      mode: this.format,
      with_raw_data: this.withRawData
    }

    this.hide();
    this.$opensilex.downloadFilefromService(path, filename, "csv", params)
    
  }
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