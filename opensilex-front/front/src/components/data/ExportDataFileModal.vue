<template>
  <b-modal
    ref="modal"
    :title="$t('DataFilesList.export')"
    ok-only
    size="lg"
  >
    <template v-slot:modal-header>
      <b-row class="mt-1" style="width: 100%">
        <b-col cols="10">
          <i>
            <h4>
              <opensilex-Icon icon="ik#ik-download" />
              {{ $t("DataFilesList.export") }}
            </h4>
          </i>
        </b-col>
      </b-row>
    </template>
    <b-row>
    <b-col cols="4">
    <b-form-group label="Format">
        <b-form-radio-group v-model="format">
            <b-form-radio value="dx">DX</b-form-radio>
            <b-form-radio value="tsv">TSV</b-form-radio>
            <b-form-radio value="csv">CSV</b-form-radio>
            <b-form-radio value="img">Image</b-form-radio>
        </b-form-radio-group>
    </b-form-group>
    </b-col>
    <b-col cols="4">
    <opensilex-CheckboxForm
        v-if="format==='tsv'"
        :value.sync="includeAverage"
        helpMessage="ExportDataFileModal.includeAverage-help"
        label="ExportDataFileModal.includeAverage"
        title="ExportDataFileModal.includeAverage-title"
    ></opensilex-CheckboxForm>
    </b-col>
    <b-col cols="4">
    <opensilex-CheckboxForm
        v-if="format==='tsv'"
        :value.sync="includeSampleDatetime"
        helpMessage="ExportDataFileModal.includeSampleDatetime-help"
        label="ExportDataFileModal.includeSampleDatetime"
        title="ExportDataFileModal.includeSampleDatetime-title"
    ></opensilex-CheckboxForm>
    </b-col>
    </b-row>
    <template v-slot:modal-footer>
    <b-button variant="outline-info" @click="goToExport()" v-on:click="hide(false)">Exporter</b-button>
  </template>

  </b-modal>
</template>

<script lang="ts">
import Vue from "vue";
import { Component, Ref} from "vue-property-decorator";
import VueI18n from "vue-i18n";

@Component
export default class ExportDataFileModal extends Vue {
    $opensilex: any;
    $t: typeof VueI18n.prototype.t;
    format = 'tsv';
    includeAverage = false;
    includeSampleDatetime = false;

    @Ref("modal") readonly modal!: any;

    show() {      
      this.modal.show();
    }

    hide() {
      this.modal.hide();
    }


    goToExport() {
        this.$emit('onCreate', this.format, this.includeAverage, this.includeSampleDatetime);
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
  ExportDataFileModal:
    format: Format
    includeAverage-help: Add row with average values of the same sample. Only for DX to TSV export.
    includeAverage: Include average
    includeAverage-title: Average
    includeSampleDatetime: Include datetime
    includeSampleDatetime-help: Add column with datetime. Only for DX to TSV export.
    includeSampleDatetime-title: Datetime

fr:
  ExportDataFileModal:
    format: Format
    includeAverage-help: Ajouter une ligne avec les valeurs moyennes du même échantillon. Uniquement pour l'export DX vers TSV.
    includeAverage: Inclure la moyenne
    includeAverage-title: Moyenne
    includeSampleDatetime: Inclure la date et l'heure
    includeSampleDatetime-help: Ajouter une colonne avec la date et heure. Uniquement pour l'export DX vers TSV.
    includeSampleDatetime-title: Date et heure

</i18n>