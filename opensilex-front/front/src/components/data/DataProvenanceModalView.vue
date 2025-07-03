<template>
  <b-modal v-model="modalShow" size="lg" centered scrollable :title="$t('DataProvenanceModalView.title')">
    <template v-slot:modal-header>
      <b-row class="mt-1" style="width: 100%">
        <b-col cols="10">
          <i>
            <h4>
              <opensilex-Icon icon="fa#eye" />
            </h4>
          </i>
        </b-col>
        <b-col cols="2">
          <!-- Emulate built in modal header close button action -->
          <button type="button" class="close" @click="hide" data-dismiss="modal" aria-label="Close">
            <span aria-hidden="true">&times;</span>
          </button>
        </b-col>
      </b-row>
    </template>
    <template>
      <h3 v-if="datafile">{{ $t("DataProvenanceModalView.datafile") }}</h3>
      <h3 v-else>{{ $t("DataProvenanceModalView.data") }}</h3>
      <pre>{{ data }}</pre>
      <h3>Provenance</h3>
      <pre>{{ provenance }}</pre>
      <h3 v-if="batch">Batch</h3>
      <pre v-if="batch">{{ batch }}</pre>
    </template>

    <template v-slot:modal-footer>
      <button
          type="button"
          class="btn greenThemeColor"
          v-on:click="hide(false)"
      >
          {{ $t('component.common.close') }}
      </button>
    </template>

  </b-modal>
</template>

<script lang="ts">
import Vue from "vue";
import { Component, Prop } from "vue-property-decorator";

@Component
export default class DataProvenanceModalView extends Vue {
  @Prop({
    default: false
  })
  datafile: boolean;

  modalShow: boolean = false;

  info: any = null;
  data: string = null;
  provenance: string = null;
  batch: string = null;

  setProvenanceAndBatch(value) {
    this.data = JSON.stringify(value.data, null, 2);
    this.provenance = JSON.stringify(value.provenance, null, 2);
    this.batch = JSON.stringify(value.batch, null, 2);
  }

  show() {
    this.modalShow = true;
  }

  hide() {
    this.modalShow = false;
  }
}
</script>

<i18n>
  fr: 
    DataProvenanceModalView:
      data : Donnée
      datafile : Fichier de données
      

  en: 
    DataProvenanceModalView:
      data : Data
      datafile : Datafile
</i18n>