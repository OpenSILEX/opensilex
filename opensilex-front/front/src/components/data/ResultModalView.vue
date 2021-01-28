<template>
  <b-modal
    @hide="clearModal"
    v-model="modalShow"
    :title="$t('ResultModalView.title')"
    ok-only
  >
    <template v-slot:modal-header>
      <b-row class="mt-1" style="width: 100%">
        <b-col cols="10">
          <i>
            <h4>
              <opensilex-Icon icon="fa#list" />
              {{ $t("ResultModalView.title") }}
            </h4>
          </i>
        </b-col>
      </b-row>
    </template>
    <template>
      <p v-if="nbLinesImported != null" class="validation-confirm-container">
        {{ nbLinesImported }}
        {{
          nbLinesImported > 1
            ? $t("ResultModalView.data-imported")
            : $t("ResultModalView.datum-imported")
        }}
      </p>
      <opensilex-ProvenanceDetails
        label="ResultModalView.provenanceLabel"
        v-if="provenance"
        :provenance="provenance"
      ></opensilex-ProvenanceDetails>
    </template>
  </b-modal>
</template>

<script lang="ts">
import Vue from "vue";
import { Component, Prop } from "vue-property-decorator";

@Component
export default class ResultModalView extends Vue {
  $t: any;
  modalShow: boolean = false;

  nbLinesImported: number = null;

  provenance: any = null;

  setNbLinesImported(value: number) {
    this.nbLinesImported = value;
  }

  setProvenance(value) {
    this.provenance = value;
  }

  show() {
    this.modalShow = true;
  }

  hide() {
    this.modalShow = false;
  }

  clearModal() {
    this.nbLinesImported = null;
    this.provenance = null;
    this.$emit("onHide");
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
  fr: 
    ResultModalView:
      data-imported: observations ont été importées avec succès
      datum-imported: L'observation a été importée avec succès
      title : Rapport de l'insertion des données 
      provenanceLabel : Description des données

  en: 
    ResultModalView:
      data-imported: Observations have been imported successfully
      datum-imported: Observation has been imported successfully
      title : Data insertion report
      provenanceLabel : Data description 
</i18n>