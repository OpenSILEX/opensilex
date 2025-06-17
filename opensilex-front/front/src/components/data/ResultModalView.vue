<template>
  <b-modal
    @hide="clearModal"
    v-model="modalShow"
    :title="$t('ResultModalView.title')"
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
        {{
          nbLinesImported > 1
            ? `${nbLinesImported} ${$t("ResultModalView.data-imported")}`
            : $t("ResultModalView.datum-imported")
        }}
      </p>
      <p v-if="nbAnnotationsImported != null && nbAnnotationsImported > 0" class="validation-confirm-container">
        {{
          nbAnnotationsImported > 1
            ? `${nbAnnotationsImported} ${$t("ResultModalView.annotations-imported")}`
            : $t("ResultModalView.annotation-imported")
        }}
      </p>
      <div class="details-container">
        <opensilex-ProvenanceDetails
          label="ResultModalView.provenanceLabel"
          v-if="provenance"
          :provenance="provenance"
          :dataImportResult="true"
        />

        <opensilex-BatchDetails
          label="ResultModalView.batchLabel"
          v-if="batch"
          :batchUri="batch"
        />
      </div>
    </template>

    <template v-slot:modal-footer> 
      <button
        type="button"
        class="btn greenThemeColor"
        v-on:click="hide()"
      >
        {{ $t('component.common.ok') }}
      </button>
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
  nbAnnotationsImported: number = null;

  provenance: any = null;
  batch: string = null;

  setNbLinesImported(value: number) {
    this.nbLinesImported = value;
  }

  setNbAnnotationsImported(value: number) {
      this.nbAnnotationsImported = value;
  }

  setProvenance(value) {
    this.provenance = value;
  }

  setBatch(value) {
    this.batch = value;
  }

  show() {
    this.modalShow = true;
  }

  hide() {
    this.modalShow = false;
  }

  clearModal() {
    this.nbLinesImported = null;
    this.nbAnnotationsImported = null;
    this.provenance = null;
    this.$emit("onHide");
  }
}
</script>

<style scoped>
.validation-confirm-container {
  color: rgb(40, 167, 69);
  font-weight: bold;
}

.details-container {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
}
</style>
<i18n>
  fr: 
    ResultModalView:
      data-imported: observations ont été importées avec succès
      datum-imported: Observation importée avec succès
      title : Rapport de l'insertion des données 
      provenanceLabel : Provenance
      annotations-imported: annotations ont été importées avec succès
      annotation-imported: Annotation importée avec succès
      batchLabel : Batch

  en: 
    ResultModalView:
      data-imported: Observations have been imported successfully
      datum-imported: Observation imported successfully
      title : Data insertion report
      provenanceLabel : Provenance
      annotations-imported: annotations have been imported successfully
      annotation-imported: Annotation imported successfully
      batchLabel : Batch
</i18n>